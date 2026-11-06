package net.rai.scp_unleashed.scp148.integration;

import net.rai.scp_unleashed.scp148.Scp148Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp148JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp148JadeProvider.Instance, Scp148Block.class);
    }
}
