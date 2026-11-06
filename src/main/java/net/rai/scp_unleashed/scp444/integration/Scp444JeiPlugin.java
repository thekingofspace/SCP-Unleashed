package net.rai.scp_unleashed.scp444.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp444.Scp444Registry;

@JeiPlugin
public class Scp444JeiPlugin implements IModPlugin {
    public static final int Pages = 3;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp444_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Component[] Lines = new Component[Pages];
        for (int Index = 0; Index < Pages; Index++) {
            Lines[Index] = Component.translatable("jei.scp_unleashed.info.scp444_book." + (Index + 1));
        }
        Registration.addItemStackInfo(new ItemStack(Scp444Registry.Book.get()), Lines);
    }
}
