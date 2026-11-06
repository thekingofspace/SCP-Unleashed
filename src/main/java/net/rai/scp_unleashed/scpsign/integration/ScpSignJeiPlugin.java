package net.rai.scp_unleashed.scpsign.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scpsign.ScpSignRegistry;

@JeiPlugin
public class ScpSignJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_sign_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(ScpSignRegistry.SignItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp_sign.1"),
                Component.translatable("jei.scp_unleashed.info.scp_sign.2"),
                Component.translatable("jei.scp_unleashed.info.scp_sign.3"));
        Registration.addItemStackInfo(new ItemStack(ScpSignRegistry.SmallSignItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp_sign_small.1"),
                Component.translatable("jei.scp_unleashed.info.scp_sign.2"),
                Component.translatable("jei.scp_unleashed.info.scp_sign.3"));
    }
}
