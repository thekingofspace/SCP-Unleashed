package net.rai.scp_unleashed.scp268.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp268.Scp268Registry;
import net.rai.scp_unleashed.senses.Senses;

@JeiPlugin
public class Scp268JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp268Registry.location("scp268_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.scp268_cap.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp268Registry.Cap.get()),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3"),
                Component.translatable(Senses.curiosLoaded() ? "jei.scp_unleashed.info.senses.worn_curios" : "jei.scp_unleashed.info.senses.worn_head"));
    }
}
