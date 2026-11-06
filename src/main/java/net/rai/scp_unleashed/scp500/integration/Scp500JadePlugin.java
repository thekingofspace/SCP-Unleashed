package net.rai.scp_unleashed.scp500.integration;

import net.rai.scp_unleashed.scp500.Scp500BottleBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp500JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp500BottleJadeProvider.Instance, Scp500BottleBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp500BottleJadeProvider.Instance, Scp500BottleBlock.class);
    }
}
