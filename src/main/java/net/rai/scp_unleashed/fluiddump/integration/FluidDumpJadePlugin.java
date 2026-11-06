package net.rai.scp_unleashed.fluiddump.integration;

import net.rai.scp_unleashed.fluiddump.FluidDumpBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class FluidDumpJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(FluidDumpJadeProvider.Instance, FluidDumpBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(FluidDumpJadeProvider.Instance, FluidDumpBlock.class);
    }
}
