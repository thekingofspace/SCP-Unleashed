package net.rai.scp_unleashed.instability;

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

public class InstabilityMenu extends AbstractContainerMenu {
    public static final int FuelX = 21;
    public static final int FuelY = 27;
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int MachineSlots = 1;
    public static final int InventoryEnd = MachineSlots + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final InstabilityReactorBlockEntity Reactor;
    private final ContainerData Data;

    public InstabilityMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(1), new SimpleContainerData(InstabilityReactorBlockEntity.DataCount));
    }

    public InstabilityMenu(int ContainerId, Inventory PlayerInventory, InstabilityReactorBlockEntity Reactor, ContainerData Data) {
        this(ContainerId, PlayerInventory, Reactor, Reactor.getFuel(), Data);
    }

    private InstabilityMenu(int ContainerId, Inventory PlayerInventory, @Nullable InstabilityReactorBlockEntity Reactor, IItemHandler Fuel, ContainerData Data) {
        super(InstabilityRegistry.ReactorMenuType.get(), ContainerId);
        this.Reactor = Reactor;
        this.Data = Data;
        addSlot(new SlotItemHandler(Fuel, 0, FuelX, FuelY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return ReactorFuel.isFuel(Stack);
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

    public int getBurnSeconds() {
        return Data.get(InstabilityReactorBlockEntity.DataBurnSeconds);
    }

    public int getTotalSeconds() {
        return Data.get(InstabilityReactorBlockEntity.DataTotalSeconds);
    }

    public float getBurnFraction() {
        int Total = getTotalSeconds();
        return Total <= 0 ? 0.0f : Math.min(1.0f, (float) getBurnSeconds() / Total);
    }

    public int getOutput() {
        return Data.get(InstabilityReactorBlockEntity.DataOutput);
    }

    public float getStability() {
        return Math.min(1.0f, Math.max(0.0f, Data.get(InstabilityReactorBlockEntity.DataStability) / 1000.0f));
    }

    @Nullable
    public ReactorFuel getBurning() {
        return ReactorFuel.byTier(Data.get(InstabilityReactorBlockEntity.DataGrade));
    }

    public int getStored() {
        return Data.get(InstabilityReactorBlockEntity.DataStored) * InstabilityReactorBlockEntity.StoredScale;
    }

    public ReactorStatus getStatus() {
        return ReactorStatus.byIndex(Data.get(InstabilityReactorBlockEntity.DataStatus));
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
        } else if (ReactorFuel.isFuel(Stack)) {
            if (!moveItemStackTo(Stack, 0, MachineSlots, false)) {
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
        return Reactor == null || Reactor.isUsableBy(User);
    }
}
