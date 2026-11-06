package net.rai.scp_unleashed.scp2845.integration;

import net.rai.scp_unleashed.scp2845.Scp2845;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp2845JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp2845JadeProvider.Instance, Scp2845.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp2845JadeProvider.Instance, Scp2845.class);
        Registration.addTooltipCollectedCallback(Scp2845JadeProvider::collected);
    }
}
