package net.rai.scp_unleashed.scp280.integration;

import net.rai.scp_unleashed.scp280.Scp280Shade;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp280JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp280JadeProvider.Instance, Scp280Shade.class);
    }
}
