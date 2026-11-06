package net.rai.scp_unleashed.scp261.integration;

import net.rai.scp_unleashed.scp261.Scp261VendingBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp261JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp261VendingJadeProvider.Instance, Scp261VendingBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp261VendingJadeProvider.Instance, Scp261VendingBlock.class);
    }
}
