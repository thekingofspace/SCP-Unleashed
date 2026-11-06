package net.rai.scp_unleashed.scp5001.event.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.Scp5001Event;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventConfig;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class Scp5001Vessel extends Entity implements GeoEntity {
    public static final byte ModeIdle = 0;
    public static final byte ModeRise = 1;
    public static final byte ModeStrain = 2;
    public static final byte ModePinned = 3;
    public static final byte ModeBurst = 4;
    public static final byte ModeSettle = 5;
    public static final int BurstTicks = 60;
    public static final double CentreHeight = 7.0;
    public static final double HullRadius = 1.9;
    public static final int HumEvery = 100;
    public static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation Rise = RawAnimation.begin().thenLoop("rise");
    public static final RawAnimation Strain = RawAnimation.begin().thenLoop("strain");
    public static final RawAnimation Pinned = RawAnimation.begin().thenLoop("pinned");
    public static final RawAnimation Burst = RawAnimation.begin().thenPlayAndHold("burst");
    public static final DustParticleOptions FleshLight = new DustParticleOptions(new Vector3f(0.85f, 0.25f, 0.22f), 2.5f);
    public static final DustParticleOptions FieldLight = new DustParticleOptions(new Vector3f(0.61f, 0.55f, 0.94f), 1.6f);
    private static final EntityDataAccessor<Byte> Mode = SynchedEntityData.defineId(Scp5001Vessel.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> Serial = SynchedEntityData.defineId(Scp5001Vessel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> Lift = SynchedEntityData.defineId(Scp5001Vessel.class, EntityDataSerializers.FLOAT);
    public static Consumer<Scp5001Vessel> ClientTick = Vessel -> {
    };
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Bursting;
    public int SeenSerial;
    public int WaveStart = -1000;

    public Scp5001Vessel(EntityType<? extends Scp5001Vessel> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(Mode, ModeIdle);
        Builder.define(Serial, 0);
        Builder.define(Lift, 0.0f);
    }

    public byte mode() {
        return entityData.get(Mode);
    }

    public int serial() {
        return entityData.get(Serial);
    }

    public float lift() {
        return entityData.get(Lift);
    }

    public Vec3 centre() {
        return position().add(0.0, CentreHeight, 0.0);
    }

    public void pulse(int Value) {
        entityData.set(Serial, Value);
    }

    public void burst() {
        if (mode() != ModeBurst) {
            entityData.set(Mode, ModeBurst);
            Bursting = 0;
        }
    }

    public static Vec3 restingSpot(BlockPos Core, float Lifted) {
        double Height = 1.0 + Scp5001EventConfig.hoverHeight() + Lifted * Scp5001EventConfig.riseHeight();
        return new Vec3(Core.getX() + 0.5, Core.getY() + Height, Core.getZ() + 0.5);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            ClientTick.accept(this);
            return;
        }
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (mode() == ModeBurst) {
            Bursting++;
            if (Bursting % 4 == 0) {
                World.sendParticles(FleshLight, getX(), getY() + CentreHeight, getZ(), 30, 3.0, 5.0, 3.0, 0.05);
            }
            if (Bursting >= BurstTicks) {
                discard();
            }
            return;
        }
        Scp5001State State = Scp5001State.get(World.getServer());
        if (World != World.getServer().overworld() || !Scp5001Event.mine(State.phase()) || !State.located()) {
            discard();
            return;
        }
        Vec3 Target = restingSpot(State.core(), State.lift());
        setPos(Target);
        entityData.set(Lift, State.lift());
        entityData.set(Mode, modeFor(State, World.getGameTime()));
        if (tickCount % HumEvery == 0) {
            World.playSound(null, getX(), getY() + CentreHeight, getZ(), Scp5001EventRegistry.Hum.get(), SoundSource.AMBIENT, 4.0f, mode() == ModeIdle ? 0.8f : 1.0f);
        }
        if (mode() != ModeIdle && tickCount % 5 == 0) {
            World.sendParticles(FieldLight, getX(), getY() + CentreHeight, getZ(), 4, 2.2, 5.0, 2.2, 0.01);
        }
    }

    private static byte modeFor(Scp5001State State, long Now) {
        return switch (State.phase()) {
            case Unlocked -> ModeRise;
            case Rising -> State.lift() > 0.35f || State.overdriven() > 0 ? ModeStrain : ModeRise;
            case Overdrive, Purging -> ModePinned;
            case Resetting -> ModeSettle;
            default -> ModeIdle;
        };
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
    public boolean ignoreExplosion(net.minecraft.world.level.Explosion Blast) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 256.0 * 256.0;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(6.0, 2.0, 6.0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 10, Animated -> Animated.setAndContinue(switch (mode()) {
            case ModeRise -> Rise;
            case ModeStrain -> Strain;
            case ModePinned -> Pinned;
            case ModeBurst -> Burst;
            default -> Idle;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    public void playBurstEffects(ServerLevel World) {
        World.sendParticles(ParticleTypes.FLASH, getX(), getY() + CentreHeight, getZ(), 3, 1.0, 2.0, 1.0, 0.0);
        World.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + CentreHeight, getZ(), 2, 1.0, 3.0, 1.0, 0.0);
        World.sendParticles(FleshLight, getX(), getY() + CentreHeight, getZ(), 200, 8.0, 8.0, 8.0, 0.2);
    }
}
