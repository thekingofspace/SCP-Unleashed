package net.rai.scp_unleashed.scp999;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.motion.ActionClock;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp999Blob extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn, ActionClock.Owner, ControlRefusing {
    public static final float Width = 0.8f;
    public static final float Height = 0.62f;
    public static final float EyeHeight = 0.38f;
    public static final double MaxHealth = 30.0;
    public static final double Speed = 0.25;
    public static final double FollowRange = 24.0;
    public static final double WanderModifier = 0.7;
    public static final double FollowModifier = 1.0;
    public static final double RushModifier = 1.25;
    public static final double BegModifier = 1.05;
    public static final double PlayModifier = 0.9;
    public static final double RegenRadius = 5.0;
    public static final int RegenTicks = 100;
    public static final int RegenRefresh = 60;
    public static final int RegenInterval = 20;
    public static final int SelfHealInterval = 100;
    public static final float SelfHeal = 1.0f;
    public static final int SafeTicks = 60;
    public static final int HurtFear = 80;
    public static final int ScanInterval = 10;
    public static final int LikeThreshold = 6;
    public static final int MaxAffection = 60;
    public static final int PetAffection = 3;
    public static final int FeedAffection = 5;
    public static final int HugAffection = 4;
    public static final int GreetAffection = 1;
    public static final int ArrivalAffection = 2;
    public static final int HurtAffection = -12;
    public static final int PetCooldown = 12;
    public static final int HugCooldown = 900;
    public static final double HugRange = 1.6;
    public static final double HugSeek = 4.0;
    public static final int HugStill = 60;
    public static final float HugChance = 0.35f;
    public static final double GreetRange = 6.0;
    public static final int GreetMemory = 2400;
    public static final int GreetCooldown = 160;
    public static final int CrouchWindow = 30;
    public static final int CrouchToggles = 4;
    public static final double CrouchRange = 5.0;
    public static final double CandySight = 8.0;
    public static final int CandyMemory = 600;
    public static final int TreatCooldown = 400;
    public static final int HandCooldown = 40;
    public static final int CravingMin = 2400;
    public static final int CravingMax = 4800;
    public static final int CravingTicks = 1200;
    public static final double EatReach = 1.5;
    public static final int BiteTick = 12;
    public static final float EatHeal = 4.0f;
    public static final int NapCheck = 100;
    public static final float NapChance = 0.25f;
    public static final int NapMin = 600;
    public static final int NapMax = 1200;
    public static final int SunLight = 14;
    public static final int ShapeMin = 400;
    public static final int ShapeMax = 1000;
    public static final float HeartChance = 0.35f;
    public static final int HumMin = 300;
    public static final int HumMax = 700;
    public static final int CuriousTicks = 240;
    public static final int AmbientInterval = 160;
    public static final float StepVolume = 0.22f;
    public static final float VoiceVolume = 0.7f;
    public static final float LookTurn = 40.0f;
    public static final float LookPitch = 30.0f;
    public static final float BodyLead = 30.0f;
    public static final float BodyLimit = 55.0f;
    public static final byte StateNormal = 0;
    public static final byte StateFlat = 1;
    public static final byte StateSleep = 2;
    public static final byte ActionNone = 0;
    public static final byte ActionPet = 1;
    public static final byte ActionEat = 2;
    public static final byte ActionWobble = 3;
    public static final byte ActionGreet = 4;
    public static final byte ActionExcited = 5;
    public static final byte ActionHug = 6;
    public static final byte ActionPlay = 7;
    public static final byte ActionNuzzle = 8;
    public static final byte ActionHum = 9;
    public static final byte ActionPeek = 10;
    public static final byte ActionWake = 11;
    public static final byte ActionFlatten = 12;
    public static final byte ActionDrop = 13;
    public static final byte ActionPuddle = 14;
    public static final byte ActionLumpy = 15;
    public static final byte ActionHeart = 16;
    public static final int PetTicks = 18;
    public static final int EatTicks = 36;
    public static final int WobbleTicks = 28;
    public static final int GreetTicks = 24;
    public static final int ExcitedTicks = 32;
    public static final int HugTicks = 60;
    public static final int PlayTicks = 24;
    public static final int NuzzleTicks = 20;
    public static final int HumTicks = 56;
    public static final int PeekTicks = 36;
    public static final int WakeTicks = 32;
    public static final int FlattenTicks = 6;
    public static final int ShapeTicks = 72;
    public static final int PokeTick = 6;
    public static final double WalkDistance = 0.5;
    public static final double RunDistance = 0.875;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.006, 0.12, 0.1, WalkDistance, RunDistance, 0.25, 2);
    public static final double[] Contacts = {0.0};
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp999Blob.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp999Blob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(Scp999Blob.class, EntityDataSerializers.BYTE);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation HopLoop = RawAnimation.begin().thenLoop("hop");
    private static final RawAnimation BoundLoop = RawAnimation.begin().thenLoop("bound");
    private static final RawAnimation SleepLoop = RawAnimation.begin().thenLoop("sleep");
    private static final RawAnimation FlatLoop = RawAnimation.begin().thenLoop("flat");
    private static final Map<String, RawAnimation> Plays = new HashMap<>();

    static {
        for (String Name : List.of("pet", "eat", "wobble", "greet", "excited", "hug", "play", "nuzzle", "hum", "peek", "wake", "flatten", "shape_drop", "shape_puddle",
                "shape_lumpy", "shape_heart")) {
            Plays.put(Name, RawAnimation.begin().thenPlayAndHold(Name));
        }
    }

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ActionClock Clock = new ActionClock();
    private final StrideGait Stride = new StrideGait();
    private final Scp999Eyes Eyes = new Scp999Eyes();
    private final Map<UUID, Integer> Affection = new HashMap<>();
    private final Map<UUID, Integer> Greeted = new HashMap<>();
    private final Map<UUID, Integer> Noticed = new HashMap<>();
    private final Map<UUID, Crouching> Crouches = new HashMap<>();
    private final Map<UUID, Stillness> Still = new HashMap<>();
    @Nullable
    private AnimationController<Scp999Blob> BodyController;
    private int ActionTicks;
    private int TickOrigin;
    private float RenderPartial;
    private int DangerLeft;
    @Nullable
    private Vec3 DangerFrom;
    private int SleepLeft;
    private int PetLeft;
    private int HugLeft;
    private int GreetLeft;
    private int TreatLeft;
    private int HandLeft;
    private int CravingAt;
    private int CravingLeft;
    private int ShapeLeft;
    private int HumLeft;
    private int CuriousLeft;
    private int PlayLeft;
    @Nullable
    private UUID CuriousAbout;
    @Nullable
    private UUID Sought;
    @Nullable
    private BlockPos MealBowl;
    @Nullable
    private ItemEntity MealItem;
    private boolean MealHand;
    @Nullable
    private UUID MealFrom;
    @Nullable
    private Entity Facing;
    @Nullable
    private ItemEntity Toy;
    private byte QueuedShape = ActionNone;
    private int Meals;

    private static final class Crouching {
        private boolean Down;
        private final int[] Times = new int[CrouchToggles];
        private int Count;
    }

    private static final class Stillness {
        private Vec3 Spot = Vec3.ZERO;
        private int Since;
    }

    public Scp999Blob(EntityType<? extends Scp999Blob> Type, Level World) {
        super(Type, World);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0f);
        setPathfindingMalus(PathType.LAVA, -1.0f);
        setPathfindingMalus(PathType.WATER, 4.0f);
        CravingAt = CravingMin + (World.getRandom().nextInt(CravingMax - CravingMin + 1));
        ShapeLeft = ShapeMin + World.getRandom().nextInt(ShapeMax - ShapeMin + 1);
        HumLeft = HumMin;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.FOLLOW_RANGE, FollowRange);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        GroundPathNavigation Ground = new GroundPathNavigation(this, World);
        Ground.setCanFloat(true);
        Ground.setCanOpenDoors(false);
        return Ground;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                float Head = getYHeadRot();
                float Body = yBodyRot;
                float Delta = Mth.wrapDegrees(Head - Body);
                Vec3 Motion = getDeltaMovement();
                if (Motion.x * Motion.x + Motion.z * Motion.z > 2.5e-5) {
                    Body = Mth.approachDegrees(Body, getYRot(), 20.0f);
                    float Turn = Mth.wrapDegrees(Head - Body);
                    if (Math.abs(Turn) > BodyLimit) {
                        setYHeadRot(Body + Math.signum(Turn) * BodyLimit);
                    }
                    setYBodyRot(Body);
                    return;
                }
                if (Math.abs(Delta) > BodyLead) {
                    Body = Mth.approachDegrees(Body, Head, 8.0f);
                }
                float After = Mth.wrapDegrees(Head - Body);
                if (Math.abs(After) > BodyLimit) {
                    Body = Head - Math.signum(After) * BodyLimit;
                }
                setYBodyRot(Body);
            }
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(StateData, StateNormal);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new Scp999Goals.SeekTreat(this));
        goalSelector.addGoal(3, new Scp999Goals.Beg(this));
        goalSelector.addGoal(4, new Scp999Goals.Comfort(this));
        goalSelector.addGoal(5, new Scp999Goals.Seek(this));
        goalSelector.addGoal(6, new Scp999Goals.Curious(this));
        goalSelector.addGoal(6, new Scp999Goals.Cuddle(this));
        goalSelector.addGoal(7, new Scp999Goals.Play(this));
        goalSelector.addGoal(8, new Scp999Goals.FollowFriend(this));
        goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(12, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        return Result;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.scp999.refuses");
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public byte mood() {
        return entityData.get(StateData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isFlat() {
        return mood() == StateFlat;
    }

    public boolean isNapping() {
        return mood() == StateSleep;
    }

    public boolean free() {
        return isAlive() && !isActing() && mood() == StateNormal;
    }

    public boolean frightened() {
        return DangerLeft > 0;
    }

    @Nullable
    public Vec3 dangerSource() {
        return DangerLeft > 0 ? DangerFrom : null;
    }

    public Scp999Eyes eyes() {
        return Eyes;
    }

    public int affection(UUID Who) {
        return Affection.getOrDefault(Who, 0);
    }

    public boolean likes(Player Who) {
        return affection(Who.getUUID()) >= LikeThreshold;
    }

    public void adjustAffection(UUID Who, int Amount) {
        int Next = Mth.clamp(affection(Who) + Amount, -MaxAffection, MaxAffection);
        Affection.put(Who, Next);
    }

    @Nullable
    public UUID sought() {
        return Sought;
    }

    public void seek(@Nullable UUID Who) {
        Sought = Who;
    }

    public int treatCooldown() {
        return TreatLeft;
    }

    public boolean craving() {
        return CravingLeft > 0;
    }

    public void crave() {
        CravingLeft = CravingTicks;
    }

    public int meals() {
        return Meals;
    }

    public int curiosity() {
        return CuriousLeft;
    }

    @Nullable
    public UUID curiousAbout() {
        return CuriousAbout;
    }

    public void satisfyCuriosity() {
        CuriousLeft = 0;
        CuriousAbout = null;
    }

    public int playRest() {
        return PlayLeft;
    }

    public float playChance() {
        return Scp999Goals.PlayChance;
    }

    public void restFromPlay(int Ticks) {
        PlayLeft = Math.max(PlayLeft, Ticks);
    }

    @Nullable
    public String actionClip() {
        if (level().isClientSide) {
            return Clock.clip(clockNow());
        }
        return clipOf(action());
    }

    @Nullable
    public static String clipOf(byte Kind) {
        return switch (Kind) {
            case ActionPet -> "pet";
            case ActionEat -> "eat";
            case ActionWobble -> "wobble";
            case ActionGreet -> "greet";
            case ActionExcited -> "excited";
            case ActionHug -> "hug";
            case ActionPlay -> "play";
            case ActionNuzzle -> "nuzzle";
            case ActionHum -> "hum";
            case ActionPeek -> "peek";
            case ActionWake -> "wake";
            case ActionFlatten -> "flatten";
            case ActionDrop -> "shape_drop";
            case ActionPuddle -> "shape_puddle";
            case ActionLumpy -> "shape_lumpy";
            case ActionHeart -> "shape_heart";
            default -> null;
        };
    }

    public static int ticksOf(byte Kind) {
        return switch (Kind) {
            case ActionPet -> PetTicks;
            case ActionEat -> EatTicks;
            case ActionWobble -> WobbleTicks;
            case ActionGreet -> GreetTicks;
            case ActionExcited -> ExcitedTicks;
            case ActionHug -> HugTicks;
            case ActionPlay -> PlayTicks;
            case ActionNuzzle -> NuzzleTicks;
            case ActionHum -> HumTicks;
            case ActionPeek -> PeekTicks;
            case ActionWake -> WakeTicks;
            case ActionFlatten -> FlattenTicks;
            case ActionDrop, ActionPuddle, ActionLumpy, ActionHeart -> ShapeTicks;
            default -> 0;
        };
    }

    public static boolean interruptible(byte Kind) {
        return Kind == ActionNone || Kind == ActionWobble || Kind == ActionHum || Kind == ActionPlay || Kind == ActionNuzzle || Kind == ActionGreet
                || Kind == ActionExcited || Kind == ActionWake || Kind == ActionPet || Kind >= ActionDrop;
    }

    @Override
    public ActionClock actionClock() {
        return Clock;
    }

    private double clockNow() {
        return tickCount + RenderPartial;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    private void syncClock() {
        byte Current = action();
        if (Current == ActionNone) {
            Clock.finish();
            return;
        }
        Clock.begin(Current, clipOf(Current), tickCount, ActionClock.already(Clock.fresh(), tickCount, TickOrigin, entityData.get(ActionStartData)));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (level().isClientSide && (Key.equals(ActionData) || Key.equals(ActionStartData))) {
            syncClock();
            if (Key.equals(ActionStartData) && BodyController != null && isActing()) {
                BodyController.forceAnimationReset();
            }
        }
    }

    public void setAction(byte Kind) {
        setAction(Kind, ticksOf(Kind));
    }

    public void setAction(byte Kind, int Ticks) {
        byte Before = action();
        if (Before == ActionEat && Kind != ActionEat) {
            clearMeal();
        }
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
        updateControlFlags();
    }

    public double actionElapsed() {
        if (!level().isClientSide) {
            return Math.max(0, tickCount - entityData.get(ActionStartData));
        }
        return Clock.elapsed(clockNow());
    }

    private void setMood(byte Next) {
        if (mood() != Next) {
            entityData.set(StateData, Next);
            updateControlFlags();
        }
    }

    public StrideGait stride() {
        return Stride;
    }

    public boolean isStriding() {
        return Stride.stride() != StrideGait.Idle;
    }

    public boolean isRunning() {
        return Stride.stride() == StrideGait.Run;
    }

    @Override
    protected void updateControlFlags() {
        super.updateControlFlags();
        boolean Held = isActing() || mood() != StateNormal;
        goalSelector.setControlFlag(Goal.Flag.MOVE, !Held && !isPassenger());
        goalSelector.setControlFlag(Goal.Flag.JUMP, !Held && !isPassenger());
        goalSelector.setControlFlag(Goal.Flag.LOOK, !(mood() != StateNormal || action() == ActionHug || action() == ActionEat));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (!isAlive()) {
            return;
        }
        if (ActionTicks > 0) {
            ActionTicks--;
            actionTick();
            if (ActionTicks <= 0) {
                finishAction();
            }
        }
        if (mood() != StateNormal || isActing()) {
            Vec3 Motion = getDeltaMovement();
            setDeltaMovement(Motion.x * 0.5, Motion.y, Motion.z * 0.5);
        }
    }

    private void clientTick() {
        Clock.settle();
        Eyes.tick(this);
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        boolean Moving = Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive() && mood() == StateNormal && !isActing() && (onGround() || isInWater()), Gait);
        if (!Moving || isSilent()) {
            return;
        }
        for (double Contact : Contacts) {
            if (Math.floor(Stride.to() - Contact) > Math.floor(Stride.from() - Contact) && level().hasNearbyAlivePlayer(getX(), getY(), getZ(), 12.0)) {
                for (var Layer : Scp999Registry.HopLayers) {
                    level().playLocalSound(getX(), getY(), getZ(), Layer.get(), getSoundSource(), StepVolume * (0.85f + random.nextFloat() * 0.15f),
                            (isRunning() ? 1.1f : 1.0f) * (0.94f + random.nextFloat() * 0.12f), false);
                }
            }
        }
    }

    private void actionTick() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        int Elapsed = (int) actionElapsed();
        if (Facing != null && Facing.isAlive()) {
            getLookControl().setLookAt(Facing, 30.0f, 30.0f);
            face(Facing.position());
        }
        byte Kind = action();
        if (Kind == ActionEat && Elapsed == BiteTick) {
            bite(World);
        }
        if (Kind == ActionPlay && Elapsed == PokeTick) {
            Scp999Goals.Play.poke(World, this, Toy);
        }
        if (Kind == ActionHug && Elapsed > 8 && Elapsed % 14 == 8 && Facing instanceof LivingEntity Hugged) {
            Scp999Effects.giggle(World, this, Hugged);
        }
    }

    private void finishAction() {
        byte Kind = action();
        entityData.set(ActionData, ActionNone);
        Facing = null;
        Toy = null;
        updateControlFlags();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Kind == ActionEat) {
            clearMeal();
            if (mood() == StateNormal) {
                setAction(ActionWobble);
                Scp999Effects.layers(World, this, Scp999Registry.TrillLayers, VoiceVolume, voicePitch());
            }
        } else if (Kind == ActionWake) {
            Player Nearest = World.getNearestPlayer(this, 12.0);
            CuriousAbout = Nearest != null ? Nearest.getUUID() : null;
            CuriousLeft = CuriousTicks;
            Scp999Effects.layers(World, this, Scp999Registry.ChirpLayers, VoiceVolume, voicePitch() * 1.05f);
        } else if (Kind == ActionPet && QueuedShape != ActionNone && mood() == StateNormal) {
            setAction(QueuedShape);
            QueuedShape = ActionNone;
            Scp999Effects.layers(World, this, Scp999Registry.TrillLayers, VoiceVolume, voicePitch());
        }
    }

    public void face(Vec3 Target) {
        double Dx = Target.x - getX();
        double Dz = Target.z - getZ();
        if (Dx * Dx + Dz * Dz < 1e-6) {
            return;
        }
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
    }

    public float voicePitch() {
        return 0.96f + (float) ((getUUID().getLeastSignificantBits() & 7L) * 0.012);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        PetLeft = Math.max(0, PetLeft - 1);
        HugLeft = Math.max(0, HugLeft - 1);
        GreetLeft = Math.max(0, GreetLeft - 1);
        TreatLeft = Math.max(0, TreatLeft - 1);
        HandLeft = Math.max(0, HandLeft - 1);
        CuriousLeft = Math.max(0, CuriousLeft - 1);
        PlayLeft = Math.max(0, PlayLeft - 1);
        if (CravingLeft > 0) {
            CravingLeft--;
            if (CravingLeft == 0) {
                CravingAt = tickCount + CravingMin + random.nextInt(CravingMax - CravingMin + 1);
            }
        } else if (tickCount >= CravingAt && TreatLeft == 0) {
            crave();
        }
        if (DangerLeft > 0) {
            DangerLeft--;
        }
        if (tickCount % ScanInterval == 0) {
            Vec3 Threat = Scp999Danger.threat(this);
            if (Threat != null) {
                frighten(World, Threat, SafeTicks);
            }
        }
        if (DangerLeft > 0) {
            if (tickCount % 30 == 0) {
                Scp999Effects.tremble(World, this);
            }
        } else if (mood() == StateFlat) {
            recover(World);
        }
        if (tickCount % RegenInterval == 0) {
            soothe(World);
        }
        if (tickCount % SelfHealInterval == 0 && getHealth() < getMaxHealth()) {
            heal(SelfHeal);
        }
        if (mood() == StateSleep) {
            napTick(World);
            return;
        }
        if (mood() != StateNormal) {
            return;
        }
        watchCrouches(World);
        if (!free()) {
            return;
        }
        if (tickCount % 10 == 0) {
            noticeCandy(World);
        }
        if (!free()) {
            return;
        }
        if (tickCount % 20 == 5) {
            greet(World);
        }
        if (tickCount % 10 == 0) {
            trackStill(World);
        }
        if (tickCount % NapCheck == 0 && free()) {
            considerNap(World);
        }
        if (free()) {
            idleWhims(World);
        }
    }

    public void frighten(ServerLevel World, Vec3 Source, int Ticks) {
        boolean Fresh = DangerLeft <= 0 && mood() != StateFlat;
        DangerLeft = Math.max(DangerLeft, Ticks);
        DangerFrom = Source;
        if (mood() == StateFlat) {
            return;
        }
        QueuedShape = ActionNone;
        clearMeal();
        setMood(StateFlat);
        setAction(ActionFlatten);
        getNavigation().stop();
        if (Fresh) {
            Scp999Effects.layers(World, this, Scp999Registry.SqueakLayers, VoiceVolume, voicePitch());
            Scp999Effects.fright(World, this);
        }
    }

    private void recover(ServerLevel World) {
        setMood(StateNormal);
        setAction(ActionPeek);
        Scp999Effects.layers(World, this, Scp999Registry.ChirpLayers, VoiceVolume * 0.8f, voicePitch() * 0.95f);
    }

    private void soothe(ServerLevel World) {
        for (LivingEntity Other : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RegenRadius),
                Candidate -> Candidate.isAlive() && (Candidate instanceof Player || ControlBodies.isPersonLike(Candidate)))) {
            if (Other instanceof Player User && User.isSpectator()) {
                continue;
            }
            if (Other.distanceToSqr(this) > RegenRadius * RegenRadius || Other.getHealth() >= Other.getMaxHealth()) {
                continue;
            }
            MobEffectInstance Current = Other.getEffect(MobEffects.REGENERATION);
            if (Current != null && (Current.getAmplifier() > 0 || Current.getDuration() > RegenRefresh || Current.isInfiniteDuration())) {
                continue;
            }
            Other.addEffect(new MobEffectInstance(MobEffects.REGENERATION, RegenTicks, 0, true, true, true), this);
        }
    }

    private void napTick(ServerLevel World) {
        SleepLeft--;
        if (tickCount % 90 == 0) {
            Scp999Effects.layers(World, this, Scp999Registry.SighLayers, VoiceVolume * 0.6f, voicePitch());
            Scp999Effects.snooze(World, this);
        }
        boolean Bright = World.isDay() && !World.isRaining() && World.getBrightness(LightLayer.SKY, blockPosition()) >= SunLight - 2;
        if (SleepLeft <= 0 || !Bright || Scp999Goals.candyHolder(this) != null) {
            wake(World);
        }
    }

    public boolean friendNearby(ServerLevel World) {
        return !World.getEntitiesOfClass(Player.class, getBoundingBox().inflate(HugSeek), Candidate -> Candidate.isAlive() && !Candidate.isSpectator() && likes(Candidate)).isEmpty();
    }

    public void nap(ServerLevel World, int Ticks) {
        SleepLeft = Ticks;
        QueuedShape = ActionNone;
        if (isActing()) {
            setAction(ActionNone, 0);
        }
        setMood(StateSleep);
        getNavigation().stop();
        Scp999Effects.layers(World, this, Scp999Registry.SighLayers, VoiceVolume * 0.7f, voicePitch());
    }

    public void wake(ServerLevel World) {
        if (mood() != StateSleep) {
            return;
        }
        SleepLeft = 0;
        setMood(StateNormal);
        setAction(ActionWake);
    }

    private void considerNap(ServerLevel World) {
        if (!World.isDay() || World.isRaining() || !World.canSeeSky(blockPosition().above()) || World.getBrightness(LightLayer.SKY, blockPosition()) < SunLight) {
            return;
        }
        if (Scp999Goals.hurtPerson(this) != null || Scp999Goals.candyHolder(this) != null || friendNearby(World) || getNavigation().isInProgress()) {
            return;
        }
        if (random.nextFloat() < NapChance) {
            nap(World, NapMin + random.nextInt(NapMax - NapMin + 1));
        }
    }

    private void watchCrouches(ServerLevel World) {
        for (Player User : World.getEntitiesOfClass(Player.class, getBoundingBox().inflate(CrouchRange), Candidate -> !Candidate.isSpectator() && Candidate.isAlive())) {
            Crouching Track = Crouches.computeIfAbsent(User.getUUID(), Key -> new Crouching());
            boolean Down = User.isShiftKeyDown();
            if (Down == Track.Down) {
                continue;
            }
            Track.Down = Down;
            Track.Times[Track.Count % CrouchToggles] = tickCount;
            Track.Count++;
            if (Track.Count >= CrouchToggles && tickCount - Track.Times[Track.Count % CrouchToggles] <= CrouchWindow && interruptible(action()) && !isActing()) {
                Track.Count = 0;
                welcome(World, User, true);
            }
        }
        if (tickCount % 200 == 0) {
            Crouches.keySet().removeIf(Key -> World.getPlayerByUUID(Key) == null);
        }
    }

    private void greet(ServerLevel World) {
        if (GreetLeft > 0) {
            return;
        }
        Player Nearest = World.getNearestPlayer(getX(), getY(), getZ(), GreetRange, Candidate -> !Candidate.isSpectator() && Candidate.isAlive());
        if (Nearest == null) {
            return;
        }
        Integer Last = Greeted.get(Nearest.getUUID());
        if (Last != null && tickCount - Last < GreetMemory) {
            Greeted.put(Nearest.getUUID(), tickCount);
            return;
        }
        welcome(World, Nearest, false);
    }

    public void welcome(ServerLevel World, Player User, boolean Returned) {
        Greeted.put(User.getUUID(), tickCount);
        GreetLeft = GreetCooldown;
        Facing = User;
        face(User.position());
        setAction(ActionGreet);
        adjustAffection(User.getUUID(), Returned ? GreetAffection + 1 : GreetAffection);
        Scp999Effects.layers(World, this, Scp999Registry.TrillLayers, VoiceVolume, voicePitch());
        Scp999Effects.hearts(World, this, Returned ? 3 : 1);
    }

    private void noticeCandy(ServerLevel World) {
        Player Holder = Scp999Goals.candyHolder(this);
        if (Holder == null) {
            return;
        }
        Integer Last = Noticed.get(Holder.getUUID());
        Noticed.put(Holder.getUUID(), tickCount);
        if (Last != null && tickCount - Last < CandyMemory) {
            return;
        }
        Facing = Holder;
        face(Holder.position());
        setAction(ActionExcited);
        Scp999Effects.layers(World, this, Scp999Registry.TrillLayers, VoiceVolume, voicePitch() * 1.08f);
        Scp999Effects.sparkle(World, this);
    }

    private void trackStill(ServerLevel World) {
        for (Player User : World.getEntitiesOfClass(Player.class, getBoundingBox().inflate(HugSeek + 2.0), Candidate -> !Candidate.isSpectator() && Candidate.isAlive())) {
            Stillness Track = Still.computeIfAbsent(User.getUUID(), Key -> new Stillness());
            if (Track.Spot.distanceToSqr(User.position()) > 0.04) {
                Track.Spot = User.position();
                Track.Since = tickCount;
            }
        }
        if (tickCount % 200 == 0) {
            Still.keySet().removeIf(Key -> World.getPlayerByUUID(Key) == null);
        }
    }

    public int stillFor(Player User) {
        Stillness Track = Still.get(User.getUUID());
        return Track == null ? 0 : tickCount - Track.Since;
    }

    public boolean readyToHug() {
        return HugLeft <= 0;
    }

    public void hug(ServerLevel World, LivingEntity Who) {
        HugLeft = HugCooldown;
        Facing = Who;
        face(Who.position());
        setAction(ActionHug);
        if (Who instanceof Player User) {
            adjustAffection(User.getUUID(), HugAffection);
        }
        Scp999Effects.layers(World, this, Scp999Registry.TrillLayers, VoiceVolume, voicePitch() * 1.05f);
        Scp999Effects.hearts(World, Who, 4);
    }

    private void idleWhims(ServerLevel World) {
        if (getNavigation().isInProgress()) {
            return;
        }
        if (HumLeft > 0) {
            HumLeft--;
        }
        if (ShapeLeft > 0) {
            ShapeLeft--;
        }
        long Time = World.getDayTime() % 24000L;
        boolean Night = Time >= 13000L && Time < 23000L;
        if (Night && HumLeft <= 0) {
            hum(World);
            return;
        }
        if (ShapeLeft <= 0) {
            ShapeLeft = ShapeMin + random.nextInt(ShapeMax - ShapeMin + 1);
            morph(World, pickShape(World));
        }
    }

    public void hum(ServerLevel World) {
        HumLeft = HumMin + random.nextInt(HumMax - HumMin + 1);
        setAction(ActionHum);
        Scp999Effects.layers(World, this, Scp999Registry.HumLayers, VoiceVolume * 0.7f, voicePitch());
        Scp999Effects.notes(World, this);
    }

    public byte pickShape(ServerLevel World) {
        Player Friend = World.getNearestPlayer(this, 6.0);
        if (Friend != null && likes(Friend) && random.nextFloat() < HeartChance) {
            return ActionHeart;
        }
        return switch (random.nextInt(4)) {
            case 0 -> ActionDrop;
            case 1 -> ActionPuddle;
            case 2 -> ActionLumpy;
            default -> ActionHeart;
        };
    }

    public void morph(ServerLevel World, byte Shape) {
        setAction(Shape);
        Scp999Effects.layers(World, this, Scp999Registry.SquishLayers, VoiceVolume * 0.7f, 1.0f);
        if (Shape == ActionHeart) {
            Scp999Effects.hearts(World, this, 2);
        }
    }

    public boolean pet(ServerLevel World, Player User) {
        if (PetLeft > 0) {
            return false;
        }
        PetLeft = PetCooldown;
        adjustAffection(User.getUUID(), PetAffection);
        if (mood() == StateSleep) {
            wake(World);
            Scp999Effects.layers(World, this, Scp999Registry.CooLayers, VoiceVolume * 0.7f, voicePitch() * 0.95f);
            return true;
        }
        if (mood() == StateFlat) {
            Scp999Effects.layers(World, this, Scp999Registry.CooLayers, VoiceVolume * 0.5f, voicePitch() * 0.9f);
            Scp999Effects.hearts(World, this, 1);
            return true;
        }
        if (!interruptible(action())) {
            Scp999Effects.layers(World, this, Scp999Registry.CooLayers, VoiceVolume * 0.6f, voicePitch());
            return true;
        }
        Facing = null;
        setAction(ActionPet);
        if (likes(User) && random.nextFloat() < HeartChance) {
            QueuedShape = ActionHeart;
        }
        Scp999Effects.layers(World, this, Scp999Registry.CooLayers, VoiceVolume, voicePitch());
        Scp999Effects.hearts(World, this, 3);
        return true;
    }

    public boolean feed(ServerLevel World, Player User, ItemStack Stack) {
        if (mood() != StateNormal || HandLeft > 0 || !interruptible(action()) && action() != ActionNone) {
            return false;
        }
        Stack.consume(1, User);
        HandLeft = HandCooldown;
        adjustAffection(User.getUUID(), FeedAffection);
        MealHand = true;
        MealFrom = User.getUUID();
        Facing = User;
        face(User.position());
        setAction(ActionEat);
        return true;
    }

    public void eatFromBowl(ServerLevel World, BlockPos Bowl) {
        MealBowl = Bowl.immutable();
        Facing = null;
        face(Vec3.atCenterOf(Bowl));
        setAction(ActionEat);
    }

    public void eatFromGround(ServerLevel World, ItemEntity Candy) {
        MealItem = Candy;
        Facing = Candy;
        face(Candy.position());
        setAction(ActionEat);
    }

    private void bite(ServerLevel World) {
        boolean Ate = false;
        if (MealHand) {
            Ate = true;
        } else if (MealBowl != null) {
            Ate = PinkDogBowlBlock.eatOne(World, MealBowl);
        } else if (MealItem != null && MealItem.isAlive() && MealItem.getItem().is(Scp999Registry.Candy.get()) && MealItem.distanceToSqr(this) < (EatReach + 1.0) * (EatReach + 1.0)) {
            ItemStack Stack = MealItem.getItem().copy();
            Stack.shrink(1);
            if (Stack.isEmpty()) {
                MealItem.discard();
            } else {
                MealItem.setItem(Stack);
            }
            Ate = true;
        }
        if (!Ate) {
            return;
        }
        Meals++;
        TreatLeft = TreatCooldown;
        CravingLeft = 0;
        CravingAt = tickCount + CravingMin + random.nextInt(CravingMax - CravingMin + 1);
        heal(EatHeal);
        Scp999Effects.layers(World, this, Scp999Registry.MunchLayers, VoiceVolume, 1.0f);
        Scp999Effects.crumbs(World, this);
    }

    private void clearMeal() {
        MealBowl = null;
        MealItem = null;
        MealHand = false;
        MealFrom = null;
    }

    public void playWith(ServerLevel World, Entity Target, @Nullable ItemEntity Item) {
        Facing = Target;
        Toy = Item;
        face(Target.position());
        setAction(ActionPlay);
        Scp999Effects.layers(World, this, Scp999Registry.ChirpLayers, VoiceVolume, voicePitch() * 1.1f);
    }

    public void nuzzle(ServerLevel World, LivingEntity Target) {
        Facing = Target;
        face(Target.position());
        setAction(ActionNuzzle);
        Scp999Effects.layers(World, this, Scp999Registry.CooLayers, VoiceVolume, voicePitch());
        Scp999Effects.hearts(World, Target, 2);
    }

    public void arrive(ServerLevel World, Player User) {
        Sought = null;
        adjustAffection(User.getUUID(), ArrivalAffection);
        welcome(World, User, false);
    }

    @Override
    public InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (Held.is(Scp999Registry.Candy.get())) {
            if (level() instanceof ServerLevel World) {
                return feed(World, User, Held) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        if (!Held.isEmpty() || Hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(User, Hand);
        }
        if (level() instanceof ServerLevel World) {
            pet(World, User);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World && isAlive()) {
            Entity Attacker = Source.getEntity();
            if (Attacker instanceof Player User) {
                adjustAffection(User.getUUID(), HurtAffection);
            }
            frighten(World, Attacker != null ? Attacker.position() : position(), HurtFear);
        }
        return Hurt;
    }

    @Override
    public boolean canDrownInFluidType(FluidType Type) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return true;
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
        return AmbientInterval;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return mood() == StateSleep ? Scp999Registry.Sigh.get() : Scp999Registry.Chirp.get();
    }

    @Override
    public void playAmbientSound() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (mood() == StateFlat) {
            Scp999Effects.layers(World, this, Scp999Registry.SqueakLayers, VoiceVolume * 0.5f, voicePitch() * 1.05f);
        } else if (mood() == StateNormal) {
            Scp999Effects.layers(World, this, Scp999Registry.ChirpLayers, VoiceVolume * 0.8f, voicePitch());
        }
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp999Registry.Squeak.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            Scp999Effects.layers(World, this, Scp999Registry.SqueakLayers, VoiceVolume, voicePitch());
        }
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World && !isRemoved() && !dead) {
            Scp999Effects.layers(World, this, Scp999Registry.SquishLayers, 1.0f, 0.7f);
            Scp999Effects.layers(World, this, List.of(Scp999Registry.SqueakWhine), 0.8f, 0.75f);
        }
        super.die(Source);
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        ListTag Friends = new ListTag();
        for (Map.Entry<UUID, Integer> Entry : Affection.entrySet()) {
            CompoundTag Friend = new CompoundTag();
            Friend.putUUID("Who", Entry.getKey());
            Friend.putInt("Fondness", Entry.getValue());
            Friends.add(Friend);
        }
        Saved.put("Friends", Friends);
        Saved.putBoolean("Napping", mood() == StateSleep);
        Saved.putInt("NapLeft", SleepLeft);
        Saved.putInt("TreatLeft", TreatLeft);
        Saved.putInt("Meals", Meals);
        if (Sought != null) {
            Saved.putUUID("Sought", Sought);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Affection.clear();
        for (Tag Raw : Saved.getList("Friends", Tag.TAG_COMPOUND)) {
            CompoundTag Friend = (CompoundTag) Raw;
            if (Friend.hasUUID("Who")) {
                Affection.put(Friend.getUUID("Who"), Friend.getInt("Fondness"));
            }
        }
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        DangerLeft = 0;
        SleepLeft = Saved.getInt("NapLeft");
        entityData.set(StateData, Saved.getBoolean("Napping") && SleepLeft > 0 ? StateSleep : StateNormal);
        TreatLeft = Saved.getInt("TreatLeft");
        Meals = Saved.getInt("Meals");
        Sought = Saved.hasUUID("Sought") ? Saved.getUUID("Sought") : null;
        setPersistenceRequired();
        updateControlFlags();
    }

    public RawAnimation baseAnimation() {
        if (isDeadOrDying() || mood() == StateFlat) {
            return FlatLoop;
        }
        if (mood() == StateSleep) {
            return SleepLoop;
        }
        if (isStriding()) {
            return isRunning() ? BoundLoop : HopLoop;
        }
        return IdleLoop;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        AnimationController<Scp999Blob> Body = new Scp999GaitController(this, "body", Scp999GaitController.LoopBlend, Event -> {
            String Shown = actionClip();
            Event.getController().transitionLength(Shown == null ? Scp999GaitController.LoopBlend : Scp999GaitController.ActionBlend);
            if (Shown != null && !isDeadOrDying()) {
                return Event.setAndContinue(Plays.getOrDefault(Shown, IdleLoop));
            }
            return Event.setAndContinue(baseAnimation());
        });
        BodyController = Body;
        Controllers.add(Body);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
