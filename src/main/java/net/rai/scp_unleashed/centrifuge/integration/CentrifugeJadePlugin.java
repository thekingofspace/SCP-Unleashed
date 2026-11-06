package net.rai.scp_unleashed.centrifuge.integration;

import net.rai.scp_unleashed.centrifuge.CentrifugeBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class CentrifugeJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(CentrifugeJadeProvider.Instance, CentrifugeBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(CentrifugeJadeProvider.Instance, CentrifugeBlock.class);
    }
}
