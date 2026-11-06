package net.rai.scp_unleashed.sulfur.tank;

import net.minecraft.network.RegistryFriendlyByteBuf;
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
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import org.jetbrains.annotations.Nullable;

public class IndustrialTankMenu extends AbstractContainerMenu {
    public static final int SulfurX = 6;
    public static final int SulfurY = 35;
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int MachineEnd = IndustrialTankBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final IndustrialTankBlockEntity Machine;
    private final ContainerData Data;

    public IndustrialTankMenu(int ContainerId, Inventory PlayerInventory, RegistryFriendlyByteBuf Extra) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(IndustrialTankBlockEntity.SlotCount), new SimpleContainerData(IndustrialTankBlockEntity.DataCount));
    }

    public IndustrialTankMenu(int ContainerId, Inventory PlayerInventory, IndustrialTankBlockEntity Machine, ContainerData Data) {
        this(ContainerId, PlayerInventory, Machine, Machine.getStorage(), Data);
    }

    private IndustrialTankMenu(int ContainerId, Inventory PlayerInventory, @Nullable IndustrialTankBlockEntity Machine, IItemHandler Storage, ContainerData Data) {
        super(SulfurRegistry.TankMenu.get(), ContainerId);
        checkContainerDataCount(Data, IndustrialTankBlockEntity.DataCount);
        this.Machine = Machine;
        this.Data = Data;
        addSlot(new SlotItemHandler(Storage, IndustrialTankBlockEntity.SulfurSlot, SulfurX, SulfurY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return IndustrialTankBlockEntity.accepts(Stack);
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

    public int getWater() {
        return Data.get(IndustrialTankBlockEntity.DataWater);
    }

    public int getAcid() {
        return Data.get(IndustrialTankBlockEntity.DataAcid);
    }

    public int getReceived() {
        return Data.get(IndustrialTankBlockEntity.DataReceived);
    }

    public int getDemand() {
        return Data.get(IndustrialTankBlockEntity.DataDemand);
    }

    public TankStatus getStatus() {
        return TankStatus.byIndex(Data.get(IndustrialTankBlockEntity.DataStatus));
    }

    public int getProgress() {
        return Data.get(IndustrialTankBlockEntity.DataProgress);
    }

    public float getLoad() {
        int Demand = getDemand();
        return Demand <= 0 ? 0.0f : Math.min(1.0f, getReceived() / (float) Demand);
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
        } else if (IndustrialTankBlockEntity.accepts(Stack)) {
            if (!moveItemStackTo(Stack, IndustrialTankBlockEntity.SulfurSlot, IndustrialTankBlockEntity.SulfurSlot + 1, false)) {
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
