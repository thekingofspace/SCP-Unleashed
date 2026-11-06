package net.rai.scp_unleashed.scp017.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp017.Scp017Light;
import net.rai.scp_unleashed.scp017.Scp017Registry;
import net.rai.scp_unleashed.scp017.Scp017Snuffer;

@JeiPlugin
public class Scp017JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp017_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp017Registry.ShadowEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp017.1"),
                Component.translatable("jei.scp_unleashed.info.scp017.2", Scp017Light.Threshold),
                Component.translatable("jei.scp_unleashed.info.scp017.3"),
                Component.translatable("jei.scp_unleashed.info.scp017.4", Scp017Snuffer.Radius),
                Component.translatable("jei.scp_unleashed.info.scp017.5"));
    }
}
