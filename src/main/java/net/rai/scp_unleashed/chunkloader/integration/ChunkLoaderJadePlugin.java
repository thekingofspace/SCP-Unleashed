package net.rai.scp_unleashed.chunkloader.integration;

import net.rai.scp_unleashed.chunkloader.ChunkLoaderBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ChunkLoaderJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ChunkLoaderJadeProvider.Instance, ChunkLoaderBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(ChunkLoaderJadeProvider.Instance, ChunkLoaderBlock.class);
    }
}
