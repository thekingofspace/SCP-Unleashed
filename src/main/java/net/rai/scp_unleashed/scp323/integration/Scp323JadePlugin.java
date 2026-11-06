package net.rai.scp_unleashed.scp323.integration;

import net.rai.scp_unleashed.scp323.Scp323Beast;
import net.rai.scp_unleashed.scp323.Scp323Skull;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp323JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp323SkullJadeProvider.Instance, Scp323Skull.class);
        Registration.registerEntityComponent(Scp323BeastJadeProvider.Instance, Scp323Beast.class);
    }
}
