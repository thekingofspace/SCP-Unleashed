package net.rai.scp_unleashed.scp409.integration;

import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp409.Scp409CaseBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp409JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp409CrystalJadeProvider.Instance, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp409CrystalJadeProvider.Instance, LivingEntity.class);
        Registration.registerBlockComponent(Scp409CaseJadeProvider.Instance, Scp409CaseBlock.class);
    }
}
