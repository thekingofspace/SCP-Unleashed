package net.rai.scp_unleashed.scp553.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp553.Scp553Registry;

@JeiPlugin
public class Scp553JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp553_jei_plugin");
    private static final String EggPrefix = "jei.scp_unleashed.info.scp553_spawn_egg.";
    private static final String StalactitePrefix = "jei.scp_unleashed.info.scp553_stalactite.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp553Registry.Egg.get().getDefaultInstance(),
                Component.translatable(EggPrefix + "1"),
                Component.translatable(EggPrefix + "2"),
                Component.translatable(EggPrefix + "3"),
                Component.translatable(EggPrefix + "4"));
        Registration.addItemStackInfo(Scp553Registry.StalactiteItem.get().getDefaultInstance(),
                Component.translatable(StalactitePrefix + "1"),
                Component.translatable(StalactitePrefix + "2"),
                Component.translatable(StalactitePrefix + "3"));
    }
}
