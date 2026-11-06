package net.rai.scp_unleashed.scp087;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

public class Scp087Hunter extends Monster implements GeoEntity {
    public static final float Width = 0.7f;
    public static final float Height = 2.7f;
    public static final float EyeHeight = 2.45f;
    public static final double MaxHealth = 90.0;
    public static final double Armour = 4.0;
    public static final double Toughness = 2.0;
    public static final double Steadiness = 0.7;
    public static final double Speed = 0.3;
    public static final double Follow = 48.0;
    public static final float SwipeDamage = 6.0f;
    public static final float LungeDamage = 10.0f;
    public static final double StalkSpeed = 0.7;
    public static final double ChaseSpeed = 1.0;
    public static final double RushSpeed = 1.45;
    public static final double StalkBeyond = 12.0;
    public static final double LungeMin = 3.5;
    public static final double LungeMax = 8.0;
    public static final double LungeSpeed = 1.05;
    public static final double LungeLift = 0.3;
    public static final double SwipeReach = 2.6;
    public static final int WindupTicks = 12;
    public static final int LungeTicks = 14;
    public static final int RecoverTicks = 18;
    public static final int SwipeTicks = 11;
    public static final int SwipeHit = 6;
    public static final int LungeCooldown = 70;
    public static final int SwipeCooldown = 22;
    public static final int FreezeMin = 18;
    public static final int FreezeMax = 40;
    public static final int FreezeCooldown = 160;
    public static final float FreezeChance = 0.05f;
    public static final double WatchedCone = 0.93;
    public static final int RushTicks = 30;
    public static final double DarknessRange = 14.0;
    public static final int DarknessTicks = 70;
    public static final int RegenDelay = 200;
    public static final int RegenEvery = 30;
    public static final float RegenAmount = 1.0f;
    public static final int DeathTicks = 36;
    public static final int Experience = 40;
    public static final int LoseSight = 400;
    public static final byte ActionNone = 0;
    public static final byte ActionWindup = 1;
    public static final byte ActionLunge = 2;
    public static final byte ActionRecover = 3;
    public static final byte ActionSwipe = 4;
    public static final byte ActionFreeze = 5;
    public static final String[] ActionClips = {"", "windup", "lunge", "recover", "swipe", "freeze"};
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    public static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    public static final RawAnimation[] ActionPlays = {null, RawAnimation.begin().thenPlayAndHold("windup"), RawAnimation.begin().thenPlayAndHold("lunge"),
            RawAnimation.begin().thenPlayAndHold("recover"), RawAnimation.begin().thenPlayAndHold("swipe"), RawAnimation.begin().thenLoop("freeze")};
    protected static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp087Hunter.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> SerialData = SynchedEntityData.defineId(Scp087Hunter.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Byte> TiltData = SynchedEntityData.defineId(Scp087Hunter.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID Token;
    private int ActionStart;
    private int ActionLength;
    private int LungeWait = 40;
    private int SwipeWait;
    private int FreezeWait = 80;
    private int RushLeft;
    private int LastHurt = -10000;
    private int LastSeen;
    private boolean Struck;
    private Vec3 Heading = Vec3.ZERO;
    private int LocalActionStart;
    private float RenderPartial;
    @Nullable
    private Scp087Gait Gait;

    public Scp087Hunter(EntityType<? extends Scp087Hunter> Type, Level World) {
        super(Type, World);
        xpReward = Experience;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.ARMOR, Armour)
                .add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.KNOCKBACK_RESISTANCE, Steadiness)
                .add(Attributes.MOVEMENT_SPEED, Speed)
                .add(Attributes.FOLLOW_RANGE, Follow)
                .add(Attributes.ATTACK_DAMAGE, SwipeDamage)
                .add(Attributes.STEP_HEIGHT, 1.1)
                .add(Attributes.SAFE_FALL_DISTANCE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(SerialData, 0);
        Builder.define(TiltData, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Stalk(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public void bind(UUID InstanceToken) {
        Token = InstanceToken;
    }

    @Nullable
    public UUID token() {
        return Token;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public byte tilt() {
        return entityData.get(TiltData);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        if (Gait != null) {
            Gait.setPartial(Partial);
        }
    }

    @Nullable
    public Scp087Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp087Gait();
        }
        return Gait;
    }

    public double actionElapsed() {
        if (!level().isClientSide) {
            return tickCount - ActionStart;
        }
        return Math.max(0.0, tickCount - LocalActionStart + RenderPartial);
    }

    @Nullable
    public String actionClip() {
        byte Current = action();
        return Current > ActionNone && Current < ActionClips.length ? ActionClips[Current] : null;
    }

    public RawAnimation bodyAnimation() {
        if (isDeadOrDying()) {
            return DeathPlay;
        }
        byte Current = action();
        if (Current > ActionNone && Current < ActionPlays.length) {
            return ActionPlays[Current];
        }
        Scp087Gait Walking = gait();
        if (Walking != null && Walking.striding()) {
            return Walking.running() ? RunLoop : WalkLoop;
        }
        return IdleLoop;
    }

    private void startAction(byte Kind, int Length) {
        ActionStart = tickCount;
        ActionLength = Length;
        entityData.set(ActionData, Kind);
        entityData.set(SerialData, entityData.get(SerialData) + 1);
        if (Kind != ActionFreeze) {
            entityData.set(TiltData, (byte) 0);
        }
    }

    private void endAction() {
        entityData.set(ActionData, ActionNone);
        entityData.set(TiltData, (byte) 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (level().isClientSide && SerialData.equals(Accessor)) {
            LocalActionStart = tickCount;
        }
    }

    public static boolean isPrey(Entity Hunter, @Nullable Entity Found) {
        if (!(Found instanceof LivingEntity Body) || Body instanceof Scp087Hunter || Body == Hunter) {
            return false;
        }
        if (!Body.isAlive() || Body.isRemoved() || Body.isDeadOrDying() || Body.isSpectator() || !Targeting.isTargetable(Body)) {
            return false;
        }
        if (Body instanceof Player User) {
            return !User.isCreative() && Hunter.level().getDifficulty() != Difficulty.PEACEFUL && !Scp268.isNeglected(User);
        }
        return ControlBodies.isPersonLike(Body);
    }

    public double moveSpeed() {
        if (RushLeft > 0) {
            return RushSpeed;
        }
        LivingEntity Target = getTarget();
        return Target != null && distanceTo(Target) > StalkBeyond ? StalkSpeed : ChaseSpeed;
    }

    public void shifted(double Dx, double Dy) {
        getNavigation().stop();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp087Gait Walking = gait();
            if (Walking != null) {
                Walking.tick(this);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        scan(World);
        LungeWait = Math.max(0, LungeWait - 1);
        SwipeWait = Math.max(0, SwipeWait - 1);
        FreezeWait = Math.max(0, FreezeWait - 1);
        RushLeft = Math.max(0, RushLeft - 1);
        LivingEntity Target = getTarget();
        setAggressive(Target != null);
        if (Target != null && tickCount % 40 == 0 && Target instanceof Player Prey && distanceTo(Prey) < DarknessRange) {
            Prey.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DarknessTicks, 0, true, false, false), this);
        }
        if (action() != ActionNone) {
            tickAction(World, Target);
        } else if (Target != null) {
            choose(World, Target);
        }
        if (tickCount - LastHurt > RegenDelay && tickCount % RegenEvery == 0 && getHealth() < getMaxHealth()) {
            heal(RegenAmount);
        }
    }

    private void scan(ServerLevel World) {
        LivingEntity Current = getTarget();
        if (Current != null) {
            if (!isPrey(this, Current) || Current.level() != level() || distanceToSqr(Current) > Follow * Follow) {
                setTarget(null);
            } else if (getSensing().hasLineOfSight(Current) || distanceToSqr(Current) < 100.0) {
                LastSeen = tickCount;
            } else if (tickCount - LastSeen > LoseSight) {
                setTarget(null);
            }
        }
        if (getTarget() != null || (tickCount + getId()) % 10 != 0) {
            return;
        }
        LivingEntity Best = null;
        double BestGap = Double.MAX_VALUE;
        for (LivingEntity Found : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Follow, Follow * 0.75, Follow), Body -> isPrey(this, Body))) {
            double Gap = distanceToSqr(Found);
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = Found;
            }
        }
        if (Best != null) {
            setTarget(Best);
            LastSeen = tickCount;
        }
    }

