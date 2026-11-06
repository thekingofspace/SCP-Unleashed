package net.rai.scp_unleashed.inkwire.integration;

import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.guards.PatrolNodeBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class InkwireJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(InkJadeProvider.Instance, OutletBlock.class);
        Registration.registerBlockComponent(InkJadeProvider.Instance, PatrolNodeBlock.class);
    }
}
