package net.rai.scp_unleashed.z0003.integration;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.z0003.Z0003Device;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Z0003JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Z0003DeviceProvider.Instance, Z0003Device.class);
        Registration.registerEntityDataProvider(Z0003StatueProvider.Instance, Player.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Z0003DeviceProvider.Instance, Z0003Device.class);
        Registration.registerEntityComponent(Z0003StatueProvider.Instance, LivingEntity.class);
    }
}
