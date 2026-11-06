package net.rai.scp_unleashed.rifttech.integration;

import net.rai.scp_unleashed.rifttech.InstabilityPylonBlock;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class RiftTechJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(CollectorJadeProvider.Instance, RiftCollectorBlock.class);
        Registration.registerBlockDataProvider(PylonJadeProvider.Instance, InstabilityPylonBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(CollectorJadeProvider.Instance, RiftCollectorBlock.class);
        Registration.registerBlockComponent(PylonJadeProvider.Instance, InstabilityPylonBlock.class);
    }
}
