package net.rai.scp_unleashed.scp035.integration;

import net.rai.scp_unleashed.scp035.Scp035Host;
import net.rai.scp_unleashed.scp035.Scp035Mask;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp035JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp035MaskJadeProvider.Instance, Scp035Mask.class);
        Registration.registerEntityComponent(Scp035HostJadeProvider.Instance, Scp035Host.class);
    }
}
