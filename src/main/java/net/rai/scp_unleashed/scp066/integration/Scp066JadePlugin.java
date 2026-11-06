package net.rai.scp_unleashed.scp066.integration;

import net.rai.scp_unleashed.scp066.Scp066Entity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp066JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp066JadeProvider.Instance, Scp066Entity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp066JadeProvider.Instance, Scp066Entity.class);
    }
}
