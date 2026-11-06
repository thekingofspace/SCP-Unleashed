package net.rai.scp_unleashed.space;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.gear.GearSounds;
import net.rai.scp_unleashed.upgrade.MachineUpgrades;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

public class AirInfuserBlockEntity extends BlockEntity implements WireDevice, MenuProvider, Upgradeable {
    public static final int InputSlot = 0;
    public static final int ChamberSlot = 1;
    public static final int OutputSlot = 2;
    public static final int SlotCount = 3;
    public static final int WorkDraw = 32;
    public static final float FillRate = 1.0F;
    public static final int ActiveHold = 10;
    public static final int SyncTicks = 10;
    public static final int DataAir = 0;
    public static final int DataReceived = 1;
    public static final int DataDemand = 2;
    public static final int DataStatus = 3;
    public static final int DataUpgrades = 4;
    public static final int DataLoaded = 5;
    public static final int DataCount = 6;

    private static Consumer<AirInfuserBlockEntity> ClientEffects = Machine -> {
    };

    private final ItemStackHandler Storage = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Slot != OutputSlot && Stack.getItem() instanceof AirCanisterItem;
        }

        @Override
        public int getSlotLimit(int Slot) {
            return Slot == ChamberSlot ? 1 : AirCanisterItem.StackSize;
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
            NeedsSync = true;
        }
    };
    private final IItemHandler InputSide = new SlotHandler(true, false, InputSlot);
    private final IItemHandler OutputSide = new SlotHandler(false, true, OutputSlot);
    private final IItemHandler Automation = new SlotHandler(true, true, InputSlot, OutputSlot);
    private final MachineUpgrades Upgrades = new MachineUpgrades(this::setChanged, UpgradeType.Speed);

    private int Received;
    private int LastReceived;
    private int Demand;
    private int LastDemand;
    private float Carry;
    private AirInfuserStatus Status = AirInfuserStatus.Idle;
    private long LastWorked = Long.MIN_VALUE / 2;
    private boolean NeedsSync = true;
    private long LastSync = Long.MIN_VALUE / 2;
    private float ShownFill;
    private boolean ShownActive;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataAir -> SpaceAir.canisterAir(Storage.getStackInSlot(ChamberSlot));
                case DataReceived -> Math.min(Short.MAX_VALUE, LastReceived);
                case DataDemand -> Math.min(Short.MAX_VALUE, LastDemand);
                case DataStatus -> Status.ordinal();
                case DataUpgrades -> Upgrades.count(UpgradeType.Speed);
                case DataLoaded -> Storage.getStackInSlot(ChamberSlot).isEmpty() ? 0 : 1;
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

    public AirInfuserBlockEntity(BlockPos Pos, BlockState State) {
        super(SpaceRegistry.AirInfuserEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, AirInfuserBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld) {
            Machine.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, AirInfuserBlockEntity Machine) {
        ClientEffects.accept(Machine);
    }

    public static void setClientEffects(Consumer<AirInfuserBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        LastReceived = Received;
        LastDemand = Demand;
        Received = 0;
        load();
        ItemStack Chamber = Storage.getStackInSlot(ChamberSlot);
        int Air = SpaceAir.canisterAir(Chamber);
        int Draw = Upgrades.draw(WorkDraw);
        boolean Filling = false;
        if (Chamber.isEmpty()) {
            Status = AirInfuserStatus.Idle;
        } else if (Air >= AirCanisterItem.Capacity) {
            Status = unload(World, Pos) ? AirInfuserStatus.Idle : AirInfuserStatus.Blocked;
        } else if (SpaceAir.isAirless(World)) {
            Status = AirInfuserStatus.NoAtmosphere;
        } else {
            Filling = true;
            if (LastDemand <= 0) {
                Status = AirInfuserStatus.Idle;
            } else {
                Status = LastReceived <= 0 ? AirInfuserStatus.Unpowered : LastReceived >= LastDemand ? AirInfuserStatus.Running : AirInfuserStatus.Underpowered;
            }
            float Fraction = LastDemand <= 0 ? 0.0F : Math.min(1.0F, (float) LastReceived / LastDemand);
            if (Fraction > 0.0F) {
                float Amount = FillRate * Upgrades.speedFactor() * Fraction + Carry;
                int Whole = (int) Amount;
                Carry = Amount - Whole;
                if (Whole > 0) {
                    ItemStack Filled = Chamber.copy();
                    Filled.set(SpaceRegistry.Air.get(), Math.min(AirCanisterItem.Capacity, Air + Whole));
                    Storage.setStackInSlot(ChamberSlot, Filled);
                }
                LastWorked = Now;
            }
        }
        Demand = Filling ? Draw : 0;
        updateActive(World, Pos, Now);
        if (NeedsSync && Now - LastSync >= SyncTicks) {
            NeedsSync = false;
            LastSync = Now;
            World.sendBlockUpdated(Pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void load() {
        if (!Storage.getStackInSlot(ChamberSlot).isEmpty()) {
            return;
        }
        ItemStack Input = Storage.getStackInSlot(InputSlot);
        if (Input.isEmpty()) {
            return;
        }
        Storage.setStackInSlot(ChamberSlot, Input.copyWithCount(1));
        Storage.setStackInSlot(InputSlot, Input.getCount() > 1 ? Input.copyWithCount(Input.getCount() - 1) : ItemStack.EMPTY);
        Carry = 0.0F;
    }

    private boolean unload(ServerLevel World, BlockPos Pos) {
        ItemStack Chamber = Storage.getStackInSlot(ChamberSlot);
        ItemStack Output = Storage.getStackInSlot(OutputSlot);
        if (!Output.isEmpty() && (!ItemStack.isSameItemSameComponents(Output, Chamber) || Output.getCount() >= Math.min(Output.getMaxStackSize(), AirCanisterItem.StackSize))) {
            return false;
        }
        Storage.setStackInSlot(OutputSlot, Output.isEmpty() ? Chamber.copy() : Output.copyWithCount(Output.getCount() + 1));
        Storage.setStackInSlot(ChamberSlot, ItemStack.EMPTY);
        World.playSound(null, Pos, SpaceRegistry.InfuserDone.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        World.playSound(null, Pos, GearSounds.HazmatHiss.get(), SoundSource.BLOCKS, 0.5F, 1.3F);
        return true;
    }

    private void updateActive(ServerLevel World, BlockPos Pos, long Now) {
        BlockState Current = getBlockState();
        boolean Working = Now - LastWorked <= ActiveHold;
        boolean Loaded = !Storage.getStackInSlot(ChamberSlot).isEmpty();
        if (Current.getValue(AirInfuserBlock.Active) != Working || Current.getValue(AirInfuserBlock.Loaded) != Loaded) {
            World.setBlock(Pos, Current.setValue(AirInfuserBlock.Active, Working).setValue(AirInfuserBlock.Loaded, Loaded), Block.UPDATE_CLIENTS);
        }
    }

    public ItemStackHandler getStorage() {
        return Storage;
    }

    public IItemHandler getItemHandler(@Nullable Direction Side) {
        if (Side == null) {
            return Automation;
        }
        return Side == Direction.DOWN ? OutputSide : InputSide;
    }

    public ContainerData getData() {
        return Data;
    }

    public AirInfuserStatus getStatus() {
        return Status;
    }

    public float getFill() {
        return SpaceAir.canisterAir(Storage.getStackInSlot(ChamberSlot)) / (float) AirCanisterItem.Capacity;
    }

    public float getShownFill() {
        return ShownFill;
    }

    public boolean isShownActive() {
        return ShownActive;
    }

    public int getSpeedUpgrades() {
        return Upgrades.count(UpgradeType.Speed);
    }

    public static int drawFor(int Count) {
        return Math.max(1, Math.round(WorkDraw * (1.0F + MachineUpgrades.PowerPerUpgrade * Count)));
    }

    public static int fillTicks(int Count) {
        return Math.round(AirCanisterItem.Capacity / (FillRate * (1.0F + MachineUpgrades.SpeedPerUpgrade * Count)));
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Storage.getStackInSlot(Slot).copy());
            Storage.setStackInSlot(Slot, ItemStack.EMPTY);
        }
        Upgrades.drop(World, Pos);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean acceptsUpgrade(UpgradeType Type) {
        return Upgrades.accepts(Type);
    }

    @Override
    public boolean hasUpgrade(UpgradeType Type) {
        return Upgrades.count(Type) > 0;
    }

    @Override
    public int countUpgrade(UpgradeType Type) {
        return Upgrades.count(Type);
    }

    @Override
    public void installUpgrade(UpgradeType Type) {
        Upgrades.install(Type);
    }

    @Override
    public Collection<UpgradeType> getUpgrades() {
        return Upgrades.list();
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
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Items", Storage.serializeNBT(Registries));
        Upgrades.save(Tag);
        Tag.putFloat("Carry", Carry);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            ShownFill = Tag.getFloat("Fill");
            ShownActive = Tag.getBoolean("Active");
            return;
        }
        if (Tag.contains("Items")) {
            Storage.deserializeNBT(Registries, Tag.getCompound("Items"));
        }
        Upgrades.load(Tag);
        Carry = Tag.getFloat("Carry");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        Tag.putFloat("Fill", getFill());
        Tag.putBoolean("Active", Status == AirInfuserStatus.Running || Status == AirInfuserStatus.Underpowered);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.air_infuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new AirInfuserMenu(ContainerId, PlayerInventory, this, Storage, Data);
    }

    private final class SlotHandler implements IItemHandler {
        private final boolean Inserts;
        private final boolean Extracts;
        private final int[] Slots;

        private SlotHandler(boolean Inserts, boolean Extracts, int... Slots) {
            this.Inserts = Inserts;
            this.Extracts = Extracts;
            this.Slots = Slots;
        }

        @Override
        public int getSlots() {
            return Slots.length;
        }

        @Override
        public ItemStack getStackInSlot(int Slot) {
            return Storage.getStackInSlot(Slots[Slot]);
        }

        @Override
        public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
            return Inserts && Slots[Slot] == InputSlot ? Storage.insertItem(InputSlot, Stack, Simulate) : Stack;
        }

        @Override
        public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
            return Extracts && Slots[Slot] == OutputSlot ? Storage.extractItem(OutputSlot, Amount, Simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int Slot) {
            return Storage.getSlotLimit(Slots[Slot]);
        }

        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Inserts && Slots[Slot] == InputSlot && Storage.isItemValid(InputSlot, Stack);
        }
    }
}
