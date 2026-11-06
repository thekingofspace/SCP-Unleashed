package net.rai.scp_unleashed.scp027.integration;

import net.rai.scp_unleashed.scp027.Scp027Bat;
import net.rai.scp_unleashed.scp027.Scp027Endermite;
import net.rai.scp_unleashed.scp027.Scp027Entity;
import net.rai.scp_unleashed.scp027.Scp027Rat;
import net.rai.scp_unleashed.scp027.Scp027Silverfish;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp027JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp027JadeProvider.Instance, Scp027Entity.class);
        Registration.registerEntityComponent(Scp027VerminJadeProvider.Instance, Scp027Rat.class);
        Registration.registerEntityComponent(Scp027VerminJadeProvider.Instance, Scp027Silverfish.class);
        Registration.registerEntityComponent(Scp027VerminJadeProvider.Instance, Scp027Endermite.class);
        Registration.registerEntityComponent(Scp027VerminJadeProvider.Instance, Scp027Bat.class);
    }
}
