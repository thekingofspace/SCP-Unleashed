package net.rai.scp_unleashed.scp015.integration;

import net.rai.scp_unleashed.scp015.Scp015PipeBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp015JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp015JadeProvider.Instance, Scp015PipeBlock.class);
    }
}
