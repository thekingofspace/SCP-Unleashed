package net.rai.scp_unleashed.space;

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

public class AirInfuserMenu extends AbstractContainerMenu {
    public static final int InputX = 30;
    public static final int InputY = 35;
    public static final int ChamberX = 80;
    public static final int ChamberY = 35;
    public static final int OutputX = 126;
    public static final int OutputY = 35;
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int MachineEnd = AirInfuserBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final AirInfuserBlockEntity Machine;
    private final ContainerData Data;

    public AirInfuserMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(AirInfuserBlockEntity.SlotCount), new SimpleContainerData(AirInfuserBlockEntity.DataCount));
    }

    public AirInfuserMenu(int ContainerId, Inventory PlayerInventory, @Nullable AirInfuserBlockEntity Machine, IItemHandler Storage, ContainerData Data) {
        super(SpaceRegistry.AirInfuserMenuType.get(), ContainerId);
        checkContainerDataCount(Data, AirInfuserBlockEntity.DataCount);
        this.Machine = Machine;
        this.Data = Data;
        addSlot(new SlotItemHandler(Storage, AirInfuserBlockEntity.InputSlot, InputX, InputY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return Stack.getItem() instanceof AirCanisterItem;
            }
        });
        addSlot(new SlotItemHandler(Storage, AirInfuserBlockEntity.ChamberSlot, ChamberX, ChamberY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return false;
            }
        });
        addSlot(new SlotItemHandler(Storage, AirInfuserBlockEntity.OutputSlot, OutputX, OutputY) {
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

    public int getAir() {
        return Data.get(AirInfuserBlockEntity.DataAir);
    }

    public float getFill() {
        return Math.min(1.0F, getAir() / (float) AirCanisterItem.Capacity);
    }

    public boolean isLoaded() {
        return Data.get(AirInfuserBlockEntity.DataLoaded) != 0;
    }

    public int getReceived() {
        return Data.get(AirInfuserBlockEntity.DataReceived);
    }

    public int getDemand() {
        return Data.get(AirInfuserBlockEntity.DataDemand);
    }

    public int getUpgrades() {
        return Data.get(AirInfuserBlockEntity.DataUpgrades);
    }

    public AirInfuserStatus getStatus() {
        return AirInfuserStatus.byIndex(Data.get(AirInfuserBlockEntity.DataStatus));
    }

    public int getWorkingDraw() {
        return getDemand() > 0 ? getDemand() : AirInfuserBlockEntity.drawFor(getUpgrades());
    }

    public float getLoad() {
        return Math.min(1.0F, getReceived() / (float) getWorkingDraw());
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        Slot Source = slots.get(Index);
        if (!Source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack Stack = Source.getItem();
        ItemStack Original = Stack.copy();
        if (Index < MachineEnd) {
            if (!moveItemStackTo(Stack, MachineEnd, HotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (Stack.getItem() instanceof AirCanisterItem) {
            if (!moveItemStackTo(Stack, AirInfuserBlockEntity.InputSlot, AirInfuserBlockEntity.InputSlot + 1, false)) {
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
