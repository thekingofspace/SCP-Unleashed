package net.rai.scp_unleashed.scp205.integration;

import net.rai.scp_unleashed.scp205.Scp205LampBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp205JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp205JadeProvider.Instance, Scp205LampBlock.class);
    }
}
