package net.rai.scp_unleashed.advmaterials;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class BenchItemHandler implements IItemHandler {
    private final AdvancedMaterialsBenchBlockEntity Machine;
    private final Access Mode;

    public BenchItemHandler(AdvancedMaterialsBenchBlockEntity Machine, Access Mode) {
        this.Machine = Machine;
        this.Mode = Mode;
    }

    private int inner(int Slot) {
        return Mode == Access.Drain ? AdvancedMaterialsBenchBlockEntity.OutputSlot : Slot;
    }

    private boolean valid(int Slot) {
        return Slot >= 0 && Slot < getSlots();
    }

    private boolean isInput(int Inner) {
        return Inner < AdvancedMaterialsBenchBlockEntity.InputCount;
    }

    @Override
    public int getSlots() {
        return switch (Mode) {
            case Feed -> AdvancedMaterialsBenchBlockEntity.InputCount;
            case Drain -> 1;
            case Both -> AdvancedMaterialsBenchBlockEntity.SlotCount;
        };
    }

    @Override
    public ItemStack getStackInSlot(int Slot) {
        return valid(Slot) ? Machine.getContents().getStackInSlot(inner(Slot)) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
        if (!valid(Slot) || !isInput(inner(Slot)) || !Machine.accepts(Stack)) {
            return Stack;
        }
        return Machine.getContents().insertItem(inner(Slot), Stack, Simulate);
    }

    @Override
    public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
        if (!valid(Slot) || isInput(inner(Slot))) {
            return ItemStack.EMPTY;
        }
        return Machine.getContents().extractItem(inner(Slot), Amount, Simulate);
    }

    @Override
    public int getSlotLimit(int Slot) {
        return valid(Slot) ? Machine.getContents().getSlotLimit(inner(Slot)) : 0;
    }

    @Override
    public boolean isItemValid(int Slot, ItemStack Stack) {
        return valid(Slot) && isInput(inner(Slot)) && Machine.accepts(Stack);
    }

    public enum Access {
        Feed,
        Drain,
        Both
    }
}
