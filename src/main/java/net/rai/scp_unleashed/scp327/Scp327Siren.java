package net.rai.scp_unleashed.scp327;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp327Siren extends PathfinderMob implements GeoEntity {
    public static final float Width = 1.2f;
    public static final float Height = 0.9f;
    public static final float EyeHeight = 0.7f;
    public static final byte ModeSwim = 0;
    public static final byte ModeSing = 1;
    public static final byte ModeHaul = 2;
    public static final byte ModeBeached = 3;
    public static final byte ModeBeachSing = 4;
    public static final byte ModeSlip = 5;
    public static final byte ModeDrag = 6;
    public static final byte ModeStranded = 7;
    public static final byte ModeDive = 8;
    public static final byte StageBaby = 0;
    public static final byte StageTeen = 1;
    public static final byte StageAdult = 2;
    public static final byte FlourishNone = 0;
    public static final byte FlourishRoll = 1;
    public static final byte FlourishSpiral = 2;
    public static final byte FlourishBackstroke = 3;
    public static final byte FlourishTumble = 4;
    public static final byte FlourishDart = 5;
    public static final String[] FlourishClips = {"", "barrel_roll", "spiral_descent", "backstroke", "pup_tumble", "pup_dart"};
    public static final int[] FlourishTicks = {0, 80, 80, 120, 32, 16};
    public static final double AdultHealth = 40.0;
    public static final double TeenHealth = 20.0;
    public static final double BabyHealth = 10.0;
    public static final float BabyScale = 0.42f;
    public static final float TeenScale = 0.68f;
    public static final long StageTicks = 24000L;
    public static final double SenseRange = 48.0;
    public static final double PursuitRange = 128.0;
    public static final int PursuitTicks = 2400;
    public static final double SongRange = 28.0;
    public static final double SongLeave = 34.0;
    public static final double TeenSongRange = 10.0;
    public static final double TeenSense = 16.0;
    public static final double GrabReach = 2.3;
    public static final double LungeRange = 6.0;
    public static final int EntryReach = 4;
    public static final int DiveReach = 5;
    public static final int RouteInterval = 10;
    public static final int StallLimit = 40;
    public static final double StallProgress = 0.05;
    public static final double EntryArrive = 0.45;
    public static final double BeachReach = 20.0;
    public static final double ArriveRange = 1.6;
    public static final double CloserShare = 0.75;
    public static final double CloserMargin = 2.0;
    public static final int RetargetInterval = 10;
    public static final int ReplanInterval = 40;
    public static final double ReplanShift = 4.0;
    public static final double CruiseSpeed = 0.11;
    public static final double FastSpeed = 0.22;
    public static final double DragSpeed = 0.1;
    public static final double PupSpeed = 0.14;
    public static final double DartSpeed = 0.3;
    public static final double WanderSpeed = 0.07;
    public static final float TurnAccel = 0.9f;
    public static final float RateFollow = 0.15f;
    public static final float RateClamp = 12.0f;
    public static final float PitchRateClamp = 8.0f;
    public static final float TurnLimit = 7.0f;
    public static final float PitchRate = 3.0f;
    public static final float PitchLimit = 45.0f;
    public static final double SpeedAccel = 0.006;
    public static final double SingDepth = 0.2;
    public static final double SurfaceLine = 0.89;
    public static final double ShoulderHeight = 0.9;
    public static final double CeilingGap = 0.45;
    public static final double CruiseDepth = 1.4;
    public static final double DragDepth = 6.0;
    public static final int MaxMoisture = 2400;
    public static final int PupMoisture = 1800;
    public static final float ThirstyShare = 0.3f;
    public static final float BeachShare = 0.5f;
    public static final int DryInterval = 40;
    public static final float DryDamage = 1.0f;
    public static final int HaulTicks = 40;
    public static final int SlipTicks = 24;
    public static final int DiveTicks = 28;
    public static final int GrabCooldown = 200;
    public static final int RefusedCooldown = 30;
    public static final int LightInterrupt = 40;
    public static final int HeavyInterrupt = 200;
    public static final float HeavyHit = 6.0f;
    public static final float ReleaseDamage = 8.0f;
    public static final float FleeShare = 0.3f;
    public static final int KillsPerPup = 3;
    public static final int PupRetry = 200;
    public static final int FlourishMin = 500;
    public static final int FlourishSpread = 700;
    public static final int PupFlourishMin = 120;
    public static final int PupFlourishSpread = 160;
    public static final int StuckLimit = 30;
    public static final int TurnAroundTicks = 60;
    public static final float TurnAroundRate = 4.0f;
    public static final double TurnAroundSpeed = 0.35;
    public static final float TurnAroundDone = 4.0f;
    public static final int DeathTicks = 60;
    public static final double HoldForward = 1.09;
    public static final double HoldUp = 0.375;
    public static final double FollowRange = 32.0;
    public static final double HerdRadius = 3.0;

    private static final EntityDataAccessor<Byte> ModeData = SynchedEntityData.defineId(Scp327Siren.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> StageData = SynchedEntityData.defineId(Scp327Siren.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FlourishData = SynchedEntityData.defineId(Scp327Siren.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> MoistureData = SynchedEntityData.defineId(Scp327Siren.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Moisture = MaxMoisture;
    private int Kills;
    private long StageStarted = Long.MIN_VALUE;
    private long SavedAt = Long.MIN_VALUE;
    private int ModeTicks;
    private int Interrupt;
    private int Cooldown;
    private int FlourishLeft;
    private int NextFlourish = FlourishMin;
    private int Stuck;
    private int PupWait;
    private int HeldTicks;
    private float DragDamage;
    private double Speed;
    private float YawVelocity;
    private float TurnAroundYaw;
    private int TurnAround;
    private boolean Settled;
    @Nullable
    private UUID TargetId;
    @Nullable
    private LivingEntity Chosen;
    @Nullable
    private Scp327Waters.Plan Course;
    private int RouteIndex;
    private Vec3 PlannedFor = Vec3.ZERO;
    private int Replan;
    @Nullable
    private Vec3 Wander;
    @Nullable
    private BlockPos Beach;
    @Nullable
    private BlockPos Edge;
    private Vec3 ScriptFrom = Vec3.ZERO;
    private Vec3 ScriptTo = Vec3.ZERO;
    @Nullable
    private UUID VictimId;
    @Nullable
    private BlockPos Entry;
    @Nullable
    private Vec3 DivePoint;
    private int ShoreTicks;
    private int ShoreStall;
    private double ShoreBest;
    @Nullable
    private Vec3 SpiralCentre;
    private int ForcedSong;
    @Nullable
    private Vec3 Guide;
    private int GuideTicks;
    private int Pursuit;
    public float ClientYawRate;
    public float ClientYawRateO;
    public float ClientPitchRate;
    public float ClientPitchRateO;
    public double StrokePhase;
    public double StrokePhaseO;
    private float LastYaw;
    private float LastPitch;

    public Scp327Siren(EntityType<? extends Scp327Siren> Type, Level World) {
        super(Type, World);
        lookControl = new Scp327LookControl(this);
        setPersistenceRequired();
        if (Type == Scp327Registry.Pup.get()) {
            entityData.set(StageData, StageBaby);
        }
        NextFlourish = Type == Scp327Registry.Pup.get() ? PupFlourishMin + random.nextInt(PupFlourishSpread) : FlourishMin + random.nextInt(FlourishSpread);
    }

    public static AttributeSupplier.Builder createAttributes(double Health) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Health)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, SenseRange)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.STEP_HEIGHT, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ModeData, ModeSwim);
        Builder.define(StageData, StageAdult);
        Builder.define(FlourishData, FlourishNone);
        Builder.define(MoistureData, 1.0f);
    }

    public byte mode() {
        return entityData.get(ModeData);
    }

    public byte stage() {
        return entityData.get(StageData);
    }

    public byte flourish() {
        return entityData.get(FlourishData);
    }

    public float moistureShare() {
        return entityData.get(MoistureData);
    }

    public boolean isPup() {
        return stage() != StageAdult;
    }

    public boolean isBaby() {
        return stage() == StageBaby;
    }

    public boolean isSinging() {
        byte Current = mode();
        return Current == ModeSing || Current == ModeBeachSing;
    }

    public boolean isBeachedMode() {
        byte Current = mode();
        return Current == ModeBeached || Current == ModeBeachSing;
    }

    public int kills() {
        return Kills;
    }

    public int moisture() {
        return Moisture;
    }

    public int maxMoisture() {
        return isPup() ? PupMoisture : MaxMoisture;
    }

    public long stageStarted() {
        return StageStarted;
    }

    public long stageAge() {
        return StageStarted == Long.MIN_VALUE ? 0L : level().getGameTime() - StageStarted;
    }

    @Nullable
    public LivingEntity chosen() {
        return Chosen;
    }

    @Nullable
    public BlockPos beachSpot() {
        return Beach;
    }

    public void setMoisture(int Value) {
        Moisture = Mth.clamp(Value, 0, maxMoisture());
        entityData.set(MoistureData, Moisture / (float) maxMoisture());
    }

    @Override
    public float getAgeScale() {
        return switch (stage()) {
            case StageBaby -> BabyScale;
            case StageTeen -> TeenScale;
            default -> 1.0f;
        };
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (StageData.equals(Key)) {
            refreshDimensions();
        }
    }

    @Override
    public boolean isPushedByFluid() {
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
    public int getMaxHeadYRot() {
        return isBeachedMode() ? 60 : 35;
    }

    @Override
    public int getMaxHeadXRot() {
        return 30;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected float tickHeadTurn(float YRot, float Step) {
        yBodyRot = Mth.wrapDegrees(Mth.approachDegrees(yBodyRot, getYRot(), 20.0f));
        yHeadRot = Mth.wrapDegrees(yHeadRot);
        return Step;
    }

    public void setStage(byte Stage) {
        entityData.set(StageData, Stage);
        double Health = Stage == StageBaby ? BabyHealth : Stage == StageTeen ? TeenHealth : AdultHealth;
        AttributeInstance Instance = getAttribute(Attributes.MAX_HEALTH);
        if (Instance != null) {
            float Share = getHealth() / getMaxHealth();
            Instance.setBaseValue(Health);
            setHealth((float) Math.max(1.0, Health * Share));
        }
        StageStarted = level().getGameTime();
        refreshDimensions();
        setMoisture(Moisture);
    }

    public void lookFor(@Nullable LivingEntity Candidate) {
        if (Candidate != null && isCandidate(Candidate)) {
            Chosen = Candidate;
            TargetId = Candidate.getUUID();
            Replan = 0;
        }
    }

    public void hunt(LivingEntity Listener, int Ticks) {
        lookFor(Listener);
        Pursuit = Chosen == Listener ? Ticks : 0;
    }

    public void forceSong(LivingEntity Listener, int Ticks) {
        lookFor(Listener);
        ForcedSong = Ticks;
        Interrupt = 0;
    }

    public void setMode(byte Next) {
        if (mode() != Next) {
            entityData.set(ModeData, Next);
            ModeTicks = 0;
        }
    }

    public boolean heardBy(Entity Listener) {
        if (Listener.level() != level()) {
            return false;
        }
        return level().clip(new ClipContext(songPoint(), Listener.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    public Vec3 songPoint() {
        return position().add(0.0, isSinging() && mode() == ModeSing ? 1.4 * getScale() : 0.8 * getScale(), 0.0);
    }

    public Vec3 holdPoint() {
        Vec3 Forward = Vec3.directionFromRotation(getXRot(), yBodyRot);
        return position().add(Forward.scale(HoldForward * getScale())).add(0.0, HoldUp * getScale(), 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
        }
    }

    private void clientTick() {
        ClientYawRateO = ClientYawRate;
        ClientPitchRateO = ClientPitchRate;
        float YawDelta = Mth.wrapDegrees(yBodyRot - LastYaw);
        float PitchDelta = Mth.wrapDegrees(getXRot() - LastPitch);
        LastYaw = yBodyRot;
        LastPitch = getXRot();
        ClientYawRate += (Mth.clamp(YawDelta, -RateClamp, RateClamp) - ClientYawRate) * RateFollow;
        ClientPitchRate += (Mth.clamp(PitchDelta, -PitchRateClamp, PitchRateClamp) - ClientPitchRate) * RateFollow;
        StrokePhaseO = StrokePhase;
        double Moved = new Vec3(getX() - xo, getY() - yo, getZ() - zo).length();
        double Reference = isPup() ? PupSpeed : mode() == ModeDrag ? DragSpeed : Moved > (CruiseSpeed + FastSpeed) * 0.5 ? FastSpeed : CruiseSpeed;
        double Rate = mode() == ModeDrag ? 1.0 / 24.0 : Mth.clamp(Moved / Reference, 0.35, 1.8) / (isPup() ? 16.0 : Moved > (CruiseSpeed + FastSpeed) * 0.5 ? 20.0 : 40.0);
        StrokePhase += Rate;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (!Settled) {
            settle(World);
        }
        ModeTicks++;
        if (Interrupt > 0) {
            Interrupt--;
        }
        if (Cooldown > 0) {
            Cooldown--;
        }
        if (ForcedSong > 0) {
            ForcedSong--;
        }
        if (Pursuit > 0) {
            Pursuit--;
        }
        growUp(World);
        hydrate(World);
        if (isDeadOrDying()) {
            return;
        }
        if (Kills >= KillsPerPup && --PupWait <= 0) {
            PupWait = PupRetry;
            birth(World);
        }
        byte Current = mode();
        if (Current != ModeHaul && Current != ModeSlip && !(Current == ModeDrag && Beach != null && !isInWater())) {
            noPhysics = false;
        }
        if (Current == ModeHaul || Current == ModeSlip) {
            scripted(World);
            return;
        }
        if (Current == ModeDrag) {
            dragTick(World);
            return;
        }
        if (!isInWater()) {
            landTick(World);
            return;
        }
        if (Current == ModeStranded) {
            setMode(ModeSwim);
        }
        if (Current == ModeDive && ModeTicks >= DiveTicks) {
            setMode(ModeSwim);
        }
        if (GuideTicks > 0 && Guide != null) {
            GuideTicks--;
            steerToward(Guide, CruiseSpeed);
            return;
        }
        if (tickCount % RetargetInterval == 0 && !isBaby()) {
            retarget(World);
        }
        if (isBaby()) {
            herd(World);
            return;
        }
        if (Chosen != null && grabCheck(World)) {
            return;
        }
        if (Chosen != null) {
            approach(World);
        } else {
            wander(World);
        }
    }

    private void settle(ServerLevel World) {
        Settled = true;
        if (StageStarted == Long.MIN_VALUE) {
            StageStarted = World.getGameTime();
        }
        byte Current = mode();
        if (Current == ModeDrag || Current == ModeHaul || Current == ModeSlip || Current == ModeSing || Current == ModeDive) {
            entityData.set(ModeData, isInWater() ? ModeSwim : ModeStranded);
        }
        if ((Current == ModeBeached || Current == ModeBeachSing) && Beach == null) {
            entityData.set(ModeData, isInWater() ? ModeSwim : ModeBeached);
        }
        setMoisture(Moisture);
    }

    private void growUp(ServerLevel World) {
        if (!isPup() || tickCount % 20 != 0) {
            return;
        }
        if (StageStarted == Long.MIN_VALUE) {
            StageStarted = World.getGameTime();
        }
        if (World.getGameTime() - StageStarted >= StageTicks) {
            advanceStage(World);
        }
    }

    public void advanceStage(ServerLevel World) {
        if (stage() == StageBaby) {
            setStage(StageTeen);
            playLayers(Scp327Registry.PupLayers, 0.8f, 0.85f);
        } else if (stage() == StageTeen) {
            mature(World);
        }
    }

    @Nullable
    public Scp327Siren mature(ServerLevel World) {
        Scp327Siren Grown = convertTo(Scp327Registry.Siren.get(), true);
        if (Grown == null) {
            return null;
        }
        Grown.setStage(StageAdult);
        Grown.setHealth(Grown.getMaxHealth());
        Grown.setMoisture(Grown.maxMoisture());
        Grown.Settled = true;
        return Grown;
    }

    private void hydrate(ServerLevel World) {
        int Limit = maxMoisture();
        if (isInWaterRainOrBubble()) {
            if (Moisture != Limit) {
                setMoisture(Limit);
            }
            return;
        }
        setMoisture(Moisture - 1);
        if (Moisture <= 0 && tickCount % DryInterval == 0) {
            hurt(damageSources().dryOut(), DryDamage);
        }
    }

    public void rehydrate() {
        setMoisture(maxMoisture());
    }

    private void retarget(ServerLevel World) {
        double Range = stage() == StageTeen ? TeenSense : SenseRange;
        if (Chosen != null && !keeps(Chosen, Range)) {
            Chosen = null;
            TargetId = null;
            Course = null;
        }
        if (Chosen == null && TargetId != null) {
            Entity Found = World.getEntity(TargetId);
            if (Found instanceof LivingEntity Living && keeps(Living, Range)) {
                Chosen = Living;
            } else {
                TargetId = null;
            }
        }
        LivingEntity Closest = closest(World, Range);
        if (Closest == null) {
            return;
        }
        if (Chosen == null) {
            lookFor(Closest);
            return;
        }
        if (Closest != Chosen) {
            double Current = distanceTo(Chosen);
            double Other = distanceTo(Closest);
            if (Other < Current * CloserShare - CloserMargin) {
                lookFor(Closest);
            }
        }
    }

    @Nullable
    public LivingEntity closest(ServerLevel World, double Range) {
        LivingEntity Best = null;
        double BestDistance = Range * Range;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Range), this::isCandidate)) {
            double Distance = distanceToSqr(Candidate);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    public boolean isCandidate(LivingEntity Candidate) {
        if (Candidate == this || !Candidate.isAlive() || Candidate.isRemoved() || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (Candidate instanceof Player User) {
            if (User.isFakePlayer() || User.isCreative() || User.isSpectator() || Scp268.isNeglected(User)) {
                return false;
            }
            return !(User instanceof ServerPlayer Server && ControlManager.hasPendingDeath(Server));
        }
        return ControlBodies.isPersonLike(Candidate);
    }

    private boolean keeps(LivingEntity Candidate, double Range) {
        double Limit = Pursuit > 0 ? PursuitRange : Range + 8.0;
        return isCandidate(Candidate) && Candidate.level() == level() && distanceToSqr(Candidate) <= Limit * Limit;
    }

    private double songRange() {
        return stage() == StageTeen ? TeenSongRange : SongRange;
    }

    private void approach(ServerLevel World) {
        LivingEntity Listener = Chosen;
        if (Listener == null) {
            return;
        }
        boolean Teen = stage() == StageTeen;
        double Distance = distanceTo(Listener);
        if (--Replan <= 0 || Course == null || PlannedFor.distanceTo(Listener.position()) > ReplanShift) {
            Replan = ReplanInterval;
            PlannedFor = Listener.position();
            Course = Scp327Waters.plan(World, position(), Listener.position(), !Teen && moistureShare() > BeachShare && Cooldown <= 0, BeachReach, Scp327Waters.PlanBudget).orElse(null);
            RouteIndex = 0;
            Beach = Course == null ? null : Course.Beach();
            Edge = Course == null ? null : Course.Edge();
        }
        boolean CanSing = Interrupt <= 0 && (Distance <= songRange() || ForcedSong > 0);
        if (!Teen && Cooldown <= 0 && Listener.isInWater() && Distance <= LungeRange) {
            if (mode() == ModeSing) {
                setMode(ModeSwim);
            }
            steerToward(Listener.position().add(0.0, Listener.getBbHeight() * 0.5 - 0.45, 0.0), FastSpeed);
            return;
        }
        if (mode() == ModeSing) {
            if (!CanSing || Distance > (Teen ? TeenSongRange + 4.0 : SongLeave) && ForcedSong <= 0 || Course != null && Course.Goal().distanceTo(position()) > 6.0 && ForcedSong <= 0) {
                stopSinging();
                return;
            }
            holdSurface(World, Listener);
            sing(Listener);
            return;
        }
        if (Course == null) {
            wander(World);
            return;
        }
        if (Beach != null && Edge != null) {
            Vec3 EdgePoint = Vec3.atBottomCenterOf(Edge);
            double Flat = horizontal(position(), EdgePoint);
            if (Flat <= ArriveRange && Interrupt <= 0) {
                startHaul(Beach);
                return;
            }
            steerAlong(World, EdgePoint.add(0.0, -SingDepth - 0.3, 0.0), Distance > 24.0 ? FastSpeed : CruiseSpeed, true);
            return;
        }
        Vec3 Goal = Course.Goal();
        double Flat = horizontal(position(), Goal);
        if ((Flat <= ArriveRange || Course.Route().size() <= 1 && Flat <= 3.0) && CanSing) {
            setMode(ModeSing);
            playLayers(Scp327Registry.SurfaceLayers, 0.9f, 1.0f);
            return;
        }
        steerAlong(World, Goal.add(0.0, Distance <= songRange() + 6.0 ? -SingDepth - 0.4 : -CruiseDepth, 0.0), Distance > 24.0 ? FastSpeed : Teen ? PupSpeed : CruiseSpeed, Flat > 6.0);
    }

    private void stopSinging() {
        setMode(ModeDive);
        playLayers(Scp327Registry.DiveLayers, 0.8f, 1.0f);
    }

    private void sing(LivingEntity Listener) {
        boolean Teen = stage() == StageTeen;
        if (heardBy(Listener)) {
            Scp327Lure.sing(Listener, this, Teen ? Scp327Lure.TeenCap : Scp327Lure.AdultCap, Teen ? Scp327Lure.TeenGrow : Scp327Lure.AdultGrow);
        }
        getLookControl().setLookAt(Listener, 20.0f, 20.0f);
    }

    private void holdSurface(ServerLevel World, LivingEntity Listener) {
        int Surface = Scp327Waters.surfaceAt(World, blockPosition().above());
        if (Surface == Scp327Waters.NoSurface) {
            Surface = Scp327Waters.surfaceAt(World, blockPosition());
        }
        double WantY = Surface == Scp327Waters.NoSurface ? getY() : Surface + SurfaceLine - ShoulderHeight * getScale();
        double Dy = Mth.clamp(WantY - getY(), -0.05, 0.05);
        Speed *= 0.8;
        setDeltaMovement(getDeltaMovement().scale(0.5).add(0.0, Dy, 0.0));
        float Want = (float) (Mth.atan2(Listener.getZ() - getZ(), Listener.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        float Turned = Mth.wrapDegrees(Mth.approachDegrees(getYRot(), Want, 2.5f));
        setYRot(Turned);
        yBodyRot = Turned;
        setXRot(Mth.approach(getXRot(), 0.0f, 4.0f));
    }

    private boolean grabCheck(ServerLevel World) {
        LivingEntity Listener = Chosen;
        if (Listener == null || stage() != StageAdult || Cooldown > 0 || Interrupt > HeavyInterrupt / 2 || Scp327Hold.isHeld(Listener) || !Targeting.isTargetable(Listener)) {
            return false;
        }
        Vec3 Centre = Listener.position().add(0.0, Listener.getBbHeight() * 0.5, 0.0);
        boolean Close = holdPoint().distanceTo(Centre) <= GrabReach * 0.75 || position().add(0.0, 0.45, 0.0).distanceTo(Centre) <= GrabReach;
        if (!Close) {
            return false;
        }
        return grab(World, Listener);
    }

    public boolean grab(ServerLevel World, LivingEntity Victim) {
        boolean Beached = isBeachedMode();
        BlockPos Landing = null;
        if (!Scp327Hold.wet(Victim)) {
            Landing = entryFor(World, Victim, Beached ? Edge : null);
            if (Landing == null || !Scp327Hold.routeClear(World, Victim, entryPoint(Landing))) {
                Cooldown = RefusedCooldown;
                return false;
            }
        }
        Scp327Hold.seize(this, Victim);
        VictimId = Victim.getUUID();
        HeldTicks = 0;
        DragDamage = 0.0f;
        Entry = Landing;
        DivePoint = null;
        ShoreTicks = 0;
        ShoreStall = 0;
        ShoreBest = Double.MAX_VALUE;
        Scp327Lure.release(getUUID());
        playLayers(Scp327Registry.GrabLayers, 1.0f, 1.0f);
        if (Beached) {
            startSlip();
        }
        setMode(ModeDrag);
        World.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.6, getZ(), 30, 0.6, 0.3, 0.6, 0.2);
        return true;
    }

    @Nullable
    private BlockPos entryFor(ServerLevel World, LivingEntity Victim, @Nullable BlockPos Preferred) {
        if (Preferred != null && Scp327Waters.isWater(World, Preferred)) {
            int Surface = Scp327Waters.surfaceAt(World, Preferred);
            return Surface == Scp327Waters.NoSurface ? Preferred : new BlockPos(Preferred.getX(), Surface, Preferred.getZ());
        }
        Optional<BlockPos> Found = Scp327Waters.nearestWater(World, Victim.blockPosition(), EntryReach, 2);
        if (Found.isEmpty()) {
            return null;
        }
        int Surface = Scp327Waters.surfaceAt(World, Found.get());
        return Surface == Scp327Waters.NoSurface ? Found.get() : new BlockPos(Found.get().getX(), Surface, Found.get().getZ());
    }

    private static Vec3 entryPoint(BlockPos Water) {
        return new Vec3(Water.getX() + 0.5, Water.getY() + 1.0, Water.getZ() + 0.5);
    }

    private Vec3 diveTarget(ServerLevel World) {
        int Surface = Scp327Waters.surfaceAt(World, blockPosition().above());
        if (Surface == Scp327Waters.NoSurface) {
            return position();
        }
        Vec3 Away = Entry == null ? Vec3.ZERO : position().subtract(Vec3.atCenterOf(Entry)).multiply(1.0, 0.0, 1.0);
        Vec3 Best = null;
        double BestScore = -Double.MAX_VALUE;
        for (int Dx = -DiveReach; Dx <= DiveReach; Dx++) {
            for (int Dz = -DiveReach; Dz <= DiveReach; Dz++) {
                int X = getBlockX() + Dx;
                int Z = getBlockZ() + Dz;
                if (!Scp327Waters.isWater(World, new BlockPos(X, Surface, Z))) {
                    continue;
                }
                int Depth = Scp327Waters.depthAt(World, X, Surface, Z);
                if (Depth < 2 || !open(World, X, Surface - Math.min(Depth - 1, 2), Z)) {
                    continue;
                }
                double Score = Math.min(Depth, (int) DragDepth) * 2.0 + new Vec3(Dx, 0.0, Dz).dot(Away) * 0.3 - Math.sqrt(Dx * Dx + Dz * Dz) * 0.2;
                if (Score > BestScore) {
                    BestScore = Score;
                    double Floor = Surface + 1 - Depth;
                    Best = new Vec3(X + 0.5, Math.max(Floor + 0.3, Surface - DragDepth), Z + 0.5);
                }
            }
        }
        return Best == null ? position() : Best;
    }

    private static boolean open(ServerLevel World, int X, int Y, int Z) {
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                if (!Scp327Waters.isWater(World, new BlockPos(X + Dx, Y, Z + Dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Nullable
    public LivingEntity victim() {
        if (VictimId == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        Entity Found = World.getEntity(VictimId);
        return Found instanceof LivingEntity Living ? Living : null;
    }

    private void dragTick(ServerLevel World) {
        LivingEntity Victim = victim();
        HeldTicks++;
        if (Victim == null || !Victim.isAlive() || Victim.isRemoved() || !Scp327Hold.heldBy(Victim, this) || Victim instanceof Player User && (User.isCreative() || User.isSpectator())
                || Victim instanceof ServerPlayer Server && ControlManager.hasPendingDeath(Server) || !Targeting.isTargetable(Victim)) {
            endDrag(World, false);
            return;
        }
        if (DragDamage >= ReleaseDamage || getHealth() < getMaxHealth() * FleeShare || Scp327Hold.brokeFree(Victim)) {
            endDrag(World, true);
            return;
        }
        if (Scp327Hold.stuck(World, Victim) && !Scp327Hold.unstick(World, Victim, Entry != null ? entryPoint(Entry) : position())) {
            endDrag(World, false);
            return;
        }
        boolean Ashore = !Scp327Hold.wet(Victim);
        if (Beach != null && !isInWater() && ModeTicks < SlipTicks) {
            Vec3 Point = ScriptFrom.lerp(ScriptTo, ModeTicks / (double) SlipTicks);
            moveScripted(Point);
        } else if (!isInWater()) {
            if (ModeTicks > SlipTicks + 20) {
                endDrag(World, false);
                return;
            }
            Speed = 0.0;
        } else if (Ashore && Entry != null) {
            Beach = null;
            Vec3 Wait = Vec3.atBottomCenterOf(Entry).add(0.0, -0.6, 0.0);
            steerToward(Wait, DragSpeed * 0.6);
            float Face = (float) (Mth.atan2(Victim.getZ() - getZ(), Victim.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
            float Turned = Mth.wrapDegrees(Mth.approachDegrees(getYRot(), Face, 6.0f));
            setYRot(Turned);
            yBodyRot = Turned;
        } else {
            Beach = null;
            if (DivePoint == null || position().distanceTo(DivePoint) < 1.0 && HeldTicks % 40 == 0) {
                DivePoint = diveTarget(World);
            }
            steerToward(DivePoint, DragSpeed);
        }
        if (Ashore) {
            if (Entry == null) {
                Entry = entryFor(World, Victim, null);
            }
            if (Entry == null) {
                endDrag(World, false);
                return;
            }
            Vec3 Target = entryPoint(Entry);
            ShoreTicks++;
            if (ShoreTicks % RouteInterval == 0 && !Scp327Hold.routeClear(World, Victim, Target)) {
                endDrag(World, false);
                return;
            }
            double Gap = Math.sqrt(Victim.distanceToSqr(Target.x, Victim.getY(), Target.z));
            if (Gap < ShoreBest - StallProgress) {
                ShoreBest = Gap;
                ShoreStall = 0;
            } else if (++ShoreStall > StallLimit) {
                endDrag(World, false);
                return;
            }
            Scp327Hold.haul(World, Victim, Gap < EntryArrive ? position() : Target);
        } else {
            Scp327Hold.tether(World, Victim, holdPoint());
        }
        if (Scp327Hold.eyesUnder(Victim)) {
            Scp327Hold.drown(Victim, damageSources().source(Scp327Registry.Drowned, this), HeldTicks);
        }
        if (HeldTicks % 12 == 0) {
            playLayers(Scp327Registry.StruggleLayers, 0.7f, 0.9f + random.nextFloat() * 0.2f);
            World.sendParticles(ParticleTypes.BUBBLE, Victim.getX(), Victim.getEyeY(), Victim.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
        }
    }

    public void endDrag(ServerLevel World, boolean Forced) {
        LivingEntity Victim = victim();
        if (Victim != null) {
            Scp327Hold.release(Victim);
            if (Forced && Victim.isAlive()) {
                Vec3 Away = Victim.position().subtract(position()).normalize();
                Victim.setDeltaMovement(Away.x * 0.4, 0.45, Away.z * 0.4);
                Victim.hurtMarked = true;
            }
        }
        VictimId = null;
        Entry = null;
        DivePoint = null;
        HeldTicks = 0;
        DragDamage = 0.0f;
        Cooldown = GrabCooldown;
        Interrupt = Math.max(Interrupt, Forced ? HeavyInterrupt : LightInterrupt * 2);
        setMode(isInWater() ? ModeSwim : ModeStranded);
        if (Forced) {
            playLayers(Scp327Registry.ReleaseLayers, 0.9f, 1.0f);
        }
    }

    private void startHaul(BlockPos Spot) {
        ScriptFrom = position();
        ScriptTo = Vec3.atBottomCenterOf(Spot);
        Beach = Spot;
        setMode(ModeHaul);
        Speed = 0.0;
        playLayers(Scp327Registry.HaulLayers, 0.9f, 1.0f);
    }

    private void startSlip() {
        if (Edge == null) {
            Edge = BlockPos.containing(position().add(Vec3.directionFromRotation(0.0f, yBodyRot).scale(-1.5)));
        }
        ScriptFrom = position();
        ScriptTo = Vec3.atBottomCenterOf(Edge).add(0.0, -0.4, 0.0);
        setMode(ModeSlip);
        playLayers(Scp327Registry.HaulLayers, 0.8f, 0.9f);
    }

    private void scripted(ServerLevel World) {
        boolean Hauling = mode() == ModeHaul;
        int Length = Hauling ? HaulTicks : SlipTicks;
        double Share = Math.min(1.0, ModeTicks / (double) Length);
        double Eased = Share * Share * (3.0 - 2.0 * Share);
        Vec3 Point = ScriptFrom.lerp(ScriptTo, Eased).add(0.0, Math.sin(Share * Math.PI) * 0.35, 0.0);
        moveScripted(Point);
        Vec3 Facing = Hauling ? ScriptTo.subtract(ScriptFrom) : ScriptFrom.subtract(ScriptTo);
        if (Facing.horizontalDistanceSqr() > 1.0e-4) {
            float Yaw = (float) (Mth.atan2(Facing.z, Facing.x) * Mth.RAD_TO_DEG) - 90.0f;
            float Turned = Mth.wrapDegrees(Mth.approachDegrees(getYRot(), Yaw, 9.0f));
            setYRot(Turned);
            yBodyRot = Turned;
        }
        setXRot(0.0f);
        if (ModeTicks % 10 == 0) {
            playLayers(Scp327Registry.HaulLayers, 0.5f, 0.9f + random.nextFloat() * 0.2f);
        }
        if (Share >= 1.0) {
            noPhysics = false;
            if (Hauling) {
                setMode(ModeBeached);
            } else {
                Beach = null;
                setMode(ModeSwim);
            }
        }
    }

    private void moveScripted(Vec3 Point) {
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        setPos(Point.x, Point.y, Point.z);
    }

    private void landTick(ServerLevel World) {
        byte Current = mode();
        Speed = 0.0;
        noPhysics = false;
        if ((Current == ModeBeached || Current == ModeBeachSing) && Beach != null) {
            beachedTick(World);
            return;
        }
        if (Current != ModeStranded) {
            setMode(ModeStranded);
            playLayers(Scp327Registry.FlopLayers, 0.8f, 1.0f);
        }
        if (onGround() && ModeTicks % 20 == 0) {
            Optional<BlockPos> Water = Scp327Waters.nearestWater(World, blockPosition(), 8, 3);
            Vec3 Hop;
            if (Water.isPresent()) {
                Vec3 Toward = Vec3.atCenterOf(Water.get()).subtract(position());
                Vec3 Flat = new Vec3(Toward.x, 0.0, Toward.z);
                Flat = Flat.lengthSqr() > 1.0e-4 ? Flat.normalize() : Vec3.ZERO;
                Hop = new Vec3(Flat.x * 0.28, 0.32, Flat.z * 0.28);
                float Yaw = (float) (Mth.atan2(Flat.z, Flat.x) * Mth.RAD_TO_DEG) - 90.0f;
                setYRot(Yaw);
                yBodyRot = Yaw;
            } else {
                Hop = new Vec3((random.nextDouble() - 0.5) * 0.1, 0.22, (random.nextDouble() - 0.5) * 0.1);
            }
            setDeltaMovement(Hop);
            playLayers(Scp327Registry.FlopLayers, 0.6f, 0.9f + random.nextFloat() * 0.25f);
        }
    }

    private void beachedTick(ServerLevel World) {
        setDeltaMovement(Vec3.ZERO);
        if (moistureShare() < ThirstyShare || Chosen == null || distanceTo(Chosen) > SongLeave) {
            startSlip();
            return;
        }
        if (tickCount % RetargetInterval == 0) {
            retarget(World);
            if (Chosen == null) {
                startSlip();
                return;
            }
        }
        if (grabCheck(World)) {
            return;
        }
        LivingEntity Listener = Chosen;
        boolean CanSing = Interrupt <= 0 && (distanceTo(Listener) <= SongRange || ForcedSong > 0);
        if (CanSing) {
            if (mode() != ModeBeachSing) {
                setMode(ModeBeachSing);
            }
            sing(Listener);
        } else if (mode() == ModeBeachSing) {
            setMode(ModeBeached);
        }
        getLookControl().setLookAt(Listener, 10.0f, 20.0f);
    }

    private void wander(ServerLevel World) {
        if (mode() == ModeSing) {
            stopSinging();
        }
        if (FlourishLeft > 0) {
            flourishTick(World);
            return;
        }
        if (--NextFlourish <= 0 && Chosen == null) {
            startFlourish(World);
            return;
        }
        if (Wander == null || horizontal(position(), Wander) < 1.5 || tickCount % 200 == 0) {
            Wander = pickWander(World, 10);
        }
        if (Wander == null) {
            idleDrift();
            return;
        }
        steerToward(Wander, WanderSpeed);
    }

    private void herd(ServerLevel World) {
        if (FlourishLeft > 0) {
            flourishTick(World);
            return;
        }
        if (--NextFlourish <= 0) {
            startFlourish(World);
            return;
        }
        Scp327Siren Parent = null;
        double Best = FollowRange * FollowRange;
        for (Scp327Siren Other : World.getEntitiesOfClass(Scp327Siren.class, getBoundingBox().inflate(FollowRange), Candidate -> Candidate != this && !Candidate.isBaby())) {
            double Distance = distanceToSqr(Other);
            if (Distance < Best) {
                Best = Distance;
                Parent = Other;
            }
        }
        if (Parent == null) {
            wander(World);
            return;
        }
        double Angle = (tickCount + getId() * 37) * 0.03;
        Vec3 Goal = Parent.position().add(Math.cos(Angle) * HerdRadius, 0.3 + Math.sin(Angle * 1.7) * 0.5, Math.sin(Angle) * HerdRadius);
        if (!Scp327Waters.isWater(World, BlockPos.containing(Goal))) {
            Goal = Parent.position().add(0.0, 0.2, 0.0);
        }
        steerToward(Goal, distanceTo(Parent) > 8.0 ? PupSpeed * 1.4 : PupSpeed);
        if (random.nextInt(260) == 0) {
            playLayers(Scp327Registry.PupLayers, 0.5f, 1.0f + random.nextFloat() * 0.3f);
        }
    }

    private void startFlourish(ServerLevel World) {
        RandomSource Dice = random;
        NextFlourish = isPup() ? PupFlourishMin + Dice.nextInt(PupFlourishSpread) : FlourishMin + Dice.nextInt(FlourishSpread);
        int Surface = Scp327Waters.surfaceAt(World, blockPosition().above());
        int Depth = Surface == Scp327Waters.NoSurface ? 0 : Scp327Waters.depthAt(World, getBlockX(), Surface, getBlockZ());
        byte Kind;
        if (isPup()) {
            Kind = Dice.nextBoolean() ? FlourishTumble : FlourishDart;
        } else if (Depth >= 6 && Dice.nextInt(3) == 0) {
            Kind = FlourishSpiral;
        } else if (Depth >= 2 && Dice.nextBoolean()) {
            Kind = FlourishBackstroke;
        } else {
            Kind = FlourishRoll;
        }
        if (Depth < 2) {
            return;
        }
        entityData.set(FlourishData, Kind);
        FlourishLeft = FlourishTicks[Kind];
        Vec3 Right = Vec3.directionFromRotation(0.0f, yBodyRot + 90.0f);
        SpiralCentre = position().add(Right.scale(3.0));
    }

    private void flourishTick(ServerLevel World) {
        byte Kind = flourish();
        FlourishLeft--;
        Vec3 Forward = Vec3.directionFromRotation(0.0f, yBodyRot);
        switch (Kind) {
            case FlourishSpiral -> {
                if (SpiralCentre == null) {
                    SpiralCentre = position();
                }
                Vec3 Offset = position().subtract(SpiralCentre);
                double Angle = Math.atan2(Offset.z, Offset.x) + 0.09;
                Vec3 Next = SpiralCentre.add(Math.cos(Angle) * 3.0, -0.04, Math.sin(Angle) * 3.0);
                steerToward(new Vec3(Next.x, getY() - 0.6, Next.z), CruiseSpeed);
            }
            case FlourishBackstroke -> {
                int Surface = Scp327Waters.surfaceAt(World, blockPosition().above());
                double WantY = Surface == Scp327Waters.NoSurface ? getY() : Surface - 0.55;
                steerToward(position().add(Forward.scale(3.0)).with(Direction.Axis.Y, WantY), WanderSpeed);
            }
            case FlourishDart -> steerToward(position().add(Forward.scale(4.0)), DartSpeed);
            default -> steerToward(position().add(Forward.scale(4.0)), isPup() ? PupSpeed : CruiseSpeed);
        }
        if (FlourishLeft <= 0) {
            entityData.set(FlourishData, FlourishNone);
            SpiralCentre = null;
        }
    }

    @Nullable
    private Vec3 pickWander(ServerLevel World, int Radius) {
        for (int Attempt = 0; Attempt < 8; Attempt++) {
            BlockPos Pos = blockPosition().offset(random.nextInt(Radius * 2 + 1) - Radius, random.nextInt(5) - 2, random.nextInt(Radius * 2 + 1) - Radius);
            if (Scp327Waters.isWater(World, Pos) && Scp327Waters.isWater(World, Pos.below()) && World.noCollision(this, getDimensions(getPose()).makeBoundingBox(Vec3.atBottomCenterOf(Pos)))) {
                return Vec3.atBottomCenterOf(Pos);
            }
        }
        return null;
    }

    private void idleDrift() {
        Speed = Mth.lerp(0.05, Speed, 0.0);
        setDeltaMovement(getDeltaMovement().scale(0.8));
    }

    private void steerAlong(ServerLevel World, Vec3 Goal, double WantSpeed, boolean Route) {
        Vec3 Next = Goal;
        if (Route && Course != null && !Course.Route().isEmpty()) {
            List<Vec3> Points = Course.Route();
            while (RouteIndex < Points.size() - 1 && horizontal(position(), Points.get(RouteIndex)) < 2.0) {
                RouteIndex++;
            }
            Vec3 Waypoint = Points.get(Math.min(RouteIndex, Points.size() - 1));
            if (RouteIndex < Points.size() - 1) {
                Next = new Vec3(Waypoint.x, Goal.y, Waypoint.z);
            }
        }
        steerToward(clampDepth(World, Next), WantSpeed);
    }

    private Vec3 clampDepth(ServerLevel World, Vec3 Goal) {
        BlockPos Column = BlockPos.containing(Goal.x, getY() + 0.5, Goal.z);
        int Surface = Scp327Waters.surfaceAt(World, Column);
        if (Surface == Scp327Waters.NoSurface) {
            return Goal;
        }
        double Floor = Scp327Waters.floorAt(World, Column.getX(), Surface, Column.getZ());
        double Top = Surface - SingDepth;
        double Y = Mth.clamp(Goal.y, Math.min(Top, Floor + 0.15), Top);
        return new Vec3(Goal.x, Y, Goal.z);
    }

    public void steerToward(Vec3 Goal, double WantSpeed) {
        Vec3 Toward = Goal.subtract(position());
        double Distance = Toward.length();
        double Flat = Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
        boolean Turning = TurnAround > 0;
        if (Turning) {
            TurnAround--;
        }
        double Target = (Distance < 2.0 ? WantSpeed * Math.max(0.15, Distance / 2.0) : WantSpeed) * (Turning ? TurnAroundSpeed : 1.0);
        Speed += Mth.clamp(Target - Speed, -SpeedAccel * 2.0, SpeedAccel);
        if (Distance > 0.05 || Turning) {
            float WantYaw = Turning ? TurnAroundYaw : Flat > 0.05 ? (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f : getYRot();
            float Error = Mth.wrapDegrees(WantYaw - getYRot());
            if (Turning && Math.abs(Error) < TurnAroundDone) {
                TurnAround = 0;
            }
            float Limit = Turning ? TurnAroundRate : TurnLimit;
            float Desired = Mth.clamp(Error * 0.12f, -Limit, Limit);
            YawVelocity += Mth.clamp(Desired - YawVelocity, -TurnAccel, TurnAccel);
            float Yaw = Mth.wrapDegrees(getYRot() + YawVelocity);
            setYRot(Yaw);
            yBodyRot = Yaw;
            yHeadRot = Mth.wrapDegrees(Mth.approachDegrees(yHeadRot, Yaw, 6.0f));
            float WantPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(Toward.y, Math.max(0.3, Flat))), -PitchLimit, PitchLimit);
            setXRot(Mth.approach(getXRot(), WantPitch, PitchRate));
        }
        Vec3 Heading = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 Motion = Heading.scale(Speed);
        if (Motion.y > 0.0) {
            int Surface = Scp327Waters.surfaceAt(level(), blockPosition().above());
            if (Surface != Scp327Waters.NoSurface) {
                double Ceiling = Surface + SurfaceLine - CeilingGap;
                if (getY() + Motion.y > Ceiling) {
                    Motion = new Vec3(Motion.x, Math.max(0.0, Ceiling - getY()), Motion.z);
                    setXRot(Mth.approach(getXRot(), 0.0f, PitchRate * 2.0f));
                }
            }
        }
        double Before = position().distanceTo(Goal);
        setDeltaMovement(Motion);
        if (TurnAround == 0 && Speed > 0.03 && getDeltaMovement().lengthSqr() > 1.0e-4 && horizontalCollision) {
            Stuck++;
        } else {
            Stuck = Math.max(0, Stuck - 1);
        }
        if (Stuck > StuckLimit) {
            Stuck = 0;
            Replan = 0;
            Wander = null;
            TurnAroundYaw = Mth.wrapDegrees(getYRot() + 90.0f + random.nextFloat() * 180.0f);
            TurnAround = TurnAroundTicks;
        }
    }

    @Override
    public void travel(Vec3 Input) {
        if (!isControlledByLocalInstance()) {
            super.travel(Input);
            return;
        }
        if (noPhysics) {
            return;
        }
        if (isInWater() && mode() != ModeStranded) {
            move(MoverType.SELF, getDeltaMovement());
            if (isDeadOrDying()) {
                setDeltaMovement(getDeltaMovement().scale(0.8).add(0.0, -0.01, 0.0));
            }
            return;
        }
        super.travel(Vec3.ZERO);
    }

    private static double horizontal(Vec3 From, Vec3 To) {
        double Dx = From.x - To.x;
        double Dz = From.z - To.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public void guide(Vec3 Point, int Ticks) {
        Guide = Point;
        GuideTicks = Ticks;
    }

    public void restOnShore(BlockPos Spot, BlockPos Water) {
        Vec3 Bottom = Vec3.atBottomCenterOf(Spot);
        Vec3 Inland = Bottom.subtract(Vec3.atBottomCenterOf(Water));
        float Yaw = (float) (Mth.atan2(Inland.z, Inland.x) * Mth.RAD_TO_DEG) - 90.0f;
        moveTo(Bottom.x, Bottom.y, Bottom.z, Yaw, 0.0f);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
        Beach = Spot;
        Edge = Water;
        Settled = true;
        setMode(ModeBeached);
    }

    public void scheduleFlourish(int Ticks) {
        NextFlourish = Math.max(1, Ticks);
    }

    public int flourishLeft() {
        return FlourishLeft;
    }

    public void rewindStage(long Ticks) {
        if (StageStarted == Long.MIN_VALUE) {
            StageStarted = level().getGameTime();
        }
        StageStarted -= Ticks;
    }

    public void setKills(int Value) {
        Kills = Value;
        PupWait = 0;
    }

    public void recordKill(ServerLevel World) {
        Kills++;
        PupWait = 0;
    }

    @Nullable
    public Scp327Siren birth(ServerLevel World) {
        if (Kills < KillsPerPup || !Scp327Shoal.roomFor(World, position())) {
            return null;
        }
        Scp327Siren Pup = Scp327Registry.Pup.get().create(World);
        if (Pup == null) {
            return null;
        }
        Vec3 Side = Vec3.directionFromRotation(0.0f, yBodyRot + 90.0f).scale(1.2);
        Vec3 Spot = position().add(Side).add(0.0, 0.2, 0.0);
        if (!Scp327Waters.isWater(World, BlockPos.containing(Spot))) {
            Spot = position();
        }
        Pup.moveTo(Spot.x, Spot.y, Spot.z, yBodyRot, 0.0f);
        Pup.setStage(StageBaby);
        Pup.setPersistenceRequired();
        if (!World.addFreshEntity(Pup)) {
            return null;
        }
        Kills -= KillsPerPup;
        playLayers(Scp327Registry.PupLayers, 0.9f, 1.15f);
        World.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, Spot.x, Spot.y + 0.3, Spot.z, 20, 0.3, 0.3, 0.3, 0.05);
        return Pup;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        float Before = getHealth();
        boolean Hurt = super.hurt(Source, Amount);
        if (!Hurt || level().isClientSide) {
            return Hurt;
        }
        float Taken = Math.max(Amount, Before - getHealth());
        if (mode() == ModeDrag) {
            DragDamage += Taken;
        }
        boolean Heavy = Taken >= HeavyHit || getHealth() < getMaxHealth() * 0.5f;
        if (isSinging() || mode() == ModeHaul) {
            if (Chosen != null) {
                Scp327Lure.disrupt(Chosen, Heavy ? 1.0f : 0.5f);
            }
            Interrupt = Math.max(Interrupt, Heavy ? HeavyInterrupt : LightInterrupt);
            if (mode() == ModeSing) {
                stopSinging();
            } else if (mode() == ModeBeachSing && Heavy) {
                startSlip();
            }
        }
        return Hurt;
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World && mode() == ModeDrag) {
            endDrag(World, true);
        }
        super.die(Source);
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (level() instanceof ServerLevel World && !level().isClientSide) {
            LivingEntity Victim = victim();
            if (Victim != null) {
                Scp327Hold.release(Victim);
            }
            VictimId = null;
            Scp327Lure.release(getUUID());
        }
        super.remove(Reason);
    }

    public void leaveStasis(ServerLevel World) {
        if (SavedAt != Long.MIN_VALUE && StageStarted != Long.MIN_VALUE) {
            StageStarted += Math.max(0L, World.getGameTime() - SavedAt);
        }
        SavedAt = Long.MIN_VALUE;
        Settled = false;
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (level().isClientSide) {
            return;
        }
        float Shift = isPup() ? 1.0f + (StageAdult - stage()) * 0.2f : 1.0f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY() + 0.5, getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch * Shift);
        }
    }

    @Override
    public void playAmbientSound() {
        if (isSinging() || mode() == ModeDrag) {
            return;
        }
        playLayers(isPup() ? Scp327Registry.PupLayers : random.nextInt(3) == 0 ? Scp327Registry.ChirpLayers : Scp327Registry.AmbientLayers, isPup() ? 0.6f : 0.8f,
                0.9f + random.nextFloat() * 0.2f);
    }

    @Override
    public int getAmbientSoundInterval() {
        return isPup() ? 160 : 240;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        playLayers(List.of(Scp327Registry.HurtVoice), 0.8f, 1.0f);
        return Scp327Registry.Hurt.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        playLayers(List.of(Scp327Registry.DeathVoice), 1.0f, 1.0f);
        return Scp327Registry.Death.get();
    }

    @Override
    protected float getSoundVolume() {
        return isPup() ? 0.6f : 1.0f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("Stage", stage());
        Tag.putByte("Mode", mode());
        Tag.putInt("Moisture", Moisture);
        Tag.putInt("Kills", Kills);
        if (StageStarted != Long.MIN_VALUE) {
            Tag.putLong("StageStarted", StageStarted);
        }
        Tag.putLong("SavedAt", level().getGameTime());
        Tag.putInt("Interrupt", Interrupt);
        Tag.putInt("Cooldown", Cooldown);
        if (Beach != null) {
            Tag.putLong("Beach", Beach.asLong());
        }
        if (Edge != null) {
            Tag.putLong("Edge", Edge.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Stage")) {
            entityData.set(StageData, Tag.getByte("Stage"));
        }
        entityData.set(ModeData, Tag.getByte("Mode"));
        Moisture = Tag.contains("Moisture") ? Tag.getInt("Moisture") : maxMoisture();
        entityData.set(MoistureData, Moisture / (float) maxMoisture());
        Kills = Tag.getInt("Kills");
        StageStarted = Tag.contains("StageStarted") ? Tag.getLong("StageStarted") : Long.MIN_VALUE;
        SavedAt = Tag.contains("SavedAt") ? Tag.getLong("SavedAt") : Long.MIN_VALUE;
        Interrupt = Tag.getInt("Interrupt");
        Cooldown = Tag.getInt("Cooldown");
        Beach = Tag.contains("Beach") ? BlockPos.of(Tag.getLong("Beach")) : null;
        Edge = Tag.contains("Edge") ? BlockPos.of(Tag.getLong("Edge")) : null;
        Settled = false;
        refreshDimensions();
    }

    private static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation Cruise = RawAnimation.begin().thenLoop("cruise");
    private static final RawAnimation Fast = RawAnimation.begin().thenLoop("swim_fast");
    private static final RawAnimation SurfaceSing = RawAnimation.begin().thenPlay("surface").thenLoop("sing_water");
    private static final RawAnimation DiveCruise = RawAnimation.begin().thenPlay("dive").thenLoop("cruise");
    private static final RawAnimation HaulPlay = RawAnimation.begin().thenPlayAndHold("haul");
    private static final RawAnimation SlipCruise = RawAnimation.begin().thenPlay("slip").thenLoop("cruise");
    private static final RawAnimation BeachedLoop = RawAnimation.begin().thenLoop("beached");
    private static final RawAnimation BeachSingLoop = RawAnimation.begin().thenLoop("sing_beached");
    private static final RawAnimation GrabDrag = RawAnimation.begin().thenPlay("grab").thenLoop("drag");
    private static final RawAnimation FlopLoop = RawAnimation.begin().thenLoop("flop");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation PupIdle = RawAnimation.begin().thenLoop("pup_idle");
    private static final RawAnimation PupSwim = RawAnimation.begin().thenLoop("pup_swim");
    private static final RawAnimation[] FlourishPlays = {
            Cruise,
            RawAnimation.begin().thenPlay("barrel_roll").thenLoop("cruise"),
            RawAnimation.begin().thenPlay("spiral_descent").thenLoop("cruise"),
            RawAnimation.begin().thenPlay("backstroke").thenLoop("cruise"),
            RawAnimation.begin().thenPlay("pup_tumble").thenLoop("pup_swim"),
            RawAnimation.begin().thenPlay("pup_dart").thenLoop("pup_swim"),
    };

    public double swimSpeed() {
        return new Vec3(getX() - xo, getY() - yo, getZ() - zo).length();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp327StrokeController(this, "body", Event -> {
            Event.getController().transitionLength(8);
            if (isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            byte Current = mode();
            switch (Current) {
                case ModeDrag -> {
                    return Event.setAndContinue(GrabDrag);
                }
                case ModeHaul -> {
                    return Event.setAndContinue(HaulPlay);
                }
                case ModeSlip -> {
                    return Event.setAndContinue(SlipCruise);
                }
                case ModeBeached -> {
                    return Event.setAndContinue(BeachedLoop);
                }
                case ModeBeachSing -> {
                    return Event.setAndContinue(BeachSingLoop);
                }
                case ModeStranded -> {
                    return Event.setAndContinue(isInWater() ? Cruise : FlopLoop);
                }
                case ModeSing -> {
                    return Event.setAndContinue(SurfaceSing);
                }
                case ModeDive -> {
                    return Event.setAndContinue(DiveCruise);
                }
                default -> {
                }
            }
            byte Kind = flourish();
            if (Kind > FlourishNone && Kind < FlourishPlays.length) {
                return Event.setAndContinue(FlourishPlays[Kind]);
            }
            double Moved = swimSpeed();
            if (isPup()) {
                return Event.setAndContinue(Moved > 0.06 ? PupSwim : PupIdle);
            }
            if (Moved > (CruiseSpeed + FastSpeed) * 0.5) {
                return Event.setAndContinue(Fast);
            }
            return Event.setAndContinue(Moved > 0.04 ? Cruise : Idle);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    public static boolean isStroke(String Clip) {
        return "cruise".equals(Clip) || "swim_fast".equals(Clip) || "pup_swim".equals(Clip);
    }
}
