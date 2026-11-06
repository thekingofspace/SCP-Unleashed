package net.rai.scp_unleashed.scp020.integration;

import net.rai.scp_unleashed.scp020.Scp020Registry;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp020JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.hideTarget(Scp020Registry.Mold.get());
    }
}
