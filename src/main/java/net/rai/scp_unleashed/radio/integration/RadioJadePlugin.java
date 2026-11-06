package net.rai.scp_unleashed.radio.integration;

import net.rai.scp_unleashed.radio.RadioTowerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class RadioJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(RadioTowerJadeProvider.Instance, RadioTowerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(RadioTowerJadeProvider.Instance, RadioTowerBlock.class);
    }
}
