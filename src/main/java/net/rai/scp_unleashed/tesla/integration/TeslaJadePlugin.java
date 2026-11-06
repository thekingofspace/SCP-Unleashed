package net.rai.scp_unleashed.tesla.integration;

import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import net.rai.scp_unleashed.tesla.TeslaGateBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class TeslaJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, TeslaGateBlock.class);
        Registration.registerBlockDataProvider(TeslaJadeProvider.Instance, TeslaGateBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(TeslaJadeProvider.Instance, TeslaGateBlock.class);
    }
}
