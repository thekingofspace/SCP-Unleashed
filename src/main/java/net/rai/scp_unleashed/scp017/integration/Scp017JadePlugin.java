package net.rai.scp_unleashed.scp017.integration;

import net.rai.scp_unleashed.scp017.Scp017Shadow;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp017JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp017JadeProvider.Instance, Scp017Shadow.class);
    }
}
