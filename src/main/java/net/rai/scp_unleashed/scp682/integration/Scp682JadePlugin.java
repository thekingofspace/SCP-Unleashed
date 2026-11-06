package net.rai.scp_unleashed.scp682.integration;

import net.rai.scp_unleashed.scp682.Scp682;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp682JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp682JadeProvider.Instance, Scp682.class);
    }
}
