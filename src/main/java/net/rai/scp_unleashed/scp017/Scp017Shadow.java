package net.rai.scp_unleashed.scp017;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp017Shadow extends Monster implements GeoEntity {
    public static final float Width = 0.6f;
    public static final float Height = 1.95f;
    public static final float EyeHeight = 1.8f;
    public static final float CrouchHeight = 1.35f;
    public static final float CrouchEyeHeight = 1.0f;
    public static final float MaxHealth = 40.0f;
    public static final byte ModeDormant = 0;
    public static final byte ModeHunting = 1;
    public static final byte ModeFleeing = 2;
    public static final byte ActionNone = 0;
    public static final byte ActionWake = 1;
    public static final byte ActionRecoil = 2;
    public static final byte ActionReach = 3;
    public static final byte ActionEmerge = 4;
    public static final byte ActionSettle = 5;
    public static final int WakeTicks = 40;
    public static final int WakeAnimationTicks = 16;
    public static final int RecoilTicks = 14;
    public static final int BrightTicksToRecoil = 3;
    public static final int SuddenWindow = 10;
    public static final int SuddenStep = 4;
    public static final int SuddenMemory = 40;
    public static final int NearCheckInterval = 5;
    public static final float LightBurn = 8.0f;
    public static final int BurnCooldown = 30;
    public static final int RecoverTicks = 20;
    public static final int FleeLimit = 200;
    public static final int ReachTicks = 12;
    public static final double ReachStart = 1.8;
    public static final double GraspRange = 2.1;
    public static final int ReachCooldown = 30;
    public static final int EmergeTicks = 32;
    public static final int SettleTicks = 24;
    public static final double HuntRange = 32.0;
    public static final double NoticeRange = 18.0;
    public static final double LitPenalty = 20.0;
    public static final double ShunRange = 3.0;
    public static final int ShunTicks = 100;
    public static final int RegenInterval = 60;
    public static final float FleeLitMalus = 12.0f;
    public static final int RetargetInterval = 10;
    public static final int CapInterval = 100;
    public static final int SeatCheckInterval = 20;
    public static final double SeatDrift = 0.5;
    public static final int PassCooldown = 10;
    public static final float HiddenAlpha = 0.4f;
    public static final float IdleWisp = 0.2f;
    public static final float VeilStep = 0.08f;
    public static final double CreepSpeed = 0.42;
    public static final double ProwlSpeed = 0.72;
    public static final double StalkSpeed = 0.85;
    public static final double ChaseSpeed = 1.09;
    public static final double FleeSpeed = 1.15;
    public static final Scp017Gait.Profile WalkGait = new Scp017Gait.Profile("walk", 28.0, 0.033482, 0.5, 2.5, 4, new double[]{0.98307, 0.48307}, new float[]{1.0f, 1.0f},
            Scp017Registry.Step, 0.14f, 0.9f);
    public static final Scp017Gait.Profile HuntGait = new Scp017Gait.Profile("hunt", 14.0, 0.15625, 0.5, 2.0, 2, new double[]{0.98135, 0.48135}, new float[]{1.0f, 1.0f},
            Scp017Registry.Step, 0.05f, 1.05f);
    private static final EntityDataAccessor<Byte> ModeData = SynchedEntityData.defineId(Scp017Shadow.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp017Shadow.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> SettledData = SynchedEntityData.defineId(Scp017Shadow.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SeatYawData = SynchedEntityData.defineId(Scp017Shadow.class, EntityDataSerializers.FLOAT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ProwlLoop = RawAnimation.begin().thenLoop("prowl");
    private static final RawAnimation HuntLoop = RawAnimation.begin().thenLoop("hunt");
    private static final RawAnimation CrouchPlay = RawAnimation.begin().thenPlayAndHold("crouch");
    private static final RawAnimation CrouchedLoop = RawAnimation.begin().thenLoop("crouched");
    private static final RawAnimation WakePlay = RawAnimation.begin().thenPlayAndHold("wake");
    private static final RawAnimation RecoilPlay = RawAnimation.begin().thenPlayAndHold("recoil");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation UnravelPlay = RawAnimation.begin().thenPlayAndHold("unravel");
    private static final RawAnimation ReachOnce = RawAnimation.begin().thenPlay("reach");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final int[] LightHistory = new int[SuddenWindow];
    private int HistoryIndex;
    private int LightNow = 15;
    private int DarkTicks;
    private int BrightTicks;
    private int FleeTicks;
    private int ActionTicks;
    private int BurnTimer;
    private int ReachTimer;
    private int PassTimer;
    private int RegenTimer;
    private int SearchCooldown;
    private int SnuffTimer;
    private int SuddenUntil;
    private int NearCheckTick = -100;
    private boolean NearCache;
    private boolean Bound;
    private boolean Duplicate;
    @Nullable
    private UUID ReachTarget;
    @Nullable
    private UUID Shunned;
    private long ShunUntil;
    @Nullable
    private Scp017Corners.Corner Seat;
    @Nullable
    private Vec3 SeatPoint;
    @Nullable
    private Scp017Gait WalkStride;
    @Nullable
    private Scp017Gait HuntStride;
    @Nullable
    private AnimationController<Scp017Shadow> BodyController;
    private float Veil;
    private float VeilO;

    public Scp017Shadow(EntityType<? extends Scp017Shadow> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 20;
        for (int Index = 0; Index < SuddenWindow; Index++) {
            LightHistory[Index] = 15;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, HuntRange).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ModeData, ModeDormant);
        Builder.define(ActionData, ActionNone);
        Builder.define(SettledData, false);
        Builder.define(SeatYawData, 0.0f);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp017Navigation(this, World);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp017Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp017Goals.Flee(this));
        goalSelector.addGoal(3, new Scp017Goals.Hunt(this));
        goalSelector.addGoal(4, new Scp017Goals.SeekCorner(this));
        goalSelector.addGoal(5, new Scp017Goals.Prowl(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public byte mode() {
        return entityData.get(ModeData);
    }

    public boolean isAwake() {
        return mode() != ModeDormant;
    }

    public boolean isHunting() {
        return mode() == ModeHunting;
    }

    public boolean isFleeing() {
        return mode() == ModeFleeing;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isSettled() {
        return entityData.get(SettledData);
    }

    public float seatYaw() {
        return entityData.get(SeatYawData);
    }

    public boolean locksMovement() {
        byte Current = action();
        return Current != ActionNone && Current != ActionReach || isSettled();
    }

    public boolean shunsLight() {
        return isAwake();
    }

    public int lightNow() {
        return LightNow;
    }

    public boolean isBound() {
        return Bound;
    }

    public int searchCooldown() {
        return SearchCooldown;
    }

    public void setSearchCooldown(int Ticks) {
        SearchCooldown = Ticks;
    }

    public boolean readyToReach() {
        return ReachTimer <= 0 && action() == ActionNone;
    }

    public boolean shuns(LivingEntity Candidate) {
        return Candidate.getUUID().equals(Shunned) && level().getGameTime() < ShunUntil;
    }

    private void setMode(byte Next) {
        entityData.set(ModeData, Next);
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone && Kind != ActionReach) {
            getNavigation().stop();
        }
    }

    @Override
    public float getPathfindingMalus(PathType Type) {
        if (Type == Scp017Navigation.LitPath && isAwake()) {
            return isFleeing() ? FleeLitMalus : -1.0f;
        }
        return super.getPathfindingMalus(Type);
    }

    @Override
    public float getWalkTargetValue(BlockPos Pos, LevelReader World) {
        return Scp017Light.Threshold - Scp017Light.at(level(), Pos);
    }

    @Override
    public void tick() {
        super.tick();
        if (isSettled()) {
            holdSeatYaw();
        }
        if (level().isClientSide) {
            clientTick();
        }
    }

    private void holdSeatYaw() {
        float Yaw = seatYaw();
        setYRot(Yaw);
        yRotO = Yaw;
        yBodyRot = Yaw;
        yBodyRotO = Yaw;
        yHeadRot = Yaw;
        yHeadRotO = Yaw;
        setXRot(0.0f);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (Duplicate) {
            dissolve(World);
            return;
        }
        if (Bound && (tickCount == 1 || tickCount % CapInterval == 0)) {
            Scp017World Record = Scp017World.get(World.getServer());
            if (Record.blocks(getUUID()) && !ArchiveRules.repeatable(World.getServer())) {
                Duplicate = true;
                return;
            }
            Record.claim(getUUID());
        }
        sampleLight();
        if (ActionTicks > 0) {
            byte Current = action();
            ActionTicks--;
            if (ActionTicks <= 0) {
                entityData.set(ActionData, ActionNone);
                actionFinished(World, Current);
            }
        }
        if (BurnTimer > 0) {
            BurnTimer--;
        }
        if (ReachTimer > 0) {
            ReachTimer--;
        }
        if (PassTimer > 0) {
            PassTimer--;
        }
        if (SearchCooldown > 0) {
            SearchCooldown--;
        }
        if (action() != ActionEmerge && ++SnuffTimer >= Scp017Snuffer.Interval) {
            SnuffTimer = 0;
            if (Scp017Snuffer.snuffNearest(World, this)) {
                NearCheckTick = -100;
            }
        }
        switch (mode()) {
            case ModeHunting -> huntingTick(World);
            case ModeFleeing -> fleeingTick(World);
            default -> dormantTick(World);
        }
        if (isAwake() && LightNow <= Scp017Light.Threshold && ++RegenTimer >= RegenInterval) {
            RegenTimer = 0;
            if (getHealth() < getMaxHealth()) {
                heal(1.0f);
            }
        }
        if (isSettled()) {
            holdSeatYaw();
        }
    }

    private void sampleLight() {
        LightNow = Scp017Light.of(this);
        boolean Dark = LightNow <= Scp017Light.Threshold;
        DarkTicks = Dark ? DarkTicks + 1 : 0;
        BrightTicks = Dark ? 0 : BrightTicks + 1;
    }

    private int lightEarlier() {
        return LightHistory[HistoryIndex];
    }

    private void recordLight() {
        LightHistory[HistoryIndex] = LightNow;
        HistoryIndex = (HistoryIndex + 1) % SuddenWindow;
    }

    private boolean suddenLight() {
        return LightNow > Scp017Light.Threshold && LightNow - lightEarlier() >= SuddenStep;
    }

    private void dormantTick(ServerLevel World) {
        noteSudden();
        if (DarkTicks >= WakeTicks && action() != ActionEmerge && action() != ActionSettle) {
            wake(World);
            return;
        }
        if (isSettled() && Seat == null && SearchCooldown <= 0) {
            SearchCooldown = Scp017Goals.SearchRetry;
            if (Scp017Corners.find(this).isPresent()) {
                unsettle();
                SearchCooldown = 0;
                return;
            }
        }
        if (isSettled() && tickCount % SeatCheckInterval == 0) {
            Vec3 Anchor = SeatPoint;
            boolean Drifted = Anchor != null && position().distanceToSqr(Anchor) > SeatDrift * SeatDrift;
            boolean Broken = Seat != null && !Scp017Corners.stillValid(World, Seat);
            if (Drifted || Broken) {
                unsettle();
                SearchCooldown = 0;
            }
        }
    }

    private boolean snuffableNear(ServerLevel World) {
        if (tickCount - NearCheckTick >= NearCheckInterval) {
            NearCheckTick = tickCount;
            NearCache = Scp017Snuffer.anyNear(World, this, position().add(0.0, Scp017Snuffer.CentreLift, 0.0));
        }
        return NearCache;
    }

    private boolean noteSudden() {
        if (suddenLight()) {
            SuddenUntil = tickCount + SuddenMemory;
        }
        recordLight();
        return tickCount < SuddenUntil;
    }

    private void huntingTick(ServerLevel World) {
        boolean Sudden = noteSudden();
        if (action() == ActionEmerge || action() == ActionWake) {
            return;
        }
        if (BrightTicks >= BrightTicksToRecoil && !snuffableNear(World)) {
            recoil(World, Sudden, null);
            return;
        }
        if (tickCount % RetargetInterval == 0) {
            retarget();
        }
        LivingEntity Target = getTarget();
        if (Target != null && distanceTo(Target) < ShunRange && Scp017Light.inLight(Target) && action() != ActionRecoil
                && !Scp017Snuffer.anyNear(World, this, Target.position().add(0.0, Scp017Snuffer.CentreLift, 0.0))) {
            recoil(World, false, Target);
        }
    }

    private void fleeingTick(ServerLevel World) {
        boolean Sudden = suddenLight();
        noteSudden();
        FleeTicks++;
        if (Sudden && BurnTimer <= 0 && action() != ActionRecoil && !snuffableNear(World)) {
            recoil(World, true, null);
            return;
        }
        if (action() != ActionNone) {
            return;
        }
        if (DarkTicks >= RecoverTicks) {
            setMode(ModeHunting);
            FleeTicks = 0;
            return;
        }
        if (FleeTicks >= FleeLimit) {
            goDormant();
        }
    }

    public void fleeFailed() {
        if (isFleeing() && LightNow > Scp017Light.Threshold) {
            goDormant();
        }
    }

    private void wake(ServerLevel World) {
        unsettle();
        setMode(ModeHunting);
        FleeTicks = 0;
        BrightTicks = 0;
        setAction(ActionWake, WakeAnimationTicks);
        World.playSound(null, getX(), getY() + 1.0, getZ(), Scp017Registry.Wake.get(), SoundSource.HOSTILE, 0.7f, 0.9f + random.nextFloat() * 0.15f);
    }

    private void goDormant() {
        setMode(ModeDormant);
        setTarget(null);
        getNavigation().stop();
        FleeTicks = 0;
        DarkTicks = 0;
        SearchCooldown = 0;
    }

    private void recoil(ServerLevel World, boolean Burn, @Nullable LivingEntity Source) {
        setMode(ModeFleeing);
        FleeTicks = 0;
        DarkTicks = 0;
        setAction(ActionRecoil, RecoilTicks);
        if (Source != null) {
            Shunned = Source.getUUID();
            ShunUntil = World.getGameTime() + ShunTicks;
        }
        World.playSound(null, getX(), getY() + 1.0, getZ(), Scp017Registry.Recoil.get(), SoundSource.HOSTILE, Burn ? 1.0f : 0.6f, 0.9f + random.nextFloat() * 0.2f);
        World.sendParticles(Scp017Registry.Shade.get(), getX(), getY() + 1.0, getZ(), Burn ? 26 : 10, 0.3, 0.6, 0.3, 0.06);
        if (Burn && BurnTimer <= 0) {
            BurnTimer = BurnCooldown;
            World.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 12, 0.25, 0.6, 0.25, 0.02);
            hurt(damageSources().source(Scp017Registry.LightDamage), LightBurn);
        }
    }

    private void actionFinished(ServerLevel World, byte Kind) {
        if (Kind == ActionReach) {
            grasp(World);
        }
    }

    public void beginReach(LivingEntity Victim) {
        ReachTarget = Victim.getUUID();
        ReachTimer = ReachCooldown + ReachTicks;
        setAction(ActionReach, ReachTicks);
        triggerAnim("reach", "reach");
        level().playSound(null, getX(), getY() + 1.4, getZ(), Scp017Registry.Reach.get(), SoundSource.HOSTILE, 0.8f, 0.95f + random.nextFloat() * 0.1f);
    }

    private void grasp(ServerLevel World) {
        if (ReachTarget == null || !isHunting()) {
            return;
        }
        LivingEntity Victim = World.getEntity(ReachTarget) instanceof LivingEntity Found ? Found : null;
        ReachTarget = null;
        if (Victim == null || !huntable(Victim) || World.getDifficulty() == Difficulty.PEACEFUL || distanceTo(Victim) > GraspRange || Scp017Light.inLight(Victim)
                || !hasLineOfSight(Victim)) {
            return;
        }
        consume(World, Victim);
    }

    private void consume(ServerLevel World, LivingEntity Victim) {
        World.playSound(null, Victim.getX(), Victim.getY() + 1.0, Victim.getZ(), Scp017Registry.Consume.get(), SoundSource.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.1f);
        World.sendParticles(Scp017Registry.Shade.get(), Victim.getX(), Victim.getY() + 1.0, Victim.getZ(), 48, 0.35, 0.7, 0.35, 0.05);
        Victim.hurt(damageSources().source(Scp017Registry.ConsumedDamage, this), Float.MAX_VALUE);
    }

    public static boolean huntable(LivingEntity Candidate) {
        if (!Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (Candidate instanceof Player User) {
            return User.isAlive() && !User.isSpectator() && !User.isCreative() && !Scp268.isNeglected(User);
        }
        return Candidate.isAlive() && !Candidate.isInvulnerable() && ControlBodies.isPersonLike(Candidate) && !ControlSessions.isBoundToSession(Candidate);
    }

    private void retarget() {
        LivingEntity Best = null;
        double BestScore = Double.MAX_VALUE;
        LivingEntity Current = getTarget();
        List<LivingEntity> Candidates = new ArrayList<>(level().players());
        Candidates.addAll(level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(HuntRange), Found -> !(Found instanceof Player) && huntable(Found)));
        for (LivingEntity Candidate : Candidates) {
            if (!huntable(Candidate)) {
                continue;
            }
            double Distance = distanceTo(Candidate);
            if (Distance > HuntRange) {
                continue;
            }
            if (Candidate != Current && Distance > NoticeRange && !hasLineOfSight(Candidate)) {
                continue;
            }
            double Score = Distance;
            if (Scp017Light.inLight(Candidate)) {
                Score += LitPenalty;
            }
            if (shuns(Candidate)) {
                Score += LitPenalty;
            }
            if (Candidate == Current) {
                Score -= 4.0;
            }
            if (Score < BestScore) {
                BestScore = Score;
                Best = Candidate;
            }
        }
        if (Best != Current) {
            setTarget(Best);
        }
    }

    public void settleAt(@Nullable Scp017Corners.Corner Nook) {
        Vec3 Point;
        float Yaw;
        if (Nook != null) {
            Point = Nook.seat();
            Yaw = Nook.yaw();
        } else {
            Point = position();
            Yaw = facingWall();
        }
        getNavigation().stop();
        moveTo(Point.x, Point.y, Point.z, Yaw, 0.0f);
        setDeltaMovement(Vec3.ZERO);
        Seat = Nook;
        SeatPoint = Point;
        entityData.set(SeatYawData, Yaw);
        entityData.set(SettledData, true);
        setPose(Pose.CROUCHING);
        setAction(ActionSettle, SettleTicks);
        level().playSound(null, getX(), getY() + 0.5, getZ(), Scp017Registry.Settle.get(), SoundSource.HOSTILE, 0.5f, 0.9f + random.nextFloat() * 0.15f);
    }

    private float facingWall() {
        BlockPos Pos = blockPosition();
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (Scp017Corners.wall(level(), Pos, Side)) {
                return Side.toYRot();
            }
        }
        return getYRot();
    }

    public void unsettle() {
        if (!isSettled()) {
            return;
        }
        entityData.set(SettledData, false);
        Seat = null;
        SeatPoint = null;
        setPose(Pose.STANDING);
        if (action() == ActionSettle) {
            setAction(ActionNone, 0);
        }
    }

    public void beginEmerging() {
        setAction(ActionEmerge, EmergeTicks);
    }

    public void bind() {
        Bound = true;
    }

    public static Optional<Scp017Shadow> manifest(ServerLevel World, Vec3 Spot, float Yaw) {
        if (Scp017World.get(World.getServer()).blocks(null) && !ArchiveRules.repeatable(World.getServer())) {
            return Optional.empty();
        }
        Scp017Shadow Shadow = Scp017Registry.Shadow.get().create(World);
        if (Shadow == null) {
            return Optional.empty();
        }
        Shadow.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Shadow.setYHeadRot(Yaw);
        Shadow.yBodyRot = Yaw;
        Shadow.bind();
        Shadow.LightNow = Scp017Light.of(Shadow);
        for (int Index = 0; Index < SuddenWindow; Index++) {
            Shadow.LightHistory[Index] = Shadow.LightNow;
        }
        if (Shadow.LightNow <= Scp017Light.Threshold) {
            Shadow.setMode(ModeHunting);
        }
        Shadow.beginEmerging();
        if (!World.addFreshEntity(Shadow)) {
            return Optional.empty();
        }
        Scp017World.get(World.getServer()).claim(Shadow.getUUID());
        World.playSound(null, Spot.x, Spot.y + 0.5, Spot.z, Scp017Registry.Emerge.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
        World.sendParticles(Scp017Registry.Shade.get(), Spot.x, Spot.y + 0.3, Spot.z, 30, 0.3, 0.2, 0.3, 0.04);
        return Optional.of(Shadow);
    }

    private void dissolve(ServerLevel World) {
        World.sendParticles(Scp017Registry.Shade.get(), getX(), getY() + 1.0, getZ(), 30, 0.3, 0.7, 0.3, 0.05);
        discard();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Scaling, MobSpawnType Reason, @Nullable SpawnGroupData Data) {
        Bound = Reason != MobSpawnType.COMMAND;
        return super.finalizeSpawn(World, Scaling, Reason, Data);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || Source.isCreativePlayer() || Source.is(Scp017Registry.LightDamage)) {
            return super.isInvulnerableTo(Source);
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && isAlive() && Source.getEntity() != null && isInvulnerableTo(Source) && PassTimer <= 0 && level() instanceof ServerLevel World) {
            PassTimer = PassCooldown;
            World.playSound(null, getX(), getY() + 1.0, getZ(), Scp017Registry.Pass.get(), SoundSource.HOSTILE, 0.6f, 0.9f + random.nextFloat() * 0.2f);
            World.sendParticles(Scp017Registry.Shade.get(), getX(), getY() + 1.1, getZ(), 8, 0.2, 0.4, 0.2, 0.03);
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (dead && level() instanceof ServerLevel World) {
            Scp017World.get(World.getServer()).free(getUUID());
            World.sendParticles(Scp017Registry.Shade.get(), getX(), getY() + 1.0, getZ(), 60, 0.35, 0.8, 0.35, 0.06);
        }
    }

    @Override
    public void remove(Entity.RemovalReason Reason) {
        if (Reason == Entity.RemovalReason.DISCARDED && Bound && !Duplicate && !dead && level() instanceof ServerLevel World) {
            Scp017World.get(World.getServer()).free(getUUID());
        }
        super.remove(Reason);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canDrownInFluidType(FluidType Fluid) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
    }

    @Override
    public boolean isPushable() {
        return !isSettled() && super.isPushable();
    }

    @Override
    public void push(Entity Other) {
        if (!isSettled()) {
            super.push(Other);
        }
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
    protected EntityDimensions getDefaultDimensions(Pose Stance) {
        if (Stance == Pose.CROUCHING) {
            return EntityDimensions.scalable(Width, CrouchHeight).withEyeHeight(CrouchEyeHeight);
        }
        return super.getDefaultDimensions(Stance);
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
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Override
    protected float getSoundVolume() {
        return 0.45f;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isAwake() ? Scp017Registry.Whisper.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp017Registry.RecoilCry.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp017Registry.Unravel.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    private void clientTick() {
        VeilO = Veil;
        float Goal = isAwake() && isAlive() ? 1.0f : 0.0f;
        Veil = Veil < Goal ? Math.min(Goal, Veil + VeilStep) : Math.max(Goal, Veil - VeilStep);
        Scp017Gait Active = isAwake() ? huntStride() : walkStride();
        Scp017Gait Idle = isAwake() ? walkStride() : huntStride();
        Idle.reset();
        Active.tick(this);
        if (isAlive()) {
            trail();
        }
    }

    private void trail() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        double Moving = Math.sqrt(Dx * Dx + Dz * Dz);
        float Chance = Mth.lerp(Veil, IdleWisp, (float) Math.min(1.0, 0.55 + Moving * 5.0));
        int Count = random.nextFloat() < Chance ? 1 + (Moving > 0.12 ? 1 : 0) : 0;
        float Top = getBbHeight();
        for (int Index = 0; Index < Count; Index++) {
            double Px = getX() + (random.nextDouble() - 0.5) * 0.55;
            double Py = getY() + 0.1 + random.nextDouble() * (Top - 0.15);
            double Pz = getZ() + (random.nextDouble() - 0.5) * 0.55;
            level().addParticle(Scp017Registry.Shade.get(), Px, Py, Pz, -Dx * 0.3 + (random.nextDouble() - 0.5) * 0.01, 0.006 + random.nextDouble() * 0.012,
                    -Dz * 0.3 + (random.nextDouble() - 0.5) * 0.01);
        }
    }

    public float veil(float Partial) {
        return Mth.lerp(Partial, VeilO, Veil);
    }

    public float eyeGlow(float Partial) {
        float Base = veil(Partial);
        byte Current = action();
        if (Current == ActionReach) {
            return Math.min(1.0f, Base * 1.15f);
        }
        if (Current == ActionRecoil) {
            return Base * 0.35f;
        }
        return Base * (0.86f + 0.06f * Mth.sin((tickCount + Partial) * 0.11f));
    }

    private Scp017Gait walkStride() {
        if (WalkStride == null) {
            WalkStride = new Scp017Gait(WalkGait);
        }
        return WalkStride;
    }

    private Scp017Gait huntStride() {
        if (HuntStride == null) {
            HuntStride = new Scp017Gait(HuntGait);
        }
        return HuntStride;
    }

    @Nullable
    public Scp017Gait gaitFor(String Clip) {
        if (!level().isClientSide) {
            return null;
        }
        if (WalkGait.Clip().equals(Clip)) {
            return walkStride();
        }
        if (HuntGait.Clip().equals(Clip)) {
            return huntStride();
        }
        return null;
    }

    public boolean isStriding() {
        return (isAwake() ? huntStride() : walkStride()).striding();
    }

    public void setGaitPartial(float Partial) {
        walkStride().setPartial(Partial);
        huntStride().setPartial(Partial);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (Key.equals(ActionData) && level().isClientSide && BodyController != null && action() != ActionNone && action() != ActionReach) {
            BodyController.forceAnimationReset();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("Mode", mode());
        Tag.putByte("Action", action());
        Tag.putInt("ActionTicks", ActionTicks);
        Tag.putBoolean("Bound", Bound);
        Tag.putInt("FleeTicks", FleeTicks);
        Tag.putInt("DarkTicks", DarkTicks);
        Tag.putBoolean("Settled", isSettled());
        Tag.putFloat("SeatYaw", seatYaw());
        if (SeatPoint != null) {
            Tag.putDouble("SeatX", SeatPoint.x);
            Tag.putDouble("SeatY", SeatPoint.y);
            Tag.putDouble("SeatZ", SeatPoint.z);
        }
        if (Seat != null) {
            Tag.put("Corner", NbtUtils.writeBlockPos(Seat.Pos()));
            Tag.putString("CornerFirst", Seat.First().getSerializedName());
            Tag.putString("CornerSecond", Seat.Second().getSerializedName());
            Tag.putBoolean("CornerFull", Seat.FullHeight());
            Tag.putDouble("CornerInset", Seat.Inset());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        byte SavedMode = Tag.getByte("Mode");
        setMode(SavedMode == ModeHunting || SavedMode == ModeFleeing ? SavedMode : ModeDormant);
        int Ticks = Tag.getInt("ActionTicks");
        byte SavedAction = Tag.getByte("Action");
        entityData.set(ActionData, Ticks > 0 && SavedAction != ActionReach ? SavedAction : ActionNone);
        ActionTicks = Ticks > 0 && SavedAction != ActionReach ? Ticks : 0;
        Bound = Tag.getBoolean("Bound");
        FleeTicks = Tag.getInt("FleeTicks");
        DarkTicks = Tag.getInt("DarkTicks");
        boolean WasSettled = Tag.getBoolean("Settled");
        entityData.set(SeatYawData, Tag.getFloat("SeatYaw"));
        SeatPoint = Tag.contains("SeatX") ? new Vec3(Tag.getDouble("SeatX"), Tag.getDouble("SeatY"), Tag.getDouble("SeatZ")) : null;
        Seat = null;
        Optional<BlockPos> CornerPos = NbtUtils.readBlockPos(Tag, "Corner");
        Direction First = Direction.byName(Tag.getString("CornerFirst"));
        Direction Second = Direction.byName(Tag.getString("CornerSecond"));
        if (CornerPos.isPresent() && First != null && Second != null) {
            Seat = new Scp017Corners.Corner(CornerPos.get(), First, Second, Tag.getBoolean("CornerFull"), Tag.getDouble("CornerInset"));
        }
        entityData.set(SettledData, WasSettled && mode() == ModeDormant);
        setPose(isSettled() ? Pose.CROUCHING : Pose.STANDING);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Scp017GaitController Body = new Scp017GaitController(this, "body", 6, Event -> {
            if (isDeadOrDying()) {
                return Event.setAndContinue(UnravelPlay);
            }
            switch (action()) {
                case ActionEmerge -> {
                    return Event.setAndContinue(EmergePlay);
                }
                case ActionWake -> {
                    return Event.setAndContinue(WakePlay);
                }
                case ActionRecoil -> {
                    return Event.setAndContinue(RecoilPlay);
                }
                case ActionSettle -> {
                    return Event.setAndContinue(CrouchPlay);
                }
                default -> {
                }
            }
            if (isSettled()) {
                return Event.setAndContinue(CrouchedLoop);
            }
            if (isAwake()) {
                return Event.setAndContinue(isStriding() ? HuntLoop : ProwlLoop);
            }
            return Event.setAndContinue(isStriding() ? WalkLoop : IdleLoop);
        });
        BodyController = Body;
        Controllers.add(Body);
        Controllers.add(new AnimationController<>(this, "reach", 2, Event -> PlayState.STOP).triggerableAnim("reach", ReachOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
