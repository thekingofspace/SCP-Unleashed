package net.rai.scp_unleashed.oil;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class OilMachineMenu extends AbstractContainerMenu {
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int OutputSlotX = 102;
    public static final int OutputSlotY = 37;

    private final OilMachineKind Kind;
    @Nullable
    private final OilMachineBlockEntity Machine;
    private final ContainerData Data;
    private final int MachineSlots;
    private final int InventoryEnd;
    private final int HotbarEnd;

    public OilMachineMenu(OilMachineKind Kind, int ContainerId, Inventory PlayerInventory) {
        this(Kind, ContainerId, PlayerInventory, null, new SimpleContainerData(OilMachineBlockEntity.DataCount),
                Kind.getOutputSlots() > 0 ? new ItemStackHandler(Kind.getOutputSlots()) : null);
    }

    public OilMachineMenu(OilMachineKind Kind, int ContainerId, Inventory PlayerInventory, @Nullable OilMachineBlockEntity Machine, ContainerData Data,
                          @Nullable IItemHandler Output) {
        super(Kind.menuType(), ContainerId);
        this.Kind = Kind;
        this.Machine = Machine;
        this.Data = Data;
        int Added = 0;
        if (Output != null) {
            for (int Index = 0; Index < Output.getSlots(); Index++) {
                addSlot(new SlotItemHandler(Output, Index, OutputSlotX + Index * 18, OutputSlotY) {
                    @Override
                    public boolean mayPlace(ItemStack Stack) {
                        return false;
                    }
                });
                Added++;
            }
        }
        MachineSlots = Added;
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 9; Column++) {
                addSlot(new Slot(PlayerInventory, Column + Row * 9 + 9, InventoryX + Column * 18, InventoryY + Row * 18));
            }
        }
        for (int Column = 0; Column < 9; Column++) {
            addSlot(new Slot(PlayerInventory, Column, InventoryX + Column * 18, HotbarY));
        }
        InventoryEnd = MachineSlots + 27;
        HotbarEnd = InventoryEnd + 9;
        addDataSlots(Data);
    }

    public OilMachineKind getKind() {
        return Kind;
    }

    public OilStatus getStatus() {
        return OilStatus.byIndex(Data.get(OilMachineBlockEntity.DataStatus));
    }

    public int getProgress() {
        return Data.get(OilMachineBlockEntity.DataProgress);
    }

    public int getPower() {
        return Data.get(OilMachineBlockEntity.DataPower);
    }

    public int getPowerMax() {
        return Data.get(OilMachineBlockEntity.DataPowerMax);
    }

    public int getGas() {
        return Data.get(OilMachineBlockEntity.DataGas);
    }

    public int getAmount(int Tank) {
        return Data.get(OilMachineBlockEntity.DataTanks + Tank * 2);
    }

    public Fluid getFluid(int Tank) {
        int Id = Data.get(OilMachineBlockEntity.DataTanks + Tank * 2 + 1);
        return Id < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(Id);
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
        return Machine == null || Machine.isUsableBy(User);
    }
}
