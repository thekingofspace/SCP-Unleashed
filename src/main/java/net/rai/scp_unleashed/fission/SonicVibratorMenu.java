package net.rai.scp_unleashed.fission;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SonicVibratorMenu extends AbstractContainerMenu {
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int InventoryEnd = 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final SonicVibratorBlockEntity Machine;
    private final ContainerData Data;

    public SonicVibratorMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new SimpleContainerData(SonicVibratorBlockEntity.DataCount));
    }

    public SonicVibratorMenu(int ContainerId, Inventory PlayerInventory, @Nullable SonicVibratorBlockEntity Machine, ContainerData Data) {
        super(FissionRegistry.VibratorMenu.get(), ContainerId);
        this.Machine = Machine;
        this.Data = Data;
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

    public int getInput() {
        return Data.get(SonicVibratorBlockEntity.DataInput);
    }

    public int getOutput() {
        return Data.get(SonicVibratorBlockEntity.DataOutput);
    }

    public int getReceived() {
        return Data.get(SonicVibratorBlockEntity.DataReceived);
    }

    public int getDemand() {
        return Data.get(SonicVibratorBlockEntity.DataDemand);
    }

    public int getUpgrades() {
        return Data.get(SonicVibratorBlockEntity.DataUpgrades);
    }

    public VibratorStatus getStatus() {
        return VibratorStatus.byIndex(Data.get(SonicVibratorBlockEntity.DataStatus));
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, 0, InventoryEnd, false)) {
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
        return Machine == null || Machine.isUsableBy(User);
    }
}
