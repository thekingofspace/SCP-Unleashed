package net.rai.scp_unleashed.scp020.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp020.Scp020Registry;

import java.util.List;

@JeiPlugin
public class Scp020JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp020_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp020Registry.Goggles.get()),
                Component.translatable("jei.scp_unleashed.info.scp020_hyperspectral_goggles.1"),
                Component.translatable("jei.scp_unleashed.info.scp020_hyperspectral_goggles.2"));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime Runtime) {
        Runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.of(new ItemStack(Scp020Registry.MoldItem.get())));
    }
}
