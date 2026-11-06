package net.rai.scp_unleashed.steam.integration;

import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import net.rai.scp_unleashed.steam.CoolingTowerBlock;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlock;
import net.rai.scp_unleashed.steam.SteamTurbineBlock;
import net.rai.scp_unleashed.steam.WaterBoilerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SteamJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(WaterBoilerJadeProvider.Instance, WaterBoilerBlock.class);
        Registration.registerBlockDataProvider(SteamTurbineJadeProvider.Instance, SteamTurbineBlock.class);
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, SteamTurbineBlock.class);
        Registration.registerBlockDataProvider(IndustrialTurbineJadeProvider.Instance, IndustrialTurbineBlock.class);
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, IndustrialTurbineBlock.class);
        Registration.registerBlockDataProvider(CoolingTowerJadeProvider.Instance, CoolingTowerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(WaterBoilerJadeProvider.Instance, WaterBoilerBlock.class);
        Registration.registerBlockComponent(SteamTurbineJadeProvider.Instance, SteamTurbineBlock.class);
        Registration.registerBlockComponent(IndustrialTurbineJadeProvider.Instance, IndustrialTurbineBlock.class);
        Registration.registerBlockComponent(CoolingTowerJadeProvider.Instance, CoolingTowerBlock.class);
    }
}
