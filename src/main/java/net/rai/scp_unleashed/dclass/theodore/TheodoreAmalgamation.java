package net.rai.scp_unleashed.dclass.theodore;

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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

public class TheodoreAmalgamation extends Monster implements GeoEntity {
    public static final byte ActionNone = 0;
    public static final byte ActionSwipe = 1;
    public static final byte ActionSlam = 2;
    public static final byte ActionRise = 3;
    public static final int SwipeTicks = 14;
    public static final int SlamTicks = 24;
    public static final int SlamImpact = 12;
    public static final int RiseTicks = 30;
    public static final int SlamCooldownMin = 120;
    public static final int SlamCooldownSpread = 60;
    public static final int DeathTicks = 32;
    public static final int Reward = 60;
    public static final double SlamRange = 4.5;
    public static final double SlamTrigger = 3.8;
    public static final float SlamDamage = 10.0f;
    public static final int Heads = 8;
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation SwipePlay = RawAnimation.begin().thenPlay("swipe");
    public static final RawAnimation SlamPlay = RawAnimation.begin().thenPlay("slam");
    public static final RawAnimation RisePlay = RawAnimation.begin().thenPlay("rise");
    public static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    protected static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(TheodoreAmalgamation.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int ActionLeft;
    private int SlamCooldown = SlamCooldownMin;
    @Nullable
    private AmalgamationEyes Eyes;

    public TheodoreAmalgamation(EntityType<? extends TheodoreAmalgamation> Type, Level World) {
        super(Type, World);
        xpReward = Reward;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.ATTACK_DAMAGE, 13.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Brace(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, TheodoreAmalgamation.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    private void setAction(byte Value, int Ticks) {
        entityData.set(ActionData, Value);
        ActionLeft = Ticks;
    }

    public boolean isBracing() {
        byte Current = action();
        return Current == ActionSlam || Current == ActionRise;
    }

    public void rise() {
        setAction(ActionRise, RiseTicks);
    }

    @Nullable
    public AmalgamationEyes eyes() {
        if (Eyes == null && level().isClientSide) {
            Eyes = new AmalgamationEyes(Heads);
        }
        return Eyes;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            AmalgamationEyes Current = eyes();
            if (Current != null) {
                Current.tick(this);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (ActionLeft > 0) {
            ActionLeft--;
            if (action() == ActionSlam && ActionLeft == SlamTicks - SlamImpact) {
                slamImpact(World);
            }
            if (ActionLeft == 0) {
                if (action() == ActionRise) {
                    roar();
                }
                entityData.set(ActionData, ActionNone);
            }
        }
        if (SlamCooldown > 0) {
            SlamCooldown--;
        }
        LivingEntity Target = getTarget();
        if (action() == ActionNone && SlamCooldown <= 0 && Target != null && Target.isAlive() && onGround() && distanceToSqr(Target) < SlamTrigger * SlamTrigger) {
            setAction(ActionSlam, SlamTicks);
            SlamCooldown = SlamCooldownMin + random.nextInt(SlamCooldownSpread);
            getNavigation().stop();
            playSound(TheodoreRegistry.MassRoar.get(), 1.4f, 1.1f);
        }
    }

    private void slamImpact(ServerLevel World) {
        Vec3 Centre = position().add(getLookAngle().multiply(1.2, 0.0, 1.2));
        AABB Area = new AABB(Centre.x - SlamRange, Centre.y - 1.0, Centre.z - SlamRange, Centre.x + SlamRange, Centre.y + 3.0, Centre.z + SlamRange);
        List<LivingEntity> Victims = World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found != this && Found.isAlive() && !(Found instanceof TheodoreAmalgamation)
                && !(Found instanceof Theodore) && !(Found instanceof Player Someone && (Someone.isCreative() || Someone.isSpectator())));
        for (LivingEntity Victim : Victims) {
            Vec3 Offset = Victim.position().subtract(Centre);
            double Distance = Math.sqrt(Offset.x * Offset.x + Offset.z * Offset.z);
            if (Distance > SlamRange) {
                continue;
            }
            float Share = (float) (1.0 - Distance / SlamRange * 0.5);
            if (Victim.hurt(damageSources().mobAttack(this), SlamDamage * Share)) {
                Vec3 Push = Distance > 0.01 ? new Vec3(Offset.x / Distance, 0.0, Offset.z / Distance) : getLookAngle();
                Victim.push(Push.x * 1.1 * Share, 0.55 * Share, Push.z * 1.1 * Share);
                Victim.hurtMarked = true;
                Victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
            }
        }
        BlockPos Below = BlockPos.containing(Centre.x, getY() - 0.2, Centre.z);
        BlockState Ground = World.getBlockState(Below);
        if (!Ground.isAir()) {
            for (int Index = 0; Index < 36; Index++) {
                double Angle = Index * Math.PI * 2.0 / 36.0;
                double Radius = 1.0 + random.nextDouble() * (SlamRange - 1.0);
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Centre.x + Math.cos(Angle) * Radius, getY() + 0.1, Centre.z + Math.sin(Angle) * Radius,
                        2, 0.1, 0.05, 0.1, 0.15);
            }
        }
        World.sendParticles(ParticleTypes.EXPLOSION, Centre.x, getY() + 0.3, Centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        playLayers(TheodoreRegistry.MassSlamLayers, 2.0f, 0.8f);
    }

    private void roar() {
        playLayers(TheodoreRegistry.MassRoarLayers, 2.5f, 0.7f);
    }

    private void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY() + 1.5, getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (Hit) {
            setAction(ActionSwipe, SwipeTicks);
            playSound(TheodoreRegistry.MassAttack.get(), 1.2f, 0.9f + random.nextFloat() * 0.2f);
        }
        return Hit;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (action() == ActionRise && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY() + 1.5, getZ(), TheodoreRegistry.MassDeathWail.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : TheodoreRegistry.MassStepLayers) {
            playSound(Layer.get(), 0.9f, 0.8f + random.nextFloat() * 0.2f);
        }
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return TheodoreRegistry.MassAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return TheodoreRegistry.MassHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return TheodoreRegistry.MassDeath.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 6, Event -> {
            if (isDeadOrDying()) {
                return Event.setAndContinue(DeathPlay);
            }
            return Event.setAndContinue(Event.isMoving() && !isBracing() ? WalkLoop : IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> {
            byte Current = action();
            if (isDeadOrDying() || Current == ActionNone) {
                Event.getController().forceAnimationReset();
                return PlayState.STOP;
            }
            return Event.setAndContinue(Current == ActionSwipe ? SwipePlay : Current == ActionSlam ? SlamPlay : RisePlay);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putInt("SlamCooldown", SlamCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        SlamCooldown = Mth.clamp(Saved.getInt("SlamCooldown"), 0, SlamCooldownMin + SlamCooldownSpread);
    }

    private static final class Brace extends Goal {
        private final TheodoreAmalgamation Owner;

        private Brace(TheodoreAmalgamation Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Owner.isBracing();
        }

        @Override
        public void start() {
            Owner.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Owner.getNavigation().stop();
            LivingEntity Target = Owner.getTarget();
            if (Target != null) {
                Owner.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            }
        }
    }
}
