package net.rai.scp_unleashed.rocketry.rocket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.rocketry.RocketryBursts;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.platform.LaunchCheck;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformBlockEntity;
import net.rai.scp_unleashed.rocketry.platform.PortableLaunchPadBlock;
import net.rai.scp_unleashed.rocketry.platform.RocketPlatformBlock;
import net.rai.scp_unleashed.rocketry.platform.RocketPlatformBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class RocketEntity extends Entity {
    public static final float Width = 2.6f;
    public static final float Height = 8.0f;
    public static final double SeatHeight = 3.9;
    public static final int CountdownTicks = 100;
    public static final int FlightTicks = 240;
    public static final double Climb = 0.0085;
    public static final double DismountReach = 2.0;
    public static final double RenderReach = 320.0;
    public static final byte PhaseIdle = 0;
    public static final byte PhaseCountdown = 1;
    public static final byte PhaseFlight = 2;
    public static final int PadCheckTicks = 20;
    public static final int HintTicks = 25;
    public static final int ReserveTicks = 200;
    public static final int ArrivalWait = 200;

    private static final EntityDataAccessor<Byte> DataPhase = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DataTicks = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DataStandY = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    private static Consumer<RocketEntity> ClientEffects = Rocket -> {
    };

    private int PhaseTicks;
    private int StoredFuel;
    @Nullable
    private GlobalPos ReturnSite;
    @Nullable
    private GlobalPos LaunchSite;
    @Nullable
    private BlockPos PadPos;
    @Nullable
    private ResourceLocation DestinationId;
    private boolean Portable;
    private boolean Throttle;
    @Nullable
    private RocketTravel.Arrival Arrival;
    private int CrewTicks;
    private long LastRefusal = Long.MIN_VALUE / 2;

    public RocketEntity(EntityType<? extends RocketEntity> Type, Level World) {
        super(Type, World);
        this.blocksBuilding = true;
        this.noPhysics = true;
        setNoGravity(true);
    }

    public static void setClientEffects(Consumer<RocketEntity> Effects) {
        ClientEffects = Effects;
    }

    public static RocketEntity standing(ServerLevel World, Vec3 At, float Yaw, ItemStack Stack, BlockPos Pad, boolean OnPortable) {
        RocketEntity Rocket = new RocketEntity(RocketryRegistry.RocketType.get(), World);
        Rocket.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Rocket.entityData.set(DataStandY, (float) At.y);
        Rocket.StoredFuel = Math.max(0, Stack.getOrDefault(RocketryRegistry.RocketFuel.get(), 0));
        Rocket.ReturnSite = Stack.get(RocketryRegistry.ReturnSite.get());
        Rocket.PadPos = Pad.immutable();
        Rocket.Portable = OnPortable;
        return Rocket;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataPhase, PhaseIdle);
        Builder.define(DataTicks, 0);
        Builder.define(DataStandY, 0.0f);
    }

    public byte getPhase() {
        return entityData.get(DataPhase);
    }

    public int getPhaseTicks() {
        return PhaseTicks;
    }

    public float getPhaseTime(float PartialTick) {
        return PhaseTicks + PartialTick;
    }

    public boolean isLaunching() {
        return getPhase() != PhaseIdle;
    }

    public boolean isOnPad() {
        return getPhase() != PhaseFlight;
    }

    public boolean hasCrew() {
        return !getPassengers().isEmpty();
    }

    public int getStoredFuel() {
        return StoredFuel;
    }

    @Nullable
    public GlobalPos getReturnSite() {
        return ReturnSite;
    }

    @Nullable
    public GlobalPos getLaunchSite() {
        return LaunchSite;
    }

    @Nullable
    public BlockPos getPadPos() {
        return PadPos;
    }

    public boolean isPortable() {
        return Portable;
    }

    @Nullable
    public ResourceLocation getDestinationId() {
        return DestinationId;
    }

    public double getStandY() {
        return entityData.get(DataStandY);
    }

    public static double altitude(double Ticks) {
        return Ticks <= 0.0 ? 0.0 : 0.5 * Climb * Ticks * Ticks;
    }

    public double flightY(double Ticks) {
        return getStandY() + altitude(Ticks);
    }

    private void setPhase(byte Phase) {
        PhaseTicks = 0;
        entityData.set(DataPhase, Phase);
        entityData.set(DataTicks, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (level().isClientSide && (Key.equals(DataTicks) || Key.equals(DataPhase))) {
            PhaseTicks = entityData.get(DataTicks);
        }
    }

    public ItemStack toItem() {
        ItemStack Stack = new ItemStack(RocketryRegistry.Rocket.get());
        if (StoredFuel > 0) {
            Stack.set(RocketryRegistry.RocketFuel.get(), StoredFuel);
        }
        if (ReturnSite != null) {
            Stack.set(RocketryRegistry.ReturnSite.get(), ReturnSite);
        }
        return Stack;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(RocketryRegistry.Rocket.get());
    }

    public void setThrottle(boolean Pressed, Player From) {
        boolean Rising = Pressed && !Throttle;
        Throttle = Pressed;
        if (Rising && getPhase() == PhaseIdle) {
            requestLaunch(From);
        }
    }

    public boolean requestLaunch(@Nullable Player From) {
        if (!(level() instanceof ServerLevel World) || getPhase() != PhaseIdle || isRemoved()) {
            return false;
        }
        if (PadPos == null) {
            popOff(null);
            return false;
        }
        BlockState Pad = World.getBlockState(PadPos);
        if (Pad.getBlock() instanceof RocketPlatformBlock && World.getBlockEntity(PadPos) instanceof RocketPlatformBlockEntity Platform) {
            return Platform.tryLaunch(World, From);
        }
        if (Pad.getBlock() instanceof PortableLaunchPadBlock Folding) {
            int Mask = Folding.checks(World, PadPos, Pad, this);
            for (LaunchCheck Check : PortableLaunchPadBlock.Checks) {
                if (!Check.passes(Mask)) {
                    refuse(From, Check);
                    return false;
                }
            }
            RocketDestination Route = RocketDestinations.portableRoute(World.getServer(), World.dimension());
            if (Route == null) {
                refuse(From, LaunchCheck.Route);
                return false;
            }
            ignite(null, Route);
            return true;
        }
        popOff(null);
        return false;
    }

    private void refuse(@Nullable Player From, LaunchCheck Check) {
        long Now = level().getGameTime();
        if (From != null) {
            From.displayClientMessage(Component.translatable(Check.failKey()).withStyle(ChatFormatting.RED), true);
        }
        if (Now - LastRefusal > 10) {
            LastRefusal = Now;
            level().playSound(null, getX(), getY() + 1.0, getZ(), RocketryRegistry.Refused.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
    }

    public void ignite(@Nullable LaunchPlatformBlockEntity Platform, RocketDestination Goal) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        int Cost = Goal.Fuel();
        if (Platform == null || StoredFuel >= Cost) {
            StoredFuel = Math.max(0, StoredFuel - Cost);
        } else {
            Platform.burnFuel(Cost);
        }
        if (Platform != null) {
            Platform.drainPower(Goal.Power());
        }
        DestinationId = Goal.Id();
        LaunchSite = GlobalPos.of(World.dimension(), BlockPos.containing(getX(), getStandY(), getZ()));
        for (Entity Rider : getPassengers()) {
            RocketTravel.recordLaunch(Rider, LaunchSite);
        }
        entityData.set(DataStandY, (float) getY());
        setPhase(PhaseCountdown);
        play(World, RocketryRegistry.IgnitionLayers, 2.0f, 1.0f);
        Arrival = RocketTravel.plan(this);
        RocketTravel.reserve(Arrival);
    }

    @Nullable
    public RocketTravel.Arrival getArrival() {
        return Arrival;
    }

    private void keepArrival() {
        if (Arrival == null) {
            Arrival = RocketTravel.plan(this);
            RocketTravel.reserve(Arrival);
        } else if (PhaseTicks % ReserveTicks == 0) {
            RocketTravel.reserve(Arrival);
        }
    }

    private void play(ServerLevel World, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, getX(), getY() + 1.0, getZ(), Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    public void popOff(@Nullable Player Breaker) {
        if (isRemoved() || !(level() instanceof ServerLevel World)) {
            return;
        }
        ejectPassengers();
        ItemStack Dropped = toItem();
        discard();
        spawnAtLocation(Dropped, 1.0f);
        World.playSound(null, getX(), getY() + 1.0, getZ(), RocketryRegistry.PodBreak.get(), SoundSource.BLOCKS, 1.0f, 1.1f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickClient();
            return;
        }
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        byte Phase = getPhase();
        holdRiders(Phase);
        if (Phase == PhaseIdle) {
            setPos(getX(), getStandY(), getZ());
            CrewTicks = hasCrew() ? CrewTicks + 1 : 0;
            if (CrewTicks == HintTicks) {
                for (Entity Rider : getPassengers()) {
                    if (Rider instanceof Player Seated) {
                        Seated.displayClientMessage(Component.translatable("message.scp_unleashed.rocketry.boarded").withStyle(ChatFormatting.AQUA), true);
                    }
                }
            }
            if (tickCount % PadCheckTicks == 0 && !padStillThere(World)) {
                popOff(null);
            }
            return;
        }
        PhaseTicks++;
        if (PhaseTicks % 20 == 0) {
            entityData.set(DataTicks, PhaseTicks);
        }
        keepArrival();
        if (Phase == PhaseCountdown) {
            setPos(getX(), getStandY(), getZ());
            if (PhaseTicks % 20 == 0 && PhaseTicks < CountdownTicks) {
                World.playSound(null, getX(), getStandY() + 1.0, getZ(), RocketryRegistry.Countdown.get(), SoundSource.BLOCKS, 1.6f,
                        0.9f + 0.1f * (PhaseTicks / 20));
            }
            if (PhaseTicks >= CountdownTicks) {
                liftoff(World);
            }
            return;
        }
        setPos(getX(), flightY(PhaseTicks), getZ());
        if (PhaseTicks >= FlightTicks && (RocketTravel.ready(Arrival) || PhaseTicks >= FlightTicks + ArrivalWait)) {
            RocketTravel.transfer(this);
        }
    }

    private void liftoff(ServerLevel World) {
        setPhase(PhaseFlight);
        play(World, RocketryRegistry.LiftoffLayers, 4.0f, 0.9f);
        World.playSound(null, getX(), getStandY(), getZ(), RocketryRegistry.Clamps.get(), SoundSource.BLOCKS, 1.4f, 0.8f);
        World.broadcastEntityEvent(this, RocketryBursts.Liftoff);
    }

    private void holdRiders(byte Phase) {
        if (Phase == PhaseIdle) {
            return;
        }
        for (Entity Rider : getPassengers()) {
            if (Rider instanceof Player Seated && Seated.isShiftKeyDown()) {
                Seated.setShiftKeyDown(false);
                if (tickCount % 20 == 0) {
                    Seated.displayClientMessage(Component.translatable("message.scp_unleashed.rocketry.strapped_in").withStyle(ChatFormatting.GOLD), true);
                }
            }
        }
    }

    private boolean padStillThere(ServerLevel World) {
        if (PadPos == null || !World.isLoaded(PadPos)) {
            return PadPos != null;
        }
        BlockState Pad = World.getBlockState(PadPos);
        if (Portable) {
            return Pad.getBlock() instanceof PortableLaunchPadBlock Folding && Folding.isMaster(Pad);
        }
        return Pad.getBlock() instanceof RocketPlatformBlock Platform && Platform.isMaster(Pad);
    }

    private void tickClient() {
        byte Phase = getPhase();
        if (Phase != PhaseIdle) {
            PhaseTicks++;
        }
        if (Phase == PhaseFlight) {
            double Y = flightY(PhaseTicks);
            setPos(getX(), Y, getZ());
        }
        ClientEffects.accept(this);
    }

    @Override
    public void handleEntityEvent(byte Id) {
        if (RocketryBursts.handles(Id)) {
            RocketryBursts.play(this, Id);
            return;
        }
        super.handleEntityEvent(Id);
    }

    @Override
    public void lerpTo(double X, double Y, double Z, float YRot, float XRot, int Steps) {
        if (getPhase() == PhaseFlight) {
            setRot(YRot, XRot);
            return;
        }
        super.lerpTo(X, Y, Z, YRot, XRot, Steps);
    }

    @Override
    public InteractionResult interact(Player User, InteractionHand Hand) {
        if (User.isSecondaryUseActive() || getPhase() != PhaseIdle || !getPassengers().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            if (User.startRiding(this)) {
                level().playSound(null, getX(), getY() + SeatHeight, getZ(), RocketryRegistry.Board.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
            }
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
        if (getPhase() != PhaseIdle) {
            return false;
        }
        if (!getPassengers().isEmpty()) {
            Breaker.displayClientMessage(Component.translatable("message.scp_unleashed.rocketry.occupied").withStyle(ChatFormatting.GOLD), true);
            return false;
        }
        popOff(Breaker);
        return true;
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
        return getPhase() == PhaseIdle;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    protected boolean canAddPassenger(Entity Passenger) {
        return getPassengers().isEmpty() && getPhase() == PhaseIdle;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity Passenger, EntityDimensions Size, float PartialTick) {
        return new Vec3(0.0, SeatHeight, 0.0);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity Passenger) {
        double Base = getStandY();
        float Facing = getYRot();
        for (int Step = 0; Step < 8; Step++) {
            double Angle = Math.toRadians(Facing + 180.0 + Step * 45.0);
            Vec3 Spot = new Vec3(getX() - Math.sin(Angle) * DismountReach, Base, getZ() + Math.cos(Angle) * DismountReach);
            for (int Rise = 0; Rise < 3; Rise++) {
                Vec3 Found = DismountHelper.findSafeDismountLocation(Passenger.getType(), level(), BlockPos.containing(Spot.x, Spot.y + Rise, Spot.z), true);
                if (Found != null) {
                    return new Vec3(Spot.x, Found.y, Spot.z);
                }
            }
        }
        return new Vec3(getX(), getY() + Height, getZ());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < RenderReach * RenderReach;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        PhaseTicks = Tag.getInt("PhaseTicks");
        byte Phase = (byte) Mth.clamp(Tag.getByte("Phase"), PhaseIdle, PhaseFlight);
        entityData.set(DataPhase, Phase);
        entityData.set(DataStandY, Tag.contains("StandY") ? Tag.getFloat("StandY") : (float) getY());
        StoredFuel = Math.max(0, Tag.getInt("StoredFuel"));
        ReturnSite = readSite(Tag, "ReturnSite");
        LaunchSite = readSite(Tag, "LaunchSite");
        PadPos = NbtUtils.readBlockPos(Tag, "Pad").orElse(null);
        DestinationId = Tag.contains("Destination") ? ResourceLocation.tryParse(Tag.getString("Destination")) : null;
        Portable = Tag.getBoolean("Portable");
        if (Phase == PhaseFlight) {
            PhaseTicks = Math.max(PhaseTicks, FlightTicks - 20);
        }
        entityData.set(DataTicks, PhaseTicks);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putByte("Phase", getPhase());
        Tag.putInt("PhaseTicks", PhaseTicks);
        Tag.putFloat("StandY", (float) getStandY());
        Tag.putInt("StoredFuel", StoredFuel);
        writeSite(Tag, "ReturnSite", ReturnSite);
        writeSite(Tag, "LaunchSite", LaunchSite);
        if (PadPos != null) {
            Tag.put("Pad", NbtUtils.writeBlockPos(PadPos));
        }
        Tag.putBoolean("Portable", Portable);
        if (DestinationId != null) {
            Tag.putString("Destination", DestinationId.toString());
        }
    }

    @Nullable
    public static GlobalPos readSite(CompoundTag Tag, String Key) {
        if (!Tag.contains(Key)) {
            return null;
        }
        return GlobalPos.CODEC.parse(NbtOps.INSTANCE, Tag.get(Key)).result().orElse(null);
    }

    public static void writeSite(CompoundTag Tag, String Key, @Nullable GlobalPos Site) {
        if (Site != null) {
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, Site).result().ifPresent(Encoded -> Tag.put(Key, Encoded));
        }
    }
}
