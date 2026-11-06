package net.rai.scp_unleashed.scp346.integration;

import net.rai.scp_unleashed.scp346.Scp346Pterosaur;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp346JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp346JadeProvider.Instance, Scp346Pterosaur.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp346JadeProvider.Instance, Scp346Pterosaur.class);
    }
}
