package net.rai.scp_unleashed.scp492;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.motion.ActionClock;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp492Figure extends TamableAnimal implements GeoEntity, IEntityWithComplexSpawn, ActionClock.Owner, ControlRefusing {
    public static final float Width = 0.45f;
    public static final float Height = 0.7f;
    public static final float EyeHeight = 0.42f;
    public static final double MaxHealth = 12.0;
    public static final double Speed = 0.2;
    public static final double FollowRange = 24.0;
    public static final double WanderModifier = 0.6;
    public static final double ApproachModifier = 0.75;
    public static final double FollowModifier = 1.0;
    public static final double FleeModifier = 1.15;
    public static final float FollowStart = 6.0f;
    public static final float FollowStop = 2.5f;
    public static final float FireScale = 2.0f;
    public static final float TornHealth = 1.0f;
    public static final float SewHeal = 4.0f;
    public static final float RiseHealth = 6.0f;
    public static final int FearMemory = 40;
    public static final int ScanInterval = 10;
    public static final double LookoutRange = 14.0;
    public static final double LookoutOwnerRange = 10.0;
    public static final int LookoutCooldown = 160;
    public static final int GlowTicks = 100;
    public static final double GreetRange = 4.0;
    public static final int GreetMemory = 2400;
    public static final int GreetCooldown = 200;
    public static final int GrudgeTicks = 1200;
    public static final int AmbientInterval = 320;
    public static final float StepVolume = 0.18f;
    public static final float VoicePitch = 1.0f;
    public static final byte ActionNone = 0;
    public static final byte ActionPoint = 1;
    public static final byte ActionTip = 2;
    public static final byte ActionFlop = 3;
    public static final byte ActionRise = 4;
    public static final byte ActionSitDown = 5;
    public static final byte ActionStandUp = 6;
    public static final byte ActionSlump = 7;
    public static final int PointTicks = 36;
    public static final int TipTicks = 32;
    public static final int FlopTicks = 14;
    public static final int RiseTicks = 22;
    public static final int SitDownTicks = 12;
    public static final int StandUpTicks = 12;
    public static final int SlumpTicks = 10;
    public static final double WalkDistance = 0.275;
    public static final double RunDistance = 0.5;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.006, 0.05, 0.04, WalkDistance, RunDistance, 0.25, 2);
    public static final double[] Contacts = {0.0, 0.5};
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp492Figure.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp492Figure.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TornData = SynchedEntityData.defineId(Scp492Figure.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AfraidData = SynchedEntityData.defineId(Scp492Figure.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation PanicLoop = RawAnimation.begin().thenLoop("panic");
    private static final RawAnimation CowerLoop = RawAnimation.begin().thenLoop("cower");
    private static final RawAnimation SitLoop = RawAnimation.begin().thenLoop("sit");
    private static final RawAnimation HeapLoop = RawAnimation.begin().thenLoop("heap");
    private static final RawAnimation PointPlay = RawAnimation.begin().thenPlayAndHold("point");
    private static final RawAnimation TipPlay = RawAnimation.begin().thenPlayAndHold("tip");
    private static final RawAnimation FlopPlay = RawAnimation.begin().thenPlayAndHold("flop");
    private static final RawAnimation RisePlay = RawAnimation.begin().thenPlayAndHold("rise");
    private static final RawAnimation SitDownPlay = RawAnimation.begin().thenPlayAndHold("sit_down");
    private static final RawAnimation StandUpPlay = RawAnimation.begin().thenPlayAndHold("stand_up");
    private static final RawAnimation SlumpPlay = RawAnimation.begin().thenPlayAndHold("slump");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ActionClock Clock = new ActionClock();
    private final StrideGait Stride = new StrideGait();
    private final Map<UUID, Integer> Greeted = new HashMap<>();
    @Nullable
    private AnimationController<Scp492Figure> BodyController;
    private int ActionTicks;
    private int TickOrigin;
    private float RenderPartial;
    private int FearLeft;
    @Nullable
    private Vec3 FearFrom;
    private int LookoutLeft;
    private int GreetLeft;
    @Nullable
    private Mob Spotted;
    @Nullable
    private UUID HurtBy;
    private int HurtAt = -100000;
    @Nullable
    private UUID Sought;
    private boolean Blunt;
    private boolean TearPending;
    private boolean Panicky;

    public Scp492Figure(EntityType<? extends Scp492Figure> Type, Level World) {
        super(Type, World);
        setPathfindingMalus(PathType.DANGER_FIRE, -1.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0f);
        setPathfindingMalus(PathType.LAVA, -1.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.FOLLOW_RANGE, FollowRange);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        GroundPathNavigation Ground = new GroundPathNavigation(this, World);
        Ground.setCanFloat(true);
        Ground.setCanOpenDoors(false);
        return Ground;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(TornData, false);
        Builder.define(AfraidData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp492Goals.Flee(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(4, new FollowOwnerGoal(this, FollowModifier, FollowStart, FollowStop));
        goalSelector.addGoal(5, new Scp492Goals.Curious(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        return Result;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.scp492.refuses");
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isTorn() {
        return entityData.get(TornData);
    }

    public boolean isAfraid() {
        return entityData.get(AfraidData);
    }

    public boolean free() {
        return isAlive() && !isTorn() && !isActing();
    }

    @Nullable
    public Vec3 fearSource() {
        return FearLeft > 0 ? FearFrom : null;
    }

    @Nullable
    public UUID sought() {
        return Sought;
    }

    public void seek(@Nullable UUID Who) {
        Sought = Who;
    }

    @Nullable
    public Mob spotted() {
        return Spotted;
    }

    public int greetedCount() {
        return Greeted.size();
    }

    @Override
    public void setInSittingPose(boolean Value) {
        boolean Was = isInSittingPose();
        super.setInSittingPose(Value);
        if (!level().isClientSide && Was != Value && isAlive() && !isTorn()) {
            setAction(Value ? ActionSitDown : ActionStandUp, Value ? SitDownTicks : StandUpTicks);
        }
    }

    public void setTorn(boolean Value) {
        entityData.set(TornData, Value);
        if (Value) {
            getNavigation().stop();
            setOrderedToSit(false);
            setInSittingPose(false);
        }
        updateControlFlags();
    }

    @Nullable
    public String actionClip() {
        if (level().isClientSide) {
            return Clock.clip(clockNow());
        }
        return clipOf(action());
    }

    @Nullable
    private static String clipOf(byte Kind) {
        return switch (Kind) {
            case ActionPoint -> "point";
            case ActionTip -> "tip";
            case ActionFlop -> "flop";
            case ActionRise -> "rise";
            case ActionSitDown -> "sit_down";
            case ActionStandUp -> "stand_up";
            case ActionSlump -> "slump";
            default -> null;
        };
    }

    @Override
    public ActionClock actionClock() {
        return Clock;
    }

    private double clockNow() {
        return tickCount + RenderPartial;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    private void syncClock() {
        byte Current = action();
        if (Current == ActionNone) {
            Clock.finish();
            return;
        }
        Clock.begin(Current, clipOf(Current), tickCount, ActionClock.already(Clock.fresh(), tickCount, TickOrigin, entityData.get(ActionStartData)));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (level().isClientSide && (Key.equals(ActionData) || Key.equals(ActionStartData))) {
            syncClock();
            if (Key.equals(ActionStartData) && BodyController != null && isActing()) {
                BodyController.forceAnimationReset();
            }
        }
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionNone) {
            getNavigation().stop();
        }
        updateControlFlags();
    }

    public double actionElapsed() {
        if (!level().isClientSide) {
            return Math.max(0, tickCount - entityData.get(ActionStartData));
        }
        return Clock.elapsed(clockNow());
    }

    public StrideGait stride() {
        return Stride;
    }

    public boolean isStriding() {
        return Stride.stride() != StrideGait.Idle;
    }

    public boolean isRunning() {
        return Stride.stride() == StrideGait.Run;
    }

    @Override
    protected void updateControlFlags() {
        super.updateControlFlags();
        if (isTorn() || isActing() && action() != ActionSitDown) {
            goalSelector.setControlFlag(Goal.Flag.MOVE, false);
            goalSelector.setControlFlag(Goal.Flag.JUMP, false);
            goalSelector.setControlFlag(Goal.Flag.LOOK, false);
        }
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
        if (ActionTicks > 0) {
            ActionTicks--;
            actionTick();
            if (ActionTicks <= 0) {
                entityData.set(ActionData, ActionNone);
                Spotted = null;
                updateControlFlags();
            }
        }
        if (isTorn()) {
            Vec3 Motion = getDeltaMovement();
            setDeltaMovement(0.0, Motion.y, 0.0);
        }
    }

    private void clientTick() {
        Clock.settle();
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        int Before = Stride.stride();
        boolean Moving = Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive() && !isTorn() && !isInSittingPose() && (onGround() || isInWater()), Gait);
        if (Stride.stride() != Before || Stride.stride() != StrideGait.Run) {
            Panicky = isAfraid();
        }
        if (!Moving || isSilent()) {
            return;
        }
        for (double Contact : Contacts) {
            if (Math.floor(Stride.to() - Contact) > Math.floor(Stride.from() - Contact) && level().hasNearbyAlivePlayer(getX(), getY(), getZ(), 12.0)) {
                level().playLocalSound(getX(), getY(), getZ(), Scp492Registry.Step.get(), getSoundSource(), StepVolume * (0.85f + random.nextFloat() * 0.15f),
                        (isRunning() ? 1.12f : 1.0f) * (0.94f + random.nextFloat() * 0.12f), false);
            }
        }
    }

    private void actionTick() {
        if (action() == ActionPoint && Spotted != null && Spotted.isAlive()) {
            getLookControl().setLookAt(Spotted, 30.0f, 30.0f);
            face(Spotted.position());
        }
    }

    public void face(Vec3 Target) {
        double Dx = Target.x - getX();
        double Dz = Target.z - getZ();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        setYBodyRot(Yaw);
        setYHeadRot(Yaw);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        LookoutLeft = Math.max(0, LookoutLeft - 1);
        GreetLeft = Math.max(0, GreetLeft - 1);
        if (FearLeft > 0) {
            FearLeft--;
        }
        if (tickCount % ScanInterval == 0) {
            Vec3 Source = Scp492Fear.threat(this);
            if (Source != null) {
                frighten(World, Source);
            }
        }
        boolean Afraid = (FearLeft > 0 || isOnFire()) && !isTorn();
        if (Afraid != isAfraid()) {
            entityData.set(AfraidData, Afraid);
        }
        if (isTorn()) {
            return;
        }
        if (tickCount % 20 == 0) {
            lookout(World);
        }
        if (tickCount % 20 == 10) {
            greet(World);
        }
    }

    public void frighten(ServerLevel World, Vec3 Source) {
        boolean Fresh = FearLeft <= 0 && !isOnFire();
        FearLeft = FearMemory;
        FearFrom = Source;
        if (isTorn()) {
            return;
        }
        if (action() == ActionPoint || action() == ActionTip) {
            setAction(ActionNone, 0);
            Spotted = null;
        }
        if (isOrderedToSit()) {
            setInSittingPose(false);
        }
        if (Fresh) {
            Scp492Effects.layers(World, this, Scp492Registry.FearLayers, 0.9f, VoicePitch);
            Scp492Effects.fright(World, this);
        }
    }

    private void lookout(ServerLevel World) {
        if (!isTame() || !free() || isAfraid() || LookoutLeft > 0) {
            return;
        }
        LivingEntity Owner = getOwner();
        if (Owner == null || Owner.level() != level() || Owner.distanceTo(this) > LookoutRange * 1.5) {
            return;
        }
        Mob Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Mob Other : World.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(LookoutRange),
                Candidate -> Candidate instanceof Enemy && Candidate.isAlive() && !Candidate.isInvisible() && !Candidate.hasEffect(MobEffects.GLOWING))) {
            if (Other.distanceTo(Owner) > LookoutOwnerRange && Other.getTarget() != Owner) {
                continue;
            }
            double Distance = Other.distanceToSqr(this);
            if (Distance < BestDistance && hasLineOfSight(Other)) {
                Best = Other;
                BestDistance = Distance;
            }
        }
        if (Best == null) {
            return;
        }
        point(World, Best);
    }

    public void point(ServerLevel World, Mob Target) {
        if (!isInSittingPose()) {
            Spotted = Target;
            face(Target.position());
            setAction(ActionPoint, PointTicks);
        }
        Target.addEffect(new MobEffectInstance(MobEffects.GLOWING, GlowTicks, 0, false, false));
        Scp492Effects.layers(World, this, Scp492Registry.AlertLayers, 1.0f, VoicePitch);
        Scp492Effects.spotted(World, Target);
        LookoutLeft = LookoutCooldown;
    }

    private void greet(ServerLevel World) {
        if (!free() || isAfraid() || isInSittingPose() || GreetLeft > 0) {
            return;
        }
        Player Nearest = World.getNearestPlayer(getX(), getY(), getZ(), GreetRange, Candidate -> !Candidate.isSpectator() && Candidate.isAlive());
        if (Nearest == null) {
            return;
        }
        Integer Last = Greeted.get(Nearest.getUUID());
        if (Last != null && tickCount - Last < GreetMemory) {
            return;
        }
        tipTo(World, Nearest);
    }

    public void tipTo(ServerLevel World, Player Target) {
        Greeted.put(Target.getUUID(), tickCount);
        if (!isInSittingPose()) {
            face(Target.position());
            setAction(ActionTip, TipTicks);
        }
        Scp492Effects.layers(World, this, Scp492Registry.AhoyLayers, 0.9f, VoicePitch);
        GreetLeft = GreetCooldown;
    }

    public boolean holdsGrudge(Player User) {
        return User.getUUID().equals(HurtBy) && tickCount - HurtAt < GrudgeTicks;
    }

    @Override
    public InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (Held.is(Items.STRING)) {
            if (isTorn() || getHealth() < getMaxHealth()) {
                if (level() instanceof ServerLevel World) {
                    sew(World, User, Held);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return InteractionResult.PASS;
        }
        if (isTorn()) {
            return InteractionResult.PASS;
        }
        if (!Held.isEmpty()) {
            return super.mobInteract(User, Hand);
        }
        if (Hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(level() instanceof ServerLevel World)) {
            return InteractionResult.SUCCESS;
        }
        if (!isTame()) {
            persuade(World, User);
            return InteractionResult.SUCCESS;
        }
        if (isOwnedBy(User) && User.isSecondaryUseActive()) {
            setOrderedToSit(!isOrderedToSit());
            setInSittingPose(isOrderedToSit());
            getNavigation().stop();
            setTarget(null);
            Scp492Effects.layers(World, this, Scp492Registry.RustleLayers, 0.5f, 1.2f);
            return InteractionResult.SUCCESS;
        }
        if (free() && !isAfraid()) {
            tipTo(World, User);
        }
        return InteractionResult.SUCCESS;
    }

    public boolean persuade(ServerLevel World, Player User) {
        if (isTame() || isTorn() || isAfraid() || isOnFire()) {
            return false;
        }
        if (Scp492Fear.frightening(User)) {
            frighten(World, User.position());
            return false;
        }
        if (holdsGrudge(User)) {
            Scp492Effects.layers(World, this, List.of(Scp492Registry.Refuse), 0.9f, VoicePitch);
            face(User.position());
            return false;
        }
        tame(User);
        setOrderedToSit(false);
        getNavigation().stop();
        World.broadcastEntityEvent(this, (byte) 7);
        tipTo(World, User);
        Sought = null;
        return true;
    }

    public boolean sew(ServerLevel World, Player User, ItemStack Spool) {
        if (!isTorn() && getHealth() >= getMaxHealth()) {
            return false;
        }
        Spool.consume(1, User);
        heal(SewHeal);
        Scp492Effects.layers(World, this, Scp492Registry.SewLayers, 0.8f, 1.0f);
        Scp492Effects.threads(World, this);
        if (isTorn() && getHealth() >= RiseHealth) {
            rise(World);
        }
        return true;
    }

    public void rise(ServerLevel World) {
        setTorn(false);
        setAction(ActionRise, RiseTicks);
        Scp492Effects.layers(World, this, List.of(Scp492Registry.Rise), 0.8f, VoicePitch);
    }

    public void tear(ServerLevel World) {
        boolean Seated = isInSittingPose();
        setTorn(true);
        setAction(Seated ? ActionSlump : ActionFlop, Seated ? SlumpTicks : FlopTicks);
        Spotted = null;
        Scp492Effects.layers(World, this, Scp492Registry.TearLayers, 1.0f, 1.0f);
        Scp492Effects.stuffing(World, this, Scp492Effects.StuffingSpill);
    }

    public static boolean fireDamage(DamageSource Source) {
        return Source.is(DamageTypeTags.IS_FIRE);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level().isClientSide || !(level() instanceof ServerLevel World)) {
            return super.hurt(Source, Amount);
        }
        boolean Fire = fireDamage(Source);
        boolean Absolute = Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!Fire && !Absolute && isTorn()) {
            if (!isInvulnerableTo(Source) && invulnerableTime <= 10) {
                Scp492Effects.stuffing(World, this, Scp492Effects.StuffingPuff);
                Scp492Effects.layers(World, this, Scp492Registry.RustleLayers, 0.6f, 1.1f);
                invulnerableTime = 20;
            }
            return false;
        }
        float Taken = Fire ? Amount * FireScale : Amount;
        Blunt = !Fire && !Absolute;
        boolean Hurt;
        try {
            Hurt = super.hurt(Source, Taken);
        } finally {
            Blunt = false;
        }
        if (Hurt && Source.getEntity() instanceof Player User) {
            HurtBy = User.getUUID();
            HurtAt = tickCount;
        }
        if (Hurt && !Fire) {
            Scp492Effects.stuffing(World, this, Scp492Effects.StuffingPuff);
        }
        if (TearPending) {
            TearPending = false;
            if (isAlive()) {
                tear(World);
            }
        }
        return Hurt;
    }

    @Override
    public void setHealth(float Health) {
        if (Blunt && Health <= 0.0f) {
            Health = TornHealth;
            TearPending = true;
        }
        super.setHealth(Health);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        if (Effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            return false;
        }
        return super.canBeAffected(Effect);
    }

    @Override
    public boolean canDrownInFluidType(FluidType Type) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
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

    @Override
    public boolean canBeLeashed() {
        return !isTorn();
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
    protected boolean isImmobile() {
        return super.isImmobile() || isTorn();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isTorn() ? null : isAfraid() ? Scp492Registry.Fear.get() : Scp492Registry.Chatter.get();
    }

    @Override
    public void playAmbientSound() {
        if (!(level() instanceof ServerLevel World) || isTorn()) {
            return;
        }
        Scp492Effects.layers(World, this, isAfraid() ? Scp492Registry.FearLayers : Scp492Registry.ChatterLayers, isAfraid() ? 0.8f : 0.6f, VoicePitch);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp492Registry.Rustle.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            Scp492Effects.layers(World, this, fireDamage(Source) ? Scp492Registry.FearLayers : Scp492Registry.RustleLayers, 0.8f, 1.0f);
        }
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void die(DamageSource Source) {
        if (level() instanceof ServerLevel World && !isRemoved() && !dead) {
            Scp492Effects.layers(World, this, fireDamage(Source) ? Scp492Registry.BurnLayers : Scp492Registry.TearLayers, 1.0f, 1.0f);
            Scp492Effects.stuffing(World, this, Scp492Effects.StuffingSpill);
        }
        super.die(Source);
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putBoolean("Torn", isTorn());
        if (HurtBy != null) {
            Saved.putUUID("HurtBy", HurtBy);
            Saved.putInt("HurtAgo", Math.max(0, tickCount - HurtAt));
        }
        if (Sought != null) {
            Saved.putUUID("Sought", Sought);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        entityData.set(TornData, Saved.getBoolean("Torn"));
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        HurtBy = Saved.hasUUID("HurtBy") ? Saved.getUUID("HurtBy") : null;
        HurtAt = HurtBy != null ? tickCount - Saved.getInt("HurtAgo") : -100000;
        Sought = Saved.hasUUID("Sought") ? Saved.getUUID("Sought") : null;
        setPersistenceRequired();
        updateControlFlags();
    }

    public RawAnimation baseAnimation() {
        if (isDeadOrDying() || isTorn()) {
            return HeapLoop;
        }
        if (isInSittingPose()) {
            return SitLoop;
        }
        if (isStriding()) {
            if (isRunning()) {
                return Panicky ? PanicLoop : RunLoop;
            }
            return WalkLoop;
        }
        return isAfraid() ? CowerLoop : IdleLoop;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        AnimationController<Scp492Figure> Body = new Scp492GaitController(this, "body", Scp492GaitController.LoopBlend, Event -> {
            String Shown = actionClip();
            Event.getController().transitionLength(Shown == null ? Scp492GaitController.LoopBlend : Scp492GaitController.ActionBlend);
            if (Shown != null && !isDeadOrDying()) {
                RawAnimation Play = switch (Shown) {
                    case "point" -> PointPlay;
                    case "tip" -> TipPlay;
                    case "flop" -> FlopPlay;
                    case "sit_down" -> SitDownPlay;
                    case "stand_up" -> StandUpPlay;
                    case "slump" -> SlumpPlay;
                    default -> RisePlay;
                };
                return Event.setAndContinue(Play);
            }
            return Event.setAndContinue(baseAnimation());
        });
        BodyController = Body;
        Controllers.add(Body);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
