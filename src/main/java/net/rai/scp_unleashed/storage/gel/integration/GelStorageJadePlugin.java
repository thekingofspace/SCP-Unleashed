package net.rai.scp_unleashed.storage.gel.integration;

import net.rai.scp_unleashed.storage.gel.GelStorageContainerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class GelStorageJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(GelStorageJadeProvider.Instance, GelStorageContainerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(GelStorageJadeProvider.Instance, GelStorageContainerBlock.class);
    }
}
