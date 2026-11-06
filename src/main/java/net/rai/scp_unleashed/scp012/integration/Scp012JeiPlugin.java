package net.rai.scp_unleashed.scp012.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp012.Scp012CompositionItem;

@JeiPlugin
public class Scp012JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp012_jei_plugin");
    public static final int Pages = 4;

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Component[] Lines = new Component[Pages];
        for (int Index = 0; Index < Pages; Index++) {
            Lines[Index] = Component.translatable("jei.scp_unleashed.info.scp012_composition." + (Index + 1));
        }
        Registration.addItemStackInfo(Scp012CompositionItem.blank(), Lines);
    }
}
