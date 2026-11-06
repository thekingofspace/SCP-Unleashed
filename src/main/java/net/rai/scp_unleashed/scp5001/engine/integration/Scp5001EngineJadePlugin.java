package net.rai.scp_unleashed.scp5001.engine.integration;

import net.rai.scp_unleashed.scp5001.engine.Scp5001EngineBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp5001EngineJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp5001EngineProvider.Instance, Scp5001EngineBlock.class);
    }
}
