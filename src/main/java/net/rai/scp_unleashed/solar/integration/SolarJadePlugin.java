package net.rai.scp_unleashed.solar.integration;

import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import net.rai.scp_unleashed.solar.SolarArrayBlock;
import net.rai.scp_unleashed.solar.SolarPanelBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SolarJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, SolarArrayBlock.class);
        Registration.registerBlockDataProvider(SolarJadeProvider.Instance, SolarPanelBlock.class);
        Registration.registerBlockDataProvider(SolarJadeProvider.Instance, SolarArrayBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(SolarJadeProvider.Instance, SolarPanelBlock.class);
        Registration.registerBlockComponent(SolarJadeProvider.Instance, SolarArrayBlock.class);
    }
}
