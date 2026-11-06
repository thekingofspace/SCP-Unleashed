package net.rai.scp_unleashed.z0002;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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

public class Z0002Shrine extends Entity implements GeoEntity {
    public static final float Width = 5.0f;
    public static final float Height = 12.0f;
    public static final float EyeHeight = 1.5f;
    public static final double SinkDepth = 11.5;
    public static final double RenderReach = 160.0;
    public static final double CullReach = 5.0;
    public static final int CheckEvery = 100;
    public static final int BreathEvery = 80;
    public static final double VentSide = 3.0;
    public static final double VentHeight = 1.8;
    public static final double VentForward = 0.6;
    public static final double LanternSide = 3.25;
    public static final double LanternForward = 3.4;
    public static final double LanternHeight = 1.5;
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final EntityDataAccessor<Long> RiseStartData = SynchedEntityData.defineId(Z0002Shrine.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> RiseTicksData = SynchedEntityData.defineId(Z0002Shrine.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> SinkStartData = SynchedEntityData.defineId(Z0002Shrine.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> SinkTicksData = SynchedEntityData.defineId(Z0002Shrine.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID DomainId;

    public Z0002Shrine(EntityType<? extends Z0002Shrine> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(RiseStartData, 0L);
        Builder.define(RiseTicksData, 0);
        Builder.define(SinkStartData, -1L);
        Builder.define(SinkTicksData, 0);
    }

    public void rise(long Now, int Ticks) {
        entityData.set(RiseStartData, Now);
        entityData.set(RiseTicksData, Ticks);
        entityData.set(SinkStartData, -1L);
    }

    public void sink(long Now, int Ticks) {
        entityData.set(SinkStartData, Now);
        entityData.set(SinkTicksData, Ticks);
    }

    public static double ease(double Value) {
        double Clamped = Mth.clamp(Value, 0.0, 1.0);
        return Clamped * Clamped * (3.0 - 2.0 * Clamped);
    }

    public double offset(float PartialTick) {
        double Time = level().getGameTime() + PartialTick;
        long SinkStart = entityData.get(SinkStartData);
        if (SinkStart >= 0L) {
            int Ticks = Math.max(1, entityData.get(SinkTicksData));
            return -SinkDepth * ease((Time - SinkStart) / Ticks);
        }
        int Ticks = entityData.get(RiseTicksData);
        if (Ticks <= 0) {
            return 0.0;
        }
        return -SinkDepth * (1.0 - ease((Time - entityData.get(RiseStartData)) / Ticks));
    }

    public void bind(UUID Domain) {
        DomainId = Domain;
    }

    @Nullable
    public UUID domainId() {
        return DomainId;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            vapour();
            return;
        }
        if (tickCount % CheckEvery == 0 && level() instanceof ServerLevel World && !Z0002Domains.shrineValid(World, this)) {
            discard();
            return;
        }
        if (tickCount % BreathEvery == 0 && offset(0.0f) > -0.05) {
            level().playSound(null, getX(), getY() + 2.0, getZ(), Z0002Registry.Breath.get(), SoundSource.HOSTILE, 1.2f, 0.9f + random.nextFloat() * 0.1f);
        }
    }

    public Vec3 local(double Side, double Up, double Forward) {
        float Yaw = getYRot() * Mth.DEG_TO_RAD;
        double FaceX = -Mth.sin(Yaw);
        double FaceZ = Mth.cos(Yaw);
        return new Vec3(getX() + FaceX * Forward + FaceZ * Side, getY() + Up, getZ() + FaceZ * Forward - FaceX * Side);
    }

    private void vapour() {
        if (offset(0.0f) < -0.05) {
            return;
        }
        RandomSource Random = random;
        if (tickCount % 3 == 0) {
            for (int Side = -1; Side <= 1; Side += 2) {
                Vec3 Vent = local(Side * VentSide, VentHeight + Random.nextDouble() * 0.8, VentForward + (Random.nextDouble() - 0.5) * 1.6);
                Vec3 Drift = local(Side * 0.04, 0.0, 0.0).subtract(position());
                level().addParticle(Z0002Registry.Vapour.get(), Vent.x, Vent.y, Vent.z, Drift.x, 0.012 + Random.nextDouble() * 0.01, Drift.z);
            }
        }
        if (tickCount % 9 == 0) {
            for (int Side = -1; Side <= 1; Side += 2) {
                Vec3 Lamp = local(Side * LanternSide, LanternHeight + Random.nextDouble() * 0.3, LanternForward);
                if (Random.nextFloat() < 0.5f) {
                    level().addParticle(Z0002Registry.Spark.get(), Lamp.x, Lamp.y, Lamp.z, (Random.nextDouble() - 0.5) * 0.01, 0.02, (Random.nextDouble() - 0.5) * 0.01);
                }
            }
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
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
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < RenderReach * RenderReach;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(CullReach, 2.0, CullReach);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
        DomainId = Saved.hasUUID("Domain") ? Saved.getUUID("Domain") : null;
        entityData.set(RiseStartData, Saved.getLong("RiseStart"));
        entityData.set(RiseTicksData, Saved.getInt("RiseTicks"));
        entityData.set(SinkStartData, Saved.contains("SinkStart") ? Saved.getLong("SinkStart") : -1L);
        entityData.set(SinkTicksData, Saved.getInt("SinkTicks"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
        if (DomainId != null) {
            Saved.putUUID("Domain", DomainId);
        }
        Saved.putLong("RiseStart", entityData.get(RiseStartData));
        Saved.putInt("RiseTicks", entityData.get(RiseTicksData));
        Saved.putLong("SinkStart", entityData.get(SinkStartData));
        Saved.putInt("SinkTicks", entityData.get(SinkTicksData));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "idle", 0, State -> State.setAndContinue(IdleLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
