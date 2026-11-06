package net.rai.scp_unleashed.scp407;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class Scp407Titan extends Monster implements GeoEntity, Scp407Bound, Scp407Animated, IEntityWithComplexSpawn {
    public static final float Width = 0.9f;
    public static final float Height = 2.9f;
    public static final float EyeHeight = 2.62f;
    public static final double Health = 160.0;
    public static final double Armour = 6.0;
    public static final double Speed = 0.24;
    public static final double Damage = 9.0;
    public static final double KnockbackResistance = 0.9;
    public static final double FollowRange = 32.0;
    public static final double ChaseModifier = 1.15;
    public static final double FeedModifier = 1.15;
    public static final double WanderModifier = 0.7;
    public static final double SwipeReach = 3.2;
    public static final double GrabReach = 1.45;
    public static final double FeedRange = 20.0;
    public static final double PreferPeople = 10.0;
    public static final int SwipeCooldown = 30;
    public static final int GrabCooldown = 40;
    public static final int FeedLook = 10;
    public static final int FeedRepath = 10;
    public static final int FeedProbes = 4;
    public static final int FeedPatience = 300;
    public static final int FeedStall = 40;
    public static final double StallStep = 0.0025;
    public static final int SpurnTicks = 100;
    public static final int StartleTicks = Scp407RigData.TitanGrabCatch + 4;
    public static final float SwipeKnock = 1.1f;
    public static final float AbsorbHeal = 12.0f;
    public static final int RoomRadius = 16;
    public static final int FieldRadius = 20;
    public static final int StoopScan = 10;
    public static final int StoopClear = 4;
    public static final float StoopEase = 0.08f;
    public static final int GroanInterval = 260;
    public static final int BindCheckTicks = 20;
    public static final int DeathTicks = 50;
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final byte ActionSwipe = 2;
    public static final byte ActionGrab = 3;
    public static final byte ActionWither = 4;
    public static final Scp407Gait.Shape TitanGait = new Scp407Gait.Shape(
            new Scp407Gait.Profile(Scp407RigData.TitanWalkCycle, Scp407RigData.TitanWalkReference, 0.45, 2.0, Scp407RigData.TitanWalkContacts, new float[]{1.0f, 1.0f}),
            new Scp407Gait.Profile(Scp407RigData.TitanWalkCycle, Scp407RigData.TitanWalkReference, 0.45, 2.0, Scp407RigData.TitanWalkContacts, new float[]{1.0f, 1.0f}),
            10.0, 9.0, 2, Scp407Registry.TitanStep, Scp407Registry.TitanStepCreak, 0.9f, 0.55f, 1.0f, 28.0);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp407Titan.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp407Titan.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LockedYawData = SynchedEntityData.defineId(Scp407Titan.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> StoopData = SynchedEntityData.defineId(Scp407Titan.class, EntityDataSerializers.FLOAT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation SwipePlay = RawAnimation.begin().thenPlayAndHold("swipe");
    private static final RawAnimation GrabPlay = RawAnimation.begin().thenPlayAndHold("grab");
    private static final RawAnimation WitherPlay = RawAnimation.begin().thenPlayAndHold("wither");
    private static final List<Predicate<LivingEntity>> Quarry = new CopyOnWriteArrayList<>();
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int ActionTicks;
    private int Session = Unbound;
    @Nullable
    private LivingEntity Victim;
    @Nullable
    private Scp407Sprout Prey;
    private int SwipeReady;
    private int GrabReady;
    private int Eaten;
    private final Object2IntOpenHashMap<UUID> Spurned = new Object2IntOpenHashMap<>();
    @Nullable
    private BlockPos Home;
    private boolean Room;
    private int TickOrigin;
    private float RenderPartial;
    private float StoopNow;
    private float StoopBefore;
    @Nullable
    private Scp407Gait Gait;

    public Scp407Titan(EntityType<? extends Scp407Titan> Type, Level World) {
        super(Type, World);
        xpReward = 30;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.ARMOR, Armour).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance).add(Attributes.FOLLOW_RANGE, FollowRange)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.ATTACK_KNOCKBACK, 0.6);
    }

    public static void addQuarry(Predicate<LivingEntity> Test) {
        Quarry.add(Test);
    }

    public static boolean isQuarry(LivingEntity Found) {
        if (!Found.isAlive() || Found.isSpectator() || !Targeting.isTargetable(Found) || Found instanceof Scp407Titan || Found instanceof Scp407Sprout) {
            return false;
        }
        if (Scp407Fusion.isHeld(Found)) {
            return false;
        }
        for (Predicate<LivingEntity> Each : Quarry) {
            if (Each.test(Found)) {
                return true;
            }
        }
        if (Found instanceof Player User) {
            return !User.isCreative();
        }
        return ControlBodies.isPersonLike(Found);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(LockedYawData, 0.0f);
        Builder.define(StoopData, 0.0f);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HoldStill());
        goalSelector.addGoal(2, new Swipe());
        goalSelector.addGoal(3, new Feed());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Scp407Titan.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, Scp407Titan::isQuarry));
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isEmerging() {
        return action() == ActionEmerge;
    }

    public boolean isFeeding() {
        return action() == ActionGrab;
    }

    public boolean locksMovement() {
        return isActing();
    }

    public boolean locksYaw() {
        return isActing();
    }

    public int eaten() {
        return Eaten;
    }

    @Nullable
    @Override
    public String actionClip() {
        return switch (action()) {
            case ActionEmerge -> "emerge";
            case ActionSwipe -> "swipe";
            case ActionGrab -> "grab";
            case ActionWither -> "wither";
            default -> null;
        };
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(LockedYawData, getYRot());
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        getNavigation().stop();
        applyLockedYaw();
    }

    @Override
    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    public void applyLockedYaw() {
        if (!locksYaw()) {
            return;
        }
        float Yaw = entityData.get(LockedYawData);
        setYRot(Yaw);
        yRotO = Yaw;
        yBodyRot = Yaw;
        yBodyRotO = Yaw;
    }

    private void face(Entity Target) {
        float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
    }

    @Override
    public int boundSession() {
        return Session;
    }

    @Override
    public void bindSession(int Id) {
        Session = Id;
    }

    public void setHome(BlockPos Origin, boolean InRoom) {
        Home = Origin.immutable();
        Room = InRoom;
        restrictTo(Home, InRoom ? RoomRadius : FieldRadius);
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
        dropPrey();
        setAction(ActionWither, Scp407RigData.TitanWitherTicks);
        playSound(Scp407Registry.WitherCrackle.get(), 1.4f, 0.75f);
    }

    public void beginEmerge() {
        setAction(ActionEmerge, Scp407RigData.TitanEmergeTicks);
    }

    public boolean canSwipe() {
        return SwipeReady <= 0 && !isActing();
    }

    public boolean canGrab() {
        return GrabReady <= 0 && !isActing();
    }

    public void beginSwipe(LivingEntity Target) {
        face(Target);
        Victim = Target;
        SwipeReady = SwipeCooldown;
        setAction(ActionSwipe, Scp407RigData.TitanSwipeTicks);
        playSound(Scp407Registry.TitanGroan.get(), 1.0f, 1.15f + random.nextFloat() * 0.1f);
    }

    public void beginGrab(Scp407Sprout Target) {
        face(Target);
        Prey = Target;
        GrabReady = GrabCooldown;
        setAction(ActionGrab, Scp407RigData.TitanGrabTicks);
        Target.startle(StartleTicks);
    }

    public void spurn(Scp407Sprout Target) {
        Spurned.put(Target.getUUID(), tickCount + SpurnTicks);
    }

    public boolean spurns(Scp407Sprout Target) {
        int Until = Spurned.getOrDefault(Target.getUUID(), -1);
        if (Until < 0) {
            return false;
        }
        if (Until <= tickCount) {
            Spurned.removeInt(Target.getUUID());
            return false;
        }
        return true;
    }

    public double reachTo(Entity Target) {
        return Math.sqrt(distanceToSqr(Target)) - Target.getBbWidth() * 0.5;
    }

    public float stoop(float Partial) {
        return Mth.lerp(Partial, StoopBefore, StoopNow);
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
        applyLockedYaw();
        if (level().isClientSide) {
            StoopBefore = StoopNow;
            StoopNow += Mth.clamp(entityData.get(StoopData) - StoopNow, -StoopEase, StoopEase);
            Scp407Gait Current = gait();
            if (Current != null) {
                Current.tick(this, locksMovement());
            }
            return;
        }
        if (!isAlive()) {
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Session != Unbound && !isWithering() && (tickCount + getId()) % BindCheckTicks == 0 && !Scp407Sessions.get(World).isPlaying(Session)) {
            beginWither();
        }
        if (tickCount % StoopScan == 0) {
            entityData.set(StoopData, stoopFor(World));
        }
        SwipeReady = Math.max(0, SwipeReady - 1);
        GrabReady = Math.max(0, GrabReady - 1);
        if (tickCount % SpurnTicks == 0 && !Spurned.isEmpty()) {
            Spurned.object2IntEntrySet().removeIf(Entry -> Entry.getIntValue() <= tickCount);
        }
        if (ActionTicks > 0) {
            byte Current = action();
            actionTick(World, Current, (int) actionElapsed());
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved()) {
                if (Current == ActionWither) {
                    crumble(World);
                    return;
                }
                entityData.set(ActionData, ActionNone);
                Victim = null;
                dropPrey();
            }
        }
    }

    private float stoopFor(ServerLevel World) {
        BlockPos Feet = blockPosition();
        int Clear = 0;
        for (int Rise = 0; Rise < StoopClear; Rise++) {
            BlockState State = World.getBlockState(Feet.above(Rise));
            if (!State.getCollisionShape(World, Feet.above(Rise)).isEmpty() && !(State.getBlock() instanceof Scp407MatBlock)) {
                break;
            }
            Clear++;
        }
        return Clear >= StoopClear ? 0.0f : 1.0f;
    }

    private void actionTick(ServerLevel World, byte Kind, int Elapsed) {
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (Kind == ActionEmerge) {
            if (Elapsed % 3 == 0 && Elapsed < Scp407RigData.TitanEmergeTicks - 10) {
                BlockState Ground = World.getBlockState(blockPosition().below());
                if (!Ground.isAir()) {
                    World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), getX(), getY() + 0.1, getZ(), 8, 0.5, 0.05, 0.5, 0.1);
                }
                World.sendParticles(Scp407Registry.Leaf.get(), getX(), getY() + 0.4, getZ(), 4, 0.5, 0.3, 0.5, 0.03);
            }
            if (Elapsed == 1) {
                World.playSound(null, blockPosition(), Scp407Registry.TitanEmerge.get(), SoundSource.HOSTILE, 1.8f, 0.85f);
            }
            if (Elapsed == Scp407RigData.TitanEmergeTicks * 2 / 3) {
                World.playSound(null, blockPosition(), Scp407Registry.TitanGroan.get(), SoundSource.HOSTILE, 1.8f, 0.8f);
                World.sendParticles(Scp407Registry.Spore.get(), getX(), getY() + 2.4, getZ(), 30, 0.6, 0.6, 0.6, 0.02);
            }
            return;
        }
        if (Kind == ActionSwipe) {
            if (Victim != null && Victim.isAlive()) {
                getLookControl().setLookAt(Victim, 30.0f, 30.0f);
            }
            if (Elapsed == Scp407RigData.TitanSwipeHit) {
                swipeHit(World);
            }
            return;
        }
        if (Kind == ActionGrab) {
            grabTick(World, Elapsed);
            return;
        }
        if (Kind == ActionWither && Elapsed % 4 == 0) {
            World.sendParticles(Scp407Registry.Ash.get(), getX(), getY() + 1.6, getZ(), 6, 0.4, 0.9, 0.4, 0.02);
            if (Elapsed % 12 == 0) {
                World.sendParticles(Scp407Registry.Leaf.get(), getX(), getY() + 2.0, getZ(), 4, 0.4, 0.6, 0.4, 0.02);
            }
        }
    }

    private void swipeHit(ServerLevel World) {
        playSound(Scp407Registry.TitanSwipe.get(), 1.2f, 0.9f + random.nextFloat() * 0.15f);
        AABB Reach = getBoundingBox().inflate(SwipeReach, 0.5, SwipeReach);
        List<LivingEntity> Struck = World.getEntitiesOfClass(LivingEntity.class, Reach, Found -> isQuarry(Found) && reachTo(Found) <= SwipeReach + 0.4
                && inFront(Found) && hasLineOfSight(Found));
        for (LivingEntity Target : Struck) {
            if (doHurtTarget(Target)) {
                Target.knockback(SwipeKnock, getX() - Target.getX(), getZ() - Target.getZ());
                Target.hurtMarked = true;
            }
        }
    }

    private boolean inFront(Entity Target) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Toward = new Vec3(Target.getX() - getX(), 0.0, Target.getZ() - getZ());
        double Length = Toward.length();
        return Length < 0.8 || Facing.dot(Toward.scale(1.0 / Length)) > -0.2;
    }

    public Vec3 handPoint(double Elapsed) {
        float[][] Path = Scp407RigData.GrabHand;
        int Index = Mth.clamp((int) Elapsed, 0, Path.length - 1);
        int Next = Math.min(Path.length - 1, Index + 1);
        double Share = Mth.clamp(Elapsed - Index, 0.0, 1.0);
        double Side = Mth.lerp(Share, Path[Index][0], Path[Next][0]);
        double Up = Mth.lerp(Share, Path[Index][1], Path[Next][1]);
        double Ahead = Mth.lerp(Share, Path[Index][2], Path[Next][2]);
        float Yaw = getYRot() * Mth.DEG_TO_RAD;
        double Cos = Mth.cos(Yaw);
        double Sin = Mth.sin(Yaw);
        return new Vec3(getX() + Side * Cos - Ahead * Sin, getY() + Up, getZ() + Side * Sin + Ahead * Cos);
    }

    private void grabTick(ServerLevel World, int Elapsed) {
        Scp407Sprout Target = Prey;
        if (Target == null || !Target.isAlive() || Target.isRemoved()) {
            return;
        }
        if (Elapsed < Scp407RigData.TitanGrabCatch) {
            face(Target);
            entityData.set(LockedYawData, getYRot());
            return;
        }
        if (Elapsed == Scp407RigData.TitanGrabCatch) {
            if (reachTo(Target) > GrabReach + 0.9 || Target.isWithering()) {
                Prey = null;
                return;
            }
            Target.grabbed();
            playSound(Scp407Registry.TitanGrab.get(), 1.0f, 0.95f + random.nextFloat() * 0.1f);
        }
        if (!Target.isHeld()) {
            return;
        }
        Vec3 Hand = handPoint(Elapsed);
        Target.holdAt(Hand.subtract(0.0, Target.getBbHeight() * 0.5, 0.0), getYRot() + 180.0f);
        if (Elapsed >= Scp407RigData.TitanGrabAbsorb) {
            Vec3 Mask = new Vec3(getX(), getY() + 2.6, getZ());
            World.sendParticles(Scp407Registry.Leaf.get(), Hand.x, Hand.y, Hand.z, 16, 0.25, 0.25, 0.25, 0.05);
            World.sendParticles(Scp407Registry.Spore.get(), Mask.x, Mask.y, Mask.z, 8, 0.3, 0.3, 0.3, 0.02);
            World.playSound(null, blockPosition(), Scp407Registry.TitanAbsorb.get(), SoundSource.HOSTILE, 1.1f, 0.9f + random.nextFloat() * 0.15f);
            Target.absorbed();
            Prey = null;
            Eaten++;
            heal(AbsorbHeal);
        }
    }

    private void dropPrey() {
        if (Prey != null && Prey.isAlive()) {
            Prey.release();
        }
        Prey = null;
    }

    private void crumble(ServerLevel World) {
        World.sendParticles(Scp407Registry.Ash.get(), getX(), getY() + 1.4, getZ(), 40, 0.5, 1.2, 0.5, 0.03);
        World.sendParticles(Scp407Registry.Leaf.get(), getX(), getY() + 1.4, getZ(), 24, 0.5, 1.2, 0.5, 0.03);
        World.playSound(null, blockPosition(), Scp407Registry.TitanDeath.get(), SoundSource.HOSTILE, 1.4f, 0.8f);
        World.playSound(null, blockPosition(), Scp407Registry.WitherCrumble.get(), SoundSource.HOSTILE, 1.2f, 0.7f);
        discard();
    }

    @Override
    protected void tickDeath() {
        ++deathTime;
        if (level() instanceof ServerLevel World && deathTime % 5 == 0) {
            World.sendParticles(Scp407Registry.Ash.get(), getX(), getY() + 1.2, getZ(), 6, 0.4, 0.8, 0.4, 0.02);
        }
        if (deathTime >= DeathTicks && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource Source) {
        dropPrey();
        super.die(Source);
        if (!level().isClientSide) {
            entityData.set(LockedYawData, getYRot());
            entityData.set(ActionStartData, tickCount);
            entityData.set(ActionData, ActionWither);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && (isEmerging() || isWithering())) {
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World) {
            World.sendParticles(Scp407Registry.Leaf.get(), getX(), getY() + 1.6, getZ(), 4, 0.3, 0.5, 0.3, 0.04);
        }
        return Hurt;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isActing() && super.isPushable();
    }

    @Override
    public int getAmbientSoundInterval() {
        return GroanInterval;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp407Registry.TitanGroan.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp407Registry.TitanHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp407Registry.TitanDeath.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected float getSoundVolume() {
        return 1.4f;
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
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.5, 1.2, 1.5);
    }

    @Nullable
    @Override
    public Scp407Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp407Gait(TitanGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp407Gait Current = gait();
        return Current != null && Current.striding();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp407Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    public float witherShare() {
        if (!isWithering()) {
            return 0.0f;
        }
        return (float) Math.min(1.0, actionElapsed() / Scp407RigData.TitanWitherTicks);
    }

    public String stateKey() {
        return switch (action()) {
            case ActionEmerge -> "emerging";
            case ActionGrab -> "feeding";
            case ActionWither -> "withering";
            case ActionSwipe -> "striking";
            default -> getTarget() != null && getTarget().isAlive() ? "hunting" : "roaming";
        };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Data) {
        super.addAdditionalSaveData(Data);
        Data.putInt("Scp407Session", Session);
        Data.putInt("Eaten", Eaten);
        if (Home != null) {
            Data.putLong("Home", Home.asLong());
            Data.putBoolean("Room", Room);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Data) {
        super.readAdditionalSaveData(Data);
        Session = Data.contains("Scp407Session") ? Data.getInt("Scp407Session") : Unbound;
        Eaten = Data.getInt("Eaten");
        if (Data.contains("Home")) {
            setHome(BlockPos.of(Data.getLong("Home")), Data.getBoolean("Room"));
        }
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp407GaitController<>(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Current == ActionEmerge ? 0 : Current == ActionNone ? Scp407GaitController.LoopBlend : Scp407GaitController.ActionBlend);
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionWither) {
                return Event.setAndContinue(WitherPlay);
            }
            if (Current == ActionSwipe) {
                return Event.setAndContinue(SwipePlay);
            }
            if (Current == ActionGrab) {
                return Event.setAndContinue(GrabPlay);
            }
            if (isStriding()) {
                return Event.setAndContinue(WalkLoop);
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
            return locksMovement() || isDeadOrDying();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }
    }

    private class Swipe extends Goal {
        private int Repath;

        Swipe() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = getTarget();
            return Target != null && isQuarry(Target) && !isActing() && distanceToSqr(Target) <= FollowRange * FollowRange;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = getTarget();
            if (Target == null) {
                return;
            }
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (reachTo(Target) <= SwipeReach && hasLineOfSight(Target)) {
                getNavigation().stop();
                if (canSwipe()) {
                    beginSwipe(Target);
                }
                return;
            }
            if (--Repath <= 0) {
                Repath = 8;
                getNavigation().moveTo(Target, ChaseModifier);
            }
        }
    }

    private class Feed extends Goal {
        @Nullable
        private Scp407Sprout Meal;
        @Nullable
        private Path Route;
        private int Repath;
        private int Chase;
        private int Stall;
        private int NextLook;
        private Vec3 Last = Vec3.ZERO;

        Feed() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean edible(Scp407Sprout Sprout) {
            return Sprout.isAlive() && !Sprout.isHeld() && !Sprout.isWithering() && Sprout.action() != Scp407Sprout.ActionHatch && !spurns(Sprout);
        }

        private boolean inReach(Scp407Sprout Sprout) {
            return reachTo(Sprout) <= GrabReach && hasLineOfSight(Sprout);
        }

        @Nullable
        private Path routeTo(Scp407Sprout Sprout) {
            Path Way = getNavigation().createPath(Sprout, 1);
            return Way != null && Way.canReach() ? Way : null;
        }

        @Nullable
        private Scp407Sprout find() {
            List<Scp407Sprout> Candidates = new ArrayList<>(level().getEntitiesOfClass(Scp407Sprout.class, getBoundingBox().inflate(FeedRange, 6.0, FeedRange), this::edible));
            Candidates.sort(Comparator.comparingDouble(Scp407Titan.this::distanceToSqr));
            int Probes = 0;
            for (Scp407Sprout Sprout : Candidates) {
                if (inReach(Sprout)) {
                    Route = null;
                    return Sprout;
                }
                if (Probes++ >= FeedProbes) {
                    break;
                }
                Path Way = routeTo(Sprout);
                if (Way != null) {
                    Route = Way;
                    return Sprout;
                }
                spurn(Sprout);
            }
            return null;
        }

        @Override
        public boolean canUse() {
            if (isActing() || GrabReady > 0 || !onGround() || tickCount < NextLook) {
                return false;
            }
            LivingEntity Target = getTarget();
            if (Target != null && Target.isAlive() && distanceToSqr(Target) < PreferPeople * PreferPeople) {
                return false;
            }
            NextLook = tickCount + FeedLook;
            Meal = find();
            return Meal != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Meal != null && edible(Meal) && !isActing();
        }

        @Override
        public void start() {
            Chase = 0;
            Stall = 0;
            Last = position();
            Repath = FeedRepath;
            if (Route != null) {
                getNavigation().moveTo(Route, FeedModifier);
            }
        }

        @Override
        public void stop() {
            Meal = null;
            Route = null;
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp407Sprout Current = Meal;
            if (Current == null) {
                return;
            }
            getLookControl().setLookAt(Current, 30.0f, 30.0f);
            if (inReach(Current)) {
                getNavigation().stop();
                if (canGrab()) {
                    beginGrab(Current);
                    Meal = null;
                }
                return;
            }
            boolean Moved = position().distanceToSqr(Last) >= StallStep;
            Last = position();
            Stall = getNavigation().isInProgress() && Moved ? 0 : Stall + 1;
            if (++Chase > FeedPatience || Stall > FeedStall) {
                spurn(Current);
                Meal = null;
                return;
            }
            if (--Repath <= 0 && onGround()) {
                Repath = FeedRepath;
                Route = routeTo(Current);
                if (Route == null) {
                    spurn(Current);
                    Meal = null;
                    return;
                }
                getNavigation().moveTo(Route, FeedModifier);
            }
        }
    }
}
