package net.rai.scp_unleashed.scp148.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp148.Scp148Registry;

@JeiPlugin
public class Scp148JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp148Registry.location("scp148_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp148Registry.Ingot.get()),
                Component.translatable(Prefix + "scp148_ingot.1"),
                Component.translatable(Prefix + "scp148_ingot.2"),
                Component.translatable(Prefix + "scp148_ingot.3"));
        Registration.addItemStackInfo(new ItemStack(Scp148Registry.WardItem.get()),
                Component.translatable(Prefix + "scp148_block.1"),
                Component.translatable(Prefix + "scp148_block.2"),
                Component.translatable(Prefix + "scp148_block.3"));
        Registration.addItemStackInfo(new ItemStack(Scp148Registry.Helmet.get()),
                Component.translatable(Prefix + "scp148_helmet.1"),
                Component.translatable(Prefix + "scp148_helmet.2"),
                Component.translatable(Prefix + "scp148_helmet.3"));
    }
}
