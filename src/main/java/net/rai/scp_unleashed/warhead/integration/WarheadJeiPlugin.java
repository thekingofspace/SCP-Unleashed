package net.rai.scp_unleashed.warhead.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.warhead.WarheadRegistry;

@JeiPlugin
public class WarheadJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "warhead_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(WarheadRegistry.AlphaWarheadItem.get()),
                Component.translatable("jei.scp_unleashed.info.warhead_alpha.1"),
                Component.translatable("jei.scp_unleashed.info.warhead_alpha.2"),
                Component.translatable("jei.scp_unleashed.info.warhead_alpha.3"),
                Component.translatable("jei.scp_unleashed.info.warhead_alpha.4"));
        Registration.addItemStackInfo(new ItemStack(WarheadRegistry.PanelItem.get()),
                Component.translatable("jei.scp_unleashed.info.warhead_panel.1"),
                Component.translatable("jei.scp_unleashed.info.warhead_panel.2"),
                Component.translatable("jei.scp_unleashed.info.warhead_panel.3"),
                Component.translatable("jei.scp_unleashed.info.warhead_panel.4"));
    }
}
