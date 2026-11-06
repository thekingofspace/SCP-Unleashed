package net.rai.scp_unleashed.centrifuge;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class CentrifugeItemHandler implements IItemHandler {
    private final CentrifugeBlockEntity Machine;
    private final Access Mode;

    public CentrifugeItemHandler(CentrifugeBlockEntity Machine, Access Mode) {
        this.Machine = Machine;
        this.Mode = Mode;
    }

    private int inner(int Slot) {
        return Mode == Access.Drain ? CentrifugeBlockEntity.FirstOutput + Slot : Mode == Access.Feed ? CentrifugeBlockEntity.InputSlot : Slot;
    }

    private boolean valid(int Slot) {
        return Slot >= 0 && Slot < getSlots();
    }

    @Override
    public int getSlots() {
        return switch (Mode) {
            case Feed -> 1;
            case Drain -> CentrifugeBlockEntity.OutputCount;
            case Both -> CentrifugeBlockEntity.SlotCount;
        };
    }

    @Override
    public ItemStack getStackInSlot(int Slot) {
        return valid(Slot) ? Machine.getContents().getStackInSlot(inner(Slot)) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
        if (!valid(Slot) || inner(Slot) != CentrifugeBlockEntity.InputSlot || !Machine.isProcessable(Stack)) {
            return Stack;
        }
        return Machine.getContents().insertItem(CentrifugeBlockEntity.InputSlot, Stack, Simulate);
    }

    @Override
    public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
        if (!valid(Slot) || inner(Slot) == CentrifugeBlockEntity.InputSlot) {
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
        return valid(Slot) && inner(Slot) == CentrifugeBlockEntity.InputSlot && Machine.isProcessable(Stack);
    }

    public enum Access {
        Feed,
        Drain,
        Both
    }
}
