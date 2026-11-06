package net.rai.scp_unleashed.scp256.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp256.Scp256Registry;

@JeiPlugin
public class Scp256JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp256Registry.location("scp256_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp256Registry.TypewriterItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp256_typewriter.1"),
                Component.translatable("jei.scp_unleashed.info.scp256_typewriter.2"),
                Component.translatable("jei.scp_unleashed.info.scp256_typewriter.3"));
    }
}
