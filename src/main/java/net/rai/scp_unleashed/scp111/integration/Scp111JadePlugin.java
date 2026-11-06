package net.rai.scp_unleashed.scp111.integration;

import net.rai.scp_unleashed.scp111.Scp111EggBlock;
import net.rai.scp_unleashed.scp111.Scp111Snail;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp111JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp111SnailJadeProvider.Instance, Scp111Snail.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp111SnailJadeProvider.Instance, Scp111Snail.class);
        Registration.registerBlockComponent(Scp111EggJadeProvider.Instance, Scp111EggBlock.class);
        Registration.registerBlockComponent(Scp111ShelfJadeProvider.Instance, StorageContainerBlock.class);
    }
}
