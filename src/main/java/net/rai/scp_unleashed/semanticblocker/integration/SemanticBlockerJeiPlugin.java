package net.rai.scp_unleashed.semanticblocker.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerBlockEntity;
import net.rai.scp_unleashed.semanticblocker.SemanticBlockerRegistry;

@JeiPlugin
public class SemanticBlockerJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = SemanticBlockerRegistry.location(SemanticBlockerRegistry.BlockerId + "_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info." + SemanticBlockerRegistry.BlockerId + ".";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(SemanticBlockerRegistry.BlockerItem.get()),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2", SemanticBlockerBlockEntity.PowerDraw, SemanticBlockerBlockEntity.radiusBlocks()),
                Component.translatable(Prefix + "3"));
    }
}
