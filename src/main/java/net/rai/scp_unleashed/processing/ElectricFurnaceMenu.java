package net.rai.scp_unleashed.processing;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ElectricFurnaceMenu extends ProcessingMenu {
    public static final int InputX = 48;
    public static final int InputY = 25;
    public static final int OutputX = 110;
    public static final int OutputY = 25;

    public ElectricFurnaceMenu(int ContainerId, Inventory PlayerInventory) {
        super(ProcessingRegistry.ElectricFurnaceMenuType.get(), ContainerId, PlayerInventory, null, new ItemStackHandler(ProcessingMachineBlockEntity.SlotCount),
                new SimpleContainerData(ProcessingMachineBlockEntity.DataCount), InputX, InputY, OutputX, OutputY, true);
    }

    public ElectricFurnaceMenu(int ContainerId, Inventory PlayerInventory, ElectricFurnaceBlockEntity Furnace, ContainerData Data) {
        super(ProcessingRegistry.ElectricFurnaceMenuType.get(), ContainerId, PlayerInventory, Furnace, Furnace.getStorage(), Data, InputX, InputY, OutputX, OutputY, true);
    }

    @Override
    protected boolean accepts(Level World, ItemStack Stack) {
        return ProcessingRecipes.canSmelt(World, Stack);
    }

    @Override
    public int getWorkDraw() {
        return ElectricFurnaceBlockEntity.WorkDraw;
    }
}
