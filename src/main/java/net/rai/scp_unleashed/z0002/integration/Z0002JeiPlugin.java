package net.rai.scp_unleashed.z0002.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0002.Z0002FingerItem;

@JeiPlugin
public class Z0002JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0002_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Z0002FingerItem.create(),
                Component.translatable("jei.scp_unleashed.info.z0002_finger.1"),
                Component.translatable("jei.scp_unleashed.info.z0002_finger.2"),
                Component.translatable("jei.scp_unleashed.info.z0002_finger.3"));
    }
}
