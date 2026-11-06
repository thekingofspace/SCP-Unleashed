package net.rai.scp_unleashed.scp3199.integration;

import net.rai.scp_unleashed.scp3199.Scp3199;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp3199JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp3199JadeProvider.Instance, Scp3199.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp3199JadeProvider.Instance, Scp3199.class);
        Registration.addTooltipCollectedCallback(Scp3199JadeTooltips::collected);
    }
}
