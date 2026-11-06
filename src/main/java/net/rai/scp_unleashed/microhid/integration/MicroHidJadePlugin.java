package net.rai.scp_unleashed.microhid.integration;

import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlock;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class MicroHidJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(MicroChargingPlatformJadeProvider.Instance, MicroChargingPlatformBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(MicroChargingPlatformJadeProvider.Instance, MicroChargingPlatformBlock.class);
    }
}
