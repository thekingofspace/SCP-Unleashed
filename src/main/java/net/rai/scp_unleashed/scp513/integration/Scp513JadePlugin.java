package net.rai.scp_unleashed.scp513.integration;

import net.rai.scp_unleashed.storage.StorageContainerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp513JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp513JadeProvider.Instance, StorageContainerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp513JadeProvider.Instance, StorageContainerBlock.class);
    }
}
