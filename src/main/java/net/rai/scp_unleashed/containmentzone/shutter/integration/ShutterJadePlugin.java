package net.rai.scp_unleashed.containmentzone.shutter.integration;

import net.rai.scp_unleashed.containmentzone.CellShutterBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ShutterJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(ShutterJadeProvider.Instance, CellShutterBlock.class);
    }
}
