package net.rai.scp_unleashed.scp140.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp140.Scp140Registry;

@JeiPlugin
public class Scp140JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp140Registry.location("scp140_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp140Registry.BookItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp140_chronicle.1"),
                Component.translatable("jei.scp_unleashed.info.scp140_chronicle.2"),
                Component.translatable("jei.scp_unleashed.info.scp140_chronicle.3"));
    }
}
