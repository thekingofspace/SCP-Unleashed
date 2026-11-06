package net.rai.scp_unleashed.scp3199.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp3199.Scp3199Registry;

@JeiPlugin
public class Scp3199JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp3199Registry.location("scp3199_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp3199Registry.Egg.get()),
                Component.translatable(Prefix + "scp3199_spawn_egg.1"),
                Component.translatable(Prefix + "scp3199_spawn_egg.2"),
                Component.translatable(Prefix + "scp3199_spawn_egg.3"),
                Component.translatable(Prefix + "scp3199_spawn_egg.4"),
                Component.translatable(Prefix + "scp3199_spawn_egg.5"));
    }
}
