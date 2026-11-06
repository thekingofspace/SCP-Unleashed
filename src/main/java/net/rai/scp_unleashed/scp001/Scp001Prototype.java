package net.rai.scp_unleashed.scp001;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
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

public class Scp001Prototype extends Monster implements GeoEntity {
    public static final float Width = 0.95f;
    public static final float Height = 2.0f;
    public static final float EyeHeight = 1.7f;
    public static final float MaxHealth = 150.0f;
    public static final float Armour = 6.0f;
    public static final float StrikeDamage = 8.0f;
    public static final double MoveSpeed = 0.3;
    public static final double DashSpeed = 1.2;
    public static final double LungeSpeed = 1.35;
    public static final double CreepSpeed = 0.42;
    public static final double LungeRange = 6.0;
    public static final int CoilTicks = 6;
    public static final int TwitchOdds = 120;
    public static final int VoiceOdds = 420;
    public static final int HuntVoiceOdds = 300;
    public static final double HuntRange = 64.0;
    public static final int PinInterval = 10;
    public static final double SenseRange = 12.0;
    public static final double StrikeReach = 3.0;
    public static final double GuardReach = 3.0;
    public static final int StrikeTicks = 10;
    public static final int StrikeImpact = 5;
    public static final int StrikeCooldown = 16;
    public static final int DashMin = 8;
    public static final int DashMax = 14;
    public static final int FreezeMin = 4;
    public static final int FreezeMax = 10;
    public static final int WarpCooldownMin = 120;
    public static final int WarpCooldownMax = 180;
    public static final int WarpOutTicks = 8;
    public static final int WarpInTicks = 9;
    public static final double WarpChanceRange = 16.0;
    public static final int FlashCooldownMin = 300;
    public static final int FlashCooldownMax = 420;
    public static final int FirstFlashDelay = 160;
    public static final int SummonCooldownMin = 600;
    public static final int SummonCooldownMax = 900;
    public static final int FirstSummonDelay = 240;
    public static final double SummonMinRange = 6.0;
    public static final double SummonMaxRange = 28.0;
    public static final int SummonTicks = 36;
    public static final int SummonLead = 6;
    public static final int HushBeat = 6;
    public static final int OrbMin = 4;
    public static final int OrbMax = 7;
    public static final int LaunchGap = 5;
    public static final int CastTail = 8;
    public static final int CastTargets = 4;
    public static final int FlashWindup = 20;
    public static final int FlashTicks = 40;
    public static final double FlashMinRange = 4.0;
    public static final float EscapeShare = 0.2f;
    public static final float FloorShare = 0.1f;
    public static final int EscapeTicks = 28;
    public static final int EmergeTicks = 30;
    public static final int LockTicks = 180;
    public static final int AnchorHold = 30;
    public static final int AnchorFitEvery = 6;
    public static final float AnchorThrash = 70.0f;
    public static final int RestraintInterval = 10;
    public static final int RestraintGrip = 2;
    public static final int RestraintSlip = 60;
    public static final int LoadGrace = 100;
    public static final int RetargetInterval = 10;
    public static final int LostLimit = 100;
    public static final int StuckLimit = 30;
    public static final int DeathTicks = 16;
    public static final int HealInterval = 60;
    public static final double PaceRadius = 3.0;
    public static final float SwingStiffness = 0.22f;
    public static final float SwingDamping = 0.18f;
    public static final float SwingGain = 1.2f;
    public static final float SwingTurn = 0.6f;
    public static final float SwingLimit = 0.9f;
    public static final int ActionBlend = 2;
    public static final byte ActionNone = 0;
    public static final byte ActionStrike = 1;
    public static final byte ActionWarpOut = 2;
    public static final byte ActionWarpIn = 3;
    public static final byte ActionFlash = 4;
    public static final byte ActionEscape = 5;
    public static final byte ActionEmerge = 6;
    public static final byte ActionCoil = 7;
    public static final byte ActionSummon = 8;
    public static final byte ActionCast = 9;
    public static final int[] TwitchClickTicks = {1, 3, 5};
    public static final Vector3f Gold = new Vector3f(1.0f, 0.76f, 0.24f);
    public static final Vector3f Blue = new Vector3f(0.55f, 0.8f, 1.0f);

    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp001Prototype.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> LockedData = SynchedEntityData.defineId(Scp001Prototype.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RestrainedData = SynchedEntityData.defineId(Scp001Prototype.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HushData = SynchedEntityData.defineId(Scp001Prototype.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation ReelLoop = RawAnimation.begin().thenLoop("reel");
    private static final RawAnimation WarpOutPlay = RawAnimation.begin().thenPlayAndHold("warp_out");
    private static final RawAnimation WarpInPlay = RawAnimation.begin().thenPlayAndHold("warp_in");
    private static final RawAnimation FlashPlay = RawAnimation.begin().thenPlayAndHold("flash");
    private static final RawAnimation EscapePlay = RawAnimation.begin().thenPlayAndHold("escape");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation SlashLeft = RawAnimation.begin().thenPlay("slash_left");
    private static final RawAnimation SlashRight = RawAnimation.begin().thenPlay("slash_right");
    private static final RawAnimation SummonPlay = RawAnimation.begin().thenPlayAndHold("summon");
    private static final RawAnimation CastFlick = RawAnimation.begin().thenPlay("cast");
    private static final RawAnimation TwitchA = RawAnimation.begin().thenPlay("twitch_a");
    private static final RawAnimation TwitchB = RawAnimation.begin().thenPlay("twitch_b");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Scp001Murmur Murmur = new Scp001Murmur();
    private final Scp001Jitter Jitter = new Scp001Jitter();
    private int HushTicks;
    private Scp001Gait Gait = Scp001Gait.Idle;
    private double GaitPhase;
    private double GaitPhaseO;
    private double IdlePhase;
    private double IdlePhaseO;
    private double PaceSmooth;
    private int StillCount;
    private float BodyYawO;
    private int ActionTicks;
    private int LockLeft;
    private int StrikeTimer;
    private int WarpTimer = WarpCooldownMin / 2;
    private int SummonTimer = FirstSummonDelay;
    private int SummonCount;
    private int Launched;
    private final List<Scp001Singularity> Orbs = new ArrayList<>();
    private final List<LivingEntity> Marks = new ArrayList<>();
    private Vec3 LastAim = Vec3.ZERO;
    private int FlashTimer = FirstFlashDelay;
    private int RetargetTimer;
    private int LostTicks;
    private int StuckTicks;
    private int PhaseTicks;
    private boolean Dashing;
    private int ZigSign = 1;
    private boolean LeftSlash;
    private int RestraintTimer;
    private int GripCount;
    private int SlipTicks;
    private int GraceTicks;
    private int HealTimer;
    private boolean FreshRelease;
    private boolean Anchored;
    private boolean Pinned;
    private int PinCheckedAt = -PinInterval;
    @Nullable
    private Vec3 WarpSpot;
    @Nullable
    private BlockPos Home;
    @Nullable
    private BlockPos Footing;
    @Nullable
    private UUID PortalId;
    private Vec3 LastSpot = Vec3.ZERO;
    private float GlowLevel;
    private float GlowLevelO;
    private int ActionAge;
    private float SwingX;
    private float SwingZ;
    private float SwingXO;
    private float SwingZO;
    private float SwingVelX;
    private float SwingVelZ;
    private Vec3 VelocityO = Vec3.ZERO;
    private float YawO;
    @Nullable
    private AnimationController<Scp001Prototype> BodyController;

    public Scp001Prototype(EntityType<? extends Scp001Prototype> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.ARMOR, Armour).add(Attributes.MOVEMENT_SPEED, MoveSpeed)
                .add(Attributes.ATTACK_DAMAGE, StrikeDamage).add(Attributes.FOLLOW_RANGE, HuntRange).add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean refusesStasis(Entity Target) {
        return Target instanceof Scp001Prototype Creature && !Creature.isLocked();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(LockedData, false);
        Builder.define(RestrainedData, false);
        Builder.define(HushData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isLocked() {
        return entityData.get(LockedData);
    }

    public boolean isRestrained() {
        return entityData.get(RestrainedData);
    }

    public int lockLeft() {
        return LockLeft;
    }

    public int flashTimer() {
        return FlashTimer;
    }

    public int warpTimer() {
        return WarpTimer;
    }

    public boolean busy() {
        return action() != ActionNone;
    }

    public boolean hangsCord() {
        byte Current = action();
        return !isDeadOrDying() && (Current == ActionNone || Current == ActionStrike || Current == ActionCoil || Current == ActionSummon || Current == ActionCast);
    }

    public boolean casting() {
        byte Current = action();
        return isAlive() && !isLocked() && (Current == ActionSummon || Current == ActionCast);
    }

    public int summonTimer() {
        return SummonTimer;
    }

    public List<Scp001Singularity> orbs() {
        return List.copyOf(Orbs);
    }

    public boolean canSummon() {
        return isAlive() && !isLocked() && !isRestrained() && !escaping() && action() == ActionNone && level() instanceof ServerLevel World && AnomalyRealm.allows(World);
    }

    public boolean forceSummon(LivingEntity Target) {
        if (!(level() instanceof ServerLevel World) || !canSummon() || Target == null || !Target.isAlive()) {
            return false;
        }
        setTarget(Target);
        beginSummon(World, Target);
        return true;
    }

    public Vec3 orbitPoint(int Slot, int Count, double Time) {
        double Angle = Time * Scp001Singularity.OrbitSpin + Slot * Mth.TWO_PI / Math.max(1, Count);
        double Lift = Scp001Singularity.OrbitHeight + Scp001Singularity.OrbitBob * Math.sin(Angle * 2.0 + Slot);
        return position().add(Math.cos(Angle) * Scp001Singularity.OrbitRadius, Lift, Math.sin(Angle) * Scp001Singularity.OrbitRadius);
    }

    public boolean isHushed() {
        return entityData.get(HushData);
    }

    public boolean trembles() {
        byte Current = action();
        return Current == ActionCoil || Current == ActionFlash && ActionAge < FlashWindup || Current == ActionSummon && ActionAge > SummonTicks - HushBeat;
    }

    public Scp001Gait gait() {
        return Gait;
    }

    public Scp001Jitter jitter() {
        return Jitter;
    }

    public double gaitPhase(Scp001Gait Clip, float Partial) {
        if (Clip == Scp001Gait.Idle) {
            return Mth.lerp(Partial, IdlePhaseO, IdlePhase);
        }
        return Mth.lerp(Partial, GaitPhaseO, GaitPhase);
    }

    private void hush(int Ticks) {
        HushTicks = Math.max(HushTicks, Ticks);
        if (!isHushed()) {
            entityData.set(HushData, true);
        }
    }

    private void twitch(ServerLevel World) {
        triggerAnim("twitch", random.nextBoolean() ? "twitch_a" : "twitch_b");
        if (!isHushed()) {
            for (int Delay : TwitchClickTicks) {
                Murmur.schedule(this, Delay, Scp001Registry.ClickLayers, 0.5f, 1.0f + (random.nextFloat() - 0.5f) * 0.3f, false);
            }
        }
    }

    private void voice(ServerLevel World, float Volume) {
        boolean Low = random.nextInt(5) < 3;
        Murmur.stutter(this, Low ? Scp001Registry.LowVoiceLayers : Scp001Registry.HighVoiceLayers, 3 + random.nextInt(4), Volume, 1.0f, Low ? -0.03f : 0.04f, random);
    }

    private void setAction(byte Kind, int Ticks) {
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone && Kind != ActionStrike) {
            getNavigation().stop();
        }
    }

    public void beginEmerging() {
        setAction(ActionEmerge, EmergeTicks);
        SummonTimer = Math.max(SummonTimer, FirstSummonDelay);
    }

    public Vec3 orbPosition() {
        Vec3 Look = Vec3.directionFromRotation(0.0f, getYHeadRot());
        return getEyePosition().add(Look.scale(0.7)).add(0.0, -0.2, 0.0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (StrikeTimer > 0) {
            StrikeTimer--;
        }
        if (WarpTimer > 0) {
            WarpTimer--;
        }
        if (FlashTimer > 0) {
            FlashTimer--;
        }
        if (SummonTimer > 0) {
            SummonTimer--;
        }
        if (GraceTicks > 0) {
            GraceTicks--;
        }
        if (onGround()) {
            Footing = blockPosition();
        }
        if (HushTicks > 0 && --HushTicks == 0) {
            entityData.set(HushData, false);
        }
        Murmur.tick(this, isHushed());
        checkAnchor(World);
        if (isLocked()) {
            tickLock(World);
            return;
        }
        if (--RestraintTimer <= 0 || FreshRelease) {
            RestraintTimer = RestraintInterval;
            checkRestraint(World);
        }
        if (busy()) {
            tickAction(World);
            return;
        }
        if (isRestrained()) {
            tickRestrained(World);
            return;
        }
        if (!AnomalyRealm.allows(World)) {
            setTarget(null);
            return;
        }
        if (getHealth() <= getMaxHealth() * EscapeShare) {
            if (!pinned(World)) {
                beginEscape(World);
                return;
            }
            if (tickCount % 20 == 0) {
                fizzle(World);
            }
        }
        if (--RetargetTimer <= 0) {
            RetargetTimer = RetargetInterval;
            retarget(World);
        }
        LivingEntity Target = getTarget();
        setAggressive(Target != null);
        if (Target == null) {
            wander(World);
            return;
        }
        hunt(World, Target);
    }

    private void halt() {
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
        setSpeed(0.0f);
        setZza(0.0f);
        setXxa(0.0f);
    }

    private void checkAnchor(ServerLevel World) {
        Anchored = Scp1730Anchors.thrown(this);
        if (!Anchored) {
            return;
        }
        if (!isLocked()) {
            daze(AnchorHold);
        } else {
            LockLeft = Math.max(LockLeft, AnchorHold);
        }
    }

    public boolean anchored() {
        return Anchored;
    }

    private void tickLock(ServerLevel World) {
        halt();
        Vec3 Motion = getDeltaMovement();
        setDeltaMovement(0.0, Math.min(0.0, Motion.y), 0.0);
        if (Anchored) {
            if (tickCount % AnchorFitEvery == 0) {
                triggerAnim("twitch", random.nextBoolean() ? "twitch_a" : "twitch_b");
                float Yaw = getYRot() + (random.nextFloat() - 0.5f) * AnchorThrash;
                setYRot(Yaw);
                setYBodyRot(Yaw);
                setYHeadRot(Yaw + (random.nextFloat() - 0.5f) * AnchorThrash);
            }
            if (tickCount % 3 == 0) {
                World.sendParticles(Scp1730Registry.AnchorParticle.get(), getX(), getY() + 1.2, getZ(), 2, 0.5, 0.7, 0.5, 0.0);
            }
            if (tickCount % 16 == 0) {
                playLayers(World, Scp001Registry.ReelLayers, 0.8f, 0.9f + random.nextFloat() * 0.4f);
            }
        }
        if (tickCount % 4 == 0) {
            World.sendParticles(new DustParticleOptions(Blue, 0.9f), getX(), getY() + 1.7, getZ(), 2, 0.3, 0.25, 0.3, 0.0);
            World.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.6, getZ(), 1, 0.3, 0.3, 0.3, 0.05);
        }
        if (tickCount % 30 == 0) {
            playLayers(World, Scp001Registry.ReelLayers, 0.7f, 0.85f);
        }
        if (--LockLeft <= 0) {
            LockLeft = 0;
            entityData.set(LockedData, false);
            PhaseTicks = 0;
            if (!isRestrained() && getHealth() <= getMaxHealth() * EscapeShare && !pinned(World)) {
                beginEscape(World);
                return;
            }
            playLayers(World, Scp001Registry.ChitterLayers, 1.0f, 1.2f);
        }
    }

    public void daze(int Ticks) {
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        byte Current = action();
        if (Current == ActionEscape) {
            closePortal(World);
        }
        if (Current != ActionNone && Current != ActionEmerge) {
            setAction(ActionNone, 0);
        }
        WarpSpot = null;
        LockLeft = Math.max(LockLeft, Ticks);
        entityData.set(LockedData, true);
        halt();
        playLayers(World, Scp001Registry.ReelLayers, 1.2f, 1.0f);
        World.sendParticles(Scp1730Registry.AnchorParticle.get(), getX(), getY() + 1.2, getZ(), 24, 0.6, 0.8, 0.6, 0.02);
    }

    private void checkRestraint(ServerLevel World) {
        boolean Holds = Scp001Restraint.holds(World, position().add(0.0, 1.0, 0.0));
        boolean Fresh = FreshRelease;
        FreshRelease = false;
        if (Holds) {
            SlipTicks = 0;
            if (!isRestrained()) {
                GripCount++;
                if (Fresh || GripCount >= RestraintGrip) {
                    restrain(World);
                }
            }
            return;
        }
        GripCount = 0;
        if (!isRestrained() || GraceTicks > 0) {
            return;
        }
        SlipTicks += Fresh ? RestraintSlip : RestraintInterval;
        if (SlipTicks >= RestraintSlip) {
            breakFree(World);
        }
    }

    private void restrain(ServerLevel World) {
        entityData.set(RestrainedData, true);
        GripCount = 0;
        SlipTicks = 0;
        Home = blockPosition();
        setTarget(null);
        setAggressive(false);
        if (action() != ActionEmerge) {
            setAction(ActionNone, 0);
        }
        getNavigation().stop();
        playLayers(World, List.of(Scp001Registry.Settle, Scp001Registry.ChitterLow), 0.9f, 0.8f);
    }

    private void breakFree(ServerLevel World) {
        entityData.set(RestrainedData, false);
        SlipTicks = 0;
        Home = null;
        RetargetTimer = 0;
        PhaseTicks = 0;
        playLayers(World, Scp001Registry.ReelLayers, 1.3f, 1.25f);
        triggerAnim("twitch", "twitch_a");
    }

    public void leaveStasis(ServerLevel World) {
        LockLeft = 0;
        entityData.set(LockedData, false);
        setAction(ActionEmerge, EmergeTicks / 2);
        WarpSpot = null;
        PortalId = null;
        FreshRelease = true;
        RestraintTimer = 0;
        GraceTicks = 0;
        checkRestraint(World);
    }

    private void tickAction(ServerLevel World) {
        byte Current = action();
        ActionTicks--;
        switch (Current) {
            case ActionStrike -> {
                LivingEntity Target = getTarget();
                if (Target != null) {
                    getLookControl().setLookAt(Target, 90.0f, 90.0f);
                }
                if (ActionTicks == StrikeTicks - StrikeImpact) {
                    impact(World);
                }
            }
            case ActionCoil -> {
                halt();
                LivingEntity Target = getTarget();
                if (Target != null) {
                    getLookControl().setLookAt(Target, 90.0f, 90.0f);
                }
                if (ActionTicks <= 0) {
                    setAction(ActionNone, 0);
                    if (Target != null && Target.isAlive()) {
                        beginStrike(World, Target);
                    }
                    return;
                }
            }
            case ActionSummon -> {
                tickSummon(World);
                if (ActionTicks <= 0) {
                    beginCast(World);
                }
                return;
            }
            case ActionCast -> {
                tickCast(World);
                if (ActionTicks <= 0 && Launched >= Orbs.size()) {
                    finishCast();
                }
                return;
            }
            case ActionWarpOut -> {
                getNavigation().stop();
                if (tickCount % 2 == 0) {
                    World.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.0, getZ(), 3, 0.35, 0.7, 0.35, 0.08);
                }
                if (ActionTicks <= 0) {
                    finishWarp(World);
                    return;
                }
            }
            case ActionFlash -> {
                getNavigation().stop();
                LivingEntity Target = getTarget();
                if (Target != null) {
                    getLookControl().setLookAt(Target, 30.0f, 30.0f);
                }
                if (ActionTicks == FlashTicks - FlashWindup) {
                    Scp001Flash.burst(World, this);
                }
            }
            case ActionEscape -> {
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
                if (ActionTicks <= 0) {
                    if (Scp001Restraint.pins(World, position().add(0.0, 1.0, 0.0))) {
                        closePortal(World);
                        fizzle(World);
                        setAction(ActionNone, 0);
                        PhaseTicks = 0;
                        return;
                    }
                    Scp001Returns.get(World).schedule(World, this);
                    closePortal(World);
                    discard();
                    return;
                }
            }
            default -> getNavigation().stop();
        }
        if (ActionTicks <= 0) {
            setAction(ActionNone, 0);
            PhaseTicks = 0;
        }
    }

    private void retarget(ServerLevel World) {
        LivingEntity Current = getTarget();
        if (Current != null && Scp001Prey.prey(this, Current) && distanceTo(Current) <= HuntRange) {
            if (hasLineOfSight(Current) || distanceTo(Current) <= SenseRange) {
                LostTicks = 0;
            } else {
                LostTicks += RetargetInterval;
            }
            LivingEntity Better = Scp001Prey.best(World, this, HuntRange, SenseRange);
            if (LostTicks < LostLimit && (Better == null || Better == Current || !Scp001Prey.better(this, Better, Current))) {
                return;
            }
        }
        LostTicks = 0;
        LivingEntity Best = Scp001Prey.best(World, this, HuntRange, SenseRange);
        if (Best != Current) {
            setTarget(Best);
            if (Best != null) {
                playLayers(World, Scp001Registry.ChitterLayers, 1.0f, 1.15f);
                PhaseTicks = 0;
            }
        }
    }

    private void hunt(ServerLevel World, LivingEntity Target) {
        double Distance = distanceTo(Target);
        boolean Sees = hasLineOfSight(Target);
        if (Distance <= StrikeReach && Sees && StrikeTimer <= 0) {
            beginCoil(World, Target);
            return;
        }
        if (!isHushed() && random.nextInt(HuntVoiceOdds) == 0) {
            voice(World, 0.7f);
        }
        if (SummonTimer <= 0 && Sees && Distance >= SummonMinRange && Distance <= SummonMaxRange && canSummon()) {
            beginSummon(World, Target);
            return;
        }
        if (FlashTimer <= 0 && Distance >= FlashMinRange && Distance <= Scp001Flash.Range && Scp001Flash.anyWitness(World, this)) {
            beginFlash(World);
            return;
        }
        if (WarpTimer <= 0 && Distance <= WarpChanceRange && (StuckTicks >= StuckLimit || Distance > 6.0 && random.nextInt(14) == 0 || !Sees && random.nextInt(6) == 0)) {
            if (beginWarp(World, Target)) {
                return;
            }
            WarpTimer = 20;
        }
        stepHunt(World, Target, Distance);
    }

    private void stepHunt(ServerLevel World, LivingEntity Target, double Distance) {
        if (position().distanceToSqr(LastSpot) < 0.0025 && Dashing) {
            StuckTicks++;
        } else {
            StuckTicks = Math.max(0, StuckTicks - 1);
        }
        LastSpot = position();
        if (--PhaseTicks <= 0 || Dashing && getNavigation().isDone()) {
            if (Dashing) {
                Dashing = false;
                PhaseTicks = Mth.nextInt(random, FreezeMin, FreezeMax);
                getNavigation().stop();
                snapFacing(Target);
                if (Distance < LungeRange + 2.0) {
                    hush(PhaseTicks + 1);
                } else if (random.nextInt(3) == 0) {
                    twitch(World);
                }
            } else {
                Dashing = true;
                PhaseTicks = Mth.nextInt(random, DashMin, DashMax);
                dashToward(Target, Distance);
                if (Distance < LungeRange) {
                    HushTicks = 0;
                    entityData.set(HushData, false);
                    playLayers(World, Scp001Registry.LungeLayers, 1.3f, 1.0f, true);
                } else {
                    playLayers(World, Scp001Registry.SkitterLayers, 0.55f, 1.0f);
                }
            }
        }
        if (!Dashing) {
            Vec3 Motion = getDeltaMovement();
            setDeltaMovement(Motion.x * 0.3, Motion.y, Motion.z * 0.3);
            getLookControl().setLookAt(Target, 90.0f, 90.0f);
        }
    }

    private void dashToward(LivingEntity Target, double Distance) {
        if (Distance < LungeRange) {
            if (!getNavigation().moveTo(Target, LungeSpeed)) {
                getNavigation().stop();
            }
            return;
        }
        ZigSign = -ZigSign;
        double Angle = Math.toRadians((20.0 + random.nextDouble() * 30.0) * ZigSign);
        Vec3 Toward = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0).normalize();
        Vec3 Turned = new Vec3(Toward.x * Math.cos(Angle) - Toward.z * Math.sin(Angle), 0.0, Toward.x * Math.sin(Angle) + Toward.z * Math.cos(Angle));
        double Travel = Math.min(Distance - 1.5, 4.0 + random.nextDouble() * 3.0);
        Vec3 Waypoint = position().add(Turned.scale(Travel));
        if (!getNavigation().moveTo(Waypoint.x, Target.getY(), Waypoint.z, DashSpeed) && !getNavigation().moveTo(Target, DashSpeed)) {
            getNavigation().stop();
        }
    }

    private void snapFacing(Entity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
    }

    private void wander(ServerLevel World) {
        if (getNavigation().isDone() && random.nextInt(60) == 0) {
            Vec3 Spot = position().add((random.nextDouble() - 0.5) * 12.0, 0.0, (random.nextDouble() - 0.5) * 12.0);
            getNavigation().moveTo(Spot.x, getY(), Spot.z, CreepSpeed);
        }
        if (random.nextInt(TwitchOdds) == 0) {
            twitch(World);
        }
        if (random.nextInt(VoiceOdds) == 0) {
            voice(World, 0.45f);
        }
    }

    private void tickRestrained(ServerLevel World) {
        if (getHealth() < getMaxHealth() && ++HealTimer >= HealInterval) {
            HealTimer = 0;
            heal(1.0f);
        }
        LivingEntity Near = tickCount % 5 == 0 || getTarget() != null ? Scp001Prey.nearest(World, this, GuardReach) : null;
        if (Near != null) {
            setTarget(Near);
            getLookControl().setLookAt(Near, 90.0f, 90.0f);
            if (StrikeTimer <= 0) {
                beginCoil(World, Near);
                return;
            }
            if (distanceTo(Near) > StrikeReach * 0.8) {
                getNavigation().moveTo(Near, CreepSpeed * 1.6);
            }
            return;
        }
        if (getTarget() != null) {
            setTarget(null);
            getNavigation().stop();
        }
        setAggressive(false);
        if (getNavigation().isDone() && random.nextInt(50) == 0) {
            BlockPos Anchor = Home != null ? Home : blockPosition();
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = random.nextDouble() * PaceRadius;
            getNavigation().moveTo(Anchor.getX() + 0.5 + Math.cos(Angle) * Reach, Anchor.getY(), Anchor.getZ() + 0.5 + Math.sin(Angle) * Reach, CreepSpeed);
        }
        if (random.nextInt(90) == 0) {
            twitch(World);
        }
    }

    private void beginCoil(ServerLevel World, LivingEntity Target) {
        StrikeTimer = StrikeCooldown + StrikeTicks + CoilTicks;
        setAction(ActionCoil, CoilTicks);
        halt();
        snapFacing(Target);
        hush(CoilTicks + 1);
    }

    private void beginStrike(ServerLevel World, LivingEntity Target) {
        StrikeTimer = Math.max(StrikeTimer, StrikeCooldown + StrikeTicks);
        setAction(ActionStrike, StrikeTicks);
        snapFacing(Target);
        LeftSlash = !LeftSlash;
        HushTicks = 0;
        entityData.set(HushData, false);
        triggerAnim("attack", LeftSlash ? "slash_left" : "slash_right");
        playLayers(World, Scp001Registry.StrikeLayers, 1.5f, 1.0f, true);
        playLayers(World, Scp001Registry.SlashLayers, 1.0f, 1.0f, true);
    }

    private void impact(ServerLevel World) {
        LivingEntity Target = getTarget();
        if (Target == null || !Target.isAlive() || distanceTo(Target) > StrikeReach + 0.6 || !hasLineOfSight(Target)) {
            return;
        }
        Target.hurt(damageSources().source(Scp001Registry.Claw, this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        Vec3 Push = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (Push.lengthSqr() > 1.0E-4) {
            Push = Push.normalize();
            Target.knockback(0.3, -Push.x, -Push.z);
        }
        if (!isRestrained() && random.nextInt(5) < 2) {
            Vec3 Side = new Vec3(-Push.z, 0.0, Push.x).scale(random.nextBoolean() ? 0.45 : -0.45).add(Push.scale(-0.25));
            setDeltaMovement(getDeltaMovement().add(Side.x, 0.12, Side.z));
            Dashing = false;
            PhaseTicks = Mth.nextInt(random, FreezeMin, FreezeMax);
        }
    }

    private boolean beginWarp(ServerLevel World, LivingEntity Target) {
        if (pinned(World)) {
            return false;
        }
        Optional<Vec3> Spot = Scp001Warp.find(World, this, Target, random);
        if (Spot.isEmpty() || Scp001Restraint.pins(World, Spot.get().add(0.0, 1.0, 0.0))) {
            return false;
        }
        WarpSpot = Spot.get();
        WarpTimer = Mth.nextInt(random, WarpCooldownMin, WarpCooldownMax);
        setAction(ActionWarpOut, WarpOutTicks);
        hush(WarpOutTicks + 1);
        playLayers(World, Scp001Registry.WarpTickLayers, 0.35f, 1.0f, true);
        return true;
    }

    private void finishWarp(ServerLevel World) {
        Vec3 From = position();
        Vec3 Spot = WarpSpot;
        WarpSpot = null;
        boolean Held = Spot != null && (Scp001Restraint.pins(World, From.add(0.0, 1.0, 0.0)) || Scp001Restraint.pins(World, Spot.add(0.0, 1.0, 0.0)));
        if (Held) {
            fizzle(World);
        }
        if (Spot != null && !Held && Scp001Warp.fits(World, this, BlockPos.containing(Spot))) {
            moveTo(Spot.x, Spot.y, Spot.z, getYRot(), getXRot());
            getNavigation().stop();
            LivingEntity Target = getTarget();
            if (Target != null) {
                snapFacing(Target);
            }
            Scp001Warp.trail(World, From, position());
            HushTicks = 0;
            entityData.set(HushData, false);
            playLayers(World, Scp001Registry.WarpLayers, 1.5f, 1.0f, true);
        }
        setAction(ActionWarpIn, WarpInTicks);
        StuckTicks = 0;
        Dashing = false;
        PhaseTicks = WarpInTicks + Mth.nextInt(random, 2, 6);
    }

    private boolean pinned(ServerLevel World) {
        if (tickCount - PinCheckedAt >= PinInterval || tickCount < PinCheckedAt) {
            PinCheckedAt = tickCount;
            Pinned = Scp001Restraint.pins(World, position().add(0.0, 1.0, 0.0));
        }
        return Pinned;
    }

    public boolean isPinned() {
        return level() instanceof ServerLevel World && pinned(World);
    }

    private void fizzle(ServerLevel World) {
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.2, getZ(), 6, 0.4, 0.5, 0.4, 0.08);
        World.sendParticles(new DustParticleOptions(Gold, 0.8f), getX(), getY() + 1.2, getZ(), 4, 0.35, 0.45, 0.35, 0.0);
    }

    public boolean warpTo(Vec3 Spot) {
        if (!(level() instanceof ServerLevel World) || !Scp001Warp.fits(World, this, BlockPos.containing(Spot))) {
            return false;
        }
        WarpSpot = Spot;
        finishWarp(World);
        return true;
    }

    private void beginSummon(ServerLevel World, LivingEntity Target) {
        SummonCount = Mth.nextInt(random, OrbMin, OrbMax);
        SummonTimer = Mth.nextInt(random, SummonCooldownMin, SummonCooldownMax);
        Orbs.clear();
        Marks.clear();
        Launched = 0;
        LastAim = Target.getBoundingBox().getCenter();
        setAction(ActionSummon, SummonTicks);
        halt();
        snapFacing(Target);
        playLayers(World, Scp001Registry.SingularityChargeLayers, 1.8f, 1.0f, true);
    }

    private int formGap() {
        return Math.max(3, (SummonTicks - SummonLead - HushBeat - 3) / Math.max(1, SummonCount));
    }

    private void tickSummon(ServerLevel World) {
        halt();
        LivingEntity Target = getTarget();
        if (Target != null && Target.isAlive()) {
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
            LastAim = Target.getBoundingBox().getCenter();
        }
        int Elapsed = SummonTicks - ActionTicks;
        int Formed = Elapsed < SummonLead ? 0 : Math.min(SummonCount, (Elapsed - SummonLead) / formGap() + 1);
        while (Orbs.size() < Formed) {
            Orbs.add(Scp001Singularity.form(World, this, Orbs.size(), SummonCount));
        }
        if (Elapsed > 0 && Elapsed % 10 == 0 && ActionTicks > HushBeat) {
            playLayers(World, Scp001Registry.SingularityHumLayers, 1.2f, 0.8f + Orbs.size() * 0.05f, true);
        }
        if (ActionTicks == HushBeat) {
            hush(HushBeat + 1);
        }
    }

    private void beginCast(ServerLevel World) {
        HushTicks = 0;
        entityData.set(HushData, false);
        Marks.clear();
        Marks.addAll(castTargets(World));
        Launched = 0;
        setAction(ActionCast, Orbs.size() * LaunchGap + CastTail);
    }

    private List<LivingEntity> castTargets(ServerLevel World) {
        List<LivingEntity> Found = new ArrayList<>();
        LivingEntity Main = getTarget();
        if (Main != null && Main.isAlive() && Scp001Prey.prey(this, Main) && hasLineOfSight(Main)) {
            Found.add(Main);
        }
        List<LivingEntity> Nearby = World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SummonMaxRange),
                Candidate -> Candidate != Main && Scp001Prey.prey(this, Candidate) && distanceTo(Candidate) <= SummonMaxRange && hasLineOfSight(Candidate));
        Nearby.sort((First, Second) -> Double.compare(distanceToSqr(First), distanceToSqr(Second)));
        for (LivingEntity Candidate : Nearby) {
            if (Found.size() >= CastTargets) {
                break;
            }
            Found.add(Candidate);
        }
        return Found;
    }

    private void tickCast(ServerLevel World) {
        halt();
        LivingEntity Main = Marks.isEmpty() ? getTarget() : Marks.get(0);
        if (Main != null && Main.isAlive()) {
            getLookControl().setLookAt(Main, 60.0f, 60.0f);
        }
        int Elapsed = Orbs.size() * LaunchGap + CastTail - ActionTicks;
        if (Launched >= Orbs.size() || Elapsed < Launched * LaunchGap) {
            return;
        }
        Scp001Singularity Orb = Orbs.get(Launched);
        LivingEntity Mark = Launched < Marks.size() ? Marks.get(Launched) : Main;
        if (Mark != null && Mark.isAlive()) {
            LastAim = Mark.getBoundingBox().getCenter();
        }
        Launched++;
        if (Orb.isRemoved() || !Orb.waiting()) {
            return;
        }
        Orb.launch(LastAim);
        triggerAnim("attack", "cast");
    }

    private void finishCast() {
        setAction(ActionNone, 0);
        Orbs.clear();
        Marks.clear();
        PhaseTicks = 0;
    }

    private void beginFlash(ServerLevel World) {
        FlashTimer = Mth.nextInt(random, FlashCooldownMin, FlashCooldownMax);
        setAction(ActionFlash, FlashTicks);
        playLayers(World, Scp001Registry.ChargeLayers, 1.4f, 1.0f, true);
        for (int Step = 0; Step < 5; Step++) {
            Murmur.schedule(this, Step * FlashWindup / 5, Scp001Registry.StrainLayers, 0.9f + Step * 0.12f, 0.6f + Step * 0.22f, true);
        }
    }

    public void forceFlash() {
        if (level() instanceof ServerLevel World && !isLocked()) {
            beginFlash(World);
        }
    }

    private void beginEscape(ServerLevel World) {
        setAction(ActionEscape, EscapeTicks);
        setTarget(null);
        Vec3 Back = Vec3.directionFromRotation(0.0f, getYRot()).scale(-1.4);
        Scp001Portal Portal = Scp001Portal.open(World, position().add(Back), getYRot(), EscapeTicks + 14);
        PortalId = Portal == null ? null : Portal.getUUID();
        playLayers(World, Scp001Registry.ReelLayers, 1.0f, 0.75f);
    }

    public void forceEscape() {
        if (level() instanceof ServerLevel World && !isLocked()) {
            beginEscape(World);
        }
    }

    private void closePortal(ServerLevel World) {
        if (PortalId != null && World.getEntity(PortalId) instanceof Scp001Portal Portal) {
            Portal.close();
        }
        PortalId = null;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World && isAlive() && !isLocked() && !isRestrained() && Source.getEntity() instanceof LivingEntity Attacker
                && Scp001Prey.prey(this, Attacker)) {
            LivingEntity Current = getTarget();
            if (Current == null || Current != Attacker && (!Scp001Prey.prey(this, Current) || Scp001Prey.rank(Attacker) < Scp001Prey.rank(Current)
                    || Scp001Prey.rank(Attacker) == Scp001Prey.rank(Current) && distanceTo(Attacker) < distanceTo(Current))) {
                setTarget(Attacker);
            }
            if (!busy() && WarpTimer <= 0 && random.nextInt(3) == 0 && getHealth() > getMaxHealth() * EscapeShare) {
                beginWarp(World, Attacker);
            }
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World && !isRemoved() && !dead) {
            closePortal(World);
        }
        super.die(Source);
    }

