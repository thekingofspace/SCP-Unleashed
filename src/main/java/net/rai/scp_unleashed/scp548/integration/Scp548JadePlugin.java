package net.rai.scp_unleashed.scp548.integration;

import net.rai.scp_unleashed.scp548.Scp548Spider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp548JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp548JadeProvider.Instance, Scp548Spider.class);
    }
}
