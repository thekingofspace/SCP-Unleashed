package net.rai.scp_unleashed.scp009.integration;

import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp009.RedIceBlock;
import net.rai.scp_unleashed.scp009.Scp009Statue;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp009JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(DoomJadeProvider.Instance, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(RedIceJadeProvider.Instance, RedIceBlock.class);
        Registration.registerBlockComponent(ContainmentJadeProvider.Instance, StorageContainerBlock.class);
        Registration.registerEntityComponent(DoomJadeProvider.Instance, LivingEntity.class);
        Registration.registerEntityComponent(StatueJadeProvider.Instance, Scp009Statue.class);
    }
}
