package net.rai.scp_unleashed.scp027;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp027Rat extends Monster implements GeoEntity, Scp027Vermin {
    public static final float Width = 0.45f;
    public static final float Height = 0.35f;
    public static final float EyeHeight = 0.25f;
    public static final double MaxHealth = 6.0;
    public static final double MoveSpeed = 0.3;
    public static final double BiteDamage = 2.0;
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation BitePlay = RawAnimation.begin().thenPlay("bite");
    public static final double WalkReference = 0.12;

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Scp027Leash Leash = new Scp027Leash();

    public Scp027Rat(EntityType<? extends Scp027Rat> Type, Level World) {
        super(Type, World);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed)
                .add(Attributes.ATTACK_DAMAGE, BiteDamage)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public Scp027Leash leash() {
        return Leash;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new Scp027Goals.Flee(this, this, 1.4));
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(4, new Scp027Goals.Heel(this, this, 1.2));
        goalSelector.addGoal(5, new Scp027Goals.Mill(this, this, 0.8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new Scp027Goals.PreyTarget(this, this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Leash.tick(this);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp027Registry.RatSqueak.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp027Registry.RatHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp027Registry.RatDeath.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
        playSound(Scp027Registry.RatStep.get(), 0.18f, 1.0f);
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return Distance > 48.0 * 48.0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 3, Event -> {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            double Speed = Math.sqrt(Dx * Dx + Dz * Dz);
            if (Event.isMoving() || Speed > 0.01) {
                Event.getController().setAnimationSpeed(Math.max(0.6, Math.min(2.2, Speed / WalkReference)));
                return Event.setAndContinue(WalkLoop);
            }
            Event.getController().setAnimationSpeed(1.0);
            return Event.setAndContinue(IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "bite", 1, Event -> {
            if (swinging) {
                return Event.setAndContinue(BitePlay);
            }
            Event.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Leash.save(Saved);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Leash.load(Saved);
    }
}
