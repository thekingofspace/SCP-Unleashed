package net.rai.scp_unleashed.scp151.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp151.Scp151Registry;

@JeiPlugin
public class Scp151JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp151Registry.location("scp151_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp151Registry.PaintingItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp151_painting.1"),
                Component.translatable("jei.scp_unleashed.info.scp151_painting.2"),
                Component.translatable("jei.scp_unleashed.info.scp151_painting.3"));
    }
}
