package net.rai.scp_unleashed.z0001.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0001.Z0001BookItem;

@JeiPlugin
public class Z0001JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0001_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Z0001BookItem.create(),
                Component.translatable("jei.scp_unleashed.info.z0001_book.1"),
                Component.translatable("jei.scp_unleashed.info.z0001_book.2"),
                Component.translatable("jei.scp_unleashed.info.z0001_book.3"));
    }
}
