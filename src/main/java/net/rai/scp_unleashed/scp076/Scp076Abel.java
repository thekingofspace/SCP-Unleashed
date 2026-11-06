package net.rai.scp_unleashed.scp076;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

public class Scp076Abel extends DClassLike {
    public static final float Width = 0.6f;
    public static final float Height = 1.95f;
    public static final float EyeHeight = 1.74f;
    public static final double MaxHealth = 300.0;
    public static final double Armour = 10.0;
    public static final double Toughness = 4.0;
    public static final double Steadiness = 0.75;
    public static final double Speed = 0.3;
    public static final double Follow = 64.0;
    public static final double Stride = 1.0;
    public static final double ChaseSpeed = 1.15;
    public static final double ReturnSpeed = 0.85;
    public static final double ProwlSpeed = 0.6;
    public static final double MeleeCreep = 0.55;
    public static final float SlashDamage = 6.0f;
    public static final float StabDamage = 9.0f;
    public static final double MeleeStart = 2.6;
    public static final double MeleeReach = 3.1;
    public static final int MeleeCooldown = 16;
    public static final float KnifeDamage = 7.0f;
    public static final float KnifeSpeed = 1.6f;
    public static final double ThrowMin = 4.5;
    public static final double ThrowMax = 20.0;
    public static final int ThrowCooldown = 36;
    public static final int ThrowJitter = 16;
    public static final float BladeDamage = 8.0f;
    public static final double SummonMin = 11.0;
    public static final double SummonMax = 30.0;
    public static final double SummonPrefer = 14.0;
    public static final int SummonCooldown = 90;
    public static final int SummonJitter = 30;
    public static final int BladeCount = 5;
    public static final double BladeSpread = 1.7;
    public static final int BladeLead = 14;
    public static final double BladeLeadLimit = 4.0;
    public static final int RiseTicks = 44;
    public static final int SinkTicks = 36;
    public static final int DeathTicks = 32;
    public static final int IdleLimit = 1200;
    public static final int ReturnLimit = 2400;
    public static final int StuckLimit = 400;
    public static final double ReturnLeash = 160.0;
    public static final double HomeReach = 2.2;
    public static final int LoseSight = 600;
    public static final double LoseRange = 96.0;
    public static final double SenseRange = 12.0;
    public static final int ScanEvery = 10;
    public static final int RegenDelay = 200;
    public static final int RegenEvery = 40;
    public static final float RegenAmount = 2.0f;
    public static final int Experience = 50;
    public static final byte AttackNone = 0;
    public static final byte AttackSlashA = 1;
    public static final byte AttackSlashB = 2;
    public static final byte AttackStab = 3;
    public static final byte AttackThrow = 4;
    public static final byte AttackSummon = 5;
    public static final int[] AttackLength = {0, 10, 10, 13, 15, 18};
    public static final int[] AttackHit = {0, 5, 5, 7, 7, 10};
    public static final String[] AttackClips = {"", "slash_a", "slash_b", "stab", "throw", "summon"};
    public static final byte PhaseHunting = 0;
    public static final byte PhaseRising = 1;
    public static final byte PhaseSinking = 2;
    public static final byte PhaseReturning = 3;
    public static final RawAnimation RisePlay = RawAnimation.begin().thenPlayAndHold("rise");
    public static final RawAnimation SinkPlay = RawAnimation.begin().thenPlayAndHold("sink");
    public static final RawAnimation CrumblePlay = RawAnimation.begin().thenPlayAndHold("crumble");
    public static final RawAnimation[] AttackPlays = {null, RawAnimation.begin().thenPlayAndHold("slash_a"), RawAnimation.begin().thenPlayAndHold("slash_b"),
            RawAnimation.begin().thenPlayAndHold("stab"), RawAnimation.begin().thenPlayAndHold("throw"), RawAnimation.begin().thenPlayAndHold("summon")};
    public static final int Appearance = DClassAppearance.pack(0, DClassAppearance.Bald, 0, 0, 0, 0, 0, DClassAppearance.Jumpsuit);
    protected static final EntityDataAccessor<Byte> AttackData = SynchedEntityData.defineId(Scp076Abel.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> SerialData = SynchedEntityData.defineId(Scp076Abel.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Byte> PhaseData = SynchedEntityData.defineId(Scp076Abel.class, EntityDataSerializers.BYTE);

    @Nullable
    private ResourceKey<Level> HomeDimension;
    @Nullable
    private BlockPos HomeDoor;
    @Nullable
    private BlockPos HomeCoffin;
    private Direction HomeFront = Direction.SOUTH;
    private int AttackStart;
    private int PhaseStart;
    private int ComboStep;
    private int MeleeWait;
    private int ThrowWait = 20;
    private int SummonWait = 40;
    private int IdleTicks;
    private int ReturnTicks;
    private int StuckTicks;
    private double BestDistance = Double.MAX_VALUE;
    private int LastSeen;
    private int LastHurt = -10000;
    private boolean HeavyStrike;
    private int TrackedId = -1;
    @Nullable
    private Vec3 TrackedPos;
    private Vec3 TrackedVelocity = Vec3.ZERO;
    private boolean Departed;
    private int LocalAttackStart;
    private int LocalPhaseStart;
    private float RenderPartial;
    @Nullable
    private Scp076AbelController AttackController;

    public Scp076Abel(EntityType<? extends Scp076Abel> Type, Level World) {
        super(Type, World);
        setAppearance(Appearance);
        xpReward = Experience;
        if (getNavigation() instanceof GroundPathNavigation Ground) {
            Ground.setCanOpenDoors(true);
            Ground.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.ARMOR, Armour)
                .add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.KNOCKBACK_RESISTANCE, Steadiness)
                .add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.ATTACK_DAMAGE, SlashDamage)
                .add(Attributes.STEP_HEIGHT, Stride)
                .add(Attributes.SAFE_FALL_DISTANCE, 6.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(AttackData, AttackNone);
        Builder.define(SerialData, 0);
        Builder.define(PhaseData, PhaseHunting);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp076AbelGoals.Hunt(this));
        goalSelector.addGoal(2, new Scp076AbelGoals.ReturnHome(this));
        goalSelector.addGoal(5, new Scp076AbelGoals.Prowl(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setAppearance(Appearance);
        return Result;
    }

    public void setHome(ResourceKey<Level> Dimension, BlockPos Door, @Nullable BlockPos Coffin, Direction Front) {
        HomeDimension = Dimension;
        HomeDoor = Door.immutable();
        HomeCoffin = Coffin == null ? null : Coffin.immutable();
        HomeFront = Front;
    }

    public boolean hasHome() {
        return HomeDoor != null && HomeDimension != null;
    }

    public byte attack() {
        return entityData.get(AttackData);
    }

    public byte phase() {
        return entityData.get(PhaseData);
    }

    public boolean busy() {
        byte Current = phase();
        return Current == PhaseRising || Current == PhaseSinking;
    }

    public boolean returning() {
        return phase() == PhaseReturning;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public double attackElapsed() {
        if (!level().isClientSide) {
            return tickCount - AttackStart;
        }
        return Math.max(0.0, tickCount - LocalAttackStart + RenderPartial);
    }

    public double phaseElapsed() {
        if (!level().isClientSide) {
            return tickCount - PhaseStart;
        }
        return Math.max(0.0, tickCount - LocalPhaseStart + RenderPartial);
    }

    @Nullable
    public String attackClip() {
        byte Current = attack();
        return Current > AttackNone && Current < AttackClips.length ? AttackClips[Current] : null;
    }

    public boolean knifeInHand() {
        return attack() == AttackThrow && attackElapsed() < AttackHit[AttackThrow];
    }

    private void setPhase(byte Value) {
        PhaseStart = tickCount;
        entityData.set(PhaseData, Value);
        boolean Locked = Value == PhaseRising || Value == PhaseSinking;
        for (Goal.Flag Flag : new Goal.Flag[]{Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP}) {
            goalSelector.setControlFlag(Flag, !Locked);
        }
        if (Locked) {
            getNavigation().stop();
        }
    }

    public void beginRise() {
        setPhase(PhaseRising);
    }

    private void startAttack(byte Kind) {
        AttackStart = tickCount;
        entityData.set(AttackData, Kind);
        entityData.set(SerialData, entityData.get(SerialData) + 1);
    }

    private void endAttack() {
        entityData.set(AttackData, AttackNone);
        ComboStep = 0;
    }

    private static final List<BiPredicate<Entity, LivingEntity>> Quarry = new CopyOnWriteArrayList<>();

    public static void addPrey(BiPredicate<Entity, LivingEntity> Test) {
        Quarry.add(Test);
    }

    public static boolean isPrey(Entity Hunter, @Nullable Entity Found) {
        if (!(Found instanceof LivingEntity Body) || Body instanceof Scp076Abel || Body == Hunter) {
            return false;
        }
        if (!Body.isAlive() || Body.isRemoved() || Body.isDeadOrDying() || Body.isSpectator() || !Targeting.isTargetable(Body)) {
            return false;
        }
        for (BiPredicate<Entity, LivingEntity> Each : Quarry) {
            if (Each.test(Hunter, Body)) {
                return true;
            }
        }
        if (Body instanceof Player User) {
            return !User.isCreative() && Hunter.level().getDifficulty() != Difficulty.PEACEFUL && !Scp268.isNeglected(User);
        }
        return ControlBodies.isPersonLike(Body);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (!level().isClientSide) {
            return;
        }
        if (SerialData.equals(Accessor)) {
            LocalAttackStart = tickCount;
            if (AttackController != null) {
                AttackController.forceAnimationReset();
            }
        } else if (PhaseData.equals(Accessor)) {
            LocalPhaseStart = tickCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientEffects();
        }
    }

    private void clientEffects() {
        if (phase() == PhaseRising && phaseElapsed() < RiseTicks - 6 && tickCount % 2 == 0) {
            level().addParticle(Scp076DoorBlockEntity.Ash, getRandomX(0.6), getY() + random.nextDouble() * 1.4, getRandomZ(0.6), 0.0, 0.03, 0.0);
        }
        if (isAlive() && tickCount % 9 == 0) {
            level().addParticle(Scp076DoorBlockEntity.Ember, getRandomX(0.5), getY() + 0.8 + random.nextDouble() * 0.9, getRandomZ(0.5), 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        byte Current = phase();
        if (Current == PhaseRising) {
            tickRise(World);
            return;
        }
        if (Current == PhaseSinking) {
            tickSink(World);
            return;
        }
        scan(World);
        track(getTarget());
        setAggressive(getTarget() != null);
        tickCooldowns();
        tickAttack(World);
        tickIdle(World);
        if (tickCount - LastHurt > RegenDelay && tickCount % RegenEvery == 0 && getHealth() < getMaxHealth()) {
            heal(RegenAmount);
        }
    }

    private void tickRise(ServerLevel World) {
        int Time = tickCount - PhaseStart;
        getNavigation().stop();
        setDeltaMovement(0.0, Math.min(0.0, getDeltaMovement().y), 0.0);
        if (Time % 4 == 0 && Time < RiseTicks - 8) {
            World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 2, 0.25, 0.1, 0.25, 0.01);
        }
        if (Time >= RiseTicks) {
            setPhase(PhaseHunting);
            Vec3 Out = new Vec3(HomeFront.getStepX(), 0.0, HomeFront.getStepZ());
            if (HomeCoffin == null) {
                Out = Vec3.directionFromRotation(0.0f, getYRot());
            }
            setDeltaMovement(Out.x * 0.28, 0.56, Out.z * 0.28);
            hasImpulse = true;
            playSound(Scp076Registry.Breath.get(), 1.0f, 0.75f);
        }
    }

    private void tickSink(ServerLevel World) {
        int Time = tickCount - PhaseStart;
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (Time % 3 == 0) {
            World.sendParticles(Scp076DoorBlockEntity.Ash, getX(), getY() + 1.0, getZ(), 4, 0.3, 0.5, 0.3, 0.01);
        }
        if (Time >= SinkTicks) {
            depart(World, false);
        }
    }

    private void scan(ServerLevel World) {
        LivingEntity Current = getTarget();
        if (Current != null) {
            if (!isPrey(this, Current) || Current.level() != level() || distanceToSqr(Current) > LoseRange * LoseRange) {
                setTarget(null);
            } else if (getSensing().hasLineOfSight(Current) || distanceToSqr(Current) < SenseRange * SenseRange) {
                LastSeen = tickCount;
            } else if (tickCount - LastSeen > LoseSight) {
                setTarget(null);
            }
        }
        if (getTarget() != null || (tickCount + getId()) % ScanEvery != 0) {
            return;
        }
        double Range = getAttributeValue(Attributes.FOLLOW_RANGE);
        LivingEntity Best = null;
        double BestGap = Double.MAX_VALUE;
        for (LivingEntity Found : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Range, Range * 0.5, Range), Body -> isPrey(this, Body))) {
            double Gap = distanceToSqr(Found);
            if (Gap < BestGap && (Gap < SenseRange * SenseRange || hasLineOfSight(Found))) {
                BestGap = Gap;
                Best = Found;
            }
        }
        if (Best != null) {
            pursue(Best);
        }
    }

    private void pursue(LivingEntity Prey) {
        setTarget(Prey);
        LastSeen = tickCount;
        IdleTicks = 0;
        if (phase() == PhaseReturning) {
            setPhase(PhaseHunting);
        }
    }

    private void tickCooldowns() {
        MeleeWait = Math.max(0, MeleeWait - 1);
        ThrowWait = Math.max(0, ThrowWait - 1);
        SummonWait = Math.max(0, SummonWait - 1);
    }

    private void tickIdle(ServerLevel World) {
        if (getTarget() != null) {
            IdleTicks = 0;
            return;
        }
        if (phase() == PhaseHunting) {
            IdleTicks++;
            if (IdleTicks >= IdleLimit && hasHome()) {
                setPhase(PhaseReturning);
                ReturnTicks = 0;
                StuckTicks = 0;
                BestDistance = Double.MAX_VALUE;
            }
            return;
        }
        if (phase() != PhaseReturning) {
            return;
        }
        ReturnTicks++;
        Vec3 Home = homePoint();
        if (Home == null || HomeDimension == null || !level().dimension().equals(HomeDimension) || !World.isLoaded(HomeDoor) || position().distanceTo(Home) > ReturnLeash) {
            crumbleAway(World);
            return;
        }
        double Flat = Math.sqrt(Mth.square(getX() - Home.x) + Mth.square(getZ() - Home.z));
        if (Flat < HomeReach && Math.abs(getY() - Home.y) < 1.6) {
            beginSink(Home);
            return;
        }
        if (Flat < BestDistance - 0.5) {
            BestDistance = Flat;
            StuckTicks = 0;
        } else {
            StuckTicks++;
        }
        if (StuckTicks > StuckLimit || ReturnTicks > ReturnLimit) {
            crumbleAway(World);
        }
    }

    @Nullable
    public Vec3 homePoint() {
        if (HomeDoor == null) {
            return null;
        }
        if (HomeCoffin != null) {
            return Vec3.atBottomCenterOf(HomeCoffin).add(0.0, Scp076CoffinBlockEntity.FloorHeight, 0.0);
        }
        return Vec3.atBottomCenterOf(Scp076Vault.centreFromDoor(HomeDoor, HomeFront).above());
    }

    private void beginSink(Vec3 Home) {
        moveTo(Home.x, Home.y, Home.z, HomeFront.toYRot(), 0.0f);
        setYHeadRot(HomeFront.toYRot());
        setYBodyRot(HomeFront.toYRot());
        endAttack();
        setTarget(null);
        setPhase(PhaseSinking);
        playSound(Scp076Registry.Rise.get(), 1.2f, 0.65f);
    }

    private void crumbleAway(ServerLevel World) {
        World.sendParticles(Scp076DoorBlockEntity.Ash, getX(), getY() + 1.0, getZ(), 60, 0.35, 0.8, 0.35, 0.02);
        World.sendParticles(ParticleTypes.ASH, getX(), getY() + 1.0, getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        World.playSound(null, getX(), getY(), getZ(), Scp076Registry.Crumble.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
        depart(World, false);
    }

    private void depart(ServerLevel World, boolean Killed) {
        if (Departed) {
            return;
        }
        Departed = true;
        if (hasHome() && World.getServer() != null) {
            Scp076Vault.abelGone(World.getServer(), HomeDimension, HomeDoor, Killed);
        }
        if (!Killed) {
            discard();
        }
    }

    public void hunt(Scp076AbelGoals.Hunt Pursuit, LivingEntity Prey) {
        getLookControl().setLookAt(Prey, 40.0f, 40.0f);
        double Gap = distanceTo(Prey);
        boolean Sees = getSensing().hasLineOfSight(Prey);
        byte Current = attack();
        if (Current != AttackNone) {
            if (Current == AttackThrow || Current == AttackSummon) {
                getNavigation().stop();
                turnTo(Prey);
            } else if (Gap > 1.4) {
                Pursuit.chase(Prey, MeleeCreep);
            } else {
                getNavigation().stop();
                turnTo(Prey);
            }
            return;
        }
        if (Gap <= MeleeStart && Sees && MeleeWait <= 0) {
            ComboStep = 1;
            getNavigation().stop();
            startAttack(AttackSlashA);
            return;
        }
        boolean CanSummon = Gap >= SummonMin && Gap <= SummonMax && SummonWait <= 0 && (Sees || Gap < SummonPrefer * 1.5);
        boolean CanThrow = Gap >= ThrowMin && Gap <= ThrowMax && ThrowWait <= 0 && Sees;
        if (CanSummon && (Gap >= SummonPrefer || !CanThrow)) {
            getNavigation().stop();
            startAttack(AttackSummon);
            return;
        }
        if (CanThrow) {
            getNavigation().stop();
            startAttack(AttackThrow);
            return;
        }
        Pursuit.chase(Prey, ChaseSpeed);
    }

    private void turnTo(LivingEntity Prey) {
        double Dx = Prey.getX() - getX();
        double Dz = Prey.getZ() - getZ();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Mth.approachDegrees(getYRot(), Yaw, 30.0f));
        setYBodyRot(getYRot());
    }

    private void tickAttack(ServerLevel World) {
        byte Current = attack();
        if (Current == AttackNone) {
            return;
        }
        int Time = tickCount - AttackStart;
        LivingEntity Prey = getTarget();
        if (Time == 1) {
            switch (Current) {
                case AttackSummon -> playSound(Scp076Registry.Summon.get(), 1.3f, 0.8f + random.nextFloat() * 0.1f);
                case AttackThrow -> playSound(Scp076Registry.Slash.get(), 0.7f, 1.3f);
                default -> {
                }
            }
        }
        if (Time == AttackHit[Current]) {
            switch (Current) {
                case AttackSlashA, AttackSlashB -> strike(World, Prey, SlashDamage, false);
                case AttackStab -> strike(World, Prey, StabDamage, true);
                case AttackThrow -> throwKnife(World, Prey);
                case AttackSummon -> summonBlades(World, Prey);
                default -> {
                }
            }
        }
        if (Time < AttackLength[Current]) {
            return;
        }
        if (Current == AttackSlashA || Current == AttackSlashB || Current == AttackStab) {
            if (ComboStep < 3 && Prey != null && isPrey(this, Prey) && distanceTo(Prey) <= MeleeReach + 0.6) {
                ComboStep++;
                startAttack(ComboStep == 2 ? AttackSlashB : AttackStab);
                return;
            }
            endAttack();
            MeleeWait = MeleeCooldown;
            return;
        }
        if (Current == AttackThrow) {
            ThrowWait = ThrowCooldown + random.nextInt(ThrowJitter + 1);
        } else {
            SummonWait = SummonCooldown + random.nextInt(SummonJitter + 1);
            ThrowWait = Math.max(ThrowWait, 12);
        }
        endAttack();
    }

    private void strike(ServerLevel World, @Nullable LivingEntity Prey, float Amount, boolean Heavy) {
        playSound(Heavy ? Scp076Registry.Stab.get() : Scp076Registry.Slash.get(), 1.0f, 0.85f + random.nextFloat() * 0.2f);
        if (Prey == null || !isPrey(this, Prey)) {
            return;
        }
        double Reach = MeleeReach + Prey.getBbWidth() * 0.5;
        if (distanceTo(Prey) > Reach) {
            return;
        }
        Vec3 Toward = Prey.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        Vec3 Look = Vec3.directionFromRotation(0.0f, getYRot());
        if (Toward.lengthSqr() > 1.0e-4 && Toward.normalize().dot(Look) < 0.1) {
            return;
        }
        DamageSource Source = damageSources().mobAttack(this);
        HeavyStrike = Heavy;
        boolean Hit;
        try {
            Hit = Prey.hurt(Source, Amount);
        } finally {
            HeavyStrike = false;
        }
        if (!Hit) {
            return;
        }
        Prey.knockback(Heavy ? 0.6 : 0.3, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
        setLastHurtMob(Prey);
        World.playSound(null, Prey.getX(), Prey.getY(), Prey.getZ(), Scp076Registry.SlashRing.get(), SoundSource.HOSTILE, 0.9f, Heavy ? 0.7f : 0.95f);
        Vec3 Mid = position().add(Prey.position()).scale(0.5).add(0.0, getBbHeight() * 0.6, 0.0);
        World.sendParticles(Heavy ? ParticleTypes.CRIT : ParticleTypes.SWEEP_ATTACK, Mid.x, Mid.y, Mid.z, Heavy ? 8 : 1, 0.1, 0.1, 0.1, 0.0);
    }

    public Vec3 handPoint(boolean Right) {
        float Side = getYRot() + (Right ? 90.0f : -90.0f);
        Vec3 Offset = Vec3.directionFromRotation(0.0f, Side).scale(0.38);
        return position().add(Offset.x, getBbHeight() * 0.8, Offset.z);
    }

    private void throwKnife(ServerLevel World, @Nullable LivingEntity Prey) {
        if (Prey == null || !isPrey(this, Prey)) {
            return;
        }
        Vec3 Hand = handPoint(false);
        Scp076Knife Thrown = new Scp076Knife(World, this, Hand);
        Vec3 Aim = new Vec3(Prey.getX(), Prey.getY() + Prey.getBbHeight() * 0.55, Prey.getZ());
        double Flight = Math.min(30.0, Aim.distanceTo(Hand) / KnifeSpeed);
        Vec3 Motion = drift(Prey);
        Aim = Aim.add(Motion.x * Flight * 0.8, 0.0, Motion.z * Flight * 0.8);
        Vec3 Path = Aim.subtract(Hand);
        double Lift = 0.5 * Scp076Knife.Gravity * Flight * Flight;
        float Spread = switch (World.getDifficulty()) {
            case HARD -> 0.8f;
            case NORMAL -> 1.6f;
            default -> 2.6f;
        };
        Thrown.shoot(Path.x, Path.y + Lift, Path.z, KnifeSpeed, Spread);
        Thrown.setDamage(KnifeDamage);
        World.addFreshEntity(Thrown);
        playSound(Scp076Registry.Throw.get(), 1.1f, 0.9f + random.nextFloat() * 0.15f);
    }

    private void summonBlades(ServerLevel World, @Nullable LivingEntity Prey) {
        if (Prey == null || !isPrey(this, Prey)) {
            return;
        }
        Vec3 Base = Prey.position();
        Vec3 Motion = drift(Prey).scale(BladeLead);
        if (Motion.length() > BladeLeadLimit) {
            Motion = Motion.normalize().scale(BladeLeadLimit);
        }
        Vec3 Ahead = Base.add(Motion);
        List<Vec3> Spots = new ArrayList<>();
        Spots.add(Ahead);
        if (Motion.length() > 0.6) {
            Spots.add(Base);
        }
        double Turn = random.nextDouble() * Mth.TWO_PI;
        while (Spots.size() < BladeCount) {
            double Angle = Turn + Spots.size() * Mth.TWO_PI / (BladeCount - 1);
            Spots.add(Ahead.add(Math.cos(Angle) * BladeSpread, 0.0, Math.sin(Angle) * BladeSpread));
        }
        for (int Index = 0; Index < Spots.size(); Index++) {
            Vec3 Spot = Spots.get(Index);
            Optional<Double> Ground = groundAt(World, Spot.x, Spot.z, Prey.getY());
            if (Ground.isEmpty()) {
                continue;
            }
            Scp076Blades Blades = new Scp076Blades(World, Spot.x, Ground.get(), Spot.z, random.nextFloat() * 360.0f, Index == 0 ? 0 : 2 + Index * 2, this, BladeDamage);
            World.addFreshEntity(Blades);
        }
    }

    private Vec3 drift(LivingEntity Prey) {
        return Prey.getId() == TrackedId ? TrackedVelocity : Vec3.ZERO;
    }

    private void track(@Nullable LivingEntity Prey) {
        if (Prey == null) {
            TrackedId = -1;
            TrackedPos = null;
            TrackedVelocity = Vec3.ZERO;
            return;
        }
        Vec3 Now = Prey.position();
        if (Prey.getId() == TrackedId && TrackedPos != null) {
            Vec3 Step = Now.subtract(TrackedPos).multiply(1.0, 0.0, 1.0);
            if (Step.lengthSqr() > 1.0) {
                Step = Vec3.ZERO;
            }
            TrackedVelocity = TrackedVelocity.scale(0.5).add(Step.scale(0.5));
        } else {
            TrackedVelocity = Vec3.ZERO;
        }
        TrackedId = Prey.getId();
        TrackedPos = Now;
    }

    public static Optional<Double> groundAt(ServerLevel World, double X, double Z, double Around) {
        BlockPos Pos = BlockPos.containing(X, Around + 2.0, Z);
        int Lowest = Mth.floor(Around) - 6;
        while (Pos.getY() >= Lowest && !World.isOutsideBuildHeight(Pos)) {
            BlockPos Below = Pos.below();
            BlockState Under = World.getBlockState(Below);
            if (Under.isFaceSturdy(World, Below, Direction.UP)) {
                double Offset = 0.0;
                if (!World.isEmptyBlock(Pos)) {
                    VoxelShape Shape = World.getBlockState(Pos).getCollisionShape(World, Pos);
                    if (!Shape.isEmpty()) {
                        Offset = Shape.max(Direction.Axis.Y);
                    }
                }
                if (Offset < 1.0) {
                    return Optional.of(Pos.getY() + Offset);
                }
            }
            Pos = Below;
        }
        return Optional.empty();
    }

    @Override
    public boolean canDisableShield() {
        return HeavyStrike || super.canDisableShield();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (busy() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        if (Source.getEntity() instanceof Scp076Abel) {
            return true;
        }
        return super.isInvulnerableTo(Source);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide) {
            LastHurt = tickCount;
            Entity Attacker = Source.getEntity();
            if (isAlive() && !busy() && isPrey(this, Attacker) && Attacker instanceof LivingEntity Body) {
                LivingEntity Current = getTarget();
                if (Current == null || distanceToSqr(Body) < distanceToSqr(Current) * 0.5) {
                    pursue(Body);
                }
            }
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World && !Departed) {
            depart(World, true);
        }
        super.die(Source);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        AABB Box = getBoundingBox();
        double Top = Box.maxY - (Box.maxY - Box.minY) * Math.min(1.0, deathTime / (double) DeathTicks) * 0.8;
        if (deathTime == 1) {
            World.playSound(null, getX(), getY(), getZ(), Scp076Registry.Crumble.get(), SoundSource.HOSTILE, 1.4f, 0.7f);
        }
        World.sendParticles(Scp076DoorBlockEntity.Ash, getX(), (Box.minY + Top) * 0.5, getZ(), 3 + deathTime / 4, 0.3, (Top - Box.minY) * 0.3, 0.3, 0.02);
        World.sendParticles(ParticleTypes.ASH, getX(), Top, getZ(), 2, 0.3, 0.2, 0.3, 0.0);
        if (deathTime % 6 == 0) {
            World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), Box.minY + 0.4, getZ(), 2, 0.3, 0.1, 0.3, 0.01);
        }
        if (deathTime >= DeathTicks && !isRemoved()) {
            World.sendParticles(Scp076DoorBlockEntity.Ash, getX(), getY() + 0.3, getZ(), 40, 0.45, 0.2, 0.45, 0.03);
            World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 8, 0.4, 0.1, 0.4, 0.01);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected RawAnimation bodyAnimation() {
        if (isDeadOrDying()) {
            return CrumblePlay;
        }
        byte Current = phase();
        if (Current == PhaseRising) {
            return RisePlay;
        }
        if (Current == PhaseSinking) {
            return SinkPlay;
        }
        return super.bodyAnimation();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        super.registerControllers(Controllers);
        AttackController = new Scp076AbelController(this, "attack", 2, Event -> {
            byte Current = attack();
            if (Current == AttackNone || isDeadOrDying() || busy()) {
                return PlayState.STOP;
            }
            return Event.setAndContinue(AttackPlays[Current]);
        });
        Controllers.add(AttackController);
    }

    @Override
    public boolean eyesShut() {
        if (phase() == PhaseRising && phaseElapsed() < RiseTicks * 0.55) {
            return true;
        }
        if (phase() == PhaseSinking && phaseElapsed() > SinkTicks * 0.4) {
            return true;
        }
        return super.eyesShut();
    }

    @Override
    public float glanceRate() {
        return 0.55f;
    }

    @Override
    public boolean isPushable() {
        return !busy() && super.isPushable();
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean Passengers) {
        return false;
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
    public boolean canDrownInFluidType(FluidType Kind) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return Other instanceof Scp076Abel || super.isAlliedTo(Other);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return busy() ? null : Scp076Registry.Breath.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp076Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp076Registry.Death.get();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.0, 0.4, 1.0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (HomeDimension != null && HomeDoor != null) {
            Saved.putString("HomeDimension", HomeDimension.location().toString());
            Saved.put("HomeDoor", NbtUtils.writeBlockPos(HomeDoor));
            Saved.putString("HomeFront", HomeFront.getSerializedName());
            if (HomeCoffin != null) {
                Saved.put("HomeCoffin", NbtUtils.writeBlockPos(HomeCoffin));
            }
        }
        Saved.putByte("Phase", phase());
        Saved.putInt("IdleTicks", IdleTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        setAppearance(Appearance);
        ResourceLocation Dimension = ResourceLocation.tryParse(Saved.getString("HomeDimension"));
        Optional<BlockPos> Door = NbtUtils.readBlockPos(Saved, "HomeDoor");
        if (Dimension != null && Door.isPresent()) {
            HomeDimension = ResourceKey.create(Registries.DIMENSION, Dimension);
            HomeDoor = Door.get();
            Direction Front = Direction.byName(Saved.getString("HomeFront"));
            HomeFront = Front != null && Front.getAxis().isHorizontal() ? Front : Direction.SOUTH;
            HomeCoffin = NbtUtils.readBlockPos(Saved, "HomeCoffin").orElse(null);
        }
        IdleTicks = Saved.getInt("IdleTicks");
        byte Stored = Saved.getByte("Phase");
        setPhase(Stored == PhaseReturning ? PhaseReturning : PhaseHunting);
    }
}
