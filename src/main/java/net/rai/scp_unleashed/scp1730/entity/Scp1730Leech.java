package net.rai.scp_unleashed.scp1730.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp1730Leech extends Monster implements GeoEntity {
    public static final float Width = 0.3f;
    public static final float Height = 0.2f;
    public static final int MaxLatched = 3;
    public static final int DrainEvery = 20;
    public static final float DrainDamage = 1.0f;
    public static final double FarAway = 48.0;
    private static final EntityDataAccessor<Integer> DataLatched = SynchedEntityData.defineId(Scp1730Leech.class, EntityDataSerializers.INT);
    private static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation Crawl = RawAnimation.begin().thenLoop("crawl");
    private static final RawAnimation Latched = RawAnimation.begin().thenLoop("latched");
    private static final RawAnimation BiteOnce = RawAnimation.begin().thenPlay("bite");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos Home;
    private int Slot;

    public Scp1730Leech(EntityType<? extends Scp1730Leech> Type, Level World) {
        super(Type, World);
        xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 1.5)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(DataLatched, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setHome(BlockPos Pos) {
        Home = Pos.immutable();
    }

    public int latchedTo() {
        return entityData.get(DataLatched);
    }

    public boolean latched() {
        return latchedTo() >= 0;
    }

    public static int latchedCount(Level World, LivingEntity Host) {
        int Count = 0;
        for (Scp1730Leech Each : World.getEntitiesOfClass(Scp1730Leech.class, Host.getBoundingBox().inflate(2.0))) {
            if (Each.latchedTo() == Host.getId()) {
                Count++;
            }
        }
        return Count;
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        triggerAnim("action", "bite");
        playSound(Scp1730Registry.LeechBite.get(), 0.8f, 0.9f + random.nextFloat() * 0.3f);
        if (Hit && Target instanceof LivingEntity Host && !latched() && !InfectionImmunity.isImmune(Host) && latchedCount(level(), Host) < MaxLatched) {
            Slot = latchedCount(level(), Host);
            entityData.set(DataLatched, Host.getId());
        }
        return Hit;
    }

    public void unlatch() {
        if (latched()) {
            entityData.set(DataLatched, -1);
            setDeltaMovement(new Vec3((random.nextDouble() - 0.5) * 0.4, 0.25, (random.nextDouble() - 0.5) * 0.4));
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && Source.getEntity() != null) {
            unlatch();
        }
        return Hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (latched()) {
            Entity Host = level().getEntity(latchedTo());
            if (!(Host instanceof LivingEntity Living) || !Living.isAlive() || Living.distanceToSqr(this) > 16.0) {
                unlatch();
                return;
            }
            double Angle = Living.yBodyRot * (Math.PI / 180.0) + Math.PI + (Slot - 1) * 0.9;
            setPos(Living.getX() + Math.sin(-Angle) * 0.3, Living.getY() + 0.4 + Slot * 0.28, Living.getZ() + Math.cos(Angle) * 0.3);
            setDeltaMovement(Vec3.ZERO);
            setYRot(Living.yBodyRot);
            if (tickCount % DrainEvery == 0 && level() instanceof ServerLevel World) {
                Living.hurt(new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp1730Registry.LeechDamage), this), DrainDamage);
                Living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
                heal(0.5f);
            }
        }
        if (tickCount % 20 == 0) {
            if (Scp1730Anchors.anchored(this)) {
                hurt(damageSources().magic(), 2.0f);
            }
            if (level().getNearestPlayer(this, FarAway) == null) {
                discard();
            }
        }
    }

    @Override
    public boolean isPushable() {
        return !latched();
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp1730Registry.LeechCrawl.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp1730Registry.LeechDeath.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 4, Animated -> {
            if (latched()) {
                return Animated.setAndContinue(Latched);
            }
            return Animated.setAndContinue(Animated.isMoving() ? Crawl : Idle);
        }));
        Controllers.add(new AnimationController<>(this, "action", 2, Animated -> Animated.getController().isPlayingTriggeredAnimation() ? PlayState.CONTINUE : PlayState.STOP)
                .receiveTriggeredAnimations().triggerableAnim("bite", BiteOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
