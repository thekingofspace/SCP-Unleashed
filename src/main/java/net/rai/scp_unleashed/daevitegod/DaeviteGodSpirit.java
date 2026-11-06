package net.rai.scp_unleashed.daevitegod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class DaeviteGodSpirit extends Monster implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.7f;
    public static final float Height = 1.95f;
    public static final float EyeHeight = 1.7f;
    public static final double Health = 20.0;
    public static final double Armour = 4.0;
    public static final double Speed = 0.31;
    public static final double FollowRange = 32.0;
    public static final float SpearDamage = 5.0f;
    public static final double SpearReach = 2.7;
    public static final double ChaseModifier = 1.15;
    public static final int Lifetime = 400;
    public static final int RiseTicks = 20;
    public static final int AttackTicks = 14;
    public static final int AttackHit = 8;
    public static final int AttackCooldown = 18;
    public static final int FadeTicks = 15;
    public static final int CheckEvery = 20;
    public static final byte ActionNone = 0;
    public static final byte ActionRise = 1;
    public static final byte ActionAttack = 2;
    public static final byte ActionFade = 3;
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(DaeviteGodSpirit.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(DaeviteGodSpirit.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("spirit_idle");
    private static final RawAnimation GlideLoop = RawAnimation.begin().thenLoop("spirit_glide");
    private static final RawAnimation RisePlay = RawAnimation.begin().thenPlayAndHold("spirit_rise");
    private static final RawAnimation AttackPlay = RawAnimation.begin().thenPlayAndHold("spirit_attack");
    private static final RawAnimation FadePlay = RawAnimation.begin().thenPlayAndHold("spirit_fade");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID OwnerId;
    private int Age;
    private int AttackWait;
    private int TickOrigin;
    private float RenderPartial;

    public DaeviteGodSpirit(EntityType<? extends DaeviteGodSpirit> Type, Level World) {
        super(Type, World);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.ARMOR, Armour).add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.ATTACK_DAMAGE, SpearDamage).add(Attributes.KNOCKBACK_RESISTANCE, 0.4).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0f));
    }

    public void bind(@Nullable LivingEntity Owner) {
        OwnerId = Owner == null ? null : Owner.getUUID();
    }

    @Nullable
    public UUID ownerId() {
        return OwnerId;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean fading() {
        return action() == ActionFade;
    }

    public void beginRise() {
        setAction(ActionRise);
    }

    public void fade() {
        if (!fading() && !level().isClientSide) {
            setAction(ActionFade);
            playSound(DaeviteGodRegistry.SpiritDeath.get(), 0.8f, 1.1f + random.nextFloat() * 0.2f);
        }
    }

    private void setAction(byte Kind) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float opacity(float Partial) {
        if (fading()) {
            return Mth.clamp(1.0f - (float) actionElapsed() / FadeTicks, 0.0f, 1.0f);
        }
        if (action() == ActionRise) {
            return Mth.clamp(0.25f + (float) actionElapsed() / RiseTicks, 0.0f, 1.0f);
        }
        return 1.0f;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (isAlive() && tickCount % 3 == 0) {
                level().addParticle(DaeviteGodRegistry.Ember.get(), getRandomX(0.4), getY() + random.nextDouble() * 1.6, getRandomZ(0.4), 0.0, 0.02, 0.0);
            }
            return;
        }
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        Age++;
        byte Current = action();
        int Elapsed = (int) actionElapsed();
        if (Current == ActionFade) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 0.0, 0.0).add(0.0, 0.03, 0.0));
            if (Elapsed % 3 == 0) {
                World.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 3, 0.25, 0.5, 0.25, 0.02);
            }
            if (Elapsed >= FadeTicks) {
                World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 1.0, getZ(), 14, 0.3, 0.6, 0.3, 0.04);
                discard();
            }
            return;
        }
        if (Current == ActionRise) {
            setDeltaMovement(0.0, Math.min(0.0, getDeltaMovement().y), 0.0);
            if (Elapsed >= RiseTicks) {
                setAction(ActionNone);
            }
            return;
        }
        if (Age >= Lifetime || Age % CheckEvery == 0 && !ownerStands(World)) {
            fade();
            return;
        }
        AttackWait = Math.max(0, AttackWait - 1);
        LivingEntity Target = getTarget();
        if (Target != null && (!DaeviteGodTargets.isPrey(this, Target) || distanceToSqr(Target) > FollowRange * FollowRange)) {
            setTarget(null);
            Target = null;
        }
        if (Target == null && (tickCount + getId()) % 10 == 0) {
            Target = pickTarget(World);
            setTarget(Target);
        }
        if (Current == ActionAttack) {
            if (Target != null) {
                getLookControl().setLookAt(Target, 40.0f, 40.0f);
                turnTo(Target);
            }
            setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
            if (Elapsed == AttackHit) {
                spear(Target);
            }
            if (Elapsed >= AttackTicks) {
                setAction(ActionNone);
                AttackWait = AttackCooldown;
            }
            return;
        }
        if (Target == null) {
            getNavigation().stop();
            return;
        }
        getLookControl().setLookAt(Target, 40.0f, 40.0f);
        double Gap = Math.sqrt(distanceToSqr(Target)) - Target.getBbWidth() * 0.5;
        if (Gap <= SpearReach - 0.3 && AttackWait <= 0 && hasLineOfSight(Target)) {
            setAction(ActionAttack);
            playSound(DaeviteGodRegistry.SpiritAttack.get(), 0.9f, 0.95f + random.nextFloat() * 0.15f);
            return;
        }
        if (tickCount % 8 == 0 || getNavigation().isDone()) {
            getNavigation().moveTo(Target, ChaseModifier);
        }
    }

    private boolean ownerStands(ServerLevel World) {
        if (OwnerId == null) {
            return true;
        }
        Entity Owner = World.getEntity(OwnerId);
        if (Owner instanceof DaeviteGod God) {
            return God.isAlive() && !God.isRemoved();
        }
        DaeviteGodLedger Ledger = DaeviteGodLedger.get(World.getServer());
        return Ledger.alive() && OwnerId.equals(Ledger.godId());
    }

    @Nullable
    private LivingEntity pickTarget(ServerLevel World) {
        LivingEntity Best = null;
        double BestGap = Double.MAX_VALUE;
        AABB Reach = getBoundingBox().inflate(FollowRange, FollowRange * 0.5, FollowRange);
        for (LivingEntity Found : World.getEntitiesOfClass(LivingEntity.class, Reach, Body -> DaeviteGodTargets.isPrey(this, Body))) {
            double Gap = distanceToSqr(Found);
            if (Gap < BestGap && (Gap < 100.0 || hasLineOfSight(Found))) {
                BestGap = Gap;
                Best = Found;
            }
        }
        return Best;
    }

    private void turnTo(LivingEntity Target) {
        float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Mth.approachDegrees(getYRot(), Yaw, 30.0f));
        setYBodyRot(getYRot());
    }

    private void spear(@Nullable LivingEntity Target) {
        if (Target == null || !DaeviteGodTargets.isPrey(this, Target)) {
            return;
        }
        double Gap = Math.sqrt(distanceToSqr(Target)) - Target.getBbWidth() * 0.5;
        if (Gap > SpearReach || !hasLineOfSight(Target)) {
            return;
        }
        if (Target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            Target.knockback(0.4, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
            setLastHurtMob(Target);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (action() == ActionRise || fading() || DaeviteGodTargets.isAlly(Source.getEntity()) || DaeviteGodTargets.isAlly(Source.getDirectEntity())) {
            return true;
        }
        if (Source.is(DamageTypeTags.IS_FALL) || Source.is(DamageTypeTags.IS_DROWNING) || Source.is(DamageTypes.IN_WALL) || Source.is(DamageTypeTags.IS_FREEZING)) {
            return true;
        }
        return super.isInvulnerableTo(Source);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 12, 0.3, 0.6, 0.3, 0.03);
            World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 1.0, getZ(), 16, 0.3, 0.6, 0.3, 0.05);
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= 6 && !level().isClientSide() && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
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
    public boolean canDrownInFluidType(FluidType Kind) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return action() == ActionNone && super.isPushable();
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
    protected boolean canRide(Entity Vehicle) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return DaeviteGodTargets.isAlly(Other) || super.isAlliedTo(Other);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return fading() ? null : DaeviteGodRegistry.SpiritAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return DaeviteGodRegistry.SpiritHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DaeviteGodRegistry.SpiritDeath.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (OwnerId != null) {
            Saved.putUUID("Owner", OwnerId);
        }
        Saved.putInt("Age", Age);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        OwnerId = Saved.hasUUID("Owner") ? Saved.getUUID("Owner") : null;
        Age = Saved.getInt("Age");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new DaeviteGodSpiritController(this, "body", 2, Event -> {
            byte Current = action();
            if (Current == ActionRise) {
                return Event.setAndContinue(RisePlay);
            }
            if (Current == ActionFade || isDeadOrDying()) {
                return Event.setAndContinue(FadePlay);
            }
            if (Current == ActionAttack) {
                return Event.setAndContinue(AttackPlay);
            }
            Vec3 Motion = getDeltaMovement();
            return Event.setAndContinue(Motion.x * Motion.x + Motion.z * Motion.z > 0.0025 ? GlideLoop : IdleLoop);
        }));
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionRise -> "spirit_rise";
            case ActionAttack -> "spirit_attack";
            case ActionFade -> "spirit_fade";
            default -> null;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    public static class DaeviteGodSpiritController extends AnimationController<DaeviteGodSpirit> {
        public DaeviteGodSpiritController(DaeviteGodSpirit Spirit, String Name, int Blend, AnimationStateHandler<DaeviteGodSpirit> Handler) {
            super(Spirit, Name, Blend, Handler);
        }

        @Override
        protected double adjustTick(double Tick) {
            if (animationState != State.TRANSITIONING && currentAnimation != null) {
                String Clip = animatable.actionClip();
                if (Clip != null && Clip.equals(currentAnimation.animation().name())) {
                    return animatable.actionElapsed();
                }
            }
            return super.adjustTick(Tick);
        }
    }
}
