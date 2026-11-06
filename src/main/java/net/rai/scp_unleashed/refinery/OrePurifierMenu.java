package net.rai.scp_unleashed.refinery;

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
import org.jetbrains.annotations.Nullable;

public class OrePurifierMenu extends AbstractContainerMenu {
    public static final int InputX = 23;
    public static final int InputY = 35;
    public static final int[] OutputX = {99, 117, 99, 117, 99, 117};
    public static final int[] OutputY = {17, 17, 35, 35, 53, 53};
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int MachineSlots = OrePurifierBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineSlots + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final OrePurifierBlockEntity Machine;
    private final ContainerData Data;

    public OrePurifierMenu(int ContainerId, Inventory PlayerInventory, @Nullable RegistryFriendlyByteBuf Snapshot) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(OrePurifierBlockEntity.SlotCount), readState(Snapshot));
    }

    public static void writeState(RegistryFriendlyByteBuf Buffer, ContainerData Source) {
        for (int Index = 0; Index < OrePurifierBlockEntity.DataCount; Index++) {
            Buffer.writeVarInt(Source.get(Index));
        }
    }

    private static ContainerData readState(@Nullable RegistryFriendlyByteBuf Snapshot) {
        SimpleContainerData State = new SimpleContainerData(OrePurifierBlockEntity.DataCount);
        if (Snapshot != null) {
            for (int Index = 0; Index < OrePurifierBlockEntity.DataCount; Index++) {
                State.set(Index, Snapshot.readVarInt());
            }
        }
        return State;
    }

    public OrePurifierMenu(int ContainerId, Inventory PlayerInventory, OrePurifierBlockEntity Machine, ContainerData Data) {
        this(ContainerId, PlayerInventory, Machine, Machine.getContents(), Data);
    }

    private OrePurifierMenu(int ContainerId, Inventory PlayerInventory, @Nullable OrePurifierBlockEntity Machine, IItemHandler Handler, ContainerData Data) {
        super(RefineryRegistry.OrePurifierMenuType.get(), ContainerId);
        this.Machine = Machine;
        this.Data = Data;
        Player Viewer = PlayerInventory.player;
        addSlot(new SlotItemHandler(Handler, OrePurifierBlockEntity.InputSlot, InputX, InputY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return OrePurifierBlockEntity.isProcessable(Viewer.level(), Stack);
            }
        });
        for (int Index = 0; Index < OrePurifierBlockEntity.OutputCount; Index++) {
            addSlot(new SlotItemHandler(Handler, OrePurifierBlockEntity.FirstOutput + Index, OutputX[Index], OutputY[Index]) {
                @Override
                public boolean mayPlace(ItemStack Stack) {
                    return false;
                }
            });
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

    public float getProgress() {
        return Math.min(1.0f, Data.get(OrePurifierBlockEntity.DataProgress) / (float) OrePurifierBlockEntity.ProgressScale);
    }

    public int getReceived() {
        return Data.get(OrePurifierBlockEntity.DataReceived);
    }

    public int getDemand() {
        return Data.get(OrePurifierBlockEntity.DataDemand);
    }

    public PurifierStatus getStatus() {
        return PurifierStatus.byIndex(Data.get(OrePurifierBlockEntity.DataStatus));
    }

    public int getWater() {
        return Data.get(OrePurifierBlockEntity.DataWater);
    }

    public int getWaste() {
        return Data.get(OrePurifierBlockEntity.DataWaste);
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
        } else if (OrePurifierBlockEntity.isProcessable(User.level(), Stack)) {
            if (!moveItemStackTo(Stack, OrePurifierBlockEntity.InputSlot, OrePurifierBlockEntity.InputSlot + 1, false)) {
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
