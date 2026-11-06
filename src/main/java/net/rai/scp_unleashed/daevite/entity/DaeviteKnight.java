package net.rai.scp_unleashed.daevite.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.daevite.DaeviteRegistry;
import net.rai.scp_unleashed.dclass.HumanoidGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.HashSet;
import java.util.Set;

public class DaeviteKnight extends DaeviteMob {
    public static final float Width = 0.7f;
    public static final float Height = 2.1f;
    public static final float EyeHeight = 1.86f;
    public static final double MaxHealth = 50.0;
    public static final double Armour = 14.0;
    public static final double Toughness = 4.0;
    public static final double Steadiness = 0.6;
    public static final double Speed = 0.27;
    public static final double Follow = 40.0;
    public static final double ChaseSpeed = 1.1;
    public static final double Stride = 1.0;
    public static final float ThrustDamage = 8.0f;
    public static final double ThrustStart = 3.6;
    public static final double ThrustReach = 4.3;
    public static final float ChargeDamage = 12.0f;
    public static final double ChargeMin = 6.0;
    public static final double ChargeMax = 16.0;
    public static final double ChargePace = 0.55;
    public static final float ChargeTurn = 4.0f;
    public static final int ChargeCooldown = 140;
    public static final int ChargeJitter = 60;
    public static final float SlashDamage = 6.0f;
    public static final float OverheadDamage = 10.0f;
    public static final float RiposteDamage = 8.0f;
    public static final double SlashStart = 2.6;
    public static final double SlashReach = 3.2;
    public static final float GuardShare = 0.2f;
    public static final double GuardCosine = 0.34;
    public static final float GuardChance = 0.4f;
    public static final int GuardCooldown = 70;
    public static final int MeleeCooldown = 16;
    public static final int Experience = 12;
    public static final byte Spear = 0;
    public static final byte Sword = 1;
    public static final byte MoveThrust = 1;
    public static final byte MoveChargeReady = 2;
    public static final byte MoveCharge = 3;
    public static final byte MoveRecover = 4;
    public static final byte MoveSlashA = 5;
    public static final byte MoveSlashB = 6;
    public static final byte MoveOverhead = 7;
    public static final byte MoveGuard = 8;
    public static final byte MoveRiposte = 9;
    public static final String[] Clips = {"", "thrust", "charge_ready", "charge", "charge_recover", "slash_a", "slash_b", "overhead", "guard", "riposte"};
    public static final int[] Length = {0, 14, 14, 26, 18, 10, 10, 15, 34, 10};
    public static final int[] Hit = {0, 8, 0, 0, 0, 5, 5, 9, 0, 4};
    public static final RawAnimation IdlePlay = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation ReadyPlay = RawAnimation.begin().thenLoop("ready");
    public static final RawAnimation WalkPlay = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation RunPlay = RawAnimation.begin().thenLoop("run");
    public static final RawAnimation SwimPlay = RawAnimation.begin().thenLoop("swim");
    public static final RawAnimation[] Plays = new RawAnimation[Clips.length];
    protected static final EntityDataAccessor<Byte> LoadoutData = SynchedEntityData.defineId(DaeviteKnight.class, EntityDataSerializers.BYTE);

    static {
        for (int Index = 1; Index < Clips.length; Index++) {
            Plays[Index] = Index == MoveCharge || Index == MoveGuard ? RawAnimation.begin().thenLoop(Clips[Index]) : RawAnimation.begin().thenPlayAndHold(Clips[Index]);
        }
    }

    private boolean LoadoutChosen;
    private int MeleeWait;
    private int ChargeWait = 40;
    private int GuardWait;
    private int Combo;
    private boolean RiposteQueued;
    private boolean HeavyStrike;
    private Vec3 ChargeDir = Vec3.ZERO;
    private final Set<Integer> ChargeStruck = new HashSet<>();
    private double LastCycle;

