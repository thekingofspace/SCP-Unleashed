package net.rai.scp_unleashed.cooler.integration;

import net.rai.scp_unleashed.cooler.CoolerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class CoolerJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(CoolerJadeProvider.Instance, CoolerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(CoolerJadeProvider.Instance, CoolerBlock.class);
    }
}
