package net.rai.scp_unleashed.scp085.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp085.Scp085Registry;

@JeiPlugin
public class Scp085JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp085_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp085Registry.HomeSheetItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp085_home_sheet.1"),
                Component.translatable("jei.scp_unleashed.info.scp085_home_sheet.2"),
                Component.translatable("jei.scp_unleashed.info.scp085_home_sheet.3"),
                Component.translatable("jei.scp_unleashed.info.scp085_home_sheet.4"));
        Registration.addItemStackInfo(new ItemStack(Scp085Registry.DrawingItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp085_drawing.1"),
                Component.translatable("jei.scp_unleashed.info.scp085_drawing.2"),
                Component.translatable("jei.scp_unleashed.info.scp085_drawing.3"));
    }
}
