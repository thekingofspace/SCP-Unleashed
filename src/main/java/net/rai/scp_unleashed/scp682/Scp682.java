package net.rai.scp_unleashed.scp682;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.motion.StrideGait;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp008.Scp008Registry;
import net.rai.scp_unleashed.scp009.Scp009Doom;
import net.rai.scp_unleashed.scp009.Scp009Registry;
import net.rai.scp_unleashed.scp016.Scp016Affliction;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import net.rai.scp_unleashed.scp409.Scp409Registry;
import net.rai.scp_unleashed.scp548.Scp548Chill;
import net.rai.scp_unleashed.scp548.Scp548Registry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp682 extends Monster implements GeoEntity {
    public static final float Width = 3.0f;
    public static final float Height = 3.2f;
    public static final float EyeHeight = 2.65f;
    public static final double MaxHealth = 600.0;
    public static final double Speed = 0.28;
    public static final double AttackDamage = 20.0;
    public static final double FollowRange = 40.0;
    public static final double Armor = 10.0;
    public static final double Toughness = 6.0;
    public static final double KnockbackResistance = 1.0;
    public static final double StepHeight = 1.5;
    public static final double RoamModifier = 0.55;
    public static final double HuntModifier = 1.0;
    public static final float SwimShare = 0.08f;
    public static final float WaterDrag = 0.9f;
    public static final double WalkDistance = 3.54;
    public static final double RunDistance = 5.9;
    public static final double HeadReach = 6.0;
    public static final double VisualReach = 10.5;
    public static final int QuietGrowlChance = 4;
    public static final int ActionBlend = 4;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.004, 0.12, 0.1, WalkDistance, RunDistance, 0.6, Scp682GaitController.Blend);
    public static final int CollapseTicks = 160;
    public static final int RiseTicks = 28 + Scp682GaitController.Blend;
    public static final float RegenShare = 0.006f;
    public static final float DownRegenShare = 0.03f;
    public static final float RiseShare = 0.25f;
    public static final int RegrowTicks = 6000;
    public static final int AcidShrinkTicks = 200;
    public static final int AcidCheck = 10;
    public static final int AcidHold = AcidCheck * 2;
    public static final int CalmRange = 12;
    public static final int CalmLinger = 60;
    public static final int SealRange = 24;
    public static final int SealLinger = 100;
    public static final int SenseInterval = 10;
    public static final int ExposureInterval = 20;
    public static final int DecayInterval = 20;
    public static final int FlakeChance = 40;
    public static final int FadingShift = 8;
    public static final double ShedNoticeRange = 48.0;
    public static final String ShedNotice = "notice.scp_unleashed.scp682.shed";
    public static final int ProvokeInterval = 40;
    public static final double ProvokeRange = 20.0;
    public static final int AttackCooldown = 24;
    public static final int RoarCooldown = 400;
    public static final int AmbientInterval = 200;
    public static final int CommandRise = 20;
    public static final int CommandVerdict = 24;
    public static final int CommandSinkStart = 70;
    public static final int CommandEnd = 90;
    public static final double RescueClimb = 2.0;
    public static final int EyeNormal = 0;
    public static final int EyeMany = 1;
    public static final int EyeSealed = 2;
    public static final DustParticleOptions AdaptMote = new DustParticleOptions(new Vector3f(0.86f, 0.84f, 0.74f), 1.4f);
    public static final DustParticleOptions MendMote = new DustParticleOptions(new Vector3f(0.55f, 0.5f, 0.52f), 1.0f);
    private static final ResourceLocation HealthId = Scp682Registry.location("scp682_size_health");
    private static final ResourceLocation DamageId = Scp682Registry.location("scp682_size_damage");
    private static final ResourceLocation SpeedId = Scp682Registry.location("scp682_size_speed");
    private static final ResourceLocation SteadyId = Scp682Registry.location("scp682_size_steady");
    private static final ResourceLocation StepId = Scp682Registry.location("scp682_size_step");
    private static final EntityDataAccessor<Byte> SizeData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> EyeData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> HideData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> AdaptedData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> CollapsedData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RisingData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CalmData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CommandData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SoakedData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SignatureData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FadingData = SynchedEntityData.defineId(Scp682.class, EntityDataSerializers.INT);
    private static final RawAnimation BiteOnce = RawAnimation.begin().thenPlay("bite");
    private static final RawAnimation SwipeOnce = RawAnimation.begin().thenPlay("swipe");
    private static final RawAnimation RoarOnce = RawAnimation.begin().thenPlay("roar");
    private static final RawAnimation ShrinkOnce = RawAnimation.begin().thenPlay("shrink");
    private static final RawAnimation GrowOnce = RawAnimation.begin().thenPlay("grow");
    private static final Map<ResourceLocation, Scp682Signature> Lingerers = Map.of(Scp409Registry.DocumentId, Scp682Signature.Crystal, Scp009Registry.DocumentId,
            Scp682Signature.RedIce, Scp008Registry.DocumentId, Scp682Signature.Plague);

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    private final Scp682Adaptation Adaptation = new Scp682Adaptation();
    private final Scp682Smash Smasher = new Scp682Smash();
    private int Collapse;
    private int Rise;
    private int RegrowWait = RegrowTicks;
    private int AcidTicks;
    private int CalmTicks;
    private int SealTicks;
    private int AttackWait;
    private int RoarWait;
    private int CommandAge = -1;
    @Nullable
    private UUID CommandVictim;
    @Nullable
    private BlockPos LastGround;
    private boolean Vanishing;
    private float RenderPartial;
    private float ShownScale = 1.0f;
    private float ShownScaleO = 1.0f;
    private float CommandShown;
    private float CommandShownO;
    private int DownedFor;
    private int CalmFor;
    private int Lingering;

    public Scp682(EntityType<? extends Scp682> Type, Level World) {
        super(Type, World);
        xpReward = 0;
        setPersistenceRequired();
        setPathfindingMalus(PathType.WATER, 0.0f);
        setPathfindingMalus(PathType.WATER_BORDER, 0.0f);
        setPathfindingMalus(PathType.DAMAGE_OTHER, 0.0f);
        moveControl = new SmoothSwimmingMoveControl(this, 70, 10, SwimShare, 1.0f, false);
        lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, AttackDamage)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.ARMOR, Armor).add(Attributes.ARMOR_TOUGHNESS, Toughness)
                .add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance).add(Attributes.STEP_HEIGHT, StepHeight).add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    public static boolean refusesStasis(Entity Target) {
        return Target instanceof Scp682 Reptile && !Reptile.size().holdsInStasis();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(SizeData, (byte) 0);
        Builder.define(EyeData, (byte) EyeNormal);
        Builder.define(HideData, 0);
        Builder.define(AdaptedData, (byte) 0);
        Builder.define(CollapsedData, false);
        Builder.define(RisingData, false);
        Builder.define(CalmData, false);
        Builder.define(CommandData, false);
        Builder.define(SoakedData, false);
        Builder.define(SignatureData, 0);
        Builder.define(FadingData, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp682Goals.Maul(this));
        goalSelector.addGoal(3, new Scp682Goals.Escape(this));
        goalSelector.addGoal(5, new Scp682Goals.Roam(this));
        goalSelector.addGoal(7, new Scp682Goals.Gaze(this));
        targetSelector.addGoal(1, new Scp682Goals.Retaliate(this));
        targetSelector.addGoal(2, new Scp682Goals.Hunt(this));
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new AmphibiousPathNavigation(this, World);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        applySize(size(), true);
        return Result;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (SizeData.equals(Key)) {
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose Stance) {
        return super.getDefaultDimensions(Stance).scale(size().scale());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(VisualReach * size().scale(), 1.2 * size().scale(), VisualReach * size().scale());
    }

    public Scp682Size size() {
        return Scp682Size.byIndex(entityData.get(SizeData));
    }

    public int eyes() {
        return entityData.get(EyeData);
    }

    public int hide() {
        return entityData.get(HideData);
    }

    public int adaptedCount() {
        return entityData.get(AdaptedData);
    }

    public boolean isCollapsed() {
        return entityData.get(CollapsedData);
    }

    public boolean isRising() {
        return entityData.get(RisingData);
    }

    public boolean isCalm() {
        return entityData.get(CalmData);
    }

    public boolean isDowned() {
        return isCollapsed() || isRising();
    }

    public int signatures() {
        return entityData.get(SignatureData);
    }

    public boolean holds(Scp682Signature Signature) {
        return (signatures() & Signature.bit()) != 0;
    }

    public int fadingHide() {
        return entityData.get(FadingData) & ((1 << FadingShift) - 1);
    }

    public int fadingSignatures() {
        return entityData.get(FadingData) >>> FadingShift;
    }

    public static boolean spared(Entity Target, @Nullable TagKey<EntityType<?>> Own) {
        return Scp409Registry.Immune.equals(Own) && Target instanceof Scp682 Reptile && Reptile.holds(Scp682Signature.Crystal);
    }

    public static boolean takes(LivingEntity Target, ResourceLocation Agent) {
        if (!(Target instanceof Scp682 Reptile)) {
            return false;
        }
        Scp682Signature Signature = Lingerers.get(Agent);
        return Signature != null && (Signature == Scp682Signature.Plague || !Reptile.Adaptation.holds(Signature));
    }

    public static void absorb(LivingEntity Target, ResourceLocation Agent) {
        if (Target instanceof Scp682 Reptile && Lingerers.get(Agent) instanceof Scp682Signature Signature) {
            Reptile.Lingering |= Signature.bit();
        }
    }

    public boolean lingers(Scp682Signature Signature) {
        return (Lingering & Signature.bit()) != 0;
    }

    @Override
    public boolean fireImmune() {
        return super.fireImmune() || holds(Scp682Signature.Cinder);
    }

    @Override
    public boolean canFreeze() {
        return super.canFreeze() && !holds(Scp682Signature.Rime);
    }

    public int downedFor() {
        return DownedFor;
    }

    public int calmFor() {
        return CalmFor;
    }

    public boolean isCommanding() {
        return entityData.get(CommandData);
    }

    public boolean isSoaked() {
        return entityData.get(SoakedData);
    }

    public boolean isCommandBusy() {
        return CommandAge >= 0;
    }

    public Scp682Adaptation adaptation() {
        return Adaptation;
    }

    public Scp682Smash smasher() {
        return Smasher;
    }

    public StrideGait stride() {
        return Stride;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public float shownScale(float Partial) {
        return Mth.lerp(Partial, ShownScaleO, ShownScale);
    }

    public float commandShown(float Partial) {
        return Mth.lerp(Partial, CommandShownO, CommandShown);
    }

    public boolean canAct() {
        return isAlive() && !isCollapsed() && !isRising() && !isCommandBusy() && !Vanishing;
    }

    public boolean isStriding() {
        return Stride.stride() != StrideGait.Idle;
    }

    public boolean isRunning() {
        return Stride.stride() == StrideGait.Run;
    }

    public boolean isSwimmingNow() {
        return isInWater() && !onGround();
    }

    public float voicePitch() {
        return (0.9f + random.nextFloat() * 0.12f) * (1.0f + (1.0f - size().scale()) * 0.55f);
    }

    public float voiceVolume() {
        return 0.6f + size().scale() * 1.4f;
    }

    public void setSize(Scp682Size Size) {
        entityData.set(SizeData, (byte) Size.ordinal());
        applySize(Size, false);
        refreshDimensions();
    }

    private void applySize(Scp682Size Size, boolean Fill) {
        shift(Attributes.MAX_HEALTH, HealthId, Size.healthShift(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        shift(Attributes.ATTACK_DAMAGE, DamageId, Size.damageShift(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        shift(Attributes.MOVEMENT_SPEED, SpeedId, Size.speedShift(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        shift(Attributes.KNOCKBACK_RESISTANCE, SteadyId, Size.steadyShift(), AttributeModifier.Operation.ADD_VALUE);
        shift(Attributes.STEP_HEIGHT, StepId, Size == Scp682Size.Shrunk ? -0.9 : Size == Scp682Size.Reduced ? -0.5 : 0.0, AttributeModifier.Operation.ADD_VALUE);
        if (Fill) {
            setHealth(getMaxHealth());
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    private void shift(Holder<Attribute> Stat, ResourceLocation Id, double Amount, AttributeModifier.Operation Operation) {
        AttributeInstance Instance = getAttribute(Stat);
        if (Instance == null) {
            return;
        }
        Instance.removeModifier(Id);
        if (Amount != 0.0) {
            Instance.addPermanentModifier(new AttributeModifier(Id, Amount, Operation));
        }
    }

    public StrideGait.Shape gait() {
        float Scale = size().scale();
        return new StrideGait.Shape(Gait.StillPace() * Scale, Gait.RunEnter() * Scale, Gait.RunExit() * Scale, WalkDistance * Scale, RunDistance * Scale, Gait.MaxStep(),
                Gait.Blend());
    }

    @Override
    public boolean canDrownInFluidType(FluidType Kind) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || isCollapsed() || isRising() || isCommandBusy();
    }

    @Override
    public void travel(Vec3 Input) {
        if (isControlledByLocalInstance() && isInWater()) {
            Vec3 Push = Input.lengthSqr() > 1.0e-6 ? Input.normalize() : Vec3.ZERO;
            moveRelative(getSpeed(), Push);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(WaterDrag));
        } else {
            super.travel(Input);
        }
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return Scp682Rivals.mayAttack(this, Target) && super.canAttack(Target);
    }

    public boolean strike(LivingEntity Target) {
        if (AttackWait > 0 || !canAct() || isCalm()) {
            return false;
        }
        AttackWait = AttackCooldown;
        Vec3 Way = Target.position().subtract(position());
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        double Side = Facing.x * Way.z - Facing.z * Way.x;
        boolean Swipe = Math.abs(Side) > Way.horizontalDistance() * 0.55 || random.nextInt(4) == 0;
        swing(InteractionHand.MAIN_HAND);
        if (level() instanceof ServerLevel World) {
            triggerAnim("action", Swipe ? "swipe" : "bite");
            Scp682Sounds.play(World, Target.position(), Swipe ? Scp682Registry.SwipeLayers : Scp682Registry.BiteLayers, voiceVolume() * 0.7f, voicePitch());
        }
        return doHurtTarget(Target);
    }

    public boolean attackReady() {
        return AttackWait <= 0;
    }

    public double reach() {
        return getBbWidth() * 0.5 + HeadReach * size().scale();
    }

    public void roar(ServerLevel World) {
        RoarWait = RoarCooldown;
        triggerAnim("action", "roar");
        Scp682Sounds.play(World, getEyePosition(), Scp682Registry.RoarLayers, voiceVolume() * 1.6f, voicePitch() * 0.95f);
    }

    public boolean roarReady() {
        return RoarWait <= 0 && !isSoaked();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive() && !isDowned() && !isSwimmingNow(), gait());
            DownedFor = isCollapsed() ? DownedFor + 1 : 0;
            CalmFor = isCalm() ? CalmFor + 1 : 0;
            ShownScaleO = ShownScale;
            ShownScale += (size().scale() - ShownScale) * 0.12f;
            if (Math.abs(ShownScale - size().scale()) < 0.002f) {
                ShownScale = size().scale();
            }
            CommandShownO = CommandShown;
            CommandShown = Mth.clamp(CommandShown + (isCommanding() ? 1.0f / CommandRise : -1.0f / CommandRise), 0.0f, 1.0f);
            if (isCollapsed() && random.nextInt(6) == 0) {
                Vec3 Spot = position().add((random.nextDouble() - 0.5) * getBbWidth(), getBbHeight() * 0.6, (random.nextDouble() - 0.5) * getBbWidth());
                level().addParticle(MendMote, Spot.x, Spot.y, Spot.z, 0.0, 0.02, 0.0);
            }
            if (entityData.get(FadingData) != 0 && random.nextInt(FlakeChance) == 0) {
                Vec3 Spot = position().add((random.nextDouble() - 0.5) * getBbWidth(), getBbHeight() * (0.35 + random.nextDouble() * 0.45),
                        (random.nextDouble() - 0.5) * getBbWidth());
                level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.BONE_BLOCK.defaultBlockState()), Spot.x, Spot.y, Spot.z, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive() || Vanishing) {
            return;
        }
        if (AttackWait > 0) {
            AttackWait--;
        }
        if (RoarWait > 0) {
            RoarWait--;
        }
        if (onGround() && !isInWater()) {
            LastGround = blockPosition();
        }
        if (isCommandBusy()) {
            Scp682Kill.tick(World, this);
        }
        int Clock = tickCount + getId();
        if (Clock % SenseInterval == 0) {
            sense(World);
        }
        if (Clock % AcidCheck == 0) {
            acidTick(World, AcidCheck);
        }
        if (Clock % ExposureInterval == 0) {
            exposures(World);
        }
        if (Clock % DecayInterval == 0) {
            decayTick(World, DecayInterval);
        }
        if (Clock % ProvokeInterval == 0 && canAct()) {
            Scp682Rivals.provoke(World, this);
        }
        recover(World);
    }

    private void recover(ServerLevel World) {
        if (Collapse > 0) {
            Collapse--;
            if (!isSoaked()) {
                heal(getMaxHealth() * DownRegenShare / 20.0f);
            }
            if (Collapse % 20 == 0) {
                Scp682Sounds.play(World, position(), Scp682Registry.MendLayers, voiceVolume() * 0.4f, voicePitch());
            }
            if (Collapse == 0 && isSoaked() && size() != Scp682Size.Shrunk) {
                Collapse = AcidHold;
                return;
            }
            if (Collapse == 0) {
                entityData.set(CollapsedData, false);
                entityData.set(RisingData, true);
                Rise = RiseTicks;
                setHealth(Math.max(getHealth(), isSoaked() ? 1.0f : getMaxHealth() * RiseShare));
            }
            return;
        }
        if (Rise > 0 && --Rise == 0) {
            entityData.set(RisingData, false);
            if (roarReady() && !isSoaked()) {
                roar(World);
            }
        }
        if (!isSoaked() && tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
            heal(getMaxHealth() * RegenShare);
        }
    }

    private void sense(ServerLevel World) {
        boolean Soothed = Scp682Rivals.soothed(World, this);
        CalmTicks = Soothed ? CalmLinger : Math.max(0, CalmTicks - SenseInterval);
        boolean Calm = CalmTicks > 0;
        if (Calm != isCalm()) {
            entityData.set(CalmData, Calm);
            if (Calm) {
                setTarget(null);
                getNavigation().stop();
                Scp682Sounds.play(World, position(), List.of(Scp682Registry.Calm), voiceVolume() * 0.6f, voicePitch() * 0.9f);
            }
        }
        if ((tickCount + getId()) % (SenseInterval * 2) == 0) {
            boolean Unseen = Scp682Rivals.unseenNear(World, this);
            SealTicks = Unseen ? SealLinger : Math.max(0, SealTicks - SenseInterval * 2);
        }
        refreshEyes(World);
    }

    public void refreshEyes(ServerLevel World) {
        int Wanted = SealTicks > 0 ? EyeSealed : Adaptation.isImmune(Scp682Adaptation.Snap) ? EyeMany : EyeNormal;
        if (Wanted != eyes()) {
            entityData.set(EyeData, (byte) Wanted);
            Scp682Sounds.play(World, getEyePosition(), Wanted == EyeSealed ? List.of(Scp682Registry.Seal) : Scp682Registry.EyesLayers, voiceVolume() * 0.6f, voicePitch());
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()), getX(), getY() + getBbHeight() * 0.7, getZ(), 24,
                    getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.05);
            if (Wanted == EyeSealed && getTarget() != null && !Scp682Rivals.mayAttack(this, getTarget())) {
                setTarget(null);
            }
        }
    }

    public void sealFor(int Ticks) {
        SealTicks = Math.max(SealTicks, Ticks);
    }

    public boolean acidAt() {
        AABB Box = getBoundingBox().deflate(0.05, 0.0, 0.05);
        double Reach = Math.min(Box.maxY, Box.minY + Math.max(0.3, getBbHeight() * 0.4));
        for (BlockPos Pos : BlockPos.betweenClosed(BlockPos.containing(Box.minX, Box.minY, Box.minZ), BlockPos.containing(Box.maxX, Reach, Box.maxZ))) {
            FluidState Liquid = level().getFluidState(Pos);
            if (!Liquid.isEmpty() && Liquid.is(Scp682Registry.AcidFluid) && Pos.getY() + Liquid.getHeight(level(), Pos) > Box.minY + 0.05) {
                return true;
            }
        }
        return false;
    }

    public void acidTick(ServerLevel World, int Step) {
        boolean Soaked = acidAt();
        if (Soaked != isSoaked()) {
            entityData.set(SoakedData, Soaked);
        }
        if (Soaked) {
            RegrowWait = RegrowTicks;
            AcidTicks += Step;
            if (AcidTicks >= AcidShrinkTicks) {
                AcidTicks = 0;
                if (size() != Scp682Size.Shrunk) {
                    shrink(World);
                }
            }
            return;
        }
        AcidTicks = 0;
        if (size() == Scp682Size.Full || isCollapsed()) {
            RegrowWait = RegrowTicks;
            return;
        }
        RegrowWait -= Step;
        if (RegrowWait <= 0) {
            RegrowWait = RegrowTicks;
            grow(World);
        }
    }

    public int regrowWait() {
        return RegrowWait;
    }

    public void setRegrowWait(int Ticks) {
        RegrowWait = Ticks;
    }

    public void shrink(ServerLevel World) {
        if (size() == Scp682Size.Shrunk) {
            return;
        }
        setSize(size().smaller());
        Adaptation.clearTally();
        RegrowWait = RegrowTicks;
        Smasher.clear(World, this);
        if (!isDowned()) {
            triggerAnim("action", "shrink");
        }
        Scp682Sounds.play(World, position(), Scp682Registry.ShrinkLayers, voiceVolume(), voicePitch());
        World.sendParticles(ParticleTypes.CLOUD, getX(), getY() + getBbHeight() * 0.5, getZ(), 30, getBbWidth() * 0.6, getBbHeight() * 0.4, getBbWidth() * 0.6, 0.02);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), getX(), getY() + getBbHeight() * 0.5, getZ(), 30,
                getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.1);
    }

    public void grow(ServerLevel World) {
        if (size() == Scp682Size.Full) {
            return;
        }
        setSize(size().larger());
        triggerAnim("action", "grow");
        Scp682Sounds.play(World, position(), Scp682Registry.GrowLayers, voiceVolume(), voicePitch());
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), getX(), getY() + getBbHeight() * 0.5, getZ(), 20,
                getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.1);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level().isClientSide || isRemoved() || Vanishing) {
            return false;
        }
        if (Source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            rescue();
            return false;
        }
        if (isCommandBusy()) {
            return false;
        }
        if (size() == Scp682Size.Shrunk && Source.is(Scp682Registry.AcidDamage)) {
            return false;
        }
        ResourceLocation Kind = Scp682Adaptation.kindOf(Source);
        Entity Attacker = Source.getEntity();
        if (Adaptation.isImmune(Kind, Attacker)) {
            if (Adaptation.refresh(Kind, Attacker) > 0) {
                syncAdaptation();
            }
            glance(Source, Kind, Attacker);
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World) {
            float Dealt = Math.min(Amount, getMaxHealth());
            Scp682Adaptation.Outcome Result = Adaptation.record(Kind, Scp682Adaptation.categoryOf(Source), Dealt, World.getGameTime(), Attacker);
            if (Result.Adapted()) {
                adapted(World, Result.Gained() != null ? Result.Gained() : Kind);
            }
            if (Result.Shrinks() && size() != Scp682Size.Shrunk) {
                shrink(World);
            }
        }
        return Hurt;
    }

    public void adapted(ServerLevel World, ResourceLocation Kind) {
        syncAdaptation();
        refreshEyes(World);
        Scp682Sounds.play(World, position(), Scp682Registry.AdaptLayers, voiceVolume(), voicePitch());
        if (canAct() && roarReady()) {
            roar(World);
        }
        World.sendParticles(AdaptMote, getX(), getY() + getBbHeight() * 0.6, getZ(), 50, getBbWidth() * 0.55, getBbHeight() * 0.4, getBbWidth() * 0.55, 0.0);
        World.sendParticles(ParticleTypes.FLASH, getX(), getY() + getBbHeight() * 0.6, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        Scp682Signature Signature = Scp682Signature.byId(Kind);
        if (Signature != null) {
            World.sendParticles(Signature.flare(), getX(), getY() + getBbHeight() * 0.6, getZ(), 40, getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.02);
            cleanse();
        }
    }

    private void exposures(ServerLevel World) {
        if (getTicksFrozen() > 0 || Scp548Chill.isFrostbitten(this)) {
            exposeTo(World, Scp682Signature.Rime);
        }
        if (Scp409Infection.isInfected(this) || lingers(Scp682Signature.Crystal)) {
            exposeTo(World, Scp682Signature.Crystal);
        }
        if (Scp009Doom.isDoomed(this) || lingers(Scp682Signature.RedIce)) {
            exposeTo(World, Scp682Signature.RedIce);
        }
        if (Scp008Infection.isInfected(this) || Scp016Affliction.isAfflicted(this) || lingers(Scp682Signature.Plague)) {
            exposeTo(World, Scp682Signature.Plague);
        }
        cleanse();
    }

    private void exposeTo(ServerLevel World, Scp682Signature Signature) {
        if (Adaptation.holds(Signature)) {
            if (Adaptation.renew(Signature.id()) > 0) {
                syncAdaptation();
            }
            return;
        }
        Scp682Adaptation.Outcome Result = Adaptation.expose(Signature);
        if (Result.Adapted()) {
            adapted(World, Signature.id());
        }
    }

    private void cleanse() {
        if (Adaptation.holds(Scp682Signature.Cinder) && getRemainingFireTicks() > 0) {
            clearFire();
        }
        if (Adaptation.holds(Scp682Signature.Rime)) {
            if (Scp548Chill.isFrostbitten(this)) {
                removeEffect(Scp548Registry.Frostbite);
            }
            setTicksFrozen(0);
        }
        if (Adaptation.holds(Scp682Signature.Crystal) && Scp409Infection.isInfected(this)) {
            Scp409Infection.cure(this);
        }
        if (Adaptation.holds(Scp682Signature.RedIce) && Scp009Doom.isDoomed(this)) {
            Scp009Doom.clear(this);
        }
        if (Adaptation.holds(Scp682Signature.Plague)) {
            if (Scp008Infection.isInfected(this)) {
                Scp008Infection.cure(this);
            }
            if (Scp016Affliction.isAfflicted(this)) {
                Scp016Affliction.cure(this);
            }
        }
        for (Scp682Signature Each : Lingerers.values()) {
            if (Adaptation.holds(Each)) {
                Lingering &= ~Each.bit();
            }
        }
    }

    public boolean refuses(Holder<MobEffect> Effect) {
        return Effect.value() == Scp548Registry.Frostbite.get() && Adaptation.holds(Scp682Signature.Rime)
                || Effect.value() == Scp009Registry.Doomed.get() && Adaptation.holds(Scp682Signature.RedIce);
    }

    public void syncAdaptation() {
        entityData.set(HideData, Adaptation.categories());
        entityData.set(AdaptedData, (byte) Adaptation.count());
        entityData.set(SignatureData, Adaptation.signatures());
        entityData.set(FadingData, Adaptation.fadingCategories() | Adaptation.fadingSignatures() << FadingShift);
    }

    public void decayTick(ServerLevel World, int Step) {
        ResourceLocation Lost = Adaptation.decay(Step);
        if (Lost != null) {
            shed(World, Lost);
        } else {
            syncAdaptation();
        }
    }

    public void shed(ServerLevel World, ResourceLocation Kind) {
        syncAdaptation();
        refreshEyes(World);
        Scp682Sounds.play(World, position(), Scp682Registry.ShedLayers, voiceVolume() * 0.8f, voicePitch() * 0.9f);
        if (!isDowned() && !isCommandBusy()) {
            triggerAnim("action", "shrink");
        }
        World.getChunkSource().broadcast(this, new ClientboundHurtAnimationPacket(this));
        double Mid = getY() + getBbHeight() * 0.6;
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), getX(), Mid, getZ(), 36, getBbWidth() * 0.5,
                getBbHeight() * 0.35, getBbWidth() * 0.5, 0.1);
        World.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.BONE_BLOCK.defaultBlockState()), getX(), Mid, getZ(), 28, getBbWidth() * 0.55,
                getBbHeight() * 0.35, getBbWidth() * 0.55, 0.0);
        Scp682Signature Signature = Scp682Signature.byId(Kind);
        if (Signature != null) {
            World.sendParticles(Signature.flare(), getX(), Mid, getZ(), 16, getBbWidth() * 0.45, getBbHeight() * 0.35, getBbWidth() * 0.45, 0.01);
        }
        Notices.sendNear(World, position(), ShedNoticeRange, Notice.anomaly(DocumentClass.Keter, Component.translatable(ShedNotice))
                .withDetail(Component.translatable(ShedNotice + ".detail")));
    }

    private void glance(DamageSource Source, ResourceLocation Kind, @Nullable Entity Attacker) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Adaptation.holds(Scp682Signature.Cinder) && Scp682Signature.Cinder.covers(Kind, null)
                || Adaptation.holds(Scp682Signature.Rime) && Scp682Signature.Rime.covers(Kind, null)) {
            return;
        }
        Vec3 At = Source.getSourcePosition() != null ? Source.getSourcePosition() : position().add(0.0, getBbHeight() * 0.6, 0.0);
        Vec3 Spot = At.subtract(position()).normalize().scale(getBbWidth() * 0.45).add(position()).add(0.0, getBbHeight() * 0.6, 0.0);
        if (Adaptation.holds(Scp682Signature.Plated) && Scp682Signature.Plated.covers(Kind, Attacker)) {
            World.sendParticles(ParticleTypes.CRIT, Spot.x, Spot.y, Spot.z, 10, 0.1, 0.1, 0.1, 0.45);
            World.sendParticles(ParticleTypes.SMALL_FLAME, Spot.x, Spot.y, Spot.z, 2, 0.05, 0.05, 0.05, 0.02);
            if (tickCount % 2 == 0 || Attacker instanceof Player) {
                Scp682Sounds.play(World, Spot, Scp682Registry.RicochetLayers, 0.8f, 0.9f + random.nextFloat() * 0.3f);
            }
            return;
        }
        World.sendParticles(ParticleTypes.CRIT, Spot.x, Spot.y, Spot.z, 6, 0.15, 0.15, 0.15, 0.2);
        if (tickCount % 4 == 0 || Attacker instanceof Player) {
            Scp682Sounds.play(World, Spot, Scp682Registry.GlanceLayers, 0.7f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    @Override
    public void setHealth(float Value) {
        if (Value <= 0.0f && !level().isClientSide && !Vanishing) {
            super.setHealth(1.0f);
            collapse();
            return;
        }
        super.setHealth(Value);
    }

    public void collapse() {
        if (Collapse > 0 || isRemoved()) {
            return;
        }
        Collapse = CollapseTicks;
        Rise = 0;
        entityData.set(RisingData, false);
        entityData.set(CollapsedData, true);
        getNavigation().stop();
        if (level() instanceof ServerLevel World) {
            Smasher.clear(World, this);
            Scp682Sounds.play(World, position(), Scp682Registry.CollapseLayers, voiceVolume(), voicePitch());
        }
    }

    public int collapseLeft() {
        return Collapse;
    }

    @Override
    public void die(DamageSource Source) {
        if (Vanishing || level().isClientSide) {
            super.die(Source);
            return;
        }
        super.setHealth(1.0f);
        collapse();
    }

    @Override
    public void kill() {
        if (Vanishing || level().isClientSide) {
            super.kill();
            return;
        }
        super.setHealth(1.0f);
        collapse();
    }

    @Override
    protected void onBelowWorld() {
        rescue();
    }

    public void rescue() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        BlockPos Anchor = LastGround != null ? LastGround : World.getSharedSpawnPos();
        int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING, Anchor.getX(), Anchor.getZ());
        double Y = Math.max(Top, World.getMinBuildHeight() + 1) + RescueClimb;
        teleportTo(Anchor.getX() + 0.5, Y, Anchor.getZ() + 0.5);
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 30, getBbWidth() * 0.4, 0.4, getBbWidth() * 0.4, 0.02);
    }

    public void answerKill(@Nullable ServerPlayer Issuer) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (isCommandBusy()) {
            if (CommandVictim == null && Issuer != null && CommandAge < CommandVerdict) {
                CommandVictim = Issuer.getUUID();
            }
            return;
        }
        CommandAge = 0;
        CommandVictim = Issuer == null ? null : Issuer.getUUID();
        entityData.set(CommandData, true);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        Scp682Sounds.play(World, position(), Scp682Registry.CommandLayers, 1.4f, 0.9f);
    }

    public int commandAge() {
        return CommandAge;
    }

    public void setCommandAge(int Age) {
        CommandAge = Age;
    }

    @Nullable
    public UUID commandVictim() {
        return CommandVictim;
    }

    public void endCommandShow() {
        entityData.set(CommandData, false);
    }

    public void finishCommand() {
        CommandAge = -1;
        CommandVictim = null;
        entityData.set(CommandData, false);
    }

    public boolean isVanishing() {
        return Vanishing;
    }

    public void vanish(ServerLevel World) {
        if (Vanishing || isRemoved()) {
            return;
        }
        Vanishing = true;
        Smasher.clear(World, this);
        Vec3 Centre = getBoundingBox().getCenter();
        World.sendParticles(ParticleTypes.REVERSE_PORTAL, Centre.x, Centre.y, Centre.z, 120, getBbWidth() * 0.6, getBbHeight() * 0.5, getBbWidth() * 0.6, 0.05);
        World.sendParticles(ParticleTypes.CLOUD, Centre.x, Centre.y, Centre.z, 40, getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.02);
        Scp682Sounds.play(World, Centre, Scp682Registry.RemovedLayers, 1.2f, 1.0f);
        discard();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
        if (level() instanceof ServerLevel World) {
            Scp682Sounds.play(World, position(), Scp682Registry.StepLayers, 0.25f + 0.45f * size().scale(), voicePitch());
        }
    }

    @Override
    protected void playSwimSound(float Volume) {
        if (level() instanceof ServerLevel World && random.nextInt(3) == 0) {
            Scp682Sounds.play(World, position(), Scp682Registry.SwimLayers, Math.min(1.0f, Volume * 4.0f) * size().scale(), voicePitch());
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp682Registry.Growl.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (isCalm()) {
            Scp682Sounds.play(World, position(), List.of(Scp682Registry.Calm), voiceVolume() * 0.5f, voicePitch() * 0.9f);
            return;
        }
        if (isSoaked()) {
            if (random.nextInt(QuietGrowlChance) == 0) {
                Scp682Sounds.play(World, position(), List.of(Scp682Registry.GrowlLow), voiceVolume() * 0.4f, voicePitch() * 0.9f);
            }
            return;
        }
        if (random.nextBoolean()) {
            Scp682Sounds.play(World, position(), Scp682Registry.GrowlLayers, voiceVolume() * 0.7f, voicePitch());
        } else {
            Scp682Sounds.play(World, position(), Scp682Registry.BreatheLayers, voiceVolume() * 0.6f, voicePitch());
        }
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            Scp682Sounds.play(World, position(), Scp682Registry.HurtLayers, voiceVolume() * 0.8f, voicePitch());
        }
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp682Registry.Hurt.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return Scp682Registry.Collapse.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("Size", (byte) size().ordinal());
        Tag.put("Adaptation", Adaptation.save());
        Tag.putInt("Collapse", Collapse);
        Tag.putInt("RegrowWait", RegrowWait);
        Tag.putInt("AcidTicks", AcidTicks);
        Tag.putInt("Lingering", Lingering);
        if (LastGround != null) {
            Tag.put("LastGround", NbtUtils.writeBlockPos(LastGround));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Scp682Size Saved = Scp682Size.byIndex(Tag.getByte("Size"));
        entityData.set(SizeData, (byte) Saved.ordinal());
        applySize(Saved, false);
        refreshDimensions();
        Adaptation.load(Tag.getCompound("Adaptation"));
        syncAdaptation();
        Collapse = Math.max(0, Tag.getInt("Collapse"));
        entityData.set(CollapsedData, Collapse > 0);
        RegrowWait = Tag.contains("RegrowWait") ? Math.max(1, Tag.getInt("RegrowWait")) : RegrowTicks;
        AcidTicks = Math.max(0, Tag.getInt("AcidTicks"));
        Lingering = Tag.getInt("Lingering");
        LastGround = NbtUtils.readBlockPos(Tag, "LastGround").orElse(null);
        entityData.set(EyeData, (byte) (Adaptation.isImmune(Scp682Adaptation.Snap) ? EyeMany : EyeNormal));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp682GaitController(this));
        Controllers.add(new AnimationController<>(this, "action", ActionBlend, Scp682::overlay).receiveTriggeredAnimations().triggerableAnim("bite", BiteOnce)
                .triggerableAnim("swipe", SwipeOnce).triggerableAnim("roar", RoarOnce).triggerableAnim("shrink", ShrinkOnce).triggerableAnim("grow", GrowOnce));
    }

    private static PlayState overlay(AnimationState<Scp682> Event) {
        return Event.getController().isPlayingTriggeredAnimation() && !Event.getAnimatable().isDowned() ? PlayState.CONTINUE : PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
