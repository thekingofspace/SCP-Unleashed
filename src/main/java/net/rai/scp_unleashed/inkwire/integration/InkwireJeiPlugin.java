package net.rai.scp_unleashed.inkwire.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.inkwire.InkwireRegistry;
import net.rai.scp_unleashed.inkwire.InvisibleInkItem;

@JeiPlugin
public class InkwireJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "inkwire_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.invisible_ink.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(InkwireRegistry.InvisibleInk.get().getDefaultInstance(),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3", InvisibleInkItem.Uses));
    }
}
