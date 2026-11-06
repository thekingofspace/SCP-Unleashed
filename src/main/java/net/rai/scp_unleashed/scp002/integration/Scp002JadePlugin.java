package net.rai.scp_unleashed.scp002.integration;

import net.rai.scp_unleashed.scp002.FleshBlock;
import net.rai.scp_unleashed.scp002.MeteoricShellBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp002JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp002JadeProvider.Instance, MeteoricShellBlock.class);
        Registration.registerBlockComponent(Scp002JadeProvider.Instance, FleshBlock.class);
    }
}
