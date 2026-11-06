package net.rai.scp_unleashed.scp1762.integration;

import net.rai.scp_unleashed.scp1762.Scp1762BoxBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp1762JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp1762BoxJadeProvider.Instance, Scp1762BoxBlock.class);
    }
}
