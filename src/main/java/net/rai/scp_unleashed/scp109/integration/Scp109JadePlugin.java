package net.rai.scp_unleashed.scp109.integration;

import net.rai.scp_unleashed.scp109.Scp109TapBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp109JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp109TapJadeProvider.Instance, Scp109TapBlock.class);
        Registration.registerFluidStorage(Scp109TapWaterProvider.Instance, Scp109TapBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp109TapJadeProvider.Instance, Scp109TapBlock.class);
        Registration.registerFluidStorageClient(Scp109TapWaterProvider.Instance);
    }
}
