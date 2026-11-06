package net.rai.scp_unleashed.disperser.integration;

import net.rai.scp_unleashed.disperser.DisperserBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class DisperserJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(DisperserJadeProvider.Instance, DisperserBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(DisperserJadeProvider.Instance, DisperserBlock.class);
    }
}
