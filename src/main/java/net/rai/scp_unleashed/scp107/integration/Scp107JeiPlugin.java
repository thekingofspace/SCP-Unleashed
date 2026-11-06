package net.rai.scp_unleashed.scp107.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp107.Scp107Registry;

@JeiPlugin
public class Scp107JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp107_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp107Registry.ShellItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp107_shell.1"),
                Component.translatable("jei.scp_unleashed.info.scp107_shell.2"),
                Component.translatable("jei.scp_unleashed.info.scp107_shell.3"),
                Component.translatable("jei.scp_unleashed.info.scp107_shell.4"),
                Component.translatable("jei.scp_unleashed.info.scp107_shell.5"));
    }
}
