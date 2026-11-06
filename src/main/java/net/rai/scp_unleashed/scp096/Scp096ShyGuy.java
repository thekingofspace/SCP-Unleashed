package net.rai.scp_unleashed.scp096;

import net.minecraft.core.BlockPos;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.health.LethalDamage;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class Scp096ShyGuy extends Monster implements GeoEntity, IEntityWithComplexSpawn, ControlRefusing {
    public static final float Width = 0.8f;
    public static final float Height = 3.25f;
    public static final float EyeHeight = 2.85f;
    public static final float RageHeight = 1.95f;
    public static final float RageEyeHeight = 1.4f;
    public static final float SeatHeight = 2.15f;
    public static final float SeatEyeHeight = 1.4f;
    public static final double MaxHealth = 100.0;
    public static final double Armour = 20.0;
    public static final double Toughness = 12.0;
    public static final double Steadiness = 1.0;
    public static final double Speed = 0.3;
    public static final double Follow = 64.0;
    public static final double WanderSpeed = 0.45;
    public static final double ChaseSpeed = 1.8;
    public static final double LungeSpeed = 1.2;
    public static final float RageDamageShare = 0.2f;
    public static final float RegenPerTick = 2.5f;
    public static final float RecoverShare = 0.5f;
    public static final double VoidDepth = 64.0;
    public static final int ChargeTicks = 206;
    public static final int ScreamTicks = 111;
    public static final float ChargeQuiet = 0.25f;
    public static final double SwellRate = 1.5;
    public static final int PeakTicks = 70;
    public static final int UncurlTicks = 40;
    public static final int CollapseTicks = 12;
    public static final int SitTicks = 46;
    public static final int RiseTicks = 35;
    public static final int UncurlBlend = 4;
    public static final int CollapseBlend = 2;
    public static final int SitBlend = 6;
    public static final int RiseBlend = 3;
    public static final int SettleBlend = 16;
    public static final double SitLow = 0.3;
    public static final double RiseLow = 0.75;
    public static final int CalmingTicks = 300;
    public static final int CryMin = 600;
    public static final int CrySpread = 900;
    public static final int StrollsMin = 3;
    public static final int StrollsSpread = 3;
    public static final double StrollMin = 8.0;
    public static final double StrollMax = 30.0;
    public static final int StrollTries = 10;
    public static final int StrollLimit = 1600;
    public static final int StallTicks = 60;
    public static final double StallDistance = 0.4;
    public static final int PauseMin = 40;
    public static final int PauseSpread = 100;
    public static final int FirstPause = 30;
    public static final int RetryPause = 20;
    public static final float WeepStrollChance = 0.35f;
    public static final double LeashRadius = 32.0;
    public static final double LeashBias = 0.5;
    public static final double LeashLost = 96.0;
    public static final int FootingRange = 6;
    public static final int CalmDrop = 2;
    public static final float CalmWaterBorder = 4.0f;
    public static final int AttackTicks = 10;
    public static final int AttackHit = 5;
    public static final double AttackStart = 2.2;
    public static final double AttackReach = 3.4;
    public static final double AttackVertical = 2.6;
    public static final int TargetPatience = 3600;
    public static final double LoseRange = 160.0;
    public static final int SearchTicks = 200;
    public static final int WindDownTicks = 40;
    public static final float WaterPace = 0.8f;
    public static final float SlipBase = 0.21600002f;
    public static final double WaterGlide = 0.91;
    public static final float WaterSteer = 0.02f;
    public static final double WaterClimb = 0.24;
    public static final int BubbleGap = 3;
    public static final int ViewerScan = 5;
    public static final int SeenGap = 4;
    public static final int StuckLimit = 8;
    public static final double LeapRise = 1.6;
    public static final double DropRise = 1.6;
    public static final double ColumnReach = 2.2;
    public static final double LeapClearance = 0.9;
    public static final double LeapSteer = 0.32;
    public static final double LeapSpeedCap = 30.0;
    public static final double LeapHeadroom = 0.6;
    public static final double LiftSlack = 0.001;
    public static final double Gravity = 0.08;
    public static final double AirDrag = 0.98;
    public static final int LeapCrouch = 7;
    public static final int LeapLimit = 600;
    public static final int LeapGap = 10;
    public static final int RepathGap = 5;
    public static final double DirectRange = 24.0;
    public static final double DirectRise = 1.2;
    public static final float PitchLimit = 30.0f;
    public static final float HoodPitchLimit = 15.0f;
    public static final float LookDownLimit = 0.0f;
    public static final float YawLimit = 40.0f;
    public static final byte StateCalm = 0;
    public static final byte StateDistressed = 1;
    public static final byte StateRaging = 2;
    public static final byte StateCalming = 3;
    public static final byte ActionNone = 0;
    public static final byte ActionCry = 1;
    public static final byte ActionUncurl = 2;
    public static final byte ActionAttack = 3;
    public static final byte ActionTear = 4;
    public static final byte ActionLeap = 5;
    public static final byte ActionSmash = 6;
    public static final byte ActionSit = 7;
    public static final byte ActionRise = 8;
    public static final byte ActionCollapse = 9;
    public static final byte ActionWeepWalk = 10;
    public static final Scp096Gait.Profile WalkGait = new Scp096Gait.Profile("walk", 32.0, 0.0315, 0.4, 2.2, 3, new double[]{0.50751, 0.00751}, new float[]{1.0f, 1.0f},
            Scp096Registry.Step, Scp096Registry.Pad, 0.45f, 0.25f, 1.0f);
    public static final Scp096Gait.Profile SprintGait = new Scp096Gait.Profile("sprint", 8.0, 0.599, 0.5, 1.8, 3, new double[]{0.98934, 0.0896, 0.43707, 0.54033},
            new float[]{1.0f, 0.95f, 0.75f, 0.7f}, Scp096Registry.RunStep, Scp096Registry.RunThud, 0.9f, 0.6f, 1.0f);
    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(Scp096ShyGuy.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> StateStartData = SynchedEntityData.defineId(Scp096ShyGuy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp096ShyGuy.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp096ShyGuy.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BaggedData = SynchedEntityData.defineId(Scp096ShyGuy.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TargetCountData = SynchedEntityData.defineId(Scp096ShyGuy.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SprintLoop = RawAnimation.begin().thenLoop("sprint");
    private static final RawAnimation RageIdleLoop = RawAnimation.begin().thenLoop("rage_idle");
    private static final RawAnimation CryLoop = RawAnimation.begin().thenLoop("cry");
    private static final RawAnimation DistressLoop = RawAnimation.begin().thenLoop("distress");
    private static final RawAnimation PeakLoop = RawAnimation.begin().thenLoop("distress_peak");
    private static final RawAnimation TearLoop = RawAnimation.begin().thenLoop("tear");
    private static final RawAnimation SmashLoop = RawAnimation.begin().thenLoop("smash");
    private static final RawAnimation WeepWalkLoop = RawAnimation.begin().thenLoop("walk_weep");
    private static final RawAnimation LeapPlay = RawAnimation.begin().thenPlayAndHold("leap");
    private static final RawAnimation ChargePlay = RawAnimation.begin().thenPlayAndHold("charge");
    private static final RawAnimation AttackPlay = RawAnimation.begin().thenPlayAndHold("attack");
    private static final RawAnimation SitPlay = RawAnimation.begin().thenPlayAndHold("sit_down");
    private static final RawAnimation RisePlay = RawAnimation.begin().thenPlayAndHold("stand_up");
    private static final RawAnimation CollapsePlay = RawAnimation.begin().thenPlayAndHold("collapse");
    private static final Set<Scp096ShyGuy> Active = Collections.newSetFromMap(new WeakHashMap<>());

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Integer> Targets = new LinkedHashMap<>();
    private final Map<UUID, Integer> LastSeen = new HashMap<>();
    private final Scp096Rampage Rampage = new Scp096Rampage();
    @Nullable
    private UUID Quarry;
    private int StateTicks;
    private int ActionTicks;
    private int NoTarget;
    @Nullable
    private BlockPos Home;
    private int StrollsLeft;
    private int PauseLeft = FirstPause;
    private boolean Strolling;
    private int StrollTicks;
    @Nullable
    private Vec3 StallSpot;
    private int StallClock;
    private boolean LastSeated;
    @Nullable
    private RawAnimation ShownClip;
    private int Stuck;
    private int LeapReady;
    private int LeapTicks;
    private boolean Launched;
    private double LeapGoal;
    private boolean Soaring;
    private double Lift;
    private boolean PathOpen;
    private boolean PathKnown;
    private boolean Mortal;
    private boolean Struck;
    @Nullable
    private Vec3 LastSpot;
    private int TickOrigin;
    private float RenderPartial;
    @Nullable
    private Scp096Gait WalkStride;
    @Nullable
    private Scp096Gait SprintStride;
    @Nullable
    private Scp096Controller BodyController;

    public Scp096ShyGuy(EntityType<? extends Scp096ShyGuy> Type, Level World) {
        super(Type, World);
        xpReward = 80;
        setPersistenceRequired();
        setPathfindingMalus(PathType.WATER, -1.0f);
        setPathfindingMalus(PathType.WATER_BORDER, CalmWaterBorder);
        setPathfindingMalus(PathType.DANGER_FIRE, 12.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 12.0f);
        if (getNavigation() instanceof GroundPathNavigation Ground) {
            Ground.setCanOpenDoors(false);
            Ground.setCanFloat(false);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ARMOR, Armour)
                .add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, Steadiness).add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.STEP_HEIGHT, 1.1).add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 12.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(9, new Scp096Goals.LookAround(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(StateData, StateCalm);
        Builder.define(StateStartData, 0);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(BaggedData, false);
        Builder.define(TargetCountData, 0);
    }

    public byte state() {
        return entityData.get(StateData);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isBagged() {
        return entityData.get(BaggedData);
    }

    public boolean isCalm() {
        return state() == StateCalm;
    }

    public boolean isDistressed() {
        return state() == StateDistressed;
    }

    public boolean isRaging() {
        return state() == StateRaging;
    }

    public boolean isCalming() {
        return state() == StateCalming;
    }

    public int targetCount() {
        return entityData.get(TargetCountData);
    }

    public String stateKey() {
        return switch (state()) {
            case StateDistressed -> "distressed";
            case StateRaging -> "enraged";
            case StateCalming -> "calming";
            default -> action() == ActionCry || action() == ActionWeepWalk ? "crying" : "calm";
        };
    }

    private double elapsed(EntityDataAccessor<Integer> Key, boolean Partial) {
        int Start = entityData.get(Key);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + (Partial ? RenderPartial : 0.0f));
    }

    public double stateElapsed() {
        return elapsed(StateStartData, false);
    }

    public double actionElapsed() {
        return elapsed(ActionStartData, true);
    }

    public double chargeTime() {
        return elapsed(StateStartData, true);
    }

    public float chargeGain() {
        double Share = Mth.clamp(chargeTime() / ScreamTicks, 0.0, 1.0);
        return (float) (ChargeQuiet + (1.0 - ChargeQuiet) * Share * Share);
    }

    public double distressPhase() {
        double Time = chargeTime();
        double Swell = (SwellRate - 1.0) * ScreamTicks / 3.0;
        if (Time <= ScreamTicks) {
            double Share = Time / ScreamTicks;
            return Time + Swell * Share * Share * Share;
        }
        return ScreamTicks + Swell + SwellRate * (Time - ScreamTicks);
    }

    public int chargeKey() {
        return entityData.get(StateStartData);
    }

    public boolean faceCovered() {
        byte Current = action();
        if (isDistressed()) {
            return Current != ActionUncurl;
        }
        return (isCalm() || isCalming()) && Current == ActionCry;
    }

    public boolean canBeSeen() {
        return isAlive() && !isBagged() && !faceCovered();
    }

    public boolean isFacingRelevant() {
        return action() != ActionUncurl;
    }

    public boolean locksMovement() {
        byte Current = action();
        return isDistressed() || isCalming() || Current == ActionCry || Current == ActionSit || Current == ActionRise || Current == ActionLeap || Current == ActionSmash;
    }

    public boolean looksAround() {
        return isCalm() && action() == ActionNone;
    }

    public boolean isWandering() {
        byte Current = action();
        return isCalm() && (Current == ActionNone || Current == ActionWeepWalk);
    }

    public boolean isPausing() {
        return isWandering() && !Strolling;
    }

    public boolean isCrying() {
        byte Current = action();
        return (isCalm() || isCalming()) && (Current == ActionCry || Current == ActionWeepWalk);
    }

    public boolean isEnraged() {
        return isRaging() && targetCount() > 0;
    }

    public boolean isSeated() {
        byte Current = action();
        if (isDistressed()) {
            return Current != ActionCollapse && Current != ActionUncurl;
        }
        if (!isCalm() && !isCalming()) {
            return false;
        }
        return switch (Current) {
            case ActionCry -> true;
            case ActionSit -> actionElapsed() >= SitTicks * 0.5;
            case ActionRise -> actionElapsed() < RiseTicks * 0.5;
            default -> false;
        };
    }

    private boolean crouched() {
        if (isSeated()) {
            return true;
        }
        if (!isCalm() && !isCalming()) {
            return false;
        }
        byte Current = action();
        return Current == ActionSit && actionElapsed() >= SitTicks * SitLow || Current == ActionRise && actionElapsed() < RiseTicks * RiseLow;
    }

    @Nullable
    public String bodyClip() {
        return switch (action()) {
            case ActionUncurl -> "charge";
            case ActionAttack -> "attack";
            case ActionLeap -> "leap";
            case ActionSit -> "sit_down";
            case ActionRise -> "stand_up";
            case ActionCollapse -> "collapse";
            default -> null;
        };
    }

    public boolean timedClip() {
        return bodyClip() != null;
    }

    private void setState(byte Value, int Ticks) {
        entityData.set(StateStartData, tickCount);
        entityData.set(StateData, Value);
        StateTicks = Ticks;
        refreshDimensions();
    }

    private void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
    }

    public void setBagged(boolean Value) {
        entityData.set(BaggedData, Value);
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.scp096.refuses");
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
    public EntityDimensions getDefaultDimensions(Pose Stance) {
        if (isSeated()) {
            return EntityDimensions.scalable(Width, SeatHeight).withEyeHeight(SeatEyeHeight);
        }
        if (isRaging()) {
            return EntityDimensions.scalable(Width, RageHeight).withEyeHeight(RageEyeHeight);
        }
        return super.getDefaultDimensions(Stance);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (StateData.equals(Key) || ActionData.equals(Key)) {
            LastSeated = isSeated();
            refreshDimensions();
        }
        if (ActionStartData.equals(Key) && level().isClientSide && BodyController != null && bodyClip() != null) {
            BodyController.forceAnimationReset();
        }
    }

    @Override
    public void tick() {
        super.tick();
        boolean Seated = isSeated();
        if (Seated != LastSeated) {
            LastSeated = Seated;
            refreshDimensions();
        }
        if (level().isClientSide) {
            trailBubbles();
            Scp096Gait Active = activeGait();
            Scp096Gait Other = Active == walkStride() ? sprintStride() : walkStride();
            Other.reset();
            Active.tick(this);
        }
    }

    private Scp096Gait activeGait() {
        return isRaging() ? sprintStride() : walkStride();
    }

    private Scp096Gait walkStride() {
        if (WalkStride == null) {
            WalkStride = new Scp096Gait(WalkGait);
        }
        return WalkStride;
    }

    private Scp096Gait sprintStride() {
        if (SprintStride == null) {
            SprintStride = new Scp096Gait(SprintGait);
        }
        return SprintStride;
    }

    @Nullable
    public Scp096Gait gaitFor(String Clip) {
        if (!level().isClientSide) {
            return null;
        }
        if (WalkGait.Clip().equals(Clip) || "walk_weep".equals(Clip)) {
            return walkStride();
        }
        if (SprintGait.Clip().equals(Clip)) {
            return sprintStride();
        }
        return null;
    }

    public boolean isStriding() {
        return activeGait().striding();
    }

    public void setGaitPartial(float Partial) {
        RenderPartial = Partial;
        walkStride().setPartial(Partial);
        sprintStride().setPartial(Partial);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        tickRegen();
        updatePathing();
        regainLift(World);
        switch (state()) {
            case StateDistressed -> tickDistress(World);
            case StateRaging -> tickRage(World);
            case StateCalming -> tickCalming(World);
            default -> tickCalm(World);
        }
        clearLift(World);
        if ((tickCount + getId()) % ViewerScan == 0 && canBeSeen()) {
            scanWitnesses(World);
        }
        if ((tickCount + getId()) % 200 == 0) {
            LastSeen.values().removeIf(Tick -> tickCount - Tick > 200);
        }
        if (isRaging() || isDistressed()) {
            Active.add(this);
        } else {
            Active.remove(this);
        }
    }

    private void tickRegen() {
        if (getHealth() >= getMaxHealth() || inVoid()) {
            return;
        }
        heal(RegenPerTick);
    }

    private boolean inVoid() {
        return getY() < level().getMinBuildHeight() - VoidDepth;
    }

    public static boolean mortal(DamageSource Source) {
        return Source.is(DamageTypes.GENERIC_KILL) || Source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    private void updatePathing() {
        boolean Free = isRaging() || isInWater();
        float Water = Free ? 0.0f : -1.0f;
        float Border = Free ? 0.0f : CalmWaterBorder;
        if (getPathfindingMalus(PathType.WATER) != Water) {
            setPathfindingMalus(PathType.WATER, Water);
        }
        if (getPathfindingMalus(PathType.WATER_BORDER) != Border) {
            setPathfindingMalus(PathType.WATER_BORDER, Border);
        }
    }

    @Override
    public int getMaxFallDistance() {
        return isRaging() ? super.getMaxFallDistance() : CalmDrop;
    }

    private void tickCalm(ServerLevel World) {
        if (Home == null || Home.distSqr(blockPosition()) > LeashLost * LeashLost) {
            Home = blockPosition();
        }
        switch (action()) {
            case ActionSit -> {
                hold();
                if (--ActionTicks <= 0) {
                    setAction(ActionCry, CryMin + random.nextInt(CrySpread + 1));
                }
            }
            case ActionCry -> {
                hold();
                if (--ActionTicks <= 0) {
                    setAction(ActionRise, RiseTicks);
                }
            }
            case ActionRise -> {
                hold();
                if (--ActionTicks <= 0) {
                    beginRoam();
                }
            }
            default -> roam(World);
        }
    }

    private void beginRoam() {
        setAction(ActionNone, 0);
        Strolling = false;
        StrollsLeft = StrollsMin + random.nextInt(StrollsSpread + 1);
        PauseLeft = FirstPause;
    }

    private void roam(ServerLevel World) {
        if (Strolling) {
            StrollTicks++;
            if (++StallClock >= StallTicks) {
                boolean Stalled = StallSpot != null && StallSpot.distanceToSqr(position()) < StallDistance * StallDistance;
                StallSpot = position();
                StallClock = 0;
                if (Stalled) {
                    endStroll();
                    return;
                }
            }
            if (getNavigation().isDone() || StrollTicks > StrollLimit) {
                endStroll();
            }
            return;
        }
        if (--PauseLeft > 0) {
            return;
        }
        if (StrollsLeft <= 0 && onGround() && !isInWater()) {
            getNavigation().stop();
            setAction(ActionSit, SitTicks);
            return;
        }
        Vec3 Target = strollTarget(World);
        if (Target == null || !getNavigation().moveTo(Target.x, Target.y, Target.z, WanderSpeed)) {
            PauseLeft = RetryPause;
            StrollsLeft--;
            return;
        }
        Strolling = true;
        StrollTicks = 0;
        StallClock = 0;
        StallSpot = position();
        if (random.nextFloat() < WeepStrollChance) {
            setAction(ActionWeepWalk, 0);
        }
    }

    private void endStroll() {
        Strolling = false;
        StrollsLeft--;
        PauseLeft = PauseMin + random.nextInt(PauseSpread + 1);
        getNavigation().stop();
        if (action() == ActionWeepWalk) {
            setAction(ActionNone, 0);
        }
    }

    @Nullable
    private Vec3 strollTarget(ServerLevel World) {
        Vec3 Here = position();
        BlockPos Anchor = Home == null ? blockPosition() : Home;
        double Away = Math.hypot(Here.x - Anchor.getX() - 0.5, Here.z - Anchor.getZ() - 0.5);
        for (int Attempt = 0; Attempt < StrollTries; Attempt++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            if (Away > LeashRadius * LeashBias) {
                double Back = Math.atan2(Anchor.getZ() + 0.5 - Here.z, Anchor.getX() + 0.5 - Here.x);
                double Spread = Math.PI * Mth.clamp((LeashRadius - Away) / (LeashRadius * (1.0 - LeashBias)), 0.15, 1.0);
                Angle = Back + (random.nextDouble() * 2.0 - 1.0) * Spread;
            }
            double Reach = StrollMin + random.nextDouble() * (StrollMax - StrollMin);
            double X = Here.x + Math.cos(Angle) * Reach;
            double Z = Here.z + Math.sin(Angle) * Reach;
            double Out = Math.hypot(X - Anchor.getX() - 0.5, Z - Anchor.getZ() - 0.5);
            if (Out > LeashRadius && Out > Away) {
                continue;
            }
            BlockPos Spot = footing(World, X, Here.y, Z);
            if (Spot != null) {
                return Vec3.atBottomCenterOf(Spot);
            }
        }
        return null;
    }

    @Nullable
    private BlockPos footing(ServerLevel World, double X, double Y, double Z) {
        BlockPos Base = BlockPos.containing(X, Y, Z);
        for (int Step = 0; Step <= FootingRange * 2; Step++) {
            int Dy = Step % 2 == 0 ? -Step / 2 : (Step + 1) / 2;
            BlockPos Spot = Base.above(Dy);
            if (!World.isLoaded(Spot)) {
                return null;
            }
            BlockPos Below = Spot.below();
            BlockState Floor = World.getBlockState(Below);
            if (Floor.getCollisionShape(World, Below).isEmpty() || !Floor.getFluidState().isEmpty()) {
                continue;
            }
            boolean Room = true;
            for (int Up = 0; Up < Mth.ceil(Height); Up++) {
                BlockPos Cell = Spot.above(Up);
                BlockState Space = World.getBlockState(Cell);
                if (!Space.getCollisionShape(World, Cell).isEmpty() || !Space.getFluidState().isEmpty()) {
                    Room = false;
                    break;
                }
            }
            if (Room && getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(this, Spot)) == 0.0f) {
                return Spot;
            }
        }
        return null;
    }

    private void tickDistress(ServerLevel World) {
        hold();
        if (action() == ActionCollapse && --ActionTicks <= 0) {
            setAction(ActionNone, 0);
        }
        if (StateTicks <= UncurlTicks && action() != ActionUncurl) {
            setAction(ActionUncurl, StateTicks);
        }
        if (--StateTicks <= 0) {
            beginRage(World);
        }
    }

    private void tickCalming(ServerLevel World) {
        hold();
        if (--StateTicks <= 0) {
            Targets.clear();
            Quarry = null;
            syncTargets();
            Home = blockPosition();
            setState(StateCalm, 0);
            setAction(ActionRise, RiseTicks);
        }
    }

    private void tickRage(ServerLevel World) {
        if (LeapReady > 0) {
            LeapReady--;
        }
        LivingEntity Target = pickQuarry(World);
        if (Target == null) {
            Rampage.clear(World, this);
            if (action() != ActionNone) {
                setAction(ActionNone, 0);
                Launched = false;
            }
            getNavigation().stop();
            if (++NoTarget >= (Targets.isEmpty() ? WindDownTicks : SearchTicks)) {
                beginCalming(World);
            }
            return;
        }
        NoTarget = 0;
        Targets.merge(keyFor(Target), 1, Integer::sum);
        if (Targets.getOrDefault(keyFor(Target), 0) > TargetPatience) {
            dropTarget(keyFor(Target));
            return;
        }
        chase(World, Target);
    }

    private UUID keyFor(LivingEntity Target) {
        return Scp096Persons.keyOf(Target);
    }

    @Nullable
    private LivingEntity pickQuarry(ServerLevel World) {
        LivingEntity Current = null;
        double CurrentGap = Double.MAX_VALUE;
        LivingEntity Best = null;
        double BestGap = Double.MAX_VALUE;
        for (UUID Key : new ArrayList<>(Targets.keySet())) {
            if (Scp096Persons.elsewhere(World, Key)) {
                dropTarget(Key);
                continue;
            }
            LivingEntity Found = Scp096Persons.resolve(World, Key);
            if (Found != null && !Targeting.isTargetable(Found)) {
                dropTarget(Key);
                continue;
            }
            if (Found == null || !Scp096Persons.huntable(Found)) {
                continue;
            }
            double Gap = Found.distanceToSqr(this);
            if (Gap > LoseRange * LoseRange) {
                dropTarget(Key);
                continue;
            }
            if (Key.equals(Quarry)) {
                Current = Found;
                CurrentGap = Gap;
            }
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = Found;
            }
        }
        LivingEntity Chosen = Current != null && BestGap > CurrentGap * 0.25 ? Current : Best;
        Quarry = Chosen == null ? null : keyFor(Chosen);
        return Chosen;
    }

    private void chase(ServerLevel World, LivingEntity Target) {
        getLookControl().setLookAt(Target, 60.0f, 60.0f);
        Vec3 Here = position();
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        double Gap = Math.sqrt(Dx * Dx + Dz * Dz);
        double Rise = Target.getY() - getY();
        if (action() == ActionLeap) {
            leap(World, Target, Dx, Dz, Gap);
            return;
        }
        if (action() == ActionAttack) {
            face(Target.position(), 40.0f);
            int Elapsed = (int) actionElapsed();
            if (Elapsed < AttackHit) {
                getMoveControl().setWantedPosition(Target.getX(), Target.getY(), Target.getZ(), LungeSpeed);
            }
            if (Elapsed == AttackHit && !Struck) {
                Struck = true;
                if (Gap <= AttackReach + Target.getBbWidth() * 0.5 && Math.abs(Rise) <= AttackVertical + 0.5 && hasLineOfSight(Target)) {
                    kill(World, Target);
                }
            }
            if (--ActionTicks <= 0) {
                setAction(ActionNone, 0);
            }
            return;
        }
        if (Gap <= AttackStart + Target.getBbWidth() * 0.5 && Math.abs(Rise) <= AttackVertical && hasLineOfSight(Target)) {
            Rampage.clear(World, this);
            face(Target.position(), 60.0f);
            Struck = false;
            setAction(ActionAttack, AttackTicks);
            return;
        }
        if (!PathKnown || tickCount % RepathGap == 0) {
            Path Found = getNavigation().createPath(Target, 0);
            PathKnown = true;
            PathOpen = Found != null && Found.canReach();
            if (PathOpen) {
                getNavigation().moveTo(Found, ChaseSpeed);
            }
        }
        boolean Moved = LastSpot != null && LastSpot.distanceToSqr(Here) > 0.01;
        LastSpot = Here;
        Stuck = Moved ? Math.max(0, Stuck - 2) : Stuck + 1;
        boolean Open = Gap <= DirectRange && Math.abs(Rise) <= DirectRise && hasLineOfSight(Target);
        boolean Direct = !PathOpen || Open || Stuck >= StuckLimit;
        Vec3 Toward = new Vec3(Dx, 0.0, Dz);
        if (Direct && Gap <= ColumnReach && Rise >= LeapRise) {
            hold();
            settle(World, false, false);
            if (onGround() && LeapReady <= 0) {
                startLeap();
            }
            return;
        }
        if (Direct && Gap <= ColumnReach && Rise <= -DropRise) {
            hold();
            settle(World, false, onGround() && Rampage.tearBelow(World, this));
            return;
        }
        Vec3 Heading = Toward;
        if (Direct) {
            if (!PathOpen) {
                getNavigation().stop();
            }
            getMoveControl().setWantedPosition(Target.getX(), Target.getY(), Target.getZ(), ChaseSpeed);
        } else {
            Path Current = getNavigation().getPath();
            if (Current != null && !Current.isDone()) {
                Vec3 Node = Current.getNextEntityPos(this).subtract(Here);
                if (Node.horizontalDistanceSqr() > 0.04) {
                    Heading = Node;
                }
            }
        }
        boolean Blocked = horizontalCollision || Stuck >= StuckLimit;
        boolean Tearing = Blocked && Rampage.tear(World, this, Heading, verticalCollision && !onGround());
        settle(World, Tearing, false);
    }

    private void hold() {
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
    }

    private void settle(ServerLevel World, boolean Tearing, boolean Smashing) {
        byte Wanted = Smashing ? ActionSmash : Tearing ? ActionTear : ActionNone;
        if (!Tearing && !Smashing) {
            Rampage.clear(World, this);
        }
        if (action() != Wanted) {
            setAction(Wanted, 0);
        }
    }

    private void startLeap() {
        getNavigation().stop();
        setAction(ActionLeap, LeapLimit);
        LeapTicks = 0;
        Launched = false;
    }

    private void endLeap() {
        setAction(ActionNone, 0);
        Launched = false;
        LeapReady = LeapGap;
        Stuck = 0;
        LastSpot = null;
        PathKnown = false;
    }

    private void leap(ServerLevel World, LivingEntity Target, double Dx, double Dz, double Gap) {
        LeapTicks++;
        if (!Launched) {
            hold();
            face(Target.position(), 30.0f);
            if (LeapTicks < LeapCrouch) {
                return;
            }
            if (!onGround()) {
                endLeap();
                return;
            }
            LeapGoal = Target.getY();
            double Height = Math.max(LeapClearance, LeapGoal - getY() + LeapClearance);
            setDeltaMovement(0.0, leapSpeed(Height), 0.0);
            hasImpulse = true;
            Launched = true;
            Soaring = true;
            World.playSound(null, getX(), getY(), getZ(), Scp096Registry.RunThud.get(), SoundSource.HOSTILE, 1.6f, 0.7f);
            return;
        }
        Vec3 Motion = getDeltaMovement();
        if (Gap <= AttackReach + Target.getBbWidth() * 0.5 && Math.abs(Target.getY() - getY()) <= AttackVertical + 0.5 && hasLineOfSight(Target)) {
            kill(World, Target);
        }
        if ((getY() >= LeapGoal - 0.25 || Motion.y <= 0.0) && Gap > 0.3) {
            Vec3 Push = new Vec3(Dx, 0.0, Dz).normalize().scale(Math.min(LeapSteer, Gap * 0.5));
            setDeltaMovement(Push.x, getDeltaMovement().y, Push.z);
        }
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
        if (LeapTicks > LeapCrouch + 2 && onGround() || --ActionTicks <= 0) {
            endLeap();
        }
    }

    private void regainLift(ServerLevel World) {
        if (!Soaring) {
            return;
        }
        Vec3 Motion = getDeltaMovement();
        if (onGround() || Lift <= 0.0) {
            Soaring = false;
            return;
        }
        if (!verticalCollision || verticalCollisionBelow || Motion.y >= Lift - LiftSlack) {
            return;
        }
        AABB Body = getBoundingBox();
        Rampage.soar(World, this, Body, Lift, LeapHeadroom);
        if (Scp096Rampage.room(World, this, Body, Lift) > LiftSlack) {
            setDeltaMovement(Motion.x, Lift, Motion.z);
            hasImpulse = true;
        } else {
            Soaring = false;
        }
    }

    private void clearLift(ServerLevel World) {
        if (!Soaring) {
            return;
        }
        double Rise = getDeltaMovement().y;
        if (Rise <= 0.0) {
            Soaring = false;
            return;
        }
        Rampage.soar(World, this, getBoundingBox(), Rise, LeapHeadroom);
        Lift = (Rise - getGravity()) * AirDrag;
    }

    public static double leapApex(double Speed) {
        double Rise = 0.0;
        double Velocity = Speed;
        for (int Tick = 0; Tick < 2000 && Velocity > 0.0; Tick++) {
            Rise += Velocity;
            Velocity = (Velocity - Gravity) * AirDrag;
        }
        return Rise;
    }

    public static double leapSpeed(double Height) {
        double Low = 0.0;
        double High = LeapSpeedCap;
        for (int Step = 0; Step < 48; Step++) {
            double Middle = (Low + High) * 0.5;
            if (leapApex(Middle) < Height) {
                Low = Middle;
            } else {
                High = Middle;
            }
        }
        return High;
    }

    private void face(Vec3 Point, float Step) {
        double Dx = Point.x - getX();
        double Dz = Point.z - getZ();
        if (Dx * Dx + Dz * Dz < 1.0e-4) {
            return;
        }
        float Want = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        float Turned = Mth.approachDegrees(getYRot(), Want, Step);
        setYRot(Turned);
        setYBodyRot(Turned);
        setYHeadRot(Turned);
    }

    private void kill(ServerLevel World, LivingEntity Target) {
        UUID Key = keyFor(Target);
        Vec3 At = Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp096Registry.StrikeLayers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, 1.6f, 0.9f + random.nextFloat() * 0.15f);
        }
        World.sendParticles(ParticleTypes.SWEEP_ATTACK, At.x, At.y, At.z, 2, 0.3, 0.2, 0.3, 0.0);
        World.sendParticles(ParticleTypes.POOF, At.x, At.y, At.z, 10, 0.3, 0.5, 0.3, 0.02);
        Target.invulnerableTime = 0;
        Target.hurt(damageSources().source(Scp096Registry.Caught, this), LethalDamage.against(Target));
        boolean Dead;
        if (Target instanceof ServerPlayer Controller && ControlManager.hasPendingDeath(Controller)) {
            Dead = true;
        } else {
            Dead = Target.isDeadOrDying() || Target.isRemoved();
        }
        if (Dead) {
            dropTarget(Key);
        }
    }

    private void dropTarget(UUID Key) {
        if (Targets.remove(Key) != null) {
            syncTargets();
        }
        if (Key.equals(Quarry)) {
            Quarry = null;
        }
    }

    public void forget(UUID Key) {
        dropTarget(Key);
    }

    private void syncTargets() {
        entityData.set(TargetCountData, Targets.size());
    }

    public List<LivingEntity> targets(ServerLevel World) {
        List<LivingEntity> Found = new ArrayList<>();
        for (UUID Key : Targets.keySet()) {
            LivingEntity Body = Scp096Persons.resolve(World, Key);
            if (Body != null) {
                Found.add(Body);
            }
        }
        return Found;
    }

    public Set<UUID> targetKeys() {
        return Collections.unmodifiableSet(Targets.keySet());
    }

    private void scanWitnesses(ServerLevel World) {
        Vec3 Face = Scp096Sight.faceOf(this);
        Vec3 Facing = Scp096Sight.facingOf(this);
        AABB Area = getBoundingBox().inflate(Scp096Sight.NpcRange);
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> !(Found instanceof Player) && ControlBodies.isPersonLike(Found) && Scp096Persons.isWitness(Found))) {
            if (Targets.containsKey(keyFor(Each))) {
                continue;
            }
            if (Scp096Sight.npcSees(World, Each, Face, Facing)) {
                notice(World, Each);
            }
        }
    }

    public boolean reportSighting(ServerLevel World, ServerPlayer User) {
        Integer Last = LastSeen.get(User.getUUID());
        if (Last != null && tickCount - Last < SeenGap) {
            return false;
        }
        LastSeen.put(User.getUUID(), tickCount);
        if (!isAlive() || isBagged() || !Scp096Sight.plausible(World, User, this)) {
            return false;
        }
        notice(World, User);
        return true;
    }

    public void notice(ServerLevel World, LivingEntity Witness) {
        if (!isAlive() || isBagged() || !Scp096Persons.isWitness(Witness)) {
            return;
        }
        UUID Key = keyFor(Witness);
        if (!Targets.containsKey(Key)) {
            Targets.put(Key, 0);
            syncTargets();
        }
        switch (state()) {
            case StateCalm -> beginDistress(World);
            case StateCalming -> beginDistress(World);
            default -> {
            }
        }
    }

    public void provoke(ServerLevel World, LivingEntity Attacker) {
        if (!isAlive()) {
            return;
        }
        UUID Key = keyFor(Attacker);
        if (!Targets.containsKey(Key)) {
            Targets.put(Key, 0);
            syncTargets();
        }
        if (isCalm() || isCalming()) {
            beginDistress(World);
        }
    }

    public void beginDistress(ServerLevel World) {
        boolean Seated = crouched();
        tearBag(World);
        getNavigation().stop();
        Rampage.clear(World, this);
        Strolling = false;
        setState(StateDistressed, ChargeTicks);
        setAction(Seated ? ActionNone : ActionCollapse, Seated ? 0 : CollapseTicks);
    }

    public void beginRage(ServerLevel World) {
        tearBag(World);
        setState(StateRaging, 0);
        NoTarget = 0;
        Stuck = 0;
        LastSpot = null;
        PathKnown = false;
        setAction(ActionNone, 0);
    }

    public void beginCalming(ServerLevel World) {
        Rampage.clear(World, this);
        getNavigation().stop();
        setState(StateCalming, CalmingTicks);
        setAction(ActionCry, 0);
        Quarry = null;
        NoTarget = 0;
    }

    public void forceCalm(ServerLevel World) {
        Rampage.clear(World, this);
        getNavigation().stop();
        Targets.clear();
        Quarry = null;
        syncTargets();
        setState(StateCalm, 0);
        beginRoam();
    }

    public void forceRage(ServerLevel World, @Nullable LivingEntity Witness) {
        if (Witness != null && Scp096Persons.isWitness(Witness)) {
            UUID Key = keyFor(Witness);
            Targets.putIfAbsent(Key, 0);
            syncTargets();
        }
        if (isCalm() || isCalming()) {
            beginDistress(World);
        }
    }

    private void tearBag(ServerLevel World) {
        if (!isBagged()) {
            return;
        }
        setBagged(false);
        Vec3 Head = position().add(0.0, getBbHeight() * 0.9, 0.0);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Scp096Registry.Bag.get())), Head.x, Head.y, Head.z, 24, 0.3, 0.25, 0.3, 0.12);
        World.playSound(null, Head.x, Head.y, Head.z, Scp096Registry.BagTear.get(), SoundSource.HOSTILE, 1.2f, 0.9f + random.nextFloat() * 0.15f);
    }

    public boolean putBagOn(ServerLevel World) {
        if (isBagged() || !isCalm()) {
            return false;
        }
        setBagged(true);
        World.playSound(null, getX(), getEyeY(), getZ(), Scp096Registry.BagOn.get(), SoundSource.NEUTRAL, 1.0f, 0.95f + random.nextFloat() * 0.1f);
        return true;
    }

    public boolean takeBagOff(ServerLevel World, boolean Drop) {
        if (!isBagged()) {
            return false;
        }
        setBagged(false);
        World.playSound(null, getX(), getEyeY(), getZ(), Scp096Registry.BagOff.get(), SoundSource.NEUTRAL, 1.0f, 0.95f + random.nextFloat() * 0.1f);
        if (Drop) {
            spawnAtLocation(new ItemStack(Scp096Registry.Bag.get()), getBbHeight() * 0.8f);
        }
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (Stack.is(Scp096Registry.Bag.get())) {
            if (isBagged() || !isCalm()) {
                return InteractionResult.PASS;
            }
            if (level() instanceof ServerLevel World && putBagOn(World) && !User.getAbilities().instabuild) {
                Stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (Stack.isEmpty() && User.isSecondaryUseActive() && isBagged()) {
            if (level() instanceof ServerLevel World) {
                takeBagOff(World, true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(User, Hand);
    }

    public static Set<Scp096ShyGuy> active() {
        return Collections.unmodifiableSet(Active);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (Source.is(DamageTypeTags.IS_FALL) || Source.is(DamageTypeTags.IS_DROWNING) || Source.is(DamageTypes.IN_WALL) || Source.is(DamageTypes.CRAMMING)
                || Source.is(DamageTypeTags.IS_FREEZING)) {
            return true;
        }
        return super.isInvulnerableTo(Source);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Lethal = mortal(Source);
        float Taken = Amount;
        if (!Lethal && (isRaging() || isDistressed()) && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            Taken *= RageDamageShare;
        }
        Mortal = Lethal;
        boolean Hurt;
        try {
            Hurt = super.hurt(Source, Taken);
        } finally {
            Mortal = false;
        }
        if (Hurt && !Lethal && isAlive() && level() instanceof ServerLevel World) {
            LivingEntity Attacker = Scp096Persons.attacker(Source, this);
            if (Attacker != null) {
                provoke(World, Attacker);
            }
        }
        return Hurt;
    }

    @Override
    public void setHealth(float Value) {
        if (Value <= 0.0f && !Mortal && isAlive() && !level().isClientSide) {
            super.setHealth(1.0f);
            super.setHealth(Math.min(getMaxHealth(), 1.0f + getMaxHealth() * RecoverShare));
            return;
        }
        super.setHealth(Value);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        if ((isRaging() || isDistressed()) && Effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            return false;
        }
        return super.canBeAffected(Effect);
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            Rampage.clear(World, this);
            if (isBagged()) {
                takeBagOff(World, true);
            }
            World.playSound(null, getX(), getEyeY(), getZ(), Scp096Registry.Death.get(), SoundSource.HOSTILE, 1.5f, 0.8f);
        }
        Active.remove(this);
        super.die(Source);
    }

    @Override
    public void remove(RemovalReason Reason) {
        Active.remove(this);
        super.remove(Reason);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setPersistenceRequired();
        Home = blockPosition();
        beginRoam();
        return super.finalizeSpawn(World, Difficulty, Reason, Group);
    }

    @Override
    public float getWalkTargetValue(BlockPos Pos, LevelReader World) {
        return 0.0f;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
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

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canDrownInFluidType(FluidType Type) {
        return false;
    }

    @Override
    public boolean canSwimInFluidType(FluidType Type) {
        return false;
    }

    @Override
    public void jumpInFluid(FluidType Type) {
        if (onGround()) {
            jumpFromGround();
        }
    }

    @Override
    public void travel(Vec3 Input) {
        if (!isControlledByLocalInstance() || !isInWater()) {
            super.travel(Input);
            return;
        }
        boolean Planted = onGround();
        BlockPos Under = getBlockPosBelowThatAffectsMyMovement();
        float Slip = level().getBlockState(Under).getFriction(level(), Under, this);
        moveRelative(Planted ? getSpeed() * (SlipBase / (Slip * Slip * Slip)) * WaterPace : WaterSteer, Input);
        move(MoverType.SELF, getDeltaMovement());
        Vec3 Motion = getDeltaMovement();
        double Drag = Planted ? Slip * 0.91 : WaterGlide;
        double Rise = (Motion.y - getGravity()) * AirDrag;
        if (horizontalCollision && Input.lengthSqr() > 1.0e-4) {
            Rise = Math.max(Rise, WaterClimb);
        }
        setDeltaMovement(Motion.x * Drag, Rise, Motion.z * Drag);
        calculateEntityAnimation(false);
    }

    public void stirWater(int Limb) {
        Level World = level();
        if (!World.isClientSide || !isInWater()) {
            return;
        }
        BlockPos Below = BlockPos.containing(getX(), getY() - 0.2, getZ());
        BlockState Bed = World.getBlockState(Below);
        Vec3 Ahead = Vec3.directionFromRotation(0.0f, getYRot());
        double Reach = Limb >= 2 ? 0.9 : -0.2;
        double Side = Limb % 2 == 0 ? 0.25 : -0.25;
        double X = getX() + Ahead.x * Reach - Ahead.z * Side;
        double Z = getZ() + Ahead.z * Reach + Ahead.x * Side;
        for (int Index = 0; Index < 6; Index++) {
            World.addParticle(ParticleTypes.BUBBLE, X + (random.nextDouble() - 0.5) * 0.4, getY() + 0.1 + random.nextDouble() * 0.3, Z + (random.nextDouble() - 0.5) * 0.4,
                    (random.nextDouble() - 0.5) * 0.05, 0.05 + random.nextDouble() * 0.05, (random.nextDouble() - 0.5) * 0.05);
        }
        if (!Bed.isAir() && Bed.getFluidState().isEmpty() && Bed.getRenderShape() != RenderShape.INVISIBLE) {
            for (int Index = 0; Index < 5; Index++) {
                World.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Bed), X + (random.nextDouble() - 0.5) * 0.5, getY() + 0.05, Z + (random.nextDouble() - 0.5) * 0.5,
                        (random.nextDouble() - 0.5) * 0.6, 0.2 + random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.6);
            }
        }
    }

    private void trailBubbles() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        if (!isInWater() || tickCount % BubbleGap != 0 || Dx * Dx + Dz * Dz < 1.0e-3) {
            return;
        }
        for (int Index = 0; Index < 2; Index++) {
            level().addParticle(ParticleTypes.BUBBLE, getRandomX(0.6), getY() + random.nextDouble() * getBbHeight(), getRandomZ(0.6), 0.0, 0.06, 0.0);
        }
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp096Registry.Hurt.get();
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
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.6, 1.2, 1.6);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("State", state());
        Tag.putInt("StateTicks", StateTicks);
        Tag.putBoolean("Bagged", isBagged());
        if (Home != null) {
            Tag.putLong("Home", Home.asLong());
        }
        ListTag Saved = new ListTag();
        for (Map.Entry<UUID, Integer> Entry : Targets.entrySet()) {
            CompoundTag One = new CompoundTag();
            One.put("Key", NbtUtils.createUUID(Entry.getKey()));
            One.putInt("Chased", Entry.getValue());
            Saved.add(One);
        }
        Tag.put("Targets", Saved);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Targets.clear();
        ListTag Saved = Tag.getList("Targets", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag One = Saved.getCompound(Index);
            if (One.contains("Key")) {
                Targets.put(NbtUtils.loadUUID(One.get("Key")), One.getInt("Chased"));
            }
        }
        syncTargets();
        setBagged(Tag.getBoolean("Bagged"));
        Home = Tag.contains("Home") ? BlockPos.of(Tag.getLong("Home")) : null;
        byte SavedState = Tag.getByte("State");
        int Left = Tag.getInt("StateTicks");
        setAction(ActionNone, 0);
        switch (SavedState) {
            case StateDistressed -> {
                int Remaining = Mth.clamp(Left, UncurlTicks + 1, ChargeTicks);
                setState(StateDistressed, Remaining);
                entityData.set(StateStartData, tickCount - (ChargeTicks - Remaining));
            }
            case StateRaging -> setState(StateRaging, 0);
            case StateCalming -> {
                setState(StateCalming, Math.max(20, Left));
                setAction(ActionCry, 0);
            }
            default -> {
                setState(StateCalm, 0);
                beginRoam();
            }
        }
        setPersistenceRequired();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Scp096Controller Body = new Scp096Controller(this, "body", 5, Event -> {
            RawAnimation Next = chooseClip();
            if (Next != ShownClip) {
                Event.getController().transitionLength(blendInto(ShownClip, Next));
                ShownClip = Next;
            }
            return Event.setAndContinue(Next);
        });
        BodyController = Body;
        Controllers.add(Body);
    }

    private RawAnimation chooseClip() {
        byte Current = action();
        byte Mood = state();
        if (bodyClip() != null) {
            return switch (Current) {
                case ActionUncurl -> ChargePlay;
                case ActionLeap -> LeapPlay;
                case ActionSit -> SitPlay;
                case ActionRise -> RisePlay;
                case ActionCollapse -> CollapsePlay;
                default -> AttackPlay;
            };
        }
        if (Mood == StateDistressed) {
            return stateElapsed() < PeakTicks ? DistressLoop : PeakLoop;
        }
        if (Current == ActionCry) {
            return CryLoop;
        }
        if (Mood == StateRaging) {
            if (Current == ActionTear) {
                return TearLoop;
            }
            if (Current == ActionSmash) {
                return SmashLoop;
            }
            return isStriding() ? SprintLoop : RageIdleLoop;
        }
        if (isStriding() && (ShownClip == WalkLoop || ShownClip == WeepWalkLoop)) {
            return ShownClip;
        }
        if (Current == ActionWeepWalk && isStriding()) {
            return WeepWalkLoop;
        }
        return isStriding() ? WalkLoop : IdleLoop;
    }

    private int blendInto(@Nullable RawAnimation Former, RawAnimation Next) {
        if (Next == SitPlay) {
            return SitBlend;
        }
        if (Next == RisePlay) {
            return RiseBlend;
        }
        if (Next == CollapsePlay) {
            return CollapseBlend;
        }
        if (Next == ChargePlay) {
            return UncurlBlend;
        }
        if (Next == DistressLoop) {
            return Former == CollapsePlay ? 0 : 6;
        }
        if (Next == PeakLoop) {
            return 6;
        }
        if (Next == CryLoop) {
            return Former == SitPlay ? 0 : SettleBlend;
        }
        if (Next == IdleLoop) {
            return Former == RisePlay ? 0 : 6;
        }
        if (Next == WalkLoop || Next == WeepWalkLoop) {
            return WalkGait.HoldTicks();
        }
        if (Next == SprintLoop) {
            return SprintGait.HoldTicks();
        }
        return 3;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
