package net.rai.scp_unleashed.scp143.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp143.Scp143Registry;

@JeiPlugin
public class Scp143JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp143_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp143Registry.LogItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp143_log.1"),
                Component.translatable("jei.scp_unleashed.info.scp143_log.2"));
        Registration.addItemStackInfo(new ItemStack(Scp143Registry.LeavesItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp143_leaves.1"),
                Component.translatable("jei.scp_unleashed.info.scp143_leaves.2"),
                Component.translatable("jei.scp_unleashed.info.scp143_bleeding.1"),
                Component.translatable("jei.scp_unleashed.info.scp143_bleeding.2"));
        Registration.addItemStackInfo(new ItemStack(Scp143Registry.SaplingItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp143_sapling.1"),
                Component.translatable("jei.scp_unleashed.info.scp143_sapling.2"),
                Component.translatable("jei.scp_unleashed.info.scp143_bleeding.1"));
    }
}
