package net.rai.scp_unleashed.scp082.integration;

import net.rai.scp_unleashed.scp082.Scp082Fernand;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp082JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp082JadeProvider.Instance, Scp082Fernand.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp082JadeProvider.Instance, Scp082Fernand.class);
    }
}
