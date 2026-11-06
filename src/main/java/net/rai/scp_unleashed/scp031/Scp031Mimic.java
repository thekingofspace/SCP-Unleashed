package net.rai.scp_unleashed.scp031;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

public class Scp031Mimic extends PathfinderMob implements GeoEntity {
    public static final float Width = 0.6f;
    public static final float Height = 1.95f;
    public static final float EyeHeight = 1.74f;
    public static final float CrouchHeight = 1.65f;
    public static final float CrouchEyeHeight = 1.42f;
    public static final float MaxHealth = 20.0f;
    public static final float DamageTaken = 0.5f;
    public static final double WalkSpeed = 0.3;
    public static final double ApproachSpeed = 0.96;
    public static final double FriendlySpeed = 0.8;
    public static final double StrollSpeed = 0.62;
    public static final double NoticeRange = 32.0;
    public static final double LoseRange = 40.0;
    public static final double FriendlyRange = 12.0;
    public static final double GrabRange = 2.5;
    public static final double HugDistance = 0.85;
    public static final double HoldBreakDistance = 4.0;
    public static final float HoldDamage = 2.0f;
    public static final int HoldPulse = 14;
    public static final float HoldBreakDamage = 8.0f;
    public static final int BreakCooldown = 100;
    public static final int KillCooldown = 80;
    public static final int RetargetInterval = 10;
    public static final int CensusInterval = 100;
    public static final int WaveChance = 12;
    public static final double DisguiseRange = 64.0;
    public static final double DisguiseBand = 2.0;
    public static final double RenderRange = 160.0;
    public static final int HeadTwist = 50;
    public static final int HeadLerpSteps = 1;
    public static final Vector3f Pink = new Vector3f(1.0f, 0.56f, 0.7f);
    public static final Vector3f Rose = new Vector3f(0.86f, 0.2f, 0.26f);
    private static final EntityDataAccessor<Boolean> EmbraceData = SynchedEntityData.defineId(Scp031Mimic.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation PulseLoop = RawAnimation.begin().thenLoop("pulse");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID HoldTarget;
    @Nullable
    private Vec3 HoldAnchor;
    private int HoldTicks;
    private float HoldDamageTaken;
    private int GrabCooldown;
    private int CrouchClock;
    private int CrouchHalf = 3;
    private int CrouchToggles = 4;
    private int CrouchPause;
    private boolean Friendly;
    private boolean ViewKnown;
    private boolean ViewNear;

    public Scp031Mimic(EntityType<? extends Scp031Mimic> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 5;
        lookControl = new Scp031LookControl(this);
    }

    @Override
    public int getMaxHeadYRot() {
        return HeadTwist;
    }

    @Override
    public int getMaxHeadXRot() {
        return (int) Scp031Gaze.MaxPitch;
    }

    @Override
    public void lerpHeadTo(float Yaw, int Steps) {
        super.lerpHeadTo(Yaw, HeadLerpSteps);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, WalkSpeed).add(Attributes.FOLLOW_RANGE, LoseRange)
                .add(Attributes.ARMOR, 0.0).add(Attributes.STEP_HEIGHT, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(EmbraceData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp031Goals.Embrace(this));
        goalSelector.addGoal(2, new Scp031Goals.Approach(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, StrollSpeed));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public boolean isEmbracing() {
        return entityData.get(EmbraceData);
    }

    public boolean isHolding() {
        return HoldTarget != null;
    }

    public boolean isFriendly() {
        return Friendly;
    }

    public int grabCooldown() {
        return GrabCooldown;
    }

    @Nullable
    public LivingEntity holdTarget() {
        if (HoldTarget == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(HoldTarget) instanceof LivingEntity Found ? Found : null;
    }

    public void setCrouched(boolean Down) {
        Pose Wanted = Down ? Pose.CROUCHING : Pose.STANDING;
        if (getPose() != Wanted) {
            setPose(Wanted);
        }
        if (isShiftKeyDown() != Down) {
            setShiftKeyDown(Down);
        }
    }

    public void setFriendly(boolean Value) {
        Friendly = Value;
        if (!Value) {
            CrouchPause = 0;
            CrouchClock = 0;
        }
    }

    public void crouchSpam() {
        if (CrouchPause > 0) {
            CrouchPause--;
            setCrouched(false);
            return;
        }
        if (++CrouchClock < CrouchHalf) {
            return;
        }
        CrouchClock = 0;
        CrouchHalf = 2 + random.nextInt(2);
        boolean Down = getPose() == Pose.CROUCHING;
        setCrouched(!Down);
        if (Down && --CrouchToggles <= 0) {
            CrouchToggles = 3 + random.nextInt(4);
            CrouchPause = 10 + random.nextInt(30);
        }
        if (!Down && random.nextInt(WaveChance) == 0) {
            swing(InteractionHand.MAIN_HAND);
        }
    }

    public boolean mayGrab(LivingEntity Target) {
        if (GrabCooldown > 0 || !Scp031Targets.isValid(Target) || heldByOther(Target)) {
            return false;
        }
        return !(Target instanceof Player) || level().getDifficulty() != Difficulty.PEACEFUL;
    }

    public boolean heldByOther(LivingEntity Target) {
        UUID Holder = Scp031Holds.holderOf(Target);
        return Holder != null && !Holder.equals(getUUID());
    }

    public void beginHold(LivingEntity Target) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        HoldTarget = Target.getUUID();
        HoldAnchor = Target.position();
        HoldTicks = 0;
        HoldDamageTaken = 0.0f;
        setFriendly(false);
        setCrouched(false);
        getNavigation().stop();
        entityData.set(EmbraceData, true);
        Scp031Holds.hold(Target, this, HoldAnchor, World.getServer().getTickCount());
        World.playSound(null, Target.getX(), Target.getY() + 1.0, Target.getZ(), Scp031Registry.Embrace.get(), SoundSource.HOSTILE, 0.9f, 0.95f + random.nextFloat() * 0.1f);
        World.playSound(null, Target.getX(), Target.getY() + 1.0, Target.getZ(), Scp031Registry.EmbraceSoft.get(), SoundSource.HOSTILE, 0.6f, 0.9f + random.nextFloat() * 0.1f);
        hearts(World, Target, 6);
    }

    public void releaseHold(boolean Broken, int Cooldown) {
        LivingEntity Target = holdTarget();
        if (Target != null && Scp031Holds.isHeldBy(Target, this)) {
            Scp031Holds.release(Target);
        }
        HoldTarget = null;
        HoldAnchor = null;
        HoldTicks = 0;
        HoldDamageTaken = 0.0f;
        GrabCooldown = Math.max(GrabCooldown, Cooldown);
        entityData.set(EmbraceData, false);
        if (Broken && level() instanceof ServerLevel World) {
            World.playSound(null, getX(), getY() + 1.0, getZ(), Scp031Registry.Release.get(), SoundSource.HOSTILE, 0.9f, 0.9f + random.nextFloat() * 0.2f);
            if (Target != null) {
                Vec3 Away = position().subtract(Target.position()).multiply(1.0, 0.0, 1.0);
                if (Away.lengthSqr() > 1.0e-4) {
                    Away = Away.normalize();
                    setDeltaMovement(Away.x * 0.45, 0.2, Away.z * 0.45);
                    hasImpulse = true;
                }
            }
        }
    }

    private void hearts(ServerLevel World, LivingEntity Target, int Count) {
        Vec3 Middle = position().add(Target.position()).scale(0.5);
        for (int Index = 0; Index < Count; Index++) {
            World.sendParticles(new DustParticleOptions(Index % 2 == 0 ? Pink : Rose, 0.9f), Middle.x, Middle.y + 1.1 + random.nextDouble() * 0.5, Middle.z, 1, 0.25, 0.2, 0.25, 0.0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (tickCount % CensusInterval == 0) {
            Scp031Census.get(World).touch(getUUID(), World.getGameTime());
        }
        if (GrabCooldown > 0) {
            GrabCooldown--;
        }
        if (isHolding()) {
            holdTick(World);
            return;
        }
        if (isEmbracing()) {
            entityData.set(EmbraceData, false);
        }
        if (tickCount % RetargetInterval == 0) {
            retarget(World);
        }
        if (getTarget() == null) {
            setFriendly(false);
            setCrouched(false);
        }
    }

    private void holdTick(ServerLevel World) {
        LivingEntity Target = holdTarget();
        if (Target == null || !Scp031Targets.isValid(Target) || Target.level() != level() || distanceTo(Target) > HoldBreakDistance || heldByOther(Target)) {
            boolean Killed = Target != null && Target.isDeadOrDying();
            releaseHold(false, Killed ? KillCooldown : 20);
            return;
        }
        if (HoldAnchor == null) {
            HoldAnchor = Target.position();
        }
        Scp031Holds.hold(Target, this, HoldAnchor, World.getServer().getTickCount());
        HoldTicks++;
        if (HoldTicks % HoldPulse == 0) {
            World.playSound(null, Target.getX(), Target.getY() + 1.0, Target.getZ(), Scp031Registry.Squeeze.get(), SoundSource.HOSTILE, 0.55f, 0.9f + random.nextFloat() * 0.15f);
            hearts(World, Target, 2 + random.nextInt(2));
            Target.hurt(damageSources().source(Scp031Registry.EmbraceDamage, this), HoldDamage);
        }
    }

    private void retarget(ServerLevel World) {
        LivingEntity Current = getTarget();
        if (Current != null && (!Scp031Targets.isValid(Current) || Current.level() != level() || distanceTo(Current) > LoseRange || heldByOther(Current))) {
            setTarget(null);
            Current = null;
        }
        if (Current instanceof Player) {
            return;
        }
        Player BestPlayer = null;
        double BestPlayerDistance = NoticeRange;
        for (Player Candidate : World.players()) {
            if (!Scp031Targets.isValid(Candidate) || heldByOther(Candidate)) {
                continue;
            }
            double Distance = distanceTo(Candidate);
            if (Distance < BestPlayerDistance && hasLineOfSight(Candidate)) {
                BestPlayerDistance = Distance;
                BestPlayer = Candidate;
            }
        }
        if (BestPlayer != null) {
            setTarget(BestPlayer);
            return;
        }
        if (Current != null) {
            return;
        }
        LivingEntity BestOther = null;
        double BestOtherDistance = NoticeRange;
        AABB Search = getBoundingBox().inflate(NoticeRange, 8.0, NoticeRange);
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, Search, Found -> !(Found instanceof Player) && Scp031Targets.isValid(Found))) {
            if (heldByOther(Candidate)) {
                continue;
            }
            double Distance = distanceTo(Candidate);
            if (Distance < BestOtherDistance && hasLineOfSight(Candidate)) {
                BestOtherDistance = Distance;
                BestOther = Candidate;
            }
        }
        if (BestOther != null) {
            setTarget(BestOther);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Full = Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || Source.is(DamageTypeTags.BYPASSES_RESISTANCE);
        float Before = getHealth();
        boolean Hit = super.hurt(Source, Full ? Amount : Amount * DamageTaken);
        if (Hit && !level().isClientSide && isHolding()) {
            Entity Attacker = Source.getEntity();
            boolean FromVictim = Attacker != null && Attacker.getUUID().equals(HoldTarget);
            if (!FromVictim) {
                HoldDamageTaken += Math.max(0.0f, Before - getHealth());
            }
            if (HoldDamageTaken >= HoldBreakDamage && isAlive()) {
                releaseHold(true, BreakCooldown);
            }
        }
        return Hit;
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
        if (!isHolding()) {
            super.knockback(Strength, X, Z);
        }
    }

    @Override
    public boolean isPushable() {
        return !isHolding() && super.isPushable();
    }

    @Override
    public void push(Entity Other) {
        if (!isHolding()) {
            super.push(Other);
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (level() instanceof ServerLevel World) {
            Scp031Census.get(World).touch(getUUID(), World.getGameTime());
        }
    }

    @Override
    public void die(DamageSource Source) {
        if (isHolding()) {
            releaseHold(false, 0);
        }
        super.die(Source);
        if (dead && level() instanceof ServerLevel World) {
            Scp031Census.get(World).free(getUUID());
            World.playSound(null, getX(), getY() + 0.5, getZ(), Scp031Registry.Collapse.get(), SoundSource.HOSTILE, 0.8f, 0.9f + random.nextFloat() * 0.15f);
            for (int Index = 0; Index < 10; Index++) {
                World.sendParticles(new DustParticleOptions(Index % 2 == 0 ? Rose : Pink, 1.1f), getX(), getY() + 0.3 + random.nextDouble() * 1.2, getZ(), 1, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    @Override
    public void remove(Entity.RemovalReason Reason) {
        if (!level().isClientSide && isHolding()) {
            releaseHold(false, 0);
        }
        if (level() instanceof ServerLevel World && (Reason == Entity.RemovalReason.DISCARDED || Reason == Entity.RemovalReason.CHANGED_DIMENSION || Reason == Entity.RemovalReason.KILLED)) {
            Scp031Census.get(World).free(getUUID());
        }
        super.remove(Reason);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose Stance) {
        if (Stance == Pose.CROUCHING) {
            return EntityDimensions.scalable(Width, CrouchHeight).withEyeHeight(CrouchEyeHeight);
        }
        return super.getDefaultDimensions(Stance);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Range = RenderRange * getViewScale();
        return Distance < Range * Range;
    }

    public boolean seenNear(double Distance) {
        if (!ViewKnown) {
            ViewKnown = true;
            ViewNear = Distance < DisguiseRange;
        } else if (ViewNear && Distance > DisguiseRange + DisguiseBand) {
            ViewNear = false;
        } else if (!ViewNear && Distance < DisguiseRange - DisguiseBand) {
            ViewNear = true;
        }
        return ViewNear;
    }

    public boolean lastSeenNear() {
        return ViewKnown && ViewNear;
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
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp031Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp031Registry.Death.get();
    }

    public static Optional<Scp031Mimic> spawnAt(ServerLevel World, Vec3 Spot, float Yaw) {
        Scp031Mimic Mimic = Scp031Registry.Mimic.get().create(World);
        if (Mimic == null) {
            return Optional.empty();
        }
        Mimic.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Mimic.setYHeadRot(Yaw);
        Mimic.yBodyRot = Yaw;
        if (!World.addFreshEntity(Mimic)) {
            return Optional.empty();
        }
        return Optional.of(Mimic);
    }

    public static boolean roomFor(ServerLevel World, BlockPos Cell) {
        return World.noCollision(Scp031Registry.Mimic.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Cell)));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("GrabCooldown", GrabCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        GrabCooldown = Tag.getInt("GrabCooldown");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "pulse", 0, Event -> Event.setAndContinue(PulseLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
