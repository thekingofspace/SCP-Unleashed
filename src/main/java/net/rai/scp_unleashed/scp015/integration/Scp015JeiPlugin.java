package net.rai.scp_unleashed.scp015.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp015.Scp015Registry;

@JeiPlugin
public class Scp015JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp015_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp015Registry.PipeItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp015_pipe.1"),
                Component.translatable("jei.scp_unleashed.info.scp015_pipe.2"),
                Component.translatable("jei.scp_unleashed.info.scp015_pipe.3"),
                Component.translatable("jei.scp_unleashed.info.scp015_pipe.4"));
    }
}
