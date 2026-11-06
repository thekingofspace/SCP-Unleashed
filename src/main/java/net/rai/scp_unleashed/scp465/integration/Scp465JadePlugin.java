package net.rai.scp_unleashed.scp465.integration;

import net.rai.scp_unleashed.scp465.Scp465BoxBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp465JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp465BoxJadeProvider.Instance, Scp465BoxBlock.class);
    }
}
