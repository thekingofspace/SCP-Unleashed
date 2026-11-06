package net.rai.scp_unleashed.assembler;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AssemblerInventory extends ItemStackHandler {
    private final Supplier<Level> World;
    private final Runnable Changed;

    public AssemblerInventory(Supplier<Level> World, Runnable Changed) {
        super(AdvancedAssemblerBlockEntity.SlotCount);
        this.World = World;
        this.Changed = Changed;
    }

    @Override
    public boolean isItemValid(int Slot, ItemStack Stack) {
        return Slot < AdvancedAssemblerBlockEntity.InputCount && AssemblingRecipes.isIngredient(World.get(), Stack);
    }

    @Override
    protected void onContentsChanged(int Slot) {
        Changed.run();
    }

    @Nullable
    public Level getWorld() {
        return World.get();
    }

    public List<ItemStack> inputs() {
        List<ItemStack> Items = new ArrayList<>(AdvancedAssemblerBlockEntity.InputCount);
        for (int Slot = 0; Slot < AdvancedAssemblerBlockEntity.InputCount; Slot++) {
            Items.add(getStackInSlot(Slot));
        }
        return Items;
    }

    public boolean holdsInput(ItemStack Stack) {
        for (int Slot = 0; Slot < AdvancedAssemblerBlockEntity.InputCount; Slot++) {
            if (ItemStack.isSameItemSameComponents(getStackInSlot(Slot), Stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean outputFits(ItemStack Stack) {
        ItemStack Present = getStackInSlot(AdvancedAssemblerBlockEntity.OutputSlot);
        if (Present.isEmpty()) {
            return Stack.getCount() <= Math.min(getSlotLimit(AdvancedAssemblerBlockEntity.OutputSlot), Stack.getMaxStackSize());
        }
        if (!ItemStack.isSameItemSameComponents(Present, Stack)) {
            return false;
        }
        return Present.getCount() + Stack.getCount() <= Math.min(getSlotLimit(AdvancedAssemblerBlockEntity.OutputSlot), Present.getMaxStackSize());
    }

    public void addOutput(ItemStack Stack) {
        ItemStack Present = getStackInSlot(AdvancedAssemblerBlockEntity.OutputSlot);
        setStackInSlot(AdvancedAssemblerBlockEntity.OutputSlot, Present.isEmpty() ? Stack.copy() : Present.copyWithCount(Present.getCount() + Stack.getCount()));
    }
}
