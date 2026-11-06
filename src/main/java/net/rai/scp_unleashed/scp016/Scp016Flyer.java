package net.rai.scp_unleashed.scp016;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

public class Scp016Flyer extends Scp016Spawn {
    public static final byte ActionDive = 3;
    public static final byte ActionTakeoff = 4;
    public static final double Health = 22.0;
    public static final double FlySpeed = 0.55;
    public static final double WalkSpeed = 0.2;
    public static final double Damage = 5.0;
    public static final double Armour = 2.0;
    public static final int TakeoffTicks = 12;
    public static final double TakeoffLift = 0.42;
    public static final int StrikeTicks = 10;
    public static final int DiveTicks = 40;
    public static final double DiveSpeed = 1.0;
    public static final double DiveAccel = 0.14;
    public static final double StrikeReach = 1.3;
    public static final double KnockUp = 0.55;
    public static final int FlapEvery = 12;
    public static final double HoverLift = 0.012;
    public static final double HoverFloor = 1.6;
    public static final double HoverDrag = 0.88;
    public static final float BankLimit = 28.0f;
    public static final Scp016Gait.Profile FlyerGait = new Scp016Gait.Profile(12.0, 0.0312, 0.5, 2.5, 2,
            new double[]{0.978, 0.031}, new float[]{1.0f, 0.7f},
            Scp016Registry.FlyerStep, null, null, null, 0.3f, 0.0f, 0.0f, 0.0f, 1.1f, 0.0f);
    private static final EntityDataAccessor<Boolean> PerchedData = SynchedEntityData.defineId(Scp016Flyer.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation PerchLoop = RawAnimation.begin().thenLoop("perch");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FlyLoop = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation GlideLoop = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation DiveLoop = RawAnimation.begin().thenLoop("dive");
    private static final RawAnimation ShudderLoop = RawAnimation.begin().thenLoop("shudder");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation StrikePlay = RawAnimation.begin().thenPlayAndHold("strike");
    private static final RawAnimation TakeoffPlay = RawAnimation.begin().thenPlayAndHold("takeoff");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private final Scp016Flight Flight;
    private boolean Steering;
    private float Bank;
    private float BankO;
    private int Swoops;

    public Scp016Flyer(EntityType<? extends Scp016Flyer> Type, Level World) {
        super(Type, World);
        xpReward = 9;
        Flight = new Scp016Flight(this);
        moveControl = Flight;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, WalkSpeed).add(Attributes.FLYING_SPEED, FlySpeed)
                .add(Attributes.ATTACK_DAMAGE, Damage).add(Attributes.ARMOR, Armour).add(Attributes.FOLLOW_RANGE, Follow).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public String variant() {
        return "flyer";
    }

    @Override
    public boolean hasDeathAnimation() {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(PerchedData, false);
    }

    @Override
    protected void registerGoals() {
        registerKinGoals(false, true);
        goalSelector.addGoal(2, new Scp016Goals.Swoop(this));
        goalSelector.addGoal(5, new Scp016Goals.Perch(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 0.8));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        FlyingPathNavigation Navigation = new FlyingPathNavigation(this, World);
        Navigation.setCanOpenDoors(false);
        Navigation.setCanFloat(true);
        Navigation.setCanPassDoors(true);
        return Navigation;
    }

    public Scp016Flight flight() {
        return Flight;
    }

    public boolean isPerched() {
        return entityData.get(PerchedData);
    }

    public boolean steering() {
        return Steering;
    }

    public void setSteering(boolean Value) {
        Steering = Value;
    }

    public void land() {
        entityData.set(PerchedData, true);
        setNoGravity(false);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        playSound(Scp016Registry.FlyerStep.get(), 0.7f, 0.8f);
    }

    public void takeOff() {
        if (!isPerched()) {
            return;
        }
        entityData.set(PerchedData, false);
        setNoGravity(true);
        setDeltaMovement(getDeltaMovement().add(0.0, TakeoffLift, 0.0));
        hasImpulse = true;
        if (!isActing()) {
            setAction(ActionTakeoff, TakeoffTicks);
        }
        playSound(Scp016Registry.FlyerTakeoff.get(), 1.0f, 0.9f + random.nextFloat() * 0.1f);
    }

    public void hover() {
        if (isPerched()) {
            return;
        }
        Vec3 Motion = getDeltaMovement().multiply(HoverDrag, HoverDrag, HoverDrag);
        double Gap = groundGap();
        double Bob = Math.sin((tickCount + getId()) * 0.15) * HoverLift;
        if (Gap < HoverFloor) {
            Bob += HoverLift * 2.5;
        }
        setDeltaMovement(Motion.add(0.0, Bob, 0.0));
    }

    public double groundGap() {
        BlockPos Pos = blockPosition();
        for (int Depth = 0; Depth <= 6; Depth++) {
            BlockPos Below = Pos.below(Depth);
            BlockState State = level().getBlockState(Below);
            if (!State.getCollisionShape(level(), Below).isEmpty() || !level().getFluidState(Below).isEmpty()) {
                return getY() - (Below.getY() + Math.max(State.getCollisionShape(level(), Below).isEmpty() ? 0.9 : State.getCollisionShape(level(), Below).max(Direction.Axis.Y), 0.0));
            }
        }
        return 7.0;
    }

    public void beginDive() {
        setAction(ActionDive, DiveTicks);
        playSound(Scp016Registry.FlyerSwoop.get(), 1.2f, 0.85f + random.nextFloat() * 0.1f);
    }

    public boolean strike(LivingEntity Target) {
        setAction(ActionStrike, StrikeTicks);
        swing(InteractionHand.MAIN_HAND);
        boolean Landed = doHurtTarget(Target);
        float Pitch = 0.9f + random.nextFloat() * 0.15f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp016Registry.StrikeLayers) {
            level().playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.HOSTILE, 1.0f, Pitch);
        }
        if (Landed) {
            Target.setDeltaMovement(Target.getDeltaMovement().multiply(1.0, 0.0, 1.0).add(0.0, KnockUp, 0.0));
            Target.hurtMarked = true;
            Swoops++;
        }
        return Landed;
    }

    public double strikeReach(LivingEntity Target) {
        return StrikeReach + (getBbWidth() + Target.getBbWidth()) * 0.5;
    }

    @Override
    public boolean locksMovement() {
        return isEmerging();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        BankO = Bank;
        float Turning = Mth.wrapDegrees(getYRot() - yRotO);
        Bank = Mth.lerp(0.25f, Bank, isPerched() ? 0.0f : Mth.clamp(Turning * 2.0f, -BankLimit, BankLimit));
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (isPerched()) {
            setXRot(Mth.approachDegrees(getXRot(), 0.0f, 8.0f));
            if (!onGround() && getDeltaMovement().y < -0.2) {
                takeOff();
            }
        } else if (!isNoGravity()) {
            setNoGravity(true);
        }
        if (!isPerched() && !isEmerging() && (tickCount + getId()) % FlapEvery == 0 && action() != ActionDive) {
            playSound(Scp016Registry.FlyerFlap.get(), 0.6f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    @Override
    public void beginEmerging() {
        super.beginEmerging();
        entityData.set(PerchedData, true);
        setNoGravity(false);
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind == ActionEmerge && isPerched() && getTarget() != null) {
            takeOff();
        }
    }

    @Override
    public float bodyPitch(float Partial) {
        return Mth.lerp(Partial, xRotO, getXRot());
    }

    @Override
    public float bodyRoll(float Partial) {
        return Mth.lerp(Partial, BankO, Bank);
    }

    public int swoops() {
        return Swoops;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide && isPerched() && !isEmerging()) {
            takeOff();
        }
        return Hurt;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double Dy, boolean Grounded, BlockState State, BlockPos Pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp016Registry.FlyerAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp016Registry.FlyerHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp016Registry.FlyerDeath.get();
    }

    @Override
    protected Scp016Gait.Profile gaitProfile() {
        return FlyerGait;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putBoolean("Perched", isPerched());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(PerchedData, Tag.getBoolean("Perched"));
        setNoGravity(!isPerched());
        if (action() != ActionEmerge) {
            setAction(ActionNone, 0);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp016GaitController<>(this, "body", 3, Event -> {
            if (isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionStrike) {
                return Event.setAndContinue(StrikePlay);
            }
            if (Current == ActionDive) {
                return Event.setAndContinue(DiveLoop);
            }
            if (Current == ActionTakeoff) {
                return Event.setAndContinue(TakeoffPlay);
            }
            if (isPerched()) {
                if (isCollapsing()) {
                    return Event.setAndContinue(ShudderLoop);
                }
                return Event.setAndContinue(isStriding() ? WalkLoop : PerchLoop);
            }
            double Sink = getY() - yo;
            return Event.setAndContinue(Sink < -0.12 && Math.abs(getX() - xo) + Math.abs(getZ() - zo) > 0.2 ? GlideLoop : FlyLoop);
        }));
    }

    public boolean canStrike(@Nullable LivingEntity Target) {
        return Target != null && Target.isAlive() && !(Target instanceof Player User && (User.isCreative() || User.isSpectator()));
    }
}
