package net.rai.scp_unleashed.labware;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.Predicate;

public class LabMachineSlot extends SlotItemHandler {
    private final Predicate<ItemStack> Accepts;
    private final Runnable Changed;

    public LabMachineSlot(IItemHandler Storage, int Index, int X, int Y, Predicate<ItemStack> Accepts, Runnable Changed) {
        super(Storage, Index, X, Y);
        this.Accepts = Accepts;
        this.Changed = Changed;
    }

    @Override
    public boolean mayPlace(ItemStack Stack) {
        return !Stack.isEmpty() && Accepts.test(Stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        Changed.run();
    }
}
