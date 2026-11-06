package net.rai.scp_unleashed.centrifuge;

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

public class CentrifugeMenu extends AbstractContainerMenu {
    public static final int InputX = 26;
    public static final int InputY = 41;
    public static final int[] OutputX = {106, 124, 106, 124};
    public static final int[] OutputY = {26, 26, 44, 44};
    public static final int InventoryX = 8;
    public static final int InventoryY = 84;
    public static final int HotbarY = 142;
    public static final int MachineSlots = CentrifugeBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineSlots + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final CentrifugeBlockEntity Machine;
    private final ContainerData Data;
    private final Player Viewer;

    public CentrifugeMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new ItemStackHandler(CentrifugeBlockEntity.SlotCount), new SimpleContainerData(CentrifugeBlockEntity.DataCount));
    }

    public CentrifugeMenu(int ContainerId, Inventory PlayerInventory, CentrifugeBlockEntity Machine, ContainerData Data) {
        this(ContainerId, PlayerInventory, Machine, Machine.getContents(), Data);
    }

    private CentrifugeMenu(int ContainerId, Inventory PlayerInventory, @Nullable CentrifugeBlockEntity Machine, IItemHandler Handler, ContainerData Data) {
        super(CentrifugeRegistry.CentrifugeMenuType.get(), ContainerId);
        this.Machine = Machine;
        this.Data = Data;
        this.Viewer = PlayerInventory.player;
        addSlot(new SlotItemHandler(Handler, CentrifugeBlockEntity.InputSlot, InputX, InputY) {
            @Override
            public boolean mayPlace(ItemStack Stack) {
                return CentrifugeBlockEntity.isProcessable(Viewer.level(), Stack);
            }
        });
        for (int Index = 0; Index < CentrifugeBlockEntity.OutputCount; Index++) {
            addSlot(new SlotItemHandler(Handler, CentrifugeBlockEntity.FirstOutput + Index, OutputX[Index], OutputY[Index]) {
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
        return Math.min(1.0f, Data.get(CentrifugeBlockEntity.DataProgress) / (float) CentrifugeBlockEntity.ProgressScale);
    }

    public int getReceived() {
        return Data.get(CentrifugeBlockEntity.DataReceived);
    }

    public int getDemand() {
        return Data.get(CentrifugeBlockEntity.DataDemand);
    }

    public CentrifugeStatus getStatus() {
        return CentrifugeStatus.byIndex(Data.get(CentrifugeBlockEntity.DataStatus));
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
        } else if (CentrifugeBlockEntity.isProcessable(User.level(), Stack)) {
            if (!moveItemStackTo(Stack, CentrifugeBlockEntity.InputSlot, CentrifugeBlockEntity.InputSlot + 1, false)) {
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
