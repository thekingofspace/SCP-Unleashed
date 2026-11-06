package net.rai.scp_unleashed.scp539.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp539.Scp539Registry;

@JeiPlugin
public class Scp539JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp539Registry.location("scp539_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.scp539_stone.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp539Registry.Stone.get()),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3"));
    }
}
