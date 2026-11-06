package net.rai.scp_unleashed.scp006.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp006.Scp006Registry;
import net.rai.scp_unleashed.scp006.Scp006VesselItem;

@JeiPlugin
public class Scp006JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp006Registry.location("scp006_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp006Registry.Vessel.get()),
                Component.translatable("jei.scp_unleashed.info.scp006_vessel.1"),
                Component.translatable("jei.scp_unleashed.info.scp006_vessel.2", Scp006VesselItem.Pours),
                Component.translatable("jei.scp_unleashed.info.scp006_vessel.3"));
        Registration.addItemStackInfo(new ItemStack(Scp006Registry.Bucket.get()),
                Component.translatable("jei.scp_unleashed.info.scp006_bucket.1"),
                Component.translatable("jei.scp_unleashed.info.scp006_bucket.2"));
    }
}
