package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.rai.scp_unleashed.motion.ActionClock;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class Scp407Sprout extends PathfinderMob implements GeoEntity, Scp407Bound, Scp407Animated, IEntityWithComplexSpawn, ActionClock.Owner {
    public static final float Width = 0.5f;
    public static final float Height = 0.8f;
    public static final float EyeHeight = 0.62f;
    public static final double Health = 6.0;
    public static final double Speed = 0.27;
    public static final double StrollModifier = 0.9;
    public static final double PanicModifier = 1.3;
    public static final double FleeWalk = 0.95;
    public static final double FleeRun = 0.95;
    public static final float FleeRange = 7.0f;
    public static final int HopChance = 140;
    public static final double HopLift = 0.38;
    public static final double HopPush = 0.12;
    public static final int AmbientInterval = 90;
    public static final int BindCheckTicks = 20;
    public static final int DropTicks = 6;
    public static final byte ActionNone = 0;
    public static final byte ActionHatch = 1;
    public static final byte ActionHop = 2;
    public static final byte ActionChirp = 3;
    public static final byte ActionHeld = 4;
    public static final byte ActionWither = 5;
    public static final byte ActionStartle = 6;
    public static final Scp407Gait.Shape SproutGait = new Scp407Gait.Shape(
            new Scp407Gait.Profile(Scp407RigData.SproutWalkCycle, Scp407RigData.SproutWalkReference, 0.5, 2.2, Scp407RigData.SproutWalkContacts, new float[]{1.0f, 1.0f}),
            new Scp407Gait.Profile(Scp407RigData.SproutRunCycle, Scp407RigData.SproutRunReference, 0.6, 1.8, Scp407RigData.SproutRunContacts, new float[]{1.0f, 1.0f}),
            Scp407RigData.SproutWalkReference * 1.6, Scp407RigData.SproutWalkReference * 1.25, 1, Scp407Registry.SproutStep, Scp407Registry.SproutStep, 0.25f, 0.0f, 1.25f, 12.0);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp407Sprout.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp407Sprout.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation HeldLoop = RawAnimation.begin().thenLoop("held");
    private static final RawAnimation HatchPlay = RawAnimation.begin().thenPlayAndHold("hatch");
    private static final RawAnimation HopPlay = RawAnimation.begin().thenPlayAndHold("hop");
    private static final RawAnimation ChirpPlay = RawAnimation.begin().thenPlayAndHold("chirp");
    private static final RawAnimation WitherPlay = RawAnimation.begin().thenPlayAndHold("wither");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ActionClock Clock = new ActionClock();
    private int ActionTicks;
    private int Session = Unbound;
    private int Loose;
    private int TickOrigin;
    private float RenderPartial;
    @Nullable
    private Scp407Gait Gait;

    public Scp407Sprout(EntityType<? extends Scp407Sprout> Type, Level World) {
        super(Type, World);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.STEP_HEIGHT, 0.6);
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
        goalSelector.addGoal(1, new HoldStill());
        goalSelector.addGoal(2, new PanicGoal(this, PanicModifier));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Scp407Titan.class, FleeRange, FleeWalk, FleeRun));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, StrollModifier));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isHeld() {
        return action() == ActionHeld;
    }

    public boolean locksMovement() {
        byte Current = action();
        return Current == ActionHatch || Current == ActionHeld || Current == ActionWither || Current == ActionStartle;
    }

    @Nullable
    @Override
    public String actionClip() {
        if (level().isClientSide) {
            return Clock.clip(clockNow());
        }
        return clipOf(action());
    }

    @Nullable
    private static String clipOf(byte Kind) {
        return switch (Kind) {
            case ActionHatch -> "hatch";
            case ActionHop -> "hop";
            case ActionChirp, ActionStartle -> "chirp";
            case ActionWither -> "wither";
            default -> null;
        };
    }

    public boolean isPerforming() {
        return level().isClientSide ? actionClip() != null || isHeld() : isActing();
    }

    @Override
    public ActionClock actionClock() {
        return Clock;
    }

    private double clockNow() {
        return tickCount + RenderPartial;
    }

    private void syncClock() {
        byte Current = action();
        if (Current == ActionNone) {
            Clock.finish();
            return;
        }
        String Clip = Current == ActionChirp && isStriding() ? null : clipOf(Current);
        Clock.begin(Current, Clip, tickCount, ActionClock.already(Clock.fresh(), tickCount, TickOrigin, entityData.get(ActionStartData)));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (level().isClientSide && (Key.equals(ActionData) || Key.equals(ActionStartData))) {
            syncClock();
        }
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind == ActionHatch || Kind == ActionHeld || Kind == ActionWither || Kind == ActionStartle) {
            getNavigation().stop();
        }
    }

    @Override
    public double actionElapsed() {
        if (!level().isClientSide) {
            return Math.max(0, tickCount - entityData.get(ActionStartData));
        }
        return Clock.elapsed(clockNow());
    }

    @Override
    public int boundSession() {
        return Session;
    }

    @Override
    public void bindSession(int Id) {
        Session = Id;
        if (Id != Unbound) {
            setPersistenceRequired();
        }
    }

    @Override
    public boolean isWithering() {
        return action() == ActionWither;
    }

    @Override
    public void beginWither() {
        if (isWithering() || !isAlive()) {
            return;
        }
        release();
        setAction(ActionWither, Scp407RigData.SproutWitherTicks);
        playSound(Scp407Registry.WitherCrackle.get(), 0.6f, 1.4f);
    }

    public void beginHatch() {
        setAction(ActionHatch, Scp407RigData.SproutHatchTicks);
    }

    public void startle(int Ticks) {
        byte Current = action();
        if (!isAlive() || Current == ActionHatch || Current == ActionHeld || Current == ActionWither) {
            return;
        }
        setAction(ActionStartle, Ticks);
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        playSound(Scp407Registry.SproutChirp.get(), 0.8f, 1.35f + random.nextFloat() * 0.15f);
    }

    public void grabbed() {
        if (!isAlive() || isWithering()) {
            return;
        }
        setAction(ActionHeld, Integer.MAX_VALUE);
        setNoGravity(true);
        Loose = 0;
        playSound(Scp407Registry.SproutHurt.get(), 0.9f, 1.3f);
    }

    public void holdAt(Vec3 Point, float Yaw) {
        Loose = 0;
        setPos(Point.x, Point.y, Point.z);
        setDeltaMovement(Vec3.ZERO);
        setYRot(Yaw);
        yBodyRot = Yaw;
        fallDistance = 0.0f;
    }

    public void release() {
        if (isHeld()) {
            entityData.set(ActionData, ActionNone);
            ActionTicks = 0;
        }
        setNoGravity(false);
    }

    public void absorbed() {
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp407Registry.Leaf.get(), getX(), getY() + 0.3, getZ(), 18, 0.25, 0.25, 0.25, 0.06);
            World.sendParticles(Scp407Registry.Spore.get(), getX(), getY() + 0.3, getZ(), 10, 0.2, 0.2, 0.2, 0.02);
        }
        discard();
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
            Scp407Gait Current = gait();
            if (Current != null) {
                Current.tick(this, locksMovement());
            }
            Clock.settle();
            return;
        }
        if (!isAlive()) {
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Session != Unbound && !isWithering() && (tickCount + getId()) % BindCheckTicks == 0 && !Scp407Sessions.get(World).isPlaying(Session)) {
            beginWither();
        }
        if (isHeld()) {
            if (++Loose > DropTicks) {
                release();
            }
            return;
        }
        if (ActionTicks > 0) {
            ActionTicks--;
            if (ActionTicks <= 0) {
                if (isWithering()) {
                    crumble(World);
                    return;
                }
                entityData.set(ActionData, ActionNone);
            }
        }
        if (!isActing() && onGround() && getNavigation().isInProgress() && random.nextInt(HopChance) == 0) {
            hop();
        }
    }

    private void hop() {
        setAction(ActionHop, Scp407RigData.SproutHopTicks);
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        setDeltaMovement(getDeltaMovement().add(Facing.x * HopPush, HopLift, Facing.z * HopPush));
        hasImpulse = true;
        playSound(Scp407Registry.SproutHop.get(), 0.5f, 1.1f + random.nextFloat() * 0.3f);
    }

    private void crumble(ServerLevel World) {
        World.sendParticles(Scp407Registry.Ash.get(), getX(), getY() + 0.3, getZ(), 14, 0.25, 0.2, 0.25, 0.02);
        World.sendParticles(Scp407Registry.Leaf.get(), getX(), getY() + 0.3, getZ(), 6, 0.25, 0.2, 0.25, 0.02);
        World.playSound(null, blockPosition(), Scp407Registry.WitherCrumble.get(), SoundSource.NEUTRAL, 0.6f, 1.3f);
        discard();
    }

    @Override
    public void playAmbientSound() {
        if (isActing() || !onGround()) {
            return;
        }
        setAction(ActionChirp, Scp407RigData.SproutChirpTicks);
        playSound(Scp407Registry.SproutChirp.get(), 0.7f, 0.95f + random.nextFloat() * 0.2f);
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp407Registry.SproutChirp.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp407Registry.SproutHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp407Registry.SproutDeath.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isHeld() && super.isPushable();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && (isWithering() || action() == ActionHatch)) {
            return false;
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp407Registry.Leaf.get(), getX(), getY() + 0.3, getZ(), 12, 0.25, 0.2, 0.25, 0.04);
        }
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return Session != Unbound || super.requiresCustomPersistence();
    }

    @Nullable
    @Override
    public Scp407Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp407Gait(SproutGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp407Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        Scp407Gait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp407Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    public float witherShare(float Partial) {
        if (!isWithering()) {
            return 0.0f;
        }
        return (float) Math.min(1.0, actionElapsed() / Scp407RigData.SproutWitherTicks);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Data) {
        super.addAdditionalSaveData(Data);
        Data.putInt("Scp407Session", Session);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Data) {
        super.readAdditionalSaveData(Data);
        Session = Data.contains("Scp407Session") ? Data.getInt("Scp407Session") : Unbound;
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        setNoGravity(false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp407GaitController<>(this, "body", 0, Event -> {
            String Shown = actionClip();
            boolean Held = isHeld();
            Event.getController().transitionLength(Shown == null && !Held ? Scp407GaitController.LoopBlend : Scp407GaitController.ActionBlend);
            if (Held) {
                return Event.setAndContinue(HeldLoop);
            }
            if (Shown != null) {
                RawAnimation Play = switch (Shown) {
                    case "hatch" -> HatchPlay;
                    case "wither" -> WitherPlay;
                    case "hop" -> HopPlay;
                    default -> ChirpPlay;
                };
                return Event.setAndContinue(Play);
            }
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? RunLoop : WalkLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    private class HoldStill extends Goal {
        HoldStill() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return locksMovement();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }
    }
}
