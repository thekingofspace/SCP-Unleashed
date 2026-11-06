package net.rai.scp_unleashed.labware;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class LabViralStorageMenu extends AbstractContainerMenu {
    public static final int RackX = 40;
    public static final int RackY = 30;
    public static final int InventoryX = 8;
    public static final int InventoryY = 112;
    public static final int HotbarY = 170;
    public static final int LockerEnd = LabViralStorageBlockEntity.SlotCount;
    public static final int InventoryEnd = LockerEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final LabViralStorageBlockEntity Locker;
    private final ContainerData Data;

    public LabViralStorageMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new LabVialRack(), new SimpleContainerData(LabViralStorageBlockEntity.DataCount));
    }

    public LabViralStorageMenu(int ContainerId, Inventory PlayerInventory, @Nullable LabViralStorageBlockEntity Locker, IItemHandler Storage, ContainerData Data) {
        super(LabwareRegistry.ViralStorageMenuType.get(), ContainerId);
        checkContainerDataCount(Data, LabViralStorageBlockEntity.DataCount);
        this.Locker = Locker;
        this.Data = Data;
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 3; Column++) {
                addSlot(new LabMachineSlot(Storage, Column + Row * 3, RackX + Column * 18, RackY + Row * 18, LabVials::isVial, this::markLocker));
            }
        }
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

    private void markLocker() {
        if (Locker != null) {
            Locker.setChanged();
        }
    }

    @Nullable
    public LabViralStorageBlockEntity getLocker() {
        return Locker;
    }

    public boolean isPowered() {
        return Data.get(LabViralStorageBlockEntity.DataPowered) != 0;
    }

    public int getReceived() {
        return Data.get(LabViralStorageBlockEntity.DataReceived);
    }

    public int getCultures() {
        return Data.get(LabViralStorageBlockEntity.DataCultures);
    }

    public boolean isStocked() {
        for (int Slot = 0; Slot < LockerEnd; Slot++) {
            if (slots.get(Slot).hasItem()) {
                return true;
            }
        }
        return false;
    }

    public int getDemand() {
        return isStocked() ? LabViralStorageBlockEntity.PowerDraw : 0;
    }

    public boolean isBreached() {
        return !isPowered() && getCultures() > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < LockerEnd) {
            if (!moveItemStackTo(Stack, LockerEnd, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (LabVials.isVial(Stack)) {
            if (!moveItemStackTo(Stack, 0, LockerEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, LockerEnd, InventoryEnd, false)) {
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
        return Locker == null || Locker.isUsableBy(User);
    }
}
