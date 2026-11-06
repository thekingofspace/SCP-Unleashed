package net.rai.scp_unleashed.refinery.integration;

import net.rai.scp_unleashed.refinery.OrePurifierBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class RefineryJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(OrePurifierJadeProvider.Instance, OrePurifierBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(OrePurifierJadeProvider.Instance, OrePurifierBlock.class);
    }
}
