package net.rai.scp_unleashed.scp001scarlet.king;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class ScarletThorn extends Entity implements GeoEntity {
    public static final float Width = 1.3f;
    public static final float Height = 2.8f;
    public static final int Waiting = 0;
    public static final int Up = 1;
    public static final int Sinking = 2;
    public static final int SinkTicks = 12;
    public static final float EruptDamage = 10.0f;
    public static final float WallDamage = 3.0f;
    private static final EntityDataAccessor<Integer> StateData = SynchedEntityData.defineId(ScarletThorn.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> StateTickData = SynchedEntityData.defineId(ScarletThorn.class, EntityDataSerializers.INT);
    private static final RawAnimation Telegraph = RawAnimation.begin().thenLoop("telegraph");
    private static final RawAnimation Erupt = RawAnimation.begin().thenPlayAndHold("erupt");
    private static final RawAnimation Sink = RawAnimation.begin().thenPlayAndHold("sink");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Fuse = 16;
    private int Life = 40;
    private boolean Wall;
    @Nullable
    private UUID Owner;
    private int Age;

    public ScarletThorn(EntityType<? extends ScarletThorn> Type, Level World) {
        super(Type, World);
        noPhysics = true;
    }

    public static ScarletThorn spawn(ServerLevel World, @Nullable Entity Caster, Vec3 At, int Fuse, int Life, boolean Wall) {
        ScarletThorn Made = new ScarletThorn(ScarletKingRegistry.ThornType.get(), World);
        Made.moveTo(At.x, At.y, At.z, World.random.nextFloat() * 360.0f, 0.0f);
        Made.Fuse = Math.max(1, Fuse);
        Made.Life = Math.max(1, Life);
        Made.Wall = Wall;
        Made.Owner = Caster == null ? null : Caster.getUUID();
        World.addFreshEntity(Made);
        return Made;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(StateData, Waiting);
        Builder.define(StateTickData, 0);
    }

    public int state() {
        return entityData.get(StateData);
    }

    public boolean wall() {
        return Wall;
    }

    private void state(int Value) {
        entityData.set(StateData, Value);
        entityData.set(StateTickData, tickCount);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Age++;
        if (Age < Fuse) {
            if (Age % 4 == 0) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, World.getBlockState(blockPosition().below())), getX(), getY() + 0.1, getZ(), 4, 0.4, 0.05, 0.4,
                        0.05);
                World.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getY() + 0.1, getZ(), 1, 0.3, 0.0, 0.3, 0.0);
            }
            return;
        }
        if (Age == Fuse) {
            state(Up);
            if (getId() % 3 == 0) {
                World.playSound(null, getX(), getY(), getZ(), ScarletKingRegistry.Thorn.get(), SoundSource.HOSTILE, 1.6f, 0.7f + random.nextFloat() * 0.2f);
            }
            World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 3, 0.3, 0.4, 0.3, 0.02);
            strike(World, EruptDamage, 0.6);
            return;
        }
        if (Age < Fuse + Life) {
            if (Wall && Age % 10 == 0) {
                strike(World, WallDamage, 0.2);
            }
            return;
        }
        if (Age == Fuse + Life) {
            state(Sinking);
        }
        if (Age >= Fuse + Life + SinkTicks) {
            discard();
        }
    }

    private void strike(ServerLevel World, float Damage, double Lift) {
        AABB Area = getBoundingBox().inflate(0.15, 0.2, 0.15);
        for (LivingEntity Victim : World.getEntitiesOfClass(LivingEntity.class, Area, Each -> Each.isAlive() && !(Each instanceof ScarletKing)
                && !Each.getTags().contains(ScarletKing.ShadeTag) && (Owner == null || !Each.getUUID().equals(Owner)))) {
            Entity Caster = Owner == null ? null : World.getEntity(Owner);
            DamageSource Source = Caster instanceof LivingEntity Living ? World.damageSources().mobAttack(Living) : World.damageSources().magic();
            if (Victim.hurt(Source, Damage)) {
                Vec3 Away = Victim.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                Away = Away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : Away.normalize().scale(0.35);
                Victim.setDeltaMovement(Victim.getDeltaMovement().add(Away.x, Lift, Away.z));
                Victim.hurtMarked = true;
            }
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return Wall && state() == Up;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 0, State -> {
            int Now = State.getAnimatable().state();
            return State.setAndContinue(Now == Up ? Erupt : Now == Sinking ? Sink : Telegraph);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
