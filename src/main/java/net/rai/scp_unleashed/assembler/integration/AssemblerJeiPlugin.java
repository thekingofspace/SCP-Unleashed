package net.rai.scp_unleashed.assembler.integration;

import net.rai.scp_unleashed.integration.jei.QuietClickArea;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.assembler.AdvancedAssemblerBlockEntity;
import net.rai.scp_unleashed.assembler.AssemblerMenu;
import net.rai.scp_unleashed.assembler.AssemblerRegistry;
import net.rai.scp_unleashed.assembler.AssemblingRecipes;
import net.rai.scp_unleashed.assembler.client.AdvancedAssemblerScreen;

@JeiPlugin
public class AssemblerJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "assembler_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new AssemblingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addRecipes(AssemblingCategory.Type, AssemblingRecipes.all(Minecraft.getInstance().level));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(AssemblerRegistry.AdvancedAssemblerItem.get()), AssemblingCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        QuietClickArea.register(Registration, AdvancedAssemblerScreen.class, AdvancedAssemblerScreen.ArrowX, AdvancedAssemblerScreen.ArrowY,
                AdvancedAssemblerScreen.ArrowWidth, AdvancedAssemblerScreen.ArrowHeight, AssemblingCategory.Type);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration Registration) {
        Registration.addRecipeTransferHandler(AssemblerMenu.class, AssemblerRegistry.AssemblerMenuType.get(), AssemblingCategory.Type,
                0, AdvancedAssemblerBlockEntity.InputCount, AdvancedAssemblerBlockEntity.SlotCount, 36);
    }
}
