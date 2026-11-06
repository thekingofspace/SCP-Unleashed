package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.motion.ActionClock;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class Scp354Grasp extends Entity implements GeoEntity, IEntityWithComplexSpawn, ActionClock.Owner {
    public static final float Width = 0.8f;
    public static final float Height = 2.0f;
    public static final int WrapTick = 8;
    public static final int PullTick = 18;
    public static final int PullTicks = 26;
    public static final int KillTick = 46;
    public static final int RetryEvery = 5;
    public static final int RetryLimit = 30;
    public static final int ReleaseTicks = 26;
    public static final double SnapDistance = 1.2;
    public static final double Follow = 0.7;
    public static final float Lethal = 100000.0f;
    public static final double SinkSpeed = 0.06;
    public static final float MinGirth = 0.6f;
    public static final byte StageRise = 0;
    public static final byte StageWrap = 1;
    public static final byte StagePull = 2;
    public static final byte StageRelease = 3;
    private static final EntityDataAccessor<Byte> StageData = SynchedEntityData.defineId(Scp354Grasp.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> StageStartData = SynchedEntityData.defineId(Scp354Grasp.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GirthData = SynchedEntityData.defineId(Scp354Grasp.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> VictimData = SynchedEntityData.defineId(Scp354Grasp.class, EntityDataSerializers.INT);
    private static final RawAnimation RisePlay = RawAnimation.begin().thenPlayAndHold("rise");
    private static final RawAnimation WrapPlay = RawAnimation.begin().thenPlayAndHold("wrap");
    private static final RawAnimation PullLoop = RawAnimation.begin().thenLoop("pull");
    private static final RawAnimation ReleasePlay = RawAnimation.begin().thenPlayAndHold("release");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ActionClock Clock = new ActionClock();
    @Nullable
    private UUID VictimId;
    private Vec3 Start = Vec3.ZERO;
    private double FloorY;
    private double SurfaceY;
    private int Age;
    private int KillAt = -1;
    private int ReleaseAt = -1;
    private boolean WasNoAi;
    private int TickOrigin;
    private float RenderPartial;

    public Scp354Grasp(EntityType<? extends Scp354Grasp> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(StageData, StageRise);
        Builder.define(StageStartData, 0);
        Builder.define(GirthData, MinGirth);
        Builder.define(VictimData, -1);
    }

    public void setup(LivingEntity Victim, Vec3 Point, double Floor) {
        VictimId = Victim.getUUID();
        Start = Point;
        FloorY = Floor;
        SurfaceY = Point.y;
        if (level() instanceof ServerLevel World) {
            BlockPos Cursor = BlockPos.containing(Point.x, Floor + 0.5, Point.z);
            while (Scp354Fluid.isRed(World.getFluidState(Cursor.above())) && Cursor.getY() < World.getMaxBuildHeight()) {
                Cursor = Cursor.above();
            }
            SurfaceY = Cursor.getY() + Scp354Fluid.SurfaceHeight;
        }
        entityData.set(GirthData, Math.max(MinGirth, Math.max(Victim.getBbWidth(), Victim.getBbHeight() / 3.0f)));
        entityData.set(VictimData, Victim.getId());
        setPos(Point.x, Point.y, Point.z);
        setYRot(Victim.getRandom().nextFloat() * 360.0f);
        yRotO = getYRot();
    }

    public void rememberAi(boolean NoAi) {
        WasNoAi = NoAi;
    }

    public boolean holds(Entity Target) {
        return Target.getUUID().equals(VictimId) && stage() != StageRelease;
    }

    public byte stage() {
        return entityData.get(StageData);
    }

    public float girth() {
        return entityData.get(GirthData);
    }

    @Nullable
    public Entity victim() {
        int Id = entityData.get(VictimData);
        return Id < 0 ? null : level().getEntity(Id);
    }

    public double stageElapsed() {
        if (!level().isClientSide) {
            return Math.max(0, tickCount - entityData.get(StageStartData));
        }
        return Clock.elapsed(tickCount + RenderPartial);
    }

    @Override
    public ActionClock actionClock() {
        return Clock;
    }

    private void syncClock() {
        byte Stage = stage();
        Clock.begin((byte) (Stage + 1), stageClip(), tickCount, ActionClock.already(Clock.fresh(), tickCount, TickOrigin, entityData.get(StageStartData)));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (level().isClientSide && (StageData.equals(Key) || StageStartData.equals(Key))) {
            syncClock();
        }
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    private void setStage(byte Stage) {
        entityData.set(StageStartData, tickCount);
        entityData.set(StageData, Stage);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
        syncClock();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel World)) {
            Clock.settle();
            return;
        }
        Age++;
        if (stage() == StageRelease) {
            setPos(getX(), getY() - SinkSpeed, getZ());
            if (Age - ReleaseAt >= ReleaseTicks) {
                discard();
            }
            return;
        }
        LivingEntity Victim = VictimId != null && World.getEntity(VictimId) instanceof LivingEntity Found ? Found : null;
        if (Victim == null || Victim.isRemoved() || Victim.level() != World) {
            if (VictimId != null) {
                Scp354Grab.forget(VictimId);
            }
            release(World, null, false);
            return;
        }
        if (Victim.isDeadOrDying()) {
            release(World, Victim, false);
            return;
        }
        if (Victim instanceof Player User && (User.isCreative() || User.isSpectator()) || !Targeting.isTargetable(Victim)) {
            release(World, Victim, true);
            return;
        }
        if (KillAt >= 0) {
            Scp354Grab.hold(Victim, new Vec3(Start.x, FloorY, Start.z));
            setPos(Start.x, FloorY, Start.z);
            int Since = Age - KillAt;
            if (Since >= RetryLimit) {
                release(World, Victim, true);
            } else if (Since % RetryEvery == 0) {
                strike(World, Victim);
            }
            return;
        }
        double Y = Start.y;
        if (Age >= PullTick) {
            double Share = Mth.clamp((Age - PullTick) / (double) PullTicks, 0.0, 1.0);
            double Eased = Share * Share * (3.0 - 2.0 * Share);
            Y = Mth.lerp(Eased, Start.y, FloorY);
        }
        Vec3 Point = new Vec3(Start.x, Y, Start.z);
        Scp354Grab.hold(Victim, Point);
        setPos(Point.x, Point.y, Point.z);
        effects(World, Victim);
        if (Age == WrapTick) {
            setStage(StageWrap);
        } else if (Age == PullTick) {
            setStage(StagePull);
        } else if (Age >= KillTick) {
            strike(World, Victim);
        }
    }

    private void effects(ServerLevel World, LivingEntity Victim) {
        if (Age == 1) {
            Scp354Sounds.play(World, new Vec3(Start.x, SurfaceY, Start.z), Scp354Registry.SplashLayers, 1.2f, 0.85f + random.nextFloat() * 0.15f);
            World.sendParticles(Scp354Registry.Splash.get(), Start.x, SurfaceY + 0.05, Start.z, 26, 0.35, 0.05, 0.35, 0.22);
        }
        if (Age == WrapTick) {
            Scp354Sounds.play(World, Victim.position(), Scp354Registry.CoilLayers, 1.0f, 0.8f + random.nextFloat() * 0.15f);
        }
        if (Age == PullTick) {
            Scp354Sounds.play(World, Victim.position(), Scp354Registry.PullLayers, 1.1f, 0.85f + random.nextFloat() * 0.1f);
        }
        if (Age > PullTick && Age % 3 == 0) {
            World.sendParticles(Scp354Registry.Bubble.get(), Victim.getX(), Math.min(SurfaceY - 0.1, Victim.getY() + Victim.getBbHeight() * 0.6), Victim.getZ(), 4,
                    Victim.getBbWidth() * 0.4, 0.3, Victim.getBbWidth() * 0.4, 0.0);
        }
        if (Age > 1 && Age < PullTick + 8 && Age % 2 == 0) {
            World.sendParticles(Scp354Registry.Ripple.get(), Start.x, SurfaceY + 0.012, Start.z, 1, 0.2, 0.0, 0.2, 0.0);
        }
    }

    private void strike(ServerLevel World, LivingEntity Victim) {
        if (KillAt < 0) {
            KillAt = Age;
            Scp354Sounds.play(World, new Vec3(Start.x, SurfaceY, Start.z), Scp354Registry.GurgleLayers, 1.1f, 0.7f + random.nextFloat() * 0.1f);
            World.sendParticles(Scp354Registry.Bubble.get(), Start.x, SurfaceY - 0.3, Start.z, 18, 0.4, 0.2, 0.4, 0.0);
        }
        boolean Controlled = Victim instanceof ServerPlayer User && ControlSessions.isControlling(User);
        Victim.invulnerableTime = 0;
        Victim.hurt(World.damageSources().source(Scp354Registry.TakenDamage), Math.max(Lethal, Victim.getMaxHealth() * 10.0f));
        if (Controlled) {
            release(World, Victim, true);
        }
    }

    private void release(ServerLevel World, @Nullable LivingEntity Victim, boolean Spare) {
        if (Victim != null) {
            Scp354Grab.release(Victim, Spare);
            if (Victim instanceof Mob Body && Victim.isAlive()) {
                Body.setNoAi(WasNoAi);
            }
        }
        ReleaseAt = Age;
        setStage(StageRelease);
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1.5, 3.5, 1.5);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp354StageController<>(this, "grasp", Scp354Grasp::stageClip, Scp354Grasp::stageElapsed, Event -> {
            Event.getController().transitionLength(0);
            return switch (stage()) {
                case StageWrap -> Event.setAndContinue(WrapPlay);
                case StagePull -> Event.setAndContinue(PullLoop);
                case StageRelease -> Event.setAndContinue(ReleasePlay);
                default -> Event.setAndContinue(RisePlay);
            };
        }));
    }

    @Nullable
    public String stageClip() {
        return switch (stage()) {
            case StageWrap -> "wrap";
            case StageRelease -> "release";
            case StageRise -> "rise";
            default -> null;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
