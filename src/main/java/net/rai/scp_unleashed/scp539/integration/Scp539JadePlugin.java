package net.rai.scp_unleashed.scp539.integration;

import net.rai.scp_unleashed.scp539.Scp539Helicopter;
import net.rai.scp_unleashed.scp539.Scp539Stone;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp539JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp539JadeProvider.Stone, Scp539Stone.class);
        Registration.registerEntityComponent(Scp539JadeProvider.Helicopter, Scp539Helicopter.class);
    }
}
