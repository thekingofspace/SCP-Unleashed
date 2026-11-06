package net.rai.scp_unleashed.scp163;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp163Castaway extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.9f;
    public static final float Height = 1.9f;
    public static final float EyeHeight = 1.45f;
    public static final double Health = 60.0;
    public static final double Armour = 2.0;
    public static final double SuitArmour = 6.0;
    public static final double KnockbackResistance = 0.4;
    public static final double Speed = 0.25;
    public static final double WanderModifier = 0.5;
    public static final double HurryModifier = 0.78;
    public static final double FollowRange = 24.0;
    public static final double BreathRange = 64.0;
    public static final double SeekRange = 160.0;
    public static final int AirReserve = 200;
    public static final int BreathInterval = 20;
    public static final int ChokeInterval = 40;
    public static final float ChokeDamage = 1.0f;
    public static final int RecoverStep = 80;
    public static final int RegenInterval = 80;
    public static final int HomeRadius = 12;
    public static final double HomeComfort = 3.0;
    public static final float OffLimits = -100.0f;
    public static final int GestureTicks = 32;
    public static final int GestureCooldown = 30;
    public static final int HurtAnger = 600;
    public static final int SuitAnger = 300;
    public static final int RecoilTicks = 60;
    public static final int ForcedMoodTicks = 600;
    public static final int GreetCooldown = 1200;
    public static final double GreetRange = 6.0;
    public static final int GiftCooldown = 100;
    public static final int TinkerCooldown = 200;
    public static final int OpinionLimit = 10;
    public static final int DislikeThreshold = -3;
    public static final float TreatHeal = 3.0f;
    public static final int GaspInterval = 30;
    public static final int BreatheInterval = 120;
    public static final int AmbientInterval = 220;
    public static final int FiddleTicks = 100;
    public static final int FiddleCooldownMin = 400;
    public static final int FiddleCooldownSpread = 500;
    public static final float FallShare = 0.5f;
    public static final byte ActionNone = 0;
    public static final byte ActionApprove = 1;
    public static final byte ActionDisapprove = 2;
    public static final byte ActionFiddle = 3;
    public static final byte ActionLookUp = 11;
    public static final byte ActionTap = 12;
    public static final int QuirkMin = 200;
    public static final int QuirkSpread = 300;
    public static final float SettledQuirkShare = 0.7f;
    public static final int SurveyMinGap = 600;
    public static final int SurveySpread = 600;
    public static final int SurveyLoadDelay = 60;
    public static final int SurveyRetry = 200;
    public static final int SurveyCheck = 20;
    public static final double MovedTrigger = 8.0;
    public static final double StandBack = 0.5;
    public static final int SettledRecovery = 3;
    public static final int UneaseTicks = 40;
    public static final ResourceLocation SuitModifier = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp163_suit");
    public static final Scp163Gait.Shape CastawayGait = new Scp163Gait.Shape(
            new Scp163Gait.Profile(12.0, 0.0336, 0.5, 2.0, new double[]{0.986, 0.486}, new float[]{1.0f, 1.0f}),
            new Scp163Gait.Profile(6.0, 0.0801, 0.6, 1.8, new double[]{0.98, 0.498}, new float[]{1.0f, 1.0f}),
            0.06, 0.05, 1, Scp163Registry.Step, Scp163Registry.StepTap, 0.3f, 0.22f, 1.0f, 16.0);
    private static final EntityDataAccessor<Byte> MoodData = SynchedEntityData.defineId(Scp163Castaway.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> SuitedData = SynchedEntityData.defineId(Scp163Castaway.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GaspingData = SynchedEntityData.defineId(Scp163Castaway.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp163Castaway.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp163Castaway.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SettledData = SynchedEntityData.defineId(Scp163Castaway.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation FiddleLoop = RawAnimation.begin().thenLoop("fiddle");
    private static final RawAnimation GaspLoop = RawAnimation.begin().thenLoop("gasp");
    private static final RawAnimation ApprovePlay = RawAnimation.begin().thenPlayAndHold("approve");
    private static final RawAnimation DisapprovePlay = RawAnimation.begin().thenPlayAndHold("disapprove");
    private static final Map<Byte, RawAnimation> ActionPlays = new HashMap<>();

    static {
        ActionPlays.put(ActionApprove, ApprovePlay);
        ActionPlays.put(ActionDisapprove, DisapprovePlay);
        for (Scp163Quirk Quirk : Scp163Quirk.values()) {
            ActionPlays.put(Quirk.action(), RawAnimation.begin().thenPlayAndHold(Quirk.clip()));
        }
    }

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Integer> Opinions = new HashMap<>();
    private final Map<UUID, Long> Greeted = new HashMap<>();
    private final Map<UUID, Long> Gifted = new HashMap<>();
    private int ActionTicks;
    private int Air = AirReserve;
    private int ChokeClock;
    private int AngerTicks;
    private int ForcedTicks;
    private Scp163Mood ForcedMood = Scp163Mood.Calm;
    private long GestureReady;
    private long TinkerReady;
    private int FiddleCooldown = FiddleCooldownMin;
    private int Recoil;
    @Nullable
    private LivingEntity RecoilFrom;
    @Nullable
    private BlockPos Home;
    private int Clock;
    private float RenderPartial;
    private int TickOrigin;
    @Nullable
    private Scp163Gait Gait;
    private float DomeAcross = 1.0f;
    private float DomeAcrossO = 1.0f;
    private float DomeTall = 1.0f;
    private float DomeTallO = 1.0f;
    private long QuirkReady = -1L;
    @Nullable
    private Scp163Quirk LastQuirk;
    private long SurveyDue = -1L;
    private long LastSurveyAt = -1L;
    private boolean SurveyOnLoad = true;
    private boolean Surveying;
    @Nullable
    private BlockPos SurveyOrigin;
    @Nullable
    private LongOpenHashSet SurveyedCells;
    private Scp163Survey.Outcome LastOutcome = Scp163Survey.Outcome.Unsurveyed;
    private int LastCells;

    public Scp163Castaway(EntityType<? extends Scp163Castaway> Type, Level World) {
        super(Type, World);
        xpReward = 0;
        moveControl = new Scp163MoveControl(this);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ARMOR, Armour)
                .add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance).add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(MoodData, (byte) Scp163Mood.Calm.ordinal());
        Builder.define(SuitedData, true);
        Builder.define(GaspingData, false);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(SettledData, false);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        Scp163Navigation Navigation = new Scp163Navigation(this, World);
        Navigation.setCanOpenDoors(true);
        Navigation.setCanPassDoors(true);
        return Navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp163Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp163Goals.StepOff(this, WanderModifier));
        goalSelector.addGoal(2, new Scp163Goals.SeekAir(this, HurryModifier));
        goalSelector.addGoal(3, new Scp163Goals.Recoil(this, HurryModifier));
        goalSelector.addGoal(4, new Scp163Goals.AvoidUnkind(this, WanderModifier, HurryModifier));
        goalSelector.addGoal(5, new Scp163Goals.Survey(this, WanderModifier));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new Scp163Goals.Fiddle(this, WanderModifier));
        goalSelector.addGoal(7, new Scp163Goals.Greet(this));
        goalSelector.addGoal(8, new MoveTowardsRestrictionGoal(this, WanderModifier));
        goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(11, new RandomLookAroundGoal(this));
    }

    public Scp163Mood mood() {
        return Scp163Mood.byId(entityData.get(MoodData));
    }

    public boolean isSuited() {
        return entityData.get(SuitedData);
    }

    public boolean isGasping() {
        return entityData.get(GaspingData);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isGesturing() {
        byte Current = action();
        return Current == ActionApprove || Current == ActionDisapprove;
    }

    public boolean isFiddling() {
        return action() == ActionFiddle;
    }

    public boolean isQuirking() {
        Scp163Quirk Quirk = Scp163Quirk.byAction(action());
        return Quirk != null && Quirk.isIdle();
    }

    public boolean isPlaying(byte Kind) {
        return action() == Kind;
    }

    public boolean isSettled() {
        return entityData.get(SettledData);
    }

    public boolean isSurveying() {
        return Surveying;
    }

    @Nullable
    public BlockPos perch() {
        if (!onGround()) {
            return null;
        }
        BlockPos Below = getOnPos();
        return Scp163Footing.isInstrument(level().getBlockState(Below)) ? Below : null;
    }

    public boolean isPerched() {
        return perch() != null;
    }

    public void sidestep(double X, double Z, double Speed) {
        if (getMoveControl() instanceof Scp163MoveControl Control) {
            Control.sidestep(X, Z, Speed);
        }
    }

    public boolean locksMovement() {
        return isGesturing() || isFiddling() || isQuirking() || isPlaying(ActionLookUp) || isPlaying(ActionTap);
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionApprove -> "approve";
            case ActionDisapprove -> "disapprove";
            default -> {
                Scp163Quirk Quirk = Scp163Quirk.byAction(action());
                yield Quirk == null ? null : Quirk.clip();
            }
        };
    }

    public void endAction(byte Kind) {
        if (action() == Kind) {
            entityData.set(ActionData, ActionNone);
            ActionTicks = 0;
        }
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public int air() {
        return Air;
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
    }

    public void endFiddle() {
        if (isFiddling()) {
            entityData.set(ActionData, ActionNone);
            ActionTicks = 0;
        }
        FiddleCooldown = FiddleCooldownMin + random.nextInt(FiddleCooldownSpread);
    }

    public boolean fiddleReady() {
        return FiddleCooldown <= 0;
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    public boolean gesture(boolean Approve) {
        long Now = level().getGameTime();
        if (Now < GestureReady || isGesturing() || !isAlive()) {
            return false;
        }
        GestureReady = Now + GestureTicks + GestureCooldown;
        setAction(Approve ? ActionApprove : ActionDisapprove, GestureTicks);
        playLayers(Approve ? Scp163Registry.ApproveLayers : Scp163Registry.DisapproveLayers, 0.9f, (Approve ? 1.05f : 0.9f) + random.nextFloat() * 0.1f);
        return true;
    }

    public int opinionOf(UUID Who) {
        return Opinions.getOrDefault(Who, 0);
    }

    public void adjustOpinion(UUID Who, int Change) {
        Opinions.put(Who, Mth.clamp(opinionOf(Who) + Change, -OpinionLimit, OpinionLimit));
    }

    public boolean dislikes(LivingEntity Other) {
        return opinionOf(Other.getUUID()) <= DislikeThreshold;
    }

    public boolean mayGreet(LivingEntity Guest) {
        Long Last = Greeted.get(Guest.getUUID());
        return Last == null || level().getGameTime() - Last >= GreetCooldown;
    }

    public void greet(LivingEntity Guest) {
        Greeted.put(Guest.getUUID(), level().getGameTime());
        boolean Kind = opinionOf(Guest.getUUID()) >= 0 && mood() != Scp163Mood.Anger;
        gesture(Kind);
        if (Kind) {
            playLayers(Scp163Registry.ChitterLayers, 0.7f, 1.1f + random.nextFloat() * 0.1f);
        }
    }

    public void noticeTinkering(Player User) {
        long Now = level().getGameTime();
        if (Now < TinkerReady || isGasping() || mood() == Scp163Mood.Anger || dislikes(User)) {
            return;
        }
        TinkerReady = Now + TinkerCooldown;
        getLookControl().setLookAt(User, 30.0f, 30.0f);
        gesture(true);
    }

    @Nullable
    public LivingEntity recoilFrom() {
        return Recoil > 0 && RecoilFrom != null && RecoilFrom.isAlive() ? RecoilFrom : null;
    }

    public void forceMood(Scp163Mood Mood, int Ticks) {
        ForcedMood = Mood;
        ForcedTicks = Ticks;
        entityData.set(MoodData, (byte) Mood.ordinal());
    }

    public boolean surveyReady() {
        return SurveyDue >= 0L && level().getGameTime() >= SurveyDue;
    }

    public void requestSurvey(boolean Force) {
        long Now = level().getGameTime();
        long Earliest = Force || LastSurveyAt < 0L ? Now : Math.max(Now, LastSurveyAt + SurveyMinGap);
        SurveyDue = SurveyDue < 0L ? Earliest : Math.min(SurveyDue, Earliest);
        if (Force) {
            SurveyDue = Now;
        }
    }

    public Scp163Survey beginSurvey() {
        Surveying = true;
        return new Scp163Survey(blockPosition());
    }

    public void abandonSurvey() {
        Surveying = false;
        endAction(ActionLookUp);
        endAction(ActionTap);
        SurveyDue = level().getGameTime() + SurveyRetry;
    }

    public void finishSurvey(Scp163Survey Scan) {
        Surveying = false;
        long Now = level().getGameTime();
        LastSurveyAt = Now;
        SurveyDue = Now + SurveyMinGap + random.nextInt(SurveySpread);
        SurveyOrigin = Scan.origin();
        Scp163Survey.Outcome Outcome = Scan.result();
        if (Outcome == Scp163Survey.Outcome.Enclosed && (Home == null || isGasping())) {
            Outcome = Scp163Survey.Outcome.Breathless;
        }
        LastOutcome = Outcome;
        LastCells = Scan.cells();
        boolean Was = isSettled();
        boolean Enclosed = Outcome == Scp163Survey.Outcome.Enclosed;
        SurveyedCells = Enclosed ? new LongOpenHashSet(Scan.cellSet()) : null;
        entityData.set(SettledData, Enclosed);
        if (Enclosed && !Was) {
            playLayers(Scp163Registry.SettleLayers, 0.6f, 1.0f + random.nextFloat() * 0.1f);
            if (!isActing()) {
                setAction(Scp163Quirk.Ripple.action(), Scp163Quirk.Ripple.ticks());
            }
        } else if (!Enclosed && Was) {
            playLayers(Scp163Registry.ChitterLayers, 0.6f, 1.3f + random.nextFloat() * 0.1f);
        }
    }

    private void unsettle() {
        if (isSettled()) {
            entityData.set(SettledData, false);
        }
        SurveyedCells = null;
    }

    public Scp163Survey.Outcome lastOutcome() {
        return LastOutcome;
    }

    public int lastCells() {
        return LastCells;
    }

    public long lastSurveyAt() {
        return LastSurveyAt;
    }

    public long surveyDue() {
        return SurveyDue;
    }

    private void watchSurroundings(ServerLevel World) {
        long Now = World.getGameTime();
        if (SurveyOnLoad) {
            SurveyOnLoad = false;
            SurveyDue = Now + SurveyLoadDelay;
        }
        if (isGasping()) {
            unsettle();
            return;
        }
        if (isSettled()) {
            if (SurveyedCells != null && !SurveyedCells.contains(blockPosition().asLong())) {
                unsettle();
                requestSurvey(false);
            }
            return;
        }
        if (SurveyOrigin != null && !isLeashed() && blockPosition().distSqr(SurveyOrigin) > MovedTrigger * MovedTrigger) {
            SurveyOrigin = blockPosition();
            requestSurvey(false);
        }
    }

    private void quirkTick(ServerLevel World) {
        Scp163Quirk Playing = Scp163Quirk.byAction(action());
        if (Playing != null && Playing.onBeat((int) actionElapsed())) {
            playLayers(Playing.sound(), Playing.volume(), Playing.pitch() + random.nextFloat() * 0.08f);
        }
        long Now = World.getGameTime();
        if (QuirkReady < 0L) {
            QuirkReady = Now + QuirkMin + random.nextInt(QuirkSpread);
            return;
        }
        if (Now < QuirkReady || isActing() || isGasping() || Surveying || isLeashed() || !onGround() || isPerched() || !getNavigation().isDone()
                || getDeltaMovement().horizontalDistanceSqr() > 1.0e-4) {
            return;
        }
        Scp163Quirk Chosen = Scp163Quirk.pick(random, mood(), LastQuirk);
        int Wait = QuirkMin + random.nextInt(QuirkSpread);
        QuirkReady = Now + (isSettled() ? (long) (Wait * SettledQuirkShare) : Wait);
        if (Chosen != null) {
            LastQuirk = Chosen;
            setAction(Chosen.action(), Chosen.ticks());
        }
    }

    private void setSuited(boolean Suited) {
        entityData.set(SuitedData, Suited);
        applySuitArmour();
    }

    private void applySuitArmour() {
        AttributeInstance Armour0 = getAttribute(Attributes.ARMOR);
        if (Armour0 == null) {
            return;
        }
        if (isSuited()) {
            Armour0.addOrUpdateTransientModifier(new AttributeModifier(SuitModifier, SuitArmour, AttributeModifier.Operation.ADD_VALUE));
        } else {
            Armour0.removeModifier(SuitModifier);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (!isAlive() || !(level() instanceof ServerLevel World)) {
            return;
        }
        Clock++;
        if (ActionTicks > 0 && --ActionTicks <= 0) {
            entityData.set(ActionData, ActionNone);
        }
        if (isGesturing()) {
            getNavigation().stop();
        }
        AngerTicks = Math.max(0, AngerTicks - (isSettled() ? SettledRecovery : 1));
        ForcedTicks = Math.max(0, ForcedTicks - 1);
        FiddleCooldown = Math.max(0, FiddleCooldown - 1);
        Recoil = Math.max(0, Recoil - 1);
        if (Clock % BreathInterval == 0) {
            breathe(World);
            settleHome();
        }
        if (Clock % SurveyCheck == 0) {
            watchSurroundings(World);
        }
        quirkTick(World);
        boolean Uneasy = isGasping() || !isSuited() && !isSettled();
        Scp163Mood Next = ForcedTicks > 0 ? ForcedMood : AngerTicks > 0 ? Scp163Mood.Anger : Uneasy ? Scp163Mood.Distress : Scp163Mood.Calm;
        if (mood() != Next) {
            entityData.set(MoodData, (byte) Next.ordinal());
        }
        if (isGasping()) {
            if (Clock % GaspInterval == 0) {
                playLayers(Scp163Registry.GaspLayers, 0.75f, 0.95f + random.nextFloat() * 0.15f);
            }
        } else {
            if (Clock % BreatheInterval == 0) {
                playSound(Scp163Registry.Breathe.get(), 0.4f, 0.95f + random.nextFloat() * 0.1f);
            }
            if (Clock % RegenInterval == 0 && getHealth() < getMaxHealth()) {
                heal(1.0f);
            }
        }
    }

    private void breathe(ServerLevel World) {
        BlockPos Source = Scp163Breathing.get(World).nearest(World, getX(), getY() + getBbHeight() * 0.5, getZ(), BreathRange);
        if (Source != null) {
            Home = Source;
            if (World.getBlockEntity(Source) instanceof Scp163ApparatusBlockEntity Machine) {
                Machine.feed(World.getGameTime());
            }
            Air = Math.min(AirReserve, Air + RecoverStep);
            ChokeClock = 0;
            if (isGasping()) {
                entityData.set(GaspingData, false);
                recovered(World);
            }
            return;
        }
        if (!isGasping()) {
            entityData.set(GaspingData, true);
            unsettle();
            if (isFiddling()) {
                endFiddle();
            }
            if (isQuirking()) {
                endAction(action());
            }
        }
        Air = Math.max(0, Air - BreathInterval);
        if (Air > 0) {
            return;
        }
        ChokeClock += BreathInterval;
        if (ChokeClock >= ChokeInterval) {
            ChokeClock = 0;
            hurt(damageSources().source(Scp163Registry.Asphyxiation), ChokeDamage);
        }
    }

    private void recovered(ServerLevel World) {
        Player Nearest = World.getNearestPlayer(this, GreetRange * 2.0);
        if (Nearest != null) {
            getLookControl().setLookAt(Nearest, 30.0f, 30.0f);
        }
        gesture(true);
    }

    @Nullable
    public BlockPos nearestAir(ServerLevel World) {
        return Scp163Breathing.get(World).nearest(World, getX(), getY() + getBbHeight() * 0.5, getZ(), SeekRange);
    }

    private void settleHome() {
        if (Home != null && !isGasping() && !isLeashed()) {
            restrictTo(Home, HomeRadius);
        } else if (!isLeashed()) {
            clearRestriction();
        }
    }

    private void clientTick() {
        Scp163Gait Current = gait();
        if (Current != null) {
            Current.tick(this);
        }
        Scp163Mood Mood = mood();
        DomeAcrossO = DomeAcross;
        DomeTallO = DomeTall;
        DomeAcross += (Mood.across() - DomeAcross) * 0.2f;
        DomeTall += (Mood.tall() - DomeTall) * 0.2f;
        if (isGasping() && random.nextInt(6) == 0) {
            level().addParticle(ParticleTypes.BUBBLE_POP, getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 0.5, getZ() + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.04, 0.0);
        }
    }

    public float domeAcross(float Partial) {
        return Mth.lerp(Partial, DomeAcrossO, DomeAcross);
    }

    public float domeTall(float Partial) {
        return Mth.lerp(Partial, DomeTallO, DomeTall);
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        boolean Client = level().isClientSide;
        if (Stack.is(Scp163Registry.Suit.get())) {
            if (isSuited()) {
                return InteractionResult.PASS;
            }
            if (!Client) {
                dress(User, Stack);
            }
            return InteractionResult.sidedSuccess(Client);
        }
        if (Stack.isEmpty()) {
            if (Hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!Client) {
                if (isSuited()) {
                    undress(User);
                } else {
                    getLookControl().setLookAt(User, 30.0f, 30.0f);
                    playLayers(Scp163Registry.ChitterLayers, 0.8f, 1.2f + random.nextFloat() * 0.1f);
                }
            }
            return InteractionResult.sidedSuccess(Client);
        }
        if (Stack.is(Scp163Registry.Treats)) {
            if (!Client) {
                accept(User, Stack);
            }
            return InteractionResult.sidedSuccess(Client);
        }
        if (User.isShiftKeyDown() && !(Stack.getItem() instanceof NameTagItem) && !(Stack.getItem() instanceof LeadItem)) {
            if (!Client) {
                getLookControl().setLookAt(User, 30.0f, 30.0f);
                rewardGift(User, 0);
                gesture(true);
            }
            return InteractionResult.sidedSuccess(Client);
        }
        return super.mobInteract(User, Hand);
    }

    private void dress(Player User, ItemStack Stack) {
        setSuited(true);
        if (!User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        playLayers(Scp163Registry.SuitOnLayers, 0.9f, 1.0f);
        adjustOpinion(User.getUUID(), 3);
        AngerTicks = 0;
        GestureReady = 0L;
        gesture(true);
    }

    private void undress(Player User) {
        setSuited(false);
        ItemStack Suit = new ItemStack(Scp163Registry.Suit.get());
        if (!User.getInventory().add(Suit)) {
            User.drop(Suit, false);
        }
        playLayers(Scp163Registry.SuitOffLayers, 0.9f, 0.95f);
        if (isSettled()) {
            forceMood(Scp163Mood.Distress, UneaseTicks);
            getLookControl().setLookAt(User, 30.0f, 30.0f);
            playLayers(Scp163Registry.ChitterLayers, 0.6f, 1.25f + random.nextFloat() * 0.1f);
            return;
        }
        adjustOpinion(User.getUUID(), -2);
        AngerTicks = Math.max(AngerTicks, SuitAnger);
        GestureReady = 0L;
        gesture(false);
    }

    private void accept(Player User, ItemStack Stack) {
        if (!User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        heal(TreatHeal);
        playSound(Scp163Registry.Eat.get(), 0.8f, 1.0f + random.nextFloat() * 0.15f);
        rewardGift(User, 1);
        gesture(true);
    }

    private void rewardGift(Player User, int Amount) {
        long Now = level().getGameTime();
        Long Last = Gifted.get(User.getUUID());
        if (Amount > 0 && (Last == null || Now - Last >= GiftCooldown)) {
            adjustOpinion(User.getUUID(), Amount);
        }
        Gifted.put(User.getUUID(), Now);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide && Source.getEntity() instanceof LivingEntity Attacker && Attacker != this) {
            if (Attacker instanceof Player User) {
                adjustOpinion(User.getUUID(), -3);
            } else {
                adjustOpinion(Attacker.getUUID(), -3);
            }
            AngerTicks = HurtAnger;
            RecoilFrom = Attacker;
            Recoil = RecoilTicks;
            if (isFiddling()) {
                endFiddle();
            }
            gesture(false);
        }
        return Hurt;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return super.causeFallDamage(Distance, Multiplier * FallShare, Source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel World, DamageSource Source, boolean Recent) {
        super.dropCustomDeathLoot(World, Source, Recent);
        if (isSuited()) {
            spawnAtLocation(new ItemStack(Scp163Registry.Suit.get()));
        }
    }

    @Override
    public float getWalkTargetValue(BlockPos Pos, LevelReader World) {
        if (Scp163Footing.isInstrumentSpot(World, Pos)) {
            return OffLimits;
        }
        if (Home == null) {
            return 0.0f;
        }
        return (float) (HomeRadius - Math.abs(Math.sqrt(Pos.distSqr(Home)) - HomeComfort)) * 0.5f;
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (isGasping() || isGesturing()) {
            return;
        }
        float Pitch = switch (mood()) {
            case Distress -> 1.25f;
            case Anger -> 0.85f;
            default -> 1.0f;
        };
        playLayers(Scp163Registry.ChitterLayers, 0.7f, Pitch + random.nextFloat() * 0.1f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp163Registry.Chitter.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp163Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp163Registry.Death.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.0);
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
    public Scp163Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp163Gait(CastawayGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp163Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        Scp163Gait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp163Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("Suited", isSuited());
        Tag.putInt("Air", Air);
        Tag.putInt("Anger", AngerTicks);
        Tag.putInt("FiddleCooldown", FiddleCooldown);
        Tag.putBoolean("Settled", isSettled());
        Tag.putString("Survey", LastOutcome.name());
        Tag.putInt("SurveyCells", LastCells);
        if (SurveyOrigin != null) {
            Tag.putLong("SurveyOrigin", SurveyOrigin.asLong());
        }
        if (Home != null) {
            Tag.putLong("Home", Home.asLong());
        }
        ListTag Saved = new ListTag();
        for (Map.Entry<UUID, Integer> Entry : Opinions.entrySet()) {
            CompoundTag Record = new CompoundTag();
            Record.putUUID("Who", Entry.getKey());
            Record.putInt("Opinion", Entry.getValue());
            Saved.add(Record);
        }
        Tag.put("Opinions", Saved);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Suited")) {
            entityData.set(SuitedData, Tag.getBoolean("Suited"));
        }
        applySuitArmour();
        Air = Tag.contains("Air") ? Tag.getInt("Air") : AirReserve;
        AngerTicks = Tag.getInt("Anger");
        FiddleCooldown = Tag.contains("FiddleCooldown") ? Tag.getInt("FiddleCooldown") : FiddleCooldownMin;
        Home = Tag.contains("Home") ? BlockPos.of(Tag.getLong("Home")) : null;
        entityData.set(SettledData, Tag.getBoolean("Settled"));
        LastOutcome = Scp163Survey.Outcome.byName(Tag.getString("Survey"));
        LastCells = Tag.getInt("SurveyCells");
        SurveyOrigin = Tag.contains("SurveyOrigin") ? BlockPos.of(Tag.getLong("SurveyOrigin")) : null;
        SurveyedCells = null;
        SurveyOnLoad = true;
        Surveying = false;
        Opinions.clear();
        ListTag Saved = Tag.getList("Opinions", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Record = Saved.getCompound(Index);
            if (Record.hasUUID("Who")) {
                Opinions.put(Record.getUUID("Who"), Record.getInt("Opinion"));
            }
        }
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp163GaitController(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp163GaitController.blendFor(Current));
            if (isGesturing()) {
                return Event.setAndContinue(ActionPlays.get(Current));
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? RunLoop : WalkLoop);
            }
            RawAnimation Play = ActionPlays.get(Current);
            if (Play != null) {
                return Event.setAndContinue(Play);
            }
            if (Current == ActionFiddle) {
                return Event.setAndContinue(FiddleLoop);
            }
            if (isGasping()) {
                return Event.setAndContinue(GaspLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
