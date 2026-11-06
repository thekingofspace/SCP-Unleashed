package net.rai.scp_unleashed.scp075;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Scp075Snail extends Monster implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.9f;
    public static final float Height = 0.75f;
    public static final float EyeHeight = 0.45f;
    public static final double Health = 24.0;
    public static final double Armour = 4.0;
    public static final double KnockbackResistance = 0.6;
    public static final double Speed = 0.09;
    public static final double ChaseModifier = 1.0;
    public static final double WanderModifier = 0.8;
    public static final double BabyChaseModifier = 1.25;
    public static final double DeadZone = 0.003;
    public static final double JuvenilePace = 0.5;
    public static final ResourceLocation JuvenilePaceId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp075_juvenile_pace");
    public static final double FollowRange = 24.0;
    public static final double HoldRange = 12.0;
    public static final double SpitMin = 2.2;
    public static final double SwipeReach = 2.2;
    public static final double SwipeDamage = 3.0;
    public static final int SpitTicks = 24;
    public static final int SpitReleaseTick = 13;
    public static final int SpitCooldown = 60;
    public static final int SpitCooldownSpread = 30;
    public static final int SwipeTicks = 14;
    public static final int SwipeHitTick = 7;
    public static final int SwipeCooldown = 30;
    public static final float AimTolerance = 20.0f;
    public static final float TurnSpeed = 9.0f;
    public static final int EatTicks = 32;
    public static final int EatPullStart = 6;
    public static final int EatConsumeTick = 20;
    public static final int DigestTicks = 100;
    public static final int ChurnInterval = 18;
    public static final int BirthTicks = 24;
    public static final int BirthReleaseTick = 13;
    public static final double BirthPush = 0.2;
    public static final double BirthLift = 0.32;
    public static final int BreedCooldown = 6000;
    public static final double SeekRange = 8.0;
    public static final double EatReach = 0.8;
    public static final int ShellScanInterval = 10;
    public static final int SeekPatience = 300;
    public static final int ShellIgnoreTicks = 1200;
    public static final int BreedCap = 8;
    public static final double BreedArea = 32.0;
    public static final int CrowdInterval = 40;
    public static final int GrowTicks = 24000;
    public static final float BabyScale = 0.35f;
    public static final int ScaleSteps = 32;
    public static final int AgeSync = 20;
    public static final double MouthForward = 0.26;
    public static final double MouthUp = 0.23;
    public static final double SpitMouthForward = 0.29;
    public static final double SpitMouthUp = 0.22;
    public static final int AmbientInterval = 220;
    public static final byte ActionNone = 0;
    public static final byte ActionSpit = 1;
    public static final byte ActionSwipe = 2;
    public static final byte ActionEat = 3;
    public static final byte ActionBirth = 4;
    public static final Scp075Gait.Shape SnailGait = new Scp075Gait.Shape(
            new Scp075Gait.Profile(24.0, 0.0126, 0.4, 3.0, new double[]{0.985}, new float[]{1.0f}),
            1, Scp075Registry.Step, Scp075Registry.StepClick, 0.5f, 0.32f, 1.0f, 18.0);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp075Snail.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp075Snail.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HuntingData = SynchedEntityData.defineId(Scp075Snail.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DigestingData = SynchedEntityData.defineId(Scp075Snail.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AgeData = SynchedEntityData.defineId(Scp075Snail.class, EntityDataSerializers.INT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CrawlLoop = RawAnimation.begin().thenLoop("crawl");
    private static final RawAnimation ChurnLoop = RawAnimation.begin().thenLoop("churn");
    private static final RawAnimation SpitPlay = RawAnimation.begin().thenPlayAndHold("spit");
    private static final RawAnimation SwipePlay = RawAnimation.begin().thenPlayAndHold("swipe");
    private static final RawAnimation EatPlay = RawAnimation.begin().thenPlayAndHold("eat");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int ActionTicks;
    @Nullable
    private LivingEntity StrikeTarget;
    @Nullable
    private ItemEntity Shell;
    @Nullable
    private ItemEntity EatShell;
    private int SpitReady;
    private int SwipeReady;
    private int BreedReady;
    private int DigestLeft;
    private int SeekTicks;
    private int IgnoreLeft;
    @Nullable
    private UUID IgnoredShell;
    private int Age;
    private boolean Crowded;
    private float RenderPartial;
    private int TickOrigin;
    @Nullable
    private Scp075Gait Gait;
    private Vec3 Carried = Vec3.ZERO;

    public Scp075Snail(EntityType<? extends Scp075Snail> Type, Level World) {
        super(Type, World);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, SwipeDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance).add(Attributes.FOLLOW_RANGE, FollowRange)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.ATTACK_KNOCKBACK, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(HuntingData, false);
        Builder.define(DigestingData, false);
        Builder.define(AgeData, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp075Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp075Goals.SeekShell(this));
        goalSelector.addGoal(3, new Scp075Goals.Engage(this));
        goalSelector.addGoal(4, new Scp075Goals.Approach(this));
        goalSelector.addGoal(5, new Scp075Goals.FollowAdult(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new Scp075Goals.Defend(this));
        targetSelector.addGoal(2, new Scp075Goals.Hunt<>(this, Player.class, Scp075Snail::isQuarry));
        targetSelector.addGoal(3, new Scp075Goals.Hunt<>(this, LivingEntity.class, Found -> ControlBodies.isPersonLike(Found) && isQuarry(Found)));
    }

    public static boolean isQuarry(LivingEntity Found) {
        if (Found instanceof Scp075Snail || !Found.isAlive() || Found.isSpectator() || !Targeting.isTargetable(Found)) {
            return false;
        }
        return !(Found instanceof Player User) || !User.isCreative();
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isHunting() {
        return entityData.get(HuntingData);
    }

    public boolean isDigesting() {
        return entityData.get(DigestingData);
    }

    public int getAge() {
        return entityData.get(AgeData);
    }

    public void setAge(int Value) {
        Age = Value;
        entityData.set(AgeData, Value);
        refreshPace();
    }

    private void refreshPace() {
        if (level().isClientSide) {
            return;
        }
        AttributeInstance Pace = getAttribute(Attributes.MOVEMENT_SPEED);
        if (Pace == null) {
            return;
        }
        float Size = getAgeScale();
        if (Size >= 1.0f) {
            Pace.removeModifier(JuvenilePaceId);
            return;
        }
        Pace.addOrUpdateTransientModifier(new AttributeModifier(JuvenilePaceId, Math.pow(Size, JuvenilePace) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
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

    public float growth() {
        return 1.0f - Math.min(1.0f, Math.max(0, -getAge()) / (float) GrowTicks);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (AgeData.equals(Key)) {
            refreshDimensions();
        }
    }

    public boolean locksMovement() {
        byte Current = action();
        return Current != ActionNone || isDigesting();
    }

    public boolean isFeeding() {
        byte Current = action();
        return Shell != null || Current == ActionEat || Current == ActionBirth || DigestLeft > 0;
    }

    public boolean canHunt() {
        return isAlive() && !isBaby() && !isFeeding();
    }

    public boolean canFeed() {
        byte Current = action();
        return isAlive() && !isBaby() && BreedReady <= 0 && DigestLeft <= 0 && Current != ActionEat && Current != ActionBirth && !Crowded;
    }

    @Nullable
    public ItemEntity noticedShell() {
        return Shell != null && Shell.isAlive() ? Shell : null;
    }

    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionSpit, ActionBirth -> "spit";
            case ActionSwipe -> "swipe";
            case ActionEat -> "eat";
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

    public boolean canSpit() {
        return SpitReady <= 0 && !isActing();
    }

    public boolean canSwipe() {
        return SwipeReady <= 0 && !isActing();
    }

    public double reachTo(LivingEntity Target) {
        return Math.sqrt(distanceToSqr(Target)) - Target.getBbWidth() * 0.5;
    }

    public float yawTo(double X, double Z) {
        return (float) (Mth.atan2(Z - getZ(), X - getX()) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public float facingError(LivingEntity Target) {
        return Math.abs(Mth.wrapDegrees(yawTo(Target.getX(), Target.getZ()) - getYRot()));
    }

    public void turnToward(double X, double Z, float Step) {
        float Yaw = Mth.approachDegrees(getYRot(), yawTo(X, Z), Step);
        setYRot(Yaw);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
    }

    public Vec3 mouth(double Forward, double Up) {
        float Scale = getAgeScale();
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        return position().add(Facing.x * Forward * Scale, Up * Scale, Facing.z * Forward * Scale);
    }

    public double mouthReach(ItemEntity Drop) {
        Vec3 Mouth = mouth(MouthForward, 0.0);
        double Dx = Drop.getX() - Mouth.x;
        double Dz = Drop.getZ() - Mouth.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public void beginSpit(LivingEntity Target) {
        StrikeTarget = Target;
        SpitReady = SpitCooldown + random.nextInt(SpitCooldownSpread + 1);
        setAction(ActionSpit, SpitTicks);
        playLayers(Scp075Registry.RearLayers, 0.9f, 0.95f + random.nextFloat() * 0.1f);
    }

    public void beginSwipe(LivingEntity Target) {
        StrikeTarget = Target;
        SwipeReady = SwipeCooldown;
        setAction(ActionSwipe, SwipeTicks);
    }

    public void beginEat(ItemEntity Drop) {
        EatShell = Drop;
        Drop.setPickUpDelay(EatTicks);
        float Yaw = yawTo(Drop.getX(), Drop.getZ());
        setYRot(Yaw);
        yBodyRot = Yaw;
        yHeadRot = Yaw;
        setAction(ActionEat, EatTicks);
    }

    private void beginBirth() {
        setAction(ActionBirth, BirthTicks);
        playLayers(Scp075Registry.RearLayers, 0.8f, 1.1f);
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
        growUp();
        SpitReady = Math.max(0, SpitReady - 1);
        SwipeReady = Math.max(0, SwipeReady - 1);
        BreedReady = Math.max(0, BreedReady - 1);
        if (tickCount % CrowdInterval == 0) {
            Crowded = !isBaby() && level().getEntitiesOfClass(Scp075Snail.class, getBoundingBox().inflate(BreedArea), Scp075Snail::isAlive).size() >= BreedCap;
        }
        if (IgnoreLeft > 0 && --IgnoreLeft == 0) {
            IgnoredShell = null;
        }
        if (tickCount % ShellScanInterval == 0) {
            ItemEntity Previous = Shell;
            Shell = canFeed() ? findShell() : null;
            SeekTicks = Shell != null && Shell == Previous && action() != ActionEat ? SeekTicks + ShellScanInterval : 0;
            if (Shell != null && SeekTicks > SeekPatience) {
                IgnoredShell = Shell.getUUID();
                IgnoreLeft = ShellIgnoreTicks;
                Shell = null;
                SeekTicks = 0;
            }
        } else if (Shell != null && (!Shell.isAlive() || !canFeed())) {
            Shell = null;
        }
        if (!canHunt() && getTarget() != null) {
            setTarget(null);
        }
        boolean Hunting = canHunt() && getTarget() != null && getTarget().isAlive();
        if (entityData.get(HuntingData) != Hunting) {
            entityData.set(HuntingData, Hunting);
        }
        digest();
        if (ActionTicks > 0) {
            byte Current = action();
            actionTick(Current, (int) actionElapsed());
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved()) {
                entityData.set(ActionData, ActionNone);
                StrikeTarget = null;
                EatShell = null;
            }
        }
    }

    private void clientTick() {
        Scp075Gait Current = gait();
        if (Current != null) {
            Current.tick(this);
        }
    }

    private void growUp() {
        if (Age >= 0) {
            return;
        }
        Age++;
        if (Age == 0 || Age % AgeSync == 0) {
            entityData.set(AgeData, Age);
            refreshPace();
        }
    }

    @Nullable
    private ItemEntity findShell() {
        List<ItemEntity> Found = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(SeekRange, 3.0, SeekRange),
                Drop -> Drop.isAlive() && Drop.getItem().is(Items.NAUTILUS_SHELL) && Drop.distanceToSqr(this) <= SeekRange * SeekRange && !Drop.getUUID().equals(IgnoredShell));
        return Found.stream().min(Comparator.comparingDouble(Drop -> Drop.distanceToSqr(this))).orElse(null);
    }

    private void digest() {
        if (DigestLeft <= 0 || isActing()) {
            return;
        }
        DigestLeft--;
        if (DigestLeft % ChurnInterval == 0) {
            playLayers(Scp075Registry.ChurnLayers, 0.6f, 0.85f + random.nextFloat() * 0.2f);
            if (level() instanceof ServerLevel World) {
                Vec3 Spot = mouth(0.0, 0.45);
                World.sendParticles(Scp075Registry.Drip.get(), Spot.x, Spot.y, Spot.z, 2, 0.2, 0.15, 0.2, 0.01);
            }
        }
        if (DigestLeft == 0) {
            entityData.set(DigestingData, false);
            beginBirth();
        }
    }

    private void actionTick(byte Kind, int Elapsed) {
        LivingEntity Target = StrikeTarget;
        if (Kind == ActionSpit || Kind == ActionSwipe) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Target != null && Target.isAlive()) {
                getLookControl().setLookAt(Target, 30.0f, 30.0f);
                turnToward(Target.getX(), Target.getZ(), TurnSpeed);
            }
            if (Kind == ActionSpit && Elapsed == SpitReleaseTick) {
                release(Target);
            } else if (Kind == ActionSwipe && Elapsed == SwipeHitTick) {
                swipeHit(Target);
            }
        } else if (Kind == ActionEat) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Elapsed >= EatPullStart && Elapsed < EatConsumeTick) {
                pullShell(Elapsed);
            } else if (Elapsed == EatConsumeTick) {
                consumeShell();
            }
        } else if (Kind == ActionBirth) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Elapsed == BirthReleaseTick) {
                spawnBaby();
            }
        } else {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
    }

    private void release(@Nullable LivingEntity Target) {
        if (Target == null || !Target.isAlive() || !(level() instanceof ServerLevel World)) {
            return;
        }
        Scp075Glob.fire(World, this, Target);
        playLayers(Scp075Registry.SpitLayers, 1.0f, 0.9f + random.nextFloat() * 0.15f);
    }

    private void swipeHit(@Nullable LivingEntity Target) {
        playLayers(Scp075Registry.SwipeLayers, 0.9f, 0.95f + random.nextFloat() * 0.1f);
        if (Target == null || !Target.isAlive() || reachTo(Target) > SwipeReach + 0.3 || !hasLineOfSight(Target)) {
            return;
        }
        doHurtTarget(Target);
    }

    private void pullShell(int Elapsed) {
        ItemEntity Drop = EatShell;
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

    private void consumeShell() {
        ItemEntity Drop = EatShell;
        EatShell = null;
        Shell = null;
        if (Drop == null || !Drop.isAlive() || !Drop.getItem().is(Items.NAUTILUS_SHELL) || !(level() instanceof ServerLevel World)) {
            return;
        }
        ItemStack Stack = Drop.getItem().copy();
        Stack.shrink(1);
        if (Stack.isEmpty()) {
            Drop.discard();
        } else {
            Drop.setItem(Stack);
        }
        DigestLeft = DigestTicks;
        entityData.set(DigestingData, true);
        setPersistenceRequired();
        Vec3 Mouth = mouth(MouthForward, MouthUp * 0.5);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.NAUTILUS_SHELL)), Mouth.x, Mouth.y, Mouth.z, 14, 0.12, 0.08, 0.12, 0.06);
        World.sendParticles(Scp075Registry.Drip.get(), Mouth.x, Mouth.y, Mouth.z, 6, 0.15, 0.08, 0.15, 0.02);
        playLayers(Scp075Registry.EatLayers, 0.9f, 0.85f + random.nextFloat() * 0.15f);
    }

    private void spawnBaby() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Scp075Snail Baby = Scp075Registry.Snail.get().create(World);
        BreedReady = BreedCooldown;
        if (Baby == null) {
            return;
        }
        Vec3 Mouth = mouth(SpitMouthForward, SpitMouthUp);
        float Yaw = yBodyRot;
        Baby.setAge(-GrowTicks);
        Baby.moveTo(Mouth.x, Mouth.y, Mouth.z, Yaw, 0.0f);
        Baby.setYBodyRot(Yaw);
        Baby.setYHeadRot(Yaw);
        if (!World.noCollision(Baby)) {
            Baby.moveTo(getX(), getY() + 0.1, getZ(), Yaw, 0.0f);
        }
        Baby.finalizeSpawn(World, World.getCurrentDifficultyAt(blockPosition()), MobSpawnType.BREEDING, null);
        Vec3 Facing = Vec3.directionFromRotation(0.0f, Yaw);
        Baby.setDeltaMovement(Facing.x * BirthPush, BirthLift, Facing.z * BirthPush);
        Baby.setPersistenceRequired();
        if (!World.addFreshEntity(Baby)) {
            return;
        }
        World.sendParticles(Scp075Registry.Drip.get(), Mouth.x, Mouth.y, Mouth.z, 18, 0.2, 0.15, 0.2, 0.05);
        playLayers(Scp075Registry.BirthLayers, 1.0f, 0.95f + random.nextFloat() * 0.1f);
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Size = getAgeScale();
        float Shift = 1.0f + (1.0f - Size) * 0.6f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume * (0.5f + 0.5f * Size), Pitch * Shift);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        if (Reason == MobSpawnType.SPAWN_EGG) {
            setPersistenceRequired();
        }
        return super.finalizeSpawn(World, Difficulty, Reason, Group);
    }

    @Override
    protected void onOffspringSpawnedFromEgg(Player User, Mob Child) {
        Child.setPersistenceRequired();
    }

    @Override
    public void aiStep() {
        Carried = getDeltaMovement();
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        keepCreeping();
    }

    private void keepCreeping() {
        if (locksMovement() || !onGround() || isInWaterOrBubble() || !getMoveControl().hasWanted()) {
            return;
        }
        Vec3 Current = getDeltaMovement();
        double X = Current.x == 0.0 && Math.abs(Carried.x) < DeadZone ? Carried.x : Current.x;
        double Z = Current.z == 0.0 && Math.abs(Carried.z) < DeadZone ? Carried.z : Current.z;
        if (X != Current.x || Z != Current.z) {
            setDeltaMovement(X, Current.y, Z);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return !Effect.is(Scp075Registry.Corrosion) && super.canBeAffected(Effect);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World) {
            World.sendParticles(Scp075Registry.Drip.get(), getX(), getY() + getBbHeight() * 0.4, getZ(), 3, 0.2, 0.15, 0.2, 0.02);
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp075Registry.Drip.get(), getX(), getY() + 0.3, getZ(), 16, 0.3, 0.2, 0.3, 0.04);
            playLayers(Scp075Registry.DeathLayers.subList(1, 2), 1.0f, 0.8f);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (isActing()) {
            return;
        }
        playLayers(Scp075Registry.AmbientLayers, 0.5f, 0.85f + random.nextFloat() * 0.2f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp075Registry.Ambient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp075Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp075Registry.Death.get();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (1.0f + (1.0f - getAgeScale()) * 0.6f);
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
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
        return super.getBoundingBoxForCulling().inflate(0.6);
    }

    @Nullable
    public Scp075Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp075Gait(SnailGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp075Gait Current = gait();
        return Current != null && Current.striding();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp075Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Age", Age);
        Tag.putInt("BreedCooldown", BreedReady);
        Tag.putInt("Digesting", DigestLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setAge(Tag.getInt("Age"));
        BreedReady = Tag.getInt("BreedCooldown");
        DigestLeft = Tag.getInt("Digesting");
        entityData.set(DigestingData, DigestLeft > 0);
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp075GaitController(this, "body", 0, Event -> {
            byte Current = action();
            Event.getController().transitionLength(Scp075GaitController.blendFor(Current));
            if (Current == ActionSpit || Current == ActionBirth) {
                return Event.setAndContinue(SpitPlay);
            }
            if (Current == ActionSwipe) {
                return Event.setAndContinue(SwipePlay);
            }
            if (Current == ActionEat) {
                return Event.setAndContinue(EatPlay);
            }
            if (isDigesting()) {
                return Event.setAndContinue(ChurnLoop);
            }
            if (isStriding()) {
                return Event.setAndContinue(CrawlLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
