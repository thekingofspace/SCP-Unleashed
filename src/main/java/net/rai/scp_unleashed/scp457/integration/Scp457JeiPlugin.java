package net.rai.scp_unleashed.scp457.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp457.Scp457Registry;

@JeiPlugin
public class Scp457JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp457_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.scp457_spawn_egg.";
    private static final String PilePrefix = "jei.scp_unleashed.info.scp457_ash_pile.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp457Registry.BurningEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3"),
                Component.translatable(Prefix + "4"));
        Registration.addItemStackInfo(Scp457Registry.AshPileItem.get().getDefaultInstance(),
                Component.translatable(PilePrefix + "1"),
                Component.translatable(PilePrefix + "2"));
    }
}
