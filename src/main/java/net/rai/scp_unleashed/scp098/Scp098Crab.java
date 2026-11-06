package net.rai.scp_unleashed.scp098;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;

public class Scp098Crab extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.9f;
    public static final float Height = 0.8f;
    public static final float EyeHeight = 0.62f;
    public static final double Health = 14.0;
    public static final double Armour = 4.0;
    public static final double Damage = 3.0;
    public static final double Speed = 0.25;
    public static final double WanderModifier = 0.6;
    public static final double HuntModifier = 1.1;
    public static final double RushModifier = 1.3;
    public static final double FollowRange = 20.0;
    public static final double SnipReach = 1.0;
    public static final int SnipTicks = 10;
    public static final int SnipHitTick = 5;
    public static final int SnipCooldown = 16;
    public static final int EatTicks = 20;
    public static final int EatPullStart = 4;
    public static final int EatConsumeTick = 12;
    public static final int EmergeTicks = 24;
    public static final int BurrowTicks = 24;
    public static final int SurfaceTicks = 24;
    public static final double MouthForward = 0.42;
    public static final double MouthUp = 0.3;
    public static final double EatReach = 0.85;
    public static final double MorselRange = 8.0;
    public static final int FeedPatience = 100;
    public static final int ForageMin = 400;
    public static final int ForageMax = 900;
    public static final int GuardTicks = 2400;
    public static final double ForageRadius = 10.0;
    public static final double HomeRange = 32.0;
    public static final double AdoptRange = 48.0;
    public static final int AdoptInterval = 100;
    public static final int LostPreyTicks = 60;
    public static final int HuntPatience = 1200;
    public static final double LeapForward = 0.18;
    public static final double LeapUp = 0.15;
    public static final int FoodPerNutrition = 4;
    public static final int FoodPerScrap = 2;
    public static final int AmbientInterval = 400;
    public static final float AmbientChance = 0.4f;
    public static final String MorselKey = "Scp098Morsel";
    public static final float PaleBelow = 0.34f;
    public static final float FadedBelow = 0.67f;
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final byte ActionBurrow = 2;
    public static final byte ActionSurface = 3;
    public static final byte ActionSnip = 4;
    public static final byte ActionEat = 5;
    public static final byte ModeRush = 0;
    public static final byte ModeHunt = 1;
    public static final byte ModeFeed = 2;
    public static final byte ModeReturn = 3;
    public static final byte ModeForage = 4;
    public static final byte ModeStray = 5;
    public static final Scp098Gait.Shape CrabGait = new Scp098Gait.Shape(
            new Scp098Gait.Profile(10.0, 0.0469, 0.5, 2.6, new double[]{0.485, 0.986}, new float[]{1.0f, 1.0f}),
            new Scp098Gait.Profile(5.0, 0.15, 0.6, 2.2, new double[]{0.503, 0.986}, new float[]{1.0f, 1.0f}),
            0.11, 0.085, 1, Scp098Registry.Step, Scp098Registry.StepClick, 0.35f, 0.25f, 1.0f, 16.0);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp098Crab.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp098Crab.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RipenessData = SynchedEntityData.defineId(Scp098Crab.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> ModeData = SynchedEntityData.defineId(Scp098Crab.class, EntityDataSerializers.BYTE);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation BurrowPlay = RawAnimation.begin().thenPlayAndHold("burrow");
    private static final RawAnimation SurfacePlay = RawAnimation.begin().thenPlayAndHold("surface");
    private static final RawAnimation SnipPlay = RawAnimation.begin().thenPlayAndHold("snip");
    private static final RawAnimation EatPlay = RawAnimation.begin().thenPlayAndHold("eat");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int ActionTicks;
    @Nullable
    private LivingEntity StrikeTarget;
    @Nullable
    private ItemEntity EatMorsel;
    @Nullable
    private BlockPos Home;
    private int Belly;
    private int ModeTicks;
    private int SnipReady;
    private int Hungry;
    private int Lost;
    private float RenderPartial;
    private int TickOrigin;
    @Nullable
    private Scp098Gait Gait;

    public Scp098Crab(EntityType<? extends Scp098Crab> Type, Level World) {
        super(Type, World);
        xpReward = 3;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.ARMOR, Armour).add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(RipenessData, 1.0f);
        Builder.define(ModeData, ModeStray);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp098Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp098Goals.Strike(this));
        goalSelector.addGoal(3, new Scp098Goals.Feed(this));
        goalSelector.addGoal(4, new Scp098Goals.GoHome(this));
        goalSelector.addGoal(5, new Scp098Goals.Roam(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new Scp098Goals.Retaliate(this));
        targetSelector.addGoal(2, new Scp098Goals.Prey(this));
    }

    public static boolean isPrey(LivingEntity Found) {
        return Found.isAlive() && isPreyKind(Found);
    }

    public static boolean isPreyKind(LivingEntity Found) {
        if (!(Found instanceof Animal) || Found instanceof Enemy || ControlBodies.isPersonLike(Found)) {
            return false;
        }
        if (Found instanceof TamableAnimal Pet && Pet.isTame()) {
            return false;
        }
        return !(Found instanceof AbstractHorse Horse) || !Horse.isTamed();
    }

    public static int valueOf(ItemStack Stack) {
        FoodProperties Food = Stack.get(DataComponents.FOOD);
        int Each = Food != null ? Math.max(1, Food.nutrition()) * FoodPerNutrition : FoodPerScrap;
        return Each * Stack.getCount();
    }

    public static boolean isMorsel(ItemEntity Drop) {
        return Drop.isAlive() && Drop.getPersistentData().getBoolean(MorselKey);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean locksMovement() {
        byte Current = action();
        return Current != ActionNone && Current != ActionSnip;
    }

    public boolean isEmerging() {
        return action() == ActionEmerge;
    }

    public byte mode() {
        return entityData.get(ModeData);
    }

    public void setMode(byte Next) {
        if (mode() != Next) {
            entityData.set(ModeData, Next);
            ModeTicks = 0;
            Lost = 0;
            if (Next != ModeForage) {
                clearRestriction();
            }
        }
    }

    public float ripeness() {
        return entityData.get(RipenessData);
    }

    public void setRipeness(float Value) {
        entityData.set(RipenessData, Mth.clamp(Value, 0.0f, 1.0f));
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public void setHome(@Nullable BlockPos Pos) {
        Home = Pos == null ? null : Pos.immutable();
    }

    public int belly() {
        return Belly;
    }

    public int modeTicks() {
        return ModeTicks;
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionEmerge -> "emerge";
            case ActionBurrow -> "burrow";
            case ActionSurface -> "surface";
            case ActionSnip -> "snip";
            case ActionEat -> "eat";
            default -> null;
        };
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind != ActionSnip && Kind != ActionNone) {
            getNavigation().stop();
        }
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

    public boolean canSnip() {
        return SnipReady <= 0 && !isActing();
    }

    public void beginSnip(LivingEntity Target) {
        StrikeTarget = Target;
        SnipReady = SnipCooldown;
        setAction(ActionSnip, SnipTicks);
    }

    public void beginEat(ItemEntity Drop) {
        EatMorsel = Drop;
        Drop.setPickUpDelay(EatTicks + 2);
        faceToward(Drop.getX(), Drop.getZ());
        setAction(ActionEat, EatTicks);
    }

    public void beginBurrow() {
        faceToward(getX() + getLookAngle().x, getZ() + getLookAngle().z);
        setAction(ActionBurrow, BurrowTicks);
        playLayers(Scp098Registry.BurrowLayers, 0.9f, 0.95f + random.nextFloat() * 0.1f);
    }

    public void faceToward(double X, double Z) {
        float Yaw = (float) (Mth.atan2(Z - getZ(), X - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Yaw);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
    }

    public Vec3 mouth(double Forward, double Up) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        return position().add(Facing.x * Forward, Up, Facing.z * Forward);
    }

    public double mouthReach(ItemEntity Drop) {
        Vec3 Mouth = mouth(MouthForward, 0.0);
        double Dx = Drop.getX() - Mouth.x;
        double Dz = Drop.getZ() - Mouth.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public double reachTo(LivingEntity Target) {
        return Math.sqrt(distanceToSqr(Target)) - Target.getBbWidth() * 0.5 - getBbWidth() * 0.5;
    }

    @Nullable
    public Scp098HiveBlockEntity hive() {
        if (Home == null || !level().isLoaded(Home)) {
            return null;
        }
        return level().getBlockEntity(Home) instanceof Scp098HiveBlockEntity Found ? Found : null;
    }

    public boolean homeExists() {
        if (Home == null || !(level() instanceof ServerLevel World)) {
            return false;
        }
        return Scp098Hives.get(World).contains(Home);
    }

    public double homeDistance() {
        return Home == null ? Double.MAX_VALUE : Math.sqrt(distanceToSqr(Vec3.atBottomCenterOf(Home.above())));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp098Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
            return;
        }
        if (!isAlive()) {
            return;
        }
        ModeTicks++;
        SnipReady = Math.max(0, SnipReady - 1);
        think();
        if (ActionTicks > 0) {
            byte Current = action();
            actionTick(Current, (int) actionElapsed());
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved()) {
                finishAction(Current);
            }
        }
    }

    private void think() {
        byte Current = mode();
        if (Home != null && tickCount % 20 == 0 && !homeExists()) {
            Home = null;
            setMode(ModeStray);
            return;
        }
        if (Current == ModeStray) {
            if (tickCount % AdoptInterval == 0 && level() instanceof ServerLevel World) {
                BlockPos Found = Scp098Hives.get(World).nearest(blockPosition(), AdoptRange);
                if (Found != null) {
                    Home = Found;
                    setMode(ModeReturn);
                }
            }
            return;
        }
        if (Home == null && (Current == ModeRush || Current == ModeReturn)) {
            setMode(ModeStray);
            return;
        }
        LivingEntity Target = getTarget();
        boolean Chasing = Target != null && Target.isAlive();
        if (Chasing && Home != null && homeDistance() > HomeRange && isPrey(Target)) {
            setTarget(null);
            setMode(ModeReturn);
            return;
        }
        if (Current == ModeHunt) {
            Lost = Chasing ? 0 : Lost + 1;
            if (Lost > LostPreyTicks || ModeTicks > HuntPatience) {
                setTarget(null);
                setMode(Home == null ? ModeStray : ModeReturn);
            }
        } else if (Current == ModeFeed) {
            Hungry++;
            if (Hungry > FeedPatience) {
                setMode(ModeReturn);
            }
        } else if (Current == ModeForage && !Chasing && ModeTicks > forageLength()) {
            setMode(ModeReturn);
        }
    }

    private int forageLength() {
        return ForageMin + Math.floorMod(getId() * 37, ForageMax - ForageMin + 1);
    }

    public void sawMorsel() {
        Hungry = 0;
    }

    public void onPreyKilled() {
        setTarget(null);
        Hungry = 0;
        setMode(ModeFeed);
    }

    private void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionSnip) {
            LivingEntity Target = StrikeTarget;
            if (Target != null && Target.isAlive()) {
                getLookControl().setLookAt(Target, 40.0f, 40.0f);
                faceToward(Target.getX(), Target.getZ());
            }
            if (Elapsed == SnipHitTick) {
                snipHit(Target);
            }
            return;
        }
        if (Kind != ActionEmerge || onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
        if (Kind == ActionEat) {
            if (Elapsed >= EatPullStart && Elapsed < EatConsumeTick) {
                pullMorsel(Elapsed);
            } else if (Elapsed == EatConsumeTick) {
                consumeMorsel();
            }
        } else if (Kind == ActionBurrow && Elapsed % 4 == 0 && level() instanceof ServerLevel World) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), getX(), getY() + 0.1, getZ(), 5, 0.35, 0.05, 0.35, 0.02);
        } else if (Kind == ActionSurface && Elapsed % 4 == 0 && Elapsed < 18 && level() instanceof ServerLevel World) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), getX(), getY() + 0.1, getZ(), 5, 0.35, 0.05, 0.35, 0.02);
        }
    }

    private void finishAction(byte Kind) {
        entityData.set(ActionData, ActionNone);
        StrikeTarget = null;
        EatMorsel = null;
        if (Kind == ActionBurrow) {
            enterHive();
        } else if (Kind == ActionEmerge && mode() == ModeRush && Home == null) {
            setMode(ModeStray);
        }
    }

    private void enterHive() {
        Scp098HiveBlockEntity Hive = hive();
        if (Hive == null) {
            Home = null;
            setMode(ModeStray);
            return;
        }
        Hive.feed(Belly);
        Belly = 0;
        if (Hive.admit(ripeness(), getHealth())) {
            discard();
            return;
        }
        setMode(ModeForage);
        ModeTicks = -GuardTicks;
        restrictTo(Home, (int) ForageRadius);
    }

    public boolean readyToBurrow() {
        Scp098HiveBlockEntity Hive = hive();
        if (Hive == null || isActing()) {
            return false;
        }
        if (!Hive.hasRoom()) {
            Hive.feed(Belly);
            Belly = 0;
            setMode(ModeForage);
            ModeTicks = -GuardTicks;
            restrictTo(Home, (int) ForageRadius);
            return false;
        }
        return true;
    }

    private void snipHit(@Nullable LivingEntity Target) {
        playLayers(Scp098Registry.SnipLayers, 0.9f, 0.95f + random.nextFloat() * 0.15f);
        if (Target == null || !Target.isAlive() || reachTo(Target) > SnipReach + 0.3 || !hasLineOfSight(Target)) {
            return;
        }
        doHurtTarget(Target);
    }

    private void pullMorsel(int Elapsed) {
        ItemEntity Drop = EatMorsel;
        if (Drop == null || !Drop.isAlive()) {
            return;
        }
        Vec3 Mouth = mouth(MouthForward, 0.05);
        double Share = 1.0 / Math.max(1, EatConsumeTick - Elapsed);
        Vec3 Next = Drop.position().add(Mouth.subtract(Drop.position()).scale(Share));
        Drop.setPos(Next.x, Next.y, Next.z);
        Drop.setDeltaMovement(Vec3.ZERO);
        Drop.setPickUpDelay(EatTicks);
    }

    private void consumeMorsel() {
        ItemEntity Drop = EatMorsel;
        EatMorsel = null;
        if (Drop == null || !isMorsel(Drop) || !(level() instanceof ServerLevel World)) {
            return;
        }
        ItemStack Stack = Drop.getItem().copy();
        Belly += valueOf(Stack);
        Drop.discard();
        Hungry = 0;
        Vec3 Mouth = mouth(MouthForward, MouthUp);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Stack.copyWithCount(1)), Mouth.x, Mouth.y, Mouth.z, 10, 0.1, 0.06, 0.1, 0.05);
        playLayers(Scp098Registry.NibbleLayers, 0.8f, 0.95f + random.nextFloat() * 0.15f);
    }

    @Nullable
    public ItemEntity nearestMorsel() {
        List<ItemEntity> Found = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(MorselRange, 3.0, MorselRange), Scp098Crab::isMorsel);
        return Found.stream().min(Comparator.comparingDouble(Drop -> Drop.distanceToSqr(this))).orElse(null);
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch);
        }
    }

    @Nullable
    public static Scp098Crab emergeFrom(ServerLevel World, LivingEntity Host, Vec3 Head, BlockPos Hive) {
        Scp098Crab Crab = Scp098Registry.Crab.get().create(World);
        if (Crab == null) {
            return null;
        }
        float Yaw = Host.getYHeadRot();
        Vec3 Facing = Vec3.directionFromRotation(0.0f, Yaw);
        Vec3[] Spots = {new Vec3(Head.x, Head.y - 0.2, Head.z), Host.position().add(Facing.scale(0.9)), Host.position().subtract(Facing.scale(0.9)), Host.position()};
        Vec3 Chosen = Spots[Spots.length - 1];
        for (Vec3 Spot : Spots) {
            Crab.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
            if (World.noCollision(Crab)) {
                Chosen = Spot;
                break;
            }
        }
        Crab.moveTo(Chosen.x, Chosen.y, Chosen.z, Yaw, 0.0f);
        Crab.setYHeadRot(Yaw);
        Crab.setYBodyRot(Yaw);
        Crab.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Chosen)), MobSpawnType.EVENT, null);
        Crab.setRipeness(0.0f);
        Crab.setHome(Hive);
        Crab.setMode(ModeRush);
        Crab.setAction(ActionEmerge, EmergeTicks);
        Crab.setDeltaMovement(Facing.x * LeapForward, LeapUp, Facing.z * LeapForward);
        return World.addFreshEntity(Crab) ? Crab : null;
    }

    @Nullable
    public static Scp098Crab surface(ServerLevel World, BlockPos Hive, Scp098HiveBlockEntity.Occupant Resident, @Nullable LivingEntity Prey, @Nullable LivingEntity Grudge,
                                     boolean Collapsed) {
        Scp098Crab Crab = Scp098Registry.Crab.get().create(World);
        if (Crab == null) {
            return null;
        }
        float Yaw = World.getRandom().nextFloat() * 360.0f;
        Vec3 Chosen = null;
        BlockPos Start = Collapsed ? Hive : Hive.above();
        Vec3[] Spots = {Vec3.atBottomCenterOf(Start), Vec3.atBottomCenterOf(Hive.relative(Direction.NORTH)), Vec3.atBottomCenterOf(Hive.relative(Direction.SOUTH)),
                Vec3.atBottomCenterOf(Hive.relative(Direction.EAST)), Vec3.atBottomCenterOf(Hive.relative(Direction.WEST)), Vec3.atBottomCenterOf(Hive.above(2))};
        for (Vec3 Spot : Spots) {
            Crab.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
            if (World.noCollision(Crab)) {
                Chosen = Spot;
                break;
            }
        }
        if (Chosen == null) {
            return null;
        }
        Crab.setYHeadRot(Yaw);
        Crab.setYBodyRot(Yaw);
        Crab.finalizeSpawn(World, World.getCurrentDifficultyAt(Hive), MobSpawnType.EVENT, null);
        Crab.setRipeness(Resident.Ripeness());
        Crab.setHealth(Mth.clamp(Resident.Health(), 1.0f, Crab.getMaxHealth()));
        Crab.setHome(Collapsed ? null : Hive);
        if (Prey != null) {
            Crab.setMode(ModeHunt);
            Crab.setTarget(Prey);
        } else if (Grudge != null) {
            Crab.setMode(Collapsed ? ModeStray : ModeReturn);
            Crab.setTarget(Grudge);
            Crab.setLastHurtByMob(Grudge);
        } else {
            Crab.setMode(Collapsed ? ModeStray : ModeForage);
            if (!Collapsed) {
                Crab.restrictTo(Hive, (int) ForageRadius);
            }
        }
        if (!Collapsed) {
            Crab.setAction(ActionSurface, SurfaceTicks);
            Crab.playLayers(Scp098Registry.BurrowLayers, 0.9f, 1.05f);
        }
        return World.addFreshEntity(Crab) ? Crab : null;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Data) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Data);
        if (Reason != MobSpawnType.EVENT) {
            setMode(ModeStray);
            setRipeness(1.0f);
        }
        return Result;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (action() == ActionBurrow || action() == ActionSurface) {
            Amount *= 0.5f;
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), getX(), getY() + 0.4, getZ(), 18, 0.3, 0.2, 0.3, 0.05);
            playSound(Scp098Registry.DeathCrack.get(), 1.0f, 0.9f + random.nextFloat() * 0.1f);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (!isActing() && random.nextFloat() < AmbientChance) {
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp098Registry.Ambient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp098Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp098Registry.Death.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(0.6);
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Nullable
    public Scp098Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp098Gait(CrabGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp098Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        Scp098Gait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp098Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (Home != null) {
            Saved.put("Home", NbtUtils.writeBlockPos(Home));
        }
        Saved.putByte("Mode", mode());
        Saved.putFloat("Ripeness", ripeness());
        Saved.putInt("Belly", Belly);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Home = NbtUtils.readBlockPos(Saved, "Home").orElse(null);
        entityData.set(ModeData, Saved.contains("Mode") ? Saved.getByte("Mode") : ModeStray);
        if (Saved.contains("Ripeness")) {
            setRipeness(Saved.getFloat("Ripeness"));
        }
        Belly = Math.max(0, Saved.getInt("Belly"));
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        if (mode() == ModeHunt || mode() == ModeFeed) {
            entityData.set(ModeData, ModeReturn);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp098GaitController(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp098GaitController.blendFor(Current));
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionBurrow) {
                return Event.setAndContinue(BurrowPlay);
            }
            if (Current == ActionSurface) {
                return Event.setAndContinue(SurfacePlay);
            }
            if (Current == ActionSnip) {
                return Event.setAndContinue(SnipPlay);
            }
            if (Current == ActionEat) {
                return Event.setAndContinue(EatPlay);
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
}
