package net.rai.scp_unleashed.archive;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public record ArchiveAutomation(ItemStackHandler Storage) implements IItemHandler {
    @Override
    public int getSlots() {
        return Storage.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int Slot) {
        return Storage.getStackInSlot(Slot);
    }

    @Override
    public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
        if (Slot == ArchiveBlockEntity.OutputSlot) {
            return Stack;
        }
        return Storage.insertItem(Slot, Stack, Simulate);
    }

    @Override
    public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
        return Slot == ArchiveBlockEntity.OutputSlot ? Storage.extractItem(Slot, Amount, Simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int Slot) {
        return Storage.getSlotLimit(Slot);
    }

    @Override
    public boolean isItemValid(int Slot, ItemStack Stack) {
        return Slot != ArchiveBlockEntity.OutputSlot && Storage.isItemValid(Slot, Stack);
    }
}
