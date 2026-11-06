package net.rai.scp_unleashed.storage.integration;

import net.rai.scp_unleashed.storage.StorageContainerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class StorageJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(StorageJadeProvider.Instance, StorageContainerBlock.class);
    }
}
