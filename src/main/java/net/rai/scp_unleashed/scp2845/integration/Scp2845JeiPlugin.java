package net.rai.scp_unleashed.scp2845.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp2845.Scp2845Registry;

@JeiPlugin
public class Scp2845JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp2845Registry.location("scp2845_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp2845Registry.Egg.get()), Component.translatable(Prefix + "scp2845_spawn_egg.1"),
                Component.translatable(Prefix + "scp2845_spawn_egg.2"), Component.translatable(Prefix + "scp2845_spawn_egg.3"), Component.translatable(Prefix + "scp2845_spawn_egg.4"));
        Registration.addItemStackInfo(new ItemStack(Scp2845Registry.StoneItem.get()), Component.translatable(Prefix + "scp2845_stone.1"),
                Component.translatable(Prefix + "scp2845_stone.2"));
        Registration.addItemStackInfo(new ItemStack(Scp2845Registry.PillarItem.get()), Component.translatable(Prefix + "scp2845_pillar.1"));
    }
}
