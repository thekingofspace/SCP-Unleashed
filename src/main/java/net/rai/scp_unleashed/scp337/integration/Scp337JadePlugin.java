package net.rai.scp_unleashed.scp337.integration;

import net.rai.scp_unleashed.scp337.Scp337Mass;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp337JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp337JadeProvider.Instance, Scp337Mass.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp337JadeProvider.Instance, Scp337Mass.class);
    }
}
