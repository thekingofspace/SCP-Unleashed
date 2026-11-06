package net.rai.scp_unleashed.scp3199;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp3199 extends Monster implements GeoEntity {
    public static final float Width = 0.8f;
    public static final float Height = 2.1f;
    public static final float EyeHeight = 1.95f;
    public static final double MaxHealth = 40.0;
    public static final double Speed = 0.3;
    public static final double AttackDamage = 7.0;
    public static final double FollowRange = 32.0;
    public static final double Armor = 2.0;
    public static final double StepHeight = 1.0;
    public static final double RoamModifier = 0.55;
    public static final double HuntModifier = 1.16;
    public static final float SwimShare = 0.1f;
    public static final double WalkDistance = 0.9;
    public static final double RunDistance = 1.6;
    public static final double Reach = 1.7;
    public static final int ActionBlend = 4;
    public static final int AttackCooldown = 20;
    public static final int AttackLead = ActionBlend + 1;
    public static final int AttackCallTick = AttackLead + 3;
    public static final int AttackHitTick = AttackLead + 6;
    public static final double HitGrace = 0.6;
    public static final double[] WalkContacts = {0.975, 0.475};
    public static final double[] RunContacts = {0.989, 0.489};
    public static final float StepVolume = 0.35f;
    public static final double HandoverRange = 3.0;
    public static final double StepSide = 0.3;
    public static final int SpotCooldown = 100;
    public static final int SwimStroke = 32;
    public static final int GrowTicks = 8000;
    public static final float JuvenileScale = 0.6f;
    public static final double JuvenileSpeedShift = -0.1;
    public static final double JuvenileDamageShare = 0.5;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.004, 0.15, 0.12, WalkDistance, RunDistance, 0.6, Scp3199GaitController.StrideBlend);
    private static final EntityDataAccessor<Integer> AgeData = SynchedEntityData.defineId(Scp3199.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LayingData = SynchedEntityData.defineId(Scp3199.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation AttackOnce = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation HurtOnce = RawAnimation.begin().thenPlay("hurt");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    private final Scp3199Nesting Nesting = new Scp3199Nesting();
    @Nullable
    private Scp3199GaitController Gaits;
    private float RenderPartial;
    private int AttackWait;
    private int LayingFor;
    private int StrikeFor = -1;
    @Nullable
    private LivingEntity Striking;
    private int SpotWait;
    private int SwimFor;
    private int HandoverTick = -1;
    private boolean Handover;

    public Scp3199(EntityType<? extends Scp3199> Type, Level World) {
        super(Type, World);
        xpReward = 10;
        setPersistenceRequired();
        setPathfindingMalus(PathType.WATER, 0.0f);
        setPathfindingMalus(PathType.WATER_BORDER, 0.0f);
        moveControl = new SmoothSwimmingMoveControl(this, 70, 10, SwimShare, 1.0f, false);
        lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, AttackDamage)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.ARMOR, Armor).add(Attributes.STEP_HEIGHT, StepHeight).add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(AgeData, GrowTicks);
        Builder.define(LayingData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp3199Goals.Maul(this));
        goalSelector.addGoal(4, new Scp3199Goals.Roam(this));
        goalSelector.addGoal(6, new Scp3199Goals.Gaze(this));
        targetSelector.addGoal(1, new Scp3199Goals.Retaliate(this));
        targetSelector.addGoal(2, new Scp3199Goals.Hunt(this));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new AmphibiousPathNavigation(this, World);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        return Result;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (AgeData.equals(Key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose Stance) {
        return super.getDefaultDimensions(Stance).scale(growthScale());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1.6, 0.6, 1.6);
    }

    public int growth() {
        return entityData.get(AgeData);
    }

    public boolean isAdult() {
        return growth() >= GrowTicks;
    }

    public void makeJuvenile() {
        entityData.set(AgeData, 0);
        refreshDimensions();
        applyGrowth();
    }

    public float growthScale() {
        return JuvenileScale + (1.0f - JuvenileScale) * Mth.clamp(growth() / (float) GrowTicks, 0.0f, 1.0f);
    }

    private void applyGrowth() {
        var Movement = getAttribute(Attributes.MOVEMENT_SPEED);
        if (Movement != null) {
            float Share = 1.0f - Mth.clamp(growth() / (float) GrowTicks, 0.0f, 1.0f);
            Movement.setBaseValue(Speed * (1.0 + JuvenileSpeedShift * Share));
        }
        var Bite = getAttribute(Attributes.ATTACK_DAMAGE);
        if (Bite != null) {
            Bite.setBaseValue(AttackDamage * (1.0 - JuvenileDamageShare * (1.0f - Mth.clamp(growth() / (float) GrowTicks, 0.0f, 1.0f))));
        }
    }

    public boolean isLaying() {
        return entityData.get(LayingData);
    }

    public void setLaying(boolean Laying) {
        entityData.set(LayingData, Laying);
    }

    public int layingFor() {
        return LayingFor;
    }

    public boolean eggHandedOver() {
        if (!isLaying()) {
            return false;
        }
        if (HandoverTick != tickCount) {
            HandoverTick = tickCount;
            long Now = level().getGameTime();
            Handover = !level().getEntitiesOfClass(Scp3199Egg.class, getBoundingBox().inflate(HandoverRange),
                    Egg -> Egg.mother() == getId() && (!Egg.landed() || Now - Egg.landedAt() < Scp3199Nesting.LayTicks)).isEmpty();
        }
        return Handover;
    }

    public Scp3199Nesting nesting() {
        return Nesting;
    }

    public StrideGait.Shape gait() {
        float Scale = growthScale();
        if (Scale >= 1.0f) {
            return Gait;
        }
        return new StrideGait.Shape(Gait.StillPace() * Scale, Gait.RunEnter() * Scale, Gait.RunExit() * Scale, WalkDistance * Scale, RunDistance * Scale, Gait.MaxStep(),
                Gait.Blend());
    }

    public StrideGait stride() {
        return Stride;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public boolean isStriding() {
        return Stride.stride() != StrideGait.Idle;
    }

    public boolean isRunning() {
        return Stride.stride() == StrideGait.Run;
    }

    public boolean isSwimmingNow() {
        return isInWater() && !onGround();
    }

    public boolean canAct() {
        return isAlive() && !isLaying();
    }

    public float voicePitch() {
        return (0.92f + random.nextFloat() * 0.14f) * (1.0f + (1.0f - growthScale()) * 0.6f);
    }

    public void faceTowards(Vec3 Where) {
        double Dx = Where.x - getX();
        double Dz = Where.z - getZ();
        if (Dx * Dx + Dz * Dz < 1.0e-4) {
            return;
        }
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYHeadRot(Yaw);
        setYBodyRot(Yaw);
        yRotO = Yaw;
        yBodyRotO = Yaw;
        yHeadRotO = Yaw;
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        if (Target instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return false;
        }
        return !(Target instanceof Scp3199) && super.canAttack(Target);
    }

    public boolean attackReady() {
        return AttackWait <= 0 && canAct();
    }

    public double reach() {
        return getBbWidth() * 0.5 + Reach * growthScale();
    }

    public boolean strike(LivingEntity Target) {
        if (!attackReady() || StrikeFor >= 0) {
            return false;
        }
        AttackWait = AttackCooldown;
        Striking = Target;
        StrikeFor = 0;
        if (level() instanceof ServerLevel) {
            triggerAnim("action", "attack");
        }
        return true;
    }

    public boolean isStriking() {
        return StrikeFor >= 0;
    }

    public boolean inReach(LivingEntity Target, double Grace) {
        return Math.sqrt(getBoundingBox().distanceToSqr(Target.getBoundingBox().getCenter())) <= reach() - getBbWidth() * 0.5 + Grace;
    }

    private void strikeTick(ServerLevel World) {
        if (StrikeFor < 0) {
            return;
        }
        StrikeFor++;
        if (StrikeFor == AttackCallTick) {
            Scp3199Sounds.play(World, getEyePosition(), List.of(Scp3199Registry.Attack), 1.0f, voicePitch());
        }
        if (StrikeFor < AttackHitTick) {
            return;
        }
        LivingEntity Target = Striking;
        Striking = null;
        StrikeFor = -1;
        swing(InteractionHand.MAIN_HAND);
        if (Target != null && Target.isAlive() && canAttack(Target) && inReach(Target, HitGrace)) {
            Scp3199Sounds.play(World, Target.position(), List.of(Scp3199Registry.AttackSnap), 0.9f, voicePitch());
            doHurtTarget(Target);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity Target) {
        LivingEntity Before = getTarget();
        super.setTarget(Target);
        if (Target != null && Target != Before && SpotWait <= 0 && isAlive() && level() instanceof ServerLevel World) {
            SpotWait = SpotCooldown;
            Scp3199Sounds.play(World, getEyePosition(), Scp3199Registry.ShriekLayers, 1.0f, voicePitch());
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && isAlive() && level() instanceof ServerLevel) {
            triggerAnim("action", "hurt");
            if (StrikeFor >= 0 && StrikeFor < AttackCallTick) {
                StrikeFor = -1;
                Striking = null;
            }
        }
        return Hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            boolean Stepped = Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive() && !isLaying() && !isSwimmingNow(), gait());
            LayingFor = isLaying() ? LayingFor + 1 : 0;
            if (Stepped && isAlive()) {
                footsteps();
            }
        }
    }

    public double[] contacts() {
        return isRunning() ? RunContacts : WalkContacts;
    }

    public double strideOffset() {
        return Gaits == null ? 0.0 : Gaits.strideOffset();
    }

    public static boolean crosses(double From, double To, double Contact) {
        return Math.floor(To - Contact) > Math.floor(From - Contact);
    }

    private void footsteps() {
        double Offset = strideOffset();
        double[] Contacts = contacts();
        for (int Index = 0; Index < Contacts.length; Index++) {
            if (!crosses(Stride.from() - Offset, Stride.to() - Offset, Contacts[Index])) {
                continue;
            }
            double Side = (Index == 0 ? 1.0 : -1.0) * StepSide * growthScale();
            float Yaw = yBodyRot * Mth.DEG_TO_RAD;
            double X = getX() + Mth.cos(Yaw) * Side;
            double Z = getZ() + Mth.sin(Yaw) * Side;
            float Pitch = voicePitch();
            float Volume = StepVolume * (0.7f + 0.3f * growthScale());
            level().playLocalSound(X, getY(), Z, Scp3199Registry.Step.get(), SoundSource.HOSTILE, Volume, Pitch, false);
            level().playLocalSound(X, getY(), Z, Scp3199Registry.StepWet.get(), SoundSource.HOSTILE, Volume, Pitch, false);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (AttackWait > 0) {
            AttackWait--;
        }
        if (SpotWait > 0) {
            SpotWait--;
        }
        strikeTick(World);
        swimTick(World);
        if (!isAdult() && tickCount % 20 == 0) {
            entityData.set(AgeData, Math.min(GrowTicks, growth() + 20));
            applyGrowth();
        }
        Nesting.tick(World, this);
    }

    private void swimTick(ServerLevel World) {
        if (!isSwimmingNow()) {
            SwimFor = 0;
            return;
        }
        if (SwimFor % SwimStroke == 0) {
            Scp3199Sounds.play(World, position(), List.of(Scp3199Registry.Swim), 0.6f, voicePitch());
        }
        SwimFor++;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            Scp3199Sounds.play(World, getEyePosition(), Scp3199Registry.HurtLayers, getSoundVolume(), voicePitch());
        }
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void die(DamageSource Source) {
        boolean Fresh = !isRemoved() && !dead;
        if (Fresh && level() instanceof ServerLevel World) {
            Nesting.abandon(World, this);
        }
        super.die(Source);
        if (Fresh && level() instanceof ServerLevel World) {
            Scp3199Sounds.play(World, getEyePosition(), Scp3199Registry.DeathLayers, 1.0f, voicePitch());
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= Scp3199GaitController.DeathSpan && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Growth", growth());
        Tag.put("Nesting", Nesting.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Growth")) {
            entityData.set(AgeData, Mth.clamp(Tag.getInt("Growth"), 0, GrowTicks));
        }
        Nesting.load(Tag.getCompound("Nesting"), this);
        refreshDimensions();
        applyGrowth();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Gaits = new Scp3199GaitController(this);
        Controllers.add(Gaits);
        Controllers.add(new AnimationController<>(this, "action", ActionBlend, Scp3199::overlay).receiveTriggeredAnimations().triggerableAnim("attack", AttackOnce)
                .triggerableAnim("hurt", HurtOnce));
    }

    private static PlayState overlay(AnimationState<Scp3199> Event) {
        Scp3199 Owner = Event.getAnimatable();
        return Event.getController().isPlayingTriggeredAnimation() && Owner.isAlive() && !Owner.isLaying() ? PlayState.CONTINUE : PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
