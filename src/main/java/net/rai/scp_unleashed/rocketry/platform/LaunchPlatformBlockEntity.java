package net.rai.scp_unleashed.rocketry.platform;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.oil.OilRegistry;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LaunchPlatformBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int DataStatus = 0;
    public static final int DataChecks = 1;
    public static final int DataFuel = 2;
    public static final int DataPowerLow = 3;
    public static final int DataPowerHigh = 4;
    public static final int DataReceived = 5;
    public static final int DataDestination = 6;
    public static final int DataCount = 7;
    public static final int MaxStored = 1000000;
    public static final int PowerSplit = 15;
    public static final int PowerMask = (1 << PowerSplit) - 1;
    public static final int CheckTicks = 10;
    public static final int SyncTicks = 10;
    public static final double ReachSquared = 100.0;

    protected final LaunchPlatformKind Kind;
    private final FluidTank Fuel;
    protected int Stored;
    private int Received;
    private int LastReceived;
    protected int Checks;
    protected LaunchStatus Status = LaunchStatus.Empty;
    private boolean NeedsSync = true;
    private long LastSync = Long.MIN_VALUE / 2;
    private long LastCheck = Long.MIN_VALUE / 2;
    private boolean LampsKnown;
    private boolean LampsLit;
    private boolean SkyKnown;
    private boolean SkyOpen;
    private long SkyPrint;
    @Nullable
    private List<BlockPos> SkyColumns;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataStatus -> Status.ordinal();
                case DataChecks -> Checks;
                case DataFuel -> Fuel.getFluidAmount();
                case DataPowerLow -> Stored & PowerMask;
                case DataPowerHigh -> Stored >> PowerSplit;
                case DataReceived -> LastReceived;
                case DataDestination -> destinationIndex();
                default -> 0;
            };
        }

        @Override
        public void set(int Index, int Value) {
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    };

    protected LaunchPlatformBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State, LaunchPlatformKind Kind) {
        super(Type, Pos, State);
        this.Kind = Kind;
        this.Fuel = new FluidTank(Kind.getFuelCapacity(), OilRegistry.HeavyFuel::is) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    public LaunchPlatformKind getKind() {
        return Kind;
    }

    public FluidTank getFuel() {
        return Fuel;
    }

    public IFluidHandler getFuelHandler() {
        return Fuel;
    }

    @Nullable
    public IItemHandler getItemHandler() {
        return null;
    }

    public int getStored() {
        return Stored;
    }

    public void setStored(int Amount) {
        Stored = Math.max(0, Math.min(MaxStored, Amount));
        setChanged();
    }

    public int fuelCost() {
        return Kind.getFuelCost();
    }

    public int powerCost() {
        return Kind.getPowerCost();
    }

    public int powerCapacity() {
        return Kind.getPowerCapacity();
    }

    protected int destinationIndex() {
        return -1;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    public int getChecks() {
        return Checks;
    }

    public LaunchStatus getStatus() {
        return Status;
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.hasProperty(LaunchPlatformBlock.Facing) ? State.getValue(LaunchPlatformBlock.Facing) : Direction.NORTH;
    }

    @Nullable
    public LaunchPlatformBlock platformBlock() {
        return getBlockState().getBlock() instanceof LaunchPlatformBlock Platform ? Platform : null;
    }

    public Vec3 standAt() {
        LaunchPlatformBlock Platform = platformBlock();
        return Platform == null ? Vec3.atBottomCenterOf(worldPosition) : Platform.standAt(worldPosition, getFront());
    }

    public Vec3 getCentre() {
        LaunchPlatformBlock Platform = platformBlock();
        return Platform == null ? Vec3.atCenterOf(worldPosition) : Platform.layout().centre(worldPosition, getFront());
    }

    public boolean hasFuelFor(int Cost) {
        return Fuel.getFluidAmount() >= Cost;
    }

    public void burnFuel(int Amount) {
        Fuel.drain(Amount, IFluidHandler.FluidAction.EXECUTE);
    }

    public boolean hasPowerFor(int Cost) {
        return Stored >= Cost;
    }

    public void drainPower(int Amount) {
        Stored = Math.max(0, Stored - Amount);
        setChanged();
    }

    protected abstract int computeChecks(ServerLevel World);

    protected abstract List<BlockPos> buildSkyColumns();

    protected abstract int skyFloor();

    public List<BlockPos> skyColumns() {
        if (SkyColumns == null) {
            SkyColumns = List.copyOf(buildSkyColumns());
        }
        return SkyColumns;
    }

    public boolean skyClear(ServerLevel World) {
        LaunchPlatformBlock Platform = platformBlock();
        if (Platform == null) {
            return false;
        }
        List<BlockPos> Columns = skyColumns();
        long Print = LaunchSky.print(World, Columns);
        if (!SkyKnown || Print != SkyPrint) {
            SkyOpen = LaunchSky.clear(World, Columns, worldPosition.getY() + skyFloor(), Spot -> isOwnCell(World.getBlockState(Spot), worldPosition, Platform, Spot));
            SkyPrint = Print;
            SkyKnown = true;
        }
        return SkyOpen;
    }

    public void forgetSky() {
        SkyKnown = false;
    }

    protected abstract boolean hasPayload();

    protected abstract boolean isLaunching();

    protected abstract boolean beginLaunch(ServerLevel World, @Nullable Player User);

    public int refreshChecks(ServerLevel World) {
        Checks = computeChecks(World);
        LastCheck = World.getGameTime();
        updateStatus();
        return Checks;
    }

    private void updateStatus() {
        LaunchStatus Next;
        if (isLaunching()) {
            Next = LaunchStatus.Launching;
        } else if (!hasPayload()) {
            Next = LaunchStatus.Empty;
        } else if ((Checks & Kind.requiredMask()) == Kind.requiredMask()) {
            Next = LaunchStatus.Ready;
        } else {
            Next = LaunchStatus.NotReady;
        }
        if (Next != Status) {
            Status = Next;
            NeedsSync = true;
        }
    }

    @Nullable
    public LaunchCheck firstMissing() {
        for (LaunchCheck Check : Kind.getChecks()) {
            if (!Check.passes(Checks)) {
                return Check;
            }
        }
        return null;
    }

    public boolean tryLaunch(ServerLevel World, @Nullable Player User) {
        if (isLaunching()) {
            return false;
        }
        forgetSky();
        refreshChecks(World);
        LaunchCheck Missing = firstMissing();
        if (Missing != null) {
            if (User != null) {
                User.displayClientMessage(Component.translatable(Missing.failKey()).withStyle(ChatFormatting.RED), true);
            }
            Vec3 Centre = getCentre();
            World.playSound(null, Centre.x, Centre.y, Centre.z, RocketryRegistry.Refused.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
            return false;
        }
        boolean Started = beginLaunch(World, User);
        refreshChecks(World);
        NeedsSync = true;
        setChanged();
        return Started;
    }

    public static boolean isOwnCell(BlockState State, BlockPos Origin, LaunchPlatformBlock Platform, BlockPos At) {
        return State.is(Platform) && Platform.originOf(At, State).equals(Origin);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, LaunchPlatformBlockEntity Platform) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof LaunchPlatformBlock) {
            Platform.tickServer(ServerWorld, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, LaunchPlatformBlockEntity Platform) {
        Platform.tickClient();
    }

    protected void tickClient() {
    }

    private void tickServer(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        LastReceived = Received;
        Received = 0;
        if (Now - LastCheck >= CheckTicks) {
            refreshChecks(World);
        }
        boolean Lit = Status == LaunchStatus.Ready || Status == LaunchStatus.Launching;
        if (!LampsKnown || LampsLit != Lit) {
            LampsKnown = true;
            LampsLit = Lit;
            LaunchPlatformBlock.applyActive(World, worldPosition, State, Lit);
        }
        tickPlatform(World, Now);
        if (NeedsSync && Now - LastSync >= SyncTicks) {
            NeedsSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    protected void tickPlatform(ServerLevel World, long Now) {
    }

    protected void markForSync() {
        NeedsSync = true;
    }

    @Override
    public int getPowerDemand() {
        int Room = powerCapacity() - Stored;
        return Room <= 0 ? 0 : Math.min(Kind.getChargeRate(), Room);
    }

    @Override
    public void insertPower(int Amount) {
        if (Amount <= 0) {
            return;
        }
        int Taken = Math.min(Amount, Math.max(0, powerCapacity() - Stored));
        Stored += Taken;
        Received += Amount;
        setChanged();
    }

    @Override
    public int getStoredPower() {
        return Stored;
    }

    @Override
    public int getPowerCapacity() {
        return powerCapacity();
    }

    public void dropContents(Level World, BlockPos Pos) {
    }

    protected void saveExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
    }

    protected void loadExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
    }

    protected void writeSync(CompoundTag Tag, HolderLookup.Provider Registries) {
    }

    protected void readSync(CompoundTag Tag, HolderLookup.Provider Registries) {
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Fuel", Fuel.writeToNBT(Registries, new CompoundTag()));
        Tag.putInt("Stored", Stored);
        saveExtra(Tag, Registries);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            Status = LaunchStatus.byName(Tag.getString("Status"));
            readSync(Tag, Registries);
            return;
        }
        if (Tag.contains("Fuel")) {
            Fuel.readFromNBT(Registries, Tag.getCompound("Fuel"));
        }
        Stored = Math.max(0, Math.min(MaxStored, Tag.getInt("Stored")));
        loadExtra(Tag, Registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        Tag.putString("Status", Status.getSerializedName());
        writeSync(Tag, Registries);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(Kind.getTitleKey());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        if (level instanceof ServerLevel World) {
            forgetSky();
            refreshChecks(World);
        }
        return new LaunchPlatformMenu(Kind, ContainerId, PlayerInventory, this, Data, getItemHandler());
    }

    public boolean isUsableBy(Player User) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        Vec3 Centre = getCentre();
        return User.distanceToSqr(Centre.x, Centre.y, Centre.z) <= ReachSquared;
    }

    public void pressLaunch(ServerPlayer User) {
        if (level instanceof ServerLevel World) {
            tryLaunch(World, User);
        }
    }

    public void pressCycle(ServerPlayer User, int Step) {
    }

    public static boolean acceptsFuel(FluidStack Stack) {
        return OilRegistry.HeavyFuel.is(Stack);
    }
}
