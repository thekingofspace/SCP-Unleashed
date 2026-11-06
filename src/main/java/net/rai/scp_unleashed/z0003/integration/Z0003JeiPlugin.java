package net.rai.scp_unleashed.z0003.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0003.Z0003DeviceItem;
import net.rai.scp_unleashed.z0003.Z0003VialItem;

@JeiPlugin
public class Z0003JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0003_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Z0003DeviceItem.create(),
                Component.translatable("jei.scp_unleashed.info.z0003_device.1"),
                Component.translatable("jei.scp_unleashed.info.z0003_device.2"),
                Component.translatable("jei.scp_unleashed.info.z0003_device.3"));
        Registration.addItemStackInfo(Z0003VialItem.create(1),
                Component.translatable("jei.scp_unleashed.info.z0003_vial.1"),
                Component.translatable("jei.scp_unleashed.info.z0003_vial.2"));
    }
}
