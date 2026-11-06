package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.HumanoidGait;
import net.rai.scp_unleashed.dclass.HumanoidGaitController;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class Scp457 extends DClassLike implements ControlRefusing, Enemy {
    public static final float Width = 0.7f;
    public static final float Height = 2.4f;
    public static final float EyeHeight = 2.25f;
    public static final double MaxHealth = 60.0;
    public static final double Armour = 4.0;
    public static final double Speed = 0.28;
    public static final double AttackDamage = 6.0;
    public static final double Follow = 32.0;
    public static final double Steadiness = 0.5;
    public static final double Stride = 1.0;
    public static final double ChaseSpeed = 1.15;
    public static final double StrollSpeed = 0.7;
    public static final double ShelterSpeed = 1.3;
    public static final double FleeWalk = 1.15;
    public static final double FleeSprint = 1.4;
    public static final float FleeRange = 7.0f;
    public static final float IgniteSeconds = 6.0f;
    public static final int RiseTicks = 30;
    public static final int CollapseTicks = 36;
    public static final int RainInterval = 10;
    public static final float RainDamage = 10.0f;
    public static final float SnowballDamage = 6.0f;
    public static final int HealInterval = 10;
    public static final float FireHeal = 1.0f;
    public static final int RegenDelay = 200;
    public static final int RegenInterval = 40;
    public static final float RegenAmount = 1.0f;
    public static final float WaterBorderCost = 16.0f;
    public static final float RainCost = 32.0f;
    public static final int AmbientInterval = 240;
    public static final int BreathCalm = 64;
    public static final int BreathHunting = 44;
    public static final int ExhaleCalm = 26;
    public static final int ExhaleHunting = 18;
    public static final float BreathVolume = 1.0f;
    public static final float ExhalePitch = 0.86f;
    public static final float StepVolume = 0.4f;
    public static final float RunStepVolume = 0.5f;
    public static final double StepSpread = 0.14;
    public static final int Experience = 20;
    public static final int FlameFrames = 8;
    public static final int FrameTicks = 2;
    public static final byte PhaseActive = 0;
    public static final byte PhaseRising = 1;
    public static final byte DeathNone = 0;
    public static final byte DeathFlames = 1;
    public static final byte DeathWater = 2;
    public static final byte DeathFrost = 3;
    public static final RawAnimation ClawPlay = RawAnimation.begin().thenPlay("claw");
    public static final RawAnimation RisePlay = RawAnimation.begin().thenPlayAndHold("rise");
    public static final RawAnimation CollapsePlay = RawAnimation.begin().thenPlayAndHold("collapse");
    protected static final EntityDataAccessor<Byte> PhaseData = SynchedEntityData.defineId(Scp457.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> DeathData = SynchedEntityData.defineId(Scp457.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Boolean> SteamingData = SynchedEntityData.defineId(Scp457.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> PrimeData = SynchedEntityData.defineId(Scp457.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Byte> ChillData = SynchedEntityData.defineId(Scp457.class, EntityDataSerializers.BYTE);

    private int PhaseLeft;
    @Nullable
    private Matrix4f[] BonePoses;
    private int PosesTick = -1000;
    private boolean Hushed;
    private double LastCycle = -1.0;
    private int NextBreath;
    private int ExhaleAt;
    private int RainTicks;
    private int LastHurt = -10000;
    private int LocalPhaseStart;
    private float RenderPartial;

    public Scp457(EntityType<? extends Scp457> Type, Level World) {
        super(Type, World);
        setPathfindingMalus(PathType.WATER, -1.0f);
        setPathfindingMalus(PathType.WATER_BORDER, WaterBorderCost);
        setPathfindingMalus(Scp457Navigation.RainType, RainCost);
        setPathfindingMalus(PathType.DANGER_FIRE, 0.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0f);
        xpReward = Experience;
        if (getNavigation() instanceof Scp457Navigation Ground) {
            Ground.setCanOpenDoors(true);
            Ground.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.ARMOR, Armour)
                .add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.ATTACK_DAMAGE, AttackDamage)
                .add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.KNOCKBACK_RESISTANCE, Steadiness)
                .add(Attributes.STEP_HEIGHT, Stride);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp457Navigation(this, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(PhaseData, PhaseActive);
        Builder.define(DeathData, DeathNone);
        Builder.define(SteamingData, false);
        Builder.define(PrimeData, false);
        Builder.define(ChillData, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new Scp457Goals.Shelter(this));
        goalSelector.addGoal(1, new Scp457Goals.FleeWater(this));
        goalSelector.addGoal(1, new Scp457Cold.FleeCold(this));
        goalSelector.addGoal(2, new Scp457Goals.Hunt(this));
        goalSelector.addGoal(4, new OpenDoorGoal(this, false));
        goalSelector.addGoal(5, new Scp457Goals.Wander(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Scp457.class));
        targetSelector.addGoal(2, new Scp457Goals.Prey(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        if (Scp457Prime.makesPrime(Reason)) {
            setPrime(true);
        }
        return Result;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.scp457.refuses");
    }

    public byte phase() {
        return entityData.get(PhaseData);
    }

    public boolean isRising() {
        return phase() == PhaseRising;
    }

    public byte deathKind() {
        return entityData.get(DeathData);
    }

    public boolean isSteaming() {
        return entityData.get(SteamingData);
    }

    public boolean isPrime() {
        return entityData.get(PrimeData);
    }

    public void setPrime(boolean Prime) {
        entityData.set(PrimeData, Prime);
    }

    public void rekindle(Vec3 Spot, float Yaw) {
        setHealth(getMaxHealth());
        deathTime = 0;
        hurtTime = 0;
        dead = false;
        fallDistance = 0.0f;
        entityData.set(DeathData, DeathNone);
        entityData.set(SteamingData, false);
        setPose(Pose.STANDING);
        setPrime(true);
        clearFire();
        setTarget(null);
        setDeltaMovement(Vec3.ZERO);
        moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
        setPersistenceRequired();
        beginRise();
    }

    public void beginRise() {
        PhaseLeft = RiseTicks;
        LocalPhaseStart = tickCount;
        entityData.set(PhaseData, PhaseRising);
        lockGoals(true);
    }

    private void lockGoals(boolean Locked) {
        for (Goal.Flag Flag : new Goal.Flag[]{Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP}) {
            goalSelector.setControlFlag(Flag, !Locked);
        }
        if (Locked) {
            getNavigation().stop();
        }
    }

    public void setBonePoses(Matrix4f[] Poses) {
        BonePoses = Poses;
        PosesTick = tickCount;
    }

    @Nullable
    public Matrix4f[] bonePoses() {
        return BonePoses != null && tickCount - PosesTick <= Scp457Effects.EmitterTimeout ? BonePoses : null;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float flameStrength(float Partial) {
        if (deathTime > 0 || isDeadOrDying()) {
            float Faded = 1.0f - (deathTime + Partial) / (CollapseTicks * 0.75f);
            return Mth.clamp(Faded * (deathKind() == DeathWater || deathKind() == DeathFrost ? 0.6f : 1.0f), 0.0f, 1.0f);
        }
        if (isRising()) {
            return Mth.clamp(0.25f + (tickCount - LocalPhaseStart + Partial) / RiseTicks, 0.25f, 1.0f);
        }
        return Math.min(isSteaming() ? 0.7f : 1.0f, Scp457Cold.flameScale(this));
    }

    public int flameFrame() {
        return Math.floorMod((tickCount + getId() * 3) / FrameTicks, FlameFrames);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (PhaseData.equals(Accessor)) {
            LocalPhaseStart = tickCount;
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
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
    public float getWalkTargetValue(BlockPos Pos, LevelReader World) {
        if (!World.getFluidState(Pos).isEmpty() || !World.getFluidState(Pos.below()).isEmpty()) {
            return -20.0f;
        }
        if (Scp457Cold.chilly(World, Pos)) {
            return -10.0f;
        }
        if (level().isRaining() && level().isRainingAt(Pos)) {
            return -10.0f;
        }
        return 0.0f;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level() instanceof ServerLevel World && isAlive() && !Source.is(Scp457Registry.DousedDamage)) {
            if (Scp457Water.isWaterSource(Source)) {
                douse(World);
                return true;
            }
            if (Source.getDirectEntity() instanceof Snowball) {
                return wetten(World, SnowballDamage);
            }
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt) {
            LastHurt = tickCount;
        }
        return Hurt;
    }

    public void douse(ServerLevel World) {
        if (isDeadOrDying() || isRemoved()) {
            return;
        }
        entityData.set(DeathData, DeathWater);
        Scp457Effects.steamBurst(World, this);
        DamageSource Source = damageSources().source(Scp457Registry.DousedDamage);
        super.hurt(Source, Float.MAX_VALUE);
        if (!isDeadOrDying()) {
            setHealth(0.0f);
            die(Source);
        }
    }

    public boolean wetten(ServerLevel World, float Amount) {
        if (isDeadOrDying() || isRemoved()) {
            return false;
        }
        if (Amount >= getHealth()) {
            entityData.set(DeathData, DeathWater);
            Scp457Effects.steamBurst(World, this);
        } else {
            Scp457Effects.hiss(World, this);
        }
        boolean Hurt = super.hurt(damageSources().source(Scp457Registry.DousedDamage), Amount);
        if (Hurt) {
            LastHurt = tickCount;
        }
        return Hurt;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (!Scp457Water.isWaterItem(Held) || !isAlive()) {
            return super.mobInteract(User, Hand);
        }
        if (level() instanceof ServerLevel World) {
            if (!User.getAbilities().instabuild) {
                ItemStack Empty = new ItemStack(Held.getItem() instanceof BucketItem ? Items.BUCKET : Items.GLASS_BOTTLE);
                User.setItemInHand(Hand, ItemUtils.createFilledResult(Held, User, Empty));
            }
            World.playSound(null, getX(), getY(), getZ(), Held.getItem() instanceof BucketItem ? SoundEvents.BUCKET_EMPTY : SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0f, 1.0f);
            douse(World);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        if (!(level() instanceof ServerLevel World)) {
            return false;
        }
        float Amount = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        triggerAnim("action", "claw");
        Scp457Effects.swipe(World, this);
        boolean Hit = Target.hurt(damageSources().source(Scp457Registry.BurnDamage, this), Amount);
        if (Hit) {
            Target.igniteForSeconds(IgniteSeconds);
            if (Target instanceof LivingEntity Living) {
                Scp457Spread.mark(World, Living);
                Vec3 Push = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                if (Push.lengthSqr() > 1.0e-4) {
                    Living.knockback(0.35, -Push.x, -Push.z);
                }
            }
            Scp457Effects.ignite(World, Target);
            setLastHurtMob(Target);
        }
        return Hit;
    }

    @Override
    public void die(DamageSource Source) {
        if (entityData.get(DeathData) == DeathNone) {
            entityData.set(DeathData, DeathFlames);
        }
        if (isPrime() && !level().isClientSide) {
            smoulder();
            return;
        }
        super.die(Source);
    }

    private void smoulder() {
        if (isRemoved()) {
            return;
        }
        getNavigation().stop();
        setTarget(null);
        setPose(Pose.DYING);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel World) {
            Scp457Effects.collapsing(World, this, deathTime);
            if (deathTime >= CollapseTicks && !isRemoved()) {
                Scp457Effects.ashes(World, this);
                if (isPrime()) {
                    Scp457Prime.settle(World, this);
                    remove(Entity.RemovalReason.DISCARDED);
                    return;
                }
                remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp457Effects.clientTick(this);
            return;
        }
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (Scp457Water.touching(this)) {
            douse(World);
            return;
        }
        if (Scp457Cold.tick(World, this)) {
            return;
        }
        boolean Rained = Scp457Water.rainedOn(this);
        if (Rained != isSteaming()) {
            entityData.set(SteamingData, Rained);
        }
        if (Rained) {
            RainTicks++;
            if (RainTicks % RainInterval == 0) {
                if (isPrime()) {
                    douse(World);
                    return;
                }
                wetten(World, RainDamage);
            }
        } else {
            RainTicks = 0;
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (isRising()) {
            getNavigation().stop();
            setDeltaMovement(0.0, Math.min(0.0, getDeltaMovement().y), 0.0);
            PhaseLeft--;
            if (PhaseLeft <= 0) {
                entityData.set(PhaseData, PhaseActive);
                lockGoals(false);
            }
            return;
        }
        tickMending(World);
    }

    private void tickMending(ServerLevel World) {
        if (getHealth() >= getMaxHealth() || isSteaming()) {
            return;
        }
        boolean Burning = isInLava() || World.getBlockState(blockPosition()).is(BlockTags.FIRE);
        if (Burning && tickCount % HealInterval == 0) {
            heal(FireHeal);
        } else if (tickCount - LastHurt > RegenDelay && tickCount % RegenInterval == 0) {
            heal(RegenAmount);
        }
    }

    @Override
    public void travel(Vec3 Input) {
        if (!level().isClientSide && isAlive() && Scp457Water.wouldWade(this, Input)) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            getNavigation().stop();
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(Input);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp457Registry.Rasp.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public boolean isSilent() {
        return Hushed || super.isSilent();
    }

    @Override
    public void tickLook() {
        Hushed = true;
        try {
            super.tickLook();
        } finally {
            Hushed = false;
        }
        if (!isAlive() || isDeadOrDying() || super.isSilent()) {
            LastCycle = -1.0;
            return;
        }
        tickFootsteps();
        tickBreathing();
    }

    private void tickFootsteps() {
        HumanoidGait Current = gait();
        if (Current == null || !Current.striding() || !onGround()) {
            LastCycle = -1.0;
            return;
        }
        setGaitPartial(1.0f);
        double Cycle = Current.cycle();
        if (LastCycle >= 0.0) {
            for (int Foot = 0; Foot < 2; Foot++) {
                if (crossed(LastCycle, Cycle, Foot * 0.5)) {
                    footstep(Foot == 0, Current.running());
                }
            }
        }
        LastCycle = Cycle;
    }

    public static boolean crossed(double Before, double After, double Contact) {
        if (After >= Before) {
            return Before < Contact && Contact <= After;
        }
        return Contact > Before || Contact <= After;
    }

    private void footstep(boolean Right, boolean Running) {
        float Yaw = yBodyRot * Mth.DEG_TO_RAD;
        double Side = Right ? -StepSpread : StepSpread;
        double X = getX() + Mth.cos(Yaw) * Side;
        double Z = getZ() + Mth.sin(Yaw) * Side;
        float Volume = Running ? RunStepVolume : StepVolume;
        float Pitch = (Running ? 1.05f : 1.0f) * (0.92f + getRandom().nextFloat() * 0.14f);
        level().playLocalSound(X, getY(), Z, Scp457Registry.Step.get(), getSoundSource(), Volume, Pitch, false);
        level().playLocalSound(X, getY(), Z, Scp457Registry.StepAsh.get(), getSoundSource(), Volume * 0.6f, Pitch * (0.95f + getRandom().nextFloat() * 0.1f), false);
    }

    private void tickBreathing() {
        boolean Hunting = isAggressive();
        if (tickCount >= NextBreath) {
            breathe(1.0f);
            ExhaleAt = tickCount + (Hunting ? ExhaleHunting : ExhaleCalm);
            NextBreath = tickCount + (Hunting ? BreathHunting : BreathCalm) + getRandom().nextInt(9);
        }
        if (ExhaleAt > 0 && tickCount >= ExhaleAt) {
            breathe(ExhalePitch);
            ExhaleAt = 0;
        }
    }

    private void breathe(float Pitch) {
        double Y = getY() + getBbHeight() * 0.85;
        float Shift = Pitch * (0.97f + getRandom().nextFloat() * 0.06f);
        level().playLocalSound(getX(), Y, getZ(), Scp457Registry.Breath.get(), getSoundSource(), BreathVolume, Shift, false);
        level().playLocalSound(getX(), Y, getZ(), Scp457Registry.BreathLow.get(), getSoundSource(), BreathVolume * 0.7f, Shift, false);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp457Registry.Hurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        super.playHurtSound(Source);
        if (!isSilent()) {
            level().playSound(null, getX(), getY(), getZ(), Scp457Registry.HurtBurn.get(), getSoundSource(), getSoundVolume() * 0.7f, getVoicePitch());
        }
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return deathKind() == DeathWater || deathKind() == DeathFrost ? null : Scp457Registry.Gutter.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected boolean shouldDropLoot() {
        return true;
    }

    protected RawAnimation bodyAnimation() {
        if (isDeadOrDying()) {
            return CollapsePlay;
        }
        if (isRising()) {
            return RisePlay;
        }
        if (isStriding()) {
            return isRunning() ? RunLoop : WalkLoop;
        }
        return IdleLoop;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new HumanoidGaitController<>(this, "body", 4, Event -> Event.setAndContinue(bodyAnimation())));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("claw", ClawPlay));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putByte("Phase", phase());
        Saved.putInt("PhaseLeft", PhaseLeft);
        Saved.putBoolean("Prime", isPrime());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        if (Saved.getByte("Phase") == PhaseRising && Saved.getInt("PhaseLeft") > 0) {
            beginRise();
            PhaseLeft = Saved.getInt("PhaseLeft");
        }
        setPrime(Saved.getBoolean("Prime"));
        setPersistenceRequired();
    }
}
