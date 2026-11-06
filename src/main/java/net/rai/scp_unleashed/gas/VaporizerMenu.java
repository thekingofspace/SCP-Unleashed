package net.rai.scp_unleashed.gas;

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
import org.jetbrains.annotations.Nullable;

public class VaporizerMenu extends AbstractContainerMenu {
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int InventoryEnd = 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final VaporizerBlockEntity Machine;
    private final ContainerData Data;

    public VaporizerMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new SimpleContainerData(VaporizerBlockEntity.DataCount));
    }

    public VaporizerMenu(int ContainerId, Inventory PlayerInventory, @Nullable VaporizerBlockEntity Machine, ContainerData Data) {
        super(GasRegistry.VaporizerMenuType.get(), ContainerId);
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
        return Data.get(VaporizerBlockEntity.DataInput);
    }

    public int getGas() {
        return Data.get(VaporizerBlockEntity.DataGas);
    }

    public int getProgress() {
        return Data.get(VaporizerBlockEntity.DataProgress);
    }

    public Fluid getFluid() {
        int Id = Data.get(VaporizerBlockEntity.DataFluidKind);
        return Id < 0 ? Fluids.EMPTY : BuiltInRegistries.FLUID.byId(Id);
    }

    @Nullable
    public Gas getGasKind() {
        int Id = Data.get(VaporizerBlockEntity.DataGasKind);
        return Id < 0 ? null : GasRegistry.Gases.byId(Id);
    }

    public int getReceived() {
        return Data.get(VaporizerBlockEntity.DataReceived);
    }

    public int getDemand() {
        return Data.get(VaporizerBlockEntity.DataDemand);
    }

    public VaporizerStatus getStatus() {
        return VaporizerStatus.byIndex(Data.get(VaporizerBlockEntity.DataStatus));
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
