package net.rai.scp_unleashed.scp075.integration;

import net.rai.scp_unleashed.scp075.Scp075Snail;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp075JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp075JadeProvider.Instance, Scp075Snail.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp075JadeProvider.Instance, Scp075Snail.class);
    }
}
