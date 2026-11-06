package net.rai.scp_unleashed.scp303.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp303.Scp303Registry;

@JeiPlugin
public class Scp303JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp303Registry.location("scp303_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.scp303_doorman_spawn_egg.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp303Registry.DoormanEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3"));
    }
}
