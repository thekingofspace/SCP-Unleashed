package net.rai.scp_unleashed.scp354.integration;

import net.minecraft.world.entity.Mob;
import net.rai.scp_unleashed.scp354.Scp354Beast;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp354JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp354PoolBornProvider.Instance, Mob.class);
        Registration.registerEntityDataProvider(Scp354BeastProvider.Instance, Scp354Beast.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp354PoolBornProvider.Instance, Mob.class);
        Registration.registerEntityComponent(Scp354BeastProvider.Instance, Scp354Beast.class);
    }
}
