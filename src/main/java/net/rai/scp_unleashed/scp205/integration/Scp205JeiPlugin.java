package net.rai.scp_unleashed.scp205.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp205.Scp205Registry;

@JeiPlugin
public class Scp205JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp205Registry.location("scp205_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp205Registry.LampItem.get()),
                Component.translatable(Prefix + "scp205_lamp.1"),
                Component.translatable(Prefix + "scp205_lamp.2"),
                Component.translatable(Prefix + "scp205_lamp.3"));
    }
}
