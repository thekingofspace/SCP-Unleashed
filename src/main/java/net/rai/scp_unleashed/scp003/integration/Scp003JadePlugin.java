package net.rai.scp_unleashed.scp003.integration;

import net.rai.scp_unleashed.scp003.Scp003BoardBlock;
import net.rai.scp_unleashed.scp003.Scp003Creature;
import net.rai.scp_unleashed.scp003.Scp003PlateBlock;
import net.rai.scp_unleashed.scp003.Scp003PolypBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp003JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp003BlockProvider.Instance, Scp003BoardBlock.class);
        Registration.registerEntityDataProvider(Scp003CreatureProvider.Instance, Scp003Creature.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp003BlockProvider.Instance, Scp003BoardBlock.class);
        Registration.registerBlockComponent(Scp003BlockProvider.Instance, Scp003PlateBlock.class);
        Registration.registerBlockComponent(Scp003BlockProvider.Instance, Scp003PolypBlock.class);
        Registration.registerEntityComponent(Scp003CreatureProvider.Instance, Scp003Creature.class);
    }
}
