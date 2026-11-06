package net.rai.scp_unleashed.scp327.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp327.Scp327Registry;

@JeiPlugin
public class Scp327JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp327_jei_plugin");
    private static final String SirenPrefix = "jei.scp_unleashed.info.scp327_siren_spawn_egg.";
    private static final String PupPrefix = "jei.scp_unleashed.info.scp327_pup_spawn_egg.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp327Registry.SirenEgg.get().getDefaultInstance(),
                Component.translatable(SirenPrefix + "1"),
                Component.translatable(SirenPrefix + "2"),
                Component.translatable(SirenPrefix + "3"));
        Registration.addItemStackInfo(Scp327Registry.PupEgg.get().getDefaultInstance(),
                Component.translatable(PupPrefix + "1"),
                Component.translatable(PupPrefix + "2"));
    }
}
