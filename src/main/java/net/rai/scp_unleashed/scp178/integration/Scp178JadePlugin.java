package net.rai.scp_unleashed.scp178.integration;

import net.rai.scp_unleashed.scp178.Scp178Registry;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp178JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.hideTarget(Scp178Registry.Watcher.get());
    }
}
