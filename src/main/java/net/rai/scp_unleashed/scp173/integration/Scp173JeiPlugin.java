package net.rai.scp_unleashed.scp173.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp173.Scp173Registry;

@JeiPlugin
public class Scp173JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp173_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp173Registry.SpawnEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp173_spawn_egg.1"),
                Component.translatable(Prefix + "scp173_spawn_egg.2"),
                Component.translatable(Prefix + "scp173_spawn_egg.3"));
        Registration.addItemStackInfo(Scp173Registry.ResidueItem.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp173_residue.1"),
                Component.translatable(Prefix + "scp173_residue.2"),
                Component.translatable(Prefix + "scp173_residue.3"));
    }
}
