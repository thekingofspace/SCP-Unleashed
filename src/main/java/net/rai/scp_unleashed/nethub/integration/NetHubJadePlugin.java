package net.rai.scp_unleashed.nethub.integration;

import net.rai.scp_unleashed.nethub.NetworkHubBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class NetHubJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(NetHubJadeProvider.Instance, NetworkHubBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(NetHubJadeProvider.Instance, NetworkHubBlock.class);
    }
}
