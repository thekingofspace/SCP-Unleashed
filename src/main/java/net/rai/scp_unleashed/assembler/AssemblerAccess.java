package net.rai.scp_unleashed.assembler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class AssemblerAccess implements IItemHandler {
    private final AssemblerInventory Contents;
    private final boolean Insert;
    private final boolean Extract;

    public AssemblerAccess(AssemblerInventory Contents, boolean Insert, boolean Extract) {
        this.Contents = Contents;
        this.Insert = Insert;
        this.Extract = Extract;
    }

    @Override
    public int getSlots() {
        return Contents.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int Slot) {
        return Contents.getStackInSlot(Slot);
    }

    @Override
    public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
        if (!Insert || Slot >= AdvancedAssemblerBlockEntity.InputCount || Stack.isEmpty()) {
            return Stack;
        }
        if (Contents.getStackInSlot(Slot).isEmpty()) {
            if (Contents.holdsInput(Stack) || !AssemblingRecipes.canJoin(Contents.getWorld(), Contents.inputs(), Stack)) {
                return Stack;
            }
        }
        return Contents.insertItem(Slot, Stack, Simulate);
    }

    @Override
    public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
        if (!Extract || Slot != AdvancedAssemblerBlockEntity.OutputSlot) {
            return ItemStack.EMPTY;
        }
        return Contents.extractItem(Slot, Amount, Simulate);
    }

    @Override
    public int getSlotLimit(int Slot) {
        return Contents.getSlotLimit(Slot);
    }

    @Override
    public boolean isItemValid(int Slot, ItemStack Stack) {
        return Insert && Contents.isItemValid(Slot, Stack);
    }
}
