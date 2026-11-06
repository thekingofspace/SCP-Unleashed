package net.rai.scp_unleashed.scp082;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp082Fernand extends Monster implements GeoEntity, IEntityWithComplexSpawn, ControlRefusing {
    public static final float Width = 1.4f;
    public static final float Height = 2.9f;
    public static final float EyeHeight = 2.55f;
    public static final double MaxHealth = 320.0;
    public static final double Armour = 14.0;
    public static final double Toughness = 6.0;
    public static final double Steadiness = 0.9;
    public static final double Speed = 0.26;
    public static final double Follow = 40.0;
    public static final double SlamDamage = 16.0;
    public static final float SlamSplash = 9.0f;
    public static final double SlamRadius = 2.2;
    public static final double StrollSpeed = 0.62;
    public static final double FoodSpeed = 0.75;
    public static final double StalkSpeed = 0.9;
    public static final double HuntSpeed = 1.0;
    public static final double FrenzySpeed = 1.12;
    public static final float SpawnHunger = 72.0f;
    public static final float DrainPerTick = 1.0f / 600.0f;
    public static final float RageDrain = 2.0f;
    public static final float FoodCap = 85.0f;
    public static final float MealHeal = 80.0f;
    public static final float SnackHeal = 4.0f;
    public static final float RageCalm = 5.0f;
    public static final double IrritableRange = 12.0;
    public static final double SightRange = 24.0;
    public static final double FrenzyRange = 32.0;
    public static final double ScentRange = 16.0;
    public static final double RageSearch = 48.0;
    public static final double PersonalSpace = 3.0;
    public static final int CrowdTicks = 40;
    public static final double LoseRange = 40.0;
    public static final int LostTicks = 200;
    public static final double FoodRange = 10.0;
    public static final double FoodLure = 6.0;
    public static final double SnackReach = 2.3;
    public static final double GrabStart = 2.6;
    public static final double GrabTolerance = 1.4;
    public static final double GrabVertical = 1.6;
    public static final double SlamStart = 3.0;
    public static final double AvengeRange = 16.0;
    public static final int GrabCooldown = 40;
    public static final int MissCooldown = 30;
    public static final int RescueCooldown = 140;
    public static final int SlamCooldown = 40;
    public static final float EscapeThreshold = 24.0f;
    public static final int ScanInterval = 10;
    public static final int RetaliateTicks = 300;
    public static final int IgnoreTicks = 600;
    public static final int StuckLimit = 100;
    public static final int RageDelay = 3600;
    public static final int RageTicks = 800;
    public static final int RageRestTicks = 1200;
    public static final int SmashGap = 8;
    public static final int StompGap = 160;
    public static final double StompRadius = 4.0;
    public static final float StompDamage = 5.0f;
    public static final float SmashDamage = 8.0f;
    public static final int RegenDelay = 200;
    public static final int RegenInterval = 40;
    public static final int StruggleInterval = 9;
    public static final int MealSpeechDelay = 22;
    public static final int AmbientInterval = 300;
    public static final float PitchLimit = 25.0f;
    public static final float YawLimit = 35.0f;
    public static final byte ActionNone = 0;
    public static final byte ActionGrab = 1;
    public static final byte ActionHold = 2;
    public static final byte ActionEat = 3;
    public static final byte ActionSnack = 4;
    public static final byte ActionSlam = 5;
    public static final byte ActionRoar = 6;
    public static final byte ActionStomp = 7;
    public static final byte ActionSmash = 8;
    public static final byte ActionFlinch = 9;
    public static final Scp082Gait.Profile WalkGait = new Scp082Gait.Profile("walk", 22.0, 0.0663, 0.5, 1.9, 3, new double[]{0.99321, 0.49321}, new float[]{1.0f, 1.0f},
            Scp082Registry.Step, Scp082Registry.Rustle, 0.62f, 0.3f, 0.9f);
    public static final Scp082Gait.Profile StalkGait = new Scp082Gait.Profile("stalk", 15.0, 0.1212, 0.5, 1.9, 2, new double[]{0.99026, 0.49026}, new float[]{1.0f, 1.0f},
            Scp082Registry.Step, Scp082Registry.Rustle, 0.78f, 0.36f, 0.86f);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp082Fernand.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp082Fernand.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> MoodData = SynchedEntityData.defineId(Scp082Fernand.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> HuntingData = SynchedEntityData.defineId(Scp082Fernand.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RagingData = SynchedEntityData.defineId(Scp082Fernand.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TalkingData = SynchedEntityData.defineId(Scp082Fernand.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ProwlLoop = RawAnimation.begin().thenLoop("prowl");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation StalkLoop = RawAnimation.begin().thenLoop("stalk");
    private static final RawAnimation HoldLoop = RawAnimation.begin().thenLoop("hold");
    private static final RawAnimation TalkLoop = RawAnimation.begin().thenLoop("talk");
    private static final Map<String, RawAnimation> Plays = Map.of(
            "grab", RawAnimation.begin().thenPlayAndHold("grab"),
            "eat", RawAnimation.begin().thenPlayAndHold("eat"),
            "snack", RawAnimation.begin().thenPlayAndHold("snack"),
            "slam", RawAnimation.begin().thenPlayAndHold("slam"),
            "roar", RawAnimation.begin().thenPlayAndHold("roar"),
            "stomp", RawAnimation.begin().thenPlayAndHold("stomp"),
            "smash", RawAnimation.begin().thenPlayAndHold("smash"),
            "flinch", RawAnimation.begin().thenPlayAndHold("flinch"));

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Scp082Speech Speech = new Scp082Speech();
    private final Map<UUID, Long> Ignored = new HashMap<>();
    private final Map<Long, Integer> Cracks = new HashMap<>();
    private float Hunger = SpawnHunger;
    private int StarveTicks;
    private int RageLeft;
    private int RageRest;
    private int Meals;
    @Nullable
    private UUID Quarry;
    private boolean Punish;
    private int Unseen;
    private int Stuck;
    @Nullable
    private Vec3 LastSpot;
    @Nullable
    private UUID FoodTarget;
    @Nullable
    private UUID Held;
    private float EscapeDamage;
    @Nullable
    private UUID Attacker;
    private int AttackerUntil;
    private int CrowdTime;
    private int ActionTicks;
    private int GrabReady;
    private int SlamReady;
    private int SmashReady;
    private int StompReady;
    @Nullable
    private BlockPos SmashTarget;
    private ItemStack SnackStack = ItemStack.EMPTY;
    private int SpeechReady = 60;
    private int TalkLeft;
    @Nullable
    private Scp082Speech.Pool PendingLine;
    private int PendingAt;
    private int LastHurtTick = -RegenDelay;
    private boolean JustLoaded = true;
    private int TickOrigin;
    private float RenderPartial;
    @Nullable
    private Scp082Gait WalkStride;
    @Nullable
    private Scp082Gait StalkStride;
    @Nullable
    private Scp082Controller BodyController;

    public Scp082Fernand(EntityType<? extends Scp082Fernand> Type, Level World) {
        super(Type, World);
        xpReward = 60;
        setPersistenceRequired();
        setPathfindingMalus(PathType.WATER, 8.0f);
        setPathfindingMalus(PathType.DANGER_FIRE, 16.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ARMOR, Armour)
                .add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, Steadiness).add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.ATTACK_DAMAGE, SlamDamage).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.7);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new Scp082Goals.Wander(this, StrollSpeed));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(MoodData, (byte) Scp082Mood.of(SpawnHunger).ordinal());
        Builder.define(HuntingData, false);
        Builder.define(RagingData, false);
        Builder.define(TalkingData, false);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public Scp082Mood mood() {
        return Scp082Mood.byIndex(entityData.get(MoodData));
    }

    public boolean isHunting() {
        return entityData.get(HuntingData);
    }

    public boolean isRaging() {
        return entityData.get(RagingData);
    }

    public boolean isTalking() {
        return entityData.get(TalkingData);
    }

    public float hunger() {
        return Hunger;
    }

    public int meals() {
        return Meals;
    }

    public void setHunger(float Value) {
        Hunger = Mth.clamp(Value, 0.0f, 100.0f);
        if (Hunger >= Scp082Mood.StarvingLine) {
            StarveTicks = 0;
        }
        syncMood();
    }

    private void syncMood() {
        byte Value = (byte) Scp082Mood.of(Hunger).ordinal();
        if (entityData.get(MoodData) != Value) {
            entityData.set(MoodData, Value);
        }
    }

    private void setFlag(EntityDataAccessor<Boolean> Key, boolean Value) {
        if (entityData.get(Key) != Value) {
            entityData.set(Key, Value);
        }
    }

    public boolean locksMovement() {
        return action() != ActionNone;
    }

    public boolean isFree() {
        return action() == ActionNone && Quarry == null && FoodTarget == null && !isRaging() && Held == null;
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
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

    private double tickElapsed() {
        int Start = entityData.get(ActionStartData);
        return Math.max(0, tickCount + (level().isClientSide ? TickOrigin : 0) - Start);
    }

    @Nullable
    public String bodyClip() {
        return switch (action()) {
            case ActionGrab -> "grab";
            case ActionHold -> "hold";
            case ActionEat -> "eat";
            case ActionSnack -> "snack";
            case ActionSlam -> "slam";
            case ActionRoar -> "roar";
            case ActionStomp -> "stomp";
            case ActionSmash -> "smash";
            case ActionFlinch -> "flinch";
            default -> null;
        };
    }

    public boolean timedClip() {
        return action() != ActionHold;
    }

    public String stateKey() {
        return switch (action()) {
            case ActionGrab, ActionHold -> "holding";
            case ActionEat, ActionSnack -> "eating";
            case ActionSlam, ActionStomp, ActionSmash, ActionRoar -> isRaging() ? "raging" : "attacking";
            default -> isRaging() ? "raging" : isHunting() ? "hunting" : FoodTarget != null ? "food" : "wandering";
        };
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
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.scp082.refuses");
    }

    @Override
    public void tick() {
        super.tick();
        if (action() != ActionNone) {
            setYBodyRot(getYRot());
            setYHeadRot(getYRot());
        }
        if (level().isClientSide) {
            Scp082Gait Active = activeGait();
            Scp082Gait Other = Active == walkStride() ? stalkStride() : walkStride();
            Other.reset();
            Active.tick(this);
        }
    }

    private Scp082Gait activeGait() {
        return isHunting() || isRaging() ? stalkStride() : walkStride();
    }

    private Scp082Gait walkStride() {
        if (WalkStride == null) {
            WalkStride = new Scp082Gait(WalkGait);
        }
        return WalkStride;
    }

    private Scp082Gait stalkStride() {
        if (StalkStride == null) {
            StalkStride = new Scp082Gait(StalkGait);
        }
        return StalkStride;
    }

    @Nullable
    public Scp082Gait gaitFor(String Clip) {
        if (!level().isClientSide) {
            return null;
        }
        if (WalkGait.Clip().equals(Clip)) {
            return walkStride();
        }
        if (StalkGait.Clip().equals(Clip)) {
            return stalkStride();
        }
        return null;
    }

    public boolean isStriding() {
        return activeGait().striding();
    }

    public void setGaitPartial(float Partial) {
        RenderPartial = Partial;
        walkStride().setPartial(Partial);
        stalkStride().setPartial(Partial);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (JustLoaded) {
            JustLoaded = false;
            if (Held == null && isVehicle()) {
                dropPassengers();
            }
        }
        tickHunger();
        tickRegen();
        tickHeld(World);
        tickAction(World);
        if ((tickCount + getId()) % ScanInterval == 0) {
            scan(World);
        }
        tickRage(World);
        if (action() == ActionNone) {
            pursue(World);
        }
        tickSpeech(World);
        if ((tickCount + getId()) % 200 == 0) {
            long Now = World.getGameTime();
            Ignored.values().removeIf(Until -> Until < Now);
        }
    }

    private void tickHunger() {
        float Drain = DrainPerTick * (isRaging() ? RageDrain : 1.0f);
        Hunger = Math.max(0.0f, Hunger - Drain);
        syncMood();
    }

    private void tickRegen() {
        if (tickCount - LastHurtTick <= RegenDelay || tickCount % RegenInterval != 0 || getHealth() >= getMaxHealth()) {
            return;
        }
        Scp082Mood Mood = mood();
        if (Mood == Scp082Mood.Jovial) {
            heal(2.0f);
        } else if (Mood == Scp082Mood.Irritable) {
            heal(1.0f);
        }
    }

    private void feed(float Amount, float Cap) {
        if (Hunger < Cap) {
            Hunger = Math.min(Cap, Hunger + Amount);
        }
        if (Hunger >= Scp082Mood.StarvingLine) {
            StarveTicks = Math.max(0, StarveTicks - 600);
        }
        syncMood();
    }

    @Nullable
    public LivingEntity held(ServerLevel World) {
        return Held != null && World.getEntity(Held) instanceof LivingEntity Body ? Body : null;
    }

    @Nullable
    public LivingEntity quarry(ServerLevel World) {
        return Quarry != null && World.getEntity(Quarry) instanceof LivingEntity Body ? Body : null;
    }

    @Nullable
    private ItemEntity food(ServerLevel World) {
        return FoodTarget != null && World.getEntity(FoodTarget) instanceof ItemEntity Item && Item.isAlive() && Scp082Persons.isFood(Item.getItem()) ? Item : null;
    }

    public boolean isCarrying(@Nullable Entity Other) {
        return Other != null && hasPassenger(Other);
    }

    public boolean isBlocked() {
        return horizontalCollision || Stuck > 20;
    }

    private void tickHeld(ServerLevel World) {
        if (Held == null) {
            if (isVehicle() && action() != ActionGrab) {
                dropPassengers();
            }
            return;
        }
        LivingEntity Victim = held(World);
        if (Victim == null || !Victim.isAlive() || Victim.isRemoved() || Victim.getVehicle() != this || !Targeting.isTargetable(Victim)
                || Victim instanceof Player User && (User.isCreative() || User.isSpectator())) {
            releaseHeld(World, false);
            return;
        }
        if (!Scp082Hold.heldBy(Victim, this)) {
            Scp082Hold.seize(this, Victim);
        }
        Scp082Hold.lock(Victim);
        Victim.fallDistance = 0.0f;
        if (tickCount % StruggleInterval == 0) {
            World.playSound(null, Victim.getX(), Victim.getY() + 1.0, Victim.getZ(), Scp082Registry.Struggle.get(), SoundSource.HOSTILE, 0.7f, 0.9f + random.nextFloat() * 0.25f);
            World.sendParticles(ParticleTypes.CLOUD, Victim.getX(), Victim.getY() + Victim.getBbHeight() * 0.5, Victim.getZ(), 2, 0.25, 0.3, 0.25, 0.01);
        }
    }

    private void dropPassengers() {
        for (Entity Rider : List.copyOf(getPassengers())) {
            if (Rider instanceof LivingEntity Body) {
                Scp082Hold.release(Body);
            }
            Rider.stopRiding();
        }
    }

    private void releaseHeld(ServerLevel World, boolean Rescued) {
        LivingEntity Victim = held(World);
        Held = null;
        EscapeDamage = 0.0f;
        if (Victim != null) {
            Scp082Hold.release(Victim);
            if (Victim.getVehicle() == this) {
                Victim.stopRiding();
            }
            if (Rescued) {
                Vec3 Push = Vec3.directionFromRotation(0.0f, yBodyRot).scale(0.35);
                Victim.setDeltaMovement(Push.x, 0.3, Push.z);
                Victim.hurtMarked = true;
            }
        }
        dropPassengers();
        byte Current = action();
        if (Current == ActionHold || Current == ActionEat && tickElapsed() < Scp082Beats.BiteTick || Current == ActionGrab && tickElapsed() >= Scp082Beats.GrabContact) {
            setAction(ActionNone, 0);
        }
    }

    private void escape(ServerLevel World) {
        releaseHeld(World, true);
        setAction(ActionFlinch, Scp082Beats.FlinchTicks);
        GrabReady = tickCount + RescueCooldown;
        playSound(Scp082Registry.Hurt.get(), 1.4f, 0.8f);
        speakNow(World, Scp082Speech.Pool.Escape);
    }

    private void tickAction(ServerLevel World) {
        if (ActionTicks <= 0) {
            return;
        }
        byte Current = action();
        int Elapsed = (int) tickElapsed();
        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        boolean Continue = switch (Current) {
            case ActionGrab -> grabTick(World, Elapsed);
            case ActionHold -> true;
            case ActionEat -> eatTick(World, Elapsed);
            case ActionSnack -> snackTick(World, Elapsed);
            case ActionSlam -> slamTick(World, Elapsed);
            case ActionRoar -> roarTick(World, Elapsed);
            case ActionStomp -> stompTick(World, Elapsed);
            case ActionSmash -> smashTick(World, Elapsed);
            default -> true;
        };
        if (!Continue || action() != Current) {
            return;
        }
        if (--ActionTicks <= 0) {
            actionFinished(Current);
        }
    }

    private void actionFinished(byte Kind) {
        if (Kind == ActionGrab && Held != null) {
            setAction(ActionHold, Scp082Beats.HoldTicks);
            return;
        }
        if (Kind == ActionHold && Held != null) {
            setAction(ActionEat, Scp082Beats.EatTicks);
            return;
        }
        if (Kind == ActionEat) {
            GrabReady = tickCount + GrabCooldown;
        }
        setAction(ActionNone, 0);
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

    public Vec3 localToWorld(Vec3 Local) {
        double Yaw = Math.toRadians(yBodyRot);
        double ForwardX = -Math.sin(Yaw);
        double ForwardZ = Math.cos(Yaw);
        double RightX = -Math.cos(Yaw);
        double RightZ = -Math.sin(Yaw);
        return new Vec3(getX() + ForwardX * Local.x + RightX * Local.y, getY() + Local.z, getZ() + ForwardZ * Local.x + RightZ * Local.y);
    }

    public Vec3 mouthWorld() {
        return localToWorld(new Vec3(Scp082Beats.MouthAhead, 0.0, Scp082Beats.MouthHeight));
    }

    private Vec3 heldOffset() {
        double Elapsed = tickElapsed() + 1.0;
        return switch (action()) {
            case ActionGrab -> Scp082Beats.liftPoint(Elapsed);
            case ActionEat -> Elapsed < Scp082Beats.BiteTick ? Scp082Beats.raisePoint(Elapsed) : Scp082Beats.mouthPoint();
            default -> Scp082Beats.holdPoint();
        };
    }

    private boolean withinGrab(LivingEntity Target) {
        Vec3 Hand = localToWorld(Scp082Beats.groundPoint());
        double Dx = Target.getX() - Hand.x;
        double Dz = Target.getZ() - Hand.z;
        return Dx * Dx + Dz * Dz <= GrabTolerance * GrabTolerance && Math.abs(Target.getY() - getY()) <= GrabVertical && hasLineOfSight(Target);
    }

    private boolean grabTick(ServerLevel World, int Elapsed) {
        if (Elapsed < Scp082Beats.GrabContact) {
            LivingEntity Target = quarry(World);
            if (Target != null) {
                face(Target.position(), 25.0f);
            }
            return true;
        }
        if (Elapsed == Scp082Beats.GrabContact && Held == null) {
            LivingEntity Target = quarry(World);
            if (Target != null && Scp082Persons.isPrey(Target, this) && Scp082Persons.fitsHand(Target) && withinGrab(Target) && seize(World, Target)) {
                return true;
            }
            miss(World);
            return false;
        }
        return true;
    }

    private void miss(ServerLevel World) {
        setAction(ActionNone, 0);
        GrabReady = tickCount + MissCooldown;
        World.playSound(null, getX(), getY() + 1.5, getZ(), Scp082Registry.Whiff.get(), SoundSource.HOSTILE, 1.0f, 0.8f + random.nextFloat() * 0.2f);
    }

    private boolean seize(ServerLevel World, LivingEntity Target) {
        if (Target instanceof Player User && User.isSleeping()) {
            User.stopSleepInBed(true, true);
        } else if (Target.isSleeping()) {
            Target.stopSleeping();
        }
        if (Target.isPassenger()) {
            Target.stopRiding();
        }
        Target.ejectPassengers();
        Scp082Hold.seize(this, Target);
        if (!Target.startRiding(this, true)) {
            Scp082Hold.release(Target);
            return false;
        }
        Held = Target.getUUID();
        EscapeDamage = 0.0f;
        World.playSound(null, Target.getX(), Target.getY() + 1.0, Target.getZ(), Scp082Registry.Grab.get(), SoundSource.HOSTILE, 1.2f, 0.85f + random.nextFloat() * 0.1f);
        speakNow(World, Scp082Speech.Pool.Grab);
        return true;
    }

    private boolean eatTick(ServerLevel World, int Elapsed) {
        if (Elapsed == Scp082Beats.BiteTick) {
            LivingEntity Victim = held(World);
            if (Victim != null) {
                devour(World, Victim);
            }
        }
        if (Elapsed > Scp082Beats.BiteTick && Elapsed < Scp082Beats.BurpTick - 4 && (Elapsed - Scp082Beats.BiteTick) % 7 == 0) {
            playSound(Scp082Registry.Chew.get(), 1.0f, 0.85f + random.nextFloat() * 0.15f);
        }
        if (Elapsed == Scp082Beats.BurpTick) {
            playSound(Scp082Registry.Burp.get(), 1.3f, 0.75f + random.nextFloat() * 0.1f);
        }
        return true;
    }

    private void devour(ServerLevel World, LivingEntity Victim) {
        Scp082Persons.Kind Type = Scp082Persons.kindOf(Victim);
        Held = null;
        Scp082Hold.release(Victim);
        if (Victim.getVehicle() == this) {
            Victim.stopRiding();
        }
        Vec3 Mouth = mouthWorld();
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp082Registry.Chomp.get(), SoundSource.HOSTILE, 1.5f, 0.85f + random.nextFloat() * 0.1f);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp082Registry.Crunch.get(), SoundSource.HOSTILE, 1.1f, 0.9f + random.nextFloat() * 0.1f);
        crumbs(World, Mouth, new ItemStack(Items.BREAD), 26);
        Victim.invulnerableTime = 0;
        Victim.hurt(damageSources().source(Scp082Registry.Devoured, this), Float.MAX_VALUE);
        boolean Eaten;
        if (Victim instanceof ServerPlayer Controller && ControlManager.hasPendingDeath(Controller)) {
            LivingEntity Corpse = ControlManager.processDeath(Controller, Body -> {
            });
            Eaten = Corpse != null && (Corpse.isDeadOrDying() || Corpse.isRemoved());
            if (Eaten && !Corpse.isRemoved()) {
                Corpse.remove(RemovalReason.KILLED);
            }
        } else {
            Eaten = Victim.isDeadOrDying() || Victim.isRemoved();
            if (Eaten && Victim instanceof Player) {
                Victim.setInvisible(true);
            } else if (Eaten && !Victim.isRemoved()) {
                Victim.remove(RemovalReason.KILLED);
            }
        }
        dropPassengers();
        if (!Eaten) {
            setAction(ActionFlinch, Scp082Beats.FlinchTicks);
            GrabReady = tickCount + RescueCooldown;
            speakNow(World, Scp082Speech.Pool.Escape);
            return;
        }
        Meals++;
        Hunger = Math.min(100.0f, Hunger + Scp082Persons.mealOf(Type));
        StarveTicks = 0;
        syncMood();
        heal(MealHeal);
        if (RageLeft > 0) {
            endRage(World);
        }
        if (Quarry != null && Quarry.equals(Victim.getUUID())) {
            Quarry = null;
        }
        PendingLine = Scp082Speech.Pool.Meal;
        PendingAt = tickCount + MealSpeechDelay;
    }

    private void crumbs(ServerLevel World, Vec3 At, ItemStack Look, int Count) {
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Look), At.x, At.y, At.z, Count, 0.25, 0.2, 0.25, 0.08);
    }

    private boolean snackTick(ServerLevel World, int Elapsed) {
        if (Elapsed < Scp082Beats.SnackPickup) {
            ItemEntity Item = food(World);
            if (Item != null) {
                face(Item.position(), 25.0f);
            }
            return true;
        }
        if (Elapsed == Scp082Beats.SnackPickup) {
            ItemEntity Item = food(World);
            if (Item == null || Item.distanceToSqr(localToWorld(new Vec3(Scp082Beats.SnackForward, Scp082Beats.SnackRight, 0.0))) > SnackReach * SnackReach) {
                setAction(ActionNone, 0);
                return false;
            }
            ItemStack Stack = Item.getItem();
            SnackStack = Stack.copyWithCount(1);
            Stack.shrink(1);
            if (Stack.isEmpty()) {
                Item.discard();
                FoodTarget = null;
            } else {
                Item.setItem(Stack);
            }
            World.playSound(null, Item.getX(), Item.getY(), Item.getZ(), Scp082Registry.Grab.get(), SoundSource.HOSTILE, 0.6f, 1.3f);
            return true;
        }
        if (Elapsed == Scp082Beats.SnackBite && !SnackStack.isEmpty()) {
            Vec3 Mouth = mouthWorld();
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp082Registry.Chomp.get(), SoundSource.HOSTILE, 1.0f, 1.05f + random.nextFloat() * 0.1f);
            crumbs(World, Mouth, SnackStack, 14);
            feed(Scp082Persons.snackOf(SnackStack), FoodCap);
            heal(SnackHeal);
            SnackStack = ItemStack.EMPTY;
            if (random.nextFloat() < 0.4f && PendingLine == null) {
                PendingLine = Scp082Speech.Pool.Snack;
                PendingAt = tickCount + 14;
            }
        }
        if (Elapsed > Scp082Beats.SnackBite && (Elapsed - Scp082Beats.SnackBite) % 6 == 0) {
            playSound(Scp082Registry.Chew.get(), 0.8f, 1.0f + random.nextFloat() * 0.15f);
        }
        return true;
    }

    private boolean slamTick(ServerLevel World, int Elapsed) {
        LivingEntity Target = quarry(World);
        if (Elapsed < Scp082Beats.SlamHit && Target != null) {
            face(Target.position(), 20.0f);
        }
        if (Elapsed == Scp082Beats.SlamHit) {
            slam(World, Target);
        }
        return true;
    }

    private void slam(ServerLevel World, @Nullable LivingEntity Target) {
        Vec3 Impact = localToWorld(new Vec3(Scp082Beats.SlamAhead, 0.0, 0.0));
        AABB Area = new AABB(Impact.x - SlamRadius, Impact.y - 0.5, Impact.z - SlamRadius, Impact.x + SlamRadius, Impact.y + 2.5, Impact.z + SlamRadius);
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found != this && Found.isAlive() && !isCarrying(Found))) {
            float Damage = Each == Target ? (float) getAttributeValue(Attributes.ATTACK_DAMAGE) : SlamSplash;
            if (Each.hurt(damageSources().mobAttack(this), Damage)) {
                Vec3 Away = Each.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                Away = Away.lengthSqr() > 1.0e-4 ? Away.normalize().scale(0.55) : Vec3.ZERO;
                Each.push(Away.x, 0.35, Away.z);
                Each.hurtMarked = true;
            }
        }
        burst(World, Impact, 34);
        World.playSound(null, Impact.x, Impact.y, Impact.z, Scp082Registry.Slam.get(), SoundSource.HOSTILE, 1.5f, 0.85f + random.nextFloat() * 0.1f);
    }

    private void burst(ServerLevel World, Vec3 At, int Count) {
        BlockPos Below = BlockPos.containing(At.x, At.y - 0.5, At.z);
        BlockState Ground = World.getBlockState(Below);
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), At.x, At.y + 0.1, At.z, Count, 0.8, 0.1, 0.8, 0.15);
        }
        World.sendParticles(ParticleTypes.POOF, At.x, At.y + 0.2, At.z, Count / 4, 0.6, 0.1, 0.6, 0.02);
    }

    private boolean roarTick(ServerLevel World, int Elapsed) {
        if (Elapsed == Scp082Beats.RoarCry) {
            playSound(Scp082Registry.Roar.get(), 2.6f, 0.8f + random.nextFloat() * 0.08f);
        }
        return true;
    }

    private boolean stompTick(ServerLevel World, int Elapsed) {
        if (Elapsed != Scp082Beats.StompHit) {
            return true;
        }
        Vec3 Foot = localToWorld(new Vec3(0.3, 0.26, 0.0));
        AABB Area = getBoundingBox().inflate(StompRadius, 0.5, StompRadius);
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found != this && Found.isAlive() && Found.onGround() && !isCarrying(Found))) {
            if (Each.distanceToSqr(this) <= StompRadius * StompRadius && Each.hurt(damageSources().mobAttack(this), StompDamage)) {
                Each.push(0.0, 0.3, 0.0);
                Each.hurtMarked = true;
            }
        }
        burst(World, Foot, 40);
        World.playSound(null, Foot.x, Foot.y, Foot.z, Scp082Registry.Stomp.get(), SoundSource.HOSTILE, 2.0f, 0.8f + random.nextFloat() * 0.1f);
        return true;
    }

    private boolean smashTick(ServerLevel World, int Elapsed) {
        if (SmashTarget != null && Elapsed < Scp082Beats.SmashHit) {
            face(Vec3.atCenterOf(SmashTarget), 30.0f);
        }
        if (Elapsed == Scp082Beats.SmashHit && SmashTarget != null) {
            Vec3 Strike = Vec3.atCenterOf(SmashTarget);
            for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, new AABB(SmashTarget).inflate(1.2), Found -> Found != this && Found.isAlive() && !isCarrying(Found))) {
                if (Each.distanceToSqr(Strike) < 2.6) {
                    Each.hurt(damageSources().mobAttack(this), SmashDamage);
                }
            }
            Scp082Rage.strike(World, this, SmashTarget, Cracks);
        }
        return true;
    }

    private void scan(ServerLevel World) {
        long Now = World.getGameTime();
        LivingEntity Current = quarry(World);
        if (Current != null && !keepQuarry(World, Current)) {
            Quarry = null;
            Punish = false;
            Current = null;
            Stuck = 0;
        }
        if (Quarry != null && Current == null) {
            Quarry = null;
            Punish = false;
        }
        Scp082Mood Mood = mood();
        if (Current == null) {
            LivingEntity Avenge = Attacker != null && tickCount < AttackerUntil && World.getEntity(Attacker) instanceof LivingEntity Body && Body.isAlive() && Targeting.isTargetable(Body)
                    && Body.distanceToSqr(this) < AvengeRange * AvengeRange && !(Body instanceof Player User && (User.isCreative() || User.isSpectator() || Scp268.isNeglected(User))) ? Body : null;
            if (Avenge != null) {
                Quarry = Avenge.getUUID();
                Punish = Mood == Scp082Mood.Jovial || !Scp082Persons.isPrey(Avenge, this);
                Unseen = 0;
                Stuck = 0;
            } else {
                LivingEntity Found = findPrey(World, Mood, Now);
                if (Found != null) {
                    Quarry = Found.getUUID();
                    Punish = false;
                    Unseen = 0;
                    Stuck = 0;
                }
            }
        }
        ItemEntity Item = food(World);
        if (Hunger >= FoodCap) {
            FoodTarget = null;
        } else if (Item == null) {
            FoodTarget = null;
            ItemEntity Nearest = null;
            double Best = FoodRange * FoodRange;
            for (ItemEntity Each : World.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(FoodRange), Found -> Found.isAlive() && Scp082Persons.isFood(Found.getItem()))) {
                double Gap = Each.distanceToSqr(this);
                if (Gap < Best && hasLineOfSight(Each)) {
                    Best = Gap;
                    Nearest = Each;
                }
            }
            if (Nearest != null) {
                FoodTarget = Nearest.getUUID();
            }
        }
        setFlag(HuntingData, Quarry != null && !Punish || isRaging());
    }

    private boolean keepQuarry(ServerLevel World, LivingEntity Target) {
        if (!Target.isAlive() || Target.isRemoved() || Target.distanceToSqr(this) > LoseRange * LoseRange || Target.isSpectator() || !Targeting.isTargetable(Target)) {
            return false;
        }
        if (Target instanceof Player User && (User.isCreative() || Scp268.isNeglected(User))) {
            return false;
        }
        if (Punish) {
            return tickCount < AttackerUntil + RetaliateTicks;
        }
        if (!Scp082Persons.isPrey(Target, this) || mood() == Scp082Mood.Jovial && !isRaging()) {
            return false;
        }
        Unseen = hasLineOfSight(Target) ? 0 : Unseen + ScanInterval;
        return Unseen < LostTicks || isRaging();
    }

    @Nullable
    private LivingEntity findPrey(ServerLevel World, Scp082Mood Mood, long Now) {
        if (Mood == Scp082Mood.Jovial && !isRaging()) {
            CrowdTime = 0;
            return null;
        }
        double Range = switch (Mood) {
            case Irritable -> IrritableRange;
            case Ravenous -> SightRange;
            default -> FrenzyRange;
        };
        LivingEntity Best = null;
        double BestGap = Double.MAX_VALUE;
        Player Crowding = null;
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Range), Found -> Scp082Persons.isPrey(Found, this))) {
            if (Ignored.getOrDefault(Each.getUUID(), Long.MIN_VALUE) > Now) {
                continue;
            }
            double Gap = Each.distanceToSqr(this);
            boolean Sensed = isRaging() && Gap < ScentRange * ScentRange;
            if (!Sensed && (!Scp082Persons.isNoticeable(Each) || !hasLineOfSight(Each))) {
                continue;
            }
            if (Mood == Scp082Mood.Irritable && Each instanceof Player User) {
                if (Gap < PersonalSpace * PersonalSpace) {
                    Crowding = User;
                }
                continue;
            }
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = Each;
            }
        }
        if (Mood == Scp082Mood.Irritable) {
            CrowdTime = Crowding != null ? CrowdTime + ScanInterval : 0;
            if (Best == null && Crowding != null && CrowdTime >= CrowdTicks) {
                CrowdTime = 0;
                return Crowding;
            }
        }
        return Best;
    }

    private double huntSpeed() {
        if (isRaging() || mood() == Scp082Mood.Starving) {
            return FrenzySpeed;
        }
        if (Punish || mood() == Scp082Mood.Ravenous) {
            return HuntSpeed;
        }
        return StalkSpeed;
    }

    private void pursue(ServerLevel World) {
        ItemEntity Item = food(World);
        LivingEntity Target = quarry(World);
        if (Item != null && (Target == null || Item.distanceToSqr(this) < FoodLure * FoodLure && Target.distanceToSqr(this) > 16.0)) {
            approachFood(Item);
            return;
        }
        if (Target != null) {
            chase(World, Target);
            return;
        }
        Stuck = 0;
        LastSpot = null;
        if (isRaging()) {
            rageWander(World);
        }
    }

    private void approachFood(ItemEntity Item) {
        getLookControl().setLookAt(Item, 30.0f, 30.0f);
        Vec3 Pick = localToWorld(new Vec3(Scp082Beats.SnackForward, Scp082Beats.SnackRight, 0.0));
        double Dx = Item.getX() - getX();
        double Dz = Item.getZ() - getZ();
        boolean Close = Dx * Dx + Dz * Dz <= (SnackReach + 0.4) * (SnackReach + 0.4) && Math.abs(Item.getY() - getY()) < 1.5;
        if (Close && Item.distanceToSqr(Pick) <= SnackReach * SnackReach) {
            setAction(ActionSnack, Scp082Beats.SnackTicks);
            return;
        }
        if (Close) {
            face(Item.position(), 30.0f);
            return;
        }
        if (getNavigation().isDone() || tickCount % 10 == 0) {
            getNavigation().moveTo(Item, mood().hungry() ? StalkSpeed : FoodSpeed);
        }
    }

    private void chase(ServerLevel World, LivingEntity Target) {
        getLookControl().setLookAt(Target, 30.0f, 30.0f);
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        double Gap = Math.sqrt(Dx * Dx + Dz * Dz);
        double Rise = Math.abs(Target.getY() - getY());
        boolean CanGrab = !Punish && Scp082Persons.isPrey(Target, this) && Scp082Persons.fitsHand(Target);
        if (CanGrab && tickCount >= GrabReady && Gap <= GrabStart && Rise <= GrabVertical && hasLineOfSight(Target)) {
            face(Target.position(), 45.0f);
            setAction(ActionGrab, Scp082Beats.GrabTicks);
            return;
        }
        if ((!CanGrab || tickCount < GrabReady) && tickCount >= SlamReady && Gap <= SlamStart && Rise < 2.0 && hasLineOfSight(Target)) {
            face(Target.position(), 45.0f);
            setAction(ActionSlam, Scp082Beats.SlamTicks);
            SlamReady = tickCount + SlamCooldown + Scp082Beats.SlamTicks;
            return;
        }
        if (getNavigation().isDone() || tickCount % 10 == 0) {
            getNavigation().moveTo(Target, huntSpeed());
        }
        trackStuck(Gap);
    }

    private void trackStuck(double Gap) {
        Vec3 Here = position();
        boolean Moved = LastSpot != null && LastSpot.distanceToSqr(Here) > 0.0025;
        LastSpot = Here;
        if (Gap < GrabStart || Moved) {
            Stuck = Math.max(0, Stuck - 2);
            return;
        }
        Stuck++;
        if (Stuck >= StuckLimit && !isRaging() && Quarry != null && level() instanceof ServerLevel World) {
            Ignored.put(Quarry, World.getGameTime() + IgnoreTicks);
            Quarry = null;
            Punish = false;
            Stuck = 0;
            getNavigation().stop();
        }
    }

    private void tickRage(ServerLevel World) {
        if (Hunger < Scp082Mood.StarvingLine) {
            StarveTicks++;
        } else {
            StarveTicks = Math.max(0, StarveTicks - 4);
        }
        if (RageLeft > 0) {
            RageLeft--;
            if (RageLeft <= 0 || Hunger >= Scp082Mood.StarvingLine + RageCalm) {
                endRage(World);
            } else {
                rageTick(World);
            }
            return;
        }
        if (RageRest > 0) {
            RageRest--;
            return;
        }
        if (StarveTicks >= RageDelay) {
            startRage(World);
        }
    }

    public void startRage(ServerLevel World) {
        RageLeft = RageTicks;
        setFlag(RagingData, true);
        setFlag(HuntingData, true);
        SmashReady = tickCount + 30;
        StompReady = tickCount + StompGap / 2;
        speakNow(World, Scp082Speech.Pool.Rage);
        if (action() == ActionNone) {
            setAction(ActionRoar, Scp082Beats.RoarTicks);
        }
    }

    private void endRage(ServerLevel World) {
        RageLeft = 0;
        RageRest = RageRestTicks;
        setFlag(RagingData, false);
        if (SmashTarget != null) {
            World.destroyBlockProgress(getId(), SmashTarget, -1);
            SmashTarget = null;
        }
    }

    private void rageTick(ServerLevel World) {
        if (action() != ActionNone || Held != null) {
            return;
        }
        if (tickCount >= StompReady) {
            StompReady = tickCount + StompGap;
            setAction(ActionStomp, Scp082Beats.StompTicks);
            return;
        }
        if (tickCount < SmashReady) {
            return;
        }
        Vec3 Heading = rageHeading(World);
        boolean Near = Heading.lengthSqr() < 1.0e-4;
        if (!isBlocked() && !Near && quarry(World) != null && !getNavigation().isDone()) {
            return;
        }
        BlockPos Target = Scp082Rage.pick(World, this, Heading);
        if (Target == null) {
            return;
        }
        if (SmashTarget != null && !SmashTarget.equals(Target)) {
            World.destroyBlockProgress(getId(), SmashTarget, -1);
        }
        SmashTarget = Target;
        face(Vec3.atCenterOf(Target), 45.0f);
        setAction(ActionSmash, Scp082Beats.SmashTicks);
        SmashReady = tickCount + Scp082Beats.SmashTicks + SmashGap;
    }

    private Vec3 rageHeading(ServerLevel World) {
        LivingEntity Target = quarry(World);
        Vec3 Goal = Target != null ? Target.position() : null;
        if (Goal == null) {
            Player Nearest = World.getNearestPlayer(this, RageSearch);
            if (Nearest != null && !Nearest.isCreative() && !Nearest.isSpectator() && !Scp268.isNeglected(Nearest)) {
                Goal = Nearest.position();
            }
        }
        if (Goal == null) {
            return Vec3.directionFromRotation(0.0f, getYRot());
        }
        return Goal.subtract(position()).multiply(1.0, 0.0, 1.0);
    }

    private void rageWander(ServerLevel World) {
        Vec3 Heading = rageHeading(World);
        if (Heading.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 Goal = position().add(Heading.normalize().scale(8.0));
        if (getNavigation().isDone() || tickCount % 40 == 0) {
            getNavigation().moveTo(Goal.x, Goal.y, Goal.z, FrenzySpeed);
        }
        trackStuck(99.0);
    }

    private void tickSpeech(ServerLevel World) {
        if (TalkLeft > 0) {
            TalkLeft--;
            if (TalkLeft <= 0) {
                setFlag(TalkingData, false);
            } else if (TalkLeft % Scp082Speech.VoiceGap == 0) {
                voice();
            }
        }
        if (PendingLine != null && tickCount >= PendingAt && action() != ActionGrab) {
            Scp082Speech.Pool Line = PendingLine;
            PendingLine = null;
            speakNow(World, Line);
        }
        if (tickCount < SpeechReady) {
            return;
        }
        byte Current = action();
        if (Current != ActionNone && Current != ActionHold) {
            return;
        }
        if (!Scp082Speech.anyoneNear(World, this)) {
            SpeechReady = tickCount + 40;
            return;
        }
        speakNow(World, isRaging() ? Scp082Speech.Pool.Rage : Scp082Speech.Pool.of(mood()));
    }

    private void speakNow(ServerLevel World, Scp082Speech.Pool Source) {
        String Line = Speech.pick(Source, random);
        Scp082Speech.say(World, this, Line, isRaging() ? Scp082Mood.Starving : mood());
        TalkLeft = Scp082Speech.TalkTicks;
        setFlag(TalkingData, true);
        voice();
        SpeechReady = tickCount + Scp082Speech.RambleMin + random.nextInt(Scp082Speech.RambleMax - Scp082Speech.RambleMin);
        if (Source == Scp082Speech.Pool.Jovial && random.nextFloat() < 0.3f || Source == Scp082Speech.Pool.Meal) {
            World.playSound(null, getX(), getEyeY(), getZ(), Scp082Registry.Laugh.get(), SoundSource.HOSTILE, 1.3f, 0.9f + random.nextFloat() * 0.1f);
        }
    }

    private void voice() {
        Scp082Mood Mood = isRaging() ? Scp082Mood.Starving : mood();
        playSound(Scp082Registry.Voice.get(), 1.5f, Mood.VoicePitch * (0.95f + random.nextFloat() * 0.1f));
    }

    public void forceRage(ServerLevel World) {
        StarveTicks = RageDelay;
        RageRest = 0;
        if (Hunger >= Scp082Mood.StarvingLine) {
            setHunger(Scp082Mood.StarvingLine - 1.0f);
        }
        startRage(World);
    }

    public int releaseAll(ServerLevel World) {
        int Count = Held != null ? 1 : 0;
        releaseHeld(World, false);
        return Count;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (!Hurt || !(level() instanceof ServerLevel World) || !isAlive()) {
            return Hurt;
        }
        LastHurtTick = tickCount;
        if (Held != null) {
            EscapeDamage += Amount;
            if (EscapeDamage >= EscapeThreshold) {
                escape(World);
            }
        }
        if (Source.getEntity() instanceof LivingEntity Striker && Striker != this) {
            Attacker = Striker.getUUID();
            AttackerUntil = tickCount + RetaliateTicks;
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            releaseHeld(World, false);
            if (SmashTarget != null) {
                World.destroyBlockProgress(getId(), SmashTarget, -1);
            }
        }
        super.die(Source);
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (level() instanceof ServerLevel World && Held != null && Reason != RemovalReason.UNLOADED_TO_CHUNK && Reason != RemovalReason.UNLOADED_WITH_PLAYER) {
            releaseHeld(World, false);
        }
        super.remove(Reason);
    }

    @Override
    protected void positionRider(Entity Passenger, MoveFunction Callback) {
        if (!hasPassenger(Passenger)) {
            return;
        }
        Vec3 At = localToWorld(heldOffset());
        Callback.accept(Passenger, At.x, At.y, At.z);
        if (Passenger instanceof LivingEntity Body && !(Passenger instanceof Player)) {
            float Facing = yBodyRot + 180.0f;
            Body.setYRot(Facing);
            Body.setYBodyRot(Facing);
            Body.setYHeadRot(Facing);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity Passenger) {
        Vec3 Spot = localToWorld(new Vec3(Scp082Beats.GroundForward, Scp082Beats.GroundRight, 0.0));
        if (level().noCollision(Passenger, Passenger.getDimensions(Passenger.getPose()).makeBoundingBox(Spot))) {
            return Spot;
        }
        return super.getDismountLocationForPassenger(Passenger);
    }

    @Override
    protected boolean canAddPassenger(Entity Passenger) {
        return Passenger instanceof LivingEntity Body && Scp082Hold.isHeld(Body) && getPassengers().isEmpty();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    @Override
    public boolean showVehicleHealth() {
        return false;
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

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return super.causeFallDamage(Distance, Multiplier * 0.5f, Source);
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
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (action() != ActionNone || isTalking()) {
            return null;
        }
        if (mood().hungry() || isRaging()) {
            return Scp082Registry.Growl.get();
        }
        return random.nextFloat() < 0.35f ? Scp082Registry.Laugh.get() : null;
    }

    @Override
    protected float getSoundVolume() {
        return 1.3f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp082Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp082Registry.Death.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.2, 0.6, 1.2);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putFloat("Hunger", Hunger);
        Tag.putInt("StarveTicks", StarveTicks);
        Tag.putInt("RageLeft", RageLeft);
        Tag.putInt("RageRest", RageRest);
        Tag.putInt("Meals", Meals);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Hunger = Tag.contains("Hunger") ? Mth.clamp(Tag.getFloat("Hunger"), 0.0f, 100.0f) : SpawnHunger;
        StarveTicks = Tag.getInt("StarveTicks");
        RageLeft = Tag.getInt("RageLeft");
        RageRest = Tag.getInt("RageRest");
        Meals = Tag.getInt("Meals");
        Held = null;
        JustLoaded = true;
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        setFlag(RagingData, RageLeft > 0);
        syncMood();
        setPersistenceRequired();
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
        Scp082Controller Body = new Scp082Controller(this, "body", 5, Event -> {
            String Clip = bodyClip();
            Event.getController().transitionLength(Clip != null ? 3 : 5);
            if (action() == ActionHold) {
                return Event.setAndContinue(HoldLoop);
            }
            if (Clip != null) {
                return Event.setAndContinue(Plays.get(Clip));
            }
            boolean Eager = isHunting() || isRaging();
            if (isStriding()) {
                return Event.setAndContinue(Eager ? StalkLoop : WalkLoop);
            }
            return Event.setAndContinue(Eager || mood().hungry() ? ProwlLoop : IdleLoop);
        });
        BodyController = Body;
        Controllers.add(Body);
        Controllers.add(new AnimationController<>(this, "voice", 3, Event -> {
            byte Current = action();
            return isTalking() && (Current == ActionNone || Current == ActionHold) ? Event.setAndContinue(TalkLoop) : PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
