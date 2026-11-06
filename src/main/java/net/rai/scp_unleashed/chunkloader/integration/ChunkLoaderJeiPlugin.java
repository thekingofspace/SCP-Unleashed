package net.rai.scp_unleashed.chunkloader.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderArea;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderConfig;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderRegistry;

@JeiPlugin
public class ChunkLoaderJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "chunk_loader_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        int Max = ChunkLoaderConfig.maxRadius();
        int Side = ChunkLoaderArea.side(Max);
        Registration.addItemStackInfo(new ItemStack(ChunkLoaderRegistry.ChunkLoaderItem.get()),
                Component.translatable("jei.scp_unleashed.info.chunk_loader.1"),
                Component.translatable("jei.scp_unleashed.info.chunk_loader.2", Max, Side, Side),
                Component.translatable("jei.scp_unleashed.info.chunk_loader.3", ChunkLoaderConfig.basePower(), ChunkLoaderConfig.powerPerChunk(),
                        ChunkLoaderConfig.demand(0), ChunkLoaderConfig.demand(2), ChunkLoaderConfig.demand(8), ChunkLoaderConfig.demand(Max)),
                Component.translatable("jei.scp_unleashed.info.chunk_loader.4"));
    }
}
