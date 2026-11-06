package net.rai.scp_unleashed.dclass;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class CloningMenu extends AbstractContainerMenu {
    public static final int InputX = 26;
    public static final int InputY = 26;
    public static final int VialX = 26;
    public static final int VialY = 58;
    public static final int InventoryX = 8;
    public static final int InventoryY = 96;
    public static final int HotbarY = 154;
    public static final int MachineEnd = CloningMachineBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final CloningMachineBlockEntity Tank;
    private final ContainerData Data;

    public CloningMenu(int ContainerId, Inventory PlayerInventory, RegistryFriendlyByteBuf Buffer) {
        this(ContainerId, PlayerInventory, PlayerInventory.player.level().getBlockEntity(Buffer.readBlockPos()) instanceof CloningMachineBlockEntity Found ? Found : null,
                new SimpleContainerData(CloningMachineBlockEntity.DataCount));
    }

    public CloningMenu(int ContainerId, Inventory PlayerInventory, @Nullable CloningMachineBlockEntity Tank, ContainerData Data) {
        super(DClassRegistry.CloningMenuType.get(), ContainerId);
        this.Tank = Tank;
        this.Data = Data;
        ItemStackHandler Contents = Tank != null ? Tank.getContents() : new ItemStackHandler(CloningMachineBlockEntity.SlotCount);
        addSlot(new SlotItemHandler(Contents, CloningMachineBlockEntity.InputSlot, InputX, InputY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return Stack.getItem() instanceof CloningBloodItem;
            }
        });
        addSlot(new SlotItemHandler(Contents, CloningMachineBlockEntity.VialSlot, VialX, VialY) {
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

    @Nullable
    public CloningMachineBlockEntity tank() {
        return Tank;
    }

    public float getProgress() {
        return Data.get(CloningMachineBlockEntity.ProgressData) / (float) CloningMachineBlockEntity.ProgressScale;
    }

    public int getReceived() {
        return Data.get(CloningMachineBlockEntity.ReceivedData);
    }

    public int getDemand() {
        return Data.get(CloningMachineBlockEntity.DemandData);
    }

    public CloningStatus getStatus() {
        return CloningStatus.byIndex(Data.get(CloningMachineBlockEntity.StatusData));
    }

    public CloningKind getKind() {
        return CloningKind.byIndex(Data.get(CloningMachineBlockEntity.KindData));
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
        } else {
            boolean Loaded = Stack.getItem() instanceof CloningBloodItem && moveItemStackTo(Stack, CloningMachineBlockEntity.InputSlot, CloningMachineBlockEntity.InputSlot + 1, false);
            if (!Loaded) {
                boolean Shuffled = Index < InventoryEnd ? moveItemStackTo(Stack, InventoryEnd, HotbarEnd, false) : moveItemStackTo(Stack, MachineEnd, InventoryEnd, false);
                if (!Shuffled) {
                    return ItemStack.EMPTY;
                }
            }
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
        return Tank != null && Tank.isUsableBy(User);
    }
}
