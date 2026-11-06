package net.rai.scp_unleashed.scp457.integration;

import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457AshPileBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp457JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp457JadeProvider.Instance, Scp457.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp457JadeProvider.Instance, Scp457.class);
        Registration.registerBlockComponent(Scp457AshPileJadeProvider.Instance, Scp457AshPileBlock.class);
    }
}
