package net.rai.scp_unleashed.scp016;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class Scp016Swimmer extends Scp016Spawn {
    public static final byte ActionDrag = 3;
    public static final byte ActionBreach = 4;
    public static final double Health = 26.0;
    public static final double Speed = 1.15;
    public static final double Damage = 5.0;
    public static final double Armour = 2.0;
    public static final float WaterShare = 0.02f;
    public static final float LandShare = 0.1f;
    public static final int BiteTicks = 10;
    public static final int BiteCooldown = 20;
    public static final double BiteReach = 2.0;
    public static final int DragTicks = 50;
    public static final int DragCooldown = 160;
    public static final double DragReach = 2.4;
    public static final double DragHold = 1.0;
    public static final double DragPull = 0.45;
    public static final double DragSink = 0.07;
    public static final double DragLimit = 3.5;
    public static final float DragBreak = 6.0f;
    public static final int BreachTicks = 16;
    public static final double BreachRange = 5.0;
    public static final double BreachSpeed = 0.75;
    public static final double BreachLift = 0.5;
    public static final double BreachReach = 1.6;
    public static final int BreachCooldown = 80;
    public static final double ShoreRange = 3.0;
    public static final int DryGrace = 600;
    public static final int DryEvery = 40;
    public static final float DryDamage = 1.0f;
    public static final int FlopEvery = 16;
    public static final double FlopLift = 0.26;
    public static final double FlopPush = 0.12;
    public static final int WaterSearch = 16;
    public static final Scp016Gait.Profile SwimmerGait = new Scp016Gait.Profile(16.0, 0.03, 0.5, 2.0, 2,
            new double[]{0.25, 0.75}, new float[]{1.0f, 0.8f},
            Scp016Registry.SwimmerFlop, null, null, null, 0.45f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
    private static final RawAnimation SwimLoop = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation HoverLoop = RawAnimation.begin().thenLoop("hover");
    private static final RawAnimation BeachedLoop = RawAnimation.begin().thenLoop("beached");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ShudderLoop = RawAnimation.begin().thenLoop("shudder");
    private static final RawAnimation DragLoop = RawAnimation.begin().thenLoop("drag");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation BitePlay = RawAnimation.begin().thenPlayAndHold("bite");
    private static final RawAnimation BreachPlay = RawAnimation.begin().thenPlayAndHold("breach");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    @Nullable
    private LivingEntity Held;
    @Nullable
    private LivingEntity Leapt;
    private int BiteWait;
    private int DragWait;
    private int BreachWait;
    private int Dry;

    public Scp016Swimmer(EntityType<? extends Scp016Swimmer> Type, Level World) {
        super(Type, World);
        xpReward = 9;
        setPathfindingMalus(PathType.WATER, 0.0f);
        moveControl = new SmoothSwimmingMoveControl(this, 85, 10, WaterShare, LandShare, true);
        lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.ARMOR, Armour).add(Attributes.FOLLOW_RANGE, Follow).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public String variant() {
        return "swimmer";
    }

    @Override
    public boolean hasDeathAnimation() {
        return true;
    }

    @Override
    protected void registerGoals() {
        registerKinGoals(false, true);
        goalSelector.addGoal(2, new Scp016Goals.Ambush(this));
        goalSelector.addGoal(3, new Scp016Goals.ReturnToWater(this, WaterSearch, 1.0));
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 10));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new AmphibiousPathNavigation(this, World);
    }

    @Override
    public boolean wantsQuarry(LivingEntity Target) {
        return isQuarry(Target) && (Target.isInWater() || distanceTo(Target) <= ShoreRange || nearWater(Target, ShoreRange));
    }

    public boolean nearWater(Entity Target, double Range) {
        BlockPos Feet = Target.blockPosition();
        int Reach = Mth.ceil(Range);
        for (BlockPos Pos : BlockPos.betweenClosed(Feet.offset(-Reach, -2, -Reach), Feet.offset(Reach, 1, Reach))) {
            if (level().getFluidState(Pos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean locksMovement() {
        return isEmerging();
    }

    @Override
    public boolean canDrownInFluidType(FluidType Fluid) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 Input) {
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), Input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(Input);
        }
    }

    public boolean readyToBite() {
        return BiteWait <= 0 && !isActing();
    }

    public boolean readyToDrag() {
        return DragWait <= 0 && !isActing();
    }

    public boolean readyToBreach() {
        return BreachWait <= 0 && !isActing() && isInWater();
    }

    public void bite(LivingEntity Target) {
        BiteWait = BiteCooldown;
        setAction(ActionStrike, BiteTicks);
        playSound(Scp016Registry.SwimmerBite.get(), 1.0f, 0.9f + random.nextFloat() * 0.15f);
        swing(InteractionHand.MAIN_HAND);
        doHurtTarget(Target);
    }

    public void beginDrag(LivingEntity Target) {
        Held = Target;
        setAction(ActionDrag, DragTicks);
        sounds(Scp016Registry.DragLayers);
        swing(InteractionHand.MAIN_HAND);
        doHurtTarget(Target);
    }

    public void beginBreach(LivingEntity Target) {
        Leapt = Target;
        BreachWait = BreachCooldown;
        setAction(ActionBreach, BreachTicks);
        Vec3 Toward = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        Vec3 Push = Toward.lengthSqr() > 1.0E-4 ? Toward.normalize().scale(BreachSpeed) : Vec3.ZERO;
        setDeltaMovement(Push.x, BreachLift, Push.z);
        hasImpulse = true;
        float Facing = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Facing);
        setYBodyRot(Facing);
        sounds(Scp016Registry.BreachLayers);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.4, getZ(), 24, 0.4, 0.2, 0.4, 0.2);
        }
    }

    public boolean breachWindow(LivingEntity Target) {
        double Flat = Math.sqrt(Target.distanceToSqr(getX(), Target.getY(), getZ()));
        return Flat <= BreachRange && Target.getY() >= getY() - 0.5 && Target.getY() - getY() < 3.0 && !Target.isInWater() && getSensing().hasLineOfSight(Target);
    }

    public double biteReach(LivingEntity Target) {
        return BiteReach + Target.getBbWidth() * 0.5;
    }

    public double dragReach(LivingEntity Target) {
        return DragReach + Target.getBbWidth() * 0.5;
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind == ActionDrag) {
            dragTick();
        } else if (Kind == ActionBreach && Leapt != null && Leapt.isAlive() && distanceTo(Leapt) <= BreachReach + Leapt.getBbWidth() * 0.5) {
            swing(InteractionHand.MAIN_HAND);
            doHurtTarget(Leapt);
            playSound(Scp016Registry.SwimmerBite.get(), 1.0f, 0.85f);
            Leapt = null;
        }
    }

    private void dragTick() {
        LivingEntity Target = Held;
        if (Target == null || !Target.isAlive() || !Targeting.isTargetable(Target) || !Target.isInWater() || distanceTo(Target) > DragLimit || Target instanceof Player User && (User.isCreative() || User.isSpectator())) {
            letGo();
            return;
        }
        float Yaw = yBodyRot * Mth.DEG_TO_RAD;
        Vec3 Jaw = position().add(-Mth.sin(Yaw) * DragHold, getBbHeight() * 0.5, Mth.cos(Yaw) * DragHold);
        Vec3 Wanted = Jaw.subtract(0.0, Target.getBbHeight() * 0.5, 0.0);
        Vec3 Pull = Wanted.subtract(Target.position()).scale(DragPull).add(0.0, -DragSink, 0.0);
        Target.setDeltaMovement(Pull);
        Target.hurtMarked = true;
        Target.fallDistance = 0.0f;
        setDeltaMovement(getDeltaMovement().multiply(0.6, 0.0, 0.6).add(0.0, -DragSink, 0.0));
        if ((tickCount + getId()) % 10 == 0 && level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.BUBBLE, Target.getX(), Target.getEyeY(), Target.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private void letGo() {
        Held = null;
        if (action() == ActionDrag) {
            setAction(ActionNone, 0);
        }
        DragWait = DragCooldown;
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind == ActionDrag) {
            Held = null;
            DragWait = DragCooldown;
        } else if (Kind == ActionBreach) {
            Leapt = null;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (BiteWait > 0) {
            BiteWait--;
        }
        if (DragWait > 0 && action() != ActionDrag) {
            DragWait--;
        }
        if (BreachWait > 0) {
            BreachWait--;
        }
        if (isInWaterRainOrBubble()) {
            Dry = 0;
        } else {
            Dry++;
            if (Dry > DryGrace && Dry % DryEvery == 0) {
                hurt(damageSources().dryOut(), DryDamage);
            }
        }
        if (!isInWater()) {
            setXRot(Mth.approachDegrees(getXRot(), 0.0f, 6.0f));
            if (onGround() && !isActing() && !getNavigation().isDone() && (tickCount + getId()) % FlopEvery == 0) {
                float Yaw = getYRot() * Mth.DEG_TO_RAD;
                setDeltaMovement(getDeltaMovement().add(-Mth.sin(Yaw) * FlopPush, FlopLift, Mth.cos(Yaw) * FlopPush));
                hasImpulse = true;
            }
        } else if (isActing() && action() == ActionStrike) {
            setDeltaMovement(getDeltaMovement().scale(0.8));
        }
        if (isInWater() && (tickCount + getId()) % 40 == 0 && getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            playSound(Scp016Registry.SwimmerSwim.get(), 0.5f, 0.8f + random.nextFloat() * 0.2f);
        }
    }

    public int dryTicks() {
        return Dry;
    }

    @Override
    public float bodyPitch(float Partial) {
        return Mth.lerp(Partial, xRotO, getXRot());
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && Held != null && Amount >= DragBreak && !level().isClientSide) {
            letGo();
        }
        return Hurt;
    }

    private void sounds(List<DeferredHolder<SoundEvent, SoundEvent>> Layers) {
        float Pitch = 0.9f + random.nextFloat() * 0.15f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.HOSTILE, 1.0f, Pitch);
        }
    }

    @Override
    public int getMaxHeadXRot() {
        return 1;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp016Registry.SwimmerAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp016Registry.SwimmerHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp016Registry.SwimmerDeath.get();
    }

    @Override
    protected Scp016Gait.Profile gaitProfile() {
        return SwimmerGait;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Dry", Dry);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Dry = Tag.getInt("Dry");
        if (action() != ActionEmerge) {
            setAction(ActionNone, 0);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp016GaitController<>(this, "body", 4, Event -> {
            if (isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionDrag) {
                return Event.setAndContinue(DragLoop);
            }
            if (Current == ActionBreach) {
                return Event.setAndContinue(BreachPlay);
            }
            if (Current == ActionStrike) {
                return Event.setAndContinue(BitePlay);
            }
            if (isCollapsing()) {
                return Event.setAndContinue(ShudderLoop);
            }
            if (isInWater()) {
                return Event.setAndContinue(getDeltaMovement().lengthSqr() > 0.0025 ? SwimLoop : HoverLoop);
            }
            return Event.setAndContinue(isStriding() ? WalkLoop : BeachedLoop);
        }));
    }
}
