package net.rai.scp_unleashed.scp407.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp407.Scp407Registry;

@JeiPlugin
public class Scp407JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp407Registry.location("scp407_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp407Registry.Cassette.get()),
                Component.translatable(Prefix + "scp407_cassette.insert"),
                Component.translatable(Prefix + "scp407_cassette.2"),
                Component.translatable(Prefix + "scp407_cassette.3"));
        Registration.addItemStackInfo(new ItemStack(Scp407Registry.SproutEgg.get()), Component.translatable(Prefix + "scp407_sprout_spawn_egg.1"));
        Registration.addItemStackInfo(new ItemStack(Scp407Registry.TitanEgg.get()), Component.translatable(Prefix + "scp407_titan_spawn_egg.1"));
    }
}
