package net.rai.scp_unleashed.scp098.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp098.Scp098Crab;
import net.rai.scp_unleashed.scp098.Scp098Registry;

@JeiPlugin
public class Scp098JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp098_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp098Registry.Vial.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp098_sand_vial.1"),
                Component.translatable(Prefix + "scp098_sand_vial.2"),
                Component.translatable(Prefix + "scp098_sand_vial.3"));
        Registration.addItemStackInfo(Scp098Registry.HiveItem.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp098_hive.1"),
                Component.translatable(Prefix + "scp098_hive.2"),
                Component.translatable(Prefix + "scp098_hive.3"),
                Component.translatable(Prefix + "scp098_hive.4"));
        Registration.addItemStackInfo(Scp098Registry.CrabEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp098_crab_spawn_egg.1", (int) Scp098Crab.Health, (int) Scp098Crab.Armour),
                Component.translatable(Prefix + "scp098_crab_spawn_egg.2"),
                Component.translatable(Prefix + "scp098_crab_spawn_egg.3"),
                Component.translatable(Prefix + "scp098_crab_spawn_egg.4"));
    }
}
