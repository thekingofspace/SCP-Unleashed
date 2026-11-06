package net.rai.scp_unleashed.scp111;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Scp111Snail extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.25f;
    public static final float Height = 0.24f;
    public static final float EyeHeight = 0.16f;
    public static final double Health = 4.0;
    public static final double Armour = 2.0;
    public static final double Speed = 0.08;
    public static final double WanderModifier = 0.75;
    public static final double ForageModifier = 1.0;
    public static final double LeaveModifier = 1.2;
    public static final int WanderInterval = 70;
    public static final int GrowTicks = 12000;
    public static final float BabyScale = 0.4f;
    public static final int ScaleSteps = 32;
    public static final int AgeSync = 20;
    public static final int MaxFed = 100;
    public static final int StartFed = 50;
    public static final int HatchlingFed = 30;
    public static final int WellFed = 70;
    public static final int Content = 35;
    public static final int FedPerGreens = 25;
    public static final int FedPerNibble = 6;
    public static final int HungerInterval = 600;
    public static final int GrowthPerGreens = 1200;
    public static final int LayInterval = 200;
    public static final int WellFedLayChance = 8;
    public static final int ContentLayChance = 40;
    public static final int LayCooldown = 3600;
    public static final int LayFedCost = 25;
    public static final int CrowdCap = 12;
    public static final double CrowdRange = 16.0;
    public static final int ClutchCap = 4;
    public static final int ClutchRange = 8;
    public static final int ClutchRangeY = 3;
    public static final int PuffMin = 160;
    public static final int PuffSpread = 240;
    public static final int PuffTicks = 14;
    public static final int PuffRelease = 6;
    public static final int BreatheTicks = 28;
    public static final int JetStart = 8;
    public static final int JetEnd = 24;
    public static final int JetBurnEvery = 4;
    public static final double JetRange = 2.6;
    public static final double RetaliateRange = 4.5;
    public static final double JetCone = 0.82;
    public static final int JetCooldown = 50;
    public static final float IgniteSeconds = 2.0f;
    public static final int FireChance = 10;
    public static final int EatTicks = 24;
    public static final int EatBite = 14;
    public static final double SeekRange = 8.0;
    public static final double EatReach = 0.32;
    public static final int ScanInterval = 10;
    public static final int SeekPatience = 300;
    public static final int IgnoreTicks = 1200;
    public static final int NibbleTicks = 24;
    public static final int NibbleBite = 14;
    public static final int NibbleSteps = 10;
    public static final int NibbleHunger = 60;
    public static final int LayTicks = 30;
    public static final int LayDrop = 20;
    public static final double MouthForward = 0.217;
    public static final double MouthUp = 0.163;
    public static final double PuffMouthForward = 0.174;
    public static final double PuffMouthUp = 0.218;
    public static final double JetMouthForward = 0.232;
    public static final double JetMouthUp = 0.145;
    public static final int AmbientInterval = 260;
    public static final double Hearing = 20.0;
    public static final byte ActionNone = 0;
    public static final byte ActionPuff = 1;
    public static final byte ActionBreathe = 2;
    public static final byte ActionEat = 3;
    public static final byte ActionLay = 4;
    public static final byte ActionNibble = 5;
    public static final Scp111Gait.Shape SnailGait = new Scp111Gait.Shape(30.0, 0.0105, 0.4, 2.2);
    private static final EntityDataAccessor<Integer> VariantData = SynchedEntityData.defineId(Scp111Snail.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AgeData = SynchedEntityData.defineId(Scp111Snail.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp111Snail.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp111Snail.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation GlideLoop = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation PuffPlay = RawAnimation.begin().thenPlayAndHold("puff");
    private static final RawAnimation BreathePlay = RawAnimation.begin().thenPlayAndHold("breathe");
    private static final RawAnimation EatPlay = RawAnimation.begin().thenPlayAndHold("eat");
    private static final RawAnimation LayPlay = RawAnimation.begin().thenPlayAndHold("lay");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Age;
    private int Fed = StartFed;
    private int LayReady;
    private int Laid;
    private int PuffReady = PuffMin;
    private int JetReady;
    private int ActionTicks;
    @Nullable
    private LivingEntity StrikeTarget;
    @Nullable
    private ItemEntity Food;
    @Nullable
    private ItemEntity EatFood;
    private int SeekTicks;
    private int IgnoreLeft;
    @Nullable
    private UUID IgnoredFood;
    @Nullable
    private BlockPos LeaveFrom;
    private int LeaveLeft;
    @Nullable
    private BlockPos NibblePos;
    private int NibbleBites;
    private int TickOrigin;
    private float RenderPartial;
    @Nullable
    private Scp111Gait Gait;

    public Scp111Snail(EntityType<? extends Scp111Snail> Type, Level World) {
        super(Type, World);
        xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ARMOR, Armour)
                .add(Attributes.FOLLOW_RANGE, 12.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(VariantData, 0);
        Builder.define(AgeData, 0);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp111Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp111Goals.LeaveHome(this));
        goalSelector.addGoal(3, new Scp111Goals.SeekGreens(this));
        goalSelector.addGoal(4, new Scp111Goals.NibbleLeaves(this));
        WaterAvoidingRandomStrollGoal Stroll = new WaterAvoidingRandomStrollGoal(this, WanderModifier);
        Stroll.setInterval(WanderInterval);
        goalSelector.addGoal(5, Stroll);
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public Scp111Variant getVariant() {
        return Scp111Variant.byIndex(entityData.get(VariantData));
    }

    public void setVariant(Scp111Variant Variant) {
        entityData.set(VariantData, Variant.ordinal());
    }

    public int getGrowthAge() {
        return entityData.get(AgeData);
    }

    public void setAge(int Value) {
        Age = Value;
        entityData.set(AgeData, Value);
    }

    @Override
    public boolean isBaby() {
        return entityData.get(AgeData) < 0;
    }

    @Override
    public void setBaby(boolean Baby) {
        setAge(Baby ? -GrowTicks : 0);
    }

    @Override
    public float getAgeScale() {
        int Current = entityData.get(AgeData);
        if (Current >= 0) {
            return 1.0f;
        }
        float Grown = 1.0f - Math.min(1.0f, -Current / (float) GrowTicks);
        float Raw = BabyScale + (1.0f - BabyScale) * Grown;
        return Math.round(Raw * ScaleSteps) / (float) ScaleSteps;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (AgeData.equals(Key)) {
            refreshDimensions();
        }
    }

    public int getFed() {
        return Fed;
    }

    public void setFed(int Value) {
        Fed = Mth.clamp(Value, 0, MaxFed);
    }

    public int getLaid() {
        return Laid;
    }

    public int layCooldown() {
        return LayReady;
    }

    public void leaveFrom(BlockPos Home, int Ticks) {
        LeaveFrom = Home.immutable();
        LeaveLeft = Ticks;
    }

    @Nullable
    public BlockPos leavingFrom() {
        return LeaveLeft > 0 ? LeaveFrom : null;
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean locksMovement() {
        return isActing();
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionPuff -> "puff";
            case ActionBreathe -> "breathe";
            case ActionEat, ActionNibble -> "eat";
            case ActionLay -> "lay";
            default -> null;
        };
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        getNavigation().stop();
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

    public static boolean isGreens(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(Scp111Registry.Greens);
    }

    public boolean isHungry() {
        return Fed < MaxFed - 10;
    }

    public boolean canForage() {
        return isAlive() && !isActing() && isHungry();
    }

    public boolean canNibble() {
        return canForage() && Fed < NibbleHunger && Food == null && EventHooks.canEntityGrief(level(), this);
    }

    public boolean canKeepNibbling() {
        return canForage() && Food == null && EventHooks.canEntityGrief(level(), this);
    }

    @Nullable
    public ItemEntity noticedFood() {
        return Food != null && Food.isAlive() ? Food : null;
    }

    public Vec3 mouth(double Forward, double Up) {
        float Scale = getAgeScale();
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        return position().add(Facing.x * Forward * Scale, Up * Scale, Facing.z * Forward * Scale);
    }

    public double mouthReach(Vec3 Point) {
        Vec3 Mouth = mouth(MouthForward, 0.0);
        double Dx = Point.x - Mouth.x;
        double Dz = Point.z - Mouth.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public float yawTo(double X, double Z) {
        return (float) (Mth.atan2(Z - getZ(), X - getX()) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public void faceToward(double X, double Z) {
        float Yaw = yawTo(X, Z);
        setYRot(Yaw);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
    }

    public void beginEat(ItemEntity Drop) {
        EatFood = Drop;
        Drop.setPickUpDelay(EatTicks);
        faceToward(Drop.getX(), Drop.getZ());
        setAction(ActionEat, EatTicks);
    }

    public void beginNibble(BlockPos Leaf) {
        NibblePos = Leaf.immutable();
        faceToward(Leaf.getX() + 0.5, Leaf.getZ() + 0.5);
        setAction(ActionNibble, NibbleTicks);
    }

    @Nullable
    public BlockPos nibbling() {
        return NibblePos;
    }

    public void stopNibbling() {
        if (NibblePos != null && level() instanceof ServerLevel World) {
            World.destroyBlockProgress(getId(), NibblePos, -1);
        }
        NibblePos = null;
        NibbleBites = 0;
    }

    public void beginBreathe(LivingEntity Target) {
        StrikeTarget = Target;
        JetReady = JetCooldown;
        faceToward(Target.getX(), Target.getZ());
        setAction(ActionBreathe, BreatheTicks);
        playLayers(Scp111Registry.HissLayers, 0.8f, 1.0f + random.nextFloat() * 0.15f);
    }

    public void beginLay() {
        LayReady = LayCooldown;
        setAction(ActionLay, LayTicks);
    }

    private void beginPuff() {
        PuffReady = PuffMin + random.nextInt(PuffSpread + 1);
        setAction(ActionPuff, PuffTicks);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp111Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
            return;
        }
        if (!isAlive() || !(level() instanceof ServerLevel World)) {
            return;
        }
        growUp();
        JetReady = Math.max(0, JetReady - 1);
        LayReady = Math.max(0, LayReady - 1);
        if (LeaveLeft > 0 && --LeaveLeft == 0) {
            LeaveFrom = null;
        }
        if (tickCount % HungerInterval == 0 && Fed > 0) {
            Fed--;
        }
        if (IgnoreLeft > 0 && --IgnoreLeft == 0) {
            IgnoredFood = null;
        }
        scanFood();
        if (!isActing()) {
            if (--PuffReady <= 0 && World.hasNearbyAlivePlayer(getX(), getY(), getZ(), Hearing)) {
                beginPuff();
            } else if (PuffReady <= 0) {
                PuffReady = PuffMin;
            } else if ((tickCount + getId()) % LayInterval == 0 && wantsToLay(World)) {
                beginLay();
            }
        }
        if (ActionTicks > 0) {
            actionTick(World, action(), (int) actionElapsed());
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved()) {
                byte Ended = action();
                entityData.set(ActionData, ActionNone);
                StrikeTarget = null;
                EatFood = null;
                if (Ended == ActionNibble && NibblePos != null && !World.getBlockState(NibblePos).is(BlockTags.LEAVES)) {
                    stopNibbling();
                }
            }
        }
    }

    private void growUp() {
        if (Age >= 0) {
            return;
        }
        Age++;
        if (Age == 0 || Age % AgeSync == 0) {
            entityData.set(AgeData, Age);
        }
    }

    private void feedGrowth() {
        if (Age < 0) {
            setAge(Math.min(0, Age + GrowthPerGreens - Age / 10));
        }
    }

    private void scanFood() {
        if (tickCount % ScanInterval == 0) {
            ItemEntity Previous = Food;
            Food = canForage() ? findFood() : null;
            SeekTicks = Food != null && Food == Previous ? SeekTicks + ScanInterval : 0;
            if (Food != null && SeekTicks > SeekPatience) {
                IgnoredFood = Food.getUUID();
                IgnoreLeft = IgnoreTicks;
                Food = null;
                SeekTicks = 0;
            }
        } else if (Food != null && (!Food.isAlive() || !canForage())) {
            Food = null;
        }
    }

    @Nullable
    private ItemEntity findFood() {
        List<ItemEntity> Found = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(SeekRange, 2.0, SeekRange),
                Drop -> Drop.isAlive() && isGreens(Drop.getItem()) && Drop.distanceToSqr(this) <= SeekRange * SeekRange && !Drop.getUUID().equals(IgnoredFood));
        return Found.stream().min(Comparator.comparingDouble(Drop -> Drop.distanceToSqr(this))).orElse(null);
    }

    public boolean isGliding() {
        return getDeltaMovement().horizontalDistanceSqr() > 1.0E-5 || getNavigation().isInProgress();
    }

    private boolean wantsToLay(ServerLevel World) {
        if (isBaby() || LayReady > 0 || Fed < Content || !isGliding() || !onGround()) {
            return false;
        }
        int Chance = Fed >= WellFed ? WellFedLayChance : ContentLayChance;
        if (random.nextInt(Chance) != 0) {
            return false;
        }
        return !crowded(World) && layingSpot(World) != null;
    }

    public boolean crowded(ServerLevel World) {
        int Snails = World.getEntitiesOfClass(Scp111Snail.class, getBoundingBox().inflate(CrowdRange), Scp111Snail::isAlive).size();
        if (Snails >= CrowdCap) {
            return true;
        }
        int Clutches = 0;
        BlockPos Centre = blockPosition();
        for (BlockPos Cell : BlockPos.betweenClosed(Centre.offset(-ClutchRange, -ClutchRangeY, -ClutchRange), Centre.offset(ClutchRange, ClutchRangeY, ClutchRange))) {
            if (World.getBlockState(Cell).is(Scp111Registry.EggBlock.get()) && ++Clutches >= ClutchCap) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public BlockPos layingSpot(ServerLevel World) {
        BlockPos Here = blockPosition();
        BlockState There = World.getBlockState(Here);
        if (There.is(Scp111Registry.EggBlock.get())) {
            return There.getValue(Scp111EggBlock.Variant) == getVariant() && There.getValue(Scp111EggBlock.Eggs) < Scp111EggBlock.MaxEggs ? Here : null;
        }
        if (!There.canBeReplaced() || !There.getFluidState().isEmpty() || There.is(BlockTags.LEAVES)) {
            return null;
        }
        BlockPos Below = Here.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP) ? Here : null;
    }

    public boolean layNow(ServerLevel World, boolean Forced) {
        BlockPos Spot = layingSpot(World);
        if (Spot == null) {
            return false;
        }
        int Count = Fed >= WellFed ? 2 + random.nextInt(3) : 1 + random.nextInt(2);
        BlockState There = World.getBlockState(Spot);
        BlockState Placed;
        if (There.is(Scp111Registry.EggBlock.get())) {
            Placed = There.setValue(Scp111EggBlock.Eggs, Math.min(Scp111EggBlock.MaxEggs, There.getValue(Scp111EggBlock.Eggs) + Count));
        } else {
            Placed = Scp111EggBlock.clutch(getVariant(), Count, 0);
        }
        World.setBlock(Spot, Placed, Block.UPDATE_ALL);
        if (!Forced) {
            Fed = Math.max(0, Fed - LayFedCost);
        }
        Laid++;
        playLayers(Scp111Registry.LayLayers, 0.7f, 1.0f + random.nextFloat() * 0.15f);
        Vec3 Centre = Vec3.atBottomCenterOf(Spot);
        World.sendParticles(ParticleTypes.HAPPY_VILLAGER, Centre.x, Centre.y + 0.1, Centre.z, 2, 0.1, 0.05, 0.1, 0.0);
        return true;
    }

    private void actionTick(ServerLevel World, byte Kind, int Elapsed) {
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (Kind == ActionPuff) {
            if (Elapsed == PuffRelease) {
                Scp111Breath.puff(World, mouth(PuffMouthForward, PuffMouthUp), facing().add(0.0, 0.45, 0.0).normalize(), getVariant(), 4 + random.nextInt(3), getAgeScale());
                playLayers(Scp111Registry.PuffLayers, 0.35f, 1.05f + random.nextFloat() * 0.2f);
            }
        } else if (Kind == ActionBreathe) {
            breatheTick(World, Elapsed);
        } else if (Kind == ActionEat) {
            if (Elapsed == EatBite) {
                consumeFood(World);
            }
        } else if (Kind == ActionNibble) {
            if (Elapsed == NibbleBite) {
                nibbleBite(World);
            }
        } else if (Kind == ActionLay) {
            if (Elapsed == LayDrop) {
                layNow(World, false);
            }
        }
    }

    public Vec3 facing() {
        return Vec3.directionFromRotation(0.0f, yBodyRot);
    }

    private void breatheTick(ServerLevel World, int Elapsed) {
        LivingEntity Target = StrikeTarget;
        if (Target != null && Target.isAlive() && Elapsed < JetEnd) {
            getLookControl().setLookAt(Target, 30.0f, 30.0f);
            float Wanted = yawTo(Target.getX(), Target.getZ());
            float Yaw = Mth.approachDegrees(getYRot(), Wanted, 12.0f);
            setYRot(Yaw);
            yBodyRot = Yaw;
            yHeadRot = Yaw;
        }
        if (Elapsed < JetStart || Elapsed >= JetEnd) {
            return;
        }
        float Scale = getAgeScale();
        Vec3 Mouth = mouth(JetMouthForward, JetMouthUp);
        Vec3 Aim = Target != null && Target.isAlive() ? Target.getBoundingBox().getCenter().subtract(Mouth).normalize() : facing();
        Vec3 Way = facing().scale(0.6).add(Aim.scale(0.4)).normalize();
        double Reach = JetRange * (0.5 + 0.5 * Scale);
        Scp111Breath.jet(World, Mouth, Way, getVariant(), Scp111Breath.JetCount, Scale, Reach);
        if (Elapsed == JetStart) {
            playLayers(Scp111Registry.JetLayers, 0.9f, 1.05f + random.nextFloat() * 0.15f);
        }
        if ((Elapsed - JetStart) % JetBurnEvery == 0) {
            scorch(World, Mouth, Way, Reach);
        }
        if (Elapsed == JetEnd - 1) {
            maybeKindle(World, Mouth, Way, Reach);
        }
    }

    private void scorch(ServerLevel World, Vec3 Mouth, Vec3 Way, double Reach) {
        AABB Zone = new AABB(Mouth, Mouth.add(Way.scale(Reach))).inflate(0.6);
        for (LivingEntity Found : World.getEntitiesOfClass(LivingEntity.class, Zone, Candidate -> Candidate.isAlive() && !(Candidate instanceof Scp111Snail))) {
            Vec3 To = Found.getBoundingBox().getCenter().subtract(Mouth);
            double Distance = To.length();
            if (Distance > Reach + Found.getBbWidth() * 0.5 || Distance > 1.0E-4 && To.normalize().dot(Way) < JetCone && Distance > Found.getBbWidth()) {
                continue;
            }
            if (!Found.fireImmune() && hasLineOfSight(Found)) {
                Found.igniteForSeconds(IgniteSeconds);
            }
        }
    }

    private void maybeKindle(ServerLevel World, Vec3 Mouth, Vec3 Way, double Reach) {
        if (!EventHooks.canEntityGrief(World, this) || random.nextInt(FireChance) != 0) {
            return;
        }
        BlockPos Tip = BlockPos.containing(Mouth.add(Way.scale(Reach)));
        for (BlockPos Cell : List.of(Tip, Tip.below())) {
            if (World.getBlockState(Cell).isAir() && BaseFireBlock.canBePlacedAt(World, Cell, getDirection())) {
                World.setBlock(Cell, BaseFireBlock.getState(World, Cell), Block.UPDATE_ALL_IMMEDIATE);
                return;
            }
        }
    }

    private void consumeFood(ServerLevel World) {
        ItemEntity Drop = EatFood;
        EatFood = null;
        Food = null;
        if (Drop == null || !Drop.isAlive() || !isGreens(Drop.getItem()) || mouthReach(Drop.position()) > EatReach * 2.5) {
            return;
        }
        ItemStack Bite = Drop.getItem().copyWithCount(1);
        ItemStack Left = Drop.getItem().copy();
        Left.shrink(1);
        if (Left.isEmpty()) {
            Drop.discard();
        } else {
            Drop.setItem(Left);
        }
        eaten(World, Bite);
    }

    public void eaten(ServerLevel World, ItemStack Bite) {
        Fed = Math.min(MaxFed, Fed + FedPerGreens);
        feedGrowth();
        Vec3 Mouth = mouth(MouthForward, MouthUp);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Bite), Mouth.x, Mouth.y, Mouth.z, 6, 0.04, 0.03, 0.04, 0.03);
        playLayers(Scp111Registry.EatLayers, 0.6f, 1.15f + random.nextFloat() * 0.2f);
    }

    private void nibbleBite(ServerLevel World) {
        BlockPos Leaf = NibblePos;
        if (Leaf == null) {
            return;
        }
        BlockState State = World.getBlockState(Leaf);
        if (!State.is(BlockTags.LEAVES) || !EventHooks.canEntityGrief(World, this)) {
            stopNibbling();
            return;
        }
        NibbleBites++;
        Fed = Math.min(MaxFed, Fed + FedPerNibble);
        Vec3 Mouth = mouth(MouthForward, MouthUp);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Mouth.x, Mouth.y + 0.05, Mouth.z, 5, 0.06, 0.05, 0.06, 0.02);
        playLayers(Scp111Registry.NibbleLayers, 0.5f, 1.2f + random.nextFloat() * 0.2f);
        if (NibbleBites >= NibbleSteps) {
            World.destroyBlockProgress(getId(), Leaf, -1);
            World.destroyBlock(Leaf, false, this);
            NibblePos = null;
            NibbleBites = 0;
        } else {
            World.destroyBlockProgress(getId(), Leaf, Math.min(9, NibbleBites * 10 / NibbleSteps));
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && isAlive() && level() instanceof ServerLevel && Source.getEntity() instanceof LivingEntity Attacker && Attacker != this
                && Attacker.isAlive() && !Attacker.isSpectator() && distanceTo(Attacker) <= RetaliateRange && JetReady <= 0 && action() != ActionBreathe) {
            stopNibbling();
            beginBreathe(Attacker);
        }
        return Hurt;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (isGreens(Held) && (isHungry() || isBaby())) {
            if (level() instanceof ServerLevel World) {
                eaten(World, Held.copyWithCount(1));
                Held.consume(1, User);
                Vec3 Top = position().add(0.0, getBbHeight() + 0.1, 0.0);
                World.sendParticles(ParticleTypes.HEART, Top.x, Top.y, Top.z, 1, 0.05, 0.02, 0.05, 0.0);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(User, Hand);
    }

    public static void playLayers(ServerLevel World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.NEUTRAL, Volume, Pitch);
        }
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Size = getAgeScale();
        float Shift = 1.0f + (1.0f - Size) * 0.5f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume * (0.6f + 0.4f * Size), Pitch * Shift);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        RandomSource Random = World.getRandom();
        setVariant(Scp111Variant.random(Random));
        setPersistenceRequired();
        Fed = StartFed;
        PuffReady = PuffMin + Random.nextInt(PuffSpread + 1);
        return super.finalizeSpawn(World, Difficulty, Reason, Group);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public void die(DamageSource Source) {
        stopNibbling();
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            Scp111Breath.puff(World, position().add(0.0, getBbHeight() * 0.5, 0.0), new Vec3(0.0, 1.0, 0.0), getVariant(), 5, getAgeScale());
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        stopNibbling();
        super.remove(Reason);
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (!isActing()) {
            playLayers(Scp111Registry.AmbientLayers, 0.4f, 0.95f + random.nextFloat() * 0.2f);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp111Registry.Ambient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp111Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp111Registry.Death.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (1.0f + (1.0f - getAgeScale()) * 0.5f);
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Nullable
    public Scp111Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp111Gait(SnailGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp111Gait Current = gait();
        return Current != null && Current.striding();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp111Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putString("Variant", getVariant().getSerializedName());
        Tag.putInt("Age", Age);
        Tag.putInt("Fed", Fed);
        Tag.putInt("LayCooldown", LayReady);
        Tag.putInt("Laid", Laid);
        if (LeaveFrom != null && LeaveLeft > 0) {
            Tag.put("LeaveFrom", NbtUtils.writeBlockPos(LeaveFrom));
            Tag.putInt("LeaveTicks", LeaveLeft);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setVariant(Scp111Variant.byName(Tag.getString("Variant")));
        setAge(Tag.getInt("Age"));
        Fed = Tag.contains("Fed") ? Mth.clamp(Tag.getInt("Fed"), 0, MaxFed) : StartFed;
        LayReady = Tag.getInt("LayCooldown");
        Laid = Tag.getInt("Laid");
        LeaveLeft = Tag.getInt("LeaveTicks");
        LeaveFrom = LeaveLeft > 0 ? NbtUtils.readBlockPos(Tag, "LeaveFrom").orElse(null) : null;
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp111GaitController(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp111GaitController.blendFor(Current));
            if (Current == ActionPuff) {
                return Event.setAndContinue(PuffPlay);
            }
            if (Current == ActionBreathe) {
                return Event.setAndContinue(BreathePlay);
            }
            if (Current == ActionEat || Current == ActionNibble) {
                return Event.setAndContinue(EatPlay);
            }
            if (Current == ActionLay) {
                return Event.setAndContinue(LayPlay);
            }
            if (isStriding()) {
                return Event.setAndContinue(GlideLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