    private boolean watchedBy(LivingEntity Target) {
        Vec3 Toward = getEyePosition().subtract(CameraViews.eye(Target)).normalize();
        return !Senses.isBlindfolded(Target) && Toward.dot(CameraViews.look(Target)) > WatchedCone && CameraViews.canSee(Target, this);
    }

    private void choose(ServerLevel World, LivingEntity Target) {
        double Gap = distanceTo(Target);
        boolean Sight = getSensing().hasLineOfSight(Target);
        if (Gap <= SwipeReach && SwipeWait == 0 && Sight) {
            getNavigation().stop();
            startAction(ActionSwipe, SwipeTicks);
            Struck = false;
            playSound(Scp087Registry.HunterSwipe.get(), 0.9f, 0.85f + random.nextFloat() * 0.1f);
            return;
        }
        if (Sight && Gap >= LungeMin && Gap <= LungeMax && LungeWait == 0 && onGround()) {
            getNavigation().stop();
            startAction(ActionWindup, WindupTicks);
            playSound(Scp087Registry.HunterWindup.get(), 1.0f, 0.9f + random.nextFloat() * 0.15f);
            return;
        }
        if (Sight && Gap > 6.0 && FreezeWait == 0 && random.nextFloat() < FreezeChance && watchedBy(Target)) {
            getNavigation().stop();
            startAction(ActionFreeze, FreezeMin + random.nextInt(FreezeMax - FreezeMin + 1));
            entityData.set(TiltData, (byte) (random.nextBoolean() ? 1 : -1));
            FreezeWait = FreezeCooldown;
        }
    }

