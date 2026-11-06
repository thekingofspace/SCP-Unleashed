package net.rai.scp_unleashed.scp049;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Scp049Doctor extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn, Enemy {
    public static final float Width = 0.6f;
    public static final float Height = 1.95f;
    public static final float EyeHeight = 1.74f;
    public static final double MaxHealth = 160.0;
    public static final double Armour = 10.0;
    public static final double Toughness = 4.0;
    public static final double Steadiness = 0.8;
    public static final double Speed = 0.3;
    public static final double Follow = 40.0;
    public static final double StrollSpeed = 0.58;
    public static final double SeekSpeed = 0.72;
    public static final double DragSpeed = 0.55;
    public static final double PursueSpeed = 0.82;
    public static final double FleeSpeed = 1.0;
    public static final double SightRange = 24.0;
    public static final double AreaRadius = 48.0;
    public static final double AreaDepth = 16.0;
    public static final double VillagePadding = 8.0;
    public static final double LeashRange = 72.0;
    public static final double LoseRange = 40.0;
    public static final double TouchStart = 1.1;
    public static final double TouchReach = 1.4;
    public static final double ReachRise = 1.3;
    public static final double ReachFloor = 0.5;
    public static final double HandHeight = 1.3;
    public static final double HandInset = 0.1;
    public static final double ArriveRange = 0.9;
    public static final double HomeRange = 2.5;
    public static final double TetherLength = 1.35;
    public static final double TetherBreak = 6.0;
    public static final double LagLimit = 3.2;
    public static final double DragPull = 0.3;
    public static final double TrayReach = 12.0;
    public static final int TouchTicks = 14;
    public static final int TouchHitTick = 8;
    public static final int TouchCooldown = 30;
    public static final int RegrabTicks = 30;
    public static final int ReleasedIgnore = 600;
    public static final int StruckMemory = 60;
    public static final int ThreatTicks = 600;
    public static final int ZombieInterval = 20;
    public static final double ZombieRange = 20.0;
    public static final double ZombieVertical = 8.0;
    public static final double SquadRange = 32.0;
    public static final int ZombieSquad = 3;
    public static final int CommandGap = 100;
    public static final int ZombieMemory = 600;
    public static final double DuelRange = 8.0;
    public static final double GolemVertical = 8.0;
    public static final double GolemLeash = 32.0;
    public static final double DuelLeash = 16.0;
    public static final double DuelChase = 10.0;
    public static final double DuelDrop = 1.0;
    public static final double DuelStep = 0.5;
    public static final int DuelStall = 80;
    public static final int DuelRecheck = 10;
    public static final int GolemPatience = 300;
    public static final int GolemIgnore = 600;
    public static final int UnreachableIgnore = 2400;
    public static final int GolemHitMemory = 40;
    public static final double HomeStep = 0.5;
    public static final int HomeReplan = 60;
    public static final int HomeGiveUp = 300;
    public static final double FellStart = 1.1;
    public static final double FellReach = 1.6;
    public static final double CarryWait = 3.0;
    public static final float FellTurn = 30.0f;
    public static final int FellCooldownTicks = 20;
    public static final int ThudDelay = 14;
    public static final int MutterDelay = 30;
    public static final float FellVolume = 0.45f;
    public static final float FellFadeVolume = 0.8f;
    public static final float GolemFallVolume = 0.75f;
    public static final float GolemFallLowVolume = 0.65f;
    public static final int DoorTicks = 12;
    public static final int BowTicks = 28;
    public static final int CommandTicks = 24;
    public static final int FlinchTicks = 12;
    public static final int OperateTicks = 100;
    public static final int RiseDelay = 24;
    public static final float RescueThreshold = 12.0f;
    public static final int RescueWindow = 60;
    public static final int RegenInterval = 80;
    public static final int RegenDelay = 200;
    public static final int ScanInterval = 10;
    public static final int VillagerInterval = 40;
    public static final int LostTicks = 200;
    public static final int OrderRefresh = 100;
    public static final int IgnoreTicks = 2400;
    public static final int StuckCheck = 40;
    public static final int StuckGiveUp = 200;
    public static final int TrayCheck = 20;
    public static final int TrailLimit = 48;
    public static final int DragReplan = 60;
    public static final int DragNudge = 120;
    public static final int DragGiveUp = 200;
    public static final int ReplanEvery = 40;
    public static final int NudgeEvery = 20;
    public static final double ProgressStep = 0.25;
    public static final int ProgressEvery = 10;
    public static final int ProgressSpan = 2;
    public static final double WorkSlack = 1.8;
    public static final int NewTrayGrace = 1800;
    public static final double RoamRadius = 32.0;
    public static final int RoamStep = 12;
    public static final int RoamVertical = 6;
    public static final int RoamRecheck = 200;
    public static final int RoamLegTicks = 300;
    public static final int RoamPauseMin = 40;
    public static final int RoamPauseSpread = 80;
    public static final int HomeRetry = 600;
    public static final int HomeRetryMax = 6000;
    public static final int PursueStall = 100;
    public static final double PursueStep = 0.5;
    public static final double PursueClose = 1.6;
    public static final int OutOfReachIgnore = 600;
    public static final int FirstTrayTries = 30;
    public static final double LinkRange = 128.0;
    public static final double TrayRange = LeashRange;
    public static final float TrayProbeRange = 80.0f;
    public static final float TrayProbeNodes = 3.0f;
    public static final int TrayProbeTicks = 60;
    public static final int ShunnedRecheck = 60;
    public static final double FleeSafe = 16.0;
    public static final double FleeFar = 24.0;
    public static final int FleeStep = 16;
    public static final int FleeVertical = 7;
    public static final int FleeTries = 8;
    public static final int FleeReplan = 20;
    public static final double FleeMargin = 4.0;
    public static final double FleeProgress = 0.5;
    public static final int FearInterval = 5;
    public static final int WaryTicks = 240;
    public static final int WaryLimit = 1200;
    public static final double WaryRadius = 10.0;
    public static final int WaryStep = 8;
    public static final int CorneredTicks = 80;
    public static final int BraveTicks = 200;
    public static final int AlarmGap = 60;
    public static final byte VoiceEvent = 101;
    public static final int VoiceQuiet = 50;
    public static final byte ActionNone = 0;
    public static final byte ActionTouch = 1;
    public static final byte ActionDoor = 2;
    public static final byte ActionBow = 3;
    public static final byte ActionCommand = 4;
    public static final byte ActionFlinch = 5;
    public static final byte ActionSurgery = 6;
    public static final byte ActionFell = 7;
    public static final byte ActionFellFree = 8;
    public static final byte ModeIdle = 0;
    public static final byte ModeTidy = 1;
    public static final byte ModeDrag = 2;
    public static final byte ModeSurgery = 3;
    public static final Scp049Gait.Profile WalkGait = new Scp049Gait.Profile("walk", 15.0, 0.065, 0.5, 2.0, 3, new double[]{0.97261, 0.47261}, new float[]{1.0f, 1.0f},
            Scp049Registry.Step, Scp049Registry.Robe, 0.42f, 0.22f, 0.95f);
    public static final Scp049Gait.Profile StrideGait = new Scp049Gait.Profile("stride", 10.0, 0.13, 0.6, 1.8, 2, new double[]{0.97616, 0.47616}, new float[]{1.0f, 1.0f},
            Scp049Registry.Step, Scp049Registry.Robe, 0.55f, 0.28f, 1.0f);
    public static final Scp049Gait.Profile DragGait = new Scp049Gait.Profile("drag", 16.0, 0.06, 0.5, 2.0, 3, new double[]{0.97931, 0.47931}, new float[]{1.0f, 1.0f},
            Scp049Registry.Step, Scp049Registry.Robe, 0.5f, 0.3f, 0.92f);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp049Doctor.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp049Doctor.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> ModeData = SynchedEntityData.defineId(Scp049Doctor.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> HurriedData = SynchedEntityData.defineId(Scp049Doctor.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> TrayData = SynchedEntityData.defineId(Scp049Doctor.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> HeldData = SynchedEntityData.defineId(Scp049Doctor.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation TidyLoop = RawAnimation.begin().thenLoop("tidy");
    private static final RawAnimation HoldLoop = RawAnimation.begin().thenLoop("hold");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation StrideLoop = RawAnimation.begin().thenLoop("stride");
    private static final RawAnimation DragLoop = RawAnimation.begin().thenLoop("drag");
    private static final RawAnimation SurgeryPlay = RawAnimation.begin().thenPlayAndHold("surgery");
    private static final RawAnimation TouchPlay = RawAnimation.begin().thenPlayAndHold("touch");
    private static final RawAnimation TouchLeftPlay = RawAnimation.begin().thenPlayAndHold("touch_left");
    private static final RawAnimation DoorPlay = RawAnimation.begin().thenPlayAndHold("door");
    private static final RawAnimation BowPlay = RawAnimation.begin().thenPlayAndHold("bow");
    private static final RawAnimation CommandPlay = RawAnimation.begin().thenPlayAndHold("command");
    private static final RawAnimation FlinchPlay = RawAnimation.begin().thenPlayAndHold("flinch");

    public enum Phase {
        Wait,
        Seek,
        Drag,
        Operate,
        Greet,
        Pursue,
        Return,
        Roam,
        Flee,
        Wary
    }

    public record PendingRise(CompoundTag Look, Vec3 Spot, float Yaw, long Due) {
    }

    public record Collapse(Vec3 Spot, float Yaw, int ThudAt, int MutterAt) {
    }

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Scp049Portals Portals = new Scp049Portals(this);
    private final Scp049Fear Fear = new Scp049Fear(this);
    private final ArrayDeque<Vec3> Trail = new ArrayDeque<>();
    private final List<UUID> Cured = new ArrayList<>();
    private final List<PendingRise> Rises = new ArrayList<>();
    private final Map<UUID, Long> Ignored = new HashMap<>();
    private Phase Routine = Phase.Wait;
    private int PhaseTicks;
    @Nullable
    private BlockPos TrayPos;
    @Nullable
    private Vec3 WorkSpot;
    @Nullable
    private Vec3 PatientSpot;
    @Nullable
    private AABB Area;
    private long AreaStamp = Long.MIN_VALUE;
    @Nullable
    private UUID Prey;
    @Nullable
    private UUID Quarry;
    @Nullable
    private UUID Held;
    @Nullable
    private LivingEntity TouchTarget;
    private int ActionTicks;
    private int Cooldown;
    private int LastSeenTick;
    private int LastHurtTick = -RegenDelay;
    private long RescueStart = Long.MIN_VALUE;
    private float RescueDamage;
    private int OperateLeft;
    private int ModeLeft;
    private int StuckTicks;
    private int DragStuck;
    private final double[] DoctorSamples = new double[ProgressSpan + 1];
    private final double[] GapSamples = new double[ProgressSpan + 1];
    private int SampleCount;
    private String DragState = "idle";
    private int Replans;
    private int Nudges;
    private long NewTrayAt = -1L;
    private boolean Roaming;
    private boolean HadTray;
    private String RoamReason = "none";
    private int RoamCount;
    private int HomeFails;
    private int HomeRetryAt;
    private int RoamPause;
    private int RoamLeg;
    private int PlaceTries;
    private int TouchKills;
    private int OutOfReach;
    private double PursueBest = Double.MAX_VALUE;
    private int PursueBestTick;
    @Nullable
    private Vec3 FieldSpot;
    @Nullable
    private Vec3 FieldPatient;
    private int QuietLeft;
    private int SurgeryTick = -1;
    private boolean PendingLoss;
    @Nullable
    private UUID LostId;
    @Nullable
    private UUID LostAttacker;
    @Nullable
    private UUID Regrab;
    private String LossReason = "none";
    private String LossOutcome = "none";
    private int LossTick = Integer.MIN_VALUE;
    @Nullable
    private UUID Threat;
    private int ThreatUntil;
    @Nullable
    private Entity CommandFocus;
    private int LastCommandTick = -CommandGap;
    private final Map<UUID, Integer> KnownZombies = new HashMap<>();
    private int ZombieOrders;
    private int PlagueOrders;
    private int PlagueKills;
    @Nullable
    private UUID Golem;
    private int GolemSince;
    private int GolemHitTick = Integer.MIN_VALUE;
    @Nullable
    private Vec3 DuelStart;
    private double DuelBest;
    private int DuelBestTick;
    private boolean DuelSteered;
    private String DuelState = "none";
    private String DuelEnd = "none";
    private int DuelEndTick = Integer.MIN_VALUE;
    private double HomeBest = Double.MAX_VALUE;
    private int HomeBestTick;
    private int HomeReplanTick;
    @Nullable
    private Path HomeRoute;
    private int HomeReplans;
    private int HomeGiveUps;
    @Nullable
    private IronGolem FellTarget;
    private int FellCooldown;
    private int GolemsFelled;
    private int LastFellTick = Integer.MIN_VALUE;
    private final List<Collapse> Collapses = new ArrayList<>();
    @Nullable
    private Vec3 StuckFrom;
    @Nullable
    private UUID Dread;
    @Nullable
    private Vec3 DreadSpot;
    private boolean DreadDropped;
    @Nullable
    private Vec3 FleeGoal;
    private double FleeBest;
    private int FleeBestTick;
    private int WaryUntil;
    private int BraveUntil = Integer.MIN_VALUE;
    private int LastAlarmTick = Integer.MIN_VALUE;
    private int LastRecoilTick = -AlarmGap;
    private int Alarms;
    private int Corners;
    private int TickOrigin;
    private float RenderPartial;
    @Nullable
    private Scp049Gait WalkStride;
    @Nullable
    private Scp049Gait FastStride;
    @Nullable
    private Scp049Gait DragStride;
    @Nullable
    private Scp049Breath Breath;
    @Nullable
    private Scp049Controller BodyController;
    @Nullable
    private Scp049Navigation Prober;
    @Nullable
    private BlockPos RouteFor;
    private boolean RouteOpen = true;
    private int RouteTick;

    public Scp049Doctor(EntityType<? extends Scp049Doctor> Type, Level World) {
        super(Type, World);
        xpReward = 50;
        setPersistenceRequired();
        setPathfindingMalus(PathType.WATER, 8.0f);
        setPathfindingMalus(PathType.DANGER_FIRE, 16.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ARMOR, Armour)
                .add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, Steadiness).add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp049Navigation(this, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(ModeData, ModeIdle);
        Builder.define(HurriedData, false);
        Builder.define(TrayData, Optional.empty());
        Builder.define(HeldData, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public byte mode() {
        return entityData.get(ModeData);
    }

    public boolean isHurried() {
        return entityData.get(HurriedData);
    }

    public Phase routine() {
        return Routine;
    }

    @Nullable
    public BlockPos trayPos() {
        return TrayPos;
    }

    public boolean isRoaming() {
        return Roaming;
    }

    public boolean hasWorkableTray() {
        return TrayPos != null && !Roaming;
    }

    public boolean trayUsable(ServerLevel World) {
        return trayBar(World) == null;
    }

    @Nullable
    public String trayBar(ServerLevel World) {
        if (TrayPos == null) {
            return "lost";
        }
        if (Roaming) {
            return RoamReason;
        }
        if (!World.isLoaded(TrayPos)) {
            return "unloaded";
        }
        if (!(World.getBlockEntity(TrayPos) instanceof Scp049TrayBlockEntity Tray) || !Tray.canBind(getUUID())) {
            return "lost";
        }
        Vec3 Spot = home();
        if (Fear.covers(Spot)) {
            return "shunned";
        }
        if (distanceToSqr(Spot) > TrayRange * TrayRange) {
            return "far";
        }
        if (nearWork()) {
            return null;
        }
        return trayRoute() ? null : "unreachable";
    }

    private boolean trayRoute() {
        if (TrayPos.equals(RouteFor) && tickCount - RouteTick < TrayProbeTicks) {
            return RouteOpen;
        }
        if (!onGround() && !isInLiquid() && !isPassenger()) {
            return !TrayPos.equals(RouteFor) || RouteOpen;
        }
        return probeHome(1);
    }

    private boolean probeHome(int Accuracy) {
        Path Route = prober().routeTo(BlockPos.containing(home()), Accuracy, TrayProbeRange);
        RouteOpen = Route != null && Route.canReach();
        RouteFor = TrayPos;
        RouteTick = tickCount;
        return RouteOpen;
    }

    private Scp049Navigation prober() {
        if (Prober == null) {
            Prober = new Scp049Navigation(this, level());
            Prober.setMaxVisitedNodesMultiplier(TrayProbeNodes);
        }
        return Prober;
    }

    public int curedCount() {
        return Cured.size();
    }

    public List<UUID> cured() {
        return List.copyOf(Cured);
    }

    @Nullable
    public UUID heldId() {
        return Held;
    }

    @Nullable
    public UUID quarryId() {
        return Quarry;
    }

    public boolean isAfraid() {
        return Routine == Phase.Flee || Routine == Phase.Wary;
    }

    public boolean isBrave() {
        return tickCount < BraveUntil;
    }

    public int alarms() {
        return Alarms;
    }

    public int corners() {
        return Corners;
    }

    public int shunnedSpots() {
        return Fear.count();
    }

    public boolean shuns(int X, int Y, int Z) {
        return !level().isClientSide && Routine != Phase.Flee && !isBrave() && Fear.shuns(X, Y, Z);
    }

    public boolean locksMovement() {
        byte Current = action();
        return Current == ActionTouch || Current == ActionBow || Current == ActionCommand || Current == ActionFlinch || Current == ActionSurgery || isFelling();
    }

    public boolean isFelling() {
        byte Current = action();
        return Current == ActionFell || Current == ActionFellFree;
    }

    private void setMode(byte Value) {
        if (entityData.get(ModeData) != Value) {
            entityData.set(ModeData, Value);
        }
    }

    private void setHurried(boolean Value) {
        if (entityData.get(HurriedData) != Value) {
            entityData.set(HurriedData, Value);
        }
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (locksMovement()) {
            getNavigation().stop();
        }
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    @Nullable
    public String gestureClip() {
        return switch (action()) {
            case ActionTouch, ActionFell -> "touch";
            case ActionFellFree -> "touch_left";
            case ActionDoor -> "door";
            default -> null;
        };
    }

    @Nullable
    public String bodyClip() {
        return switch (action()) {
            case ActionBow -> "bow";
            case ActionCommand -> "command";
            case ActionFlinch -> "flinch";
            case ActionSurgery -> "surgery";
            default -> null;
        };
    }

    public void reachForDoor() {
        if (action() == ActionNone) {
            setAction(ActionDoor, DoorTicks);
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    @Override
    public void tick() {
        super.tick();
        if (isFelling()) {
            yBodyRot = Mth.approachDegrees(yBodyRot, yHeadRot, FellTurn);
        }
        if (level().isClientSide) {
            clientTick();
        }
    }

    private void clientTick() {
        Scp049Gait Active = activeGait();
        for (Scp049Gait Other : new Scp049Gait[]{walkStride(), fastStride(), dragStride()}) {
            if (Other != Active) {
                Other.reset();
            }
        }
        Active.tick(this);
        if (QuietLeft > 0) {
            QuietLeft--;
        }
        int Surgery = action() == ActionSurgery ? (int) Math.floor(tickCount + TickOrigin - entityData.get(ActionStartData)) : -1;
        if (Surgery >= 0) {
            if (SurgeryTick < 0 && Surgery > 2) {
                SurgeryTick = Surgery - 1;
            }
            Scp049Surgery.clientTick(this, SurgeryTick, Surgery);
            SurgeryTick = Surgery;
        } else {
            SurgeryTick = -1;
        }
        if (Breath == null) {
            Breath = new Scp049Breath();
        }
        byte Mode = mode();
        Scp049Breath.Effort Effort = Mode == ModeDrag || Mode == ModeSurgery ? Scp049Breath.Effort.Working : Active.striding() ? Scp049Breath.Effort.Moving
                : Scp049Breath.Effort.Idle;
        Breath.tick(this, Effort, Active, breathQuiet(Surgery));
    }

    private boolean breathQuiet(int Surgery) {
        byte Current = action();
        if (QuietLeft > 0 || hurtTime > 0 || Current != ActionNone && Current != ActionSurgery) {
            return true;
        }
        return Surgery >= 0 && Scp049Surgery.busy(Surgery, Scp049Breath.QuietAhead);
    }

    @Nullable
    public BlockPos syncedTray() {
        return entityData.get(TrayData).orElse(null);
    }

    @Nullable
    public Entity syncedHeld() {
        int Id = entityData.get(HeldData);
        return Id >= 0 ? level().getEntity(Id) : null;
    }

    public boolean isCarrying(@Nullable Entity Other) {
        if (Other == null) {
            return false;
        }
        if (level().isClientSide) {
            return entityData.get(HeldData) == Other.getId();
        }
        return Held != null && Held.equals(Other.getUUID());
    }

    private void syncTray() {
        Optional<BlockPos> Value = Optional.ofNullable(TrayPos);
        if (!entityData.get(TrayData).equals(Value)) {
            entityData.set(TrayData, Value);
        }
    }

    private void syncHeld(@Nullable LivingEntity Victim) {
        int Value = Victim != null ? Victim.getId() : -1;
        if (entityData.get(HeldData) != Value) {
            entityData.set(HeldData, Value);
        }
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        level().broadcastEntityEvent(this, VoiceEvent);
    }

    @Override
    public void handleEntityEvent(byte Id) {
        if (Id == VoiceEvent) {
            QuietLeft = VoiceQuiet;
            return;
        }
        super.handleEntityEvent(Id);
    }

    @Override
    public void push(Entity Other) {
        if (!isCarrying(Other)) {
            super.push(Other);
        }
    }

    @Override
    protected void doPush(Entity Other) {
        if (!isCarrying(Other)) {
            super.doPush(Other);
        }
    }

    private Scp049Gait activeGait() {
        if (mode() == ModeDrag) {
            return dragStride();
        }
        return isHurried() ? fastStride() : walkStride();
    }

    private Scp049Gait walkStride() {
        if (WalkStride == null) {
            WalkStride = new Scp049Gait(WalkGait);
        }
        return WalkStride;
    }

    private Scp049Gait fastStride() {
        if (FastStride == null) {
            FastStride = new Scp049Gait(StrideGait);
        }
        return FastStride;
    }

    private Scp049Gait dragStride() {
        if (DragStride == null) {
            DragStride = new Scp049Gait(DragGait);
        }
        return DragStride;
    }

    @Nullable
    public Scp049Gait gaitFor(String Clip) {
        if (!level().isClientSide) {
            return null;
        }
        if (WalkGait.Clip().equals(Clip)) {
            return walkStride();
        }
        if (StrideGait.Clip().equals(Clip)) {
            return fastStride();
        }
        if (DragGait.Clip().equals(Clip)) {
            return dragStride();
        }
        return null;
    }

    public boolean isStriding() {
        return activeGait().striding();
    }

    public void setGaitPartial(float Partial) {
        RenderPartial = Partial;
        walkStride().setPartial(Partial);
        fastStride().setPartial(Partial);
        dragStride().setPartial(Partial);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (Cooldown > 0) {
            Cooldown--;
        }
        if ((tickCount + getId()) % TrayCheck == 0) {
            tickTray(World);
        }
        tickRegen();
        tickAction(World);
        if ((tickCount + getId()) % FearInterval == 0) {
            tickFear(World);
        }
        LivingEntity Carried = tickHeld(World);
        Portals.tick(World, Carried);
        if (PendingLoss && action() == ActionNone) {
            resolveLoss(World);
        }
        if (!PendingLoss && !isAfraid() && (tickCount + getId()) % ScanInterval == 0) {
            scan(World);
        }
        if (!PendingLoss && (tickCount + getId()) % ZombieInterval == 0) {
            scanZombies(World);
        }
        if (FellCooldown > 0) {
            FellCooldown--;
        }
        if ((tickCount + getId()) % ScanInterval == 0) {
            scanGolems(World);
        }
        PhaseTicks++;
        if (PendingLoss) {
            getNavigation().stop();
        } else if (isAfraid() || !duelTick(World)) {
            switch (Routine) {
                case Wait -> waitTick(World);
                case Seek -> seekTick(World);
                case Drag -> dragTick(World);
                case Operate -> operateTick(World);
                case Greet -> greetTick(World);
                case Pursue -> pursueTick(World);
                case Return -> returnTick(World);
                case Roam -> roamTick(World);
                case Flee -> fleeTick(World);
                case Wary -> waryTick(World);
            }
        }
        tickRises(World);
        tickCollapses(World);
        if ((tickCount + getId()) % 200 == 0) {
            long Now = World.getGameTime();
            Ignored.values().removeIf(Until -> Until < Now);
        }
    }

    private void setRoutine(Phase Next) {
        if (Roaming && (Next == Phase.Return || Next == Phase.Wait || Next == Phase.Seek)) {
            Next = Phase.Roam;
        }
        if (Routine != Next) {
            if (Routine == Phase.Operate) {
                FieldSpot = null;
                FieldPatient = null;
            }
            Routine = Next;
            PhaseTicks = 0;
            StuckTicks = 0;
            StuckFrom = null;
            resetHomeWatch();
        }
        setHurried(Next == Phase.Pursue || Next == Phase.Flee);
        setMode(Next == Phase.Drag ? ModeDrag : Next == Phase.Operate ? ModeSurgery : ModeIdle);
    }

    private void tickRegen() {
        if (tickCount - LastHurtTick > RegenDelay && tickCount % RegenInterval == 0 && getHealth() < getMaxHealth()) {
            heal(1.0f);
        }
    }

    private void tickAction(ServerLevel World) {
        if (ActionTicks <= 0) {
            return;
        }
        byte Current = action();
        int Elapsed = (int) actionElapsed();
        if (locksMovement()) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
        if (Current == ActionTouch) {
            touchTick(World, Elapsed);
        } else if (Current == ActionFell || Current == ActionFellFree) {
            fellTick(World, Elapsed);
        } else if (Current == ActionCommand && CommandFocus != null && CommandFocus.isAlive()) {
            getLookControl().setLookAt(CommandFocus, 30.0f, 30.0f);
        } else if ((Current == ActionBow || Current == ActionCommand) && quarry(World) instanceof LivingEntity Target) {
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
        }
        ActionTicks--;
        if (ActionTicks <= 0) {
            entityData.set(ActionData, ActionNone);
            actionFinished(World, Current);
        }
    }

    private void actionFinished(ServerLevel World, byte Kind) {
        if (Kind == ActionTouch) {
            TouchTarget = null;
        } else if (Kind == ActionFell || Kind == ActionFellFree) {
            FellTarget = null;
        } else if (Kind == ActionBow && Routine == Phase.Greet) {
            setAction(ActionCommand, CommandTicks);
            LivingEntity Target = quarry(World);
            playSound(Scp049Registry.Command.get(), 1.4f, 0.95f + random.nextFloat() * 0.1f);
            if (Target != null) {
                commandCured(World, Target);
            }
        } else if (Kind == ActionCommand) {
            CommandFocus = null;
            if (Routine == Phase.Greet) {
                setRoutine(Phase.Pursue);
            }
        }
    }

    private void tickTray(ServerLevel World) {
        if (TrayPos != null) {
            if (!World.isLoaded(TrayPos)) {
                return;
            }
            if (World.getBlockEntity(TrayPos) instanceof Scp049TrayBlockEntity Tray && Tray.canBind(getUUID())) {
                Tray.bind(this);
                if (WorkSpot == null || (tickCount + getId()) % 200 < TrayCheck) {
                    computeSpots(World);
                }
                return;
            }
            loseTray();
        }
        BlockPos Found = findTray(World);
        if (Found != null) {
            linkTray(World, Found, false);
            return;
        }
        if (!HadTray) {
            if (isAfraid()) {
                return;
            }
            if (onGround() && Routine != Phase.Operate && placeTray(World)) {
                return;
            }
            if (++PlaceTries < FirstTrayTries) {
                return;
            }
            HadTray = true;
        }
        if (!Roaming) {
            beginRoaming(World, "lost");
        }
    }

    private void loseTray() {
        TrayPos = null;
        WorkSpot = null;
        PatientSpot = null;
        Area = null;
        NewTrayAt = -1L;
        resetProgress();
        syncTray();
    }

    public boolean needsTray(ServerLevel World) {
        if (TrayPos == null || Roaming) {
            return true;
        }
        return World.isLoaded(TrayPos) && !(World.getBlockEntity(TrayPos) instanceof Scp049TrayBlockEntity Tray && Tray.canBind(getUUID()));
    }

    public void linkTray(ServerLevel World, BlockPos Pos, boolean PlayerPlaced) {
        if (TrayPos != null && !TrayPos.equals(Pos) && World.isLoaded(TrayPos) && World.getBlockEntity(TrayPos) instanceof Scp049TrayBlockEntity Old) {
            Old.release(getUUID());
        }
        bindTray(World, Pos);
        NewTrayAt = PlayerPlaced ? World.getGameTime() : -1L;
        HomeFails = 0;
        resetProgress();
        if (Roaming) {
            endRoaming();
        } else if (Routine == Phase.Wait || Routine == Phase.Seek) {
            Prey = null;
            setRoutine(Phase.Return);
        }
    }

    @Nullable
    private BlockPos findTray(ServerLevel World) {
        BlockPos Centre = blockPosition();
        int ChunkX = Centre.getX() >> 4;
        int ChunkZ = Centre.getZ() >> 4;
        BlockPos Best = null;
        double BestDistance = TrayReach * TrayReach;
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                if (!World.hasChunk(ChunkX + Dx, ChunkZ + Dz)) {
                    continue;
                }
                LevelChunk Chunk = World.getChunk(ChunkX + Dx, ChunkZ + Dz);
                for (BlockEntity Found : Chunk.getBlockEntities().values()) {
                    if (Found instanceof Scp049TrayBlockEntity Tray && Tray.canBind(getUUID())) {
                        double Distance = Found.getBlockPos().distSqr(Centre);
                        if (Distance < BestDistance) {
                            BestDistance = Distance;
                            Best = Found.getBlockPos();
                        }
                    }
                }
            }
        }
        return Best;
    }

    public void bindTray(ServerLevel World, BlockPos Pos) {
        TrayPos = Pos.immutable();
        Area = null;
        HadTray = true;
        syncTray();
        if (World.getBlockEntity(TrayPos) instanceof Scp049TrayBlockEntity Tray) {
            Tray.bind(this);
        }
        computeSpots(World);
        resetHomeWatch();
        restrictTo(TrayPos, (int) LeashRange);
        for (Scp049Cured Follower : followers(World)) {
            Follower.bind(this, TrayPos);
        }
    }

    private boolean placeTray(ServerLevel World) {
        BlockPos Here = blockPosition();
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                if (Dx == 0 && Dz == 0) {
                    continue;
                }
                for (int Dy = 1; Dy >= -1; Dy--) {
                    BlockPos Cell = Here.offset(Dx, Dy, Dz);
                    if (!Scp049Discovery.canHoldTray(World, Cell) || getBoundingBox().intersects(new AABB(Cell))) {
                        continue;
                    }
                    double Distance = Cell.distSqr(Here);
                    if (Distance < BestDistance) {
                        BestDistance = Distance;
                        Best = Cell;
                    }
                }
            }
        }
        if (Best == null) {
            return false;
        }
        Direction Toward = Direction.getNearest(getX() - (Best.getX() + 0.5), 0.0, getZ() - (Best.getZ() + 0.5));
        World.setBlock(Best, Scp049Registry.Tray.get().defaultBlockState().setValue(Scp049TrayBlock.Facing, Toward.getAxis().isHorizontal() ? Toward : Direction.NORTH),
                Block.UPDATE_ALL);
        World.playSound(null, Best, Scp049Registry.Tools.get(), SoundSource.HOSTILE, 0.8f, 0.9f);
        bindTray(World, Best);
        NewTrayAt = -1L;
        resetProgress();
        return true;
    }

    private void computeSpots(ServerLevel World) {
        if (TrayPos == null) {
            return;
        }
        BlockState State = World.getBlockState(TrayPos);
        Direction Front = State.hasProperty(Scp049TrayBlock.Facing) ? State.getValue(Scp049TrayBlock.Facing) : Direction.NORTH;
        List<Direction> Order = List.of(Front, Front.getClockWise(), Front.getCounterClockWise(), Front.getOpposite());
        Vec3 Centre = Vec3.atBottomCenterOf(TrayPos);
        Direction WorkSide = null;
        BlockPos WorkCell = null;
        for (Direction Side : Order) {
            BlockPos Cell = standableBeside(World, TrayPos.relative(Side));
            if (Cell != null) {
                WorkSide = Side;
                WorkCell = Cell;
                break;
            }
        }
        if (WorkCell == null) {
            WorkSpot = Centre.add(Front.getStepX() * 1.1, 0.0, Front.getStepZ() * 1.1);
            PatientSpot = Centre.add(Front.getClockWise().getStepX() * 1.1, 0.0, Front.getClockWise().getStepZ() * 1.1);
            return;
        }
        WorkSpot = Vec3.atBottomCenterOf(WorkCell).add(-WorkSide.getStepX() * 0.15, 0.0, -WorkSide.getStepZ() * 0.15);
        PatientSpot = null;
        for (Direction Side : List.of(WorkSide.getClockWise(), WorkSide.getCounterClockWise(), WorkSide.getOpposite())) {
            BlockPos Cell = standableBeside(World, TrayPos.relative(Side));
            if (Cell != null) {
                PatientSpot = Vec3.atBottomCenterOf(Cell);
                break;
            }
        }
        if (PatientSpot == null) {
            Direction Aside = WorkSide.getClockWise();
            PatientSpot = WorkSpot.add(Aside.getStepX() * 0.9, 0.0, Aside.getStepZ() * 0.9);
        }
    }

    @Nullable
    private static BlockPos standableBeside(ServerLevel World, BlockPos Cell) {
        for (int Dy = 0; Dy >= -1; Dy--) {
            BlockPos Candidate = Cell.above(Dy);
            if (LeadPlanner.isDryStandable(World, Candidate)) {
                return Candidate;
            }
        }
        return null;
    }

    public AABB area(ServerLevel World) {
        long Now = World.getGameTime();
        if (Area != null && Now - AreaStamp < 1200L) {
            return Area;
        }
        AreaStamp = Now;
        Vec3 Centre = TrayPos != null ? Vec3.atCenterOf(TrayPos) : position();
        AABB Found = new AABB(Centre, Centre).inflate(AreaRadius, AreaDepth, AreaRadius);
        if (TrayPos != null && World.isLoaded(TrayPos)) {
            StructureStart Village = World.structureManager().getStructureWithPieceAt(TrayPos, StructureTags.VILLAGE);
            if (Village.isValid()) {
                BoundingBox Box = Village.getBoundingBox();
                Found = Found.minmax(new AABB(Box.minX(), Box.minY(), Box.minZ(), Box.maxX() + 1, Box.maxY() + 1, Box.maxZ() + 1).inflate(VillagePadding));
            }
        }
        Area = Found;
        return Found;
    }

    public Vec3 home() {
        if (TrayPos == null) {
            return position();
        }
        return WorkSpot != null ? WorkSpot : Vec3.atBottomCenterOf(TrayPos);
    }

    private void resetHomeWatch() {
        HomeBest = Double.MAX_VALUE;
        HomeBestTick = tickCount;
        HomeReplanTick = tickCount;
        HomeRoute = null;
    }

    private boolean homeWatch(ServerLevel World, boolean MayGiveUp) {
        Path Route = getNavigation().getPath();
        if (Route != null && !Route.isDone()) {
            double Remaining = remainingPath();
            if (Route != HomeRoute) {
                HomeRoute = Route;
                if (Route.canReach() && HomeBest < Double.MAX_VALUE && Remaining > HomeBest) {
                    HomeBest = Remaining;
                }
            }
            if (Remaining < HomeBest - HomeStep) {
                HomeBest = Remaining;
                HomeBestTick = tickCount;
            }
        }
        int Idle = tickCount - HomeBestTick;
        if (MayGiveUp && Idle >= HomeGiveUp) {
            resetHomeWatch();
            return true;
        }
        if (Idle >= HomeReplan && tickCount - HomeReplanTick >= HomeReplan) {
            HomeReplanTick = tickCount;
            HomeReplans++;
            getNavigation().stop();
            computeSpots(World);
            Vec3 Spot = home();
            getNavigation().moveTo(Spot.x, Spot.y, Spot.z, StrollSpeed);
        }
        return false;
    }

    private boolean atHome() {
        Vec3 Spot = home();
        double Dx = Spot.x - getX();
        double Dz = Spot.z - getZ();
        return Dx * Dx + Dz * Dz <= ArriveRange * ArriveRange && Math.abs(Spot.y - getY()) < 1.5;
    }

    private boolean nearHome() {
        Vec3 Spot = home();
        return distanceToSqr(Spot) <= HomeRange * HomeRange;
    }

    private void faceTray() {
        if (TrayPos != null) {
            Vec3 Centre = Vec3.atCenterOf(TrayPos);
            getLookControl().setLookAt(Centre.x, Centre.y - 0.1, Centre.z, 20.0f, 30.0f);
        }
    }

    private void moveHome(double SpeedModifier) {
        Vec3 Spot = home();
        if (getNavigation().isDone() || PhaseTicks % 20 == 0) {
            getNavigation().moveTo(Spot.x, Spot.y, Spot.z, SpeedModifier);
        }
    }

    private boolean stuck(int Limit) {
        if (PhaseTicks % StuckCheck != 0) {
            return false;
        }
        Vec3 Here = position();
        if (StuckFrom != null && StuckFrom.distanceToSqr(Here) < 0.25) {
            StuckTicks += StuckCheck;
        } else {
            StuckTicks = 0;
        }
        StuckFrom = Here;
        return StuckTicks >= Limit;
    }

    private void waitTick(ServerLevel World) {
        if (TrayPos != null && Fear.covers(home())) {
            beginRoaming(World, "shunned");
            return;
        }
        if (!nearHome()) {
            setRoutine(Phase.Return);
            return;
        }
        if (atHome()) {
            resetHomeWatch();
        } else if (nearWork()) {
            resetHomeWatch();
            if (getNavigation().isDone() && PhaseTicks % 20 == 0) {
                Vec3 Spot = home();
                getNavigation().moveTo(Spot.x, Spot.y, Spot.z, StrollSpeed);
            }
        } else {
            if (getNavigation().isDone()) {
                Vec3 Spot = home();
                getNavigation().moveTo(Spot.x, Spot.y, Spot.z, StrollSpeed);
                Path Route = getNavigation().getPath();
                if (Route == null || !Route.canReach()) {
                    setRoutine(Phase.Return);
                    return;
                }
            }
            homeWatch(World, false);
            if (tickCount - HomeBestTick >= HomeGiveUp) {
                setRoutine(Phase.Return);
                return;
            }
        }
        if (--ModeLeft <= 0) {
            boolean Tidy = mode() != ModeTidy && TrayPos != null;
            setMode(Tidy ? ModeTidy : ModeIdle);
            ModeLeft = Tidy ? 60 + random.nextInt(80) : 100 + random.nextInt(140);
            if (Tidy) {
                World.playSound(null, getX(), getY() + 1.0, getZ(), Scp049Registry.Tools.get(), SoundSource.HOSTILE, 0.45f, 1.05f + random.nextFloat() * 0.1f);
            }
        }
        if (mode() == ModeTidy) {
            faceTray();
            if (random.nextInt(30) == 0 && TrayPos != null) {
                World.playSound(null, TrayPos, Scp049Registry.Tools.get(), SoundSource.HOSTILE, 0.3f, 1.1f + random.nextFloat() * 0.15f);
            }
        }
        if (PhaseTicks % VillagerInterval == 0) {
            Villager Found = findPatient(World);
            if (Found != null) {
                Prey = Found.getUUID();
                setRoutine(Phase.Seek);
            }
        }
    }

    @Nullable
    private Villager findPatient(ServerLevel World) {
        if (!trayUsable(World)) {
            return null;
        }
        long Now = World.getGameTime();
        Villager Best = null;
        double BestDistance = Double.MAX_VALUE;
        Vec3 Home = home();
        for (Villager Candidate : World.getEntitiesOfClass(Villager.class, area(World), Scp049Persons::isPatient)) {
            if (Scp049Hold.isHeld(Candidate) || Scp049Fear.isBearer(Candidate) || Ignored.getOrDefault(Candidate.getUUID(), Long.MIN_VALUE) > Now
                    || Candidate.distanceToSqr(Home) > TrayRange * TrayRange) {
                continue;
            }
            double Distance = distanceToSqr(Candidate);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    public int patientsLeft(ServerLevel World) {
        return World.getEntitiesOfClass(Villager.class, area(World), Scp049Persons::isPatient).size();
    }

    private void seekTick(ServerLevel World) {
        if (!trayUsable(World)) {
            Prey = null;
            Regrab = null;
            setRoutine(Phase.Return);
            return;
        }
        Entity Found = Prey != null ? World.getEntity(Prey) : null;
        boolean Regrabbing = Found != null && Found.getUUID().equals(Regrab) && Found.distanceToSqr(this) <= LoseRange * LoseRange;
        if (!(Found instanceof Villager Patient) || !Scp049Persons.isPatient(Patient) || Scp049Hold.isHeld(Patient) || !Regrabbing && !area(World).contains(Patient.position())) {
            Prey = null;
            Regrab = null;
            setRoutine(Phase.Return);
            return;
        }
        getLookControl().setLookAt(Patient, 30.0f, 30.0f);
        if (action() == ActionTouch) {
            return;
        }
        if (Cooldown <= 0 && canReach(Patient, TouchStart)) {
            beginTouch(Patient);
            return;
        }
        if (getNavigation().isDone() || PhaseTicks % 10 == 0) {
            getNavigation().moveTo(Patient, SeekSpeed);
        }
        if (stuck(StuckGiveUp)) {
            Ignored.put(Patient.getUUID(), World.getGameTime() + IgnoreTicks);
            Prey = null;
            setRoutine(Phase.Return);
        }
    }

    private boolean withinTouch(LivingEntity Target, double Range) {
        double Edge = (getBbWidth() + Target.getBbWidth()) * 0.5;
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz) - Edge;
        return Flat <= Range && Math.abs(Target.getY() - getY()) < 2.0;
    }

    private double flatGap(LivingEntity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz) - (getBbWidth() + Target.getBbWidth()) * 0.5;
    }

    public boolean canReach(LivingEntity Target, double Range) {
        if (flatGap(Target) > Range || Target.getY() - getY() > ReachRise || Target.getBoundingBox().maxY < getY() + ReachFloor) {
            return false;
        }
        return handClear(Target);
    }

    private boolean handClear(LivingEntity Target) {
        Vec3 Hand = new Vec3(getX(), getY() + HandHeight, getZ());
        AABB Body = Target.getBoundingBox();
        Vec3 Point = new Vec3(Mth.clamp(Hand.x, Body.minX + HandInset, Body.maxX - HandInset), Mth.clamp(Hand.y, Body.minY + HandInset, Body.maxY - HandInset),
                Mth.clamp(Hand.z, Body.minZ + HandInset, Body.maxZ - HandInset));
        return level().clip(new ClipContext(Hand, Point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    private void beginTouch(LivingEntity Target) {
        TouchTarget = Target;
        setAction(ActionTouch, TouchTicks);
        playSound(Scp049Registry.Touch.get(), 0.9f, 0.95f + random.nextFloat() * 0.1f);
    }

    private void touchTick(ServerLevel World, int Elapsed) {
        LivingEntity Target = TouchTarget;
        if (Target == null) {
            return;
        }
        getLookControl().setLookAt(Target, 40.0f, 40.0f);
        if (Elapsed != TouchHitTick) {
            return;
        }
        boolean Valid = Target.isAlive() && !Target.isRemoved() && !Scp049Hold.isHeld(Target) && canReach(Target, TouchReach);
        boolean Person = Scp049Persons.isPerson(Target, Scp049Persons.sparesPlayers(this));
        if (!Valid || !Person && !Scp049Persons.isPatient(Target) || Held != null) {
            Cooldown = TouchCooldown;
            return;
        }
        if (!trayUsable(World)) {
            if (Person) {
                killByTouch(World, Target);
            } else {
                Cooldown = TouchCooldown;
            }
            return;
        }
        seize(World, Target, Person);
    }

    private void killByTouch(ServerLevel World, LivingEntity Target) {
        speak(Target, "touch");
        World.playSound(null, Target.getX(), Target.getY() + 1.0, Target.getZ(), Scp049Registry.Touch.get(), SoundSource.HOSTILE, 1.0f, 0.6f);
        World.sendParticles(ParticleTypes.ENCHANTED_HIT, Target.getX(), Target.getY() + Target.getBbHeight() * 0.6, Target.getZ(), 8, 0.25, 0.3, 0.25, 0.05);
        World.sendParticles(Scp049Registry.CureSpore.get(), Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), 10, 0.3, 0.4, 0.3, 0.01);
        Target.invulnerableTime = 0;
        Target.hurt(damageSources().source(Scp049Registry.CureDamage, this), Float.MAX_VALUE);
        Cooldown = Math.max(Cooldown, TouchCooldown);
        boolean Down = !Target.isAlive() || Target instanceof ServerPlayer Controller && ControlManager.hasPendingDeath(Controller);
        if (!Down) {
            return;
        }
        TouchKills++;
        if (Target.getUUID().equals(Quarry)) {
            endPersonRoutine(World);
        }
    }

    private void seize(ServerLevel World, LivingEntity Target, boolean Person) {
        Scp049Hold.seize(this, Target);
        if (Person) {
            Scp049Hold.weaken(Target);
            speak(Target, "touch");
        }
        Held = Target.getUUID();
        Trail.clear();
        Prey = null;
        Regrab = null;
        syncHeld(Target);
        resetProgress();
        Replans = 0;
        Nudges = 0;
        World.playSound(null, Target.getX(), Target.getY() + 1.0, Target.getZ(), Scp049Registry.Touch.get(), SoundSource.HOSTILE, 1.0f, 0.7f);
        World.sendParticles(ParticleTypes.ENCHANTED_HIT, Target.getX(), Target.getY() + Target.getBbHeight() * 0.6, Target.getZ(), 8, 0.25, 0.3, 0.25, 0.05);
        setRoutine(Phase.Drag);
    }

    @Nullable
    public LivingEntity held(ServerLevel World) {
        return Held != null && World.getEntity(Held) instanceof LivingEntity Body ? Body : null;
    }

    @Nullable
    private LivingEntity tickHeld(ServerLevel World) {
        if (Held == null) {
            return null;
        }
        LivingEntity Victim = held(World);
        if (Victim == null || Victim.isRemoved()) {
            loseHeld(World, "vanished", false, null);
            return null;
        }
        if (!Victim.isAlive()) {
            loseHeld(World, "died", false, null);
            return null;
        }
        if (!Targeting.isTargetable(Victim)) {
            loseHeld(World, "petrified", false, null);
            return null;
        }
        if (Victim instanceof Player User && (User.isCreative() || User.isSpectator())) {
            loseHeld(World, "untouchable", false, null);
            return null;
        }
        if (distanceTo(Victim) > TetherBreak) {
            loseHeld(World, "pulled_free", true, recentAttacker());
            return null;
        }
        if (Scp049Hold.heldBy(Victim, this)) {
            Scp049Hold.lock(Victim);
        } else {
            Scp049Hold.seize(this, Victim);
        }
        if (!(Victim instanceof Villager)) {
            Scp049Hold.weaken(Victim);
        }
        Vec3 Here = position();
        if (Trail.isEmpty() || Trail.peekFirst().distanceToSqr(Here) > 0.0025) {
            Trail.addFirst(Here);
            while (Trail.size() > TrailLimit) {
                Trail.removeLast();
            }
        }
        syncHeld(Victim);
        Vec3 Patient = FieldPatient != null ? FieldPatient : PatientSpot;
        Vec3 Point = Routine == Phase.Operate && Patient != null ? Patient : trailPoint(TetherLength);
        Portals.assist(World, Victim, Point);
        Scp049Hold.tether(Victim, Point, DragPull, position());
        if (tickCount % 12 == 0 && Routine == Phase.Drag && Victim.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            World.playSound(null, Victim.getX(), Victim.getY(), Victim.getZ(), Scp049Registry.Drag.get(), SoundSource.HOSTILE, 0.5f, 0.9f + random.nextFloat() * 0.2f);
        }
        return Victim;
    }

    private Vec3 trailPoint(double Length) {
        Vec3 Previous = position();
        double Walked = 0.0;
        for (Vec3 Point : Trail) {
            double Step = Previous.distanceTo(Point);
            if (Walked + Step >= Length && Step > 1.0e-6) {
                double Share = (Length - Walked) / Step;
                return Previous.add(Point.subtract(Previous).scale(Share));
            }
            Walked += Step;
            Previous = Point;
        }
        Vec3 Back = Vec3.directionFromRotation(0.0f, yBodyRot).scale(-(Length - Walked));
        return Previous.add(Back.x, 0.0, Back.z);
    }

    private void releaseHeld(ServerLevel World) {
        LivingEntity Victim = held(World);
        if (Victim != null) {
            Scp049Hold.release(Victim);
        }
        Held = null;
        Trail.clear();
        syncHeld(null);
        resetProgress();
        Replans = 0;
        Nudges = 0;
        OperateLeft = 0;
        Portals.closeAll(World);
        if (action() == ActionSurgery) {
            entityData.set(ActionData, ActionNone);
            ActionTicks = 0;
        }
    }

    private void loseHeld(ServerLevel World, String Reason, boolean Struck, @Nullable LivingEntity Attacker) {
        LivingEntity Victim = held(World);
        UUID Lost = Held;
        if (Victim != null && Struck) {
            speak(Victim, "rescued");
        }
        releaseHeld(World);
        Cooldown = Math.max(Cooldown, RegrabTicks);
        LossReason = Reason;
        LossOutcome = "pending";
        LossTick = tickCount;
        LostId = Victim != null && Victim.isAlive() ? Lost : null;
        LostAttacker = Attacker != null ? Attacker.getUUID() : null;
        if (Struck) {
            setAction(ActionFlinch, FlinchTicks);
            playSound(Scp049Registry.Hurt.get(), 1.0f, 0.8f);
        }
        setRoutine(Phase.Return);
        PendingLoss = true;
        if (!Struck) {
            resolveLoss(World);
        }
    }

    private void resolveLoss(ServerLevel World) {
        PendingLoss = false;
        LivingEntity Lost = LostId != null && World.getEntity(LostId) instanceof LivingEntity Body ? Body : null;
        LivingEntity Attacker = LostAttacker != null && World.getEntity(LostAttacker) instanceof LivingEntity Body ? Body : null;
        LostId = null;
        LostAttacker = null;
        retarget(World, Lost, Attacker);
    }

    @Nullable
    private LivingEntity recentAttacker() {
        LivingEntity Attacker = getLastHurtByMob();
        return Attacker != null && tickCount - getLastHurtByMobTimestamp() <= StruckMemory ? Attacker : null;
    }

    private boolean canReacquire(ServerLevel World, @Nullable LivingEntity Lost) {
        if (Lost == null || !Lost.isAlive() || Lost.isRemoved() || Lost.level() != level() || Scp049Hold.isHeld(Lost) || !Scp049Persons.isNoticeable(Lost)) {
            return false;
        }
        if (!Scp049Persons.isPerson(Lost, Scp049Persons.sparesPlayers(this)) && !(Scp049Persons.isPatient(Lost) && trayUsable(World))) {
            return false;
        }
        if (Lost.distanceToSqr(this) > LoseRange * LoseRange || hasWorkableTray() && Lost.distanceToSqr(Vec3.atCenterOf(TrayPos)) > LeashRange * LeashRange) {
            return false;
        }
        if (Ignored.getOrDefault(Lost.getUUID(), Long.MIN_VALUE) > level().getGameTime()) {
            return false;
        }
        if (hasLineOfSight(Lost)) {
            return true;
        }
        Path Route = getNavigation().createPath(Lost, 1);
        return Route != null && Route.canReach();
    }

    private void retarget(ServerLevel World, @Nullable LivingEntity Lost, @Nullable LivingEntity Attacker) {
        boolean Spare = Scp049Persons.sparesPlayers(this);
        LivingEntity Interloper = Attacker != null && Attacker != Lost && Attacker.isAlive() && Scp049Persons.isPerson(Attacker, Spare) && !Scp049Hold.isHeld(Attacker) ? Attacker : null;
        if (Lost != null && canReacquire(World, Lost)) {
            if (Scp049Persons.isPerson(Lost, Spare)) {
                resumePerson(Lost);
                if (Interloper != null) {
                    delegate(World, Interloper);
                }
                LossOutcome = "same_person";
                return;
            }
            LivingEntity Other = Interloper != null ? Interloper : findPerson(World);
            if (Other == null) {
                Prey = Lost.getUUID();
                Regrab = Prey;
                setRoutine(Phase.Seek);
                LossOutcome = "same_villager";
                return;
            }
            Interloper = Other;
        }
        LivingEntity Person = Interloper != null ? Interloper : findPerson(World);
        if (Person != null) {
            if (Quarry != null && Quarry.equals(Person.getUUID())) {
                resumePerson(Person);
            } else {
                beginPersonRoutine(World, Person);
            }
            LossOutcome = "new_person";
            return;
        }
        LivingEntity Chased = quarry(World);
        if (Chased != null && Scp049Persons.isPerson(Chased, Spare) && !Scp049Hold.isHeld(Chased) && canReacquire(World, Chased)) {
            resumePerson(Chased);
            LossOutcome = "same_person";
            return;
        }
        if (Quarry != null) {
            endPersonRoutine(World);
        }
        Villager Next = findPatient(World);
        if (Next != null) {
            Prey = Next.getUUID();
            setRoutine(Phase.Seek);
            LossOutcome = "new_villager";
            return;
        }
        setRoutine(Phase.Return);
        LossOutcome = "routine";
    }

    private void resumePerson(LivingEntity Target) {
        Quarry = Target.getUUID();
        Prey = null;
        Regrab = Target.getUUID();
        LastSeenTick = tickCount;
        PursueBest = Double.MAX_VALUE;
        PursueBestTick = tickCount;
        setRoutine(Phase.Pursue);
    }

    private void delegate(ServerLevel World, LivingEntity Target) {
        Threat = Target.getUUID();
        ThreatUntil = tickCount + ThreatTicks;
        List<Scp049Cured> Followers = followers(World);
        for (Scp049Cured Follower : Followers) {
            Follower.order(Target);
        }
        if (!Followers.isEmpty()) {
            announceOrder(Target, Held == null);
        }
    }

    @Nullable
    private LivingEntity threat(ServerLevel World) {
        if (Threat == null) {
            return null;
        }
        if (tickCount > ThreatUntil || !(World.getEntity(Threat) instanceof LivingEntity Body) || !Scp049Persons.isPerson(Body, Scp049Persons.sparesPlayers(this))
                || Scp049Hold.isHeld(Body) || Body.distanceToSqr(this) > LoseRange * LoseRange) {
            Threat = null;
            return null;
        }
        return Body;
    }

    private void announceOrder(Entity Focus, boolean Gesture) {
        if (tickCount - LastCommandTick < CommandGap) {
            return;
        }
        LastCommandTick = tickCount;
        CommandFocus = Focus;
        if (Gesture && action() == ActionNone && Routine != Phase.Operate && Held == null && !isAfraid()) {
            setAction(ActionCommand, CommandTicks);
        }
        playSound(Scp049Registry.Command.get(), 1.1f, 0.95f + random.nextFloat() * 0.1f);
        playSound(Scp049Registry.Mutter.get(), 0.7f, 0.95f + random.nextFloat() * 0.1f);
    }

    private void scanZombies(ServerLevel World) {
        KnownZombies.values().removeIf(Until -> Until < tickCount);
        List<Scp049Cured> Followers = followers(World);
        if (Followers.isEmpty()) {
            return;
        }
        List<LivingEntity> Found = intruders(World, Followers);
        if (Found.isEmpty()) {
            return;
        }
        boolean Fresh = false;
        LivingEntity First = null;
        int Assigned = 0;
        for (LivingEntity Zombie : Found) {
            int Already = 0;
            List<Scp049Cured> Free = new ArrayList<>();
            for (Scp049Cured Follower : Followers) {
                if (Follower.isOrderedAgainst(Zombie)) {
                    Already++;
                } else if (!Follower.hasPersonOrder() && Follower.getTarget() == null && !Follower.isRising()
                        && Follower.distanceToSqr(Zombie) <= SquadRange * SquadRange) {
                    Free.add(Follower);
                }
            }
            Free.sort((Left, Right) -> Double.compare(Left.distanceToSqr(Zombie), Right.distanceToSqr(Zombie)));
            for (Scp049Cured Follower : Free) {
                if (Already >= ZombieSquad) {
                    break;
                }
                if (Follower.orderAgainst(Zombie)) {
                    Already++;
                    Assigned++;
                    if (Scp049Persons.isPlagueZombie(Zombie)) {
                        PlagueOrders++;
                    }
                    if (First == null) {
                        First = Zombie;
                    }
                }
            }
            if (KnownZombies.put(Zombie.getUUID(), tickCount + ZombieMemory) == null) {
                Fresh = true;
            }
        }
        ZombieOrders += Assigned;
        if (Assigned > 0 && Fresh && First != null) {
            announceOrder(First, Routine == Phase.Wait || Routine == Phase.Seek || Routine == Phase.Return);
        }
    }

    private List<LivingEntity> intruders(ServerLevel World, List<Scp049Cured> Followers) {
        List<LivingEntity> Found = new ArrayList<>(World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(ZombieRange, ZombieVertical, ZombieRange),
                Body -> isIntruder(Body, Followers)));
        if (TrayPos != null && World.isLoaded(TrayPos) && Vec3.atCenterOf(TrayPos).distanceToSqr(position()) > ZombieRange * ZombieRange * 0.25) {
            for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, new AABB(TrayPos).inflate(ZombieRange, ZombieVertical, ZombieRange),
                    Candidate -> isIntruder(Candidate, Followers))) {
                if (!Found.contains(Body)) {
                    Found.add(Body);
                }
            }
        }
        Vec3 Here = position();
        Found.sort((Left, Right) -> Double.compare(Left.distanceToSqr(Here), Right.distanceToSqr(Here)));
        return Found;
    }

    private boolean isIntruder(LivingEntity Body, List<Scp049Cured> Followers) {
        if (!Scp049Persons.isZombieFoe(Body)) {
            return false;
        }
        if (!(Body instanceof ZombifiedPiglin Piglin)) {
            return true;
        }
        LivingEntity Aim = Piglin.getTarget();
        if (Aim == this || Aim instanceof Scp049Cured) {
            return true;
        }
        UUID Anger = Piglin.getPersistentAngerTarget();
        if (Anger == null) {
            return false;
        }
        if (Anger.equals(getUUID())) {
            return true;
        }
        for (Scp049Cured Follower : Followers) {
            if (Anger.equals(Follower.getUUID())) {
                return true;
            }
        }
        return false;
    }

    public void notePlagueKill() {
        PlagueKills++;
    }

    private boolean isMine(@Nullable Entity Body) {
        return Body == this || Body instanceof Scp049Cured Follower && Cured.contains(Follower.getUUID());
    }

    private boolean isHostileGolem(IronGolem Candidate) {
        if (!Candidate.isAlive() || Candidate.isRemoved()) {
            return false;
        }
        if (isMine(Candidate.getTarget())) {
            return true;
        }
        UUID Anger = Candidate.getPersistentAngerTarget();
        return Anger != null && (Anger.equals(getUUID()) || Cured.contains(Anger));
    }

    private void scanGolems(ServerLevel World) {
        if (Golem != null) {
            return;
        }
        long Now = World.getGameTime();
        IronGolem Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (IronGolem Candidate : World.getEntitiesOfClass(IronGolem.class, getBoundingBox().inflate(DuelRange, GolemVertical, DuelRange), this::isHostileGolem)) {
            if (Ignored.getOrDefault(Candidate.getUUID(), Long.MIN_VALUE) > Now && !canReach(Candidate, FellStart)) {
                continue;
            }
            double Distance = distanceToSqr(Candidate);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        if (Best != null) {
            engageGolem(Best);
        }
    }

    private void engageGolem(IronGolem Candidate) {
        if (Candidate.getUUID().equals(Golem)) {
            return;
        }
        Golem = Candidate.getUUID();
        GolemSince = tickCount;
        DuelStart = position();
        DuelBest = distanceTo(Candidate);
        DuelBestTick = tickCount;
        DuelSteered = false;
        DuelState = "watch";
    }

    @Nullable
    private IronGolem currentGolem(ServerLevel World) {
        if (Golem == null || !(World.getEntity(Golem) instanceof IronGolem Found) || !Found.isAlive() || Found.isRemoved()
                || Found.distanceToSqr(this) > GolemLeash * GolemLeash) {
            return null;
        }
        return Found;
    }

    @Nullable
    private IronGolem golem(ServerLevel World) {
        IronGolem Found = currentGolem(World);
        if (Found == null && Golem != null) {
            endDuel(World, "lost", 0);
        }
        return Found;
    }

    private boolean approachable(@Nullable Path Route) {
        if (Route == null || !Route.canReach() || Route.getNodeCount() == 0) {
            return false;
        }
        int Floor = getBlockY();
        for (int Index = 0; Index < Route.getNodeCount(); Index++) {
            if (Route.getNode(Index).y < Floor - DuelDrop) {
                return false;
            }
        }
        return true;
    }

    private boolean duelTick(ServerLevel World) {
        if (Routine == Phase.Operate || Golem == null) {
            return false;
        }
        IronGolem Found = golem(World);
        if (Found == null) {
            return false;
        }
        if (isFelling()) {
            getNavigation().stop();
            DuelState = "fell";
            DuelSteered = true;
            return true;
        }
        if (action() != ActionNone) {
            return false;
        }
        double Start = GolemHitTick != Integer.MIN_VALUE && tickCount - GolemHitTick <= GolemHitMemory ? FellReach : FellStart;
        if (FellCooldown <= 0 && canReach(Found, Start)) {
            beginFell(Found);
            DuelState = "fell";
            DuelSteered = true;
            return true;
        }
        if (Held != null) {
            DuelState = "watch";
            if (withinTouch(Found, CarryWait)) {
                getLookControl().setLookAt(Found, 30.0f, 30.0f);
            }
            if (tickCount - GolemSince > GolemPatience) {
                endDuel(World, "patience", GolemIgnore);
            }
            return false;
        }
        double Gap = distanceTo(Found);
        if (Gap < DuelBest - DuelStep) {
            DuelBest = Gap;
            DuelBestTick = tickCount;
        }
        String Reason = null;
        if (tickCount - GolemSince > GolemPatience) {
            Reason = "patience";
        } else if (hasWorkableTray() && position().distanceTo(home()) > DuelLeash || DuelStart != null && position().distanceTo(DuelStart) > DuelChase) {
            Reason = "leash";
        } else if (tickCount - DuelBestTick > DuelStall) {
            Reason = "stalled";
        }
        if (Reason != null) {
            endDuel(World, Reason, Reason.equals("stalled") ? UnreachableIgnore : GolemIgnore);
            return false;
        }
        if (getNavigation().isDone() || (tickCount - GolemSince) % DuelRecheck == 0) {
            Path Route = getNavigation().createPath(Found, 0);
            if (!approachable(Route)) {
                endDuel(World, "unreachable", UnreachableIgnore);
                return false;
            }
            getNavigation().moveTo(Route, PursueSpeed);
            DuelState = "approach";
        }
        getLookControl().setLookAt(Found, 30.0f, 30.0f);
        DuelSteered = true;
        return true;
    }

    private void endDuel(ServerLevel World, String Reason, int IgnoreTicks) {
        if (Golem != null && IgnoreTicks > 0) {
            Ignored.put(Golem, World.getGameTime() + IgnoreTicks);
        }
        boolean Steered = DuelSteered && Held == null;
        Golem = null;
        DuelStart = null;
        DuelSteered = false;
        DuelState = "none";
        DuelEnd = Reason;
        DuelEndTick = tickCount;
        if (!Steered) {
            return;
        }
        getNavigation().stop();
        PhaseTicks = 0;
        StuckTicks = 0;
        StuckFrom = null;
        resetHomeWatch();
        if (Quarry == null && Prey == null && !PendingLoss && Routine != Phase.Operate) {
            setRoutine(Phase.Return);
        }
    }

    private void beginFell(IronGolem Target) {
        FellTarget = Target;
        setAction(Held != null ? ActionFellFree : ActionFell, TouchTicks);
        getLookControl().setLookAt(Target, 40.0f, 40.0f);
        playSound(Scp049Registry.Touch.get(), 0.9f, 0.82f + random.nextFloat() * 0.08f);
    }

    private void fellTick(ServerLevel World, int Elapsed) {
        IronGolem Target = FellTarget;
        if (Target == null) {
            return;
        }
        getLookControl().setLookAt(Target, 40.0f, 40.0f);
        if (Elapsed != TouchHitTick) {
            return;
        }
        FellTarget = null;
        if (!Target.isAlive() || Target.isRemoved() || !canReach(Target, FellReach)) {
            FellCooldown = FellCooldownTicks;
            return;
        }
        fell(World, Target);
    }

    private void fell(ServerLevel World, IronGolem Target) {
        Vec3 Spot = Target.position();
        float Yaw = Target.yBodyRot;
        Target.invulnerableTime = 0;
        Target.hurt(damageSources().source(Scp049Registry.FellDamage, this), Float.MAX_VALUE);
        World.playSound(null, Spot.x, Spot.y + 1.3, Spot.z, Scp049Registry.Fell.get(), SoundSource.HOSTILE, FellVolume, 0.95f + random.nextFloat() * 0.1f);
        World.playSound(null, Spot.x, Spot.y + 1.3, Spot.z, Scp049Registry.FellFade.get(), SoundSource.HOSTILE, FellFadeVolume, 0.95f + random.nextFloat() * 0.1f);
        World.sendParticles(ParticleTypes.ENCHANTED_HIT, Spot.x, Spot.y + 1.4, Spot.z, 10, 0.4, 0.4, 0.4, 0.05);
        if (Target.isAlive()) {
            FellCooldown = FellCooldownTicks;
            return;
        }
        GolemsFelled++;
        LastFellTick = tickCount;
        endDuel(World, "felled", 0);
        Collapses.add(new Collapse(Spot, Yaw, tickCount + ThudDelay, tickCount + MutterDelay));
    }

    private void tickCollapses(ServerLevel World) {
        if (Collapses.isEmpty()) {
            return;
        }
        Iterator<Collapse> Each = Collapses.iterator();
        while (Each.hasNext()) {
            Collapse Fall = Each.next();
            if (tickCount == Fall.ThudAt()) {
                Vec3 Side = new Vec3(Mth.cos(Fall.Yaw() * Mth.DEG_TO_RAD), 0.0, Mth.sin(Fall.Yaw() * Mth.DEG_TO_RAD));
                World.playSound(null, Fall.Spot().x, Fall.Spot().y, Fall.Spot().z, Scp049Registry.GolemFall.get(), SoundSource.HOSTILE, GolemFallVolume, 0.95f + random.nextFloat() * 0.1f);
                World.playSound(null, Fall.Spot().x, Fall.Spot().y, Fall.Spot().z, Scp049Registry.GolemFallLow.get(), SoundSource.HOSTILE, GolemFallLowVolume, 0.95f + random.nextFloat() * 0.1f);
                BlockParticleOption Iron = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState());
                ItemParticleOption Crumbs = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_NUGGET));
                for (int Index = 0; Index < 4; Index++) {
                    Vec3 Along = Fall.Spot().add(Side.scale(0.4 + Index * 0.65));
                    World.sendParticles(Iron, Along.x, Along.y + 0.25, Along.z, 12, 0.45, 0.15, 0.45, 0.15);
                    World.sendParticles(Crumbs, Along.x, Along.y + 0.3, Along.z, 3, 0.3, 0.1, 0.3, 0.08);
                }
                World.sendParticles(ParticleTypes.POOF, Fall.Spot().x, Fall.Spot().y + 0.2, Fall.Spot().z, 8, 0.8, 0.1, 0.8, 0.02);
            }
            if (tickCount >= Fall.MutterAt()) {
                if (isAlive()) {
                    playSound(Scp049Registry.Mutter.get(), 0.9f, 0.88f + random.nextFloat() * 0.08f);
                    World.broadcastEntityEvent(this, VoiceEvent);
                }
                Each.remove();
            }
        }
    }

    public Component defenceDebug(ServerLevel World) {
        String Since = LastFellTick == Integer.MIN_VALUE ? "-" : String.format(Locale.ROOT, "%.1f", (tickCount - LastFellTick) / 20.0);
        IronGolem Current = currentGolem(World);
        Component Name = Current != null ? Current.getDisplayName() : Component.translatable("commands.scp_unleashed.scp049.loss.none");
        return Component.translatable("commands.scp_unleashed.scp049.status.defence", GolemsFelled, Since, Name, PlagueOrders, PlagueKills);
    }

    public Component navDebug(ServerLevel World) {
        Path Route = getNavigation().getPath();
        String Status = Route == null ? "none" : Route.isDone() ? "done" : Route.canReach() ? "reachable" : "partial";
        String Nodes = Route == null ? "-" : Route.getNextNodeIndex() + "/" + Route.getNodeCount();
        String Home = TrayPos == null ? "-" : String.format(Locale.ROOT, "%.1f", position().distanceTo(home()));
        String Idle = Routine == Phase.Return || Routine == Phase.Wait && !atHome() ? String.format(Locale.ROOT, "%.1f", (tickCount - HomeBestTick) / 20.0) : "-";
        String Ended = DuelEndTick == Integer.MIN_VALUE ? "-" : String.format(Locale.ROOT, "%.1f", (tickCount - DuelEndTick) / 20.0);
        return Component.translatable("commands.scp_unleashed.scp049.status.nav", Component.translatable("jade.scp_unleashed.scp049." + stateKey()),
                String.format(Locale.ROOT, "%.1f", PhaseTicks / 20.0), Component.translatable("commands.scp_unleashed.scp049.duel." + DuelState),
                Component.translatable("commands.scp_unleashed.scp049.duel_end." + DuelEnd), Ended, Component.translatable("commands.scp_unleashed.scp049.route." + Status),
                Nodes, Home, Idle, HomeReplans, HomeGiveUps);
    }

    public Component trayDebug() {
        if (TrayPos == null) {
            return Component.translatable(Roaming ? "commands.scp_unleashed.scp049.status.tray_gone" : "commands.scp_unleashed.scp049.status.no_tray", (int) LinkRange);
        }
        if (!Roaming) {
            return Component.translatable("commands.scp_unleashed.scp049.status.tray", TrayPos.getX(), TrayPos.getY(), TrayPos.getZ());
        }
        String Retry = String.format(Locale.ROOT, "%.0f", Math.max(0, HomeRetryAt - tickCount) / 20.0);
        return Component.translatable("commands.scp_unleashed.scp049.status.tray_away", TrayPos.getX(), TrayPos.getY(), TrayPos.getZ(),
                Component.translatable("commands.scp_unleashed.scp049.away." + RoamReason), Retry);
    }

    public Component reachDebug() {
        return Component.translatable("commands.scp_unleashed.scp049.status.reach", String.format(Locale.ROOT, "%.1f", TouchReach), String.format(Locale.ROOT, "%.1f", ReachRise),
                OutOfReach, TouchKills, RoamCount);
    }

    public Component lossDebug(ServerLevel World) {
        String Since = LossTick == Integer.MIN_VALUE ? "-" : String.format(Locale.ROOT, "%.1f", (tickCount - LossTick) / 20.0);
        LivingEntity Danger = threat(World);
        Component ThreatName = Danger != null ? Danger.getDisplayName() : Component.translatable("commands.scp_unleashed.scp049.loss.nobody");
        String Command = LastCommandTick < 0 ? "-" : String.format(Locale.ROOT, "%.1f", (tickCount - LastCommandTick) / 20.0);
        return Component.translatable("commands.scp_unleashed.scp049.status.loss", Component.translatable("commands.scp_unleashed.scp049.loss." + LossReason), Since,
                Component.translatable("commands.scp_unleashed.scp049.retarget." + LossOutcome), String.format(Locale.ROOT, "%.1f", Math.max(0, Cooldown) / 20.0),
                ThreatName, ZombieOrders, KnownZombies.size(), Command);
    }

    private void dragTick(ServerLevel World) {
        LivingEntity Victim = held(World);
        if (Victim == null) {
            retarget(World, null, null);
            return;
        }
        if (!hasWorkableTray()) {
            beginRoaming(World, "lost");
            return;
        }
        if (trayBar(World) != null) {
            abandonHeld(World, Victim, "barred");
            return;
        }
        if (TrayPos != null && (atHome() || nearWork() && getNavigation().isDone())) {
            beginOperate(World, Victim);
            return;
        }
        trackProgress(Victim);
        if (DragStuck >= DragGiveUp) {
            operateHere(World, Victim);
            return;
        }
        if (DragStuck >= DragNudge && (DragStuck - DragNudge) % NudgeEvery == 0) {
            Nudges++;
            DragState = "nudge_" + Scp049Hold.unstick(Victim, trailPoint(TetherLength));
        }
        boolean Replan = DragStuck >= DragReplan && (DragStuck - DragReplan) % ReplanEvery == 0;
        if (Replan) {
            Replans++;
            getNavigation().stop();
        }
        if (distanceTo(Victim) > LagLimit) {
            DragState = "waiting";
            getNavigation().stop();
            getLookControl().setLookAt(Victim, 20.0f, 20.0f);
            return;
        }
        if (DragStuck < DragNudge) {
            DragState = Replan ? "replanned" : "moving";
        }
        Vec3 Spot = home();
        if (Replan || getNavigation().isDone() || PhaseTicks % 20 == 0) {
            getNavigation().moveTo(Spot.x, Spot.y, Spot.z, DragSpeed);
        }
    }

    private void abandonHeld(ServerLevel World, LivingEntity Victim, String Reason) {
        if (!Scp049Persons.isPerson(Victim, Scp049Persons.sparesPlayers(this))) {
            loseHeld(World, Reason, false, null);
            return;
        }
        releaseHeld(World);
        LossReason = Reason;
        LossOutcome = "pending";
        LossTick = tickCount;
        cancelAction();
        setAction(ActionTouch, TouchTicks);
        killByTouch(World, Victim);
        boolean Down = !Victim.isAlive() || Victim instanceof ServerPlayer Controller && ControlManager.hasPendingDeath(Controller);
        retarget(World, Down ? null : Victim, null);
    }

    private boolean nearWork() {
        Vec3 Spot = home();
        double Dx = Spot.x - getX();
        double Dz = Spot.z - getZ();
        return Dx * Dx + Dz * Dz <= WorkSlack * WorkSlack && Math.abs(Spot.y - getY()) < 1.5;
    }

    private void resetProgress() {
        DragStuck = 0;
        SampleCount = 0;
        DragState = Held != null ? "moving" : "idle";
    }

    private void trackProgress(LivingEntity Victim) {
        DragStuck++;
        if (TrayPos == null || DragStuck % ProgressEvery != 0) {
            return;
        }
        double Doctor = remainingPath();
        double Gap = distanceTo(Victim);
        int Slot = SampleCount % (ProgressSpan + 1);
        DoctorSamples[Slot] = Doctor;
        GapSamples[Slot] = Gap;
        SampleCount++;
        if (SampleCount <= ProgressSpan) {
            return;
        }
        int Earlier = (SampleCount - 1 - ProgressSpan) % (ProgressSpan + 1);
        if (Doctor <= DoctorSamples[Earlier] - ProgressStep || Gap <= GapSamples[Earlier] - ProgressStep) {
            DragStuck = 0;
            SampleCount = 0;
        }
    }

    private double remainingPath() {
        Vec3 Spot = home();
        Path Route = getNavigation().getPath();
        if (Route == null || Route.isDone()) {
            return position().distanceTo(Spot);
        }
        double Total = 0.0;
        Vec3 Previous = position();
        for (int Index = Route.getNextNodeIndex(); Index < Route.getNodeCount(); Index++) {
            Vec3 Node = Vec3.atBottomCenterOf(Route.getNodePos(Index));
            Total += Previous.distanceTo(Node);
            Previous = Node;
        }
        return Total + Previous.distanceTo(Spot);
    }

    private void operateHere(ServerLevel World, LivingEntity Victim) {
        DragState = "operating_here";
        if (TrayPos == null || position().distanceTo(Vec3.atBottomCenterOf(TrayPos)) >= 2.5) {
            Vec3 Aside = Vec3.directionFromRotation(0.0f, yBodyRot + 90.0f);
            FieldSpot = position();
            FieldPatient = position().add(Aside.x * 0.9, 0.0, Aside.z * 0.9);
        }
        beginOperate(World, Victim);
    }

    private void beginOperate(ServerLevel World, LivingEntity Victim) {
        setRoutine(Phase.Operate);
        OperateLeft = OperateTicks;
        getNavigation().stop();
        setAction(ActionSurgery, OperateTicks);
        speak(Victim, "operate");
    }

    private void operateTick(ServerLevel World) {
        LivingEntity Victim = held(World);
        if (Victim == null) {
            setRoutine(Phase.Return);
            return;
        }
        getNavigation().stop();
        if (FieldPatient != null) {
            getLookControl().setLookAt(FieldPatient.x, FieldPatient.y + 0.4, FieldPatient.z, 20.0f, 30.0f);
        } else {
            faceTray();
        }
        if (action() != ActionSurgery && action() != ActionFlinch) {
            OperateLeft = OperateTicks;
            setAction(ActionSurgery, OperateTicks);
        }
        if (--OperateLeft <= 0) {
            finishOperation(World, Victim);
        }
    }

    public Component dragDebug(ServerLevel World) {
        Path Route = getNavigation().getPath();
        String Status = Route == null ? "none" : Route.isDone() ? "done" : Route.canReach() ? "reachable" : "partial";
        String Nodes = Route == null ? "-" : Route.getNextNodeIndex() + "/" + Route.getNodeCount();
        LivingEntity Victim = held(World);
        String Gap = Victim != null ? String.format(Locale.ROOT, "%.1f", distanceTo(Victim)) : "-";
        return Component.translatable("commands.scp_unleashed.scp049.status.drag", Component.translatable("commands.scp_unleashed.scp049.drag." + dragStateKey()),
                String.format(Locale.ROOT, "%.1f", DragStuck / 20.0), Component.translatable("commands.scp_unleashed.scp049.path." + Status), Nodes, Gap, Replans, Nudges,
                Portals.openCount());
    }

    private String dragStateKey() {
        if (Held == null) {
            return "idle";
        }
        return DragState.startsWith("nudge_") ? "nudge" : DragState;
    }

    private void finishOperation(ServerLevel World, LivingEntity Victim) {
        CompoundTag Look = Scp049Cured.lookOf(Victim);
        Vec3 Spot = Victim.position();
        float Yaw = Victim.getYRot();
        Held = null;
        Trail.clear();
        syncHeld(null);
        resetProgress();
        Scp049Hold.release(Victim);
        Victim.invulnerableTime = 0;
        Victim.hurt(damageSources().source(Scp049Registry.CureDamage, this), Float.MAX_VALUE);
        LivingEntity Remains = Victim;
        if (Victim instanceof ServerPlayer Controller && ControlManager.hasPendingDeath(Controller)) {
            List<CompoundTag> BodyLook = new ArrayList<>(1);
            Remains = ControlManager.processDeath(Controller, Body -> BodyLook.add(Scp049Cured.lookOf(Body)));
            if (Remains != null && !BodyLook.isEmpty()) {
                Look = BodyLook.get(0);
                Spot = Remains.position();
                Yaw = Remains.getYRot();
            }
        }
        if (Remains == null || Remains.isAlive()) {
            Cooldown = Math.max(Cooldown, RegrabTicks);
            LossReason = "survived";
            LossTick = tickCount;
            retarget(World, Remains, null);
            return;
        }
        boolean Loaded = TrayPos != null && World.isLoaded(TrayPos);
        boolean Revive = hasWorkableTray() && (!Loaded || World.getBlockEntity(TrayPos) instanceof Scp049TrayBlockEntity);
        if (Revive && Loaded && World.getBlockEntity(TrayPos) instanceof Scp049TrayBlockEntity Tray) {
            Tray.operated();
        }
        if (Revive) {
            Rises.add(new PendingRise(Look, Spot, Yaw, World.getGameTime() + RiseDelay));
        }
        if (Quarry != null && Quarry.equals(Victim.getUUID())) {
            endPersonRoutine(World);
        }
        setRoutine(Quarry != null ? Phase.Pursue : Phase.Wait);
    }

    private void tickRises(ServerLevel World) {
        if (Rises.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        Iterator<PendingRise> Each = Rises.iterator();
        while (Each.hasNext()) {
            PendingRise Rise = Each.next();
            if (Rise.Due() > Now) {
                continue;
            }
            Each.remove();
            Scp049Cured Risen = Scp049Cured.rise(World, Rise.Look(), Rise.Spot(), Rise.Yaw(), this, TrayPos);
            if (Risen != null) {
                Cured.add(Risen.getUUID());
                LivingEntity Target = orderFor(World);
                if (Target != null) {
                    Risen.order(Target);
                }
            }
        }
    }

    private void scan(ServerLevel World) {
        if (Quarry != null) {
            LivingEntity Target = quarry(World);
            if (Target == null || !Scp049Persons.isPerson(Target, Scp049Persons.sparesPlayers(this)) || Target.distanceToSqr(this) > LoseRange * LoseRange
                    || hasWorkableTray() && Target.distanceToSqr(Vec3.atCenterOf(TrayPos)) > LeashRange * LeashRange) {
                if (Held == null || !Quarry.equals(Held)) {
                    endPersonRoutine(World);
                }
                return;
            }
            if (Scp049Fear.isBearer(Target) && (Held == null || !Quarry.equals(Held)) && hasLineOfSight(Target)) {
                endPersonRoutine(World);
                return;
            }
            if (Scp049Persons.isNoticeable(Target) && hasLineOfSight(Target)) {
                LastSeenTick = tickCount;
            } else if (tickCount - LastSeenTick > LostTicks && (Held == null || !Quarry.equals(Held))) {
                endPersonRoutine(World);
            }
            return;
        }
        if (Held != null && Routine == Phase.Operate && !(held(World) instanceof Villager)) {
            return;
        }
        LivingEntity Spotted = findPerson(World);
        if (Spotted != null) {
            beginPersonRoutine(World, Spotted);
        }
    }

    @Nullable
    private LivingEntity findPerson(ServerLevel World) {
        boolean Spare = Scp049Persons.sparesPlayers(this);
        long Now = World.getGameTime();
        LivingEntity Best = null;
        double BestDistance = SightRange * SightRange;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SightRange),
                Body -> Scp049Persons.isPerson(Body, Spare) && Scp049Persons.isNoticeable(Body) && !Scp049Hold.isHeld(Body) && !Scp049Fear.isBearer(Body)
                        && Ignored.getOrDefault(Body.getUUID(), Long.MIN_VALUE) <= Now)) {
            double Distance = distanceToSqr(Candidate);
            if (Distance < BestDistance && (!hasWorkableTray() || Candidate.distanceToSqr(Vec3.atCenterOf(TrayPos)) <= LeashRange * LeashRange) && hasLineOfSight(Candidate)) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    public void beginPersonRoutine(ServerLevel World, LivingEntity Target) {
        if (Quarry != null && Quarry.equals(Target.getUUID())) {
            return;
        }
        LivingEntity Victim = held(World);
        if (Victim instanceof Villager) {
            releaseHeld(World);
        }
        Prey = null;
        Regrab = null;
        Quarry = Target.getUUID();
        LastSeenTick = tickCount;
        PursueBest = Double.MAX_VALUE;
        PursueBestTick = tickCount;
        if (Held != null) {
            return;
        }
        setRoutine(Phase.Greet);
        setAction(ActionBow, BowTicks);
        playSound(Scp049Registry.Greet.get(), 1.2f, 0.95f + random.nextFloat() * 0.1f);
        speak(Target, "greet");
    }

    public void endPersonRoutine(ServerLevel World) {
        Quarry = null;
        for (Scp049Cured Follower : followers(World)) {
            Follower.recall();
        }
        if (Routine == Phase.Pursue || Routine == Phase.Greet) {
            setRoutine(Phase.Return);
        }
    }

    @Nullable
    private LivingEntity quarry(ServerLevel World) {
        return Quarry != null && World.getEntity(Quarry) instanceof LivingEntity Body ? Body : null;
    }

    private void commandCured(ServerLevel World, LivingEntity Target) {
        LivingEntity Order = threat(World);
        for (Scp049Cured Follower : followers(World)) {
            Follower.order(Order != null ? Order : Target);
        }
    }

    @Nullable
    private LivingEntity orderFor(ServerLevel World) {
        LivingEntity Order = threat(World);
        return Order != null ? Order : quarry(World);
    }

    public List<Scp049Cured> followers(ServerLevel World) {
        List<Scp049Cured> Found = new ArrayList<>();
        Iterator<UUID> Each = Cured.iterator();
        while (Each.hasNext()) {
            Entity Body = World.getEntity(Each.next());
            if (Body instanceof Scp049Cured Follower && Follower.isAlive()) {
                Found.add(Follower);
            } else if (Body != null) {
                Each.remove();
            }
        }
        return Found;
    }

    public void adopt(Scp049Cured Follower) {
        if (!Cured.contains(Follower.getUUID())) {
            Cured.add(Follower.getUUID());
        }
    }

    private void greetTick(ServerLevel World) {
        LivingEntity Target = quarry(World);
        if (Target == null) {
            endPersonRoutine(World);
            return;
        }
        getNavigation().stop();
        getLookControl().setLookAt(Target, 30.0f, 30.0f);
        if (action() == ActionNone) {
            setRoutine(Phase.Pursue);
        }
    }

    private void pursueTick(ServerLevel World) {
        LivingEntity Target = quarry(World);
        if (Target == null) {
            endPersonRoutine(World);
            return;
        }
        if (Held != null) {
            setRoutine(Phase.Drag);
            return;
        }
        getLookControl().setLookAt(Target, 30.0f, 30.0f);
        if (PhaseTicks % OrderRefresh == 0) {
            commandCured(World, Target);
        }
        if (action() == ActionTouch) {
            return;
        }
        if (Cooldown <= 0 && !Scp049Hold.isHeld(Target) && canReach(Target, TouchStart)) {
            beginTouch(Target);
            return;
        }
        if (getNavigation().isDone() || PhaseTicks % 10 == 0) {
            getNavigation().moveTo(Target, PursueSpeed);
        }
        double Gap = distanceTo(Target);
        if (Gap < PursueBest - PursueStep) {
            PursueBest = Gap;
            PursueBestTick = tickCount;
        }
        if (tickCount - PursueBestTick > PursueStall && outOfReach(Target)) {
            OutOfReach++;
            Ignored.put(Target.getUUID(), World.getGameTime() + OutOfReachIgnore);
            endPersonRoutine(World);
        }
    }

    private boolean outOfReach(LivingEntity Target) {
        Path Route = getNavigation().getPath();
        return Route == null || !Route.canReach() || flatGap(Target) <= PursueClose && !canReach(Target, PursueClose);
    }

    private void tickFear(ServerLevel World) {
        if (isBrave() || isDeadOrDying()) {
            return;
        }
        Scp049Fear.Source Found = Fear.scan(World);
        if (Found == null) {
            return;
        }
        LastAlarmTick = tickCount;
        if (Routine == Phase.Flee) {
            Dread = Found.Body().getUUID();
            DreadSpot = Found.Body().position();
            DreadDropped = Found.Dropped();
            return;
        }
        beginFlee(World, Found);
    }

    private void beginFlee(ServerLevel World, Scp049Fear.Source Found) {
        Entity Body = Found.Body();
        boolean Startled = Routine != Phase.Wary;
        Dread = Body.getUUID();
        DreadSpot = Body.position();
        DreadDropped = Found.Dropped();
        Alarms++;
        if (Held != null) {
            dropInFright(World);
        }
        cancelAction();
        PendingLoss = false;
        LostId = null;
        LostAttacker = null;
        Prey = null;
        Regrab = null;
        if (Quarry != null) {
            endPersonRoutine(World);
        }
        getNavigation().stop();
        setRoutine(Phase.Flee);
        FleeGoal = null;
        FleeBest = position().distanceTo(DreadSpot);
        FleeBestTick = tickCount;
        if (Startled) {
            setAction(ActionFlinch, FlinchTicks);
        }
        alarm(World, Body);
    }

    private void cancelAction() {
        if (action() != ActionNone || ActionTicks > 0) {
            entityData.set(ActionData, ActionNone);
            ActionTicks = 0;
        }
        TouchTarget = null;
        FellTarget = null;
        CommandFocus = null;
    }

    private void dropInFright(ServerLevel World) {
        UUID Lost = Held;
        releaseHeld(World);
        if (Lost != null) {
            Ignored.put(Lost, World.getGameTime() + ReleasedIgnore);
        }
        Cooldown = Math.max(Cooldown, RegrabTicks);
        LossReason = "frightened";
        LossOutcome = "fled";
        LossTick = tickCount;
    }

    private void alarm(ServerLevel World, Entity Body) {
        if (tickCount - LastRecoilTick < AlarmGap) {
            return;
        }
        LastRecoilTick = tickCount;
        playSound(Scp049Registry.Recoil.get(), 1.1f, 0.95f + random.nextFloat() * 0.1f);
        playSound(Scp049Registry.Gasp.get(), 0.8f, 1.0f + random.nextFloat() * 0.1f);
        playSound(Scp049Registry.Robe.get(), 0.9f, 1.1f + random.nextFloat() * 0.1f);
        World.broadcastEntityEvent(this, VoiceEvent);
        if (Body instanceof ServerPlayer Bearer) {
            speak(Bearer, "recoil");
        }
    }

    private void fleeTick(ServerLevel World) {
        Entity Body = Dread != null ? World.getEntity(Dread) : null;
        if (DreadSpot == null || !Scp049Fear.stillRepels(Body, DreadDropped)) {
            beginWary();
            return;
        }
        DreadSpot = Body.position();
        getLookControl().setLookAt(Body.getX(), Body.getEyeY(), Body.getZ(), 20.0f, 30.0f);
        if (action() != ActionNone) {
            return;
        }
        double Gap = position().distanceTo(DreadSpot);
        if (Gap >= FleeSafe && (Gap >= FleeFar || !hasLineOfSight(Body))) {
            beginWary();
            return;
        }
        if (Gap > FleeBest + FleeProgress) {
            FleeBest = Gap;
            FleeBestTick = tickCount;
        }
        if (tickCount - FleeBestTick > CorneredTicks) {
            cornered(World);
            return;
        }
        boolean Stale = FleeGoal == null || FleeGoal.distanceTo(DreadSpot) < Gap + FleeMargin;
        if (getNavigation().isDone() && PhaseTicks % FearInterval == 0 || PhaseTicks % FleeReplan == 0 && Stale) {
            Vec3 Goal = fleeGoal(Gap);
            if (Goal != null) {
                FleeGoal = Goal;
                getNavigation().moveTo(Goal.x, Goal.y, Goal.z, FleeSpeed);
            }
        }
    }

    @Nullable
    private Vec3 fleeGoal(double Gap) {
        Vec3 Best = null;
        double BestGap = Gap + FleeMargin;
        for (int Try = 0; Try < FleeTries; Try++) {
            Vec3 Candidate = DefaultRandomPos.getPosAway(this, FleeStep, FleeVertical, DreadSpot);
            if (Candidate == null) {
                continue;
            }
            double Away = Candidate.distanceTo(DreadSpot);
            if (Away > BestGap) {
                BestGap = Away;
                Best = Candidate;
            }
        }
        return Best;
    }

    private void cornered(ServerLevel World) {
        Corners++;
        BraveUntil = tickCount + BraveTicks;
        playSound(Scp049Registry.Mutter.get(), 0.9f, 0.85f + random.nextFloat() * 0.08f);
        World.broadcastEntityEvent(this, VoiceEvent);
        resumeAfterFear();
    }

    private void beginWary() {
        getNavigation().stop();
        FleeGoal = null;
        WaryUntil = tickCount + WaryTicks;
        setRoutine(Phase.Wary);
    }

    private void waryTick(ServerLevel World) {
        Entity Body = Dread != null ? World.getEntity(Dread) : null;
        boolean Live = Scp049Fear.stillRepels(Body, DreadDropped);
        if (Live) {
            DreadSpot = Body.position();
        }
        Vec3 Watch = DreadSpot;
        if (Watch == null) {
            Scp049Fear.Spot Near = Fear.nearest(position());
            Watch = Near != null ? Near.centre() : null;
        }
        if (Watch != null) {
            getLookControl().setLookAt(Watch.x, Watch.y + 0.5, Watch.z, 10.0f, 20.0f);
            if (position().distanceTo(Watch) < WaryRadius) {
                if (Live && hasLineOfSight(Body)) {
                    WaryUntil = Math.max(WaryUntil, tickCount + WaryTicks);
                }
                if (getNavigation().isDone() && PhaseTicks % FearInterval == 0) {
                    Vec3 Away = DefaultRandomPos.getPosAway(this, WaryStep, FleeVertical, Watch);
                    if (Away != null) {
                        getNavigation().moveTo(Away.x, Away.y, Away.z, StrollSpeed);
                    }
                }
            }
        }
        if (tickCount < WaryUntil && PhaseTicks < WaryLimit) {
            return;
        }
        resumeAfterFear();
    }

    private void resumeAfterFear() {
        Dread = null;
        DreadSpot = null;
        FleeGoal = null;
        getNavigation().stop();
        setRoutine(Phase.Return);
    }

    public Component fearDebug() {
        String Since = LastAlarmTick == Integer.MIN_VALUE ? "-" : String.format(Locale.ROOT, "%.1f", (tickCount - LastAlarmTick) / 20.0);
        String Wary = Routine == Phase.Wary ? String.format(Locale.ROOT, "%.1f", Math.max(0, WaryUntil - tickCount) / 20.0) : "-";
        String Brave = isBrave() ? String.format(Locale.ROOT, "%.1f", (BraveUntil - tickCount) / 20.0) : "-";
        return Component.translatable("commands.scp_unleashed.scp049.status.fear", Alarms, Since, Corners, Fear.count(), Wary, Brave);
    }

    private void returnTick(ServerLevel World) {
        if (TrayPos == null) {
            getNavigation().stop();
            setRoutine(Phase.Wait);
            return;
        }
        if (Fear.covers(home())) {
            beginRoaming(World, "shunned");
            return;
        }
        if (atHome() || nearWork() && tickCount - HomeBestTick >= HomeReplan) {
            getNavigation().stop();
            NewTrayAt = -1L;
            HomeFails = 0;
            setRoutine(Phase.Wait);
            ModeLeft = 0;
            return;
        }
        moveHome(StrollSpeed);
        boolean Late = NewTrayAt >= 0L && PhaseTicks > NewTrayGrace;
        if (Late || homeWatch(World, NewTrayAt < 0L)) {
            NewTrayAt = -1L;
            HomeGiveUps++;
            beginRoaming(World, Late ? "late" : World.isLoaded(TrayPos) ? "unreachable" : "unloaded");
            HomeFails++;
        }
    }

    private void beginRoaming(ServerLevel World, String Reason) {
        RoamReason = Reason;
        HomeRetryAt = Reason.equals("shunned") ? tickCount : tickCount + Math.min(HomeRetryMax, HomeRetry << Math.min(HomeFails, 4));
        if (!Roaming) {
            Roaming = true;
            RoamCount++;
        }
        clearRestriction();
        Prey = null;
        Regrab = null;
        RoamPause = 0;
        if (Held != null) {
            LivingEntity Victim = held(World);
            if (Victim != null) {
                abandonHeld(World, Victim, Reason.equals("shunned") ? "barred" : "no_tray");
            } else {
                loseHeld(World, "no_tray", false, null);
            }
            return;
        }
        if (Routine != Phase.Pursue && Routine != Phase.Greet && !isAfraid()) {
            getNavigation().stop();
            setRoutine(Phase.Roam);
        }
    }

    private void endRoaming() {
        Roaming = false;
        RoamReason = "none";
        RoamPause = 0;
        if (TrayPos != null) {
            restrictTo(TrayPos, (int) LeashRange);
        }
        resetHomeWatch();
        if (Routine == Phase.Roam) {
            setRoutine(Phase.Return);
        }
    }

    private void roamTick(ServerLevel World) {
        if (!Roaming) {
            setRoutine(Phase.Return);
            return;
        }
        if (canGoHome(World)) {
            endRoaming();
            return;
        }
        if (RoamPause > 0) {
            RoamPause--;
            return;
        }
        if (getNavigation().isDone() || PhaseTicks - RoamLeg > RoamLegTicks) {
            RoamLeg = PhaseTicks;
            Scp049Fear.Spot Near = Fear.covering(position());
            Vec3 Goal = Near != null ? DefaultRandomPos.getPosAway(this, RoamStep, RoamVertical, Near.centre()) : roamGoal();
            if (Goal == null || Near == null && (Fear.covers(Goal) || random.nextInt(3) == 0)) {
                getNavigation().stop();
                RoamPause = RoamPauseMin + random.nextInt(RoamPauseSpread);
                return;
            }
            getNavigation().moveTo(Goal.x, Goal.y, Goal.z, StrollSpeed);
        }
    }

    @Nullable
    private Vec3 roamGoal() {
        if (TrayPos != null) {
            Vec3 Anchor = Vec3.atBottomCenterOf(TrayPos);
            if (position().distanceTo(Anchor) > RoamRadius) {
                return DefaultRandomPos.getPosTowards(this, RoamStep, RoamVertical, Anchor, Math.PI / 2.0);
            }
        }
        return LandRandomPos.getPos(this, RoamStep, RoamVertical);
    }

    private boolean canGoHome(ServerLevel World) {
        int Every = RoamReason.equals("shunned") ? ShunnedRecheck : RoamRecheck;
        if (TrayPos == null || tickCount < HomeRetryAt || (tickCount + getId()) % Every != 0 || !World.isLoaded(TrayPos)) {
            return false;
        }
        if (!(World.getBlockEntity(TrayPos) instanceof Scp049TrayBlockEntity Tray) || !Tray.canBind(getUUID())) {
            return false;
        }
        computeSpots(World);
        if (Fear.covers(home())) {
            return false;
        }
        if (probeHome(0)) {
            return true;
        }
        if (RoamReason.equals("shunned")) {
            RoamReason = "unreachable";
            HomeRetryAt = tickCount + Math.min(HomeRetryMax, HomeRetry << Math.min(HomeFails, 4));
            HomeFails++;
        }
        return false;
    }

    private void speak(LivingEntity Listener, String Occasion) {
        if (Listener instanceof ServerPlayer User) {
            Component Line = Component.translatable("entity.scp_unleashed.scp049.say." + Occasion + "." + random.nextInt(3));
            User.displayClientMessage(Senses.heard(User, Component.translatable("entity.scp_unleashed.scp049.says", Line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)), true);
        }
    }

    public int releaseAll(ServerLevel World) {
        if (Held == null) {
            return 0;
        }
        Ignored.put(Held, World.getGameTime() + ReleasedIgnore);
        if (Held.equals(Quarry)) {
            Quarry = null;
        }
        loseHeld(World, "released", false, null);
        return 1;
    }

    public String stateKey() {
        if (Routine == Phase.Wait && mode() == ModeTidy) {
            return "tidy";
        }
        return Routine.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (!Hurt || !(level() instanceof ServerLevel World)) {
            return Hurt;
        }
        LastHurtTick = tickCount;
        long Now = World.getGameTime();
        if (Now - RescueStart > RescueWindow) {
            RescueStart = Now;
            RescueDamage = 0.0f;
        }
        LivingEntity Attacker = Source.getEntity() instanceof LivingEntity Body && Body != this ? Body : null;
        if (Attacker instanceof IronGolem Hitter) {
            if (isAlive()) {
                engageGolem(Hitter);
                GolemHitTick = tickCount;
            }
            return Hurt;
        }
        RescueDamage += Amount;
        if (Held != null && RescueDamage >= RescueThreshold && isAlive()) {
            RescueDamage = 0.0f;
            loseHeld(World, "struck", true, Attacker);
            return Hurt;
        }
        if (isAlive() && Attacker != null && Scp049Persons.isPerson(Attacker, Scp049Persons.sparesPlayers(this)) && !Scp049Hold.isHeld(Attacker)) {
            noticeAttacker(World, Attacker);
        }
        return Hurt;
    }

    private void noticeAttacker(ServerLevel World, LivingEntity Attacker) {
        if (Attacker.getUUID().equals(Quarry) || Attacker.getUUID().equals(Held)) {
            return;
        }
        LivingEntity Victim = held(World);
        boolean Busy = isAfraid() || PendingLoss || Victim != null && !(Victim instanceof Villager) || Quarry != null && quarry(World) != null;
        if (Busy) {
            delegate(World, Attacker);
            return;
        }
        beginPersonRoutine(World, Attacker);
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            releaseHeld(World);
            PendingLoss = false;
            for (Scp049Cured Follower : followers(World)) {
                Follower.orphan();
            }
            if (TrayPos != null && World.isLoaded(TrayPos) && World.getBlockEntity(TrayPos) instanceof Scp049TrayBlockEntity Tray) {
                Tray.release(getUUID());
            }
        }
        super.die(Source);
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (level() instanceof ServerLevel World && Held != null && Reason != RemovalReason.UNLOADED_TO_CHUNK && Reason != RemovalReason.UNLOADED_WITH_PLAYER) {
            releaseHeld(World);
        }
        super.remove(Reason);
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return Other instanceof Scp049Cured || super.isAlliedTo(Other);
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
    public int getAmbientSoundInterval() {
        return 320;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return action() == ActionNone ? Scp049Registry.Mutter.get() : null;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp049Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp049Registry.Death.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(0.6, 0.3, 0.6);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putString("Routine", Routine.name());
        if (TrayPos != null) {
            Tag.put("Tray", NbtUtils.writeBlockPos(TrayPos));
        }
        if (Prey != null) {
            Tag.putUUID("Prey", Prey);
        }
        if (Quarry != null) {
            Tag.putUUID("Quarry", Quarry);
        }
        if (Held != null) {
            Tag.putUUID("Held", Held);
        }
        Tag.putInt("OperateLeft", OperateLeft);
        Tag.putLong("NewTrayAt", NewTrayAt);
        Tag.putBoolean("Roaming", Roaming);
        Tag.putString("RoamReason", RoamReason);
        Tag.putBoolean("HadTray", HadTray);
        Tag.putInt("HomeFails", HomeFails);
        ListTag Followers = new ListTag();
        for (UUID Id : Cured) {
            Followers.add(NbtUtils.createUUID(Id));
        }
        Tag.put("Cured", Followers);
        ListTag Pending = new ListTag();
        for (PendingRise Rise : Rises) {
            CompoundTag Entry = new CompoundTag();
            Entry.put("Look", Rise.Look());
            Entry.putDouble("X", Rise.Spot().x);
            Entry.putDouble("Y", Rise.Spot().y);
            Entry.putDouble("Z", Rise.Spot().z);
            Entry.putFloat("Yaw", Rise.Yaw());
            Entry.putLong("Due", Rise.Due());
            Pending.add(Entry);
        }
        Tag.put("Rises", Pending);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        try {
            Routine = Phase.valueOf(Tag.getString("Routine"));
        } catch (IllegalArgumentException Unknown) {
            Routine = Phase.Return;
        }
        TrayPos = NbtUtils.readBlockPos(Tag, "Tray").orElse(null);
        Prey = Tag.hasUUID("Prey") ? Tag.getUUID("Prey") : null;
        Quarry = Tag.hasUUID("Quarry") ? Tag.getUUID("Quarry") : null;
        Held = Tag.hasUUID("Held") ? Tag.getUUID("Held") : null;
        OperateLeft = Tag.getInt("OperateLeft");
        NewTrayAt = Tag.contains("NewTrayAt") ? Tag.getLong("NewTrayAt") : -1L;
        Roaming = Tag.getBoolean("Roaming");
        RoamReason = Tag.contains("RoamReason") ? Tag.getString("RoamReason") : "none";
        HadTray = Tag.contains("HadTray") ? Tag.getBoolean("HadTray") : TrayPos != null || Tag.contains("TrayLostAt") && Tag.getLong("TrayLostAt") >= 0L;
        HomeFails = Tag.getInt("HomeFails");
        HomeRetryAt = HomeRetry;
        Cured.clear();
        ListTag Followers = Tag.getList("Cured", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
        for (int Index = 0; Index < Followers.size(); Index++) {
            Cured.add(NbtUtils.loadUUID(Followers.get(Index)));
        }
        Rises.clear();
        ListTag Pending = Tag.getList("Rises", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Pending.size(); Index++) {
            CompoundTag Entry = Pending.getCompound(Index);
            Rises.add(new PendingRise(Entry.getCompound("Look"), new Vec3(Entry.getDouble("X"), Entry.getDouble("Y"), Entry.getDouble("Z")), Entry.getFloat("Yaw"),
                    Entry.getLong("Due")));
        }
        if (Held == null && (Routine == Phase.Drag || Routine == Phase.Operate)) {
            Routine = Phase.Return;
        }
        if (Routine == Phase.Greet) {
            Routine = Quarry != null ? Phase.Pursue : Phase.Return;
        }
        if (Routine == Phase.Flee || Routine == Phase.Wary) {
            Routine = Phase.Return;
        }
        if (Roaming && (Routine == Phase.Return || Routine == Phase.Wait || Routine == Phase.Seek)) {
            Routine = Phase.Roam;
        } else if (!Roaming && Routine == Phase.Roam) {
            Routine = Phase.Return;
        }
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        setHurried(Routine == Phase.Pursue);
        setMode(Routine == Phase.Drag ? ModeDrag : Routine == Phase.Operate ? ModeSurgery : ModeIdle);
        if (TrayPos != null && !Roaming) {
            restrictTo(TrayPos, (int) LeashRange);
        } else {
            clearRestriction();
        }
        syncTray();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (Key.equals(ActionStartData) && level().isClientSide && BodyController != null && bodyClip() != null) {
            BodyController.forceAnimationReset();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Scp049Controller Body = new Scp049Controller(this, "body", 4, false, Event -> {
            byte Current = action();
            Event.getController().transitionLength(bodyClip() != null ? 2 : 4);
            if (Current == ActionBow) {
                return Event.setAndContinue(BowPlay);
            }
            if (Current == ActionCommand) {
                return Event.setAndContinue(CommandPlay);
            }
            if (Current == ActionFlinch) {
                return Event.setAndContinue(FlinchPlay);
            }
            if (Current == ActionSurgery) {
                return Event.setAndContinue(SurgeryPlay);
            }
            byte Mode = mode();
            if (Mode == ModeSurgery) {
                return Event.setAndContinue(HoldLoop);
            }
            if (isStriding()) {
                return Event.setAndContinue(Mode == ModeDrag ? DragLoop : isHurried() ? StrideLoop : WalkLoop);
            }
            if (Mode == ModeDrag) {
                return Event.setAndContinue(HoldLoop);
            }
            return Event.setAndContinue(Mode == ModeTidy ? TidyLoop : IdleLoop);
        });
        BodyController = Body;
        Controllers.add(Body);
        Controllers.add(new Scp049Controller(this, "gesture", 3, true, Event -> {
            byte Current = action();
            if (Current == ActionTouch || Current == ActionFell) {
                return Event.setAndContinue(TouchPlay);
            }
            if (Current == ActionFellFree) {
                return Event.setAndContinue(TouchLeftPlay);
            }
            if (Current == ActionDoor) {
                return Event.setAndContinue(DoorPlay);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
