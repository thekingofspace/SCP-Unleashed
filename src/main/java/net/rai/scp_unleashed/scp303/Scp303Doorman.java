package net.rai.scp_unleashed.scp303;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp303Doorman extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.8f;
    public static final float Height = 2.95f;
    public static final float EyeHeight = 2.62f;
    public static final float CrouchWidth = 0.8f;
    public static final float CrouchHeight = 1.95f;
    public static final float CrouchEye = 1.55f;
    public static final double PeekReach = 0.753;
    public static final double PeekInside = 1.073;
    public static final double MouthHeight = 1.55;
    public static final double Health = 80.0;
    public static final double Armour = 4.0;
    public static final double Speed = 0.25;
    public static final double WalkModifier = 0.9;
    public static final double SprintModifier = 2.0;
    public static final double FollowRange = 64.0;
    public static final double TargetRange = 64.0;
    public static final double KeepRange = TargetRange * 1.5;
    public static final double SwitchMargin = 4.0;
    public static final double FavourBias = 2.0;
    public static final double FleeGap = 10.0;
    public static final double FreedRange = 16.0;
    public static final double WitnessRange = 9.0;
    public static final double CloseRange = 5.0;
    public static final double StalkNear = 9.0;
    public static final double StalkFar = 20.0;
    public static final double CatchUp = 28.0;
    public static final double TeleportMin = 12.0;
    public static final double TeleportMax = 20.0;
    public static final double PassDistance = 3.0;
    public static final double WatchDrop = 12.0;
    public static final double DoorCone = 0.5;
    public static final double LeaveRange = 24.0;
    public static final double WindowRange = 4.5;
    public static final double GazeCone = 0.93;
    public static final double CornerRange = 8.0;
    public static final double BoundWatch = 7.0;
    public static final double BoundDrift = 12.0;
    public static final double EntrySpan = 0.6;
    public static final double PeekSnap = 2.5;
    public static final int StandTicks = 32;
    public static final int RapidTicks = 7;
    public static final int StareLimit = 40;
    public static final int GoalInterval = 30;
    public static final int HoldLimit = 2400;
    public static final int LeaveGrace = 200;
    public static final int LeaveLimit = 300;
    public static final int FleeUnseen = 10;
    public static final int FleeLimit = 400;
    public static final int TrapCooldown = 400;
    public static final int DoorMemory = 1200;
    public static final int StuckLimit = 60;
    public static final int LurkFleeMin = 900;
    public static final int LurkFleeMax = 1800;
    public static final int LurkCalmMin = 600;
    public static final int LurkCalmMax = 1200;
    public static final int LurkLostMin = 200;
    public static final int LurkLostMax = 400;
    public static final int LurkFreedMin = 300;
    public static final int LurkFreedMax = 600;
    public static final int RattleCooldown = 10;
    public static final int GazeTicks = 60;
    public static final int RetargetInterval = 20;
    public static final int Trail = 20;
    public static final byte ActionNone = 0;
    public static final byte ActionPeek = 1;
    public static final byte ActionStand = 2;
    public static final byte ActionRapid = 3;
    public static final EntityDimensions Crouched = EntityDimensions.scalable(CrouchWidth, CrouchHeight).withEyeHeight(CrouchEye);
    public static final Scp303Gait.Shape DoormanGait = new Scp303Gait.Shape(
            new Scp303Gait.Profile(22.0, 0.0916, 0.5, 2.2, new double[]{0.99, 0.49}, new float[]{1.0f, 1.0f}),
            new Scp303Gait.Profile(12.0, 0.5534, 0.6, 1.8, new double[]{0.99, 0.49}, new float[]{1.0f, 1.0f}),
            0.25, 0.18, 1, Scp303Registry.Step, Scp303Registry.StepThud, 0.95f, 0.75f, 1.0f, 28.0);

    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp303Doorman.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp303Doorman.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SnapData = SynchedEntityData.defineId(Scp303Doorman.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HeldData = SynchedEntityData.defineId(Scp303Doorman.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SprintLoop = RawAnimation.begin().thenLoop("sprint");
    private static final RawAnimation PeekLoop = RawAnimation.begin().thenLoop("crouch_peek");
    private static final RawAnimation StandPlay = RawAnimation.begin().thenPlayAndHold("stand");
    private static final RawAnimation RapidPlay = RawAnimation.begin().thenPlayAndHold("rapid_stand");

    public enum Mode {
        Idle, Stalk, Hold, Flee, Leave, Bound
    }

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Vec3[] TrailPoints = new Vec3[Trail];
    private int TrailIndex;
    private int TrailCount;
    private Mode Current = Mode.Idle;
    @Nullable
    private UUID TargetId;
    private boolean HadTarget;
    private int RetargetTimer;
    @Nullable
    private BlockPos WatchDoor;
    private int WatchSide;
    @Nullable
    private BlockPos LastDoor;
    private long LastDoorTime = -DoorMemory;
    private long TrapReady;
    private int HeldSide;
    private int HoldTicks;
    private int FarTicks;
    private boolean Gazed;
    private int RattleTimer;
    private int ActionTicks;
    @Nullable
    private Vec3 FleeFrom;
    private int FleeTicks;
    private int UnseenTicks;
    private int LurkMin = LurkFleeMin;
    private int LurkMax = LurkFleeMax;
    private int LeaveTicks;
    @Nullable
    private Vec3 Goal;
    private int GoalTimer;
    private int StareTicks;
    private int StuckTicks;
    @Nullable
    private Vec3 StuckFrom;
    private int RenderOrigin;
    private float RenderPartial;
    @Nullable
    private Scp303Gait Gait;

    public Scp303Doorman(EntityType<? extends Scp303Doorman> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ARMOR, Armour)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    public static boolean quarry(@Nullable Entity Candidate, Level World) {
        if (!(Candidate instanceof LivingEntity Living) || !Living.isAlive() || Living.isRemoved() || Living.level() != World || !Targeting.isTargetable(Living) || Scp268.isNeglected(Living)) {
            return false;
        }
        if (Living instanceof Player User) {
            return !User.isSpectator() && !User.isCreative();
        }
        if (Living instanceof Scp303Doorman || !Scp303Sight.kind(Living) || ControlSessions.isBoundToSession(Living)) {
            return false;
        }
        return !(Living instanceof DClassLike Body) || !Body.isPuppet() && !Body.isSpecimen();
    }

    public static List<LivingEntity> people(ServerLevel World) {
        return new ArrayList<>(World.getEntities(EntityTypeTest.forClass(LivingEntity.class), Candidate -> !(Candidate instanceof Player) && quarry(Candidate, World)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(SnapData, false);
        Builder.define(HeldData, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose ThePose) {
        return ThePose == Pose.CROUCHING ? Crouched : super.getDefaultDimensions(ThePose);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean folded() {
        return action() == ActionPeek;
    }

    public boolean locksMovement() {
        return action() != ActionNone;
    }

    public boolean timedAction() {
        byte Kind = action();
        return Kind == ActionStand || Kind == ActionRapid;
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionPeek -> "crouch_peek";
            case ActionStand -> "stand";
            case ActionRapid -> "rapid_stand";
            default -> null;
        };
    }

    public Optional<BlockPos> heldDoor() {
        return entityData.get(HeldData);
    }

    public Mode mode() {
        return Current;
    }

    @Nullable
    public UUID targetId() {
        return TargetId;
    }

    public boolean snapped() {
        return entityData.get(SnapData);
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + RenderOrigin - Start + RenderPartial);
    }

    private void setAction(byte Kind, int Ticks, boolean Snap) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(SnapData, Snap);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
        setPose(Kind == ActionPeek ? Pose.CROUCHING : Pose.STANDING);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        RenderOrigin = Buffer.readVarInt() - tickCount;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp303Gait Steps = gait();
            if (Steps != null) {
                Steps.tick(this);
            }
            return;
        }
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        Scp303State State = Scp303State.get(World.getServer());
        if (!State.isCurrent(getUUID())) {
            discard();
            return;
        }
        if (!Scp303Arrival.realm(World)) {
            vanish(World, LurkLostMin, LurkLostMax);
            return;
        }
        if (RattleTimer > 0) {
            RattleTimer--;
        }
        actionTick(World);
        if (isRemoved()) {
            return;
        }
        think(World, State);
    }

    private void actionTick(ServerLevel World) {
        byte Kind = action();
        if (Kind == ActionStand || Kind == ActionRapid) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (--ActionTicks <= 0) {
                setAction(ActionNone, 0, false);
                if (Kind == ActionStand && Current != Mode.Bound) {
                    Current = Mode.Leave;
                    LeaveTicks = 0;
                }
            }
        }
    }

    private void think(ServerLevel World, Scp303State State) {
        Optional<Scp303State.Binding> Bound = State.bound();
        if (Bound.isPresent() && Bound.get().Dimension().equals(World.dimension())) {
            boundTick(World, State, Bound.get());
            return;
        }
        if (Current == Mode.Bound) {
            freed(World, null);
            return;
        }
        if (timedAction()) {
            return;
        }
        switch (Current) {
            case Hold -> holdTick(World, State);
            case Flee -> fleeTick(World);
            case Leave -> leaveTick(World);
            default -> stalkTick(World, State);
        }
    }

    @Nullable
    public LivingEntity target(ServerLevel World, Scp303State State, boolean Retarget) {
        LivingEntity Known = known(World);
        if (!Retarget) {
            return Known;
        }
        if (--RetargetTimer > 0 && (Known != null || TargetId == null)) {
            return Known;
        }
        RetargetTimer = RetargetInterval;
        UUID Liked = State.favourite();
        LivingEntity Best = Known;
        double BestScore = Known == null ? Double.MAX_VALUE : score(Known, Liked) - SwitchMargin;
        for (LivingEntity Candidate : candidates(World)) {
            double Score = score(Candidate, Liked);
            if (Score < BestScore) {
                BestScore = Score;
                Best = Candidate;
            }
        }
        if (Best == null) {
            TargetId = null;
            return null;
        }
        if (!Best.getUUID().equals(TargetId)) {
            TargetId = Best.getUUID();
            TrailCount = 0;
            WatchDoor = null;
            Goal = null;
        }
        return Best;
    }

    @Nullable
    private LivingEntity known(ServerLevel World) {
        if (TargetId == null) {
            return null;
        }
        Entity Found = World.getEntity(TargetId);
        return Found instanceof LivingEntity Living && quarry(Living, World) && Living.distanceTo(this) <= KeepRange ? Living : null;
    }

    private double score(LivingEntity Candidate, @Nullable UUID Liked) {
        return distanceTo(Candidate) - (Candidate.getUUID().equals(Liked) ? FavourBias : 0.0);
    }

    private List<LivingEntity> candidates(ServerLevel World) {
        List<LivingEntity> Found = new ArrayList<>();
        for (ServerPlayer Candidate : World.players()) {
            if (quarry(Candidate, World) && Candidate.distanceTo(this) <= TargetRange) {
                Found.add(Candidate);
            }
        }
        Found.addAll(World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(TargetRange),
                Candidate -> !(Candidate instanceof Player) && quarry(Candidate, World) && Candidate.distanceTo(this) <= TargetRange));
        return Found;
    }

    private boolean anyQuarry(ServerLevel World) {
        for (ServerPlayer Candidate : World.players()) {
            if (quarry(Candidate, World)) {
                return true;
            }
        }
        return !people(World).isEmpty();
    }

    @Nullable
    private LivingEntity nearestPerson(ServerLevel World, double Range) {
        LivingEntity Best = World.getNearestPlayer(this, Range);
        double BestDistance = Best == null ? Range : Best.distanceTo(this);
        for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Range), Found -> !(Found instanceof Player) && Scp303Sight.person(Found))) {
            double Distance = Body.distanceTo(this);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Body;
            }
        }
        return Best;
    }

    @Nullable
    private Entity witness(ServerLevel World, @Nullable LivingEntity Target) {
        AABB Around = getBoundingBox().inflate(WitnessRange);
        Entity Nearest = null;
        double Best = WitnessRange * WitnessRange;
        for (Entity Found : World.getEntities(this, Around, Scp303Sight::person)) {
            if (Found == Target) {
                continue;
            }
            double Distance = Found.distanceToSqr(this);
            if (Distance <= Best) {
                Best = Distance;
                Nearest = Found;
            }
        }
        return Nearest;
    }

    private void recordTrail(LivingEntity Target) {
        TrailPoints[TrailIndex] = Target.position();
        TrailIndex = (TrailIndex + 1) % Trail;
        TrailCount = Math.min(Trail, TrailCount + 1);
    }

    private int entrySide(BlockPos Door, BlockState State) {
        for (int Back = TrailCount; Back >= 1; Back--) {
            Vec3 Point = TrailPoints[Math.floorMod(TrailIndex - Back, Trail)];
            if (Point == null) {
                continue;
            }
            double Offset = Scp303Doors.offset(Door, State, Point);
            if (Math.abs(Offset) >= EntrySpan) {
                return Offset >= 0.0 ? 1 : -1;
            }
        }
        return 0;
    }

    private void stalkTick(ServerLevel World, Scp303State State) {
        LivingEntity Target = target(World, State, true);
        if (Target == null) {
            getNavigation().stop();
            if (HadTarget && anyQuarry(World)) {
                vanish(World, LurkLostMin, LurkLostMax);
            } else if (HadTarget) {
                HadTarget = false;
            }
            Current = Mode.Idle;
            return;
        }
        HadTarget = true;
        Current = Mode.Stalk;
        State.favour(Target.getUUID());
        recordTrail(Target);
        Entity Other = witness(World, Target);
        if (Other != null) {
            flee(World, Other.position(), LurkFleeMin, LurkFleeMax);
            return;
        }
        double Distance = distanceTo(Target);
        boolean Spotted = Scp303Sight.sees(Target, this);
        if (Spotted && Distance < CloseRange) {
            flee(World, Target.position(), LurkFleeMin, LurkFleeMax);
            return;
        }
        if (doorTick(World, State, Target)) {
            return;
        }
        getLookControl().setLookAt(Target, 20.0f, 20.0f);
        if (Spotted) {
            StareTicks++;
            if (StareTicks < StareLimit) {
                getNavigation().stop();
                return;
            }
            if (StareTicks == StareLimit || StareTicks % GoalInterval == 0) {
                Goal = Scp303Spots.cover(World, this, Target, random).orElse(null);
                if (Goal != null) {
                    getNavigation().moveTo(Goal.x, Goal.y, Goal.z, WalkModifier);
                } else {
                    getNavigation().stop();
                }
            }
            return;
        }
        if (StareTicks > 0) {
            StareTicks = 0;
            Goal = null;
            GoalTimer = 0;
        }
        trackStuck();
        if (!Spotted && (Distance > CatchUp || StuckTicks > StuckLimit) && !Scp303Sight.seen(this)) {
            Optional<Vec3> Near = Scp303Spots.hidden(World, Target, TeleportMin, TeleportMax, random, null, true, false);
            if (Near.isPresent()) {
                arriveAt(Near.get(), Target);
                return;
            }
        }
        if (Scp303Sight.inLine(Target, position(), getBbHeight())) {
            GoalTimer = Math.min(GoalTimer, 5);
        }
        if (--GoalTimer <= 0 || Goal == null || position().distanceToSqr(Goal) < 1.0) {
            GoalTimer = GoalInterval + random.nextInt(10);
            Goal = Scp303Spots.hidden(World, Target, StalkNear, StalkFar, random, position(), false, true).orElse(null);
            if (Goal != null) {
                getNavigation().moveTo(Goal.x, Goal.y, Goal.z, WalkModifier);
            }
        }
    }

    private void trackStuck() {
        if (getNavigation().isDone()) {
            StuckTicks = 0;
            StuckFrom = null;
            return;
        }
        if (tickCount % 20 == 0) {
            if (StuckFrom != null && StuckFrom.distanceToSqr(position()) < 0.25) {
                StuckTicks += 20;
            } else {
                StuckTicks = 0;
            }
            StuckFrom = position();
        }
    }

    private void arriveAt(Vec3 Spot, @Nullable Entity Facing) {
        getNavigation().stop();
        float Yaw = getYRot();
        if (Facing != null) {
            Vec3 Toward = Facing.position().subtract(Spot);
            Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        }
        moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        setYHeadRot(Yaw);
        setYBodyRot(Yaw);
        setDeltaMovement(Vec3.ZERO);
        Goal = null;
        StuckTicks = 0;
        StuckFrom = null;
    }

    private boolean doorReady(ServerLevel World, BlockPos Door) {
        long Now = World.getGameTime();
        if (Now < TrapReady) {
            return false;
        }
        return !Door.equals(LastDoor) || Now - LastDoorTime > DoorMemory;
    }

    private boolean doorTick(ServerLevel World, Scp303State State, LivingEntity Target) {
        BlockPos Feet = Target.blockPosition();
        Optional<BlockPos> Inside = Scp303Doors.lowerOf(World, Feet);
        if (Inside.isEmpty()) {
            Inside = Scp303Doors.lowerOf(World, Feet.above());
        }
        if (Inside.isPresent() && !Inside.get().equals(WatchDoor) && !State.isBoundDoor(World, Inside.get()) && doorReady(World, Inside.get())) {
            int Entry = entrySide(Inside.get(), World.getBlockState(Inside.get()));
            if (Entry != 0) {
                WatchDoor = Inside.get();
                WatchSide = Entry;
            }
        }
        if (WatchDoor == null) {
            return false;
        }
        if (!Scp303Doors.usable(World, WatchDoor) || State.isBoundDoor(World, WatchDoor)) {
            WatchDoor = null;
            return false;
        }
        BlockState DoorState = World.getBlockState(WatchDoor);
        Vec3 Centre = Scp303Doors.centre(WatchDoor);
        double Away = Target.position().distanceTo(Centre);
        if (Away > WatchDrop) {
            WatchDoor = null;
            return false;
        }
        if (Scp303Doors.sideOf(WatchDoor, DoorState, Target.position()) == WatchSide || Away < PassDistance) {
            return false;
        }
        if (Scp303Sight.facing(Target, Centre, DoorCone) || Scp303Sight.seen(this)) {
            return false;
        }
        Optional<Vec3> Spot = Scp303Doors.hold(World, WatchDoor, WatchSide);
        if (Spot.isEmpty()) {
            forgetDoor(World, WatchDoor);
            return false;
        }
        AABB Near = Scp303Doors.crouchBox(Spot.get()).inflate(WitnessRange);
        if (!World.getEntities(this, Near, Found -> Found != Target && Scp303Sight.person(Found)).isEmpty()) {
            return false;
        }
        BlockPos Door = WatchDoor;
        int Side = WatchSide;
        Scp303Doors.shut(World, Door);
        if (Scp303Sight.watched(World, Spot.get(), CrouchHeight)) {
            forgetDoor(World, Door);
            return false;
        }
        beginHold(World, Door, Side, Spot.get());
        return true;
    }

    private void forgetDoor(ServerLevel World, BlockPos Door) {
        LastDoor = Door;
        LastDoorTime = World.getGameTime();
        WatchDoor = null;
    }

    public void beginHold(ServerLevel World, BlockPos Door, int Side, Vec3 Spot) {
        BlockState DoorState = World.getBlockState(Door);
        float Yaw = Scp303Doors.facingYaw(DoorState, Side);
        getNavigation().stop();
        setAction(ActionPeek, 0, true);
        moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        setYHeadRot(Yaw);
        setYBodyRot(Yaw);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(HeldData, Optional.of(Door.immutable()));
        HeldSide = Side;
        HoldTicks = 0;
        FarTicks = 0;
        Gazed = false;
        Current = Mode.Hold;
        forgetDoor(World, Door);
    }

    private void holdTick(ServerLevel World, Scp303State State) {
        Optional<BlockPos> Held = heldDoor();
        if (Held.isEmpty() || !Scp303Doors.usable(World, Held.get())) {
            flee(World, position().add(getLookAngle()), LurkFleeMin, LurkFleeMax);
            return;
        }
        BlockPos Door = Held.get();
        BlockState DoorState = World.getBlockState(Door);
        float Yaw = Scp303Doors.facingYaw(DoorState, HeldSide);
        setYRot(Yaw);
        setYHeadRot(Yaw);
        setYBodyRot(Yaw);
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (Scp303Doors.isOpen(World, Door)) {
            Scp303Doors.shut(World, Door);
            rattle(World, Door);
        }
        LivingEntity Target = target(World, State, false);
        Entity Other = witness(World, Target);
        if (Other != null) {
            flee(World, Other.position(), LurkFleeMin, LurkFleeMax);
            return;
        }
        if (Target == null) {
            calmLeave(World);
            return;
        }
        recordTrail(Target);
        int TargetSide = Scp303Doors.sideOf(Door, DoorState, Target.position());
        if (TargetSide == HeldSide && Target.distanceTo(this) < CornerRange) {
            flee(World, Target.position(), LurkFleeMin, LurkFleeMax);
            return;
        }
        HoldTicks++;
        double Away = Target.position().distanceTo(Scp303Doors.centre(Door));
        FarTicks = Away > LeaveRange ? FarTicks + 1 : 0;
        if (FarTicks > LeaveGrace || HoldTicks > HoldLimit) {
            calmLeave(World);
            return;
        }
        if (!Gazed && TargetSide != HeldSide && Away < WindowRange && Scp303Sight.facing(Target, mouth(), GazeCone)) {
            Gazed = true;
            Target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GazeTicks, 0, false, false, true), this);
        }
    }

    public Vec3 mouth() {
        return position().add(Vec3.directionFromRotation(0.0f, getYRot()).scale(PeekReach - 0.05)).add(0.0, MouthHeight, 0.0);
    }

    public void rattle(ServerLevel World, BlockPos Door) {
        if (RattleTimer > 0) {
            return;
        }
        RattleTimer = RattleCooldown;
        Vec3 Handle = Scp303Doors.handle(Door, World.getBlockState(Door));
        float Pitch = 0.9f + random.nextFloat() * 0.2f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp303Registry.RattleLayers) {
            World.playSound(null, Handle.x, Handle.y, Handle.z, Layer.get(), SoundSource.BLOCKS, 1.0f, Pitch);
        }
    }

    public boolean holding(BlockPos Door) {
        return Current == Mode.Hold && heldDoor().map(Door::equals).orElse(false);
    }

    public void doorBroken(ServerLevel World, BlockPos Door) {
        if (holding(Door)) {
            flee(World, Scp303Doors.centre(Door), LurkFleeMin, LurkFleeMax);
        }
    }

    private void creak(ServerLevel World, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp303Registry.CreakLayers) {
            World.playSound(null, getX(), getY() + 1.2, getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch * (0.95f + random.nextFloat() * 0.1f));
        }
    }

    private void releaseDoor() {
        entityData.set(HeldData, Optional.empty());
    }

    private void calmLeave(ServerLevel World) {
        releaseDoor();
        TrapReady = World.getGameTime() + TrapCooldown;
        Current = Mode.Leave;
        LeaveTicks = 0;
        Goal = null;
        if (folded()) {
            setAction(ActionStand, StandTicks, false);
            creak(World, 0.7f, 0.85f);
        }
    }

    public void flee(ServerLevel World, Vec3 From, int Min, int Max) {
        releaseDoor();
        TrapReady = World.getGameTime() + TrapCooldown;
        FleeFrom = From;
        FleeTicks = 0;
        UnseenTicks = 0;
        LurkMin = Min;
        LurkMax = Max;
        Current = Mode.Flee;
        Goal = null;
        if (folded() || action() == ActionStand) {
            setAction(ActionRapid, RapidTicks, false);
            creak(World, 1.0f, 1.15f);
        }
    }

    private void fleeTick(ServerLevel World) {
        FleeTicks++;
        Vec3 From = FleeFrom == null ? position() : FleeFrom;
        if (getNavigation().isDone() || FleeTicks % 20 == 1) {
            Vec3 Away = LandRandomPos.getPosAway(this, 32, 8, From);
            if (Away == null) {
                Away = DefaultRandomPos.getPosAway(this, 24, 7, From);
            }
            if (Away != null) {
                getNavigation().moveTo(Away.x, Away.y, Away.z, SprintModifier);
            }
        }
        boolean Seen = Scp303Sight.seen(this);
        UnseenTicks = Seen ? 0 : UnseenTicks + 1;
        if (UnseenTicks >= FleeUnseen && (FleeTicks > 40 || nearestPerson(World, FleeGap) == null) || FleeTicks > FleeLimit && !Seen) {
            vanish(World, LurkMin, LurkMax);
        }
    }

    private void leaveTick(ServerLevel World) {
        LeaveTicks++;
        if (Goal == null) {
            Vec3 Away = DefaultRandomPos.getPosAway(this, 12, 5, position().add(getLookAngle().scale(3.0)));
            Goal = Away == null ? position() : Away;
            getNavigation().moveTo(Goal.x, Goal.y, Goal.z, WalkModifier);
        }
        if (!Scp303Sight.seen(this) && (LeaveTicks > 20 || LeaveTicks > LeaveLimit)) {
            vanish(World, LurkCalmMin, LurkCalmMax);
        }
    }

    public void vanish(ServerLevel World, int Min, int Max) {
        Scp303State State = Scp303State.get(World.getServer());
        if (State.isCurrent(getUUID())) {
            State.leave(World.getGameTime() + Min + random.nextInt(Math.max(1, Max - Min + 1)));
        }
        discard();
    }

    private void boundTick(ServerLevel World, Scp303State State, Scp303State.Binding Binding) {
        BlockPos Door = Binding.Door();
        releaseDoor();
        Current = Mode.Bound;
        if (!World.isLoaded(Door)) {
            if (!Scp303Sight.seen(this)) {
                State.lose();
                discard();
            }
            return;
        }
        if (!Scp303Doors.usable(World, Door) || Scp303Doors.isOpen(World, Door)) {
            State.unbind();
            freed(World, null);
            return;
        }
        if (timedAction()) {
            return;
        }
        BlockState DoorState = World.getBlockState(Door);
        int Cell = Binding.CellSide();
        Optional<Vec3> Peek = Scp303Doors.hold(World, Door, Cell);
        Optional<Vec3> Rest = Scp303Doors.cellIdle(World, Door, Cell);
        boolean Visitor = visitor(World, Door, DoorState, Cell);
        Vec3 Want = Visitor && Peek.isPresent() ? Peek.get() : Rest.orElse(Peek.orElse(null));
        if (Want == null) {
            getNavigation().stop();
            return;
        }
        double Off = position().distanceTo(Want);
        boolean CellSide = Scp303Doors.sideOf(Door, DoorState, position()) == Cell;
        if (Off > BoundDrift || !CellSide) {
            if (!Scp303Sight.seen(this) && !Scp303Sight.watched(World, Want, getBbHeight())) {
                if (Visitor && Peek.isPresent()) {
                    beginBoundPeek(World, Door, Cell, Want);
                } else {
                    if (folded()) {
                        setAction(ActionNone, 0, false);
                    }
                    arriveAt(Want, null);
                    faceDoor(DoorState, Cell);
                }
            }
            return;
        }
        if (Visitor && Peek.isPresent()) {
            if (folded()) {
                faceDoor(DoorState, Cell);
                setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
                return;
            }
            if (Off < PeekSnap || !Scp303Sight.seen(this)) {
                beginBoundPeek(World, Door, Cell, Want);
                return;
            }
            getNavigation().moveTo(Want.x, Want.y, Want.z, WalkModifier * 0.8);
            return;
        }
        if (folded()) {
            setAction(ActionStand, StandTicks, false);
            creak(World, 0.6f, 0.85f);
            return;
        }
        if (Off > 0.6) {
            getNavigation().moveTo(Want.x, Want.y, Want.z, WalkModifier * 0.8);
        } else {
            getNavigation().stop();
            faceDoor(DoorState, Cell);
        }
    }

    private boolean visitor(ServerLevel World, BlockPos Door, BlockState DoorState, int Cell) {
        Vec3 Centre = Scp303Doors.centre(Door);
        for (Player Viewer : World.players()) {
            if (Scp303Sight.viewer(Viewer) && Viewer.position().distanceTo(Centre) < BoundWatch && Scp303Doors.sideOf(Door, DoorState, Viewer.position()) != Cell) {
                return true;
            }
        }
        for (LivingEntity Viewer : Scp303Sight.watchers(World, Centre, BoundWatch)) {
            if (Viewer.position().distanceTo(Centre) < BoundWatch && Scp303Doors.sideOf(Door, DoorState, Viewer.position()) != Cell) {
                return true;
            }
        }
        return false;
    }

    private void beginBoundPeek(ServerLevel World, BlockPos Door, int Cell, Vec3 Spot) {
        BlockState DoorState = World.getBlockState(Door);
        float Yaw = Scp303Doors.facingYaw(DoorState, Cell);
        getNavigation().stop();
        boolean Unseen = !Scp303Sight.seen(this);
        setAction(ActionPeek, 0, Unseen);
        moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        setYHeadRot(Yaw);
        setYBodyRot(Yaw);
        setDeltaMovement(Vec3.ZERO);
    }

    private void faceDoor(BlockState DoorState, int Cell) {
        float Yaw = Scp303Doors.facingYaw(DoorState, Cell);
        setYRot(Yaw);
        setYHeadRot(Yaw);
        setYBodyRot(Yaw);
    }

    public void freed(ServerLevel World, @Nullable Player Freer) {
        Scp303State State = Scp303State.get(World.getServer());
        if (Freer != null) {
            State.favour(Freer.getUUID());
        }
        Current = Mode.Stalk;
        LivingEntity From = Freer != null ? Freer : nearestPerson(World, FreedRange);
        flee(World, From != null ? From.position() : position().add(getLookAngle()), LurkFreedMin, LurkFreedMax);
    }

    public void bindTo(ServerLevel World) {
        releaseDoor();
        if (Current != Mode.Bound) {
            Current = Mode.Bound;
            Goal = null;
            getNavigation().stop();
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && isAlive() && level() instanceof ServerLevel World && Current != Mode.Bound && Current != Mode.Flee && Source.getEntity() != null) {
            flee(World, Source.getEntity().position(), LurkFleeMin, LurkFleeMax);
        }
        return Hurt;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (Source.is(DamageTypes.IN_WALL) || Source.is(DamageTypes.CRAMMING) || Source.is(DamageTypeTags.IS_FALL)) {
            return true;
        }
        return super.isInvulnerableTo(Source);
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            Scp303State.get(World.getServer()).died(getUUID());
        }
        super.die(Source);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && Current != Mode.Hold && super.isPushable();
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean startRiding(Entity Vehicle, boolean Force) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected float getSoundVolume() {
        return 0.0f;
    }

    @Override
    public int getMaxHeadYRot() {
        return 50;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.0, 0.3, 1.0);
    }

    @Nullable
    public Scp303Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp303Gait(DoormanGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp303Gait Steps = gait();
        return Steps != null && Steps.striding();
    }

    public boolean isRunning() {
        Scp303Gait Steps = gait();
        return Steps != null && Steps.striding() && Steps.running();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp303Gait Steps = gait();
        if (Steps != null) {
            Steps.setPartial(Partial);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Optional<BlockPos> Held = heldDoor();
        if (Held.isPresent() && Current == Mode.Hold) {
            Tag.put("Held", NbtUtils.writeBlockPos(Held.get()));
            Tag.putInt("HeldSide", HeldSide);
            Tag.putInt("HoldTicks", HoldTicks);
        }
        if (TargetId != null) {
            Tag.putUUID("Target", TargetId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        TargetId = Tag.hasUUID("Target") ? Tag.getUUID("Target") : null;
        Optional<BlockPos> Held = NbtUtils.readBlockPos(Tag, "Held");
        if (Held.isPresent()) {
            entityData.set(HeldData, Held);
            HeldSide = Tag.getInt("HeldSide") >= 0 ? 1 : -1;
            HoldTicks = Tag.getInt("HoldTicks");
            Current = Mode.Hold;
            entityData.set(ActionData, ActionPeek);
            entityData.set(SnapData, true);
            setPose(Pose.CROUCHING);
        } else {
            entityData.set(ActionData, ActionNone);
            setPose(Pose.STANDING);
        }
        ActionTicks = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp303GaitController(this, "body", 0, Event -> {
            byte Kind = action();
            Event.getController().transitionLength(Scp303GaitController.blendFor(Kind, snapped()));
            if (Kind == ActionPeek) {
                return Event.setAndContinue(PeekLoop);
            }
            if (Kind == ActionStand) {
                return Event.setAndContinue(StandPlay);
            }
            if (Kind == ActionRapid) {
                return Event.setAndContinue(RapidPlay);
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? SprintLoop : WalkLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    public static List<Scp303Doorman> near(Level World, BlockPos Pos, double Range) {
        return World.getEntitiesOfClass(Scp303Doorman.class, new AABB(Pos).inflate(Range));
    }
}
