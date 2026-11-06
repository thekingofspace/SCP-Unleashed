package net.rai.scp_unleashed.scp140.integration;

import net.rai.scp_unleashed.scp140.Scp140BookBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp140JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp140JadeProvider.Instance, Scp140BookBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp140JadeProvider.Instance, Scp140BookBlock.class);
    }
}
