package net.rai.scp_unleashed.scp129.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp129.Scp129Registry;
import net.rai.scp_unleashed.scp129.Scp129SuppressantRecipe;

import java.util.List;

@JeiPlugin
public class Scp129JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp129_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.scp129_suppressant.";
    private static final ResourceLocation DisplayId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "jei/scp129_suppressant");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addRecipes(RecipeTypes.CRAFTING, List.of(new RecipeHolder<CraftingRecipe>(DisplayId, Scp129SuppressantRecipe.display())));
        Registration.addItemStackInfo(new ItemStack(Scp129Registry.Suppressant.get()),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3"));
    }
}
