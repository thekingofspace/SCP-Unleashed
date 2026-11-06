package net.rai.scp_unleashed.instability.integration;

import net.rai.scp_unleashed.instability.InstabilityReactorBlock;
import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class InstabilityJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, InstabilityReactorBlock.class);
        Registration.registerBlockDataProvider(InstabilityJadeProvider.Instance, InstabilityReactorBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(InstabilityJadeProvider.Instance, InstabilityReactorBlock.class);
    }
}
