package net.rai.scp_unleashed.scp018.integration;

import net.rai.scp_unleashed.scp018.Scp018Ball;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp018JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp018JadeProvider.Instance, Scp018Ball.class);
    }
}
