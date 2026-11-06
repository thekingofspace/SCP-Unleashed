package net.rai.scp_unleashed.processing;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class MachineItemHandler implements IItemHandler {
    private final ItemStackHandler Storage;
    private final int[] Slots;
    private final boolean Inserts;
    private final boolean Extracts;

    public MachineItemHandler(ItemStackHandler Storage, boolean Inserts, boolean Extracts, int... Slots) {
        this.Storage = Storage;
        this.Slots = Slots;
        this.Inserts = Inserts;
        this.Extracts = Extracts;
    }

    private boolean inserts(int Slot) {
        return Inserts && Slots[Slot] == ProcessingMachineBlockEntity.InputSlot;
    }

    private boolean extracts(int Slot) {
        return Extracts && Slots[Slot] == ProcessingMachineBlockEntity.OutputSlot;
    }

    @Override
    public int getSlots() {
        return Slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int Slot) {
        return Storage.getStackInSlot(Slots[Slot]);
    }

    @Override
    public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
        return inserts(Slot) ? Storage.insertItem(Slots[Slot], Stack, Simulate) : Stack;
    }

    @Override
    public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
        return extracts(Slot) ? Storage.extractItem(Slots[Slot], Amount, Simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int Slot) {
        return Storage.getSlotLimit(Slots[Slot]);
    }

    @Override
    public boolean isItemValid(int Slot, ItemStack Stack) {
        return inserts(Slot) && Storage.isItemValid(Slots[Slot], Stack);
    }
}
