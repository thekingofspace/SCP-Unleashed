package net.rai.scp_unleashed.mtf.integration;

import net.rai.scp_unleashed.mtf.MtfOperative;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class MtfJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(MtfJadeProvider.Instance, MtfOperative.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(MtfJadeProvider.Instance, MtfOperative.class);
    }
}
