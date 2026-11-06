package net.rai.scp_unleashed.assembler;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AssemblerMenu extends AbstractContainerMenu {
    public static final int GridX = 30;
    public static final int GridY = 21;
    public static final int OutputX = 129;
    public static final int OutputY = 39;
    public static final int InventoryX = 8;
    public static final int InventoryY = 94;
    public static final int HotbarY = 152;
    public static final int MachineEnd = AdvancedAssemblerBlockEntity.SlotCount;
    public static final int InventoryEnd = MachineEnd + 27;
    public static final int HotbarEnd = InventoryEnd + 9;

    @Nullable
    private final AdvancedAssemblerBlockEntity Assembler;
    private final AssemblerInventory Contents;
    private final ContainerData Data;

    public AssemblerMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new AssemblerInventory(() -> PlayerInventory.player.level(), () -> {
        }), new SimpleContainerData(AdvancedAssemblerBlockEntity.DataCount));
    }

    public AssemblerMenu(int ContainerId, Inventory PlayerInventory, AdvancedAssemblerBlockEntity Assembler, ContainerData Data) {
        this(ContainerId, PlayerInventory, Assembler, Assembler.getContents(), Data);
    }

    private AssemblerMenu(int ContainerId, Inventory PlayerInventory, @Nullable AdvancedAssemblerBlockEntity Assembler, AssemblerInventory Contents, ContainerData Data) {
        super(AssemblerRegistry.AssemblerMenuType.get(), ContainerId);
        this.Assembler = Assembler;
        this.Contents = Contents;
        this.Data = Data;
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 3; Column++) {
                addSlot(new SlotItemHandler(Contents, Column + Row * 3, GridX + Column * 18, GridY + Row * 18));
            }
        }
        addSlot(new SlotItemHandler(Contents, AdvancedAssemblerBlockEntity.OutputSlot, OutputX, OutputY) {
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
        return Data.get(AdvancedAssemblerBlockEntity.ProgressData) / (float) AdvancedAssemblerBlockEntity.ProgressScale;
    }

    public int getTotalTicks() {
        return Data.get(AdvancedAssemblerBlockEntity.TimeData);
    }

    public int getReceived() {
        return Data.get(AdvancedAssemblerBlockEntity.ReceivedData);
    }

    public int getDemand() {
        return Data.get(AdvancedAssemblerBlockEntity.DemandData);
    }

    public int getDraw() {
        return Math.max(1, Data.get(AdvancedAssemblerBlockEntity.DrawData));
    }

    public AssemblerStatus getStatus() {
        return AssemblerStatus.byIndex(Data.get(AdvancedAssemblerBlockEntity.StatusData));
    }

    public List<ItemStack> getInputs() {
        List<ItemStack> Items = new ArrayList<>(AdvancedAssemblerBlockEntity.InputCount);
        for (int Slot = 0; Slot < AdvancedAssemblerBlockEntity.InputCount; Slot++) {
            Items.add(slots.get(Slot).getItem());
        }
        return Items;
    }

    public ItemStack getOutput() {
        return slots.get(AdvancedAssemblerBlockEntity.OutputSlot).getItem();
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
            boolean Loaded = Contents.isItemValid(0, Stack) && moveItemStackTo(Stack, 0, AdvancedAssemblerBlockEntity.InputCount, false);
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
        return Assembler == null || Assembler.isUsableBy(User);
    }
}
