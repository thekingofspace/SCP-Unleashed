package net.rai.scp_unleashed.space.integration;

import net.rai.scp_unleashed.space.AirInfuserBlock;
import net.rai.scp_unleashed.space.AirInfuserBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SpaceJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(AirInfuserJadeProvider.Instance, AirInfuserBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(AirInfuserJadeProvider.Instance, AirInfuserBlock.class);
    }
}
