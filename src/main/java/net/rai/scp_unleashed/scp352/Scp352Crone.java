package net.rai.scp_unleashed.scp352;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp352Crone extends Monster implements GeoEntity {
    public static final float LowWidth = 0.75f;
    public static final float LowHeight = 1.6f;
    public static final float LowEye = 1.38f;
    public static final float TallWidth = 0.9f;
    public static final float TallHeight = 2.9f;
    public static final float TallEye = 2.7f;
    public static final float RoostWidth = 1.1f;
    public static final float RoostHeight = 1.3f;
    public static final float RoostEye = 0.95f;
    public static final double MaxHealth = 200.0;
    public static final double Armour = 8.0;
    public static final double Toughness = 3.0;
    public static final double KnockbackResistance = 0.45;
    public static final double MoveSpeed = 0.4;
    public static final double WanderModifier = 0.32;
    public static final double DriftModifier = 0.38;
    public static final double StalkModifier = 0.6;
    public static final double RunModifier = 1.0;
    public static final double SenseRange = 32.0;
    public static final double TriggerRange = 14.0;
    public static final double HuntRange = 40.0;
    public static final double FollowRange = 48.0;
    public static final int ScanTicks = 10;
    public static final int LoseTicks = 200;
    public static final int RepathTicks = 4;
    public static final int DriftTicks = 20;
    public static final int StrollOdds = 100;
    public static final float KickDamage = 9.0f;
    public static final float SwipeDamage = 5.0f;
    public static final double KickReach = 2.4;
    public static final double SwipeReach = 3.0;
    public static final double ReachHeight = 3.0;
    public static final int KickTicks = 12;
    public static final int KickHit = 6;
    public static final int KickCooldown = 26;
    public static final int SwipeTicks = 10;
    public static final int SwipeHit = 5;
    public static final int SwipeCooldown = 20;
    public static final double SwipePush = 0.9;
    public static final double KickPush = 0.45;
    public static final double ShieldPush = 0.55;
    public static final int SeekMin = 3;
    public static final int SeekMax = 6;
    public static final int SeekLimit = 160;
    public static final double SeekArrive = 1.2;
    public static final int HealInterval = 40;
    public static final int SnoreMin = 70;
    public static final int SnoreMax = 150;
    public static final int DeathTicks = 50;
    public static final int MutterInterval = 240;
    public static final float SafeFall = 6.0f;
    public static final int[] CrackleTicks = {12, 18, 24};
    public static final int SquawkTick = 22;
    public static final int RouseSquawk = 4;
    public static final int Remembered = 16;
    public static final byte ActionNone = 0;
    public static final byte ActionKick = 1;
    public static final byte ActionSwipe = 2;
    public static final byte ActionLeap = 3;
    public static final byte ActionLand = 4;
    public static final double LeapMin = 6.0;
    public static final double LeapMax = 16.0;
    public static final double LeapRise = 4.0;
    public static final double LeapDrop = 8.0;
    public static final double ClimbMin = 1.6;
    public static final double ClimbMax = 5.0;
    public static final double ClimbReach = 8.0;
    public static final double ClosingRate = 0.12;
    public static final int GapWindow = 10;
    public static final double LeadTicks = 8.0;
    public static final double LeadCap = 4.0;
    public static final double LandShort = 1.4;
    public static final double ClimbShort = 0.6;
    public static final int LeapWindup = 6;
    public static final int LeapLaunch = 3;
    public static final int LeapAirMax = 50;
    public static final int LandTicks = 7;
    public static final int LeapCooldownMin = 100;
    public static final int LeapCooldownMax = 160;
    public static final int ClimbCooldown = 50;
    public static final int HuntLeapDelay = 20;
    public static final int PlanRetry = 10;
    public static final int StuckLimit = 20;
    public static final float LandDamage = 7.0f;
    public static final double LandReach = 2.2;
    public static final double LandPush = 0.7;
    public static final double LeapClipAir = 6.0;
    public static final double LeapClipLand = 15.0;
    public static final double LeapClipEnd = 22.0;

    private static final EntityDataAccessor<Byte> PhaseData = SynchedEntityData.defineId(Scp352Crone.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> PhaseStartData = SynchedEntityData.defineId(Scp352Crone.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp352Crone.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> ActionStartData = SynchedEntityData.defineId(Scp352Crone.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> LeapAirData = SynchedEntityData.defineId(Scp352Crone.class, EntityDataSerializers.INT);
    private static final RawAnimation LeapPlay = RawAnimation.begin().thenPlayAndHold("leap");
    private static final RawAnimation KickPlay = RawAnimation.begin().thenPlayAndHold("kick");
    private static final RawAnimation SwipePlay = RawAnimation.begin().thenPlayAndHold("swipe");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation DeathLowPlay = RawAnimation.begin().thenPlayAndHold("death_low");
    private static final RawAnimation DeathRestPlay = RawAnimation.begin().thenPlayAndHold("death_rest");
    private static final RawAnimation RestLoop = RawAnimation.begin().thenLoop("rest");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Scp352Rest Rest = new Scp352Rest();
    private final Deque<UUID> Counted = new ArrayDeque<>();
    private int Kills;
    private int RetargetTimer;
    private int RepathTimer;
    private int LostTicks;
    private int KickReady;
    private int SwipeReady;
    private int LeapReady;
    private int Leaps;
    private boolean Flying;
    private int FlightAge;
    private boolean ClimbLeap;
    @Nullable
    private Scp352Leap.Plan LeapPlan;
    private Vec3 LeapAim = Vec3.ZERO;
    private final double[] Gaps = new double[GapWindow];
    private int GapIndex;
    private int GapCount;
    private int StuckCount;
    @Nullable
    private UUID TrackedTarget;
    private Vec3 TrackedPos = Vec3.ZERO;
    private Vec3 TrackedVelocity = Vec3.ZERO;
    private int SnoreTimer = SnoreMin;
    private long PendingRest = -1L;
    @Nullable
    private UUID PendingTarget;
    @Nullable
    private BlockPos RestSpot;
    private int SizeClass = -1;
    private int DeathKind = -1;
    private Scp352Gait Gait = Scp352Gait.Idle;
    private double GaitPhase;
    private double GaitPhaseO;
    private double PaceSmooth;
    private int StillCount;
    private float BodyYawO;
    private float RenderPartial;
    @Nullable
    private AnimationController<Scp352Crone> BodyController;

    public Scp352Crone(EntityType<? extends Scp352Crone> Type, Level World) {
        super(Type, World);
        moveControl = new Scp352MoveControl(this);
        setPersistenceRequired();
        xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed).add(Attributes.ATTACK_DAMAGE, KickDamage).add(Attributes.FOLLOW_RANGE, FollowRange)
                .add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean refusesStasis(Entity Target) {
        return Target instanceof Scp352Crone Creature && Creature.phase() != Scp352Phase.Rest;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(PhaseData, (byte) Scp352Phase.Wander.ordinal());
        Builder.define(PhaseStartData, 0L);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0L);
        Builder.define(LeapAirData, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        GroundPathNavigation Navigation = new GroundPathNavigation(this, World);
        Navigation.setCanFloat(true);
        Navigation.getNodeEvaluator().setCanWalkOverFences(true);
        return Navigation;
    }

    public Scp352Phase phase() {
        return Scp352Phase.byId(entityData.get(PhaseData));
    }

    public long phaseAge() {
        return Math.max(0L, level().getGameTime() - entityData.get(PhaseStartData));
    }

    public double phaseElapsed() {
        return Math.max(0.0, level().getGameTime() - entityData.get(PhaseStartData) + (level().isClientSide ? RenderPartial : 0.0f));
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public long actionAge() {
        return Math.max(0L, level().getGameTime() - entityData.get(ActionStartData));
    }

    public double actionElapsed() {
        return Math.max(0.0, level().getGameTime() - entityData.get(ActionStartData) + (level().isClientSide ? RenderPartial : 0.0f));
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionKick -> "kick";
            case ActionSwipe -> "swipe";
            case ActionLeap, ActionLand -> "leap";
            default -> null;
        };
    }

    public int kills() {
        return Kills;
    }

    public Scp352Rest rest() {
        return Rest;
    }

    public long restLeft() {
        return Rest.left(level());
    }

    @Nullable
    public BlockPos restSpot() {
        return RestSpot;
    }

    public int sizeClass() {
        return phase().size(phaseAge());
    }

    public boolean isTall() {
        return sizeClass() == Scp352Phase.Tall;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose Stance) {
        return switch (sizeClass()) {
            case Scp352Phase.Tall -> EntityDimensions.scalable(TallWidth, TallHeight).withEyeHeight(TallEye);
            case Scp352Phase.Roost -> EntityDimensions.scalable(RoostWidth, RoostHeight).withEyeHeight(RoostEye);
            default -> EntityDimensions.scalable(LowWidth, LowHeight).withEyeHeight(LowEye);
        };
    }

    @Override
    public float maxUpStep() {
        return isTall() ? 1.6f : 1.0f;
    }

    private void setPhase(Scp352Phase Next) {
        entityData.set(PhaseData, (byte) Next.ordinal());
        entityData.set(PhaseStartData, level().getGameTime());
        entityData.set(ActionData, ActionNone);
        if (Next.timed() || Next == Scp352Phase.Rest) {
            halt();
        }
        refreshSize();
    }

    private void refreshSize() {
        if (isDeadOrDying()) {
            return;
        }
        int Current = sizeClass();
        if (Current != SizeClass) {
            SizeClass = Current;
            refreshDimensions();
        }
    }

    private void halt() {
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
        setSpeed(0.0f);
        setZza(0.0f);
        setXxa(0.0f);
    }

    public void beginTransform(@Nullable LivingEntity Target) {
        if (phase() != Scp352Phase.Wander) {
            return;
        }
        PendingTarget = Target == null ? null : Target.getUUID();
        setTarget(null);
        setPhase(Scp352Phase.Transform);
        playLayers(Scp352Registry.MutterLayers, 1.0f, 0.75f);
        playSound(Scp352Registry.Rustle.get(), 1.0f, 0.8f);
    }

    public void beginHunt(@Nullable LivingEntity Target) {
        if (phase() == Scp352Phase.Rest) {
            beginRouse(Target);
            return;
        }
        if (phase() == Scp352Phase.Wander) {
            beginTransform(Target);
            return;
        }
        enterHunt(Target);
    }

    private void enterHunt(@Nullable LivingEntity Target) {
        setPhase(Scp352Phase.Hunt);
        LostTicks = 0;
        RetargetTimer = 0;
        LeapReady = Math.max(LeapReady, HuntLeapDelay);
        forgetTrack();
        if (Target != null && Scp352Prey.prey(this, Target)) {
            setTarget(Target);
        }
        PendingTarget = null;
    }

    private void beginFold() {
        setTarget(null);
        setPhase(Scp352Phase.Fold);
        playSound(Scp352Registry.Rustle.get(), 1.0f, 0.9f);
        playSound(Scp352Registry.CrackleBone.get(), 0.6f, 1.2f);
    }

    public void beginRouse(@Nullable LivingEntity Target) {
        Rest.clear();
        PendingTarget = Target != null && Scp352Prey.prey(this, Target) ? Target.getUUID() : null;
        setPhase(Scp352Phase.Rouse);
        playLayers(Scp352Registry.CrackleLayers, 1.2f, 1.0f);
    }

    public void beginWake() {
        Rest.clear();
        setPhase(Scp352Phase.Wake);
        playSound(Scp352Registry.Wake.get(), 1.0f, 1.0f);
        playSound(Scp352Registry.Rustle.get(), 0.8f, 0.85f);
    }

    public void restNow(long Ticks) {
        PendingRest = Ticks;
        setTarget(null);
        if (sizeClass() == Scp352Phase.Tall) {
            beginSettle();
        } else {
            beginRest();
        }
    }

    private void beginSettle() {
        halt();
        setPhase(Scp352Phase.Settle);
        playSound(Scp352Registry.Settle.get(), 1.0f, 1.0f);
        playSound(Scp352Registry.Rustle.get(), 0.9f, 0.8f);
    }

    private void beginRest() {
        long Length = PendingRest >= 0L ? PendingRest : Scp352Rest.roll(random);
        PendingRest = -1L;
        Rest.begin(level(), Length);
        setPhase(Scp352Phase.Rest);
        SnoreTimer = SnoreMin + random.nextInt(SnoreMax - SnoreMin);
    }

    public void beginSeek(Vec3 Kill) {
        setTarget(null);
        RestSpot = restSpotNear(Kill);
        setPhase(Scp352Phase.Seek);
        getNavigation().moveTo(RestSpot.getX() + 0.5, RestSpot.getY(), RestSpot.getZ() + 0.5, StalkModifier);
        playLayers(Scp352Registry.MutterLayers, 1.0f, 1.1f);
    }

    private BlockPos restSpotNear(Vec3 Kill) {
        BlockPos Origin = BlockPos.containing(Kill);
        for (int Attempt = 0; Attempt < 16; Attempt++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Radius = SeekMin + random.nextDouble() * (SeekMax - SeekMin);
            BlockPos Column = Origin.offset(Mth.floor(Math.cos(Angle) * Radius), 0, Mth.floor(Math.sin(Angle) * Radius));
            for (int Dy = 3; Dy >= -3; Dy--) {
                BlockPos Candidate = Column.above(Dy);
                if (roomAt(Candidate)) {
                    return Candidate;
                }
            }
        }
        return blockPosition();
    }

    public boolean roomToRise() {
        AABB Box = AABB.ofSize(position().add(0.0, TallHeight * 0.5 + 0.01, 0.0), TallWidth, TallHeight, TallWidth);
        return level().noCollision(this, Box);
    }

    private boolean roomAt(BlockPos Pos) {
        Level World = level();
        if (!World.isLoaded(Pos) || !World.getBlockState(Pos.below()).isFaceSturdy(World, Pos.below(), Direction.UP)) {
            return false;
        }
        AABB Box = AABB.ofSize(Vec3.atBottomCenterOf(Pos).add(0.0, RoostHeight * 0.5, 0.0), RoostWidth, RoostHeight, RoostWidth);
        return World.noCollision(this, Box) && !World.containsAnyLiquid(Box);
    }

    public boolean claimKill(LivingEntity Fallen) {
        UUID Id = Fallen.getUUID();
        if (Counted.contains(Id)) {
            return false;
        }
        Counted.addLast(Id);
        while (Counted.size() > Remembered) {
            Counted.removeFirst();
        }
        Kills++;
        Scp352Phase Current = phase();
        if (Current == Scp352Phase.Hunt || Current == Scp352Phase.Transform || Current == Scp352Phase.Rouse || Current == Scp352Phase.Wander || Current == Scp352Phase.Fold) {
            if (level() instanceof ServerLevel) {
                playSound(Scp352Registry.Cackle.get(), 1.4f, 0.85f);
            }
            beginSeek(Fallen.position());
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        refreshSize();
        if (level().isClientSide) {
            gaitTick();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        KickReady = Math.max(0, KickReady - 1);
        SwipeReady = Math.max(0, SwipeReady - 1);
        LeapReady = Math.max(0, LeapReady - 1);
        if (Flying) {
            FlightAge++;
            if (FlightAge > 2 && (onGround() || isInWater() || isInLava()) || FlightAge > LeapAirMax) {
                touchDown(World);
            }
        }
        Scp352Phase Current = phase();
        long Age = phaseAge();
        switch (Current) {
            case Wander -> wander(World);
            case Transform -> {
                halt();
                transformCues(World, Age);
                if (Age >= Current.ticks()) {
                    enterHunt(pending(World));
                }
            }
            case Hunt -> hunt(World);
            case Fold -> {
                halt();
                if (Age >= Current.ticks()) {
                    setPhase(Scp352Phase.Wander);
                }
            }
            case Seek -> seek(World, Age);
            case Settle -> {
                halt();
                if (Age >= Current.ticks()) {
                    beginRest();
                }
            }
            case Rest -> resting(World);
            case Wake -> {
                halt();
                if (Age == 30) {
                    playLayers(Scp352Registry.MutterLayers, 0.9f, 0.9f);
                }
                if (Age >= Current.ticks()) {
                    setPhase(Scp352Phase.Wander);
                }
            }
            case Rouse -> {
                halt();
                if (Age == RouseSquawk) {
                    playLayers(Scp352Registry.SquawkLayers, 1.6f, 1.0f);
                }
                if (Age >= Current.ticks()) {
                    enterHunt(pending(World));
                }
            }
        }
    }

    @Nullable
    private LivingEntity pending(ServerLevel World) {
        if (PendingTarget != null && World.getEntity(PendingTarget) instanceof LivingEntity Found && Scp352Prey.prey(this, Found)) {
            return Found;
        }
        return Scp352Prey.nearest(World, this, HuntRange, true);
    }

    private void transformCues(ServerLevel World, long Age) {
        for (int Mark : CrackleTicks) {
            if (Age == Mark) {
                playLayers(Scp352Registry.CrackleLayers, 1.3f, 0.9f + random.nextFloat() * 0.2f);
            }
        }
        if (Age == SquawkTick) {
            playLayers(Scp352Registry.SquawkLayers, 1.8f, 1.0f);
        }
    }

    private void wander(ServerLevel World) {
        if (!AnomalyRealm.allows(World)) {
            return;
        }
        if (--RetargetTimer <= 0) {
            RetargetTimer = ScanTicks;
            LivingEntity Near = Scp352Prey.nearest(World, this, TriggerRange, true);
            if (Near != null && roomToRise()) {
                beginTransform(Near);
                return;
            }
            LivingEntity Sensed = Scp352Prey.nearest(World, this, SenseRange, false);
            if (Sensed != null && (--RepathTimer <= 0 || getNavigation().isDone())) {
                RepathTimer = DriftTicks / ScanTicks;
                getNavigation().moveTo(Sensed, DriftModifier);
                getLookControl().setLookAt(Sensed, 10.0f, 10.0f);
                return;
            }
            if (Sensed != null) {
                return;
            }
        }
        if (getNavigation().isDone() && random.nextInt(StrollOdds) == 0) {
            Vec3 Spot = DefaultRandomPos.getPos(this, 10, 4);
            if (Spot != null) {
                getNavigation().moveTo(Spot.x, Spot.y, Spot.z, WanderModifier);
            }
        }
    }

    private void hunt(ServerLevel World) {
        if (--RetargetTimer <= 0) {
            RetargetTimer = ScanTicks;
            LivingEntity Current = getTarget();
            if (Current != null && (!Scp352Prey.prey(this, Current) || distanceTo(Current) > FollowRange)) {
                setTarget(null);
                Current = null;
            }
            LivingEntity Seen = AnomalyRealm.allows(World) ? Scp352Prey.nearest(World, this, HuntRange, true) : null;
            if (Current == null) {
                if (Seen != null) {
                    setTarget(Seen);
                }
            } else if (Seen != null && Seen != Current && Scp352Prey.clearlyCloser(this, Seen, Current)) {
                setTarget(Seen);
            }
            LivingEntity Chased = getTarget();
            boolean Visible = Chased != null && hasLineOfSight(Chased);
            LostTicks = Visible ? 0 : LostTicks + ScanTicks;
            if (LostTicks >= LoseTicks) {
                beginFold();
                return;
            }
        }
        LivingEntity Target = getTarget();
        setAggressive(Target != null);
        if (action() != ActionNone) {
            actionTick(World, Target);
            return;
        }
        if (Target == null) {
            if (getNavigation().isDone()) {
                halt();
            }
            return;
        }
        getLookControl().setLookAt(Target, 30.0f, 30.0f);
        if (--RepathTimer <= 0 || getNavigation().isDone()) {
            RepathTimer = RepathTicks;
            getNavigation().moveTo(Target, RunModifier);
        }
        track(Target);
        if (tryLeap(World, Target)) {
            return;
        }
        tryAttack(Target);
    }

    private void forgetTrack() {
        TrackedTarget = null;
        TrackedVelocity = Vec3.ZERO;
        GapCount = 0;
        GapIndex = 0;
        StuckCount = 0;
    }

    private double flatTo(Vec3 Point) {
        double Dx = Point.x - getX();
        double Dz = Point.z - getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    private void track(LivingEntity Target) {
        if (!Target.getUUID().equals(TrackedTarget)) {
            forgetTrack();
            TrackedTarget = Target.getUUID();
            TrackedPos = Target.position();
        } else {
            Vec3 Step = Target.position().subtract(TrackedPos);
            TrackedVelocity = TrackedVelocity.scale(0.6).add(Step.scale(0.4));
            TrackedPos = Target.position();
        }
        Gaps[GapIndex] = flatTo(Target.position());
        GapIndex = (GapIndex + 1) % GapWindow;
        GapCount = Math.min(GapCount + 1, GapWindow);
        boolean Still = getDeltaMovement().horizontalDistanceSqr() < 0.0025;
        StuckCount = Still && flatTo(Target.position()) > 1.5 ? StuckCount + 1 : 0;
    }

    public Vec3 trackedVelocity() {
        return TrackedVelocity;
    }

    public boolean lagging() {
        if (GapCount < GapWindow) {
            return false;
        }
        double Oldest = Gaps[GapIndex];
        double Latest = Gaps[(GapIndex + GapWindow - 1) % GapWindow];
        return (Oldest - Latest) / (GapWindow - 1) < ClosingRate;
    }

    public boolean blocked() {
        Path Route = getNavigation().getPath();
        return Route == null || !Route.canReach() || StuckCount >= StuckLimit;
    }

    public int leaps() {
        return Leaps;
    }

    public boolean flying() {
        return Flying;
    }

    @Nullable
    public Scp352Leap.Plan leapPlan() {
        return LeapPlan;
    }

    private boolean tryLeap(ServerLevel World, LivingEntity Target) {
        if (LeapReady > 0 || !onGround() || isInWater() || isInLava() || isPassenger()) {
            return false;
        }
        double Flat = flatTo(Target.position());
        double Dy = Target.getY() - getY();
        boolean Climb = Dy >= ClimbMin && Dy <= ClimbMax && Flat <= ClimbReach && blocked();
        boolean Chase = !Climb && Flat >= LeapMin && Flat <= LeapMax && Dy <= LeapRise && Dy >= -LeapDrop && lagging() && hasLineOfSight(Target);
        if (!Climb && !Chase) {
            return false;
        }
        return beginLeap(World, Target, Climb);
    }

    public boolean beginLeap(ServerLevel World, LivingEntity Target, boolean Climb) {
        if (phase() != Scp352Phase.Hunt || action() != ActionNone || Flying) {
            return false;
        }
        Vec3 Aim = Scp352Leap.aim(position(), Target.position(), Target.getUUID().equals(TrackedTarget) ? TrackedVelocity : Vec3.ZERO, Climb ? 0.0 : LeadTicks, LeadCap,
                Climb ? ClimbShort : LandShort);
        Optional<Scp352Leap.Plan> Found = Scp352Leap.plan(World, this, Aim);
        if (Found.isEmpty()) {
            LeapReady = PlanRetry;
            return false;
        }
        LeapPlan = Found.get();
        LeapAim = Aim;
        ClimbLeap = Climb;
        LeapReady = LeapCooldownMax;
        beginAction(ActionLeap);
        entityData.set(LeapAirData, LeapPlan.Ticks());
        halt();
        playSound(Scp352Registry.LeapWindup.get(), 1.0f, 0.95f + random.nextFloat() * 0.1f);
        return true;
    }

    private void face(Vec3 Point) {
        double Dx = Point.x - getX();
        double Dz = Point.z - getZ();
        if (Dx * Dx + Dz * Dz < 1.0e-4) {
            return;
        }
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Mth.approachDegrees(getYRot(), Yaw, 30.0f));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void leapTick(ServerLevel World, @Nullable LivingEntity Target) {
        long Age = actionAge();
        getNavigation().stop();
        if (action() == ActionLand) {
            setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
            if (Age >= LandTicks) {
                entityData.set(ActionData, ActionNone);
            }
            return;
        }
        if (Age < LeapWindup) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            face(LeapPlan != null ? LeapPlan.Landing() : LeapAim);
            return;
        }
        if (Age == LeapWindup && !Flying) {
            launch(World, Target);
            return;
        }
        if (!Flying && Age > LeapWindup) {
            entityData.set(ActionData, ActionNone);
        }
    }

    private void launch(ServerLevel World, @Nullable LivingEntity Target) {
        Optional<Scp352Leap.Plan> Found = Optional.empty();
        if (Target != null && Target.isAlive() && !ClimbLeap) {
            Found = Scp352Leap.plan(World, this, Scp352Leap.aim(position(), Target.position(), TrackedVelocity, LeadTicks, LeadCap, LandShort));
        }
        if (Found.isEmpty() && LeapPlan != null) {
            Found = Scp352Leap.arc(World, this, position(), LeapPlan.Landing(), LeapPlan.Apex());
        }
        if (Found.isEmpty() && LeapPlan != null) {
            Found = Scp352Leap.plan(World, this, LeapPlan.Landing());
        }
        if (Found.isEmpty()) {
            entityData.set(ActionData, ActionNone);
            LeapReady = PlanRetry * 2;
            return;
        }
        LeapPlan = Found.get();
        entityData.set(LeapAirData, LeapPlan.Ticks());
        Vec3 Velocity = LeapPlan.Velocity();
        face(LeapPlan.Landing());
        setYRot((float) (Mth.atan2(Velocity.z, Velocity.x) * Mth.RAD_TO_DEG) - 90.0f);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        setDeltaMovement(Velocity);
        hasImpulse = true;
        hurtMarked = true;
        Flying = true;
        FlightAge = 0;
        Leaps++;
        playLayers(Scp352Registry.LeapLayers, 1.4f, 0.9f + random.nextFloat() * 0.15f);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), getX(), getY() + 0.1, getZ(), 14, 0.4, 0.05, 0.4, 0.1);
    }

    private void touchDown(ServerLevel World) {
        Flying = false;
        FlightAge = 0;
        fallDistance = 0.0f;
        playLayers(Scp352Registry.LandLayers, 1.5f, 0.9f + random.nextFloat() * 0.15f);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), getX(), getY() + 0.1, getZ(), 28, 0.7, 0.05, 0.7, 0.15);
        World.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2, getZ(), 8, 0.6, 0.05, 0.6, 0.02);
        LeapReady = ClimbLeap ? ClimbCooldown : LeapCooldownMin + random.nextInt(LeapCooldownMax - LeapCooldownMin + 1);
        if (phase() != Scp352Phase.Hunt || action() != ActionLeap) {
            return;
        }
        entityData.set(ActionStartData, level().getGameTime());
        entityData.set(ActionData, ActionLand);
        LivingEntity Target = getTarget();
        if (Target != null && Target.isAlive() && inReach(Target, LandReach)) {
            if (Target.hurt(damageSources().source(Scp352Registry.Hunt, this), LandDamage)) {
                Target.knockback(LandPush, getX() - Target.getX(), getZ() - Target.getZ());
                Target.hurtMarked = true;
                setLastHurtMob(Target);
                playLayers(Scp352Registry.KickLayers, 1.0f, 0.9f);
            }
        }
    }

    public double leapClipTick() {
        double Age = actionElapsed();
        if (action() == ActionLand) {
            return Math.min(LeapClipLand + Age, LeapClipEnd);
        }
        if (Age <= LeapWindup + LeapLaunch) {
            return Age;
        }
        double Air = Math.max(1.0, entityData.get(LeapAirData) - LeapLaunch);
        return LeapWindup + LeapLaunch + LeapClipAir * Mth.clamp((Age - LeapWindup - LeapLaunch) / Air, 0.0, 1.0);
    }

    @Override
    public void travel(Vec3 Input) {
        if (Flying && !level().isClientSide && !isInWater() && !isInLava()) {
            move(MoverType.SELF, getDeltaMovement());
            Vec3 Motion = getDeltaMovement();
            setDeltaMovement(Motion.x, (Motion.y - Scp352Leap.Gravity) * Scp352Leap.Drag, Motion.z);
            return;
        }
        super.travel(Input);
    }

    public double reachTo(LivingEntity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz) - Target.getBbWidth() * 0.5;
    }

    private boolean inReach(LivingEntity Target, double Reach) {
        double Dy = Target.getY() - getY();
        return reachTo(Target) <= Reach && Dy > -ReachHeight && Dy < ReachHeight && hasLineOfSight(Target);
    }

    private void tryAttack(LivingEntity Target) {
        if (KickReady <= 0 && inReach(Target, KickReach)) {
            beginAction(ActionKick);
            KickReady = KickCooldown;
            playSound(Scp352Registry.Rustle.get(), 0.8f, 1.2f);
        } else if (SwipeReady <= 0 && inReach(Target, SwipeReach)) {
            beginAction(ActionSwipe);
            SwipeReady = SwipeCooldown;
        }
    }

    private void beginAction(byte Kind) {
        entityData.set(ActionStartData, level().getGameTime());
        entityData.set(ActionData, Kind);
        getNavigation().stop();
    }

    private void actionTick(ServerLevel World, @Nullable LivingEntity Target) {
        long Age = actionAge();
        byte Kind = action();
        if (Kind == ActionLeap || Kind == ActionLand) {
            leapTick(World, Target);
            return;
        }
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.3, 1.0, 0.3));
        if (Target != null) {
            getLookControl().setLookAt(Target, 60.0f, 60.0f);
            float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(Mth.approachDegrees(getYRot(), Yaw, 20.0f));
            yBodyRot = getYRot();
        }
        if (Kind == ActionKick && Age == KickHit) {
            strike(Target, KickReach + 0.6, KickDamage, KickPush, Scp352Registry.KickLayers);
        } else if (Kind == ActionSwipe && Age == SwipeHit) {
            strike(Target, SwipeReach + 0.6, SwipeDamage, SwipePush, List.of(Scp352Registry.Swipe));
        }
        if (Age >= (Kind == ActionKick ? KickTicks : SwipeTicks)) {
            entityData.set(ActionData, ActionNone);
        }
    }

    private void strike(@Nullable LivingEntity Target, double Reach, float Damage, double Push, List<DeferredHolder<SoundEvent, SoundEvent>> Layers) {
        playLayers(Layers, 1.0f, 0.9f + random.nextFloat() * 0.2f);
        if (Target == null || !Target.isAlive() || !inReach(Target, Reach)) {
            return;
        }
        if (Target.hurt(damageSources().source(Scp352Registry.Hunt, this), Damage)) {
            Target.knockback(Push, getX() - Target.getX(), getZ() - Target.getZ());
            Target.hurtMarked = true;
            setLastHurtMob(Target);
        }
    }

    private void seek(ServerLevel World, long Age) {
        if (RestSpot == null) {
            beginSettle();
            return;
        }
        double Dx = RestSpot.getX() + 0.5 - getX();
        double Dz = RestSpot.getZ() + 0.5 - getZ();
        boolean Arrived = Dx * Dx + Dz * Dz <= SeekArrive * SeekArrive;
        if (Arrived || Age > SeekLimit || Age > 20 && getNavigation().isDone()) {
            beginSettle();
            return;
        }
        if (Age % 20 == 0) {
            getNavigation().moveTo(RestSpot.getX() + 0.5, RestSpot.getY(), RestSpot.getZ() + 0.5, StalkModifier);
        }
    }

    private void resting(ServerLevel World) {
        halt();
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (Rest.due(World) || !Rest.active()) {
            beginWake();
            return;
        }
        if (tickCount % HealInterval == 0 && getHealth() < getMaxHealth()) {
            heal(1.0f);
        }
        if (--SnoreTimer <= 0) {
            SnoreTimer = SnoreMin + random.nextInt(SnoreMax - SnoreMin);
            if (random.nextInt(3) == 0) {
                playSound(Scp352Registry.Breath.get(), 0.7f, 0.9f + random.nextFloat() * 0.15f);
            } else {
                playLayers(Scp352Registry.SnoreLayers, 0.9f, 0.9f + random.nextFloat() * 0.15f);
            }
        }
    }

    public void leaveStasis(ServerLevel World) {
        Rest.freeze(World);
        if (phase() == Scp352Phase.Rest) {
            entityData.set(PhaseStartData, World.getGameTime());
        }
    }

    private void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (isSilent()) {
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY(), getZ(), Layer.get(), getSoundSource(), Volume, Pitch);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level().isClientSide) {
            return false;
        }
        Scp352Phase Current = phase();
        Entity Attacker = Source.getEntity();
        if (Current.shielded() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (Source.getDirectEntity() != null) {
                Vec3 Away = position().subtract(Source.getDirectEntity().position()).multiply(1.0, 0.0, 1.0);
                if (Away.lengthSqr() > 1.0e-4) {
                    Vec3 Push = Away.normalize().scale(ShieldPush);
                    setDeltaMovement(getDeltaMovement().add(Push.x, 0.15, Push.z));
                    hurtMarked = true;
                }
                playSound(Scp352Registry.Rustle.get(), 0.8f, 1.3f);
            }
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (!Hurt || !isAlive() || !(Attacker instanceof LivingEntity Striker)) {
            return Hurt;
        }
        if (Current == Scp352Phase.Rest) {
            beginRouse(Striker);
        } else if (Current == Scp352Phase.Wander && Scp352Prey.prey(this, Striker)) {
            beginTransform(Striker);
        } else if (Current == Scp352Phase.Hunt && getTarget() == null && Scp352Prey.prey(this, Striker)) {
            setTarget(Striker);
        }
        return Hurt;
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
        if (phase() == Scp352Phase.Rest) {
            return;
        }
        super.knockback(Strength, X, Z);
    }

    @Override
    public boolean isPushable() {
        Scp352Phase Current = phase();
        return Current != Scp352Phase.Rest && Current != Scp352Phase.Settle && Current != Scp352Phase.Wake && super.isPushable();
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        if (Flying || action() == ActionLeap || action() == ActionLand) {
            return false;
        }
        return Distance > SafeFall && super.causeFallDamage(Distance - SafeFall, Multiplier, Source);
    }

    @Override
    public void die(DamageSource Source) {
        if (DeathKind < 0) {
            DeathKind = sizeClass();
        }
        super.die(Source);
        if (!level().isClientSide && isDeadOrDying()) {
            playSound(Scp352Registry.DeathSigh.get(), 1.0f, 0.9f);
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    public int deathKind() {
        if (DeathKind < 0 && isDeadOrDying()) {
            DeathKind = sizeClass();
        }
        return DeathKind >= 0 ? DeathKind : sizeClass();
    }

    @Override
    public int getAmbientSoundInterval() {
        return MutterInterval;
    }

    @Override
    public void playAmbientSound() {
        Scp352Phase Current = phase();
        if (Current == Scp352Phase.Wander) {
            playLayers(Scp352Registry.MutterLayers, 0.8f, 0.9f + random.nextFloat() * 0.15f);
        } else if (Current == Scp352Phase.Hunt && random.nextBoolean()) {
            playSound(Scp352Registry.Cackle.get(), 1.2f, 0.9f + random.nextFloat() * 0.15f);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp352Registry.Hurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        playLayers(Scp352Registry.HurtLayers, getSoundVolume(), getVoicePitch());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp352Registry.Death.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.5, 1.0, 1.5);
    }

    public Scp352Gait gait() {
        return Gait;
    }

    public double gaitPhase(float Partial) {
        return Mth.lerp(Partial, GaitPhaseO, GaitPhase);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    private void gaitTick() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        double Moved = Math.sqrt(Dx * Dx + Dz * Dz);
        double Turn = Math.abs(Mth.wrapDegrees(yBodyRot - BodyYawO)) * Mth.DEG_TO_RAD * Scp352Gait.TurnRadius;
        BodyYawO = yBodyRot;
        Scp352Phase Current = phase();
        boolean Walking = Current == Scp352Phase.Wander || Current.tall() && action() == ActionNone;
        double Pace = Walking ? Moved + Math.min(Turn, 0.2) : 0.0;
        PaceSmooth = PaceSmooth * 0.4 + Pace * 0.6;
        StillCount = PaceSmooth < Scp352Gait.StillPace ? StillCount + 1 : 0;
        Scp352Gait Next = Scp352Gait.choose(Current.tall(), Gait.stage() == (Current.tall() ? 2 : 1) ? Gait : Scp352Gait.Idle, PaceSmooth, StillCount);
        if (Next != Gait) {
            Gait = Next;
        }
        GaitPhaseO = GaitPhase;
        if (!Gait.moving() || !Walking || !isAlive()) {
            return;
        }
        double From = GaitPhase;
        GaitPhase += Math.min(Pace / Gait.distance(), Scp352Gait.MaxStep);
        double To = GaitPhase;
        if (isSilent()) {
            return;
        }
        for (float Mark : Gait.taps()) {
            if (Scp352Gait.crossed(From, To, Mark)) {
                local(Scp352Registry.CaneLayers, 0.55f, 0.9f + random.nextFloat() * 0.2f);
            }
        }
        for (float Mark : Gait.steps()) {
            if (Scp352Gait.crossed(From, To, Mark)) {
                if (Gait.stage() == 1) {
                    local(List.of(Scp352Registry.Rustle), 0.3f, 0.9f + random.nextFloat() * 0.2f);
                } else {
                    local(Scp352Registry.StepLayers, Gait == Scp352Gait.Run ? 0.9f : 0.6f, (Gait == Scp352Gait.Run ? 1.0f : 0.9f) + random.nextFloat() * 0.12f);
                }
            }
        }
    }

    private void local(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playLocalSound(getX(), getY(), getZ(), Layer.get(), getSoundSource(), Volume, Pitch, false);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (Key.equals(PhaseData) || Key.equals(PhaseStartData)) {
            refreshSize();
        }
        if (level().isClientSide && BodyController != null && (Key.equals(PhaseData) || Key.equals(ActionData) && action() != ActionNone && action() != ActionLand)) {
            BodyController.forceAnimationReset();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("Phase", (byte) phase().ordinal());
        Tag.putLong("PhaseAge", phaseAge());
        Tag.putInt("Kills", Kills);
        Tag.putLong("PendingRest", PendingRest);
        if (PendingTarget != null) {
            Tag.putUUID("PendingTarget", PendingTarget);
        }
        if (RestSpot != null) {
            Tag.put("RestSpot", NbtUtils.writeBlockPos(RestSpot));
        }
        CompoundTag Sleep = new CompoundTag();
        Rest.save(Sleep, level());
        Tag.put("Rest", Sleep);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Kills = Tag.getInt("Kills");
        PendingRest = Tag.contains("PendingRest") ? Tag.getLong("PendingRest") : -1L;
        PendingTarget = Tag.hasUUID("PendingTarget") ? Tag.getUUID("PendingTarget") : null;
        RestSpot = NbtUtils.readBlockPos(Tag, "RestSpot").orElse(null);
        Rest.load(Tag.getCompound("Rest"));
        Scp352Phase Saved = Scp352Phase.byId(Tag.getByte("Phase"));
        if (Saved == Scp352Phase.Fold || Saved == Scp352Phase.Wake) {
            Saved = Scp352Phase.Wander;
        } else if (Saved == Scp352Phase.Transform || Saved == Scp352Phase.Rouse) {
            Saved = Scp352Phase.Hunt;
        }
        if (Saved == Scp352Phase.Rest && !Rest.active()) {
            Saved = Scp352Phase.Wander;
        }
        entityData.set(PhaseData, (byte) Saved.ordinal());
        entityData.set(PhaseStartData, level().getGameTime() - Math.max(0L, Tag.getLong("PhaseAge")));
        entityData.set(ActionData, ActionNone);
        refreshSize();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        AnimationController<Scp352Crone> Body = new Scp352GaitController(this, "body", Event -> {
            Scp352GaitController Controller = (Scp352GaitController) Event.getController();
            if (deathTime > 0 || isDeadOrDying()) {
                Controller.blend(Scp352GaitController.ActionBlend);
                int Kind = deathKind();
                return Event.setAndContinue(Kind == Scp352Phase.Tall ? DeathPlay : Kind == Scp352Phase.Roost ? DeathRestPlay : DeathLowPlay);
            }
            Scp352Phase Current = phase();
            if (Current == Scp352Phase.Rest) {
                Controller.blend(Scp352GaitController.LoopBlend);
                return Event.setAndContinue(RestLoop);
            }
            if (Current.timed()) {
                Controller.blend(Scp352GaitController.ActionBlend);
                return Event.setAndContinue(Scp352GaitController.play(Current.clip()));
            }
            byte Kind = action();
            if (Current.tall() && (Kind == ActionLeap || Kind == ActionLand)) {
                Controller.blend(Scp352GaitController.ActionBlend);
                return Event.setAndContinue(LeapPlay);
            }
            if (Current.tall() && Kind == ActionKick) {
                Controller.blend(Scp352GaitController.ActionBlend);
                return Event.setAndContinue(KickPlay);
            }
            if (Current.tall() && Kind == ActionSwipe) {
                Controller.blend(Scp352GaitController.ActionBlend);
                return Event.setAndContinue(SwipePlay);
            }
            Controller.blend(Scp352GaitController.LoopBlend);
            Scp352Gait Shown = Current.tall() == (Gait.stage() == 2) ? Gait : Current.tall() ? Scp352Gait.Tall : Scp352Gait.Idle;
            return Event.setAndContinue(Shown.loop());
        });
        BodyController = Body;
        Controllers.add(Body);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
