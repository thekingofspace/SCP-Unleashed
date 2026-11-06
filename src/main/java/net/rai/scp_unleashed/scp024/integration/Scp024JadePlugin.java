package net.rai.scp_unleashed.scp024.integration;

import net.rai.scp_unleashed.scp024.Scp024DoorBlock;
import net.rai.scp_unleashed.scp024.Scp024SolidBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp024JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp024DoorJadeProvider.Instance, Scp024DoorBlock.class);
        Registration.registerBlockComponent(Scp024SealedJadeProvider.Instance, Scp024SolidBlock.class);
    }
}
