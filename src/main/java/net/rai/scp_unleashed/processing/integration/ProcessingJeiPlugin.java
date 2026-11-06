package net.rai.scp_unleashed.processing.integration;

import net.rai.scp_unleashed.integration.jei.QuietClickArea;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.processing.ElectricFurnaceMenu;
import net.rai.scp_unleashed.processing.ItemPressMenu;
import net.rai.scp_unleashed.processing.ProcessingMachineBlockEntity;
import net.rai.scp_unleashed.processing.ProcessingMenu;
import net.rai.scp_unleashed.processing.ProcessingRegistry;
import net.rai.scp_unleashed.processing.client.ElectricFurnaceScreen;
import net.rai.scp_unleashed.processing.client.ItemPressScreen;

@JeiPlugin
public class ProcessingJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "processing");
    private static final int InventorySlots = ProcessingMenu.HotbarEnd - ProcessingMenu.MachineEnd;

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new ElectricSmeltingCategory(Registration.getJeiHelpers().getGuiHelper()));
        Registration.addRecipeCategories(new PressingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World == null) {
            return;
        }
        RecipeManager Recipes = World.getRecipeManager();
        Registration.addRecipes(ElectricSmeltingCategory.Type, Recipes.getAllRecipesFor(ProcessingRegistry.ElectricSmeltingType.get()));
        Registration.addRecipes(PressingCategory.Type, Recipes.getAllRecipesFor(ProcessingRegistry.PressingType.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(ElectricSmeltingCategory.catalyst(), ElectricSmeltingCategory.Type, RecipeTypes.SMELTING);
        Registration.addRecipeCatalyst(PressingCategory.catalyst(), PressingCategory.Type);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration Registration) {
        Registration.addRecipeTransferHandler(ElectricFurnaceMenu.class, ProcessingRegistry.ElectricFurnaceMenuType.get(), ElectricSmeltingCategory.Type,
                ProcessingMachineBlockEntity.InputSlot, 1, ProcessingMenu.MachineEnd, InventorySlots);
        Registration.addRecipeTransferHandler(ElectricFurnaceMenu.class, ProcessingRegistry.ElectricFurnaceMenuType.get(), RecipeTypes.SMELTING,
                ProcessingMachineBlockEntity.InputSlot, 1, ProcessingMenu.MachineEnd, InventorySlots);
        Registration.addRecipeTransferHandler(ItemPressMenu.class, ProcessingRegistry.ItemPressMenuType.get(), PressingCategory.Type,
                ProcessingMachineBlockEntity.InputSlot, 1, ProcessingMenu.MachineEnd, InventorySlots);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        QuietClickArea.register(Registration, ElectricFurnaceScreen.class, ElectricFurnaceScreen.ArrowX, ElectricFurnaceScreen.ArrowY,
                ElectricFurnaceScreen.ArrowWidth, ElectricFurnaceScreen.ArrowHeight, ElectricSmeltingCategory.Type, RecipeTypes.SMELTING);
        QuietClickArea.register(Registration, ItemPressScreen.class, ItemPressScreen.StrokeX, ItemPressScreen.StrokeY,
                ItemPressScreen.StrokeWidth, ItemPressScreen.StrokeHeight, PressingCategory.Type);
    }
}
