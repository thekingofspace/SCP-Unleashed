package net.rai.scp_unleashed.scp469.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp469.Scp469Registry;

@JeiPlugin
public class Scp469JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp469_jei_plugin");
    private static final String EggPrefix = "jei.scp_unleashed.info.scp469_angel_spawn_egg.";
    private static final String FeatherPrefix = "jei.scp_unleashed.info.scp469_feather.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp469Registry.AngelEgg.get().getDefaultInstance(),
                Component.translatable(EggPrefix + "1"),
                Component.translatable(EggPrefix + "2"),
                Component.translatable(EggPrefix + "3"));
        Registration.addItemStackInfo(Scp469Registry.Feather.get().getDefaultInstance(),
                Component.translatable(FeatherPrefix + "1"),
                Component.translatable(FeatherPrefix + "2"));
    }
}
