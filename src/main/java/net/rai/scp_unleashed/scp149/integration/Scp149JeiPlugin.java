package net.rai.scp_unleashed.scp149.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import net.rai.scp_unleashed.scp149.Scp149Registry;

@JeiPlugin
public class Scp149JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp149_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp149Registry.FlyEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp149_fly_spawn_egg.1", (int) Scp149Fly.Health),
                Component.translatable(Prefix + "scp149_fly_spawn_egg.2"),
                Component.translatable(Prefix + "scp149_fly_spawn_egg.3"),
                Component.translatable(Prefix + "scp149_fly_spawn_egg.4"));
        Registration.addItemStackInfo(Scp149Registry.LarvaEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp149_larva_spawn_egg.1"),
                Component.translatable(Prefix + "scp149_larva_spawn_egg.2"),
                Component.translatable(Prefix + "scp149_larva_spawn_egg.3"));
        Registration.addItemStackInfo(Scp149Registry.CocoonItem.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp149_cocoon.1"),
                Component.translatable(Prefix + "scp149_cocoon.2"),
                Component.translatable(Prefix + "scp149_cocoon.3"));
    }
}
