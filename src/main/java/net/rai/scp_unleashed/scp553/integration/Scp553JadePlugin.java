package net.rai.scp_unleashed.scp553.integration;

import net.rai.scp_unleashed.scp553.Scp553Butterfly;
import net.rai.scp_unleashed.scp553.Scp553StalactiteBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp553JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp553JadeProvider.Instance, Scp553Butterfly.class);
        Registration.registerBlockComponent(Scp553StalactiteJadeProvider.Instance, Scp553StalactiteBlock.class);
    }
}
