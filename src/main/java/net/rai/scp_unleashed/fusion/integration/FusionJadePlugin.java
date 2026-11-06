package net.rai.scp_unleashed.fusion.integration;

import net.rai.scp_unleashed.fusion.FusionReactorBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class FusionJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(FusionJadeProvider.Instance, FusionReactorBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(FusionJadeProvider.Instance, FusionReactorBlock.class);
    }
}
