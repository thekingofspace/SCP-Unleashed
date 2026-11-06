package net.rai.scp_unleashed.reactor;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class IrradiationChamberMenu extends AbstractContainerMenu {
    public static final int InputX = 44;
    public static final int OutputX = 116;
    public static final int SlotY = 35;
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int MachineSlots = IrradiationChamberBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineSlots + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final IrradiationChamberBlockEntity Chamber;
    private final ContainerData Data;

    public IrradiationChamberMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new SimpleContainerData(IrradiationChamberBlockEntity.DataCount));
    }

    public IrradiationChamberMenu(int ContainerId, Inventory PlayerInventory, @Nullable IrradiationChamberBlockEntity Chamber, ContainerData Data) {
        super(ReactorRegistry.ChamberMenu.get(), ContainerId);
        this.Chamber = Chamber;
        this.Data = Data;
        IItemHandler Contents = Chamber == null ? new ItemStackHandler(IrradiationChamberBlockEntity.SlotCount) : Chamber.getContents();
        addSlot(new SlotItemHandler(Contents, IrradiationChamberBlockEntity.InputSlot, InputX, SlotY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return Chamber == null || IrradiationChamberBlockEntity.isIrradiatable(Chamber.getLevel(), Stack);
            }
        });
        addSlot(new SlotItemHandler(Contents, IrradiationChamberBlockEntity.OutputSlot, OutputX, SlotY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return false;
            }
        });
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

    public float getProgress() {
        return Data.get(IrradiationChamberBlockEntity.DataProgress) / 1000.0f;
    }

    public float getFlux() {
        return Data.get(IrradiationChamberBlockEntity.DataFlux) / 100.0f;
    }

    public int getRecipeTime() {
        return Data.get(IrradiationChamberBlockEntity.DataTime);
    }

    public int getSources() {
        return Data.get(IrradiationChamberBlockEntity.DataSources);
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < MachineSlots) {
            if (!moveItemStackTo(Stack, MachineSlots, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slots.get(IrradiationChamberBlockEntity.InputSlot).mayPlace(Stack)) {
            if (!moveItemStackTo(Stack, IrradiationChamberBlockEntity.InputSlot, IrradiationChamberBlockEntity.InputSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, MachineSlots, InventoryEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (Stack.isEmpty()) {
            Source.setByPlayer(ItemStack.EMPTY);
        } else {
            Source.setChanged();
        }
        if (Stack.getCount() == Original.getCount()) {
            return ItemStack.EMPTY;
        }
        Source.onTake(User, Stack);
        return Original;
    }

    @Override
    public boolean stillValid(Player User) {
        return Chamber == null || Chamber.isUsableBy(User);
    }
}
