package net.rai.scp_unleashed.scp001day.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;
import net.rai.scp_unleashed.scp001day.Scp001DaySunlight;
import net.rai.scp_unleashed.scp001day.Scp001DayVoice;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayExposure;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp001DayFlesh extends Monster implements GeoEntity {
    public static final int TargetCheckTicks = 40;
    public static final int SearchRadius = 18;
    public static final int SearchLimit = 1600;
    public static final int StunTicks = 70;
    public static final int FuseTicks = 40;
    public static final int SplitCooldown = 200;
    public static final double FuseRange = 3.0;
    public static final double FuseOdds = 0.35;
    public static final double SplitDistance = 10.0;
    public static final double LookCone = 0.57;
    public static final double HeldSlow = -0.35;
    public static final double HoldDistance = 1.1;
    public static final double LetGoDistance = 5.0;
    private static final DustParticleOptions Spatter = new DustParticleOptions(new Vector3f(0.55f, 0.08f, 0.08f), 1.4f);
    private static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation Walk = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation Run = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation Sneak = RawAnimation.begin().thenLoop("sneak");
    private static final RawAnimation HoldLoop = RawAnimation.begin().thenLoop("hold");
    private static final RawAnimation GrabOnce = RawAnimation.begin().then("grab", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation StunOnce = RawAnimation.begin().then("stunned", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation FuseOnce = RawAnimation.begin().then("fuse", Animation.LoopType.PLAY_ONCE);
    private static final EntityDataAccessor<Integer> MassData = SynchedEntityData.defineId(Scp001DayFlesh.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HoldingData = SynchedEntityData.defineId(Scp001DayFlesh.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SneakingData = SynchedEntityData.defineId(Scp001DayFlesh.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    public final Scp001DayForm Form;
    @Nullable
    private UUID Victim;
    @Nullable
    private UUID Carrying;
    @Nullable
    private BlockPos DragTarget;
    private int DragAge;
    private int Struggle;
    private int Stunned;
    private int SplitWait;
    private int NextReplay = 400;
    private double LastGap = -1.0;
    private int Receding;

    public Scp001DayFlesh(EntityType<? extends Scp001DayFlesh> Type, Level World, Scp001DayForm Form) {
        super(Type, World);
        this.Form = Form;
        this.xpReward = 0;
        if (!World.isClientSide) {
            entityData.set(MassData, Form.Weight);
        }
    }

    public static AttributeSupplier.Builder createAttributes(Scp001DayForm Form) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Form.Health).add(Attributes.MOVEMENT_SPEED, Form.Speed).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, Form == Scp001DayForm.Mass ? 0.8 : 0.2).add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(MassData, 1);
        Builder.define(HoldingData, false);
        Builder.define(SneakingData, false);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return Form == Scp001DayForm.Clinger ? new WallClimberNavigation(this, World) : super.createNavigation(World);
    }

    @Override
    public boolean onClimbable() {
        return Form == Scp001DayForm.Clinger && horizontalCollision;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new DragGoal());
        goalSelector.addGoal(2, new HuntGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, Candidate -> candidate(Candidate)));
    }

    public boolean candidate(LivingEntity Candidate) {
        return Candidate instanceof Player User && !User.isCreative() && !User.isSpectator() && !held(User);
    }

    public static boolean held(LivingEntity Body) {
        AttributeInstance Speed = Body.getAttribute(Attributes.MOVEMENT_SPEED);
        return Speed != null && Speed.hasModifier(Scp001DayRegistry.location("scp001_day_held"));
    }

    public int mass() {
        return entityData.get(MassData);
    }

    public void setMass(int Value) {
        entityData.set(MassData, Math.max(1, Math.min(Scp001DayForm.MassCap, Value)));
        refreshDimensions();
    }

    public boolean holding() {
        return entityData.get(HoldingData);
    }

    public boolean sneaking() {
        return entityData.get(SneakingData);
    }

    public boolean stunned() {
        return Stunned > 0;
    }

    @Nullable
    public UUID carrying() {
        return Carrying;
    }

    public void carry(@Nullable UUID Speaker) {
        if (Speaker != null) {
            Carrying = Speaker;
        }
    }

    @Nullable
    public UUID victim() {
        return Victim;
    }

    public float sizeScale() {
        return Form == Scp001DayForm.Mass ? (float) Math.cbrt(mass() / (double) Scp001DayForm.FuseFloor) : 1.0f;
    }

    public boolean pile() {
        return Form == Scp001DayForm.Mass && mass() >= Scp001DayForm.PileFloor;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose Pose) {
        return super.getDefaultDimensions(Pose).scale(sizeScale());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (MassData.equals(Key)) {
            refreshDimensions();
        }
    }

    @Override
    public boolean doHurtTarget(Entity Struck) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Stunned > 0) {
            Stunned--;
        }
        if (SplitWait > 0) {
            SplitWait--;
        }
        if (isOnFire() && Victim != null) {
            release(World, true);
        }
        LivingEntity Held = heldBody(World);
        if (Victim != null && (Held == null || !Held.isAlive() || Held.distanceTo(this) > LetGoDistance || Stunned > 0)) {
            release(World, false);
            Held = null;
        }
        if (Held != null) {
            pull(Held);
        }
        entityData.set(HoldingData, Held != null);
        if (tickCount % FuseTicks == 0) {
            Scp001DayFusion.tryFuse(World, this);
        }
        if (isAlive() && tickCount % 20 == 0 && Held == null) {
            considerSplit(World);
        }
        voice(World);
    }

    @Nullable
    public LivingEntity heldBody(ServerLevel World) {
        if (Victim == null) {
            return null;
        }
        Entity Found = World.getEntity(Victim);
        return Found instanceof LivingEntity Body ? Body : null;
    }

    public boolean grab(ServerLevel World, LivingEntity Target) {
        if (Victim != null || Stunned > 0 || held(Target) || !isAlive()) {
            return false;
        }
        Victim = Target.getUUID();
        Struggle = 0;
        DragTarget = null;
        DragAge = 0;
        if (Target instanceof ServerPlayer User) {
            Carrying = User.getUUID();
        }
        AttributeInstance Speed = Target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null && !Speed.hasModifier(Scp001DayRegistry.location("scp001_day_held"))) {
            Speed.addTransientModifier(new AttributeModifier(Scp001DayRegistry.location("scp001_day_held"), HeldSlow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        triggerAnim("action", "grab");
        World.playSound(null, getX(), getY() + 1.0, getZ(), Scp001DayRegistry.FleshGrab.get(), SoundSource.HOSTILE, 1.0f, 0.8f + 0.2f * random.nextFloat());
        return true;
    }

    public void release(ServerLevel World, boolean Recoil) {
        LivingEntity Held = heldBody(World);
        if (Held != null) {
            AttributeInstance Speed = Held.getAttribute(Attributes.MOVEMENT_SPEED);
            if (Speed != null) {
                Speed.removeModifier(Scp001DayRegistry.location("scp001_day_held"));
            }
            if (Recoil) {
                Vec3 Away = Held.position().subtract(position()).normalize();
                Held.push(Away.x * 0.4, 0.15, Away.z * 0.4);
                Held.hurtMarked = true;
            }
        }
        Victim = null;
        DragTarget = null;
        Struggle = 0;
        entityData.set(HoldingData, false);
        if (Recoil) {
            Stunned = StunTicks;
            getNavigation().stop();
            triggerAnim("action", "stunned");
            World.playSound(null, getX(), getY() + 1.0, getZ(), Scp001DayRegistry.FleshRecoil.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
            World.sendParticles(Spatter, getX(), getY() + getBbHeight() * 0.5, getZ(), 12, 0.3, 0.3, 0.3, 0.05);
        }
    }

    public void pull(LivingEntity Held) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Spot = position().add(Facing.scale(HoldDistance * sizeScale())).add(0.0, 0.05, 0.0);
        Vec3 Toward = Spot.subtract(Held.position());
        double Length = Toward.length();
        if (Length > 0.15) {
            Vec3 Nudge = Toward.scale(Math.min(1.0, Length) * Form.Pull / Math.max(0.3, Length));
            Held.setDeltaMovement(Held.getDeltaMovement().scale(0.7).add(Nudge.x, Math.max(0.0, Nudge.y * 0.4), Nudge.z));
            Held.hurtMarked = true;
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && Victim != null && Source.getEntity() != null && Source.getEntity().getUUID().equals(Victim)) {
            Struggle += Source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ? Form.Grip : 1;
            if (Struggle >= Form.Grip) {
                release((ServerLevel) level(), true);
            }
        }
        if (Source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && Victim != null && !level().isClientSide) {
            release((ServerLevel) level(), true);
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public boolean isSensitiveToWater() {
        return false;
    }

    public boolean lethalDay() {
        return Scp001DaySun.lethal(Scp001DaySun.direction(level().getDayTime()));
    }

    @Nullable
    public BlockPos findLight(ServerLevel World, BlockPos From) {
        boolean Day = lethalDay();
        ArrayDeque<BlockPos> Open = new ArrayDeque<>();
        Set<Long> Seen = new HashSet<>();
        Open.add(From);
        Seen.add(From.asLong());
        int Visited = 0;
        while (!Open.isEmpty() && Visited < SearchLimit) {
            BlockPos Here = Open.poll();
            Visited++;
            if (standable(World, Here)) {
                if (Day ? lit(World, Here) : World.canSeeSky(Here)) {
                    return Here;
                }
            }
            for (Direction Way : Direction.Plane.HORIZONTAL) {
                for (int Rise = -1; Rise <= 1; Rise++) {
                    BlockPos Next = Here.relative(Way).above(Rise);
                    if (Math.abs(Next.getX() - From.getX()) > SearchRadius || Math.abs(Next.getZ() - From.getZ()) > SearchRadius || !Seen.add(Next.asLong())) {
                        continue;
                    }
                    if (standable(World, Next)) {
                        Open.add(Next);
                    }
                }
            }
        }
        return null;
    }

    public static boolean standable(ServerLevel World, BlockPos Feet) {
        return World.getBlockState(Feet).getCollisionShape(World, Feet).isEmpty() && World.getBlockState(Feet.above()).getCollisionShape(World, Feet.above()).isEmpty()
                && World.getBlockState(Feet.below()).isFaceSturdy(World, Feet.below(), Direction.UP);
    }

    public static boolean lit(ServerLevel World, BlockPos Feet) {
        long Time = World.getDayTime();
        return Scp001DayExposure.cached(World, Scp001DaySunlight.far(World), Feet.getX() + 0.5, Feet.getY() + 0.9, Feet.getZ() + 0.5, Time) > 0.5f
                && Scp001DayExposure.cached(World, Scp001DaySunlight.far(World), Feet.getX() + 0.5, Feet.getY() + 1.5, Feet.getZ() + 0.5, Time) > 0.5f;
    }

    private void considerSplit(ServerLevel World) {
        LivingEntity Target = getTarget();
        if (Target == null || Stunned > 0) {
            Receding = 0;
            LastGap = -1.0;
            return;
        }
        double Gap = distanceTo(Target);
        Receding = LastGap >= 0.0 && Gap > LastGap + 0.2 ? Receding + 1 : Math.max(0, Receding - 1);
        LastGap = Gap;
        if (SplitWait <= 0 && mass() >= Scp001DayForm.Clinger.Weight && Gap > SplitDistance && Receding >= 2) {
            Scp001DayFusion.split(World, this, Target);
            SplitWait = SplitCooldown;
        }
    }

    private void voice(ServerLevel World) {
        if (Carrying == null || holding() || --NextReplay > 0) {
            return;
        }
        NextReplay = Scp001DayVoice.nextReplay(random);
        LivingEntity Target = getTarget();
        if (Target == null || Target.getUUID().equals(Carrying) || Target.distanceTo(this) > Scp001DayVoice.ReplayRange) {
            return;
        }
        if (!Scp001DayVoice.replay(this, Carrying, Target.getUUID())) {
            World.playSound(null, getX(), getY() + 1.0, getZ(), Scp001DayRegistry.FleshMurmur.get(), SoundSource.HOSTILE, 0.9f, 0.7f + 0.2f * random.nextFloat());
        }
    }

    public boolean behind(LivingEntity Target) {
        Vec3 Look = Target.getViewVector(1.0f).multiply(1.0, 0.0, 1.0).normalize();
        Vec3 Toward = position().subtract(Target.position()).multiply(1.0, 0.0, 1.0).normalize();
        return Look.dot(Toward) < -0.2;
    }

    public boolean watched(LivingEntity Target) {
        Vec3 Look = Target.getViewVector(1.0f).normalize();
        Vec3 Toward = position().add(0.0, getBbHeight() * 0.5, 0.0).subtract(Target.getEyePosition()).normalize();
        return Look.dot(Toward) > LookCone && Target.hasLineOfSight(this);
    }

    private final class HuntGoal extends Goal {
        HuntGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Victim == null && Stunned <= 0 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void stop() {
            entityData.set(SneakingData, false);
        }

        @Override
        public void tick() {
            LivingEntity Target = getTarget();
            if (Target == null || !(level() instanceof ServerLevel World)) {
                return;
            }
            double Gap = distanceTo(Target);
            if (Form == Scp001DayForm.Lurker) {
                boolean Seen = watched(Target);
                entityData.set(SneakingData, Gap < 20.0);
                if (Seen && Gap > 2.5) {
                    getNavigation().stop();
                    return;
                }
                Vec3 Back = Target.position().subtract(Target.getViewVector(1.0f).multiply(1.0, 0.0, 1.0).normalize().scale(1.6));
                getNavigation().moveTo(Back.x, Back.y, Back.z, Gap < 20.0 ? 0.7 : 1.0);
                if (Gap < Form.Reach && behind(Target)) {
                    grab(World, Target);
                }
                return;
            }
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
            getNavigation().moveTo(Target, lethalDay() ? 1.0 : 1.2);
            if (Gap < Form.Reach * sizeScale()) {
                grab(World, Target);
            }
        }
    }

    private final class DragGoal extends Goal {
        DragGoal() {
            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Victim != null;
        }

        @Override
        public void tick() {
            if (!(level() instanceof ServerLevel World)) {
                return;
            }
            LivingEntity Held = heldBody(World);
            if (Held == null) {
                return;
            }
            if (DragTarget == null || ++DragAge > TargetCheckTicks) {
                DragAge = 0;
                BlockPos Found = findLight(World, blockPosition());
                if (Found != null) {
                    DragTarget = Found;
                }
            }
            if (DragTarget == null) {
                getNavigation().stop();
                return;
            }
            double Left = Held.position().distanceTo(Vec3.atBottomCenterOf(DragTarget));
            if (Left > 1.2) {
                getNavigation().moveTo(DragTarget.getX() + 0.5, DragTarget.getY(), DragTarget.getZ() + 0.5, Form.DragSpeed);
            } else {
                getNavigation().stop();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Mass", mass());
        if (Carrying != null) {
            Tag.putUUID("Carrying", Carrying);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Mass")) {
            setMass(Tag.getInt("Mass"));
        }
        if (Tag.hasUUID("Carrying")) {
            Carrying = Tag.getUUID("Carrying");
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (!level().isClientSide && Victim != null) {
            release((ServerLevel) level(), false);
        }
        super.remove(Reason);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp001DayRegistry.FleshAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp001DayRegistry.FleshHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp001DayRegistry.FleshDeath.get();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 4, this::bodyState));
        Controllers.add(new AnimationController<>(this, "overlay", 3, this::overlayState));
        Controllers.add(new AnimationController<>(this, "action", 2, State -> PlayState.STOP).receiveTriggeredAnimations()
                .triggerableAnim("grab", GrabOnce).triggerableAnim("stunned", StunOnce).triggerableAnim("fuse", FuseOnce));
    }

    private PlayState bodyState(AnimationState<Scp001DayFlesh> State) {
        double Speed = getDeltaMovement().horizontalDistance();
        if (!State.isMoving() && Speed < 0.01) {
            return State.setAndContinue(Idle);
        }
        if (Form == Scp001DayForm.Lurker && sneaking()) {
            return State.setAndContinue(Sneak);
        }
        return State.setAndContinue(Speed > Form.Speed * 0.55 ? Run : Walk);
    }

    private PlayState overlayState(AnimationState<Scp001DayFlesh> State) {
        if (holding()) {
            return State.setAndContinue(HoldLoop);
        }
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    public static List<Scp001DayFlesh> near(ServerLevel World, Vec3 Centre, double Range) {
        List<Scp001DayFlesh> Found = new ArrayList<>(World.getEntitiesOfClass(Scp001DayFlesh.class, new AABB(Centre, Centre).inflate(Range), Entity::isAlive));
        Found.sort(Comparator.comparingDouble(Each -> Each.distanceToSqr(Centre)));
        return Found;
    }
}
