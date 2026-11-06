package net.rai.scp_unleashed.labware;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class LabAnalyzerMenu extends AbstractContainerMenu {
    public static final int InputX = 184;
    public static final int InputY = 27;
    public static final int OutputX = 184;
    public static final int OutputY = 81;
    public static final int InventoryX = 8;
    public static final int InventoryY = 126;
    public static final int HotbarY = 184;
    public static final int MachineEnd = LabAnalyzerBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final LabAnalyzerBlockEntity Machine;
    private final ContainerData Data;
    private final BlockPos Pos;

    public LabAnalyzerMenu(int ContainerId, Inventory PlayerInventory, @Nullable RegistryFriendlyByteBuf Buffer) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(LabAnalyzerBlockEntity.SlotCount), new SimpleContainerData(LabAnalyzerBlockEntity.DataCount),
                Buffer == null ? BlockPos.ZERO : Buffer.readBlockPos());
    }

    public LabAnalyzerMenu(int ContainerId, Inventory PlayerInventory, LabAnalyzerBlockEntity Machine) {
        this(ContainerId, PlayerInventory, Machine, Machine.getStorage(), Machine.getData(), Machine.getBlockPos());
    }

    private LabAnalyzerMenu(int ContainerId, Inventory PlayerInventory, @Nullable LabAnalyzerBlockEntity Machine, IItemHandler Storage, ContainerData Data, BlockPos Pos) {
        super(LabwareRegistry.AnalyzerMenuType.get(), ContainerId);
        checkContainerDataCount(Data, LabAnalyzerBlockEntity.DataCount);
        this.Machine = Machine;
        this.Data = Data;
        this.Pos = Pos;
        addSlot(new LabMachineSlot(Storage, LabAnalyzerBlockEntity.InputSlot, InputX, InputY, LabVials::isUsedSwab, this::markMachine));
        addSlot(new LabMachineSlot(Storage, LabAnalyzerBlockEntity.OutputSlot, OutputX, OutputY, Stack -> false, this::markMachine));
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 9; Column++) {
                addSlot(new Slot(PlayerInventory, Column + Row * 9 + 9, InventoryX + Column * 18, InventoryY + Row * 18));
            }
        }
        for (int Column = 0; Column < 9; Column++) {
            addSlot(new Slot(PlayerInventory, Column, InventoryX + Column * 18, HotbarY));
        }
        addDataSlots(Data);
    }

    private void markMachine() {
        if (Machine != null) {
            Machine.slotTouched();
        }
    }

    public BlockPos getPos() {
        return Pos;
    }

    public float getProgress() {
        return Math.min(1.0f, Data.get(LabAnalyzerBlockEntity.DataProgress) / (float) LabAnalyzerBlockEntity.ProgressScale);
    }

    public int getReceived() {
        return Data.get(LabAnalyzerBlockEntity.DataReceived);
    }

    public int getDemand() {
        return Data.get(LabAnalyzerBlockEntity.DataDemand);
    }

    public LabAnalyzerStatus getStatus() {
        return LabAnalyzerStatus.byIndex(Data.get(LabAnalyzerBlockEntity.DataStatus));
    }

    public float getLoad() {
        return Math.min(1.0f, getReceived() / (float) LabAnalyzerBlockEntity.WorkDraw);
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < MachineEnd) {
            if (!moveItemStackTo(Stack, MachineEnd, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (LabVials.isUsedSwab(Stack)) {
            if (!moveItemStackTo(Stack, LabAnalyzerBlockEntity.InputSlot, LabAnalyzerBlockEntity.InputSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, MachineEnd, InventoryEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (Stack.isEmpty()) {
            Source.setByPlayer(ItemStack.EMPTY);
        } else {
            Source.set(Stack);
        }
        if (Stack.getCount() == Original.getCount()) {
            return ItemStack.EMPTY;
        }
        Source.onTake(User, Stack);
        return Original;
    }

    @Override
    public boolean stillValid(Player User) {
        return Machine == null || Machine.isUsableBy(User);
    }
}
