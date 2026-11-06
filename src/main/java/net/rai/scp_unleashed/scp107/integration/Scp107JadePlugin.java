package net.rai.scp_unleashed.scp107.integration;

import net.rai.scp_unleashed.scp107.Scp107ShellBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp107JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp107JadeProvider.Instance, Scp107ShellBlock.class);
    }
}
