package net.rai.scp_unleashed.oil.integration;

import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import net.rai.scp_unleashed.oil.OilMachineBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class OilJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(OilMachineJadeProvider.Instance, OilMachineBlock.class);
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, OilMachineBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(OilMachineJadeProvider.Instance, OilMachineBlock.class);
    }
}
