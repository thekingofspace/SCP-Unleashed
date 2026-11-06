package net.rai.scp_unleashed.rocketry.rocket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.rocketry.RocketryBursts;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DropPodEntity extends Entity {
    public static final float Width = 2.0f;
    public static final float Height = 2.6f;
    public static final double SeatHeight = 0.35;
    public static final double EarthGravity = 0.04;
    public static final float LowGravity = 0.6f;
    public static final double MaxClimb = 0.3;
    public static final int ThrusterTicks = 100;
    public static final double SoftLanding = 0.45;
    public static final double SafeDescent = 0.35;
    public static final int EjectWindow = 60;
    public static final double DismountReach = 1.8;
    public static final double RenderReach = 256.0;
    public static final byte ImpactNone = 0;
    public static final byte ImpactSoft = 1;
    public static final byte ImpactHard = 2;

    public enum Kit {
        Plain,
        Return;

        public static Kit byName(String Name) {
            return "return".equals(Name) ? Return : Plain;
        }

        public String getSerializedName() {
            return this == Return ? "return" : "plain";
        }
    }

    private static final EntityDataAccessor<Boolean> DataLanded = SynchedEntityData.defineId(DropPodEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DataThrusting = SynchedEntityData.defineId(DropPodEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DataImpact = SynchedEntityData.defineId(DropPodEntity.class, EntityDataSerializers.BYTE);
    private static Consumer<DropPodEntity> ClientEffects = Pod -> {
    };

    private Kit Contents = Kit.Plain;
    @Nullable
    private GlobalPos ReturnSite;
    private int RocketFuel;
    private int ThrusterFuel = ThrusterTicks;
    private boolean AutoBrake;
    private boolean Throttle;
    private boolean ShiftHeld;
    private long EjectArmedUntil = Long.MIN_VALUE / 2;
    private int LerpSteps;
    private double LerpX;
    private double LerpY;
    private double LerpZ;

    public DropPodEntity(EntityType<? extends DropPodEntity> Type, Level World) {
        super(Type, World);
        this.blocksBuilding = true;
    }

    public static void setClientEffects(Consumer<DropPodEntity> Effects) {
        ClientEffects = Effects;
    }

    public static DropPodEntity falling(ServerLevel World, Vec3 At, float Yaw, Kit Contents, @Nullable GlobalPos ReturnSite, int RocketFuel) {
        DropPodEntity Pod = new DropPodEntity(RocketryRegistry.DropPodType.get(), World);
        Pod.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Pod.Contents = Contents;
        Pod.ReturnSite = ReturnSite;
        Pod.RocketFuel = Math.max(0, RocketFuel);
        Pod.setDeltaMovement(0.0, -0.2, 0.0);
        return Pod;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataLanded, false);
        Builder.define(DataThrusting, false);
        Builder.define(DataImpact, ImpactNone);
    }

    public boolean isLanded() {
        return entityData.get(DataLanded);
    }

    public boolean isThrusting() {
        return entityData.get(DataThrusting);
    }

    public byte getImpact() {
        return entityData.get(DataImpact);
    }

    public Kit getContents() {
        return Contents;
    }

    public int getRocketFuel() {
        return RocketFuel;
    }

    @Nullable
    public GlobalPos getReturnSite() {
        return ReturnSite;
    }

    public int getThrusterFuel() {
        return ThrusterFuel;
    }

    public boolean isAutoBraking() {
        return AutoBrake;
    }

    public void setAutoBrake(boolean Wanted) {
        AutoBrake = Wanted;
    }

    public float gravityScale() {
        return RocketDestinations.gravity(level());
    }

    public double gravity() {
        return EarthGravity * gravityScale();
    }

    public double terminal() {
        return 1.2 + 1.2 * gravityScale();
    }

    public double thrust() {
        return 0.03 + 0.07 * gravityScale();
    }

    public void setThrottle(boolean Pressed) {
        Throttle = Pressed;
    }

    public List<ItemStack> salvage() {
        List<ItemStack> Parts = new ArrayList<>();
        ItemStack Rocket = new ItemStack(RocketryRegistry.Rocket.get());
        if (RocketFuel > 0) {
            Rocket.set(RocketryRegistry.RocketFuel.get(), RocketFuel);
        }
        if (Contents == Kit.Return && ReturnSite != null) {
            Rocket.set(RocketryRegistry.ReturnSite.get(), ReturnSite);
        }
        Parts.add(Rocket);
        if (Contents == Kit.Return) {
            Parts.add(new ItemStack(RocketryRegistry.PortableLaunchPadItem.get()));
        }
        return Parts;
    }

    public boolean breakApart() {
        if (isRemoved() || !(level() instanceof ServerLevel World) || !isLanded() || !getPassengers().isEmpty()) {
            return false;
        }
        List<ItemStack> Parts = salvage();
        World.broadcastEntityEvent(this, RocketryBursts.Salvage);
        discard();
        for (ItemStack Part : Parts) {
            spawnAtLocation(Part, 0.6f);
        }
        World.playSound(null, getX(), getY() + 1.0, getZ(), RocketryRegistry.PodBreak.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (LerpSteps > 0) {
                double X = getX() + (LerpX - getX()) / LerpSteps;
                double Y = getY() + (LerpY - getY()) / LerpSteps;
                double Z = getZ() + (LerpZ - getZ()) / LerpSteps;
                LerpSteps--;
                setPos(X, Y, Z);
            }
            ClientEffects.accept(this);
            return;
        }
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        holdRiders(World);
        if (tickCount == RocketEntity.HintTicks && !isLanded()) {
            tellRiders(Component.translatable("message.scp_unleashed.rocketry.pod_released").withStyle(ChatFormatting.AQUA));
        }
        if (isLanded()) {
            settle();
        } else {
            fall(World);
        }
    }

    private boolean hasPilot() {
        for (Entity Rider : getPassengers()) {
            if (Rider instanceof Player) {
                return true;
            }
        }
        return false;
    }

    private void fall(ServerLevel World) {
        Vec3 Motion = getDeltaMovement();
        double Climb = Motion.y - gravity();
        boolean Firing = Throttle && ThrusterFuel > 0 && hasPilot();
        if (Firing) {
            Climb += thrust();
            ThrusterFuel--;
            if (ThrusterFuel % 10 == 0) {
                tellRiders(ThrusterFuel <= 0 ? Component.translatable("message.scp_unleashed.rocketry.thrusters_empty").withStyle(ChatFormatting.RED)
                        : Component.translatable("message.scp_unleashed.rocketry.thrusters", Math.round(ThrusterFuel * 100.0f / ThrusterTicks)).withStyle(ChatFormatting.AQUA));
            }
        }
        if (AutoBrake) {
            Climb = Math.max(Climb, -SafeDescent);
        }
        Climb = Mth.clamp(Climb, -terminal(), MaxClimb);
        if (Firing != isThrusting()) {
            entityData.set(DataThrusting, Firing);
        }
        setDeltaMovement(Motion.x * 0.9, Climb, Motion.z * 0.9);
        double Speed = -Climb;
        fallDistance = 0.0f;
        move(MoverType.SELF, getDeltaMovement());
        if (onGround() || verticalCollisionBelow || isInWater() || isInLava()) {
            land(World, Speed);
        }
    }

    private void land(ServerLevel World, double Speed) {
        boolean Hard = Speed > SoftLanding;
        entityData.set(DataLanded, true);
        entityData.set(DataThrusting, false);
        entityData.set(DataImpact, Hard ? ImpactHard : ImpactSoft);
        Throttle = false;
        AutoBrake = false;
        setDeltaMovement(Vec3.ZERO);
        float Volume = Hard ? 2.4f : 1.4f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : RocketryRegistry.LandLayers) {
            World.playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.BLOCKS, Volume, Hard ? 0.75f : 0.95f);
        }
        World.broadcastEntityEvent(this, Hard ? RocketryBursts.LandHard : RocketryBursts.LandSoft);
        tellRiders(Component.translatable("message.scp_unleashed.rocketry.landed").withStyle(ChatFormatting.GREEN));
    }

    private void settle() {
        Vec3 Motion;
        if (isInWater() || isInLava()) {
            Motion = new Vec3(0.0, 0.06, 0.0);
        } else {
            Motion = new Vec3(0.0, Math.max(-1.0, getDeltaMovement().y - gravity()), 0.0);
        }
        setDeltaMovement(Motion);
        move(MoverType.SELF, Motion);
        if (onGround()) {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private void holdRiders(ServerLevel World) {
        boolean Pressed = false;
        for (Entity Rider : getPassengers()) {
            if (!(Rider instanceof Player Seated) || !Seated.isShiftKeyDown()) {
                continue;
            }
            Pressed = true;
            if (isLanded()) {
                continue;
            }
            long Now = World.getGameTime();
            boolean Rising = !ShiftHeld;
            if (Rising && Now <= EjectArmedUntil) {
                EjectArmedUntil = Long.MIN_VALUE / 2;
                Seated.displayClientMessage(Component.translatable("message.scp_unleashed.rocketry.ejected").withStyle(ChatFormatting.RED), true);
                World.playSound(null, getX(), getY() + 1.0, getZ(), RocketryRegistry.PodOpen.get(), SoundSource.BLOCKS, 1.0f, 1.2f);
                continue;
            }
            Seated.setShiftKeyDown(false);
            if (Rising) {
                EjectArmedUntil = Now + EjectWindow;
                Seated.displayClientMessage(Component.translatable(gravityScale() < LowGravity ? "message.scp_unleashed.rocketry.eject_warning_moon"
                        : "message.scp_unleashed.rocketry.eject_warning").withStyle(ChatFormatting.GOLD), true);
            }
        }
        ShiftHeld = Pressed;
    }

    private void tellRiders(Component Text) {
        for (Entity Rider : getPassengers()) {
            if (Rider instanceof Player Seated) {
                Seated.displayClientMessage(Text, true);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte Id) {
        if (RocketryBursts.handles(Id)) {
            if (Id == RocketryBursts.LandSoft || Id == RocketryBursts.LandHard) {
                LerpSteps = 0;
            }
            RocketryBursts.play(this, Id);
            return;
        }
        super.handleEntityEvent(Id);
    }

    @Override
    public void lerpTo(double X, double Y, double Z, float YRot, float XRot, int Steps) {
        LerpX = X;
        LerpY = Y;
        LerpZ = Z;
        LerpSteps = 3;
        setRot(YRot, XRot);
    }

    @Override
    public InteractionResult interact(Player User, InteractionHand Hand) {
        if (User.isSecondaryUseActive() || !isLanded() || !getPassengers().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            User.startRiding(this);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(Source)) {
            return false;
        }
        if (!(Source.getEntity() instanceof Player Breaker) || Source.getDirectEntity() != Breaker) {
            return false;
        }
        if (!isLanded()) {
            return false;
        }
        if (!getPassengers().isEmpty()) {
            Breaker.displayClientMessage(Component.translatable("message.scp_unleashed.rocketry.occupied").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        return breakApart();
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        fallDistance = 0.0f;
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return isLanded();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity Passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity Passenger, EntityDimensions Size, float PartialTick) {
        return new Vec3(0.0, SeatHeight, 0.0);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity Passenger) {
        float Facing = getYRot();
        for (int Step = 0; Step < 8; Step++) {
            double Angle = Math.toRadians(Facing + Step * 45.0);
            Vec3 Spot = new Vec3(getX() - Math.sin(Angle) * DismountReach, getY(), getZ() + Math.cos(Angle) * DismountReach);
            for (int Rise = -1; Rise < 2; Rise++) {
                Vec3 Found = DismountHelper.findSafeDismountLocation(Passenger.getType(), level(), BlockPos.containing(Spot.x, Spot.y + Rise, Spot.z), true);
                if (Found != null) {
                    return new Vec3(Spot.x, Found.y, Spot.z);
                }
            }
        }
        return new Vec3(getX(), getY() + Height, getZ());
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RocketryRegistry.Rocket.get());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < RenderReach * RenderReach;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        boolean Landed = Tag.getBoolean("Landed");
        entityData.set(DataLanded, Landed);
        entityData.set(DataImpact, Tag.getByte("Impact"));
        Contents = Kit.byName(Tag.getString("Kit"));
        ReturnSite = RocketEntity.readSite(Tag, "ReturnSite");
        RocketFuel = Math.max(0, Tag.getInt("RocketFuel"));
        ThrusterFuel = Tag.contains("ThrusterFuel") ? Mth.clamp(Tag.getInt("ThrusterFuel"), 0, ThrusterTicks) : ThrusterTicks;
        AutoBrake = !Landed;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putBoolean("Landed", isLanded());
        Tag.putByte("Impact", getImpact());
        Tag.putString("Kit", Contents.getSerializedName());
        RocketEntity.writeSite(Tag, "ReturnSite", ReturnSite);
        Tag.putInt("RocketFuel", RocketFuel);
        Tag.putInt("ThrusterFuel", ThrusterFuel);
    }
}