    public static boolean finishes(DamageSource Source) {
        return Source.is(DamageTypes.GENERIC_KILL);
    }

    public float floorHealth() {
        return getMaxHealth() * FloorShare;
    }

    public boolean escaping() {
        return action() == ActionEscape;
    }

    public void holdFloor() {
        setHealth(Math.max(getHealth(), floorHealth()));
        if (level() instanceof ServerLevel World && !isLocked() && !isRestrained() && !escaping() && !pinned(World)) {
            beginEscape(World);
        }
    }

    @Override
    protected void actuallyHurt(DamageSource Source, float Amount) {
        super.actuallyHurt(Source, Amount);
        if (!finishes(Source) && !isRemoved() && getHealth() <= floorHealth()) {
            holdFloor();
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return escaping() && !finishes(Source) || super.isInvulnerableTo(Source);
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
        if (!escaping()) {
            super.knockback(Strength, X, Z);
        }
    }

    @Override
    protected void onBelowWorld() {
        if (level() instanceof ServerLevel World && isAlive() && !isRemoved()) {
            BlockPos Origin = Footing != null ? Footing : BlockPos.containing(getX(), World.getMinBuildHeight() + 1, getZ());
            Scp001Returns.get(World).schedule(World, this, Origin);
            Vec3 Seam = new Vec3(getX(), Math.max(getY(), World.getMinBuildHeight() + 1.0), getZ());
            World.sendParticles(new DustParticleOptions(Gold, 1.2f), Seam.x, Seam.y, Seam.z, 16, 0.4, 0.8, 0.4, 0.0);
            World.playSound(null, Seam.x, Seam.y, Seam.z, Scp001Registry.PortalClose.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
            closePortal(World);
            discard();
            return;
        }
        super.onBelowWorld();
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel World) {
            if (deathTime % 3 == 0) {
                World.sendParticles(new DustParticleOptions(Gold, 1.1f), getX(), getY() + 1.0, getZ(), 6, 0.4, 0.6, 0.4, 0.0);
            }
            if (deathTime == 2) {
                playLayers(World, Scp001Registry.DissolveLayers, 1.1f, 1.0f);
            }
            if (deathTime >= DeathTicks) {
                World.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1.0, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                World.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.0, getZ(), 24, 0.4, 0.7, 0.4, 0.06);
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    private void playLayers(ServerLevel World, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float PitchBase) {
        playLayers(World, Layers, Volume, PitchBase, false);
    }

    private void playLayers(ServerLevel World, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float PitchBase, boolean Forced) {
        float Pitch = PitchBase * (0.94f + random.nextFloat() * 0.12f);
        Murmur.play(this, Layers, Volume, Pitch, Forced);
    }

    @Override
    public void playAmbientSound() {
        if (!(level() instanceof ServerLevel World) || isLocked() || isHushed() || random.nextInt(3) == 0) {
            return;
        }
        float Quiet = isRestrained() ? 0.6f : 0.8f;
        switch (random.nextInt(6)) {
            case 0 -> playLayers(World, Scp001Registry.BreathLayers, 0.5f * Quiet, 0.9f + random.nextFloat() * 0.2f);
            case 1 -> playLayers(World, Scp001Registry.DriftLayers, 0.45f * Quiet, 1.0f);
            case 2 -> voice(World, 0.5f * Quiet);
            case 3 -> Murmur.stutter(this, Scp001Registry.ClickLayers, 3 + random.nextInt(4), 0.35f * Quiet, 1.0f, 0.05f, random);
            default -> playLayers(World, Scp001Registry.ChitterLayers, 0.55f * Quiet, isRestrained() ? 0.85f : 1.0f);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp001Registry.Chitter.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return isRestrained() ? 260 : 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp001Registry.Hurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            playLayers(World, Scp001Registry.HurtLayers, getSoundVolume(), 1.0f);
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp001Registry.Dissolve.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
        if (level() instanceof ServerLevel World && !isHushed() && getDeltaMovement().horizontalDistanceSqr() > 0.04 && random.nextInt(3) == 0) {
            playLayers(World, Scp001Registry.SkitterLayers, 0.3f, 1.1f + random.nextFloat() * 0.3f);
        }
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
        return escaping();
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
    public boolean isPushable() {
        return !isLocked() && !escaping() && super.isPushable();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
        }
    }

    private void clientTick() {
        GlowLevelO = GlowLevel;
        byte Current = action();
        ActionAge = Current == ActionNone ? 0 : ActionAge + 1;
        float Goal = 0.55f + 0.12f * Mth.sin(tickCount * 0.21f);
        if (Current == ActionFlash) {
            Goal = ActionAge < FlashWindup ? 0.6f + 0.4f * ActionAge / FlashWindup : ActionAge < FlashWindup + 6 ? 1.0f : 0.8f;
        }
        if (Current == ActionSummon || Current == ActionCast) {
            Goal = 0.85f + 0.15f * Mth.sin(tickCount * 0.9f);
        }
        if (isLocked()) {
            Goal = 0.3f + 0.3f * (tickCount % 6 < 3 ? 1.0f : 0.0f);
        }
        if (deathTime > 0) {
            Goal = 0.0f;
        }
        GlowLevel += (Goal - GlowLevel) * 0.5f;
        swingTick();
        gaitTick();
        Murmur.tick(this, isHushed());
        if (Current == ActionFlash && ActionAge < FlashWindup && random.nextInt(2) == 0) {
            Vec3 Orb = orbPosition();
            level().addParticle(new DustParticleOptions(Blue, 0.6f), Orb.x + (random.nextDouble() - 0.5) * 0.6, Orb.y + (random.nextDouble() - 0.5) * 0.6,
                    Orb.z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.0, 0.0);
        }
        if (Current == ActionFlash && ActionAge == FlashWindup) {
            Vec3 Orb = orbPosition();
            level().addParticle(ParticleTypes.FLASH, Orb.x, Orb.y, Orb.z, 0.0, 0.0, 0.0);
        }
    }

    private void swingTick() {
        SwingXO = SwingX;
        SwingZO = SwingZ;
        Vec3 Velocity = new Vec3(getX() - xo, getY() - yo, getZ() - zo);
        Vec3 Push = Velocity.subtract(VelocityO);
        VelocityO = Velocity;
        float Yaw = yBodyRot * Mth.DEG_TO_RAD;
        float Forward = (float) (-Push.x * Mth.sin(Yaw) + Push.z * Mth.cos(Yaw));
        float Right = (float) -(Push.x * Mth.cos(Yaw) + Push.z * Mth.sin(Yaw));
        float Turn = Mth.wrapDegrees(yBodyRot - YawO) * Mth.DEG_TO_RAD;
        YawO = yBodyRot;
        SwingVelX += -SwingX * SwingStiffness - SwingVelX * SwingDamping - Forward * SwingGain;
        SwingVelZ += -SwingZ * SwingStiffness - SwingVelZ * SwingDamping - Right * SwingGain - Turn * SwingTurn;
        SwingX = Mth.clamp(SwingX + SwingVelX, -SwingLimit, SwingLimit);
        SwingZ = Mth.clamp(SwingZ + SwingVelZ, -SwingLimit, SwingLimit);
    }

    private void gaitTick() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        double Moved = Math.sqrt(Dx * Dx + Dz * Dz);
        double Turn = Math.abs(Mth.wrapDegrees(yBodyRot - BodyYawO)) * Mth.DEG_TO_RAD * Scp001Gait.TurnRadius;
        BodyYawO = yBodyRot;
        double Pace = Moved + Math.min(Turn, 0.3);
        PaceSmooth = PaceSmooth * 0.4 + Pace * 0.6;
        StillCount = PaceSmooth < Scp001Gait.StillPace ? StillCount + 1 : 0;
        Gait = Scp001Gait.choose(Gait, PaceSmooth, StillCount);
        GaitPhaseO = GaitPhase;
        IdlePhaseO = IdlePhase;
        double From;
        double To;
        if (Gait.moving()) {
            From = GaitPhase;
            GaitPhase += Math.min(Pace / Gait.distance(), 0.45);
            To = GaitPhase;
        } else {
            From = IdlePhase;
            IdlePhase += 1.0 / (Scp001Gait.IdleSeconds * 20.0);
            To = IdlePhase;
        }
        boolean Casting = action() == ActionSummon || action() == ActionCast;
        boolean Quiet = isHushed() || isLocked() || isDeadOrDying() || action() != ActionNone && action() != ActionStrike;
        int Odds = switch (Gait) {
            case Idle -> 45;
            case Creep -> 55;
            case Walk -> 70;
            case Gallop -> 110;
        };
        int Twitched = Jitter.tick(random, trembles(), Gait, GaitPhase, Casting ? 30 : Quiet || isRestrained() && random.nextBoolean() ? 0 : Odds);
        if (Quiet) {
            if (Casting && !isHushed() && Twitched == Scp001Jitter.Snapped) {
                Murmur.play(this, Scp001Registry.SnapLayers, 0.5f, 0.85f + random.nextFloat() * 0.35f, false);
            }
            return;
        }
        float Loud = Gait == Scp001Gait.Gallop ? 0.85f : Gait == Scp001Gait.Walk ? 0.65f : 0.45f;
        float[] Touch = Gait.touchdowns();
        for (int Limb = 0; Limb < Touch.length; Limb++) {
            if (Scp001Gait.crossed(From, To, Touch[Limb])) {
                boolean Arm = Limb == Scp001Gait.ArmLeft || Limb == Scp001Gait.ArmRight;
                Murmur.play(this, Arm ? Scp001Registry.KnockLayers : Scp001Registry.TapLayers, Arm ? Loud : Loud * 0.6f, 0.85f + random.nextFloat() * 0.3f, false);
            }
        }
        for (float Mark : Gait.knocks()) {
            if (Scp001Gait.crossed(From, To, Mark)) {
                Murmur.play(this, Scp001Registry.KnockLayers, 0.55f, 0.8f + random.nextFloat() * 0.2f, false);
            }
        }
        for (float Mark : Gait.taps()) {
            if (Scp001Gait.crossed(From, To, Mark)) {
                Murmur.play(this, Scp001Registry.TapLayers, 0.35f, 1.0f + random.nextFloat() * 0.2f, false);
            }
        }
        for (float Mark : Gait.clicks()) {
            if (Scp001Gait.crossed(From, To, Mark)) {
                Murmur.play(this, Scp001Registry.SnapLayers, 0.6f, 0.9f + random.nextFloat() * 0.3f, false);
            }
        }
        for (float Mark : Gait.bursts()) {
            if (Scp001Gait.crossed(From, To, Mark)) {
                Murmur.stutter(this, Scp001Registry.ClickLayers, 3 + random.nextInt(4), 0.5f, 1.0f, 0.06f, random);
            }
        }
        if (Twitched == Scp001Jitter.Snapped) {
            Murmur.play(this, Scp001Registry.SnapLayers, 0.55f, 0.85f + random.nextFloat() * 0.35f, false);
        } else if (Twitched == Scp001Jitter.Released) {
            Murmur.play(this, Scp001Registry.SnapLayers, 0.35f, 1.2f + random.nextFloat() * 0.2f, false);
        } else if (Twitched == Scp001Jitter.Burst) {
            Murmur.stutter(this, Scp001Registry.ClickLayers, 3 + random.nextInt(4), 0.45f, 1.05f, 0.05f, random);
        }
    }

    public float swingX(float Partial) {
        return Mth.lerp(Partial, SwingXO, SwingX);
    }

    public float swingZ(float Partial) {
        return Mth.lerp(Partial, SwingZO, SwingZ);
    }

    public float glow(float Partial) {
        return Mth.lerp(Partial, GlowLevelO, GlowLevel);
    }


    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (Key.equals(ActionData) && level().isClientSide && BodyController != null && action() != ActionNone && action() != ActionStrike && action() != ActionCast) {
            BodyController.forceAnimationReset();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("Restrained", isRestrained());
        Tag.putInt("Slip", SlipTicks);
        Tag.putInt("Locked", isLocked() ? LockLeft : 0);
        Tag.putInt("WarpTimer", WarpTimer);
        Tag.putInt("FlashTimer", FlashTimer);
        Tag.putInt("SummonTimer", SummonTimer);
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
        if (action() == ActionEmerge) {
            Tag.putInt("Emerge", ActionTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        boolean Restrained = Tag.getBoolean("Restrained");
        entityData.set(RestrainedData, Restrained);
        GraceTicks = Restrained ? LoadGrace : 0;
        SlipTicks = Tag.getInt("Slip");
        LockLeft = Tag.getInt("Locked");
        entityData.set(LockedData, LockLeft > 0);
        WarpTimer = Tag.getInt("WarpTimer");
        FlashTimer = Tag.contains("FlashTimer") ? Tag.getInt("FlashTimer") : FirstFlashDelay;
        SummonTimer = Tag.contains("SummonTimer") ? Tag.getInt("SummonTimer") : FirstSummonDelay;
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
        if (Tag.getInt("Emerge") > 0) {
            setAction(ActionEmerge, Tag.getInt("Emerge"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        AnimationController<Scp001Prototype> Body = new Scp001GaitController(this, "body", Event -> {
            Event.getController().transitionLength(ActionBlend);
            if (deathTime > 0 || isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            if (isLocked()) {
                return Event.setAndContinue(ReelLoop);
            }
            switch (action()) {
                case ActionWarpOut -> {
                    return Event.setAndContinue(WarpOutPlay);
                }
                case ActionWarpIn -> {
                    return Event.setAndContinue(WarpInPlay);
                }
                case ActionFlash -> {
                    return Event.setAndContinue(FlashPlay);
                }
                case ActionEscape -> {
                    return Event.setAndContinue(EscapePlay);
                }
                case ActionEmerge -> {
                    return Event.setAndContinue(EmergePlay);
                }
                case ActionSummon, ActionCast -> {
                    return Event.setAndContinue(SummonPlay);
                }
                default -> {
                }
            }
            Event.getController().transitionLength(0);
            return Event.setAndContinue(Gait.loop());
        });
        BodyController = Body;
        Controllers.add(Body);
        Controllers.add(new AnimationController<>(this, "attack", 1, Event -> PlayState.STOP).triggerableAnim("slash_left", SlashLeft).triggerableAnim("slash_right", SlashRight)
                .triggerableAnim("cast", CastFlick));
        Controllers.add(new AnimationController<>(this, "twitch", 1, Event -> PlayState.STOP).triggerableAnim("twitch_a", TwitchA).triggerableAnim("twitch_b", TwitchB));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
