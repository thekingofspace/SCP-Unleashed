package net.rai.scp_unleashed.reactor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;

public class TowerItemHandler implements IItemHandler {
    private final List<TowerSegmentBlockEntity> Segments;
    private final Runnable Changed;
    private final Runnable Before;

    public TowerItemHandler(List<TowerSegmentBlockEntity> Segments, Runnable Changed, Runnable Before) {
        this.Segments = Segments;
        this.Changed = Changed;
        this.Before = Before;
    }

    @Override
    public int getSlots() {
        return Segments.size();
    }

    @Override
    public ItemStack getStackInSlot(int Slot) {
        return Slot >= 0 && Slot < Segments.size() ? Segments.get(Slot).getRod() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
        if (Slot < 0 || Slot >= Segments.size() || Stack.isEmpty() || !TowerFuel.isFuel(Stack)) {
            return Stack;
        }
        TowerSegmentBlockEntity Segment = Segments.get(Slot);
        if (Segment.isRemoved() || !Segment.getRod().isEmpty()) {
            return Stack;
        }
        if (!Simulate) {
            Before.run();
            Segment.setRod(Stack.copyWithCount(1), 0.0f);
            Changed.run();
        }
        return Stack.getCount() > 1 ? Stack.copyWithCount(Stack.getCount() - 1) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
        if (Amount <= 0 || Slot < 0 || Slot >= Segments.size()) {
            return ItemStack.EMPTY;
        }
        TowerSegmentBlockEntity Segment = Segments.get(Slot);
        ItemStack Held = Segment.getRod();
        if (Segment.isRemoved() || !TowerFuel.isDepleted(Held)) {
            return ItemStack.EMPTY;
        }
        ItemStack Taken = Held.copyWithCount(1);
        if (!Simulate) {
            Before.run();
            Segment.setRod(ItemStack.EMPTY, 0.0f);
            Changed.run();
        }
        return Taken;
    }

    public ItemStack takeForHand(boolean Fresh) {
        for (int Slot = Segments.size() - 1; Slot >= 0; Slot--) {
            ItemStack Taken = extractItem(Slot, 1, false);
            if (!Taken.isEmpty()) {
                return Taken;
            }
        }
        if (!Fresh) {
            return ItemStack.EMPTY;
        }
        Before.run();
        for (int Slot = Segments.size() - 1; Slot >= 0; Slot--) {
            TowerSegmentBlockEntity Segment = Segments.get(Slot);
            ItemStack Held = Segment.getRod();
            if (!Segment.isRemoved() && TowerFuel.isFuel(Held) && Segment.getBurnDone() <= 0.0f) {
                Segment.setRod(ItemStack.EMPTY, 0.0f);
                Changed.run();
                return Held.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int Slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int Slot, ItemStack Stack) {
        return TowerFuel.isFuel(Stack);
    }
}
