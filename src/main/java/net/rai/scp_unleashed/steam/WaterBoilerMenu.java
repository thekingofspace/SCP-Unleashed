package net.rai.scp_unleashed.steam;

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

public class WaterBoilerMenu extends AbstractContainerMenu {
    public static final int[] FuelSlotX = {66, 84, 102};
    public static final int FuelSlotY = 49;
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int FuelEnd = WaterBoilerBlockEntity.FuelSlots;
    public static final int InventoryEnd = FuelEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final WaterBoilerBlockEntity Machine;
    private final ContainerData Data;

    public WaterBoilerMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(WaterBoilerBlockEntity.FuelSlots), new SimpleContainerData(WaterBoilerBlockEntity.DataCount));
    }

    public WaterBoilerMenu(int ContainerId, Inventory PlayerInventory, WaterBoilerBlockEntity Machine, ContainerData Data) {
        this(ContainerId, PlayerInventory, Machine, Machine.getFuel(), Data);
    }

    private WaterBoilerMenu(int ContainerId, Inventory PlayerInventory, @Nullable WaterBoilerBlockEntity Machine, IItemHandler Fuel, ContainerData Data) {
        super(SteamRegistry.WaterBoilerMenuType.get(), ContainerId);
        this.Machine = Machine;
        this.Data = Data;
        for (int Slot = 0; Slot < WaterBoilerBlockEntity.FuelSlots; Slot++) {
            addSlot(new SlotItemHandler(Fuel, Slot, FuelSlotX[Slot], FuelSlotY));
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

    public float burnFraction() {
        return Math.min(1.0f, Data.get(WaterBoilerBlockEntity.DataBurn) / (float) WaterBoilerBlockEntity.BurnSteps);
    }

    public int getHeat() {
        return Data.get(WaterBoilerBlockEntity.DataHeat);
    }

    public int getWater() {
        return Data.get(WaterBoilerBlockEntity.DataWater);
    }

    public int getSteam() {
        return Data.get(WaterBoilerBlockEntity.DataSteam);
    }

    public int getBoiled() {
        return Data.get(WaterBoilerBlockEntity.DataBoiled);
    }

    public BoilerStatus getStatus() {
        return BoilerStatus.byIndex(Data.get(WaterBoilerBlockEntity.DataStatus));
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < FuelEnd) {
            if (!moveItemStackTo(Stack, FuelEnd, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (WaterBoilerBlockEntity.isFuel(Stack) && moveItemStackTo(Stack, 0, FuelEnd, false)) {
            Source.setChanged();
        } else if (Index < InventoryEnd) {
            if (!moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(Stack, FuelEnd, InventoryEnd, false)) {
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
