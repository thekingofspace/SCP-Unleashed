package net.rai.scp_unleashed.scp035;

import net.minecraft.nbt.CompoundTag;
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

public class Scp035Mask extends Entity {
    public static final float Width = 0.5f;
    public static final float Height = 0.125f;
    public static final double Gravity = 0.04;
    public static final double Drag = 0.98;
    public static final double TerminalFall = 1.5;
    public static final double FaceLift = 0.06;
    public static final int GlintChance = 70;
    public static final int WeepChance = 55;
    public static final double RenderDistance = 64.0;
    public static final float RestScale = 0.7f;
    public static final double EyeSpread = 3.5 / 16.0 * RestScale;

    private ItemStack Stack = ItemStack.EMPTY;

    public Scp035Mask(EntityType<? extends Scp035Mask> Type, Level World) {
        super(Type, World);
    }

    @Nullable
    public static Scp035Mask place(ServerLevel World, Vec3 Spot, float Yaw, boolean Audible) {
        return place(World, Spot, Yaw, Scp035MaskItem.fresh(), Audible);
    }

    @Nullable
    public static Scp035Mask place(ServerLevel World, Vec3 Spot, float Yaw, ItemStack Source, boolean Audible) {
        Scp035Mask Placed = new Scp035Mask(Scp035Registry.Mask.get(), World);
        Placed.Stack = Scp035MaskItem.is(Source) ? Source.copyWithCount(1) : Scp035MaskItem.fresh();
        Placed.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        if (!World.addFreshEntity(Placed)) {
            return null;
        }
        if (Audible) {
            World.playSound(null, Spot.x, Spot.y, Spot.z, Scp035Registry.SetDown.get(), SoundSource.BLOCKS, 0.8f, 0.92f + World.random.nextFloat() * 0.12f);
        }
        return Placed;
    }

    public ItemStack stack() {
        return Stack.isEmpty() ? Scp035MaskItem.fresh() : Stack.copy();
    }

    public Vec3 gazePoint() {
        return position().add(0.0, FaceLift, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    public void tick() {
        super.tick();
        settle();
        if (level().isClientSide) {
            sparkle();
            return;
        }
        if (level() instanceof ServerLevel World && (tickCount + getId()) % Scp035Lure.NpcInterval == 0) {
            Scp035Lure.drawPeople(World, gazePoint(), this);
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

    private void sparkle() {
        RandomSource Random = level().random;
        if (Random.nextInt(GlintChance) == 0) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Reach = Random.nextDouble() * 0.16;
            level().addParticle(Scp035Registry.Glint.get(), getX() + Math.cos(Angle) * Reach, getY() + FaceLift + 0.02, getZ() + Math.sin(Angle) * Reach, 0.0, 0.0, 0.0);
        }
        if (Random.nextInt(WeepChance) == 0) {
            double Yaw = Math.toRadians(getYRot());
            double Side = Random.nextBoolean() ? EyeSpread : -EyeSpread;
            double X = getX() + Math.cos(Yaw) * Side;
            double Z = getZ() + Math.sin(Yaw) * Side;
            level().addParticle(Scp035Registry.Weep.get(), X, getY() + FaceLift, Z, 0.0, 0.0, 0.0);
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
        return Scp035Possession.touched(this, User);
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
        return Scp035MaskItem.fresh();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        Stack = Tag.contains("Item") ? ItemStack.parseOptional(registryAccess(), Tag.getCompound("Item")) : ItemStack.EMPTY;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        if (!Stack.isEmpty()) {
            Tag.put("Item", Stack.save(registryAccess()));
        }
    }
}
