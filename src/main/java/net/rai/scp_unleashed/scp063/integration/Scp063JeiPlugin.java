package net.rai.scp_unleashed.scp063.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp063.Scp063Registry;

@JeiPlugin
public class Scp063JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp063_jei_plugin");
    public static final int Pages = 6;

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Component[] Lines = new Component[Pages];
        for (int Index = 0; Index < Pages; Index++) {
            Lines[Index] = Component.translatable("jei.scp_unleashed.info.scp063_toothbrush." + (Index + 1));
        }
        Registration.addItemStackInfo(new ItemStack(Scp063Registry.Toothbrush.get()), Lines);
    }
}
