package net.rai.scp_unleashed.scp093.integration;

import net.rai.scp_unleashed.scp093.Scp093MirrorBlock;
import net.rai.scp_unleashed.scp093.Scp093ReturnMirrorBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp093JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp093MirrorProvider.Instance, Scp093MirrorBlock.class);
        Registration.registerBlockComponent(Scp093MirrorProvider.Instance, Scp093ReturnMirrorBlock.class);
    }
}