    private void face(LivingEntity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
        getLookControl().setLookAt(Target, 60.0f, 60.0f);
    }

    private void tickAction(ServerLevel World, @Nullable LivingEntity Target) {
        int Time = tickCount - ActionStart;
        byte Current = action();
        if (Current == ActionWindup) {
            getNavigation().stop();
            if (Target == null) {
                endAction();
                return;
            }
            face(Target);
            if (Time >= ActionLength) {
                Vec3 Lead = Target.position().add(Target.getDeltaMovement().scale(4.0)).subtract(position());
                Vec3 Flat = new Vec3(Lead.x, 0.0, Lead.z);
                if (Flat.lengthSqr() < 1.0e-4) {
                    Flat = Vec3.directionFromRotation(0.0f, getYRot());
                }
                Heading = Flat.normalize();
                double Rise = Mth.clamp((Target.getY() - getY()) * 0.08, -0.1, 0.35);
                setDeltaMovement(Heading.x * LungeSpeed, LungeLift + Math.max(0.0, Rise), Heading.z * LungeSpeed);
                hasImpulse = true;
                Struck = false;
                startAction(ActionLunge, LungeTicks);
                playSound(Scp087Registry.HunterLunge.get(), 1.3f, 0.9f + random.nextFloat() * 0.1f);
            }
            return;
        }
        if (Current == ActionLunge) {
            getNavigation().stop();
            if (!Struck && Target != null && getBoundingBox().inflate(0.4).intersects(Target.getBoundingBox())) {
                Struck = true;
                strike(World, Target, LungeDamage, 0.6);
                setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
            }
            if (Struck || Time >= ActionLength || Time > 3 && onGround()) {
                LungeWait = LungeCooldown;
                startAction(ActionRecover, RecoverTicks);
            }
            return;
        }
        if (Current == ActionRecover) {
            getNavigation().stop();
            if (Time >= ActionLength) {
                endAction();
            }
            return;
        }
        if (Current == ActionSwipe) {
            getNavigation().stop();
            if (Target != null) {
                face(Target);
            }
            if (Time == SwipeHit && !Struck && Target != null && distanceTo(Target) <= SwipeReach + 0.6 && getSensing().hasLineOfSight(Target)) {
                Struck = true;
                strike(World, Target, SwipeDamage, 0.35);
            }
            if (Time >= ActionLength) {
                SwipeWait = SwipeCooldown;
                endAction();
            }
            return;
        }
        if (Current == ActionFreeze) {
            getNavigation().stop();
            if (Target != null) {
                getLookControl().setLookAt(Target, 10.0f, 10.0f);
            }
            if (Time >= ActionLength) {
                RushLeft = RushTicks;
                endAction();
            }
            return;
        }
        endAction();
    }

