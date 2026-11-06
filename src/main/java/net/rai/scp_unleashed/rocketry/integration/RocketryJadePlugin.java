package net.rai.scp_unleashed.rocketry.integration;

import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformBlock;
import net.rai.scp_unleashed.rocketry.rocket.DropPodEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class RocketryJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(LaunchPlatformJadeProvider.Instance, LaunchPlatformBlock.class);
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, LaunchPlatformBlock.class);
        Registration.registerEntityDataProvider(RocketJadeProvider.Instance, RocketEntity.class);
        Registration.registerEntityDataProvider(RocketJadeProvider.Instance, DropPodEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(LaunchPlatformJadeProvider.Instance, LaunchPlatformBlock.class);
        Registration.registerEntityComponent(RocketJadeProvider.Instance, RocketEntity.class);
        Registration.registerEntityComponent(RocketJadeProvider.Instance, DropPodEntity.class);
    }
}
