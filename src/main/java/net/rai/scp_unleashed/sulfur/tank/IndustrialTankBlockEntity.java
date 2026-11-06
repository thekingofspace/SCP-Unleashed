package net.rai.scp_unleashed.sulfur.tank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.processing.MachineItemHandler;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import net.rai.scp_unleashed.sulfur.SulfurTags;
import net.rai.scp_unleashed.sulfur.acid.AcidContact;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

public class IndustrialTankBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int SulfurSlot = 0;
    public static final int SlotCount = 1;
    public static final int WaterCapacity = 4 * FluidType.BUCKET_VOLUME;
    public static final int AcidCapacity = 8 * FluidType.BUCKET_VOLUME;
    public static final int BatchSulfur = 3;
    public static final int BatchWater = FluidType.BUCKET_VOLUME;
    public static final int BatchAcid = FluidType.BUCKET_VOLUME;
    public static final int BatchTicks = 400;
    public static final int WorkDraw = 40;
    public static final int ActiveHold = 10;
    public static final int ChurnMin = 60;
    public static final int ChurnMax = 100;
    public static final int FillSteps = 4;
    public static final int ProgressSteps = 1000;
    public static final int DataWater = 0;
    public static final int DataAcid = 1;
    public static final int DataReceived = 2;
    public static final int DataDemand = 3;
    public static final int DataStatus = 4;
    public static final int DataProgress = 5;
    public static final int DataCount = 6;

    private final ItemStackHandler Storage = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Slot == SulfurSlot && accepts(Stack);
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
        }
    };
    private final IItemHandler Automation = new MachineItemHandler(Storage, true, false, SulfurSlot);
    private final FluidTank Water = new FluidTank(WaterCapacity, Stack -> Stack.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank Acid = new FluidTank(AcidCapacity, Stack -> Stack.is(SulfurTags.Acid)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final TankValves Valves = new TankValves(Water, Acid);
    private int Received;
    private int LastReceived;
    private int Demand;
    private float Progress;
    private int ChurnTimer = ChurnMin;
    private long LastWorked = Long.MIN_VALUE / 2;
    private TankStatus Status = TankStatus.Idle;
    private int Made;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataWater -> Water.getFluidAmount();
                case DataAcid -> Acid.getFluidAmount();
                case DataReceived -> Math.min(Short.MAX_VALUE, LastReceived);
                case DataDemand -> Demand;
                case DataStatus -> Status.ordinal();
                case DataProgress -> progressShown();
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

    public IndustrialTankBlockEntity(BlockPos Pos, BlockState State) {
        super(SulfurRegistry.TankEntity.get(), Pos, State);
    }

    public static boolean accepts(ItemStack Stack) {
        return Stack.is(SulfurRegistry.Sulfur.get());
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, IndustrialTankBlockEntity Machine) {
        if (World instanceof ServerLevel Server && State.getBlock() instanceof IndustrialTankBlock) {
            Machine.tickServer(Server, Pos, State);
        }
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        boolean WasDemanding = Demand > 0;
        boolean Ready = false;
        int Sulfur = Storage.getStackInSlot(SulfurSlot).getCount();
        boolean Wet = Water.getFluidAmount() >= BatchWater;
        if (Sulfur < BatchSulfur && !Wet) {
            Status = TankStatus.Idle;
        } else if (Sulfur < BatchSulfur) {
            Status = TankStatus.NoSulfur;
        } else if (!Wet) {
            Status = TankStatus.NoWater;
        } else if (Acid.getSpace() < BatchAcid) {
            Status = TankStatus.Full;
        } else {
            Ready = true;
            int Used = WasDemanding ? Math.min(Delivered, WorkDraw) : 0;
            Status = Used >= WorkDraw ? TankStatus.Running : Used > 0 ? TankStatus.Underpowered : TankStatus.Unpowered;
            if (Used > 0) {
                Progress += (float) Used / WorkDraw;
                LastWorked = Now;
                if (--ChurnTimer <= 0) {
                    ChurnTimer = Mth.nextInt(World.getRandom(), ChurnMin, ChurnMax);
                    AcidContact.play(World, Pos.getCenter(), SulfurRegistry.ChurnLayers, 0.5f, 0.9f + World.getRandom().nextFloat() * 0.15f);
                }
                if (Progress >= BatchTicks) {
                    Progress -= BatchTicks;
                    complete(World, Pos);
                }
                setChanged();
            }
        }
        Demand = Ready ? WorkDraw : 0;
        boolean Working = Now - LastWorked <= ActiveHold;
        BlockState Next = State.setValue(IndustrialTankBlock.Active, Working).setValue(IndustrialTankBlock.Fill, fillLevel());
        if (Next != State) {
            World.setBlock(Pos, Next, Block.UPDATE_CLIENTS);
            if (State.getValue(IndustrialTankBlock.Active) != Working) {
                if (Working) {
                    AcidContact.play(World, Pos.getCenter(), SulfurRegistry.StartLayers, 0.6f, 1.0f);
                } else {
                    World.playSound(null, Pos, SulfurRegistry.TankStop.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
                }
            }
        }
    }

    private void complete(ServerLevel World, BlockPos Pos) {
        Storage.extractItem(SulfurSlot, BatchSulfur, false);
        Water.drain(BatchWater, IFluidHandler.FluidAction.EXECUTE);
        Acid.fill(new FluidStack(SulfurRegistry.Acid.get(), BatchAcid), IFluidHandler.FluidAction.EXECUTE);
        Made++;
        AcidContact.play(World, Pos.getCenter(), SulfurRegistry.DoneLayers, 0.6f, 0.95f + World.getRandom().nextFloat() * 0.1f);
    }

    public int fillLevel() {
        if (Acid.isEmpty()) {
            return 0;
        }
        return Mth.clamp((int) Math.ceil(Acid.getFluidAmount() * (double) FillSteps / AcidCapacity), 1, FillSteps);
    }

    public int progressShown() {
        return Mth.clamp(Math.round(Progress * ProgressSteps / BatchTicks), 0, ProgressSteps);
    }

    public float progress() {
        return Mth.clamp(Progress / BatchTicks, 0.0f, 1.0f);
    }

    public TankStatus status() {
        return Status;
    }

    public int lastReceived() {
        return LastReceived;
    }

    public int made() {
        return Made;
    }

    public FluidTank getWaterTank() {
        return Water;
    }

    public FluidTank getAcidTank() {
        return Acid;
    }

    public TankValves getValves() {
        return Valves;
    }

    public ItemStackHandler getStorage() {
        return Storage;
    }

    public IItemHandler getAutomation() {
        return Automation;
    }

    public ContainerData getData() {
        return Data;
    }

    public int comparatorLevel() {
        return Acid.isEmpty() ? 0 : Math.max(1, Math.round(Acid.getFluidAmount() * 15.0f / AcidCapacity));
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Storage.getStackInSlot(Slot).copy());
            Storage.setStackInSlot(Slot, ItemStack.EMPTY);
        }
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.scp_unleashed." + SulfurRegistry.TankId);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new IndustrialTankMenu(ContainerId, PlayerInventory, this, Data);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Items", Storage.serializeNBT(Registries));
        Tag.put("Water", Water.writeToNBT(Registries, new CompoundTag()));
        Tag.put("Acid", Acid.writeToNBT(Registries, new CompoundTag()));
        Tag.putFloat("Progress", Progress);
        Tag.putInt("Made", Made);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Items")) {
            Storage.deserializeNBT(Registries, Tag.getCompound("Items"));
        }
        if (Tag.contains("Water")) {
            Water.readFromNBT(Registries, Tag.getCompound("Water"));
        }
        if (Tag.contains("Acid")) {
            Acid.readFromNBT(Registries, Tag.getCompound("Acid"));
        }
        Progress = Mth.clamp(Tag.getFloat("Progress"), 0.0f, BatchTicks);
        Made = Math.max(0, Tag.getInt("Made"));
    }
}
