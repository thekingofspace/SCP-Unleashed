package net.rai.scp_unleashed.gateway.integration;

import net.rai.scp_unleashed.gateway.ContainedGatewayBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class GatewayJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(GatewayJadeProvider.Instance, ContainedGatewayBlock.class);
    }
}
