package net.rai.scp_unleashed.guards.integration;

import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.guards.PatrolManagerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class GuardsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(GuardJadeProvider.Instance, DClass.class);
        Registration.registerBlockDataProvider(PatrolManagerJadeProvider.Instance, PatrolManagerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(GuardJadeProvider.Instance, DClass.class);
        Registration.registerBlockComponent(PatrolManagerJadeProvider.Instance, PatrolManagerBlock.class);
    }
}
