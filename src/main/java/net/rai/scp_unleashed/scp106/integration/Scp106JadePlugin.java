package net.rai.scp_unleashed.scp106.integration;

import net.rai.scp_unleashed.scp106.Scp106FemurBreakerBlock;
import net.rai.scp_unleashed.scp106.Scp106OldMan;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp106JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp106FemurProvider.Instance, Scp106FemurBreakerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp106FemurProvider.Instance, Scp106FemurBreakerBlock.class);
        Registration.registerEntityComponent(Scp106OldManProvider.Instance, Scp106OldMan.class);
    }
}
