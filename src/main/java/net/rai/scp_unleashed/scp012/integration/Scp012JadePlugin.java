package net.rai.scp_unleashed.scp012.integration;

import net.rai.scp_unleashed.storage.StorageContainerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp012JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp012ContainmentJadeProvider.Instance, StorageContainerBlock.class);
    }
}
