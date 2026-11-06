package net.rai.scp_unleashed.scp149;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp149Fly extends Monster implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.6f;
    public static final float Height = 0.7f;
    public static final float EyeHeight = 0.55f;
    public static final double Health = 4.0;
    public static final double Cruise = 0.3;
    public static final double ChaseModifier = 1.0;
    public static final double RetreatModifier = 0.9;
    public static final double WanderModifier = 0.4;
    public static final double FollowRange = 32.0;
    public static final float BiteDamage = 2.0f;
    public static final float BiteChance = 0.65f;
    public static final double BiteReach = 0.2;
    public static final int RetreatMin = 30;
    public static final int RetreatMax = 50;
    public static final int MissMin = 12;
    public static final int MissMax = 20;
    public static final double RetreatMinDistance = 4.0;
    public static final double RetreatMaxDistance = 7.0;
    public static final int SatedTicks = 6000;
    public static final int GorgedTicks = 1200;
    public static final double GroupRadius = 24.0;
    public static final int GroupCheck = 100;
    public static final int HungerCheck = 40;
    public static final double Drag = 0.91;
    public static final double Jitter = 0.022;
    public static final double DartKick = 0.11;
    public static final int DartMin = 8;
    public static final int DartMax = 18;
    public static final double HoverLift = 0.012;
    public static final double CeilingLift = 6.0;
    public static final int BiteTicks = 8;
    public static final double WaterMargin = 0.5;
    public static final double BiteMargin = 0.08;
    public static final double WaterRise = 0.18;
    public static final int ShunTicks = 600;
    public static final int SubmergedLimit = 200;
    public static final float BorderMalus = 16.0f;
    public static final double[] AimShares = {0.55, 0.72, 0.88};
    public static final byte ActionNone = 0;
    public static final byte ActionBite = 1;
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp149Fly.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp149Fly.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RestlessData = SynchedEntityData.defineId(Scp149Fly.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> GorgedData = SynchedEntityData.defineId(Scp149Fly.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RestingData = SynchedEntityData.defineId(Scp149Fly.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation HoverLoop = RawAnimation.begin().thenLoop("hover");
    private static final RawAnimation DartLoop = RawAnimation.begin().thenLoop("dart");
    private static final RawAnimation BitePlay = RawAnimation.begin().thenPlayAndHold("bite");
    private static final RawAnimation RestLoop = RawAnimation.begin().thenLoop("rest");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID Group;
    private long LastFed = Long.MIN_VALUE;
    private int ActionTicks;
    private int DartClock;
    private int Spooked;
    private boolean Chewing;
    private boolean Biting;
    private final Map<UUID, Long> Shunned = new HashMap<>();
    private float RenderPartial;
    private int TickOrigin;
    @Nullable
    private Scp149Wings Wings;

    public Scp149Fly(EntityType<? extends Scp149Fly> Type, Level World) {
        super(Type, World);
        moveControl = new Scp149Flight(this);
        setNoGravity(true);
        setPathfindingMalus(PathType.WATER, -1.0f);
        setPathfindingMalus(PathType.WATER_BORDER, BorderMalus);
        xpReward = 3;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Cruise).add(Attributes.FLYING_SPEED, Cruise)
                .add(Attributes.ATTACK_DAMAGE, BiteDamage).add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.ARMOR, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(RestlessData, false);
        Builder.define(GorgedData, false);
        Builder.define(RestingData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp149Goals.Chew(this));
        goalSelector.addGoal(2, new Scp149Goals.BiteRun(this));
        goalSelector.addGoal(4, new Scp149Goals.Perch(this));
        goalSelector.addGoal(5, new Scp149Goals.Buzz(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Scp149Fly.class).setAlertOthers());
        targetSelector.addGoal(2, new Scp149Goals.Seek(this));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, 10, true, false, Found -> wantsBeasts() && isQuarry(Found) && reachable(Found)));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Animal.class, 10, true, false, Found -> wantsBeasts() && isQuarry(Found) && reachable(Found)));
    }

    public static boolean isPerson(LivingEntity Found) {
        return Scp149Host.isPerson(Found) && isQuarry(Found);
    }

    public static boolean isQuarry(LivingEntity Found) {
        if (Found instanceof Scp149Fly || Found instanceof Scp149Larva || !Found.isAlive() || Found.isSpectator() || !Targeting.isTargetable(Found)) {
            return false;
        }
        return !(Found instanceof Player User) || !User.isCreative();
    }

    public boolean wantsBeasts() {
        return isRestless() || !sated();
    }

    public boolean reachable(LivingEntity Found) {
        return !shuns(Found) && biteAim(Found) != null;
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        FlyingPathNavigation Navigation = new FlyingPathNavigation(this, World) {
            @Override
            protected PathFinder createPathFinder(int Visits) {
                nodeEvaluator = new Scp149DryNodes();
                nodeEvaluator.setCanPassDoors(true);
                return new PathFinder(nodeEvaluator, Visits);
            }
        };
        Navigation.setCanOpenDoors(false);
        Navigation.setCanFloat(true);
        Navigation.setCanPassDoors(true);
        return Navigation;
    }

    public double cruise() {
        return getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public void travel(Vec3 Input) {
        if (isControlledByLocalInstance()) {
            if (isInWater() || isInLava()) {
                moveRelative(0.02f, Input);
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(0.8));
            } else {
                move(MoverType.SELF, getDeltaMovement());
                setDeltaMovement(getDeltaMovement().scale(Drag));
            }
        }
        calculateEntityAnimation(false);
    }

    public void wobble() {
        if (!isAlive() || chewing() || isResting()) {
            return;
        }
        RandomSource Random = getRandom();
        Vec3 Motion = getDeltaMovement().add((Random.nextDouble() - 0.5) * Jitter, (Random.nextDouble() - 0.5) * Jitter, (Random.nextDouble() - 0.5) * Jitter);
        if (--DartClock <= 0) {
            DartClock = DartMin + Random.nextInt(DartMax - DartMin + 1);
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Kick = DartKick * (0.6 + Random.nextDouble() * 0.4) * (isRestless() ? 1.3 : 1.0);
            Motion = Motion.add(Math.cos(Angle) * Kick, (Random.nextDouble() - 0.45) * Kick * 0.6, Math.sin(Angle) * Kick);
        }
        double Ground = groundGap();
        if (Ground < 0.6 && !Biting) {
            Motion = Motion.add(0.0, HoverLift * 2.0, 0.0);
        } else if (getTarget() == null && Ground > CeilingLift) {
            Motion = Motion.add(0.0, -HoverLift, 0.0);
        }
        setDeltaMovement(Motion);
    }

    private double groundGap() {
        BlockPos Pos = blockPosition();
        for (int Depth = 0; Depth <= 8; Depth++) {
            BlockPos Below = Pos.below(Depth);
            BlockState State = level().getBlockState(Below);
            FluidState Liquid = level().getFluidState(Below);
            if (Liquid.is(FluidTags.WATER)) {
                return getY() - (Below.getY() + Liquid.getHeight(level(), Below));
            }
            if (!State.getCollisionShape(level(), Below).isEmpty()) {
                return getY() - (Below.getY() + State.getCollisionShape(level(), Below).max(Direction.Axis.Y));
            }
        }
        return 9.0;
    }

    public boolean wetAt(Vec3 Point) {
        BlockPos Pos = BlockPos.containing(Point);
        FluidState Liquid = level().getFluidState(Pos);
        return Liquid.is(FluidTags.WATER) && Point.y <= Pos.getY() + Liquid.getHeight(level(), Pos);
    }

    public boolean wet(AABB Box) {
        for (BlockPos Pos : BlockPos.betweenClosed(Mth.floor(Box.minX), Mth.floor(Box.minY), Mth.floor(Box.minZ), Mth.floor(Box.maxX), Mth.floor(Box.maxY), Mth.floor(Box.maxZ))) {
            FluidState Liquid = level().getFluidState(Pos);
            if (Liquid.is(FluidTags.WATER) && Box.minY <= Pos.getY() + Liquid.getHeight(level(), Pos)) {
                return true;
            }
        }
        return false;
    }

    public void dryGuard() {
        if (isInWater()) {
            return;
        }
        double Margin = Biting ? BiteMargin : WaterMargin;
        Vec3 Motion = getDeltaMovement();
        AABB Ahead = getBoundingBox().move(Motion).expandTowards(0.0, -Margin, 0.0);
        if (!wet(Ahead)) {
            return;
        }
        double Rise = Math.max(Motion.y, 0.02);
        Vec3 Flat = new Vec3(Motion.x, 0.0, Motion.z);
        if (wet(getBoundingBox().move(Flat).expandTowards(0.0, -Margin, 0.0))) {
            Flat = Vec3.ZERO;
        }
        setDeltaMovement(Flat.x, Rise, Flat.z);
    }

    public void setBiting(boolean Value) {
        Biting = Value;
    }

    @Nullable
    public Vec3 biteAim(LivingEntity Target) {
        if (Target.isEyeInFluid(FluidTags.WATER)) {
            return null;
        }
        for (double Share : AimShares) {
            Vec3 Point = new Vec3(Target.getX(), Target.getY() + Target.getBbHeight() * Share, Target.getZ());
            if (!wetAt(Point)) {
                return Point;
            }
        }
        Vec3 Eye = Target.getEyePosition();
        return wetAt(Eye) ? null : Eye;
    }

    public double waterTop(LivingEntity Target) {
        BlockPos Pos = Target.blockPosition();
        for (int Rise = 0; Rise < 8; Rise++) {
            BlockPos Above = Pos.above(Rise);
            FluidState Liquid = level().getFluidState(Above);
            if (!Liquid.is(FluidTags.WATER)) {
                return Above.getY();
            }
        }
        return Pos.getY() + 8.0;
    }

    public boolean shuns(LivingEntity Target) {
        Long Until = Shunned.get(Target.getUUID());
        return Until != null && Until > tickCount;
    }

    public void shun(LivingEntity Target) {
        Shunned.put(Target.getUUID(), (long) tickCount + ShunTicks);
    }

    public boolean isResting() {
        return entityData.get(RestingData);
    }

    public void setResting(boolean Value) {
        if (entityData.get(RestingData) != Value) {
            entityData.set(RestingData, Value);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp149Wings Current = wings();
            if (Current != null) {
                Current.tick(this);
            }
            return;
        }
        if (!isAlive() || !(level() instanceof ServerLevel World)) {
            return;
        }
        if (ActionTicks > 0 && --ActionTicks <= 0) {
            entityData.set(ActionData, ActionNone);
        }
        if (Spooked > 0) {
            Spooked--;
        }
        if (isInWater()) {
            setResting(false);
            getNavigation().stop();
            Vec3 Motion = getDeltaMovement();
            setDeltaMovement(Motion.x * 0.6, Math.max(Motion.y, WaterRise), Motion.z * 0.6);
        }
        if (tickCount % 200 == 0) {
            Shunned.values().removeIf(Until -> Until <= tickCount);
        }
        long Now = Scp149World.now(World.getServer());
        if ((tickCount + getId()) % HungerCheck == 0) {
            Scp149World Data = Scp149World.get(World.getServer());
            UUID Mine = group(Data, Now);
            boolean Restless = Data.restless(Mine, Now);
            if (entityData.get(RestlessData) != Restless) {
                entityData.set(RestlessData, Restless);
            }
            boolean Gorged = LastFed != Long.MIN_VALUE && Now - LastFed < GorgedTicks;
            if (entityData.get(GorgedData) != Gorged) {
                entityData.set(GorgedData, Gorged);
            }
        }
        if ((tickCount + getId()) % GroupCheck == 0) {
            gather(World, Now);
        }
    }

    private UUID group(Scp149World Data, long Now) {
        UUID Found = Data.groupOf(getUUID());
        if (Found == null) {
            Found = Group != null ? Group : UUID.randomUUID();
            Data.join(getUUID(), Found, Now);
        }
        Group = Found;
        return Found;
    }

    @Nullable
    public UUID groupId() {
        return Group;
    }

    private void gather(ServerLevel World, long Now) {
        Scp149World Data = Scp149World.get(World.getServer());
        UUID Mine = group(Data, Now);
        for (Scp149Fly Other : World.getEntitiesOfClass(Scp149Fly.class, getBoundingBox().inflate(GroupRadius), Found -> Found != this && Found.isAlive())) {
            UUID Theirs = Data.groupOf(Other.getUUID());
            if (Theirs != null && !Theirs.equals(Mine) && Other.distanceToSqr(this) <= GroupRadius * GroupRadius) {
                Mine = Data.merge(Mine, Theirs);
                Other.Group = Mine;
            }
        }
        Group = Mine;
    }

    public void enlist(ServerLevel World) {
        Scp149World Data = Scp149World.get(World.getServer());
        long Now = Scp149World.now(World.getServer());
        UUID Found = Data.groupOf(getUUID());
        if (Found == null) {
            Found = Group;
        }
        if (Found == null) {
            Scp149Fly Nearest = null;
            double Closest = GroupRadius * GroupRadius;
            for (Scp149Fly Other : World.getEntitiesOfClass(Scp149Fly.class, getBoundingBox().inflate(GroupRadius), Candidate -> Candidate != this && Candidate.isAlive())) {
                double Distance = Other.distanceToSqr(this);
                UUID Theirs = Data.groupOf(Other.getUUID());
                if (Theirs != null && Distance <= Closest) {
                    Closest = Distance;
                    Nearest = Other;
                }
            }
            Found = Nearest != null ? Data.groupOf(Nearest.getUUID()) : UUID.randomUUID();
        }
        Data.join(getUUID(), Found, Now);
        Group = Found;
    }

    public void newborn() {
        if (level() instanceof ServerLevel World) {
            LastFed = Scp149World.now(World.getServer());
        }
    }

    public boolean sated() {
        if (!(level() instanceof ServerLevel World) || LastFed == Long.MIN_VALUE) {
            return false;
        }
        return Scp149World.now(World.getServer()) - LastFed < SatedTicks;
    }

    public boolean isRestless() {
        return entityData.get(RestlessData);
    }

    public boolean isGorged() {
        return entityData.get(GorgedData);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    @Nullable
    public String actionClip() {
        return action() == ActionBite ? "bite" : null;
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
    }

    public double actionElapsed() {
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    public boolean spooked() {
        return Spooked > 0;
    }

    public boolean chewing() {
        return Chewing;
    }

    public void setChewing(boolean Value) {
        Chewing = Value;
    }

    public boolean facesQuarry() {
        LivingEntity Target = getTarget();
        return Target != null && Target.isAlive() && distanceToSqr(Target) < 9.0 && !spooked();
    }

    public boolean touching(LivingEntity Target) {
        return getBoundingBox().inflate(BiteReach).intersects(Target.getBoundingBox());
    }

    public boolean bite(LivingEntity Target) {
        setAction(ActionBite, BiteTicks);
        if (!(level() instanceof ServerLevel World)) {
            return false;
        }
        Vec3 Contact = Target.getBoundingBox().getCenter().add(position().subtract(Target.position()).normalize().scale(Target.getBbWidth() * 0.5));
        if (Scp149Host.shielded(Target) || getRandom().nextFloat() >= BiteChance) {
            playSound(Scp149Registry.Miss.get(), 0.6f, 1.0f + getRandom().nextFloat() * 0.2f);
            return false;
        }
        if (!Target.hurt(damageSources().source(Scp149Registry.BiteDamage, this), BiteDamage)) {
            playSound(Scp149Registry.Miss.get(), 0.6f, 1.0f + getRandom().nextFloat() * 0.2f);
            return false;
        }
        setLastHurtMob(Target);
        playLayers(Scp149Registry.BiteLayers, 0.9f, 0.95f + getRandom().nextFloat() * 0.1f);
        World.sendParticles(Scp149Registry.Mote.get(), Contact.x, Contact.y, Contact.z, 5, 0.08, 0.08, 0.08, 0.03);
        if (Scp149Host.canHost(Target)) {
            Scp149Host.infect(Target);
        }
        feed(World);
        return true;
    }

    public void feed(ServerLevel World) {
        long Now = Scp149World.now(World.getServer());
        LastFed = Now;
        Scp149World Data = Scp149World.get(World.getServer());
        Data.feed(group(Data, Now), Now);
        entityData.set(GorgedData, true);
        entityData.set(RestlessData, false);
        heal(getMaxHealth());
        setTarget(null);
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch);
        }
    }

    public Vec3 retreatFrom(LivingEntity Target) {
        RandomSource Random = getRandom();
        Vec3 Away = position().subtract(Target.position());
        double Base = Away.horizontalDistanceSqr() > 1.0e-4 ? Math.atan2(Away.z, Away.x) : Random.nextDouble() * Math.PI * 2.0;
        for (int Attempt = 0; Attempt < 8; Attempt++) {
            double Angle = Base + (Random.nextDouble() - 0.5) * Math.PI * 1.2;
            double Reach = RetreatMinDistance + Random.nextDouble() * (RetreatMaxDistance - RetreatMinDistance);
            Vec3 Spot = new Vec3(Target.getX() + Math.cos(Angle) * Reach, Target.getY() + 1.2 + Random.nextDouble() * 2.0, Target.getZ() + Math.sin(Angle) * Reach);
            if (level().noCollision(this, getDimensions(getPose()).makeBoundingBox(Spot))) {
                return Spot;
            }
        }
        return position().add(Away.normalize().scale(3.0)).add(0.0, 1.0, 0.0);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && isAlive()) {
            Spooked = 10 + getRandom().nextInt(10);
            if (level() instanceof ServerLevel World) {
                World.sendParticles(Scp149Registry.Mote.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 3, 0.15, 0.15, 0.15, 0.04);
            }
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp149Registry.Mote.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 12, 0.2, 0.2, 0.2, 0.06);
            playSound(Scp149Registry.DeathPop.get(), 0.8f, 1.0f + getRandom().nextFloat() * 0.2f);
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        super.remove(Reason);
        if (level() instanceof ServerLevel World && (Reason.shouldDestroy() || Reason == RemovalReason.CHANGED_DIMENSION)) {
            Scp149World.get(World.getServer()).leave(getUUID());
        }
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double Dy, boolean Grounded, BlockState State, BlockPos Pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp149Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp149Registry.Death.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
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
        return super.getBoundingBoxForCulling().inflate(1.2);
    }

    @Nullable
    public Scp149Wings wings() {
        if (Wings == null && level().isClientSide) {
            Wings = new Scp149Wings();
        }
        return Wings;
    }

    public boolean isDarting() {
        Scp149Wings Current = wings();
        return Current != null && Current.darting();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float whinePitch() {
        Scp149Wings Current = wings();
        double Speed = Current != null ? Current.speed() : 0.0;
        return (float) (0.92 + Mth.clamp(Speed / Cruise, 0.0, 1.0) * 0.16 + (isRestless() ? 0.06 : 0.0));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (Group != null) {
            Tag.putUUID("Group", Group);
        }
        if (LastFed != Long.MIN_VALUE) {
            Tag.putLong("Fed", LastFed);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Group = Tag.hasUUID("Group") ? Tag.getUUID("Group") : null;
        LastFed = Tag.contains("Fed") ? Tag.getLong("Fed") : Long.MIN_VALUE;
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        setNoGravity(true);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp149WingController(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp149WingController.blendFor(Current));
            if (Current == ActionBite) {
                return Event.setAndContinue(BitePlay);
            }
            if (isResting()) {
                return Event.setAndContinue(RestLoop);
            }
            return Event.setAndContinue(isDarting() ? DartLoop : HoverLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
