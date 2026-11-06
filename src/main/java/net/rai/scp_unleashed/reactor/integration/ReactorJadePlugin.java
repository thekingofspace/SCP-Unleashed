package net.rai.scp_unleashed.reactor.integration;

import net.rai.scp_unleashed.reactor.IrradiationChamberBlock;
import net.rai.scp_unleashed.reactor.ReactorControllerBlock;
import net.rai.scp_unleashed.reactor.TowerSegmentBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ReactorJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(TowerJadeProvider.Instance, TowerSegmentBlock.class);
        Registration.registerBlockDataProvider(ControllerJadeProvider.Instance, ReactorControllerBlock.class);
        Registration.registerBlockDataProvider(ChamberJadeProvider.Instance, IrradiationChamberBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(TowerJadeProvider.Instance, TowerSegmentBlock.class);
        Registration.registerBlockComponent(ControllerJadeProvider.Instance, ReactorControllerBlock.class);
        Registration.registerBlockComponent(ChamberJadeProvider.Instance, IrradiationChamberBlock.class);
    }
}
