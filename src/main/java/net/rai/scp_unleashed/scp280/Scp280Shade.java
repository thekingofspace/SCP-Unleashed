package net.rai.scp_unleashed.scp280;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClassLike;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp280Shade extends Monster implements GeoEntity {
    public static final float Width = 0.6f;
    public static final float Height = 2.6f;
    public static final float EyeHeight = 2.35f;
    public static final float MaxHealth = 60.0f;
    public static final float StrikeDamage = 4.0f;
    public static final byte ModeHunt = 0;
    public static final byte ModePinned = 1;
    public static final byte ModeContained = 2;
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final byte ActionRecoil = 2;
    public static final byte ActionWindup = 3;
    public static final byte ActionStrike = 4;
    public static final int EmergeTicks = 36;
    public static final int RecoilTicks = 12;
    public static final int WindupTicks = 8;
    public static final int StrikeTicks = 8;
    public static final int StrikeCooldown = 22;
    public static final double StrikeReach = 2.4;
    public static final double StrikeSlack = 0.8;
    public static final float LightBurn = 6.0f;
    public static final int BurnCooldown = 30;
    public static final int ShineInterval = 5;
    public static final int ShineCooldown = 16;
    public static final int ShineMemory = 40;
    public static final double ShinePush = 0.55;
    public static final int SealInterval = 10;
    public static final int ContainTicks = 160;
    public static final int BreachChecks = 2;
    public static final int RegenInterval = 40;
    public static final int RetargetInterval = 10;
    public static final double HuntRange = 40.0;
    public static final double NoticeRange = 20.0;
    public static final double LitPenalty = 24.0;
    public static final int LostLimit = 3600;
    public static final int PassCooldown = 10;
    public static final float FleeLitMalus = 12.0f;
    public static final double ProwlSpeed = 0.8;
    public static final double StalkSpeed = 1.0;
    public static final double ChaseSpeed = 1.2;
    public static final double FleeSpeed = 1.3;
    public static final double CreepSpeed = 0.6;
    public static final float IdleWisp = 0.25f;
    public static final Scp280Gait.Profile StalkGait = new Scp280Gait.Profile("stalk", 20.0, 0.0909, 0.5, 2.4, 4, new double[]{0.99, 0.49}, new float[]{1.0f, 1.0f},
            Scp280Registry.Step, 0.16f, 0.9f);
    public static final Scp280Gait.Profile RunGait = new Scp280Gait.Profile("run", 16.0, 0.1875, 0.5, 2.0, 2, new double[]{0.99, 0.49}, new float[]{1.0f, 1.0f},
            Scp280Registry.Step, 0.08f, 1.0f);
    private static final EntityDataAccessor<Byte> ModeData = SynchedEntityData.defineId(Scp280Shade.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp280Shade.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> HuddledData = SynchedEntityData.defineId(Scp280Shade.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ChasingData = SynchedEntityData.defineId(Scp280Shade.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation StalkLoop = RawAnimation.begin().thenLoop("stalk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation RecoilPlay = RawAnimation.begin().thenPlayAndHold("recoil");
    private static final RawAnimation WindupPlay = RawAnimation.begin().thenPlayAndHold("windup");
    private static final RawAnimation StrikePlay = RawAnimation.begin().thenPlayAndHold("strike");
    private static final RawAnimation HuddledLoop = RawAnimation.begin().thenLoop("huddled");
    private static final RawAnimation ContainedLoop = RawAnimation.begin().thenLoop("contained");
    private static final RawAnimation DissipatePlay = RawAnimation.begin().thenPlayAndHold("dissipate");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private long Token = -1L;
    @Nullable
    private UUID Bound;
    private int ActionTicks;
    private int PinTicks;
    private int BreachCount;
    private int BurnTimer;
    private int ShineTimer;
    private int ShinedUntil;
    private int StrikeTimer;
    private int PassTimer;
    private int RegenTimer;
    private int LostTicks;
    private int LightNow;
    private boolean Sealed;
    private boolean ShelterFailed;
    private boolean ShelterTried;
    @Nullable
    private Vec3 ShineFrom;
    @Nullable
    private UUID StrikeTarget;
    @Nullable
    private Vec3 LastSeen;
    @Nullable
    private Scp280Gait StalkStride;
    @Nullable
    private Scp280Gait RunStride;
    @Nullable
    private AnimationController<Scp280Shade> BodyController;
    private int ActionClock;

    public Scp280Shade(EntityType<? extends Scp280Shade> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, StrikeDamage)
                .add(Attributes.FOLLOW_RANGE, HuntRange).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ModeData, ModeHunt);
        Builder.define(ActionData, ActionNone);
        Builder.define(HuddledData, false);
        Builder.define(ChasingData, false);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp280Navigation(this, World);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp280Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp280Goals.Flee(this));
        goalSelector.addGoal(3, new Scp280Goals.Hunt(this));
        goalSelector.addGoal(5, new Scp280Goals.Prowl(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public byte mode() {
        return entityData.get(ModeData);
    }

    public boolean isContained() {
        return mode() == ModeContained;
    }

    public boolean isPinned() {
        return mode() == ModePinned;
    }

    public boolean isHuddled() {
        return entityData.get(HuddledData);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public float actionAge(float Partial) {
        return tickCount - ActionClock + Partial;
    }

    public int pinTicks() {
        return PinTicks;
    }

    public long token() {
        return Token;
    }

    public int lightNow() {
        return LightNow;
    }

    public boolean sealedNow() {
        return Sealed;
    }

    @Nullable
    public UUID bound() {
        return Bound;
    }

    public boolean locksMovement() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionRecoil || Current == ActionWindup || Current == ActionStrike || isContained() || isDeadOrDying();
    }

    public boolean wantsShelter() {
        return !isContained() && (LightNow >= Scp280Light.BrightLevel || tickCount < ShinedUntil);
    }

    public void shelterFailed() {
        ShelterFailed = true;
        ShelterTried = true;
    }

    public void shelterFound() {
        ShelterFailed = false;
        ShelterTried = true;
    }

    @Nullable
    public Vec3 shineSource() {
        return tickCount < ShinedUntil ? ShineFrom : null;
    }

    @Nullable
    public Vec3 lure() {
        if (Bound != null && level().getPlayerByUUID(Bound) instanceof Player Victim && Victim.distanceTo(this) < HuntRange * 1.5) {
            return Victim.position();
        }
        return LastSeen;
    }

    public boolean readyToStrike() {
        return StrikeTimer <= 0 && action() == ActionNone;
    }

    private void setMode(byte Next) {
        entityData.set(ModeData, Next);
        if (Next != ModePinned) {
            entityData.set(HuddledData, false);
        }
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
    }

    @Override
    public float getPathfindingMalus(PathType Type) {
        if (Type == Scp280Navigation.LitPath) {
            return wantsShelter() ? FleeLitMalus : -1.0f;
        }
        return super.getPathfindingMalus(Type);
    }

    @Override
    public float getWalkTargetValue(BlockPos Pos, LevelReader World) {
        return Scp280Light.BrightLevel - Scp280Light.standing(level(), Pos);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        Scp280World Data = Scp280World.get(World.getServer());
        if (!isContained() && Token >= 0L && !Data.current(Token)) {
            fade(World);
            return;
        }
        if (isContained() && !Data.isCaptured()) {
            setMode(ModeHunt);
            Data.bindLoose(getUUID(), World.getGameTime());
        }
        if (!isContained()) {
            if (Data.loose() == null && Token >= 0L && Data.current(Token)) {
                Data.bindLoose(getUUID(), World.getGameTime());
            }
            Data.seen(getUUID(), World.getGameTime());
        }
        if (ActionTicks > 0) {
            byte Current = action();
            if (--ActionTicks <= 0) {
                entityData.set(ActionData, ActionNone);
                actionFinished(World, Current);
            }
        }
        BurnTimer = Math.max(0, BurnTimer - 1);
        ShineTimer = Math.max(0, ShineTimer - 1);
        StrikeTimer = Math.max(0, StrikeTimer - 1);
        PassTimer = Math.max(0, PassTimer - 1);
        LightNow = Scp280Light.of(this);
        if (tickCount % SealInterval == 0 || tickCount < 3) {
            Sealed = Scp280Light.sealed(World, this);
        }
        if (isContained()) {
            containedTick(World);
            return;
        }
        if (action() == ActionEmerge) {
            return;
        }
        boolean Bright = LightNow >= Scp280Light.BrightLevel;
        if (Bright && Sealed) {
            if (!isPinned()) {
                setMode(ModePinned);
                ShelterFailed = false;
                ShelterTried = false;
                Scp280Haunt.playAt(World, position().add(0.0, 1.4, 0.0), List.of(Scp280Registry.Huddle), 0.8f, 0.9f + random.nextFloat() * 0.15f);
            }
            PinTicks++;
            entityData.set(HuddledData, (ShelterFailed || ShelterTried) && getNavigation().isDone());
            entityData.set(ChasingData, false);
            setTarget(null);
            if (PinTicks >= ContainTicks) {
                contain(World);
                return;
            }
        } else {
            if (isPinned()) {
                setMode(ModeHunt);
            }
            PinTicks = 0;
            if (Bright) {
                burn(World);
            }
        }
        if (tickCount % ShineInterval == 0 && ShineTimer <= 0) {
            checkShine(World);
        }
        if (!isPinned()) {
            if (tickCount % RetargetInterval == 0) {
                retarget();
            }
            LivingEntity Target = getTarget();
            entityData.set(ChasingData, Target != null && Scp280Light.strikeable(Target));
            if (Target != null) {
                LastSeen = Target.position();
                LostTicks = 0;
            } else if (++LostTicks >= LostLimit) {
                fade(World);
                return;
            }
        }
        if (!Bright && ++RegenTimer >= RegenInterval) {
            RegenTimer = 0;
            if (getHealth() < getMaxHealth()) {
                heal(1.0f);
            }
        }
    }

    private void containedTick(ServerLevel World) {
        getNavigation().stop();
        setTarget(null);
        entityData.set(ChasingData, false);
        if (tickCount % 20 == 0) {
            boolean Holding = LightNow >= Scp280Light.BrightLevel && Sealed;
            BreachCount = Holding ? 0 : BreachCount + 1;
            if (BreachCount >= BreachChecks) {
                breach(World);
            }
        }
    }

    private void contain(ServerLevel World) {
        setMode(ModeContained);
        setAction(ActionNone, 0);
        PinTicks = 0;
        BreachCount = 0;
        setTarget(null);
        getNavigation().stop();
        setHealth(getMaxHealth());
        World.sendParticles(Scp280Registry.Wisp.get(), getX(), getY() + 1.0, getZ(), 24, 0.3, 0.8, 0.3, 0.02);
        Scp280Haunt.captured(World, this);
    }

    private void breach(ServerLevel World) {
        setMode(ModeHunt);
        BreachCount = 0;
        PinTicks = 0;
        LostTicks = 0;
        Scp280Haunt.released(World, this, true);
        Scp280World.get(World.getServer()).bindLoose(getUUID(), World.getGameTime());
        Scp280Haunt.playAt(World, position().add(0.0, 1.4, 0.0), List.of(Scp280Registry.Breach), 1.0f, 1.0f);
        World.sendParticles(Scp280Registry.Wisp.get(), getX(), getY() + 1.2, getZ(), 30, 0.35, 0.9, 0.35, 0.05);
    }

    private void burn(ServerLevel World) {
        if (BurnTimer > 0) {
            return;
        }
        BurnTimer = BurnCooldown;
        recoil(World, null, true);
        hurt(damageSources().source(Scp280Registry.LightDamage), LightBurn);
    }

    private void checkShine(ServerLevel World) {
        AABB Reach = getBoundingBox().inflate(Scp280Light.ShineRange);
        for (Player Holder : World.getEntitiesOfClass(Player.class, Reach, Candidate -> !Candidate.isSpectator() && Candidate.isAlive())) {
            if (Scp280Light.shining(Holder, this)) {
                push(World, Holder);
                return;
            }
        }
    }

    private void push(ServerLevel World, Player Holder) {
        ShineTimer = ShineCooldown;
        ShinedUntil = tickCount + ShineMemory;
        ShineFrom = Holder.position();
        Vec3 Away = position().subtract(Holder.position()).multiply(1.0, 0.0, 1.0);
        if (Away.lengthSqr() < 1.0e-4) {
            Away = new Vec3(random.nextDouble() - 0.5, 0.0, random.nextDouble() - 0.5);
        }
        Away = Away.normalize();
        setDeltaMovement(getDeltaMovement().add(Away.x * ShinePush, 0.12, Away.z * ShinePush));
        hurtMarked = true;
        if (StrikeTarget != null && action() == ActionWindup) {
            StrikeTarget = null;
        }
        recoil(World, Holder, false);
    }

    private void recoil(ServerLevel World, @Nullable Player Source, boolean Burned) {
        if (isPinned() && Source == null) {
            return;
        }
        setAction(ActionRecoil, RecoilTicks);
        StrikeTarget = null;
        Scp280Haunt.playAt(World, position().add(0.0, 1.6, 0.0), Burned ? Scp280Registry.RecoilLayers : List.of(Scp280Registry.Recoil), Burned ? 1.0f : 0.7f,
                0.92f + random.nextFloat() * 0.16f);
        World.sendParticles(Scp280Registry.Wisp.get(), getX(), getY() + 1.4, getZ(), Burned ? 18 : 8, 0.3, 0.7, 0.3, 0.04);
    }

    public void beginWindup(LivingEntity Victim) {
        StrikeTarget = Victim.getUUID();
        StrikeTimer = StrikeCooldown + WindupTicks;
        setAction(ActionWindup, WindupTicks);
        if (level() instanceof ServerLevel World) {
            Scp280Haunt.playAt(World, position().add(0.0, 1.8, 0.0), List.of(Scp280Registry.Windup), 0.8f, 0.95f + random.nextFloat() * 0.1f);
        }
    }

    private void actionFinished(ServerLevel World, byte Kind) {
        if (Kind == ActionWindup) {
            strike(World);
        }
    }

    private void strike(ServerLevel World) {
        setAction(ActionStrike, StrikeTicks);
        UUID Wanted = StrikeTarget;
        StrikeTarget = null;
        if (Wanted == null || World.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        Entity Found = World.getEntity(Wanted);
        Scp280Haunt.playAt(World, position().add(0.0, 1.6, 0.0), Scp280Registry.StrikeLayers, 0.9f, 0.95f + random.nextFloat() * 0.1f);
        if (!(Found instanceof LivingEntity Victim) || !huntable(this, Victim) || distanceTo(Victim) > StrikeReach + StrikeSlack || !Scp280Light.strikeable(Victim)
                || Scp280Light.isBright(this) || !hasLineOfSight(Victim)) {
            return;
        }
        float Amount = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        Victim.hurt(damageSources().source(Scp280Registry.ShadeDamage, this), Amount);
        World.sendParticles(Scp280Registry.Wisp.get(), Victim.getX(), Victim.getY() + Victim.getBbHeight() * 0.6, Victim.getZ(), 10, 0.25, 0.35, 0.25, 0.03);
    }

    public static boolean huntable(Scp280Shade Hunter, @Nullable LivingEntity Candidate) {
        if (Candidate == null || Candidate == Hunter || !Candidate.isAlive() || Candidate.isRemoved() || Candidate.isSpectator() || Candidate.isInvulnerable()
                || Candidate.level() != Hunter.level() || Candidate instanceof Scp280Shade || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (Candidate instanceof Player User) {
            if (User.isCreative() || Hunter.level().getDifficulty() == Difficulty.PEACEFUL) {
                return false;
            }
            return !(User instanceof ServerPlayer Online) || !ControlManager.hasPendingDeath(Online);
        }
        if (!ControlBodies.isPersonLike(Candidate) || ControlSessions.isBoundToSession(Candidate)) {
            return false;
        }
        return !(Candidate instanceof DClassLike Body) || !Body.isSpecimen() && !Body.isPuppet();
    }

    private void retarget() {
        LivingEntity Current = getTarget();
        LivingEntity Best = null;
        double BestScore = Double.MAX_VALUE;
        for (LivingEntity Candidate : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(HuntRange), Living -> huntable(this, Living))) {
            double Distance = distanceTo(Candidate);
            if (Distance > HuntRange) {
                continue;
            }
            if (Candidate != Current && Distance > NoticeRange && !hasLineOfSight(Candidate)) {
                continue;
            }
            double Score = Distance;
            if (!Scp280Light.strikeable(Candidate)) {
                Score += LitPenalty;
            }
            if (Candidate.getUUID().equals(Bound)) {
                Score -= 8.0;
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

    public void leaveStasis(ServerLevel World) {
        Scp280World Data = Scp280World.get(World.getServer());
        LightNow = Scp280Light.of(this);
        Sealed = Scp280Light.sealed(World, this);
        if (Sealed && LightNow >= Scp280Light.BrightLevel) {
            if (!isContained()) {
                if (Token < 0L || !Data.current(Token)) {
                    Token = Data.nextToken();
                }
                Data.bindLoose(getUUID(), World.getGameTime());
            }
            return;
        }
        if (getUUID().equals(Data.held())) {
            Scp280Haunt.released(World, this, true);
        }
        setMode(ModeHunt);
        fade(World);
    }

    private void fade(ServerLevel World) {
        World.sendParticles(Scp280Registry.Wisp.get(), getX(), getY() + 1.2, getZ(), 34, 0.3, 0.9, 0.3, 0.03);
        Scp280Haunt.playAt(World, position().add(0.0, 1.2, 0.0), List.of(Scp280Registry.DissipateAir), 0.8f, 1.0f);
        Scp280World.get(World.getServer()).freeLoose(getUUID());
        discard();
    }

    public void bind(long Value, @Nullable UUID Player) {
        Token = Value;
        Bound = Player;
    }

    public static Optional<Scp280Shade> reveal(ServerLevel World, Vec3 Spot, ServerPlayer Victim) {
        Scp280Shade Shade = Scp280Registry.Shade.get().create(World);
        if (Shade == null) {
            return Optional.empty();
        }
        Vec3 Toward = Victim.position().subtract(Spot);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        Shade.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Shade.setYHeadRot(Yaw);
        Shade.yBodyRot = Yaw;
        if (!World.noCollision(Shade)) {
            return Optional.empty();
        }
        Shade.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.EVENT, null);
        Scp280World Data = Scp280World.get(World.getServer());
        Shade.bind(Data.nextToken(), Victim.getUUID());
        Shade.LastSeen = Victim.position();
        Shade.setAction(ActionEmerge, EmergeTicks);
        if (!World.addFreshEntity(Shade)) {
            return Optional.empty();
        }
        Data.bindLoose(Shade.getUUID(), World.getGameTime());
        Data.countReveal();
        Scp280Haunt.playAt(World, Spot.add(0.0, 1.2, 0.0), Scp280Registry.EmergeLayers, 1.0f, 1.0f);
        World.sendParticles(Scp280Registry.Wisp.get(), Spot.x, Spot.y + 0.4, Spot.z, 30, 0.35, 0.3, 0.35, 0.03);
        return Optional.of(Shade);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Scaling, MobSpawnType Reason, @Nullable SpawnGroupData Data) {
        SpawnGroupData Result = super.finalizeSpawn(World, Scaling, Reason, Data);
        if (Reason != MobSpawnType.EVENT && World.getLevel() instanceof ServerLevel Server) {
            Scp280World Record = Scp280World.get(Server.getServer());
            Token = Record.nextToken();
            setAction(ActionEmerge, EmergeTicks);
        }
        return Result;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level() instanceof ServerLevel World && Token >= 0L && !isContained()) {
            Scp280World Data = Scp280World.get(World.getServer());
            if (Data.current(Token) && !getUUID().equals(Data.loose())) {
                Data.bindLoose(getUUID(), World.getGameTime());
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || Source.isCreativePlayer()) {
            return super.isInvulnerableTo(Source);
        }
        if (Source.is(Scp280Registry.LightDamage) && !isContained() && action() != ActionEmerge) {
            return super.isInvulnerableTo(Source);
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && isAlive() && Source.getEntity() != null && isInvulnerableTo(Source) && PassTimer <= 0 && level() instanceof ServerLevel World) {
            PassTimer = PassCooldown;
            Scp280Haunt.playAt(World, position().add(0.0, 1.3, 0.0), List.of(Scp280Registry.Pass), 0.6f, 0.9f + random.nextFloat() * 0.2f);
            World.sendParticles(Scp280Registry.Wisp.get(), getX(), getY() + 1.3, getZ(), 8, 0.2, 0.45, 0.2, 0.03);
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public void die(DamageSource Source) {
        boolean WasContained = isContained();
        super.die(Source);
        if (dead && level() instanceof ServerLevel World) {
            Scp280World Data = Scp280World.get(World.getServer());
            Data.freeLoose(getUUID());
            if (WasContained) {
                Scp280Haunt.released(World, this, true);
            }
            Scp280Haunt.playAt(World, position().add(0.0, 1.2, 0.0), Scp280Registry.DissipateLayers, 1.0f, 1.0f);
            World.sendParticles(Scp280Registry.Wisp.get(), getX(), getY() + 1.2, getZ(), 50, 0.35, 1.0, 0.35, 0.05);
        }
    }

    @Override
    public void remove(Entity.RemovalReason Reason) {
        if (Reason == Entity.RemovalReason.DISCARDED && !dead && level() instanceof ServerLevel World && isContained()) {
            Scp280Haunt.released(World, this, true);
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
        return !isContained() && super.isPushable();
    }

    @Override
    public void push(Entity Other) {
        if (!isContained()) {
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
        return 240;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isContained() || isPinned() ? null : Scp280Registry.Murmur.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp280Registry.RecoilCry.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp280Registry.Dissipate.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    private void clientTick() {
        Scp280Gait Active = isRunning() ? runStride() : stalkStride();
        Scp280Gait Idle = isRunning() ? stalkStride() : runStride();
        Idle.reset();
        Active.tick(this);
        if (isAlive()) {
            trail();
        }
    }

    public boolean isRunning() {
        return entityData.get(ChasingData);
    }

    public boolean isChasing() {
        return entityData.get(ChasingData);
    }

    private double deltaSpeed() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    private void trail() {
        double Moving = deltaSpeed();
        float Chance = isContained() ? 0.08f : Math.min(1.0f, IdleWisp + (float) Moving * 4.0f);
        if (random.nextFloat() >= Chance) {
            return;
        }
        float Top = getBbHeight();
        double Px = getX() + (random.nextDouble() - 0.5) * 0.6;
        double Py = getY() + 0.2 + random.nextDouble() * (Top - 0.3);
        double Pz = getZ() + (random.nextDouble() - 0.5) * 0.6;
        level().addParticle(Scp280Registry.Wisp.get(), Px, Py, Pz, -(getX() - xo) * 0.3 + (random.nextDouble() - 0.5) * 0.01, 0.004 + random.nextDouble() * 0.01,
                -(getZ() - zo) * 0.3 + (random.nextDouble() - 0.5) * 0.01);
    }

    private Scp280Gait stalkStride() {
        if (StalkStride == null) {
            StalkStride = new Scp280Gait(StalkGait);
        }
        return StalkStride;
    }

    private Scp280Gait runStride() {
        if (RunStride == null) {
            RunStride = new Scp280Gait(RunGait);
        }
        return RunStride;
    }

    @Nullable
    public Scp280Gait gaitFor(String Clip) {
        if (!level().isClientSide) {
            return null;
        }
        if (StalkGait.Clip().equals(Clip)) {
            return stalkStride();
        }
        if (RunGait.Clip().equals(Clip)) {
            return runStride();
        }
        return null;
    }

    public boolean isStriding() {
        return (isRunning() ? runStride() : stalkStride()).striding();
    }

    public void setGaitPartial(float Partial) {
        stalkStride().setPartial(Partial);
        runStride().setPartial(Partial);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (Key.equals(ActionData) && level().isClientSide) {
            ActionClock = tickCount;
            if (BodyController != null && action() != ActionNone) {
                BodyController.forceAnimationReset();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("Mode", mode());
        Tag.putLong("Token", Token);
        if (Bound != null) {
            Tag.putUUID("Bound", Bound);
        }
        Tag.putInt("PinTicks", PinTicks);
        Tag.putInt("LostTicks", LostTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        byte Saved = Tag.getByte("Mode");
        setMode(Saved == ModeContained ? ModeContained : ModeHunt);
        Token = Tag.contains("Token") ? Tag.getLong("Token") : -1L;
        Bound = Tag.hasUUID("Bound") ? Tag.getUUID("Bound") : null;
        PinTicks = Tag.getInt("PinTicks");
        LostTicks = Tag.getInt("LostTicks");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Scp280GaitController Body = new Scp280GaitController(this, "body", 5, Event -> {
            if (isDeadOrDying()) {
                return Event.setAndContinue(DissipatePlay);
            }
            switch (action()) {
                case ActionEmerge -> {
                    return Event.setAndContinue(EmergePlay);
                }
                case ActionRecoil -> {
                    return Event.setAndContinue(RecoilPlay);
                }
                case ActionWindup -> {
                    return Event.setAndContinue(WindupPlay);
                }
                case ActionStrike -> {
                    return Event.setAndContinue(StrikePlay);
                }
                default -> {
                }
            }
            if (isContained()) {
                return Event.setAndContinue(ContainedLoop);
            }
            if (isHuddled()) {
                return Event.setAndContinue(HuddledLoop);
            }
            if (isRunning()) {
                return Event.setAndContinue(isStriding() ? RunLoop : IdleLoop);
            }
            return Event.setAndContinue(isStriding() ? StalkLoop : IdleLoop);
        });
        BodyController = Body;
        Controllers.add(Body);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
