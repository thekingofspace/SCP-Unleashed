package net.rai.scp_unleashed.scp323;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Scp323Skull extends Entity {
    public static final float Width = 0.6f;
    public static final float Height = 0.45f;
    public static final double Gravity = 0.04;
    public static final double Drag = 0.98;
    public static final double TerminalFall = 1.5;
    public static final double FaceLift = 0.22;
    public static final int AshChance = 90;
    public static final double RenderDistance = 64.0;
    public static final int DeathQuiet = 40;
    public static final int CureQuiet = 200;
    public static final int WakeCheck = 10;
    private static final EntityDataAccessor<Boolean> RestlessData = SynchedEntityData.defineId(Scp323Skull.class, EntityDataSerializers.BOOLEAN);

    private ItemStack Stack = ItemStack.EMPTY;
    private int Quiet;

    public Scp323Skull(EntityType<? extends Scp323Skull> Type, Level World) {
        super(Type, World);
    }

    @Nullable
    public static Scp323Skull place(ServerLevel World, Vec3 Spot, float Yaw, boolean Audible) {
        return place(World, Spot, Yaw, Scp323SkullItem.fresh(), Audible);
    }

    @Nullable
    public static Scp323Skull place(ServerLevel World, Vec3 Spot, float Yaw, ItemStack Source, boolean Audible) {
        Scp323Skull Placed = new Scp323Skull(Scp323Registry.Skull.get(), World);
        Placed.Stack = Scp323SkullItem.is(Source) ? Source.copyWithCount(1) : Scp323SkullItem.fresh();
        Placed.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        if (!World.addFreshEntity(Placed)) {
            return null;
        }
        if (Audible) {
            World.playSound(null, Spot.x, Spot.y, Spot.z, Scp323Registry.SetDown.get(), SoundSource.BLOCKS, 0.8f, 0.92f + World.random.nextFloat() * 0.12f);
        }
        return Placed;
    }

    public ItemStack stack() {
        return Stack.isEmpty() ? Scp323SkullItem.fresh() : Stack.copy();
    }

    public Vec3 gazePoint() {
        return position().add(0.0, FaceLift, 0.0);
    }

    public boolean isQuiet() {
        return Quiet > 0;
    }

    public void hush(int Ticks) {
        Quiet = Math.max(Quiet, Ticks);
    }

    public boolean isRestless() {
        return entityData.get(RestlessData);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(RestlessData, false);
    }

    @Override
    public void tick() {
        super.tick();
        settle();
        if (level().isClientSide) {
            if (isRestless()) {
                drift();
            }
            return;
        }
        if ((tickCount + getId()) % WakeCheck == 0 || tickCount == 1) {
            boolean Night = Scp323Night.isNight(level());
            if (isRestless() != Night) {
                entityData.set(RestlessData, Night);
            }
        }
        if (Quiet > 0) {
            Quiet--;
            return;
        }
        if (isRestless() && level() instanceof ServerLevel World && (tickCount + getId()) % Scp323Lure.NpcInterval == 0) {
            Scp323Lure.drawPeople(World, gazePoint(), this);
        }
    }

    private void settle() {
        Vec3 Motion = getDeltaMovement();
        if (onGround() && Motion.y <= 0.0 && !level().noCollision(this, getBoundingBox().move(0.0, -0.04, 0.0))) {
            if (Motion.lengthSqr() > 0.0) {
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        double Fall = Math.max(-TerminalFall, (Motion.y - Gravity) * Drag);
        setDeltaMovement(0.0, Fall, 0.0);
        move(MoverType.SELF, getDeltaMovement());
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private void drift() {
        RandomSource Random = level().random;
        if (Random.nextInt(AshChance) == 0) {
            double Yaw = Math.toRadians(getYRot());
            double Side = Random.nextBoolean() ? 0.07 : -0.07;
            double X = getX() + Math.cos(Yaw) * Side;
            double Z = getZ() + Math.sin(Yaw) * Side;
            level().addParticle(ParticleTypes.WHITE_ASH, X, getY() + FaceLift + 0.05, Z, 0.0, 0.01, 0.0);
        }
    }

    @Override
    public void move(MoverType Kind, Vec3 Delta) {
        if (Kind == MoverType.SELF) {
            super.move(Kind, new Vec3(0.0, Delta.y, 0.0));
        }
    }

    @Override
    public InteractionResult interact(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return Scp323Possession.touched(this, User);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity Attacker) {
        return true;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < RenderDistance * RenderDistance;
    }

    @Override
    public ItemStack getPickResult() {
        return Scp323SkullItem.fresh();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        Stack = Tag.contains("Item") ? ItemStack.parseOptional(registryAccess(), Tag.getCompound("Item")) : ItemStack.EMPTY;
        Quiet = Tag.getInt("Quiet");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        if (!Stack.isEmpty()) {
            Tag.put("Item", Stack.save(registryAccess()));
        }
        if (Quiet > 0) {
            Tag.putInt("Quiet", Quiet);
        }
    }
}
