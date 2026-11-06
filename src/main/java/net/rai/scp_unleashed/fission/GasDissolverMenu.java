package net.rai.scp_unleashed.fission;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class GasDissolverMenu extends AbstractContainerMenu {
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int InventoryEnd = 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final GasDissolverBlockEntity Dissolver;
    private final ContainerData Data;

    public GasDissolverMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new SimpleContainerData(GasDissolverBlockEntity.DataCount));
    }

    public GasDissolverMenu(int ContainerId, Inventory PlayerInventory, @Nullable GasDissolverBlockEntity Dissolver, ContainerData Data) {
        super(FissionRegistry.DissolverMenu.get(), ContainerId);
        this.Dissolver = Dissolver;
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

    public int getWater() {
        return Data.get(GasDissolverBlockEntity.DataWater);
    }

    public int getNetworkRate() {
        return Data.get(GasDissolverBlockEntity.DataNetwork);
    }

    public int getAreaRate() {
        return Data.get(GasDissolverBlockEntity.DataArea);
    }

    public int getRate() {
        return getNetworkRate() + getAreaRate();
    }

    public int getWaterUse() {
        return Data.get(GasDissolverBlockEntity.DataUse);
    }

    public int getUpgrades() {
        return Data.get(GasDissolverBlockEntity.DataUpgrades);
    }

    public DissolverMode getStatus() {
        return DissolverMode.byIndex(Data.get(GasDissolverBlockEntity.DataStatus));
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
        return Dissolver == null || Dissolver.isUsableBy(User);
    }
}
