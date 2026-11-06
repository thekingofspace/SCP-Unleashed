package net.rai.scp_unleashed.cooler.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.cooler.CoolerBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerRegistry;

@JeiPlugin
public class CoolerJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = CoolerRegistry.location(CoolerRegistry.CoolerId + "_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info." + CoolerRegistry.CoolerId + ".";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(CoolerRegistry.CoolerItem.get()),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2", CoolerBlockEntity.PowerDraw),
                Component.translatable(Prefix + "3", CoolerBlockEntity.radiusBlocks()),
                Component.translatable(Prefix + "4"),
                Component.translatable(Prefix + "5"),
                Component.translatable(Prefix + "6"));
    }
}
