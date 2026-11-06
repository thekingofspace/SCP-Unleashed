package net.rai.scp_unleashed.scp019.integration;

import net.rai.scp_unleashed.scp019.Scp019Creature;
import net.rai.scp_unleashed.scp019.Scp019PotBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp019JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp019CreatureProvider.Instance, Scp019Creature.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp019PotProvider.Instance, Scp019PotBlock.class);
        Registration.registerEntityComponent(Scp019CreatureProvider.Instance, Scp019Creature.class);
    }
}
