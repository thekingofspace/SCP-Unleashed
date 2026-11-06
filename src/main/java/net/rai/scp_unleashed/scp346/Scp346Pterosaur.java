package net.rai.scp_unleashed.scp346;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.motion.ActionClock;
import net.rai.scp_unleashed.motion.PaceMeter;
import net.rai.scp_unleashed.motion.Settle;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Map;

public class Scp346Pterosaur extends TamableAnimal implements GeoEntity, FlyingAnimal, ActionClock.Owner {
    public static final float Width = 0.5f;
    public static final float Height = 0.75f;
    public static final float EyeHeight = 0.66f;
    public static final double Health = 6.0;
    public static final double WalkSpeed = 0.2;
    public static final double FlySpeed = 0.5;
    public static final double FollowRange = 24.0;
    public static final int TameChance = 3;
    public static final float FeedHeal = 2.0f;
    public static final byte ActionNone = 0;
    public static final byte ActionTakeoff = 1;
    public static final byte ActionLand = 2;
    public static final byte ActionPreen = 3;
    public static final byte ActionSquawk = 4;
    public static final byte ActionEat = 5;
    public static final int TakeoffTicks = 10;
    public static final int LandTicks = 12;
    public static final int PreenTicks = 40;
    public static final int SquawkTicks = 20;
    public static final int EatTicks = 20;
    public static final int LaunchTick = 3;
    public static final double LaunchLift = 0.25;
    public static final int PanicTicks = 60;
    public static final int StartleTicks = 30;
    public static final double StartleRadius = 8.0;
    public static final int RestMin = 200;
    public static final int RestMax = 520;
    public static final int IdleActionChance = 260;
    public static final int AirIdleLimit = 100;
    public static final double FallDamping = 0.6;
    public static final double WalkReference = 0.09;
    public static final double HoverBelow = 0.05;
    public static final double GlideAbove = 0.16;
    public static final int GlideTicks = 50;
    public static final int FlapTicks = 30;
    public static final int FlapPeriod = 10;
    public static final int HoverFlapPeriod = 8;
    public static final double FlapHearing = 16.0;
    public static final float FlapVolume = 0.35f;
    public static final int AmbientInterval = 220;
    public static final float FlinchSeconds = 0.35f;
    public static final float FlinchPitch = -14.0f;
    public static final float FlinchRoll = 10.0f;
    public static final byte FlightFlap = 0;
    public static final byte FlightGlide = 1;
    public static final byte FlightHover = 2;
    public static final String WalkClip = "walk";
    public static final double WalkStartShare = 0.25;
    public static final double WalkStopShare = 0.15;
    public static final double MinWalkRate = 0.6;
    public static final double MaxWalkRate = 2.0;
    public static final int WalkStartTicks = 2;
    public static final int WalkStopTicks = 4;
    public static final int ModeTicks = 4;
    public static final int BodyBlend = 4;
    private static final EntityDataAccessor<Integer> VariantData = SynchedEntityData.defineId(Scp346Pterosaur.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FlyingData = SynchedEntityData.defineId(Scp346Pterosaur.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RoostingData = SynchedEntityData.defineId(Scp346Pterosaur.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp346Pterosaur.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp346Pterosaur.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FlyLoop = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation GlideLoop = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation HoverLoop = RawAnimation.begin().thenLoop("hover");
    private static final RawAnimation SleepLoop = RawAnimation.begin().thenLoop("sleep");
    private static final RawAnimation SitLoop = RawAnimation.begin().thenLoop("sit");
    private static final RawAnimation HurtPlay = RawAnimation.begin().thenPlayAndHold("hurt");
    private static final Map<Byte, RawAnimation> ActionPlays = Map.of(
            ActionTakeoff, RawAnimation.begin().thenPlayAndHold("takeoff"),
            ActionLand, RawAnimation.begin().thenPlayAndHold("land"),
            ActionPreen, RawAnimation.begin().thenPlayAndHold("preen"),
            ActionSquawk, RawAnimation.begin().thenPlayAndHold("squawk"),
            ActionEat, RawAnimation.begin().thenPlayAndHold("eat"));
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ActionClock Clock = new ActionClock();
    private final PaceMeter GroundMeter = new PaceMeter();
    private final PaceMeter ClimbMeter = new PaceMeter();
    private final Settle<Byte> ModeSettle = new Settle<>();
    private final Settle<Boolean> WalkSettle = new Settle<>();
    private final PathNavigation GroundNav;
    private final FlyingPathNavigation AirNav;
    private final MoveControl GroundControl;
    private final FlyingMoveControl AirControl;
    @Nullable
    private AnimationController<Scp346Pterosaur> BodyController;
    private int ActionTicks;
    private int GroundTicks;
    private int RestTarget = RestMin;
    private int AirIdle;
    private int Panic;
    private Vec3 PanicFrom = Vec3.ZERO;
    private boolean CallAfterAction;
    @Nullable
    private BlockPos Home;
    @Nullable
    private Vec3 Arrival;
    private int ArrivalTicks;
    private boolean Steered;
    private double SmoothSpeed;
    private byte FlightMode = FlightFlap;
    private int GlideClock;
    private int FlapClock;
    private boolean Walking;
    private double WalkTicks;
    private double WalkTicksO;
    private int WalkHold;

    public Scp346Pterosaur(EntityType<? extends Scp346Pterosaur> Type, Level World) {
        super(Type, World);
        GroundNav = navigation;
        GroundControl = moveControl;
        AirNav = new FlyingPathNavigation(this, World);
        AirNav.setCanOpenDoors(false);
        AirNav.setCanFloat(true);
        AirNav.setCanPassDoors(true);
        AirControl = new FlyingMoveControl(this, 20, true);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0f);
        setPathfindingMalus(PathType.COCOA, -1.0f);
        RestTarget = RestMin + random.nextInt(RestMax - RestMin);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, WalkSpeed).add(Attributes.FLYING_SPEED, FlySpeed)
                .add(Attributes.FOLLOW_RANGE, FollowRange);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        GroundPathNavigation Ground = new GroundPathNavigation(this, World);
        Ground.setCanFloat(true);
        return Ground;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(VariantData, 0);
        Builder.define(FlyingData, false);
        Builder.define(RoostingData, false);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp346Goals.Escape(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new Scp346Goals.Arrive(this));
        goalSelector.addGoal(4, new Scp346Goals.FollowOwner(this));
        goalSelector.addGoal(5, new Scp346Goals.Roost(this));
        goalSelector.addGoal(6, new Scp346Goals.Tempt(this));
        goalSelector.addGoal(7, new Scp346Goals.Wander(this));
        goalSelector.addGoal(8, new Scp346Goals.Waddle(this));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    public Scp346Variant getVariant() {
        return Scp346Variant.byIndex(entityData.get(VariantData));
    }

    public void setVariant(Scp346Variant Variant) {
        entityData.set(VariantData, Variant.ordinal());
    }

    public float voicePitch() {
        return getVariant().getVoice() * (0.94f + random.nextFloat() * 0.12f);
    }

    @Override
    public boolean isFlying() {
        return entityData.get(FlyingData);
    }

    public boolean isRoosting() {
        return entityData.get(RoostingData);
    }

    public void setRoosting(boolean Value) {
        if (Value && isFlying()) {
            return;
        }
        entityData.set(RoostingData, Value);
        if (Value) {
            getNavigation().stop();
        }
    }

    public void wake() {
        if (isRoosting()) {
            entityData.set(RoostingData, false);
        }
    }

    public void setFlying(boolean Value) {
        if (level().isClientSide) {
            return;
        }
        if (isFlying() != Value) {
            entityData.set(FlyingData, Value);
        }
        applyFlight(Value);
    }

    private void applyFlight(boolean Value) {
        PathNavigation Wanted = Value ? AirNav : GroundNav;
        if (navigation != Wanted) {
            navigation.stop();
            navigation = Wanted;
            moveControl = Value ? AirControl : GroundControl;
        }
        setNoGravity(Value);
        if (Value) {
            wake();
            AirIdle = 0;
        }
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    @Nullable
    public String actionClip() {
        if (level().isClientSide) {
            return Clock.clip(tickCount);
        }
        return clipOf(action());
    }

    public byte shownAction() {
        return level().isClientSide ? Clock.kind(tickCount) : action();
    }

    @Override
    public ActionClock actionClock() {
        return Clock;
    }

    private void syncClock() {
        byte Current = action();
        if (Current == ActionNone) {
            Clock.finish();
            return;
        }
        Clock.begin(Current, clipOf(Current), tickCount, 0.0);
    }

    public double walkTicks(float Partial) {
        return WalkTicksO + (WalkTicks - WalkTicksO) * Partial;
    }

    @Nullable
    private static String clipOf(byte Kind) {
        return switch (Kind) {
            case ActionTakeoff -> "takeoff";
            case ActionLand -> "land";
            case ActionPreen -> "preen";
            case ActionSquawk -> "squawk";
            case ActionEat -> "eat";
            default -> null;
        };
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionData, Kind);
        entityData.set(ActionStartData, tickCount);
        ActionTicks = Ticks;
    }

    public int actionElapsed() {
        return Math.max(0, tickCount - entityData.get(ActionStartData));
    }

    public boolean takingOff() {
        return action() == ActionTakeoff;
    }

    public void requestFlight() {
        if (isFlying() || takingOff()) {
            return;
        }
        wake();
        if (isOrderedToSit()) {
            return;
        }
        if (!onGround() && !isInWater()) {
            setFlying(true);
            return;
        }
        getNavigation().stop();
        setAction(ActionTakeoff, TakeoffTicks);
    }

    public void land() {
        if (!isFlying()) {
            return;
        }
        setFlying(false);
        if (!isActing() || takingOff()) {
            setAction(ActionLand, LandTicks);
        }
        GroundTicks = 0;
        RestTarget = (isTame() ? RestMin + 100 : RestMin) + random.nextInt(RestMax - RestMin);
    }

    public void steer(Vec3 Target, double Speed) {
        Steered = true;
        AirIdle = 0;
        if (!isFlying()) {
            return;
        }
        if (navigation.isDone() || tickCount % 10 == 0) {
            if (!navigation.moveTo(Target.x, Target.y, Target.z, Speed)) {
                getMoveControl().setWantedPosition(Target.x, Target.y, Target.z, Speed);
            }
        }
        if (navigation.isDone()) {
            getMoveControl().setWantedPosition(Target.x, Target.y, Target.z, Speed);
        }
    }

    public boolean groundBelow(double Depth) {
        return !level().noCollision(this, getBoundingBox().move(0.0, -Depth, 0.0).expandTowards(0.0, Depth, 0.0).deflate(0.05, 0.0, 0.05))
                || level().containsAnyLiquid(getBoundingBox().move(0.0, -Depth, 0.0));
    }

    public int groundTicks() {
        return GroundTicks;
    }

    public boolean restless() {
        return GroundTicks > RestTarget;
    }

    public int panic() {
        return Panic;
    }

    public Vec3 panicFrom() {
        return PanicFrom;
    }

    public void startle(Vec3 From, int Ticks) {
        Panic = Math.max(Panic, Ticks);
        PanicFrom = From;
        wake();
        if (!level().isClientSide && !isOrderedToSit()) {
            requestFlight();
        }
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public void setHome(@Nullable BlockPos Pos) {
        Home = Pos == null ? null : Pos.immutable();
    }

    @Nullable
    public Vec3 arrival() {
        return Arrival;
    }

    public void setArrival(@Nullable Vec3 Target) {
        Arrival = Target;
        ArrivalTicks = 0;
    }

    public int arrivalTicks() {
        return ArrivalTicks;
    }

    public void beginPreen() {
        if (!isActing() && !isFlying() && onGround()) {
            getNavigation().stop();
            setAction(ActionPreen, PreenTicks);
        }
    }

    public void beginSquawk() {
        if (isFlying()) {
            playLayers(Scp346Registry.CallLayers, 0.8f, voicePitch());
            return;
        }
        if (isActing() && action() != ActionSquawk) {
            CallAfterAction = true;
            return;
        }
        getNavigation().stop();
        setAction(ActionSquawk, SquawkTicks);
        playLayers(Scp346Registry.CallLayers, 0.8f, voicePitch());
    }

    public void beginEat() {
        playLayers(Scp346Registry.EatLayers, 0.7f, voicePitch());
        if (!isFlying() && onGround()) {
            getNavigation().stop();
            setAction(ActionEat, EatTicks);
        }
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (isSilent()) {
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY() + 0.4, getZ(), Layer.get(), getSoundSource(), Volume, Pitch);
        }
    }

    public static boolean isFish(ItemStack Stack) {
        return Stack.is(Items.COD) || Stack.is(Items.SALMON);
    }

    private static boolean blocksSitToggle(ItemStack Stack) {
        return Stack.getItem() instanceof StasisPodItem || Stack.is(Items.LEAD) || Stack.is(Items.NAME_TAG);
    }

    @Override
    public InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        boolean Client = level().isClientSide;
        if (isTame() && isOwnedBy(User) && User.isShiftKeyDown() && !isFish(Stack) && !blocksSitToggle(Stack)) {
            if (!Client) {
                toggleSit();
            }
            return InteractionResult.sidedSuccess(Client);
        }
        if (isFish(Stack)) {
            if (isTame() && getHealth() >= getMaxHealth()) {
                if (!Client) {
                    beginSquawk();
                }
                return InteractionResult.sidedSuccess(Client);
            }
            if (!Client) {
                feed(User, Stack);
            }
            return InteractionResult.sidedSuccess(Client);
        }
        if (isTame() && isOwnedBy(User) && Stack.isEmpty() && Hand == InteractionHand.MAIN_HAND && !User.isShiftKeyDown()) {
            if (!Client) {
                beginSquawk();
            }
            return InteractionResult.sidedSuccess(Client);
        }
        return super.mobInteract(User, Hand);
    }

    public void toggleSit() {
        boolean Sit = !isOrderedToSit();
        setOrderedToSit(Sit);
        setInSittingPose(Sit);
        getNavigation().stop();
        if (Sit && isFlying()) {
            land();
        }
        if (!Sit) {
            wake();
        }
    }

    public boolean feed(Player User, ItemStack Stack) {
        Stack.consume(1, User);
        wake();
        beginEat();
        CallAfterAction = true;
        if (isTame()) {
            heal(FeedHeal);
            return false;
        }
        if (random.nextInt(TameChance) == 0 && !EventHooks.onAnimalTame(this, User)) {
            tame(User);
            setHome(null);
            setArrival(null);
            setOrderedToSit(false);
            level().broadcastEntityEvent(this, (byte) 7);
            return true;
        }
        level().broadcastEntityEvent(this, (byte) 6);
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (!isAlive()) {
            return;
        }
        Steered = false;
        if (Panic > 0) {
            Panic--;
        }
        if (Arrival != null) {
            ArrivalTicks++;
        }
        if (ActionTicks > 0) {
            actionTick(action(), actionElapsed());
            ActionTicks--;
            if (ActionTicks <= 0) {
                byte Ended = action();
                entityData.set(ActionData, ActionNone);
                if (Ended == ActionTakeoff && !isFlying()) {
                    setFlying(true);
                }
                if (CallAfterAction && Ended != ActionSquawk) {
                    CallAfterAction = false;
                    beginSquawk();
                } else if (Ended == ActionSquawk) {
                    CallAfterAction = false;
                }
            }
        }
        if (isFlying()) {
            GroundTicks = 0;
            if (isOrderedToSit() && !isPassenger()) {
                land();
            }
        } else {
            if (onGround()) {
                GroundTicks++;
            }
            if (isInWater() && !isPassenger() && !isOrderedToSit()) {
                requestFlight();
            }
            idleBehaviour();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!isFlying()) {
            return;
        }
        if (Steered || takingOff() || Panic > 0) {
            AirIdle = 0;
            return;
        }
        AirIdle++;
        if (AirIdle > AirIdleLimit && groundBelow(6.0)) {
            land();
        }
    }

    private void idleBehaviour() {
        if (isActing() || isRoosting() || !onGround() || !getNavigation().isDone() || isInSittingPose()) {
            return;
        }
        if (random.nextInt(IdleActionChance) != 0) {
            return;
        }
        if (random.nextFloat() < 0.6f) {
            beginPreen();
        } else if (!Scp346Flock.mates(this, 6.0).isEmpty() || isTame()) {
            beginSquawk();
        }
    }

    private void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionTakeoff) {
            if (Elapsed < LaunchTick) {
                setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
            } else if (Elapsed == LaunchTick) {
                Vec3 Motion = getDeltaMovement();
                setDeltaMovement(Motion.x, LaunchLift, Motion.z);
                hasImpulse = true;
                setFlying(true);
                playSound(Scp346Registry.Flap.get(), FlapVolume * 1.4f, 1.1f + random.nextFloat() * 0.1f);
            }
        } else if (Kind == ActionPreen || Kind == ActionEat || Kind == ActionSquawk) {
            if (isFlying()) {
                ActionTicks = 1;
            }
        }
        if (Kind == ActionEat && Elapsed == EatTicks / 2) {
            playSound(Scp346Registry.Clack.get(), 0.5f, voicePitch());
        }
        if (Kind == ActionPreen && Elapsed % 8 == 4) {
            playSound(Scp346Registry.Clack.get(), 0.25f, voicePitch() * 1.1f);
        }
    }

    private void clientTick() {
        Clock.settle();
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        SmoothSpeed += (GroundMeter.push(Math.sqrt(Dx * Dx + Dz * Dz)) - SmoothSpeed) * 0.35;
        double Climb = ClimbMeter.push(getY() - yo);
        walkTick();
        if (!isFlying()) {
            FlightMode = FlightFlap;
            GlideClock = 0;
            ModeSettle.clear();
            return;
        }
        byte Wanted;
        if (SmoothSpeed < HoverBelow) {
            Wanted = FlightHover;
            GlideClock = 0;
        } else if (SmoothSpeed > GlideAbove && Climb < 0.03) {
            GlideClock++;
            Wanted = GlideClock % (GlideTicks + FlapTicks) < GlideTicks ? FlightGlide : FlightFlap;
        } else {
            Wanted = FlightFlap;
            GlideClock = Math.max(0, GlideClock - 2);
        }
        if (ModeSettle.ready(FlightMode, Wanted, ModeTicks)) {
            FlightMode = Wanted;
            ModeSettle.clear();
        }
        if (FlightMode == FlightGlide && !takingOff()) {
            FlapClock = 0;
            return;
        }
        FlapClock++;
        int Period = FlightMode == FlightHover ? HoverFlapPeriod : FlapPeriod;
        if (FlapClock >= Period) {
            FlapClock = 0;
            if (!isSilent() && level().hasNearbyAlivePlayer(getX(), getY(), getZ(), FlapHearing)) {
                level().playLocalSound(getX(), getY(), getZ(), Scp346Registry.Flap.get(), getSoundSource(), FlapVolume * (0.85f + random.nextFloat() * 0.15f),
                        getVariant().getVoice() * (0.95f + random.nextFloat() * 0.1f), false);
            }
        }
    }

    private void walkTick() {
        boolean Wanted = !isFlying() && (Walking ? SmoothSpeed >= WalkReference * WalkStopShare : SmoothSpeed > WalkReference * WalkStartShare);
        if (WalkSettle.ready(Walking, Wanted, Wanted ? WalkStartTicks : WalkStopTicks)) {
            Walking = Wanted;
            WalkSettle.clear();
            if (Walking) {
                WalkTicks = 0.0;
                WalkTicksO = 0.0;
                WalkHold = BodyBlend;
            }
        }
        WalkTicksO = WalkTicks;
        if (!Walking) {
            return;
        }
        if (WalkHold > 0) {
            WalkHold--;
            return;
        }
        WalkTicks += Mth.clamp(SmoothSpeed / WalkReference, MinWalkRate, MaxWalkRate);
        if (WalkTicks > 4096.0) {
            WalkTicks -= 4096.0;
            WalkTicksO -= 4096.0;
        }
    }

    public double groundSpeed() {
        return SmoothSpeed;
    }

    public byte flightMode() {
        return FlightMode;
    }

    public static float flinch(float Ticks) {
        float Seconds = Ticks / 20.0f;
        if (Seconds < 0.0f || Seconds > FlinchSeconds) {
            return 0.0f;
        }
        return Mth.sin((float) Math.PI * Seconds / FlinchSeconds);
    }

    public float flinchAmount(float Partial) {
        if (hurtTime <= 0 || hurtDuration <= 0) {
            return 0.0f;
        }
        return flinch(hurtDuration - hurtTime + Partial);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        Vec3 Motion = getDeltaMovement();
        if (!isFlying() && !onGround() && Motion.y < 0.0) {
            setDeltaMovement(Motion.multiply(1.0, FallDamping, 1.0));
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (isInvulnerableTo(Source)) {
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide && isAlive()) {
            Entity Attacker = Source.getEntity();
            Vec3 From = Attacker != null ? Attacker.position() : Source.getSourcePosition() != null ? Source.getSourcePosition() : position();
            setOrderedToSit(false);
            setInSittingPose(false);
            startle(From, PanicTicks);
            for (Scp346Pterosaur Mate : Scp346Flock.mates(this, StartleRadius)) {
                Mate.startle(From, StartleTicks);
            }
        }
        return Hurt;
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double Y, boolean OnGround, BlockState State, BlockPos Pos) {
    }

    @Override
    protected boolean canFlyToOwner() {
        return true;
    }

    @Override
    public boolean isFood(ItemStack Stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal Other) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel World, AgeableMob Partner) {
        return null;
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setVariant(Scp346Variant.random(World.getRandom()));
        return super.finalizeSpawn(World, Difficulty, Reason, Group == null ? new AgeableMob.AgeableMobGroupData(false) : Group);
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (isSilent()) {
            return;
        }
        if (isRoosting()) {
            playSound(Scp346Registry.Coo.get(), 0.45f, voicePitch());
            return;
        }
        playLayers(Scp346Registry.ChirrLayers, 0.6f, voicePitch());
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isRoosting() ? Scp346Registry.Coo.get() : Scp346Registry.Chirr.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp346Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp346Registry.Death.get();
    }

    @Override
    public float getVoicePitch() {
        return voicePitch();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
        playSound(Scp346Registry.Step.get(), 0.2f, voicePitch());
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.0);
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putString("Variant", getVariant().getSerializedName());
        Tag.putBoolean("Flying", isFlying());
        Tag.putBoolean("Roosting", isRoosting());
        if (Home != null) {
            Tag.putInt("HomeX", Home.getX());
            Tag.putInt("HomeY", Home.getY());
            Tag.putInt("HomeZ", Home.getZ());
        }
        if (Arrival != null) {
            Tag.putDouble("ArrivalX", Arrival.x);
            Tag.putDouble("ArrivalY", Arrival.y);
            Tag.putDouble("ArrivalZ", Arrival.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setVariant(Scp346Variant.byName(Tag.getString("Variant")));
        setFlying(Tag.getBoolean("Flying"));
        entityData.set(RoostingData, Tag.getBoolean("Roosting") && !isFlying());
        Home = Tag.contains("HomeX") ? new BlockPos(Tag.getInt("HomeX"), Tag.getInt("HomeY"), Tag.getInt("HomeZ")) : null;
        Arrival = Tag.contains("ArrivalX") ? new Vec3(Tag.getDouble("ArrivalX"), Tag.getDouble("ArrivalY"), Tag.getDouble("ArrivalZ")) : null;
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (!level().isClientSide || !Key.equals(ActionData) && !Key.equals(ActionStartData)) {
            return;
        }
        syncClock();
        if (Key.equals(ActionStartData) && BodyController != null && isActing()) {
            BodyController.forceAnimationReset();
        }
    }

    public RawAnimation baseAnimation() {
        if (isRoosting()) {
            return SleepLoop;
        }
        if (isFlying()) {
            return switch (FlightMode) {
                case FlightGlide -> GlideLoop;
                case FlightHover -> HoverLoop;
                default -> FlyLoop;
            };
        }
        if (isInSittingPose()) {
            return SitLoop;
        }
        if (Walking) {
            return WalkLoop;
        }
        return IdleLoop;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        AnimationController<Scp346Pterosaur> Body = new Scp346Controller(this, "body", BodyBlend, Event -> {
            RawAnimation Action = ActionPlays.get(shownAction());
            if (Action != null) {
                return Event.setAndContinue(Action);
            }
            return Event.setAndContinue(baseAnimation());
        });
        BodyController = Body;
        Controllers.add(Body);
        Controllers.add(new AnimationController<>(this, "flinch", 0, Event -> {
            if (hurtTime > 0 && isAlive()) {
                return Event.setAndContinue(HurtPlay);
            }
            Event.getController().forceAnimationReset();
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
