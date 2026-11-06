package net.rai.scp_unleashed.processing;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public abstract class ProcessingMenu extends AbstractContainerMenu {
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int MachineEnd = ProcessingMachineBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final ProcessingMachineBlockEntity Machine;
    private final ContainerData Data;
    private final Level World;

    protected ProcessingMenu(MenuType<?> Type, int ContainerId, Inventory PlayerInventory, @Nullable ProcessingMachineBlockEntity Machine, IItemHandler Storage, ContainerData Data,
                             int InputX, int InputY, int OutputX, int OutputY, boolean Smelting) {
        super(Type, ContainerId);
        checkContainerDataCount(Data, ProcessingMachineBlockEntity.DataCount);
        this.Machine = Machine;
        this.Data = Data;
        this.World = PlayerInventory.player.level();
        addSlot(new SlotItemHandler(Storage, ProcessingMachineBlockEntity.InputSlot, InputX, InputY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return accepts(World, Stack);
            }
        });
        addSlot(new MachineOutputSlot(PlayerInventory.player, Machine, Smelting, Storage, ProcessingMachineBlockEntity.OutputSlot, OutputX, OutputY));
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

    protected abstract boolean accepts(Level World, ItemStack Stack);

    public abstract int getWorkDraw();

    public float getProgress() {
        return Math.min(1.0f, Data.get(0) / (float) ProcessingMachineBlockEntity.ProgressScale);
    }

    public int getTotalTicks() {
        return Data.get(1);
    }

    public int getReceived() {
        return Data.get(2);
    }

    public int getDemand() {
        return Data.get(3);
    }

    public MachineStatus getStatus() {
        return MachineStatus.byIndex(Data.get(4));
    }

    public int getMode() {
        return Data.get(5);
    }

    @Nullable
    protected ProcessingMachineBlockEntity getMachine() {
        return Machine;
    }

    public int getWorkingDraw() {
        return getDemand() > 0 ? getDemand() : getWorkDraw();
    }

    public float getLoad() {
        return Math.min(1.0f, getReceived() / (float) getWorkingDraw());
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index == ProcessingMachineBlockEntity.OutputSlot) {
            if (!moveItemStackTo(Stack, MachineEnd, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
            Source.onQuickCraft(Stack, Original);
        } else if (Index == ProcessingMachineBlockEntity.InputSlot) {
            if (!moveItemStackTo(Stack, MachineEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (accepts(World, Stack)) {
            if (!moveItemStackTo(Stack, ProcessingMachineBlockEntity.InputSlot, ProcessingMachineBlockEntity.InputSlot + 1, false)) {
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
