package net.rai.scp_unleashed.scp163.integration;

import net.rai.scp_unleashed.scp163.Scp163Castaway;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp163JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp163JadeProvider.Instance, Scp163Castaway.class);
    }
}
