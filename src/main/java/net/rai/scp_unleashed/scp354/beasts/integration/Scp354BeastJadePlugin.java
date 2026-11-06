package net.rai.scp_unleashed.scp354.beasts.integration;

import net.rai.scp_unleashed.scp354.beasts.Scp354Beast;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp354BeastJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp354BeastJadeProvider.Instance, Scp354Beast.class);
    }
}
