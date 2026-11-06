package net.rai.scp_unleashed.door.integration;

import net.rai.scp_unleashed.door.DoorButtonBlock;
import net.rai.scp_unleashed.door.DoorLeverBlock;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class DoorJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(DoorJadeProvider.Instance, DoorButtonBlock.class);
        Registration.registerBlockComponent(DoorJadeProvider.Instance, KeycardScannerBlock.class);
        Registration.registerBlockComponent(DoorJadeProvider.Instance, DoorLeverBlock.class);
    }
}