    private void strike(ServerLevel World, LivingEntity Target, float Damage, double Push) {
        DamageSource Source = damageSources().mobAttack(this);
        if (Target.hurt(Source, Damage)) {
            Vec3 Away = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
            if (Away.lengthSqr() > 1.0e-4) {
                Target.knockback(Push, -Away.x, -Away.z);
            }
            World.sendParticles(ParticleTypes.SMOKE, Target.getX(), Target.getY() + Target.getBbHeight() * 0.6, Target.getZ(), 6, 0.2, 0.3, 0.2, 0.01);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt) {
            LastHurt = tickCount;
            if (Source.getEntity() instanceof LivingEntity Attacker && isPrey(this, Attacker) && getTarget() == null) {
                setTarget(Attacker);
            }
        }
        return Hurt;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp087Registry.HunterAmbient.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 140;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp087Registry.HunterHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp087Registry.HunterDeath.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.2f;
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.playSound(null, getX(), getY(), getZ(), Scp087Registry.HunterDeathLow.get(), SoundSource.HOSTILE, 1.4f, 0.8f);
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        AABB Box = getBoundingBox();
        double Share = Math.min(1.0, deathTime / (double) DeathTicks);
        double Top = Box.maxY - (Box.maxY - Box.minY) * Share * 0.85;
        World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), (Box.minY + Top) * 0.5, getZ(), 2 + deathTime / 8, 0.25, (Top - Box.minY) * 0.3, 0.25, 0.01);
        World.sendParticles(ParticleTypes.SQUID_INK, getX(), Top, getZ(), 1, 0.2, 0.2, 0.2, 0.0);
        if (deathTime >= DeathTicks && !isRemoved()) {
            World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.4, getZ(), 16, 0.4, 0.2, 0.4, 0.01);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return Token == null && super.removeWhenFarAway(Distance);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (Token != null) {
            Saved.putUUID("Scp087Token", Token);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Token = Saved.hasUUID("Scp087Token") ? Saved.getUUID("Scp087Token") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp087HunterController(this, "body", 3, Event -> {
            Event.getController().transitionLength(Scp087HunterController.blendFor(action()));
            return Event.setAndContinue(bodyAnimation());
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    public static class Stalk extends Goal {
        private final Scp087Hunter Hunter;
        private int Repath;

        public Stalk(Scp087Hunter Hunter) {
            this.Hunter = Hunter;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Hunter.getTarget() != null && Hunter.action() == ActionNone;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Hunter.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Hunter.getTarget();
            if (Target == null) {
                return;
            }
            Hunter.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (--Repath <= 0) {
                Repath = 4 + Hunter.getRandom().nextInt(4);
                Hunter.getNavigation().moveTo(Target, Hunter.moveSpeed());
            }
        }
    }
}