    public DaeviteKnight(EntityType<? extends DaeviteKnight> Type, Level World) {
        super(Type, World);
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
                .add(Attributes.SAFE_FALL_DISTANCE, 5.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(LoadoutData, Spear);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new DaeviteGoals.Fight(this));
        goalSelector.addGoal(2, new DaeviteGoals.Patrol(this));
        goalSelector.addGoal(3, new DaeviteGoals.ReturnHome(this));
        goalSelector.addGoal(5, new DaeviteGoals.Post(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public byte loadout() {
        return entityData.get(LoadoutData);
    }

    public boolean spearman() {
        return loadout() == Spear;
    }

    public void setLoadout(byte Value) {
        entityData.set(LoadoutData, Value == Sword ? Sword : Spear);
        LoadoutChosen = true;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        if (!LoadoutChosen) {
            setLoadout(random.nextBoolean() ? Spear : Sword);
        }
        return Result;
    }

    @Override
    public String clipFor(byte Move) {
        return Move > 0 && Move < Clips.length ? Clips[Move] : "";
    }

    @Override
    public boolean loopingMove() {
        byte Current = move();
        return Current == MoveCharge || Current == MoveGuard;
    }

    @Override
    protected boolean seesThroughRobes() {
        return false;
    }

    @Override
    public String roleName() {
        return "knight";
    }

    public boolean guarding() {
        return moving(MoveGuard);
    }

    public boolean charging() {
        byte Current = move();
        return Current == MoveChargeReady || Current == MoveCharge;
    }

    @Override
    protected RawAnimation liveBody() {
        if (isSwimmingPose()) {
            return SwimPlay;
        }
        if (isStriding()) {
            return isRunning() ? RunPlay : WalkPlay;
        }
        return isAggressive() ? ReadyPlay : IdlePlay;
    }

    @Override
    protected RawAnimation movePlay(byte Move) {
        return Move > 0 && Move < Plays.length ? Plays[Move] : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            stepClank();
        }
    }

    private void stepClank() {
        HumanoidGait Gait = gait();
        if (Gait == null || !Gait.striding() || isSilent()) {
            LastCycle = 0.0;
            return;
        }
        double Cycle = Gait.cycle();
        if (LastCycle < 0.5 && Cycle >= 0.5 || Cycle < LastCycle) {
            level().playLocalSound(getX(), getY(), getZ(), DaeviteRegistry.ArmourStep.get(), getSoundSource(), Gait.running() ? 0.5f : 0.32f,
                    0.85f + random.nextFloat() * 0.2f, false);
        }
        LastCycle = Cycle;
    }

    @Override
    public void fight(DaeviteGoals.Fight Pursuit, LivingEntity Prey) {
        getLookControl().setLookAt(Prey, 40.0f, 40.0f);
        double Gap = distanceTo(Prey);
        boolean Sees = getSensing().hasLineOfSight(Prey);
        byte Current = move();
        if (Current != MoveNone) {
            if (Current == MoveChargeReady || Current == MoveGuard) {
                getNavigation().stop();
                turnTo(Prey, 20.0f);
            } else if (Current == MoveCharge || Current == MoveRecover) {
                getNavigation().stop();
            } else if (Gap > (spearman() ? ThrustReach - 0.8 : SlashReach - 0.6)) {
                Pursuit.chase(Prey, 0.55);
            } else {
                getNavigation().stop();
                turnTo(Prey, 30.0f);
            }
            return;
        }
        if (spearman()) {
            if (Gap <= ThrustStart && Sees && MeleeWait <= 0) {
                getNavigation().stop();
                startMove(MoveThrust);
                return;
            }
            if (Gap >= ChargeMin && Gap <= ChargeMax && Sees && ChargeWait <= 0 && clearLane(Prey)) {
                getNavigation().stop();
                startMove(MoveChargeReady);
                return;
            }
            Pursuit.chase(Prey, ChaseSpeed);
            return;
        }
        if (GuardWait <= 0 && Gap < 4.5 && threatening(Prey) && random.nextFloat() < GuardChance) {
            getNavigation().stop();
            GuardWait = GuardCooldown;
            RiposteQueued = false;
            startMove(MoveGuard);
            return;
        }
        if (Gap <= SlashStart && Sees && MeleeWait <= 0) {
            getNavigation().stop();
            Combo = 1;
            startMove(MoveSlashA);
            return;
        }
        Pursuit.chase(Prey, ChaseSpeed * 1.05);
    }

    private boolean threatening(LivingEntity Prey) {
        if (Prey.swinging) {
            return true;
        }
        return Prey.isUsingItem() && (Prey.getUseItem().getItem() instanceof BowItem || Prey.getUseItem().getItem() instanceof CrossbowItem);
    }

    private boolean clearLane(LivingEntity Prey) {
        Vec3 From = position().add(0.0, 0.6, 0.0);
        Vec3 To = Prey.position().add(0.0, 0.6, 0.0);
        if (level().clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) {
            return false;
        }
        return level().clip(new ClipContext(getEyePosition(), Prey.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }

    @Override
    protected void serverCombat(ServerLevel World) {
        MeleeWait = Math.max(0, MeleeWait - 1);
        ChargeWait = Math.max(0, ChargeWait - 1);
        GuardWait = Math.max(0, GuardWait - 1);
        byte Current = move();
        if (Current == MoveNone) {
            return;
        }
        int Time = moveTicks();
        LivingEntity Prey = getTarget();
        switch (Current) {
            case MoveThrust -> {
                if (Time == 2) {
                    playSound(DaeviteRegistry.ChargeCry.get(), 0.35f, 1.5f);
                }
                if (Time == Hit[Current]) {
                    strike(World, Prey, ThrustDamage, ThrustReach, false, 0.55, DaeviteRegistry.SpearThrust.get());
                }
                if (Time >= Length[Current]) {
                    endMove();
                    MeleeWait = MeleeCooldown - 2;
                }
            }
            case MoveChargeReady -> {
                if (Time == 1) {
                    playSound(DaeviteRegistry.ChargeCry.get(), 1.4f, 0.85f + random.nextFloat() * 0.1f);
                }
                if (Time % 3 == 0) {
                    kickDust(World, 3);
                }
                if (Time >= Length[Current]) {
                    beginCharge(Prey);
                }
            }
            case MoveCharge -> tickCharge(World, Prey, Time);
            case MoveRecover -> {
                getNavigation().stop();
                if (Time >= Length[Current]) {
                    endMove();
                    ChargeWait = ChargeCooldown + random.nextInt(ChargeJitter + 1);
                    MeleeWait = 6;
                }
            }
            case MoveSlashA, MoveSlashB, MoveOverhead -> {
                if (Time == Hit[Current]) {
                    boolean Heavy = Current == MoveOverhead;
                    strike(World, Prey, Heavy ? OverheadDamage : SlashDamage, SlashReach, Heavy, Heavy ? 0.7 : 0.3,
                            Heavy ? DaeviteRegistry.SwordHeavy.get() : DaeviteRegistry.SwordSlash.get());
                }
                if (Time >= Length[Current]) {
                    if (Combo < 3 && Prey != null && isPrey(Prey) && distanceTo(Prey) <= SlashReach + 0.6) {
                        Combo++;
                        startMove(Combo == 2 ? MoveSlashB : MoveOverhead);
                    } else {
                        endMove();
                        Combo = 0;
                        MeleeWait = MeleeCooldown;
                    }
                }
            }
            case MoveGuard -> {
                getNavigation().stop();
                if (RiposteQueued && Time > 4) {
                    RiposteQueued = false;
                    startMove(MoveRiposte);
                } else if (Time >= Length[Current] || Prey == null) {
                    endMove();
                }
            }
            case MoveRiposte -> {
                if (Time == Hit[Current]) {
                    strike(World, Prey, RiposteDamage, SlashReach + 0.4, false, 0.5, DaeviteRegistry.SwordSlash.get());
                }
                if (Time >= Length[Current]) {
                    endMove();
                    MeleeWait = 8;
                }
            }
            default -> endMove();
        }
    }

    private void beginCharge(@Nullable LivingEntity Prey) {
        Vec3 Toward = Prey != null ? Prey.position().add(Prey.getDeltaMovement().multiply(6.0, 0.0, 6.0)).subtract(position()) : Vec3.directionFromRotation(0.0f, getYRot());
        Toward = Toward.multiply(1.0, 0.0, 1.0);
        ChargeDir = Toward.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0.0f, getYRot()) : Toward.normalize();
        ChargeStruck.clear();
        startMove(MoveCharge);
    }

    private void tickCharge(ServerLevel World, @Nullable LivingEntity Prey, int Time) {
        getNavigation().stop();
        if (Prey != null && Prey.isAlive()) {
            Vec3 Want = Prey.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (Want.lengthSqr() > 1.0e-4) {
                float Current = (float) (Mth.atan2(ChargeDir.z, ChargeDir.x) * Mth.RAD_TO_DEG);
                float Goal = (float) (Mth.atan2(Want.z, Want.x) * Mth.RAD_TO_DEG);
                float Turned = Mth.approachDegrees(Current, Goal, ChargeTurn) * Mth.DEG_TO_RAD;
                ChargeDir = new Vec3(Math.cos(Turned), 0.0, Math.sin(Turned));
            }
        }
        float Yaw = (float) (Mth.atan2(ChargeDir.z, ChargeDir.x) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
        Vec3 Motion = getDeltaMovement();
        setDeltaMovement(ChargeDir.x * ChargePace, Motion.y, ChargeDir.z * ChargePace);
        hasImpulse = true;
        if (Time % 2 == 0) {
            kickDust(World, 2);
        }
        for (LivingEntity Found : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.45).move(ChargeDir.scale(0.4)), this::isPrey)) {
            if (!ChargeStruck.add(Found.getId())) {
                continue;
            }
            HeavyStrike = true;
            boolean Landed;
            try {
                Landed = Found.hurt(damageSources().mobAttack(this), ChargeDamage);
            } finally {
                HeavyStrike = false;
            }
            if (Landed) {
                Found.knockback(1.3, -ChargeDir.x, -ChargeDir.z);
                Found.hurtMarked = true;
                Found.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), this);
                setLastHurtMob(Found);
                World.playSound(null, Found.getX(), Found.getY(), Found.getZ(), DaeviteRegistry.ChargeHit.get(), SoundSource.HOSTILE, 1.3f, 0.8f);
                World.sendParticles(ParticleTypes.CRIT, Found.getX(), Found.getY() + Found.getBbHeight() * 0.6, Found.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
                startMove(MoveRecover);
                return;
            }
        }
        if (horizontalCollision && Time > 3) {
            World.playSound(null, getX(), getY(), getZ(), DaeviteRegistry.ChargeWall.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
            BlockState Under = level().getBlockState(blockPosition().relative(getDirection()));
            if (!Under.isAir()) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Under), getX() + ChargeDir.x * 0.6, getY() + 1.0, getZ() + ChargeDir.z * 0.6, 16, 0.3, 0.4, 0.3, 0.1);
            }
            setDeltaMovement(Vec3.ZERO);
            startMove(MoveRecover);
            return;
        }
        if (Time >= Length[MoveCharge]) {
            startMove(MoveRecover);
        }
    }

    private void kickDust(ServerLevel World, int Count) {
        BlockPos Below = getOnPos();
        BlockState Ground = World.getBlockState(Below);
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), getX(), getY() + 0.05, getZ(), Count, 0.25, 0.02, 0.25, 0.05);
        }
    }

    private void strike(ServerLevel World, @Nullable LivingEntity Prey, float Amount, double Reach, boolean Heavy, double Push, SoundEvent Swing) {
        playSound(Swing, 1.0f, 0.85f + random.nextFloat() * 0.2f);
        if (Prey == null || !isPrey(Prey) || distanceTo(Prey) > Reach + Prey.getBbWidth() * 0.5 || !facing(Prey, 0.1)) {
            return;
        }
        HeavyStrike = Heavy;
        boolean Landed;
        try {
            Landed = Prey.hurt(damageSources().mobAttack(this), Amount);
        } finally {
            HeavyStrike = false;
        }
        if (!Landed) {
            return;
        }
        Prey.knockback(Push, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
        setLastHurtMob(Prey);
        World.playSound(null, Prey.getX(), Prey.getY(), Prey.getZ(), DaeviteRegistry.BladeBite.get(), SoundSource.HOSTILE, 0.9f, Heavy ? 0.75f : 1.0f);
        Vec3 Mid = position().add(Prey.position()).scale(0.5).add(0.0, getBbHeight() * 0.6, 0.0);
        World.sendParticles(Heavy ? ParticleTypes.CRIT : ParticleTypes.SWEEP_ATTACK, Mid.x, Mid.y, Mid.z, Heavy ? 8 : 1, 0.1, 0.1, 0.1, 0.0);
    }

    @Override
    public boolean canDisableShield() {
        return HeavyStrike || super.canDisableShield();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && guarding() && !Source.is(DamageTypeTags.BYPASSES_SHIELD) && blocks(Source)) {
            level().playSound(null, getX(), getY(), getZ(), DaeviteRegistry.GuardClang.get(), SoundSource.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.2f);
            if (level() instanceof ServerLevel World) {
                Vec3 Front = position().add(Vec3.directionFromRotation(0.0f, getYRot()).scale(0.6)).add(0.0, 1.3, 0.0);
                World.sendParticles(ParticleTypes.CRIT, Front.x, Front.y, Front.z, 10, 0.15, 0.2, 0.15, 0.25);
            }
            if (Source.getDirectEntity() instanceof Projectile) {
                return false;
            }
            Entity Attacker = Source.getEntity();
            if (Attacker instanceof LivingEntity Body && isPrey(Body) && distanceTo(Body) <= SlashReach + 0.6) {
                RiposteQueued = true;
            }
            return super.hurt(Source, Amount * GuardShare);
        }
        return super.hurt(Source, Amount);
    }

    private boolean blocks(DamageSource Source) {
        Vec3 From = Source.getSourcePosition();
        if (From == null) {
            return false;
        }
        Vec3 Toward = From.subtract(position()).multiply(1.0, 0.0, 1.0);
        if (Toward.lengthSqr() < 1.0e-4) {
            return true;
        }
        return Toward.normalize().dot(Vec3.directionFromRotation(0.0f, getYRot())) >= GuardCosine;
    }

    @Override
    public boolean isPushable() {
        return !charging() && super.isPushable();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 220;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return DaeviteRegistry.KnightIdle.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return DaeviteRegistry.KnightHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DaeviteRegistry.KnightDeath.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putString("Loadout", spearman() ? "spear" : "sword");
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        if (Saved.contains("Loadout")) {
            setLoadout("sword".equals(Saved.getString("Loadout")) ? Sword : Spear);
        }
    }
}
