package net.rai.scp_unleashed.storage.gel.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.storage.gel.GelRules;
import net.rai.scp_unleashed.storage.gel.GelStorageRegistry;

@JeiPlugin
public class GelStorageJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = GelStorageRegistry.location("storage_container_gel_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.storage_container_gel.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(GelStorageRegistry.GelContainerItem.get()),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2", GelRules.PowerDraw),
                Component.translatable(Prefix + "3"),
                Component.translatable(Prefix + "4"));
    }
}
