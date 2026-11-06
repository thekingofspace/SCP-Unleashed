package net.rai.scp_unleashed.guns.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunCraftingRecipes;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.client.GunBenchScreen;

@JeiPlugin
public class GunsJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new GunCraftingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addRecipes(GunCraftingCategory.Type, GunCraftingRecipes.all(Minecraft.getInstance().level));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(GunsRegistry.GunBenchItem.get()), GunCraftingCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        Registration.addRecipeClickArea(GunBenchScreen.class, GunBenchScreen.ArrowX, GunBenchScreen.ArrowY, GunBenchScreen.ArrowWidth, GunBenchScreen.ArrowHeight,
                GunCraftingCategory.Type);
    }
}
