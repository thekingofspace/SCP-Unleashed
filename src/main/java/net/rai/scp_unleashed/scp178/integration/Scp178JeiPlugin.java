package net.rai.scp_unleashed.scp178.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp178.Scp178Registry;

@JeiPlugin
public class Scp178JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp178_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp178Registry.Glasses.get()),
                Component.translatable("jei.scp_unleashed.info.scp178_glasses.1"),
                Component.translatable("jei.scp_unleashed.info.scp178_glasses.2"),
                Component.translatable("jei.scp_unleashed.info.scp178_glasses.3"));
    }
}
