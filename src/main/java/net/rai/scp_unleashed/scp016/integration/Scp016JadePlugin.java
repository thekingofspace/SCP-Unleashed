package net.rai.scp_unleashed.scp016.integration;

import net.rai.scp_unleashed.scp016.Scp016Spawn;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp016JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp016SpawnProvider.Instance, Scp016Spawn.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp016SpawnProvider.Instance, Scp016Spawn.class);
    }
}
