package net.rai.scp_unleashed.scp999.integration;

import net.rai.scp_unleashed.scp999.PinkDogBowlBlock;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp999JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp999JadeProvider.Instance, Scp999Blob.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp999JadeProvider.Instance, Scp999Blob.class);
        Registration.registerBlockComponent(PinkDogBowlJadeProvider.Instance, PinkDogBowlBlock.class);
    }
}
