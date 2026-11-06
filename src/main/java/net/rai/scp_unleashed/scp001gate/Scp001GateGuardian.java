package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp001gate.network.Scp001GateStrikePayload;
import net.rai.scp_unleashed.scp001gate.network.Scp001GateWarnPayload;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp001GateGuardian extends Entity implements GeoEntity {
    public static final float Width = 5.0f;
    public static final float Height = 22.0f;
    public static final float RenderScale = 3.3f;
    public static final int StrikeImpact = 8;
    public static final int StrikeLength = 32;
    public static final int StrikeCooldown = 6;
    public static final int RepeatTicks = 180;
    public static final int ReentryTicks = 100;
    public static final int ForgetTicks = 2400;
    public static final int Layout = 2;
    public static final int AvoidEvery = 5;
    public static final int CrackleEvery = 70;
    public static final float TurnSpeed = 18.0f;
    public static final float SettleSpeed = 3.0f;
    public static final double HeadHeight = 19.0;
    public static final int CalmTicks = 600;
    private static final EntityDataAccessor<Float> FacingData = SynchedEntityData.defineId(Scp001GateGuardian.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> GazeData = SynchedEntityData.defineId(Scp001GateGuardian.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> StrikeData = SynchedEntityData.defineId(Scp001GateGuardian.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Vector3f> AimData = SynchedEntityData.defineId(Scp001GateGuardian.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> AlertData = SynchedEntityData.defineId(Scp001GateGuardian.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation Alert = RawAnimation.begin().thenLoop("alert");
    private static final RawAnimation Swing = RawAnimation.begin().thenPlay("strike");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Long> Warned = new HashMap<>();
    private final Map<UUID, Watch> Watches = new HashMap<>();
    private BlockPos Centre = BlockPos.ZERO;
    private float Outward;
    private Vec3 Post = Vec3.ZERO;
    private boolean Posted;
    private boolean FromSite;
    @Nullable
    private UUID Locked;
    private int StrikeAge = -1;
    private int Cooldown;
    private int Kills;
    private float FacingO;
    private float GazeYaw;
    private float GazePitch;
    private float GazeYawO;
    private float GazePitchO;
    private long SeenStrike = -1L;
    private long StrikeClock = -1000L;
    private int Calm;

    public Scp001GateGuardian(EntityType<? extends Scp001GateGuardian> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        noCulling = true;
    }

    @Nullable
    public static Scp001GateGuardian post(ServerLevel World, BlockPos Centre, Direction Front, Vec3 Post) {
        return post(World, Centre, Front, Post, false);
    }

    @Nullable
    public static Scp001GateGuardian post(ServerLevel World, BlockPos Centre, Direction Front, Vec3 Post, boolean Fresh) {
        Scp001GateGuardian Made = Scp001GateRegistry.GuardianType.get().create(World);
        if (Made == null) {
            return null;
        }
        Made.station(Centre, Front.toYRot(), Post);
        Made.FromSite = true;
        if (Fresh) {
            Made.calm(CalmTicks);
        }
        World.addFreshEntity(Made);
        return Made;
    }

    public void calm(int Ticks) {
        Calm = Math.max(0, Ticks);
    }

    public int calm() {
        return Calm;
    }

    public float outward() {
        return Outward;
    }

    public void station(BlockPos Gate, float Yaw, Vec3 Spot) {
        Centre = Gate.immutable();
        Outward = Yaw;
        Post = Spot;
        Posted = true;
        moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        entityData.set(FacingData, Yaw);
        FacingO = Yaw;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(FacingData, 0.0f);
        Builder.define(GazeData, -1);
        Builder.define(StrikeData, -1000L);
        Builder.define(AimData, new Vector3f());
        Builder.define(AlertData, false);
    }

    public BlockPos centre() {
        return Centre;
    }

    public float facing() {
        return entityData.get(FacingData);
    }

    public float facing(float Partial) {
        return Mth.rotLerp(Partial, FacingO, facing());
    }

    public int gaze() {
        return entityData.get(GazeData);
    }

    public boolean alert() {
        return entityData.get(AlertData);
    }

    public Vec3 aim() {
        Vector3f Value = entityData.get(AimData);
        return new Vec3(Value.x, Value.y, Value.z);
    }

    public long strikeStart() {
        return entityData.get(StrikeData);
    }

    public float strikeTime(float Partial) {
        return (float) (level().getGameTime() - strikeStart()) + Partial;
    }

    public boolean striking() {
        float Age = strikeTime(0.0f);
        return Age >= 0.0f && Age < StrikeLength;
    }

    public int kills() {
        return Kills;
    }

    public float gazeYaw(float Partial) {
        return Mth.rotLerp(Partial, GazeYawO, GazeYaw);
    }

    public float gazePitch(float Partial) {
        return Mth.lerp(Partial, GazePitchO, GazePitch);
    }

    private static final class Watch {
        int Tier = -1;
        int Deepest = -1;
        int Variant = -1;
        int Titles;
        long Shown = -100000L;
        long Left = -100000L;
        long Seen = -100000L;
        boolean Inside;
    }

    public int titles(Player User) {
        Watch State = Watches.get(User.getUUID());
        return State == null ? 0 : State.Titles;
    }

    public static Vec3 post(BlockPos Gate, Direction Front) {
        return new Vec3(Gate.getX() + 0.5 + Front.getStepX() * Scp001GatePlan.GuardianAhead, Gate.getY() + Scp001GatePlan.GuardianLift,
                Gate.getZ() + 0.5 + Front.getStepZ() * Scp001GatePlan.GuardianAhead);
    }

    public boolean warned(Player User, long Since) {
        Long When = Warned.get(User.getUUID());
        return When != null && When >= Since;
    }

    @Override
    public void tick() {
        FacingO = facing();
        GazeYawO = GazeYaw;
        GazePitchO = GazePitch;
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (!Posted) {
            station(blockPosition().below((int) Scp001GatePlan.GuardianLift), getYRot(), position());
        }
        if (position().distanceToSqr(Post) > 1.0e-4) {
            setPos(Post.x, Post.y, Post.z);
        }
        setDeltaMovement(Vec3.ZERO);
        Scp001GateZones.mark(World, getId(), Centre);
        serverTick(World);
    }

    @Override
    public void move(MoverType Type, Vec3 Motion) {
    }

    @Override
    public void remove(RemovalReason Reason) {
        Scp001GateZones.clear(level(), getId());
        super.remove(Reason);
    }

    private void serverTick(ServerLevel World) {
        AABB Area = new AABB(Centre.getX() + 0.5 - Scp001GateZones.AvoidRadius - 16.0, Centre.getY() - Scp001GateZones.Below, Centre.getZ() + 0.5 - Scp001GateZones.AvoidRadius - 16.0,
                Centre.getX() + 0.5 + Scp001GateZones.AvoidRadius + 16.0, Centre.getY() + Scp001GateZones.Above, Centre.getZ() + 0.5 + Scp001GateZones.AvoidRadius + 16.0);
        List<Entity> Around = World.getEntities(this, Area, Body -> !Body.isRemoved());
        List<Entity> Doomed = new ArrayList<>();
        boolean Watching = false;
        boolean Fence = tickCount % AvoidEvery == 0;
        boolean Settling = Calm > 0;
        if (Settling) {
            Calm--;
        }
        for (Entity Body : Around) {
            Vec3 At = Body.position();
            if (!Scp001GateZones.band(Centre, At.y)) {
                continue;
            }
            double Distance = Scp001GateZones.horizontal(Centre, At.x, At.z);
            if (Fence && Distance <= Scp001GateZones.AvoidRadius + 16.0 && Scp001GateAvoid.avoids(Body)) {
                Scp001GateAvoid.fence(Body, Centre, Distance);
            }
            if (Distance > Scp001GateZones.WarnRadius) {
                continue;
            }
            if (Distance <= Scp001GateRepel.SafeRing && Scp001GateRepel.tick(World, Body, Centre, Distance)) {
                continue;
            }
            if (Distance <= Scp001GateZones.KillRadius && Scp001GateStrike.burnable(Body)) {
                Scp001GateStrike.burn(World, Body);
                continue;
            }
            if (Body instanceof ServerPlayer User && !User.isSpectator()) {
                warn(World, User, Distance);
            }
            if (Body instanceof LivingEntity && Body.isAlive() && !(Body instanceof Player User0 && User0.isSpectator())) {
                Watching = true;
            }
            if (Distance <= Scp001GateZones.KillRadius && Scp001GateStrike.eligible(Body) && Body.getVehicle() == null) {
                if (Settling && Scp001GateStrike.quiet(Body)) {
                    continue;
                }
                Doomed.add(Body);
            }
        }
        entityData.set(AlertData, Watching);
        Doomed.sort(Comparator.comparingDouble((Entity Body) -> Body instanceof Player ? 0.0 : 1.0).thenComparingDouble(Body -> Body.distanceToSqr(this)));
        strikeTick(World, Doomed);
        Entity Focus = target(World, Doomed);
        entityData.set(GazeData, Focus != null ? Focus.getId() : -1);
        float Goal = Outward;
        boolean Turning = false;
        if (StrikeAge >= 0 && StrikeAge <= StrikeImpact + 4) {
            Vec3 Target = aim();
            Goal = yawTo(Target);
            Turning = true;
        } else if (Focus != null) {
            Goal = yawTo(Focus.position());
            Turning = true;
        }
        float Now = facing();
        float Delta = Mth.wrapDegrees(Goal - Now);
        float Speed = Turning ? TurnSpeed : SettleSpeed;
        float Next = Now + Mth.clamp(Delta, -Speed, Speed);
        entityData.set(FacingData, Next);
        setYRot(Next);
        if (tickCount % CrackleEvery == 0) {
            World.playSound(null, getX(), getY() + 14.0, getZ(), Scp001GateRegistry.Crackle.get(), SoundSource.HOSTILE, 4.0f, 0.7f + random.nextFloat() * 0.2f);
        }
        long Clock = World.getGameTime();
        for (Watch State : Watches.values()) {
            if (State.Inside && State.Seen != Clock) {
                State.Inside = false;
                State.Left = Clock;
            }
        }
        if (tickCount % 1200 == 0) {
            Warned.entrySet().removeIf(Entry -> Clock - Entry.getValue() > ForgetTicks);
            Watches.values().removeIf(State -> !State.Inside && Clock - State.Left > ForgetTicks);
        }
    }

    private float yawTo(Vec3 Target) {
        return (float) (Mth.atan2(Target.z - getZ(), Target.x - getX()) * Mth.RAD_TO_DEG) - 90.0f;
    }

    @Nullable
    private Entity target(ServerLevel World, List<Entity> Doomed) {
        if (Locked != null) {
            Entity Struck = World.getEntity(Locked);
            if (Struck != null && Struck.isAlive() && !Struck.isRemoved() && Scp001GateStrike.eligible(Struck)) {
                return Struck;
            }
        }
        return Doomed.isEmpty() ? null : Doomed.get(0);
    }

    private void warn(ServerLevel World, ServerPlayer User, double Distance) {
        int Tier = Distance <= Scp001GateZones.KillRadius ? Scp001GateZones.Tiers - 1 : Scp001GateZones.tier(Distance);
        long Now = World.getGameTime();
        Watch State = Watches.computeIfAbsent(User.getUUID(), Id -> new Watch());
        State.Seen = Now;
        if (!State.Inside) {
            State.Inside = true;
            if (Now - State.Left >= ReentryTicks) {
                State.Deepest = -1;
            }
        }
        boolean Deeper = Tier > State.Deepest;
        boolean Again = !Deeper && Now - State.Shown >= RepeatTicks;
        State.Tier = Tier;
        if (!Deeper && !Again) {
            return;
        }
        State.Deepest = Math.max(State.Deepest, Tier);
        State.Shown = Now;
        State.Titles++;
        State.Variant = (State.Variant + 1 + User.getRandom().nextInt(2)) % 3;
        Warned.put(User.getUUID(), Now);
        if (User instanceof FakePlayer || User.connection == null) {
            return;
        }
        float Closeness = (float) Mth.clamp((Scp001GateZones.WarnRadius - Distance) / (Scp001GateZones.WarnRadius - Scp001GateZones.KillRadius), 0.0, 1.0);
        PacketDistributor.sendToPlayer(User, new Scp001GateWarnPayload(Tier, State.Variant, Closeness));
        Vec3 From = User.getEyePosition().add(new Vec3(getX() - User.getX(), 0.0, getZ() - User.getZ()).normalize().scale(4.0));
        net.rai.scp_unleashed.archive.investigation.LeadPlanner.playTo(User, Scp001GateRegistry.Warn, From, 0.7f + Tier * 0.15f, 0.85f - Tier * 0.05f);
        net.rai.scp_unleashed.archive.investigation.LeadPlanner.playTo(User, Scp001GateRegistry.WarnLow, From, 0.6f + Tier * 0.1f, 0.8f);
    }

    private void strikeTick(ServerLevel World, List<Entity> Doomed) {
        if (Cooldown > 0) {
            Cooldown--;
        }
        if (StrikeAge >= 0) {
            StrikeAge++;
            if (StrikeAge == StrikeImpact) {
                impact(World);
            }
            if (StrikeAge >= StrikeImpact + StrikeCooldown) {
                StrikeAge = -1;
                Locked = null;
                Cooldown = 0;
            }
            return;
        }
        if (Doomed.isEmpty() || Cooldown > 0) {
            return;
        }
        Entity Target = Doomed.get(0);
        Locked = Target.getUUID();
        StrikeAge = 0;
        Vec3 At = Target.position();
        entityData.set(AimData, new Vector3f((float) At.x, (float) At.y, (float) At.z));
        entityData.set(StrikeData, World.getGameTime());
        World.playSound(null, getX(), getY() + 18.0, getZ(), Scp001GateRegistry.Raise.get(), SoundSource.HOSTILE, 5.0f, 0.9f);
    }

    public void strikeNow(ServerLevel World, Entity Target) {
        Locked = Target.getUUID();
        StrikeAge = StrikeImpact - 1;
        Vec3 At = Target.position();
        entityData.set(AimData, new Vector3f((float) At.x, (float) At.y, (float) At.z));
        entityData.set(StrikeData, World.getGameTime() - StrikeImpact + 1);
    }

    private void impact(ServerLevel World) {
        Entity Target = Locked != null ? World.getEntity(Locked) : null;
        Vec3 Point = aim();
        DamageSource Source = Scp001GateStrike.source(World, this);
        if (Target != null && Scp001GateStrike.eligible(Target) && Scp001GateZones.horizontal(Centre, Target.getX(), Target.getZ()) <= Scp001GateZones.WarnRadius + 4.0) {
            Point = Target.position();
            Entity Root = Target.getRootVehicle();
            Kills += Scp001GateStrike.smite(World, Root != Target && Scp001GateStrike.eligible(Root) ? Root : Target, Source);
        }
        AABB Blast = new AABB(Point, Point).inflate(Scp001GateStrike.ClusterRadius, 3.0, Scp001GateStrike.ClusterRadius);
        for (Entity Near : World.getEntities(this, Blast, Body -> Scp001GateStrike.eligible(Body) && Body.getVehicle() == null)) {
            if (Scp001GateZones.inKill(Centre, Near.position()) || Near.position().distanceTo(Point) <= 2.0) {
                Kills += Scp001GateStrike.smite(World, Near, Source);
            }
        }
        entityData.set(AimData, new Vector3f((float) Point.x, (float) Point.y, (float) Point.z));
        Scp001GateStrike.burst(World, Point, 2.0);
        World.sendParticles(ParticleTypes.EXPLOSION_EMITTER, Point.x, Point.y + 0.5, Point.z, 1, 0.0, 0.0, 0.0, 0.0);
        for (StrikeLayer Layer : StrikeLayer.values()) {
            World.playSound(null, Point.x, Point.y, Point.z, Layer.sound(), SoundSource.HOSTILE, Layer.Volume, Layer.Pitch);
        }
        PacketDistributor.sendToPlayersNear(World, null, Point.x, Point.y, Point.z, 160.0, new Scp001GateStrikePayload(getId(), (float) Point.x, (float) Point.y, (float) Point.z));
    }

    private enum StrikeLayer {
        Swing(5.0f, 0.8f), Impact(6.0f, 0.7f), Roar(6.0f, 0.9f);

        final float Volume;
        final float Pitch;

        StrikeLayer(float Volume, float Pitch) {
            this.Volume = Volume;
            this.Pitch = Pitch;
        }

        net.minecraft.sounds.SoundEvent sound() {
            return switch (this) {
                case Swing -> Scp001GateRegistry.StrikeSwing.get();
                case Impact -> Scp001GateRegistry.StrikeImpact.get();
                case Roar -> Scp001GateRegistry.StrikeFlame.get();
            };
        }
    }

    private void clientTick() {
        int Id = gaze();
        Entity Seen = Id >= 0 ? level().getEntity(Id) : null;
        float GoalYaw = 0.0f;
        float GoalPitch = 0.0f;
        if (striking()) {
            Vec3 Target = aim();
            GoalPitch = lookPitch(Target);
        } else if (Seen != null) {
            Vec3 Target = Seen.getEyePosition();
            float Absolute = (float) (Mth.atan2(Target.z - getZ(), Target.x - getX()) * Mth.RAD_TO_DEG) - 90.0f;
            GoalYaw = Mth.clamp(Mth.wrapDegrees(Absolute - facing()), -70.0f, 70.0f);
            GoalPitch = lookPitch(Target);
        }
        GazeYaw += Mth.clamp(Mth.wrapDegrees(GoalYaw - GazeYaw), -4.0f, 4.0f);
        GazePitch += Mth.clamp(GoalPitch - GazePitch, -3.0f, 3.0f);
        Scp001GateClientBridge.tick(this);
    }

    private float lookPitch(Vec3 Target) {
        double Dx = Target.x - getX();
        double Dz = Target.z - getZ();
        double Dy = Target.y - (getY() + HeadHeight);
        return Mth.clamp((float) (-Mth.atan2(Dy, Math.sqrt(Dx * Dx + Dz * Dz)) * Mth.RAD_TO_DEG), -40.0f, 50.0f);
    }

    public boolean newStrike() {
        long Start = strikeStart();
        if (Start != SeenStrike && striking()) {
            SeenStrike = Start;
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
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
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 512.0 * 512.0;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB Around = getBoundingBox().inflate(40.0, 14.0, 40.0).expandTowards(0.0, -Scp001GatePlan.GuardianLift, 0.0);
        if (striking()) {
            Vec3 Target = aim();
            Around = Around.minmax(new AABB(Target, Target).inflate(8.0));
        }
        return Around;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        if (Tag.contains("Centre")) {
            Centre = NbtUtils.readBlockPos(Tag, "Centre").orElse(blockPosition());
            Outward = Tag.getFloat("Outward");
            Post = new Vec3(Tag.getDouble("PostX"), Tag.getDouble("PostY"), Tag.getDouble("PostZ"));
            FromSite = Tag.getBoolean("Site") || Tag.getInt("Layout") < Layout && Post.y - Centre.getY() > 20.0;
            if (Tag.getInt("Layout") < Layout && FromSite) {
                Post = post(Centre, Direction.fromYRot(Outward));
                setPos(Post.x, Post.y, Post.z);
            }
            Posted = true;
            entityData.set(FacingData, Outward);
            Kills = Tag.getInt("Kills");
            Calm = Tag.getInt("Calm");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        if (!Posted) {
            return;
        }
        Tag.put("Centre", NbtUtils.writeBlockPos(Centre));
        Tag.putFloat("Outward", Outward);
        Tag.putDouble("PostX", Post.x);
        Tag.putDouble("PostY", Post.y);
        Tag.putDouble("PostZ", Post.z);
        Tag.putInt("Kills", Kills);
        Tag.putInt("Layout", Layout);
        Tag.putBoolean("Site", FromSite);
        if (Calm > 0) {
            Tag.putInt("Calm", Calm);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 4, State -> {
            long Start = strikeStart();
            if (striking()) {
                if (StrikeClock != Start) {
                    StrikeClock = Start;
                    State.getController().forceAnimationReset();
                }
                return State.setAndContinue(Swing);
            }
            return State.setAndContinue(alert() ? Alert : Idle);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    public static boolean pathCrosses(Mob Walker, BlockPos Gate) {
        Path Route = Walker.getNavigation().getPath();
        if (Route == null || Route.isDone()) {
            return false;
        }
        for (int Index = Route.getNextNodeIndex(); Index < Route.getNodeCount(); Index++) {
            Node Step = Route.getNode(Index);
            if (Scp001GateZones.within(Gate, new Vec3(Step.x + 0.5, Step.y, Step.z + 0.5), Scp001GateZones.KillRadius + 2.0)) {
                return true;
            }
        }
        return false;
    }
}
