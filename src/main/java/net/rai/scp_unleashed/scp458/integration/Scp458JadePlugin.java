package net.rai.scp_unleashed.scp458.integration;

import net.rai.scp_unleashed.scp458.Scp458BoxBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp458JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp458BoxJadeProvider.Instance, Scp458BoxBlock.class);
    }
}
