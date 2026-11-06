package net.rai.scp_unleashed.powerbridge.integration;

import net.rai.scp_unleashed.powerbridge.PowerBridgeBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class PowerBridgeJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(PowerBridgeJadeProvider.Instance, PowerBridgeBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(PowerBridgeJadeProvider.Instance, PowerBridgeBlock.class);
    }
}
