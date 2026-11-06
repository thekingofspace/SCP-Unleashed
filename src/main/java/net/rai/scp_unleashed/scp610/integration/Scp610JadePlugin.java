package net.rai.scp_unleashed.scp610.integration;

import net.rai.scp_unleashed.scp610.Scp610Creature;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp610JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp610CreatureProvider.Instance, Scp610Creature.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp610CreatureProvider.Instance, Scp610Creature.class);
    }
}
