package net.rai.scp_unleashed.scp079.integration;

import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import net.rai.scp_unleashed.scp079.Scp079Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp079JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, Scp079Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp079JadeProvider.Instance, Scp079Block.class);
    }
}
