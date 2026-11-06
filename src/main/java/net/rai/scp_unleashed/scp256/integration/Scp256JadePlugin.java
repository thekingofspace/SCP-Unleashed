package net.rai.scp_unleashed.scp256.integration;

import net.rai.scp_unleashed.scp256.Scp256TypewriterBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp256JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp256JadeProvider.Instance, Scp256TypewriterBlock.class);
    }
}
