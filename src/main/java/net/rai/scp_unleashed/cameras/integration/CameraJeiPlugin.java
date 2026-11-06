package net.rai.scp_unleashed.cameras.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cameras.CameraLinks;
import net.rai.scp_unleashed.cameras.CameraMount;
import net.rai.scp_unleashed.cameras.CameraRegistry;

@JeiPlugin
public class CameraJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "cameras_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(CameraRegistry.CameraItem.get()),
                Component.translatable("jei.scp_unleashed.info.cctv_camera.1"),
                Component.translatable("jei.scp_unleashed.info.cctv_camera.2", Math.round(CameraMount.PanLimit), Math.round(CameraMount.TiltLimit)),
                Component.translatable("jei.scp_unleashed.info.cctv_camera.3"));
        Registration.addItemStackInfo(new ItemStack(CameraRegistry.Tablet.get()),
                Component.translatable("jei.scp_unleashed.info.camera_tablet.1", CameraLinks.MaxLinks),
                Component.translatable("jei.scp_unleashed.info.camera_tablet.2"),
                Component.translatable("jei.scp_unleashed.info.camera_tablet.3"));
    }
}
