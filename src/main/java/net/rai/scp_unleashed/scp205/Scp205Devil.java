package net.rai.scp_unleashed.scp205;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp017.Scp017Registry;
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
import java.util.Optional;

public class Scp205Devil extends Monster implements GeoEntity {
    public static final float Width = 0.7f;
    public static final float Height = 2.3f;
    public static final float EyeHeight = 2.05f;
    public static final float MaxHealth = 40.0f;
    public static final float StrikeDamage = 5.0f;
    public static final double MoveSpeed = 0.3;
    public static final double ChaseSpeed = 1.2;
    public static final double ReturnSpeed = 0.8;
    public static final double HuntRange = 20.0;
    public static final double StrikeReach = 2.4;
    public static final double HomeSlack = 1.5;
    public static final int StrikeTicks = 12;
    public static final int StrikeImpact = 6;
    public static final int StrikeCooldown = 26;
    public static final int EmergeTicks = 30;
    public static final int FadeTicks = 24;
    public static final int DarkLimit = 8;
    public static final int AnchorInterval = 5;
    public static final int RetargetInterval = 10;
    public static final int LostLimit = 60;
    public static final int IdleLimit = 240;
    public static final int PassCooldown = 10;
    public static final float VeilStep = 1.0f / 12.0f;
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final byte ActionStrike = 2;
    public static final byte ActionFade = 3;
    public static final double EdgeProbe = 0.3;

    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp205Devil.class, EntityDataSerializers.BYTE);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation HuntLoop = RawAnimation.begin().thenLoop("hunt");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation FadePlay = RawAnimation.begin().thenPlayAndHold("fade");
    private static final RawAnimation StrikeOnce = RawAnimation.begin().thenPlay("strike");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos LampPos;
    @Nullable
    private Vec3 Home;
    private int ActionTicks;
    private int DarkTicks;
    private int AnchorTimer;
    private int RetargetTimer;
    private int LostTicks;
    private int IdleTicks;
    private int StrikeTimer;
    private int PassTimer;
    private float Veil;
    private float VeilO;
    @Nullable
    private AnimationController<Scp205Devil> BodyController;

    public Scp205Devil(EntityType<? extends Scp205Devil> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, MoveSpeed).add(Attributes.ATTACK_DAMAGE, StrikeDamage)
                .add(Attributes.FOLLOW_RANGE, HuntRange).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean prey(LivingEntity Candidate) {
        if (!Candidate.isAlive() || Candidate instanceof Scp205Devil || Candidate instanceof ArmorStand || Candidate.isInvulnerable() || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (Candidate instanceof Player User && (User.isSpectator() || User.isCreative())) {
            return false;
        }
        return Scp205Light.bright(Candidate);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp205DevilNavigation(this, World);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HuntGoal(this));
        goalSelector.addGoal(2, new ReturnGoal(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public float getPathfindingMalus(PathType Type) {
        if (Type == Scp205DevilNavigation.DarkPath) {
            return -1.0f;
        }
        return super.getPathfindingMalus(Type);
    }

    public void bind(BlockPos Lamp, Vec3 Spot) {
        LampPos = Lamp.immutable();
        Home = Spot;
    }

    @Nullable
    public BlockPos lampPos() {
        return LampPos;
    }

    @Nullable
    public Vec3 home() {
        return Home;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean busy() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionFade || Current == ActionStrike;
    }

    public boolean fading() {
        return action() == ActionFade;
    }

    private void setAction(byte Kind, int Ticks) {
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
    }

    public void beginEmerging() {
        setAction(ActionEmerge, EmergeTicks);
    }

    public void fadeOut() {
        if (fading() || !isAlive()) {
            return;
        }
        setAction(ActionFade, FadeTicks);
        setTarget(null);
        if (level() instanceof ServerLevel World) {
            playLayers(World, Scp205Registry.FadeLayers, 0.9f);
            World.sendParticles(Scp017Registry.Shade.get(), getX(), getY() + 1.1, getZ(), 24, 0.3, 0.7, 0.3, 0.03);
        }
    }

    @Override
    public void move(MoverType Type, Vec3 Delta) {
        if (!level().isClientSide && (Delta.x != 0.0 || Delta.z != 0.0)) {
            Delta = confine(Delta);
        }
        super.move(Type, Delta);
    }

    public Vec3 confine(Vec3 Delta) {
        Vec3 Here = position();
        if (!Scp205Light.bright(level(), Here.add(0.0, 0.1, 0.0))) {
            return Delta;
        }
        if (allowed(Here, Delta.x, Delta.z)) {
            return Delta;
        }
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (allowed(Here, Delta.x, 0.0)) {
            return new Vec3(Delta.x, Delta.y, 0.0);
        }
        if (allowed(Here, 0.0, Delta.z)) {
            return new Vec3(0.0, Delta.y, Delta.z);
        }
        return new Vec3(0.0, Delta.y, 0.0);
    }

    private boolean allowed(Vec3 Here, double Dx, double Dz) {
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Length < 1.0E-7) {
            return true;
        }
        double Lead = Length + EdgeProbe;
        Vec3 Probe = Here.add(Dx / Length * Lead, 0.1, Dz / Length * Lead);
        return Scp205Light.bright(level(), Probe);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (PassTimer > 0) {
            PassTimer--;
        }
        if (StrikeTimer > 0) {
            StrikeTimer--;
        }
        if (busy()) {
            ActionTicks--;
            if (action() == ActionStrike && ActionTicks == StrikeTicks - StrikeImpact) {
                impact(World);
            }
            if (ActionTicks <= 0) {
                if (action() == ActionFade) {
                    discard();
                    return;
                }
                setAction(ActionNone, 0);
            }
        }
        if (fading()) {
            return;
        }
        if (Scp205Light.bright(World, position().add(0.0, 0.1, 0.0))) {
            DarkTicks = 0;
        } else if (++DarkTicks >= DarkLimit) {
            fadeOut();
            return;
        }
        if (--AnchorTimer <= 0) {
            AnchorTimer = AnchorInterval;
            if (!anchored(World)) {
                fadeOut();
                return;
            }
        }
        if (--RetargetTimer <= 0) {
            RetargetTimer = RetargetInterval;
            retarget(World);
        }
        setAggressive(getTarget() != null);
        if (getTarget() == null && ++IdleTicks >= IdleLimit) {
            fadeOut();
        } else if (getTarget() != null) {
            IdleTicks = 0;
        }
    }

    private boolean anchored(ServerLevel World) {
        if (LampPos == null) {
            return World.isNight();
        }
        if (!World.isLoaded(LampPos)) {
            return false;
        }
        return World.getBlockEntity(LampPos) instanceof Scp205LampBlockEntity Lamp && Lamp.anchors(this);
    }

    private void retarget(ServerLevel World) {
        LivingEntity Current = getTarget();
        if (Current != null && Current.isAlive() && Targeting.isTargetable(Current) && !(Current instanceof Player User && (User.isSpectator() || User.isCreative())) && distanceTo(Current) <= HuntRange) {
            if (Scp205Light.bright(Current)) {
                LostTicks = 0;
                return;
            }
            LostTicks += RetargetInterval;
            if (LostTicks < LostLimit) {
                return;
            }
        }
        LostTicks = 0;
        LivingEntity Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(HuntRange), Scp205Devil::prey)) {
            double Distance = distanceToSqr(Candidate);
            if (Distance < BestDistance && hasLineOfSight(Candidate)) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        if (Best != Current) {
            setTarget(Best);
            if (Best != null) {
                playLayers(World, Scp205Registry.SnarlLayers, 0.8f);
            }
        }
    }

    public boolean readyToStrike() {
        return StrikeTimer <= 0 && !busy();
    }

    public void beginStrike() {
        StrikeTimer = StrikeCooldown + StrikeTicks;
        setAction(ActionStrike, StrikeTicks);
        triggerAnim("strike", "strike");
        if (level() instanceof ServerLevel World) {
            playLayers(World, List.of(Scp205Registry.Strike, Scp205Registry.SnarlGrowl), 0.9f);
        }
    }

    private void impact(ServerLevel World) {
        LivingEntity Target = getTarget();
        if (Target == null || !Target.isAlive() || distanceTo(Target) > StrikeReach + 0.6 || !hasLineOfSight(Target)) {
            return;
        }
        Target.hurt(damageSources().source(Scp205Registry.DevilDamage, this), StrikeDamage);
        Vec3 Push = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (Push.lengthSqr() > 1.0E-4) {
            Push = Push.normalize();
            Target.knockback(0.35, -Push.x, -Push.z);
        }
        World.sendParticles(Scp017Registry.Shade.get(), Target.getX(), Target.getY() + Target.getBbHeight() * 0.6, Target.getZ(), 10, 0.25, 0.35, 0.25, 0.04);
    }

    private void playLayers(ServerLevel World, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        float Pitch = 0.92f + random.nextFloat() * 0.16f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, getX(), getY() + 1.4, getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    @Override
    public void playAmbientSound() {
        if (level() instanceof ServerLevel World && !fading()) {
            playLayers(World, Scp205Registry.WhisperLayers, getSoundVolume());
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp205Registry.Whisper.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected float getSoundVolume() {
        return 0.85f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp205Registry.Pass.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp205Registry.Fade.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || Source.isCreativePlayer()) {
            return super.isInvulnerableTo(Source);
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && isAlive() && Source.getEntity() != null && isInvulnerableTo(Source) && PassTimer <= 0 && level() instanceof ServerLevel World) {
            PassTimer = PassCooldown;
            World.playSound(null, getX(), getY() + 1.2, getZ(), Scp205Registry.Pass.get(), SoundSource.HOSTILE, 0.6f, 0.9f + random.nextFloat() * 0.2f);
            World.sendParticles(Scp017Registry.Shade.get(), getX(), getY() + 1.2, getZ(), 8, 0.2, 0.4, 0.2, 0.03);
        }
        return super.hurt(Source, Amount);
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
    public boolean canDrownInFluidType(FluidType Fluid) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean startRiding(Entity Vehicle, boolean Force) {
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
    public boolean isPushedByFluid(FluidType Fluid) {
        return false;
    }

    private void clientTick() {
        VeilO = Veil;
        byte Current = action();
        float Goal = Current == ActionFade || !isAlive() ? 0.0f : 1.0f;
        float Step = Current == ActionEmerge ? 1.0f / EmergeTicks : Current == ActionFade ? 1.0f / FadeTicks : VeilStep;
        Veil = Veil < Goal ? Math.min(Goal, Veil + Step) : Math.max(Goal, Veil - Step);
        if (Veil > 0.2f && random.nextFloat() < 0.35f + (isAggressive() ? 0.3f : 0.0f)) {
            double Px = getX() + (random.nextDouble() - 0.5) * 0.6;
            double Py = getY() + 0.2 + random.nextDouble() * (getBbHeight() - 0.3);
            double Pz = getZ() + (random.nextDouble() - 0.5) * 0.6;
            level().addParticle(Scp017Registry.Shade.get(), Px, Py, Pz, (getX() - xo) * -0.2, 0.01 + random.nextDouble() * 0.01, (getZ() - zo) * -0.2);
        }
    }

    public float veil(float Partial) {
        return Mth.lerp(Partial, VeilO, Veil);
    }

    public float eyeGlow(float Partial) {
        float Base = veil(Partial);
        if (action() == ActionStrike) {
            return Math.min(1.0f, Base * 1.2f);
        }
        return Base * (0.8f + 0.1f * Mth.sin((tickCount + Partial) * 0.13f));
    }

    public double groundSpeed() {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (Key.equals(ActionData) && level().isClientSide && BodyController != null && (action() == ActionEmerge || action() == ActionFade)) {
            BodyController.forceAnimationReset();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (LampPos != null) {
            Tag.put("Lamp", NbtUtils.writeBlockPos(LampPos));
        }
        if (Home != null) {
            Tag.putDouble("HomeX", Home.x);
            Tag.putDouble("HomeY", Home.y);
            Tag.putDouble("HomeZ", Home.z);
        }
        Tag.putByte("Action", action());
        Tag.putInt("ActionTicks", ActionTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Optional<BlockPos> Saved = NbtUtils.readBlockPos(Tag, "Lamp");
        LampPos = Saved.orElse(null);
        Home = Tag.contains("HomeX") ? new Vec3(Tag.getDouble("HomeX"), Tag.getDouble("HomeY"), Tag.getDouble("HomeZ")) : null;
        byte SavedAction = Tag.getByte("Action");
        int Ticks = Tag.getInt("ActionTicks");
        if (Ticks > 0 && (SavedAction == ActionEmerge || SavedAction == ActionFade)) {
            entityData.set(ActionData, SavedAction);
            ActionTicks = Ticks;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        AnimationController<Scp205Devil> Body = new AnimationController<>(this, "body", 5, Event -> {
            switch (action()) {
                case ActionEmerge -> {
                    return Event.setAndContinue(EmergePlay);
                }
                case ActionFade -> {
                    return Event.setAndContinue(FadePlay);
                }
                default -> {
                }
            }
            double Speed = groundSpeed();
            if (Speed > 0.015) {
                boolean Running = isAggressive();
                double Reference = Running ? 0.19 : 0.11;
                Event.getController().setAnimationSpeed(Mth.clamp(Speed / Reference, 0.6, 1.8));
                return Event.setAndContinue(Running ? HuntLoop : WalkLoop);
            }
            Event.getController().setAnimationSpeed(1.0);
            return Event.setAndContinue(IdleLoop);
        });
        BodyController = Body;
        Controllers.add(Body);
        Controllers.add(new AnimationController<>(this, "strike", 2, Event -> PlayState.STOP).triggerableAnim("strike", StrikeOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    static class HuntGoal extends Goal {
        private final Scp205Devil Devil;
        private int RepathTimer;

        HuntGoal(Scp205Devil Devil) {
            this.Devil = Devil;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Devil.getTarget();
            return Target != null && Target.isAlive() && !Devil.busy();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Target = Devil.getTarget();
            return Target != null && Target.isAlive() && !Devil.fading() && Devil.action() != ActionEmerge;
        }

        @Override
        public void start() {
            RepathTimer = 0;
        }

        @Override
        public void stop() {
            Devil.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Devil.getTarget();
            if (Target == null) {
                return;
            }
            Devil.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (Devil.busy()) {
                return;
            }
            double Distance = Devil.distanceTo(Target);
            if (Distance <= StrikeReach && Devil.readyToStrike() && Devil.hasLineOfSight(Target)) {
                Devil.beginStrike();
                return;
            }
            if (--RepathTimer <= 0) {
                RepathTimer = 8 + Devil.getRandom().nextInt(5);
                if (!Devil.getNavigation().moveTo(Target, ChaseSpeed)) {
                    Devil.getNavigation().stop();
                }
            }
        }
    }

    static class ReturnGoal extends Goal {
        private final Scp205Devil Devil;

        ReturnGoal(Scp205Devil Devil) {
            this.Devil = Devil;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Vec3 Spot = Devil.home();
            return Spot != null && Devil.getTarget() == null && !Devil.busy() && Devil.position().distanceTo(Spot) > HomeSlack;
        }

        @Override
        public boolean canContinueToUse() {
            Vec3 Spot = Devil.home();
            return Spot != null && Devil.getTarget() == null && !Devil.busy() && !Devil.getNavigation().isDone();
        }

        @Override
        public void start() {
            Vec3 Spot = Devil.home();
            if (Spot != null) {
                Devil.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, ReturnSpeed);
            }
        }
    }
}
