package net.rai.scp_unleashed.nethub.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.nethub.NetHubRegistry;
import net.rai.scp_unleashed.nethub.NetworkHubBlockEntity;

@JeiPlugin
public class NetHubJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "nethub_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.network_hub.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(NetHubRegistry.NetworkHubItem.get()),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3"),
                Component.translatable(Prefix + "4", NetworkHubBlockEntity.Draw),
                Component.translatable(Prefix + "5"));
    }
}
