package net.rai.scp_unleashed.scp439.integration;

import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp439.Scp439Creature;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp439JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp439HiveProvider.Instance, Scp439Hive.class);
        Registration.registerEntityDataProvider(Scp439HostProvider.Instance, LivingEntity.class);
        Registration.registerEntityDataProvider(Scp439ColonyProvider.Instance, Scp439Creature.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp439HiveProvider.Instance, Scp439Hive.class);
        Registration.registerEntityComponent(Scp439HostProvider.Instance, LivingEntity.class);
        Registration.registerEntityComponent(Scp439ColonyProvider.Instance, Scp439Creature.class);
    }
}
