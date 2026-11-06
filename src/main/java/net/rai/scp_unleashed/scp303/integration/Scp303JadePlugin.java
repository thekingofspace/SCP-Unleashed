package net.rai.scp_unleashed.scp303.integration;

import net.rai.scp_unleashed.scp303.Scp303Doorman;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp303JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp303JadeProvider.Instance, Scp303Doorman.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp303JadeProvider.Instance, Scp303Doorman.class);
    }
}
