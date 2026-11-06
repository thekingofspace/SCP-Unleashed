package net.rai.scp_unleashed.scp031.integration;

import net.rai.scp_unleashed.scp031.Scp031Polyp;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp031JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp031PolypProvider.Instance, Scp031Polyp.class);
        Registration.addTooltipCollectedCallback(Scp031JadeTooltips::collected);
    }
}
