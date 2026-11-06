package net.rai.scp_unleashed.scp860.integration;

import net.rai.scp_unleashed.scp860.Scp860DoorBlock;
import net.rai.scp_unleashed.scp860.Scp860Watcher;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp860JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp860DoorJadeProvider.Instance, Scp860DoorBlock.class);
        Registration.registerEntityComponent(Scp860WatcherJadeProvider.Instance, Scp860Watcher.class);
    }
}
