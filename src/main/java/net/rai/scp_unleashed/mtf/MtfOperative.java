package net.rai.scp_unleashed.mtf;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.rai.scp_unleashed.npc.doors.NpcDoorNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.gear.GearRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class MtfOperative extends DClassLike implements ControlRefusing {
    public static final byte StateRoping = 0;
    public static final byte StateActive = 1;
    public static final byte StateRegroup = 2;
    public static final byte StateBoarding = 3;
    public static final byte StatusFollowing = 0;
    public static final byte StatusEngaging = 1;
    public static final byte StatusCover = 2;
    public static final byte StatusRegroup = 3;
    public static final byte StatusRoping = 4;
    public static final byte StatusBoarding = 5;
    public static final double SlideStart = 0.12;
    public static final double SlideMax = 0.45;
    public static final double SlideAccel = 0.03;
    public static final double SlideBrake = 2.5;
    public static final double SlideFloor = 0.14;
    public static final double ClimbSpeed = 0.22;
    public static final double HandReach = 2.25;
    public static final int RopeHoldTicks = 4;
    public static final int OrphanLimit = 60;
    public static final int[] SafeStyles = {DClassAppearance.Bald, DClassAppearance.Buzz, DClassAppearance.Short, DClassAppearance.Parted};
    private static final EntityDataAccessor<Byte> TierData = SynchedEntityData.defineId(MtfOperative.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> RoleData = SynchedEntityData.defineId(MtfOperative.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(MtfOperative.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> AimingData = SynchedEntityData.defineId(MtfOperative.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID CallerId;
    @Nullable
    private UUID HelicopterId;
    private int RopeSide;
    private double RopeTop;
    private double RopeSpeed;
    private int RopeHold;
    private int OrphanTicks;
    private int Reaction;
    private boolean InCover;
    private int Slot;
    @Nullable
    private Vec3 RegroupPoint;

    public MtfOperative(EntityType<? extends MtfOperative> Type, Level World) {
        super(Type, World);
        setCanPickUpLoot(false);
        setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation Ground) {
            Ground.setCanOpenDoors(true);
            Ground.setCanPassDoors(true);
        }
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            setDropChance(Slot, 0.0f);
        }
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new NpcDoorNavigation(this, World);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.ARMOR, 0.0).add(Attributes.ARMOR_TOUGHNESS, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(TierData, (byte) 0);
        Builder.define(RoleData, (byte) 0);
        Builder.define(StateData, StateActive);
        Builder.define(AimingData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(1, new MtfOperativeGoals.Regroup(this));
        goalSelector.addGoal(2, new MtfOperativeGoals.TakeCover(this));
        goalSelector.addGoal(3, new MtfOperativeGoals.Engage(this));
        goalSelector.addGoal(4, new MtfOperativeGoals.FollowCaller(this));
        goalSelector.addGoal(5, new MtfOperativeGoals.WatchCaller(this));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return onDuty() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new MtfOperativeGoals.PickTarget(this));
    }

    public void configure(UUID Caller, MtfTier Tier, MtfRole Role, int Index, RandomSource Random) {
        CallerId = Caller;
        Slot = Index;
        entityData.set(TierData, (byte) Tier.ordinal());
        entityData.set(RoleData, (byte) Role.ordinal());
        int Style = SafeStyles[Random.nextInt(SafeStyles.length)];
        int Facial = Random.nextInt(10) < 6 ? 0 : 1 + Random.nextInt(DClassAppearance.Facials - 1);
        setAppearance(DClassAppearance.pack(Random.nextInt(DClassAppearance.Skins), Style, Random.nextInt(DClassAppearance.HairColours - 1),
                Random.nextInt(DClassAppearance.EyeColours), Random.nextInt(DClassAppearance.Brows), Facial, 0, DClassAppearance.Fatigues));
        setItemSlot(EquipmentSlot.HEAD, Role.helmet());
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.MtfVest.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.MtfPants.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.MtfBoots.get()));
        setItemSlot(EquipmentSlot.MAINHAND, Role.weapon());
        for (EquipmentSlot Each : EquipmentSlot.values()) {
            setDropChance(Each, 0.0f);
        }
        base(Attributes.MAX_HEALTH, Tier.health());
        base(Attributes.ARMOR, Tier.armour());
        base(Attributes.ARMOR_TOUGHNESS, Tier.toughness());
        base(Attributes.MOVEMENT_SPEED, Tier.speed());
        setHealth(getMaxHealth());
    }

    private void base(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> Attribute, double Value) {
        AttributeInstance Instance = getAttribute(Attribute);
        if (Instance != null) {
            Instance.setBaseValue(Value);
        }
    }

    @Nullable
    public UUID callerId() {
        return CallerId;
    }

    @Nullable
    public ServerPlayer caller() {
        if (CallerId == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        ServerPlayer Found = World.getServer().getPlayerList().getPlayer(CallerId);
        return Found != null && Found.level() == World ? Found : null;
    }

    @Nullable
    public MtfMission mission() {
        return CallerId == null ? null : MtfMissions.get(CallerId);
    }

    public MtfTier tier() {
        return MtfTier.byIndex(entityData.get(TierData));
    }

    public MtfRole role() {
        return MtfRole.byIndex(entityData.get(RoleData));
    }

    public byte state() {
        return entityData.get(StateData);
    }

    public void setState(byte Value) {
        entityData.set(StateData, Value);
    }

    public boolean isRoping() {
        byte Current = state();
        return Current == StateRoping || Current == StateBoarding;
    }

    public boolean onDuty() {
        return state() == StateActive && CallerId != null;
    }

    public boolean isAiming() {
        return entityData.get(AimingData);
    }

    public void setAiming(boolean Value) {
        if (Value != isAiming()) {
            entityData.set(AimingData, Value);
        }
    }

    public int slot() {
        return Slot;
    }

    public int ropeSide() {
        return RopeSide;
    }

    public boolean inCover() {
        return InCover;
    }

    public void setInCover(boolean Value) {
        InCover = Value;
    }

    public int reaction() {
        return Reaction;
    }

    public void delayReaction() {
        Reaction = tier().reactionTicks() + getRandom().nextInt(tier().reactionTicks() / 2 + 1);
    }

    @Nullable
    public Vec3 regroupPoint() {
        return RegroupPoint;
    }

    public byte status() {
        return switch (state()) {
            case StateRoping -> StatusRoping;
            case StateBoarding -> StatusBoarding;
            case StateRegroup -> StatusRegroup;
            default -> InCover ? StatusCover : getTarget() != null ? StatusEngaging : StatusFollowing;
        };
    }

    public void beginRope(UUID Helicopter, int Side, double Top) {
        HelicopterId = Helicopter;
        RopeSide = Side;
        RopeTop = Top;
        RopeSpeed = SlideStart;
        RopeHold = RopeHoldTicks;
        setNoGravity(true);
        setState(StateRoping);
    }

    public void beginBoarding(MtfHelicopter Helicopter, int Side) {
        HelicopterId = Helicopter.getUUID();
        RopeSide = Side;
        Vec3 Stand = MtfLandingZone.standPoint(Helicopter.hover(), Helicopter.hoverYaw(), Side);
        RopeTop = Helicopter.hover().y + MtfLandingZone.AnchorUp - HandReach;
        float Yaw = MtfLandingZone.inwardYaw(Helicopter.hoverYaw(), Side);
        getNavigation().stop();
        setTarget(null);
        setAiming(false);
        moveTo(Stand.x, getY(), Stand.z, Yaw, 0.0f);
        setYHeadRot(Yaw);
        yBodyRot = Yaw;
        RopeHold = RopeHoldTicks + 2;
        setNoGravity(true);
        setState(StateBoarding);
        playLayers(MtfRegistry.RopeLandLayers, 0.7f, 1.2f);
    }

    public void regroup(Vec3 Point) {
        RegroupPoint = Point;
        setTarget(null);
        setAiming(false);
        InCover = false;
        if (state() == StateActive) {
            setState(StateRegroup);
        }
    }

    public double ropeTop() {
        return RopeTop;
    }

    @Nullable
    public MtfHelicopter helicopter() {
        if (HelicopterId == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(HelicopterId) instanceof MtfHelicopter Found && Found.isAlive() ? Found : null;
    }

    @Nullable
    public MtfHelicopter helicopterNear() {
        MtfMission Mission = mission();
        if (Mission == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return MtfMissions.helicopter(World, Mission);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (Reaction > 0) {
            Reaction--;
        }
        byte Current = state();
        if (Current == StateRoping) {
            slide();
        } else if (Current == StateBoarding) {
            climb();
        }
        if (tickCount % 20 == 0) {
            MtfMission Mission = mission();
            if (Mission == null || !Mission.operatives().contains(getUUID())) {
                OrphanTicks += 20;
                if (OrphanTicks >= OrphanLimit) {
                    discard();
                }
            } else {
                OrphanTicks = 0;
            }
        }
    }

    private void slide() {
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        if (RopeHold > 0) {
            RopeHold--;
            return;
        }
        MtfHelicopter Ride = helicopter();
        if (Ride == null) {
            release();
            return;
        }
        double Ground = MtfLandingZone.surface((ServerLevel) level(), getX(), getZ());
        double Gap = getY() - Ground;
        RopeSpeed = Gap < SlideBrake ? Math.max(SlideFloor, RopeSpeed - 0.08) : Math.min(SlideMax, RopeSpeed + SlideAccel);
        double Next = getY() - RopeSpeed;
        if (tickCount % 6 == 0) {
            level().playSound(null, getX(), getY() + 1.0, getZ(), MtfRegistry.RopeSlide.get(), SoundSource.NEUTRAL, 0.45f, 0.9f + getRandom().nextFloat() * 0.2f);
        }
        if (Next <= Ground) {
            setPos(getX(), Ground, getZ());
            land();
            return;
        }
        setPos(getX(), Next, getZ());
    }

    private void climb() {
        setDeltaMovement(Vec3.ZERO);
        resetFallDistance();
        if (RopeHold > 0) {
            RopeHold--;
            return;
        }
        MtfHelicopter Ride = helicopter();
        if (Ride == null) {
            release();
            setState(StateRegroup);
            return;
        }
        double Next = getY() + ClimbSpeed;
        if (tickCount % 8 == 0) {
            level().playSound(null, getX(), getY() + 1.0, getZ(), MtfRegistry.RopeSlide.get(), SoundSource.NEUTRAL, 0.35f, 1.15f);
        }
        if (Next >= RopeTop) {
            MtfMissions.boarded(this);
            discard();
            return;
        }
        setPos(getX(), Next, getZ());
    }

    private void release() {
        setNoGravity(false);
        setState(StateActive);
    }

    private void land() {
        release();
        delayReaction();
        playLayers(MtfRegistry.RopeLandLayers, 0.8f, 1.0f);
    }

    private void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.NEUTRAL, Volume, Pitch);
        }
    }

    @Override
    public boolean isPushable() {
        return !isRoping() && super.isPushable();
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return !isRoping() && super.causeFallDamage(Distance, Multiplier, Source);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (!level().isClientSide) {
            MtfMissions.fallen(this);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel World, DamageSource Source, boolean Recent) {
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (User instanceof ServerPlayer Server && CallerId != null && CallerId.equals(User.getUUID())) {
            MtfMissions.report(Server);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable("message.scp_unleashed.mtf.control_refused");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public float glanceRate() {
        return 0.6f;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity Other) {
        if (CallerId != null && MtfFriends.isFriend(CallerId, Other)) {
            return true;
        }
        return super.isAlliedTo(Other);
    }

    public float accuracy() {
        float Base = tier().accuracy();
        if (getDeltaMovement().horizontalDistanceSqr() > 0.004) {
            Base *= 0.9f;
        }
        return Mth.clamp(Base, 0.05f, 1.0f);
    }

    public BlockPos feet() {
        return blockPosition();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putByte("Tier", entityData.get(TierData));
        Saved.putByte("Role", entityData.get(RoleData));
        if (CallerId != null) {
            Saved.putUUID("Caller", CallerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        entityData.set(TierData, Saved.getByte("Tier"));
        entityData.set(RoleData, Saved.getByte("Role"));
        if (Saved.hasUUID("Caller")) {
            CallerId = Saved.getUUID("Caller");
        }
    }
}
