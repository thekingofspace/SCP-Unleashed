package net.rai.scp_unleashed.scp016;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class Scp016Skitter extends Scp016Spawn {
    public static final double Health = 14.0;
    public static final double Speed = 0.31;
    public static final double Damage = 3.0;
    public static final double Armour = 2.0;
    public static final float LeapLift = 0.38f;
    public static final Scp016Gait.Profile SkitterGait = new Scp016Gait.Profile(8.0, 0.0651, 0.5, 3.0, 2,
            new double[]{0.979, 0.536}, new float[]{1.0f, 1.0f},
            Scp016Registry.SkitterStep, Scp016Registry.SkitterTick, null, null, 0.3f, 0.2f, 0.0f, 0.0f, 1.0f, 0.0f);
    private static final EntityDataAccessor<Boolean> Climbing = SynchedEntityData.defineId(Scp016Skitter.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ClimbLoop = RawAnimation.begin().thenLoop("climb");
    private static final RawAnimation ShudderLoop = RawAnimation.begin().thenLoop("shudder");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation AttackOnce = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation LeapOnce = RawAnimation.begin().thenPlay("leap");

    public Scp016Skitter(EntityType<? extends Scp016Skitter> Type, Level World) {
        super(Type, World);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.ARMOR, Armour).add(Attributes.FOLLOW_RANGE, Follow).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public String variant() {
        return "skitter";
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(Climbing, false);
    }

    @Override
    protected void registerGoals() {
        registerKinGoals();
        goalSelector.addGoal(2, new Scp016Goals.Leap(this, LeapLift));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new WallClimberNavigation(this, World);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            entityData.set(Climbing, horizontalCollision && !locksMovement());
        }
    }

    public boolean isClimbing() {
        return entityData.get(Climbing);
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    @Override
    public void makeStuckInBlock(BlockState State, Vec3 Multiplier) {
        if (!State.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(State, Multiplier);
        }
    }

    public void leapt() {
        triggerAnim("action", "leap");
        playSound(Scp016Registry.SkitterAttack.get(), 0.7f, 1.25f + random.nextFloat() * 0.15f);
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Landed = super.doHurtTarget(Target);
        if (Landed) {
            triggerAnim("action", "attack");
            playSound(Scp016Registry.SkitterAttack.get(), 0.9f, 1.0f + random.nextFloat() * 0.2f);
        }
        return Landed;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp016Registry.SkitterAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp016Registry.SkitterHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp016Registry.SkitterDeath.get();
    }

    @Override
    protected Scp016Gait.Profile gaitProfile() {
        return SkitterGait;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(Climbing, false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp016GaitController<>(this, "body", 3, Event -> {
            if (isEmerging()) {
                return Event.setAndContinue(EmergePlay);
            }
            if (isClimbing()) {
                return Event.setAndContinue(ClimbLoop);
            }
            if (isStriding()) {
                return Event.setAndContinue(WalkLoop);
            }
            return Event.setAndContinue(isCollapsing() ? ShudderLoop : IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("attack", AttackOnce).triggerableAnim("leap", LeapOnce));
    }
}
