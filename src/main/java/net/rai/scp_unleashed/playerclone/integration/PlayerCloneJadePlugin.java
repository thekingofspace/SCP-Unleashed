package net.rai.scp_unleashed.playerclone.integration;

import net.rai.scp_unleashed.playerclone.PlayerClone;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class PlayerCloneJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(PlayerCloneJadeProvider.Instance, PlayerClone.class);
    }
}
