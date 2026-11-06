package net.rai.scp_unleashed.scp098.integration;

import net.rai.scp_unleashed.scp098.Scp098Crab;
import net.rai.scp_unleashed.scp098.Scp098HiveBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp098JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp098HiveJadeProvider.Instance, Scp098HiveBlock.class);
        Registration.registerEntityComponent(Scp098CrabJadeProvider.Instance, Scp098Crab.class);
    }
}
