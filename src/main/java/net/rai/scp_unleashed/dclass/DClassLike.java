package net.rai.scp_unleashed.dclass;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class DClassLike extends PathfinderMob implements GeoEntity {
    public static final byte ActionNone = 0;
    public static final byte ActionEating = 1;
    public static final byte ActionEmerging = 2;
    public static final byte ActionMerging = 3;
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    public static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    public static final RawAnimation SitLoop = RawAnimation.begin().thenLoop("sit");
    public static final RawAnimation SleepLoop = RawAnimation.begin().thenLoop("sleep");
    public static final RawAnimation SwimLoop = RawAnimation.begin().thenLoop("swim");
    public static final RawAnimation TankLoop = RawAnimation.begin().thenLoop("tank");
    public static final RawAnimation EatLoop = RawAnimation.begin().thenLoop("eat");
    public static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    public static final RawAnimation MergeLoop = RawAnimation.begin().thenLoop("merge");
    public static final EntityDimensions RestingSize = EntityDimensions.scalable(0.8f, 0.45f).withEyeHeight(0.3f);
    protected static final EntityDataAccessor<Integer> AppearanceData = SynchedEntityData.defineId(DClassLike.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Boolean> RestingData = SynchedEntityData.defineId(DClassLike.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(DClassLike.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    public final HumanoidPose Mirror = new HumanoidPose();
    @Nullable
    private HumanoidGait Gait;
    @Nullable
    private HumanoidEyes Eyes;
    private boolean Puppet;
    private boolean Specimen;
    private boolean Undead;
    private float Swim;
    private float SwimO;
    private int FlyTicks;
    @Nullable
    private LivingEntity Owner;

    protected DClassLike(EntityType<? extends DClassLike> Type, Level World) {
        super(Type, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(AppearanceData, 0);
        Builder.define(RestingData, false);
        Builder.define(ActionData, ActionNone);
    }

    public int appearance() {
        return entityData.get(AppearanceData);
    }

    public void setAppearance(int Code) {
        entityData.set(AppearanceData, Code);
    }

    public void applySample(CompoundTag Sample) {
    }

    public boolean isResting() {
        return entityData.get(RestingData);
    }

    public void setResting(boolean Resting) {
        if (Resting != isResting()) {
            entityData.set(RestingData, Resting);
            refreshDimensions();
        }
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public void setAction(byte Kind) {
        entityData.set(ActionData, Kind);
    }

    public boolean isRestingPose() {
        return isResting() || isSleeping() || Puppet && Mirror.Sleeping;
    }

    public void markPuppet() {
        Puppet = true;
    }

    public boolean isPuppet() {
        return Puppet;
    }

    public void markUndead() {
        Undead = true;
    }

    public boolean isUndead() {
        return Undead;
    }

    public void markSpecimen() {
        Specimen = true;
    }

    public boolean isSpecimen() {
        return Specimen;
    }

    public float glanceRate() {
        return 1.0f;
    }

    public boolean eyesShut() {
        return isRestingPose() || Specimen || !isAlive();
    }

    @Nullable
    public HumanoidGait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new HumanoidGait();
        }
        return Gait;
    }

    @Nullable
    public HumanoidEyes eyes() {
        if (Eyes == null && level().isClientSide) {
            Eyes = new HumanoidEyes();
        }
        return Eyes;
    }

    public boolean isStriding() {
        HumanoidGait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        HumanoidGait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    public void setGaitPartial(float Partial) {
        HumanoidGait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickLook();
        }
    }

    public float swim(float Partial) {
        return SwimO + (Swim - SwimO) * Partial;
    }

    public int flyTicks() {
        return FlyTicks;
    }

    @Nullable
    public LivingEntity puppetOwner() {
        return Owner;
    }

    public void setPuppetOwner(@Nullable LivingEntity Controller) {
        Owner = Controller;
    }

    public void tickLook() {
        SwimO = Swim;
        boolean Flat = Puppet && (getPose() == Pose.SWIMMING || Mirror.FallFlying);
        Swim = Flat ? Math.min(1.0f, Swim + 0.09f) : Math.max(0.0f, Swim - 0.09f);
        FlyTicks = Puppet && Mirror.FallFlying ? FlyTicks + 1 : 0;
        HumanoidGait Current = gait();
        if (Current != null) {
            Current.tick(this, !Puppet && !Specimen);
        }
        HumanoidEyes Seeing = eyes();
        if (Seeing != null) {
            Seeing.tick(this, glanceRate(), eyesShut(), !Puppet && !Specimen);
        }
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose Current) {
        return isResting() ? RestingSize.scale(getAgeScale()) : super.getDefaultDimensions(Current);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (RestingData.equals(Accessor)) {
            refreshDimensions();
        }
    }

    public boolean isSwimmingPose() {
        if (Puppet) {
            return getPose() == Pose.SWIMMING || Mirror.FallFlying || Mirror.Spinning;
        }
        return isSwimming() || getPose() == Pose.SWIMMING || isFallFlying() || isInWater() && !onGround() && getFluidHeight(FluidTags.WATER) > 0.8;
    }

    public boolean isSittingPose() {
        return isPassenger() || Puppet && Mirror.Sitting;
    }

    protected RawAnimation bodyAnimation() {
        if (Specimen) {
            return TankLoop;
        }
        byte Current = action();
        if (Current == ActionEmerging) {
            return EmergePlay;
        }
        if (Current == ActionMerging) {
            return MergeLoop;
        }
        if (isRestingPose()) {
            return SleepLoop;
        }
        if (isSwimmingPose()) {
            return SwimLoop;
        }
        if (isSittingPose()) {
            return SitLoop;
        }
        if (isStriding()) {
            return isRunning() ? RunLoop : WalkLoop;
        }
        return IdleLoop;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new HumanoidGaitController(this, "body", 4, Event -> Event.setAndContinue(bodyAnimation())));
        Controllers.add(new AnimationController<>(this, "action", 3, Event -> action() == ActionEating ? Event.setAndContinue(EatLoop) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putInt("Appearance", appearance());
        Saved.putBoolean("Resting", isResting());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        setAppearance(Saved.getInt("Appearance"));
        setResting(Saved.getBoolean("Resting"));
    }
}
