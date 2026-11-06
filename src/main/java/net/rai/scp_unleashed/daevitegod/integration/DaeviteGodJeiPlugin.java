package net.rai.scp_unleashed.daevitegod.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevitegod.DaeviteGodRegistry;

@JeiPlugin
public class DaeviteGodJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_god_jei_plugin");
    private static final String CrownPrefix = "jei.scp_unleashed.info.daevite_god_crown.";
    private static final String EggPrefix = "jei.scp_unleashed.info.daevite_god_spawn_egg.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(DaeviteGodRegistry.Crown.get().getDefaultInstance(),
                Component.translatable(CrownPrefix + "1"),
                Component.translatable(CrownPrefix + "2", DaeviteGodRegistry.CrownDefense),
                Component.translatable(CrownPrefix + "3"));
        Registration.addItemStackInfo(DaeviteGodRegistry.SpawnEgg.get().getDefaultInstance(),
                Component.translatable(EggPrefix + "1"),
                Component.translatable(EggPrefix + "2"),
                Component.translatable(EggPrefix + "3"),
                Component.translatable(EggPrefix + "4"));
    }
}
