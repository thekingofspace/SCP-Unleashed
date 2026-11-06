package net.rai.scp_unleashed.gateway.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.GatewayRegistry;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class GatewayJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gateway_jei_plugin");
    private static final String GatewayPrefix = "jei.scp_unleashed.info.contained_gateway.";
    private static final String ShardPrefix = "jei.scp_unleashed.info.dimensional_shard.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(GatewayRegistry.GatewayItem.get().getDefaultInstance(),
                Component.translatable(GatewayPrefix + "1"),
                Component.translatable(GatewayPrefix + "2"),
                Component.translatable(GatewayPrefix + "3"),
                Component.translatable(GatewayPrefix + "4"));
        List<ItemStack> Shards = new ArrayList<>();
        Shards.add(GatewayRegistry.Shard.get().getDefaultInstance());
        for (GatewayDestination Destination : GatewayDestinations.all()) {
            Shards.add(GatewayRegistry.shardFor(Destination.Id()));
        }
        Registration.addItemStackInfo(Shards,
                Component.translatable(ShardPrefix + "1"),
                Component.translatable(ShardPrefix + "2"),
                Component.translatable(ShardPrefix + "3"));
    }
}
