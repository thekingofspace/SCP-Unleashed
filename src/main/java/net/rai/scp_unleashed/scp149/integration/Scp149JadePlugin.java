package net.rai.scp_unleashed.scp149.integration;

import net.rai.scp_unleashed.scp149.Scp149CocoonBlock;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import net.rai.scp_unleashed.scp149.Scp149Larva;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp149JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp149FlyJadeProvider.Instance, Scp149Fly.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp149FlyJadeProvider.Instance, Scp149Fly.class);
        Registration.registerEntityComponent(Scp149LarvaJadeProvider.Instance, Scp149Larva.class);
        Registration.registerBlockComponent(Scp149CocoonJadeProvider.Instance, Scp149CocoonBlock.class);
    }
}
