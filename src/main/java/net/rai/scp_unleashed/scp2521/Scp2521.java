package net.rai.scp_unleashed.scp2521;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.motion.StrideGait;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class Scp2521 extends PathfinderMob implements GeoEntity {
    public static final float Width = 0.7f;
    public static final float Height = 3.4f;
    public static final float EyeHeight = 3.05f;
    public static final int RoleTaker = 0;
    public static final int RoleStarer = 1;
    public static final int RoleContained = 2;
    public static final int ModeRise = 0;
    public static final int ModeIdle = 1;
    public static final int ModeGrab = 2;
    public static final int ModeCarry = 3;
    public static final int ModeStare = 4;
    public static final int ModeSink = 5;
    public static final int ModeContained = 6;
    public static final int ModeHolding = 7;
    public static final int ModeBreak = 8;
    public static final int ModeReform = 9;
    public static final int HoldingTicks = 90;
    public static final int AbsorbFrom = 50;
    public static final int BreakTicks = 28;
    public static final int SinkTicks = 40;
    public static final int ReformTicks = 90;
    public static final int RustleEvery = 70;
    public static final RawAnimation RiseThenIdle = RawAnimation.begin().thenPlay("rise").thenLoop("idle");
    public static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation GrabOnce = RawAnimation.begin().thenPlayAndHold("grab");
    public static final RawAnimation CarryOnce = RawAnimation.begin().thenPlayAndHold("carry");
    public static final RawAnimation StareLoop = RawAnimation.begin().thenLoop("stare");
    public static final RawAnimation SinkOnce = RawAnimation.begin().thenPlayAndHold("sink");
    public static final RawAnimation ContainedLoop = RawAnimation.begin().thenLoop("contained");
    public static final RawAnimation HoldingFlow = RawAnimation.begin().thenPlay("rise").thenPlay("absorb").thenLoop("contained");
    public static final RawAnimation BreakOnce = RawAnimation.begin().thenPlayAndHold("break");
    public static final RawAnimation ReformFlow = RawAnimation.begin().thenPlay("sink").thenPlay("rise").thenLoop("contained");
    private static final EntityDataAccessor<Integer> ModeData = SynchedEntityData.defineId(Scp2521.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ModeTimeData = SynchedEntityData.defineId(Scp2521.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> RoleData = SynchedEntityData.defineId(Scp2521.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SquashData = SynchedEntityData.defineId(Scp2521.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SandData = SynchedEntityData.defineId(Scp2521.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GazeData = SynchedEntityData.defineId(Scp2521.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LeanData = SynchedEntityData.defineId(Scp2521.class, EntityDataSerializers.FLOAT);
    public static final float ClientEase = 0.2f;
    public static final double WalkDistance = 1.8;
    public static final double TeleportStep = 0.6;
    public static final float WalkEase = 0.15f;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.006, 99.0, 98.0, WalkDistance, WalkDistance, 0.6, Scp2521GaitController.StrideBlend);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID Owner;
    @Nullable
    private BlockPos Anchor;
    public float SwayClock;
    public float SwayClockBefore;
    public float GazeShown;
    public float GazeShownBefore;
    public float LeanShown;
    public float LeanShownBefore;
    public float WalkShown;
    public float WalkShownBefore;
    private final StrideGait Stride = new StrideGait();
    private float RenderPartial;
    private final Scp2521Idle Idle = new Scp2521Idle();

    public Scp2521(EntityType<? extends Scp2521> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ModeData, ModeRise);
        Builder.define(ModeTimeData, 0L);
        Builder.define(RoleData, RoleTaker);
        Builder.define(SquashData, 1.0f);
        Builder.define(SandData, 0);
        Builder.define(GazeData, 0.0f);
        Builder.define(LeanData, 0.0f);
    }

    public int mode() {
        return entityData.get(ModeData);
    }

    public int role() {
        return entityData.get(RoleData);
    }

    public long modeTime() {
        return entityData.get(ModeTimeData);
    }

    public float squash() {
        return entityData.get(SquashData);
    }

    public int sand() {
        return entityData.get(SandData);
    }

    public float gaze() {
        return entityData.get(GazeData);
    }

    public void setGaze(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        if (Math.abs(Clamped - gaze()) > 1.0e-4f) {
            entityData.set(GazeData, Clamped);
        }
    }

    public float lean() {
        return entityData.get(LeanData);
    }

    public void setLean(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        if (Math.abs(Clamped - lean()) > 1.0e-4f) {
            entityData.set(LeanData, Clamped);
        }
    }

    public float gazeShown(float Partial) {
        return Mth.lerp(Partial, GazeShownBefore, GazeShown);
    }

    public float leanShown(float Partial) {
        return Mth.lerp(Partial, LeanShownBefore, LeanShown);
    }

    public Scp2521Idle idle() {
        return Idle;
    }

    public int modeTicks() {
        return (int) Math.max(0L, level().getGameTime() - modeTime());
    }

    public float modeSeconds(float Partial) {
        return (level().getGameTime() - modeTime() + Partial) / 20.0f;
    }

    public void setMode(int Mode) {
        entityData.set(ModeData, Mode);
        entityData.set(ModeTimeData, level().getGameTime());
    }

    public void setRole(int Role) {
        entityData.set(RoleData, Role);
    }

    public void setSquash(float Squash) {
        entityData.set(SquashData, Mth.clamp(Squash, 0.5f, 1.0f));
    }

    public void setSand(int StateId) {
        entityData.set(SandData, StateId);
    }

    @Nullable
    public UUID owner() {
        return Owner;
    }

    public void setOwner(@Nullable UUID Id) {
        Owner = Id;
    }

    @Nullable
    public BlockPos anchor() {
        return Anchor;
    }

    public void setAnchor(@Nullable BlockPos Pos) {
        Anchor = Pos;
    }

    public boolean contained() {
        return role() == RoleContained;
    }

    public void face(Vec3 Target) {
        double Dx = Target.x - getX();
        double Dz = Target.z - getZ();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        yRotO = Yaw;
        setYHeadRot(Yaw);
        yHeadRotO = Yaw;
        setYBodyRot(Yaw);
        yBodyRotO = Yaw;
    }

    public void turn(float Yaw) {
        setYRot(Yaw);
        yRotO = Yaw;
        setYHeadRot(Yaw);
        yHeadRotO = Yaw;
        setYBodyRot(Yaw);
        yBodyRotO = Yaw;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide) {
            SwayClockBefore = SwayClock;
            SwayClock += Scp2521Motion.speed(this) / 20.0f;
            GazeShownBefore = GazeShown;
            GazeShown += (gaze() - GazeShown) * ClientEase;
            LeanShownBefore = LeanShown;
            LeanShown += (lean() - LeanShown) * ClientEase;
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            double Moved = Math.sqrt(Dx * Dx + Dz * Dz);
            Stride.tick(Moved > TeleportStep ? 0.0 : Moved, isAlive(), gait());
            WalkShownBefore = WalkShown;
            WalkShown += ((striding() ? 1.0f : 0.0f) - WalkShown) * WalkEase;
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (role() == RoleContained) {
            Scp2521Containment.tickCreature(World, this);
        } else if (!Scp2521Trips.wants(World.getServer(), this)) {
            discard();
        }
    }

    public void play(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY() + 1.5, getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void kill() {
        if (level().isClientSide) {
            return;
        }
        if (contained() && mode() != ModeBreak) {
            Scp2521Containment.reform(this);
        }
    }

    @Override
    protected void onBelowWorld() {
        if (contained() && Anchor != null) {
            setPos(Anchor.getX() + 0.5, Anchor.getY(), Anchor.getZ() + 0.5);
            return;
        }
        discard();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Nullable
    @Override
    public Entity changeDimension(DimensionTransition Transition) {
        return null;
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
    public void checkDespawn() {
    }

    @Override
    public boolean shouldBeSaved() {
        return contained() && super.shouldBeSaved();
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 128.0 * 128.0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putInt("Scp2521Role", role());
        Saved.putInt("Scp2521Mode", mode());
        Saved.putFloat("Scp2521Squash", squash());
        if (Owner != null) {
            Saved.putUUID("Scp2521Owner", Owner);
        }
        if (Anchor != null) {
            Saved.putLong("Scp2521Anchor", Anchor.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        setRole(Saved.getInt("Scp2521Role"));
        int Mode = Saved.getInt("Scp2521Mode");
        entityData.set(ModeData, Mode == ModeHolding || Mode == ModeReform ? ModeContained : Mode);
        if (Saved.contains("Scp2521Squash")) {
            setSquash(Saved.getFloat("Scp2521Squash"));
        }
        Owner = Saved.hasUUID("Scp2521Owner") ? Saved.getUUID("Scp2521Owner") : null;
        Anchor = Saved.contains("Scp2521Anchor") ? BlockPos.of(Saved.getLong("Scp2521Anchor")) : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp2521GaitController(this, false));
        Controllers.add(new Scp2521GaitController(this, true));
    }

    public StrideGait.Shape gait() {
        float Squash = squash();
        if (Squash >= 1.0f) {
            return Gait;
        }
        return new StrideGait.Shape(Gait.StillPace() * Squash, Gait.RunEnter(), Gait.RunExit(), WalkDistance * Squash, WalkDistance * Squash, Gait.MaxStep(), Gait.Blend());
    }

    public StrideGait stride() {
        return Stride;
    }

    public boolean striding() {
        return Stride.stride() != StrideGait.Idle;
    }

    public float walkShown(float Partial) {
        return Mth.lerp(Partial, WalkShownBefore, WalkShown);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public static boolean walksUpright(int Mode) {
        return Mode == ModeContained || Mode == ModeIdle;
    }

    public static RawAnimation animationFor(int Mode) {
        return switch (Mode) {
            case ModeRise -> RiseThenIdle;
            case ModeGrab -> GrabOnce;
            case ModeCarry -> CarryOnce;
            case ModeStare -> StareLoop;
            case ModeSink -> SinkOnce;
            case ModeContained -> ContainedLoop;
            case ModeHolding -> HoldingFlow;
            case ModeBreak -> BreakOnce;
            case ModeReform -> ReformFlow;
            default -> IdleLoop;
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Nullable
    public BlockState sandState() {
        int Id = sand();
        return Id <= 0 ? null : Block.stateById(Id);
    }

    public static boolean isPlayerNear(Player User, Entity Creature, double Range) {
        return User.distanceToSqr(Creature) <= Range * Range;
    }
}
