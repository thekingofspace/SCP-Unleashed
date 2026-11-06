package net.rai.scp_unleashed.scp058.integration;

import net.rai.scp_unleashed.scp058.Scp058Heart;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp058JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp058JadeProvider.Instance, Scp058Heart.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp058JadeProvider.Instance, Scp058Heart.class);
    }
}
