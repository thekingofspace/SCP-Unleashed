package net.rai.scp_unleashed.scp179.integration;

import net.rai.scp_unleashed.scp179.SatelliteTerminalBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp179JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(SatelliteTerminalJadeProvider.Instance, SatelliteTerminalBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(SatelliteTerminalJadeProvider.Instance, SatelliteTerminalBlock.class);
    }
}
