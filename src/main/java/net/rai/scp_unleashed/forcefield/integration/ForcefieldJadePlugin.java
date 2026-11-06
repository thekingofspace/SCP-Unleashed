package net.rai.scp_unleashed.forcefield.integration;

import net.rai.scp_unleashed.forcefield.ForcefieldBarrierBlock;
import net.rai.scp_unleashed.forcefield.ForcefieldEmitterBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ForcefieldJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ForcefieldJadeProvider.Instance, ForcefieldEmitterBlock.class);
        Registration.registerBlockDataProvider(ForcefieldJadeProvider.Instance, ForcefieldBarrierBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(ForcefieldJadeProvider.Instance, ForcefieldEmitterBlock.class);
        Registration.registerBlockComponent(ForcefieldJadeProvider.Instance, ForcefieldBarrierBlock.class);
    }
}
