package net.rai.scp_unleashed.scp529.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp529.Scp529Registry;

@JeiPlugin
public class Scp529JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp529_jei_plugin");
    private static final String EggPrefix = "jei.scp_unleashed.info.scp529_spawn_egg.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp529Registry.Egg.get().getDefaultInstance(),
                Component.translatable(EggPrefix + "1"),
                Component.translatable(EggPrefix + "2"),
                Component.translatable(EggPrefix + "3"),
                Component.translatable(EggPrefix + "4"));
    }
}
