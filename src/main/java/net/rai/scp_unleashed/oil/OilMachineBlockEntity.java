package net.rai.scp_unleashed.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.RecipeManager;
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
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class OilMachineBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int DataStatus = 0;
    public static final int DataProgress = 1;
    public static final int DataPower = 2;
    public static final int DataPowerMax = 3;
    public static final int DataGas = 4;
    public static final int DataTanks = 5;
    public static final int MaxTanks = 3;
    public static final int DataCount = DataTanks + MaxTanks * 2;
    public static final int ProgressScale = 1000;
    public static final int RateSteps = 16;
    public static final int SyncTicks = 5;
    public static final int ActiveHold = 10;
    public static final double ReachSquared = 100.0;

    private static Consumer<OilMachineBlockEntity> ClientEffects = Machine -> {
    };

    protected final OilMachineKind Kind;
    private final List<FluidTank> Tanks = new ArrayList<>();
    private final List<PortRole> Roles = new ArrayList<>();
    private final Map<Integer, IFluidHandler> CellHandlers = new HashMap<>();
    private final IFluidHandler Combined = new IFluidHandler() {
        @Override
        public int getTanks() {
            return Tanks.size();
        }

        @Override
        public FluidStack getFluidInTank(int Tank) {
            return Tank >= 0 && Tank < Tanks.size() ? Tanks.get(Tank).getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int Tank) {
            return Tank >= 0 && Tank < Tanks.size() ? Tanks.get(Tank).getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int Tank, FluidStack Stack) {
            return Tank >= 0 && Tank < Tanks.size() && Roles.get(Tank).canFill() && Tanks.get(Tank).isFluidValid(Stack);
        }

        @Override
        public int fill(FluidStack Resource, FluidAction Action) {
            for (int Index = 0; Index < Tanks.size(); Index++) {
                if (Roles.get(Index).canFill() && Tanks.get(Index).isFluidValid(Resource)) {
                    int Filled = Tanks.get(Index).fill(Resource, Action);
                    if (Filled > 0) {
                        return Filled;
                    }
                }
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack Resource, FluidAction Action) {
            for (int Index = 0; Index < Tanks.size(); Index++) {
                if (Roles.get(Index).canDrain()) {
                    FluidStack Drained = Tanks.get(Index).drain(Resource, Action);
                    if (!Drained.isEmpty()) {
                        return Drained;
                    }
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int MaxDrain, FluidAction Action) {
            for (int Index = 0; Index < Tanks.size(); Index++) {
                if (Roles.get(Index).canDrain() && !Tanks.get(Index).isEmpty()) {
                    return Tanks.get(Index).drain(MaxDrain, Action);
                }
            }
            return FluidStack.EMPTY;
        }
    };

    protected int Received;
    protected int LastReceived;
    protected int Demand;
    protected OilStatus Status = OilStatus.Idle;
    protected long LastWorked = Long.MIN_VALUE / 2;
    @Nullable
    private RecipeManager KnownRecipes;
    private int Rate;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private boolean Primed;
    private float Churn;
    private float PreviousChurn;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            if (Index == DataStatus) {
                return Status.ordinal();
            }
            if (Index == DataProgress) {
                return Math.round(Mth.clamp(getProgress(), 0.0f, 1.0f) * ProgressScale);
            }
            if (Index == DataPower) {
                return shownPower();
            }
            if (Index == DataPowerMax) {
                return shownPowerMax();
            }
            if (Index == DataGas) {
                return shownGas();
            }
            int Slot = Index - DataTanks;
            int Tank = Slot / 2;
            if (Tank < 0 || Tank >= Tanks.size()) {
                return Slot % 2 == 0 ? 0 : -1;
            }
            FluidTank Held = Tanks.get(Tank);
            if (Slot % 2 == 0) {
                return Held.getFluidAmount();
            }
            return Held.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(Held.getFluid().getFluid());
        }

        @Override
        public void set(int Index, int Value) {
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    };

    protected OilMachineBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State, OilMachineKind Kind) {
        super(Type, Pos, State);
        this.Kind = Kind;
    }

    public static void setClientEffects(Consumer<OilMachineBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    protected FluidTank addTank(Predicate<FluidStack> Filter, PortRole Role) {
        int Index = Tanks.size();
        FluidTank Made = new FluidTank(Kind.capacity(Index), Filter) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                tankChanged();
            }
        };
        Tanks.add(Made);
        Roles.add(Role);
        return Made;
    }

    protected void tankChanged() {
    }

    protected void recipesReloaded() {
    }

    public OilMachineKind getKind() {
        return Kind;
    }

    public List<FluidTank> getTanks() {
        return Tanks;
    }

    public IFluidHandler getCombined() {
        return Combined;
    }

    public IFluidHandler cellHandler(int FillTank, int DrainTank) {
        int Key = (FillTank + 1) * 16 + DrainTank + 1;
        return CellHandlers.computeIfAbsent(Key, Ignored -> new OilCellHandler(FillTank >= 0 ? Tanks.get(FillTank) : null, DrainTank >= 0 ? Tanks.get(DrainTank) : null));
    }

    @Nullable
    public GasHandler getExhaust() {
        return null;
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction Side) {
        return null;
    }

    public OilStatus getStatus() {
        return Status;
    }

    public float getProgress() {
        return 0.0f;
    }

    protected int shownPower() {
        return LastReceived;
    }

    protected int shownPowerMax() {
        return Demand;
    }

    protected int shownGas() {
        return 0;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    public int getDemand() {
        return Demand;
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.hasProperty(OilMachineBlock.Facing) ? State.getValue(OilMachineBlock.Facing) : Direction.NORTH;
    }

    public Vec3 getCentre() {
        if (getBlockState().getBlock() instanceof OilMachineBlock Machine) {
            return Machine.layout().centre(worldPosition, getFront());
        }
        return Vec3.atCenterOf(worldPosition);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, OilMachineBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof OilMachineBlock) {
            Machine.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, OilMachineBlockEntity Machine) {
        Machine.tickClient();
        ClientEffects.accept(Machine);
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        boolean WasDemanding = Demand > 0;
        Demand = 0;
        RecipeManager Recipes = World.getRecipeManager();
        if (Recipes != KnownRecipes) {
            KnownRecipes = Recipes;
            recipesReloaded();
        }
        float Activity = work(World, Pos, State, Now, Delivered, WasDemanding);
        boolean Active = Now - LastWorked <= ActiveHold;
        if (State.getValue(OilMachineBlock.Active) != Active) {
            OilMachineBlock.applyActive(World, Pos, State, Active);
        }
        updateSync(World, Now, Activity);
    }

    protected abstract float work(ServerLevel World, BlockPos Pos, BlockState State, long Now, int Delivered, boolean WasDemanding);

    protected float consume(long Now, int Draw, int Delivered, boolean WasDemanding) {
        Demand = Draw;
        int Used = WasDemanding ? Math.min(Delivered, Draw) : 0;
        if (Used > 0) {
            LastWorked = Now;
            Status = Used >= Draw ? OilStatus.Working : OilStatus.LowPower;
            setChanged();
            return (float) Used / Draw;
        }
        if (WasDemanding) {
            Status = OilStatus.Unpowered;
        } else if (!Status.isWorking()) {
            Status = OilStatus.Idle;
        }
        return 0.0f;
    }

    private void updateSync(ServerLevel World, long Now, float Activity) {
        int Wanted = Mth.clamp(Math.round(Activity * RateSteps), 0, RateSteps);
        if (Wanted != Rate) {
            UrgentSync |= Wanted == 0 || Rate == 0;
            Rate = Wanted;
            NeedsSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    protected void tickClient() {
        float Target = (float) Rate / RateSteps;
        if (!Primed) {
            Primed = true;
            Churn = Target;
        }
        PreviousChurn = Churn;
        if (Churn < Target) {
            Churn = Math.min(Target, Churn + (Target - Churn) * 0.08f + 0.006f);
        } else if (Churn > Target) {
            Churn = Math.max(Target, Churn - (Churn - Target) * 0.05f - 0.004f);
        }
    }

    public float getChurn() {
        return Churn;
    }

    public float getRenderChurn(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousChurn, Churn);
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    public void dropContents(Level World, BlockPos Pos) {
    }

    public void releaseContents(ServerLevel World) {
    }

    protected void saveExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
    }

    protected void loadExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        for (int Index = 0; Index < Tanks.size(); Index++) {
            Tag.put("Tank" + Index, Tanks.get(Index).writeToNBT(Registries, new CompoundTag()));
        }
        saveExtra(Tag, Registries);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            Rate = Mth.clamp(Tag.getByte("Rate"), 0, RateSteps);
            return;
        }
        for (int Index = 0; Index < Tanks.size(); Index++) {
            if (Tag.contains("Tank" + Index)) {
                Tanks.get(Index).readFromNBT(Registries, Tag.getCompound("Tank" + Index));
            }
        }
        loadExtra(Tag, Registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        Tag.putByte("Rate", (byte) Rate);
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
        return new OilMachineMenu(Kind, ContainerId, PlayerInventory, this, Data, getItemHandler(null));
    }

    public boolean isUsableBy(Player User) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        Vec3 Centre = getCentre();
        return User.distanceToSqr(Centre.x, Centre.y, Centre.z) <= ReachSquared;
    }
}
