package net.rai.scp_unleashed.instability;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ReactorFuelHandler implements IItemHandler {
    private final InstabilityReactorBlockEntity Reactor;

    public ReactorFuelHandler(InstabilityReactorBlockEntity Reactor) {
        this.Reactor = Reactor;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int Slot) {
        return Slot == 0 ? Reactor.getFuel().getStackInSlot(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
        if (Slot != 0 || !ReactorFuel.isFuel(Stack)) {
            return Stack;
        }
        return Reactor.getFuel().insertItem(0, Stack, Simulate);
    }

    @Override
    public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int Slot) {
        return Slot == 0 ? Reactor.getFuel().getSlotLimit(0) : 0;
    }

    @Override
    public boolean isItemValid(int Slot, ItemStack Stack) {
        return Slot == 0 && ReactorFuel.isFuel(Stack);
    }
}
