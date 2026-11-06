package net.rai.scp_unleashed.scp127.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp127.Scp127Item;

@JeiPlugin
public class Scp127JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp127_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp127Item.create(),
                Component.translatable("jei.scp_unleashed.info.scp127_living_gun.1"),
                Component.translatable("jei.scp_unleashed.info.scp127_living_gun.2"),
                Component.translatable("jei.scp_unleashed.info.scp127_living_gun.3"),
                Component.translatable("jei.scp_unleashed.info.scp127_living_gun.4"));
    }
}
