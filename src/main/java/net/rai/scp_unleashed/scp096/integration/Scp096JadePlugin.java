package net.rai.scp_unleashed.scp096.integration;

import net.rai.scp_unleashed.scp096.Scp096ShyGuy;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp096JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp096JadeProvider.Instance, Scp096ShyGuy.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp096JadeProvider.Instance, Scp096ShyGuy.class);
    }
}
