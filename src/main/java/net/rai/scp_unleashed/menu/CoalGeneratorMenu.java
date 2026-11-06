package net.rai.scp_unleashed.menu;

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
import net.rai.scp_unleashed.block.entity.CoalGeneratorBlockEntity;
import net.rai.scp_unleashed.registry.ModMenuTypes;
import org.jetbrains.annotations.Nullable;

public class CoalGeneratorMenu extends AbstractContainerMenu {
    public static final int[] FuelSlotX = {44, 80, 116};
    public static final int FuelSlotY = 53;
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;

    @Nullable
    private final CoalGeneratorBlockEntity Generator;
    private final ContainerData Data;

    public CoalGeneratorMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(CoalGeneratorBlockEntity.SlotCount), new SimpleContainerData(CoalGeneratorBlockEntity.DataCount));
    }

    public CoalGeneratorMenu(int ContainerId, Inventory PlayerInventory, CoalGeneratorBlockEntity Generator, ContainerData Data) {
        this(ContainerId, PlayerInventory, Generator, Generator.getFuel(), Data);
    }

    private CoalGeneratorMenu(int ContainerId, Inventory PlayerInventory, @Nullable CoalGeneratorBlockEntity Generator, IItemHandler Fuel, ContainerData Data) {
        super(ModMenuTypes.CoalGenerator.get(), ContainerId);
        this.Generator = Generator;
        this.Data = Data;
        for (int Slot = 0; Slot < CoalGeneratorBlockEntity.SlotCount; Slot++) {
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

    public float burnFraction(int Slot) {
        int Total = Data.get(CoalGeneratorBlockEntity.SlotCount + Slot);
        int Remaining = Data.get(Slot);
        return Total <= 0 || Remaining <= 0 ? 0.0f : Math.min(1.0f, (float) Remaining / Total);
    }

    public int getStored() {
        return Data.get(CoalGeneratorBlockEntity.SlotCount * 2);
    }

    public int getBurningSlots() {
        return Data.get(CoalGeneratorBlockEntity.SlotCount * 2 + 1);
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        int FuelEnd = CoalGeneratorBlockEntity.SlotCount;
        if (Index < FuelEnd) {
            if (!moveItemStackTo(Stack, FuelEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (CoalGeneratorBlockEntity.isFuel(Stack)) {
            if (!moveItemStackTo(Stack, 0, FuelEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
        return Generator == null || Generator.isUsableBy(User);
    }
}
