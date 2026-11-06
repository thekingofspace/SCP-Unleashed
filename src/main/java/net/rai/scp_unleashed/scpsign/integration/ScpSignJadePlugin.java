package net.rai.scp_unleashed.scpsign.integration;

import net.rai.scp_unleashed.scpsign.ScpSignBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ScpSignJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(ScpSignJadeProvider.Instance, ScpSignBlock.class);
    }
}
