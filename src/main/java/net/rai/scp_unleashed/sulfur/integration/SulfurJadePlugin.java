package net.rai.scp_unleashed.sulfur.integration;

import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SulfurJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(IndustrialTankJadeProvider.Instance, IndustrialTankBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(IndustrialTankJadeProvider.Instance, IndustrialTankBlock.class);
    }
}
