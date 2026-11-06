package net.rai.scp_unleashed.scp352.integration;

import net.rai.scp_unleashed.scp352.Scp352Crone;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp352JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp352JadeProvider.Instance, Scp352Crone.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp352JadeProvider.Instance, Scp352Crone.class);
    }
}
