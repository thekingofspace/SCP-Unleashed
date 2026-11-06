package net.rai.scp_unleashed.scp939.compressor;

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
import net.rai.scp_unleashed.scp939.Scp939Registry;
import org.jetbrains.annotations.Nullable;

public class GasCompressorMenu extends AbstractContainerMenu {
    public static final int InputX = 44;
    public static final int InputY = 35;
    public static final int OutputX = 116;
    public static final int OutputY = 35;
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int MachineEnd = GasCompressorBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final GasCompressorBlockEntity Machine;
    private final ContainerData Data;

    public GasCompressorMenu(int ContainerId, Inventory PlayerInventory, RegistryFriendlyByteBuf Extra) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(GasCompressorBlockEntity.SlotCount), new SimpleContainerData(GasCompressorBlockEntity.DataCount));
    }

    public GasCompressorMenu(int ContainerId, Inventory PlayerInventory, GasCompressorBlockEntity Machine, ContainerData Data) {
        this(ContainerId, PlayerInventory, Machine, Machine.getStorage(), Data);
    }

    private GasCompressorMenu(int ContainerId, Inventory PlayerInventory, @Nullable GasCompressorBlockEntity Machine, IItemHandler Storage, ContainerData Data) {
        super(Scp939Registry.CompressorMenu.get(), ContainerId);
        checkContainerDataCount(Data, GasCompressorBlockEntity.DataCount);
        this.Machine = Machine;
        this.Data = Data;
        addSlot(new SlotItemHandler(Storage, GasCompressorBlockEntity.InputSlot, InputX, InputY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return GasCompressorBlockEntity.accepts(Stack);
            }
        });
        addSlot(new SlotItemHandler(Storage, GasCompressorBlockEntity.OutputSlot, OutputX, OutputY) {
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

    public float getProgress() {
        return Math.min(1.0f, Data.get(0) / (float) GasCompressorBlockEntity.ProgressScale);
    }

    public int getTotalTicks() {
        return Data.get(1);
    }

    public int getReceived() {
        return Data.get(2);
    }

    public int getDemand() {
        return Data.get(3);
    }

    public CompressorStatus getStatus() {
        return CompressorStatus.byIndex(Data.get(4));
    }

    public float getSources() {
        return Data.get(5) / 10.0f;
    }

    public float getLoad() {
        return Math.min(1.0f, getReceived() / (float) GasCompressorBlockEntity.WorkDraw);
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
        } else if (GasCompressorBlockEntity.accepts(Stack)) {
            if (!moveItemStackTo(Stack, GasCompressorBlockEntity.InputSlot, GasCompressorBlockEntity.InputSlot + 1, false)) {
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
