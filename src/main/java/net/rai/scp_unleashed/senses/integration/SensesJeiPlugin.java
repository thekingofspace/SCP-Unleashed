package net.rai.scp_unleashed.senses.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.senses.Senses;
import net.rai.scp_unleashed.senses.SensesRegistry;

@JeiPlugin
public class SensesJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = SensesRegistry.location("senses_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        String Worn = Senses.curiosLoaded() ? "senses.worn_curios" : "senses.worn_head";
        Registration.addItemStackInfo(new ItemStack(SensesRegistry.Earplugs.get()),
                Component.translatable(Prefix + SensesRegistry.EarplugsId + ".1"),
                Component.translatable(Prefix + SensesRegistry.EarplugsId + ".2"),
                Component.translatable(Prefix + Worn));
        Registration.addItemStackInfo(new ItemStack(SensesRegistry.Blindfold.get()),
                Component.translatable(Prefix + SensesRegistry.BlindfoldId + ".1"),
                Component.translatable(Prefix + SensesRegistry.BlindfoldId + ".2"),
                Component.translatable(Prefix + Worn));
    }
}
