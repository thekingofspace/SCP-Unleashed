package net.rai.scp_unleashed.processing;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ItemPressMenu extends ProcessingMenu {
    public static final int InputX = 42;
    public static final int InputY = 35;
    public static final int OutputX = 119;
    public static final int OutputY = 35;
    public static final int CycleDieButton = 0;

    public ItemPressMenu(int ContainerId, Inventory PlayerInventory) {
        super(ProcessingRegistry.ItemPressMenuType.get(), ContainerId, PlayerInventory, null, new ItemStackHandler(ProcessingMachineBlockEntity.SlotCount),
                new SimpleContainerData(ProcessingMachineBlockEntity.DataCount), InputX, InputY, OutputX, OutputY, false);
    }

    public ItemPressMenu(int ContainerId, Inventory PlayerInventory, ItemPressBlockEntity Press, ContainerData Data) {
        super(ProcessingRegistry.ItemPressMenuType.get(), ContainerId, PlayerInventory, Press, Press.getStorage(), Data, InputX, InputY, OutputX, OutputY, false);
    }

    public PressDie getDie() {
        return PressDie.byIndex(getMode());
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Id != CycleDieButton) {
            return false;
        }
        if (getMachine() instanceof ItemPressBlockEntity Press) {
            Press.cycleDie();
        }
        return true;
    }

    @Override
    protected boolean accepts(Level World, ItemStack Stack) {
        return ProcessingRecipes.canPress(World, Stack);
    }

    @Override
    public int getWorkDraw() {
        return ItemPressBlockEntity.WorkDraw;
    }
}
