package net.rai.scp_unleashed.scp372.integration;

import net.rai.scp_unleashed.scp372.Scp372AnchorBlock;
import net.rai.scp_unleashed.scp372.Scp372Insect;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp372JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp372JadeProvider.Instance, Scp372AnchorBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp372JadeProvider.Instance, Scp372AnchorBlock.class);
        Registration.registerEntityComponent(Scp372JadeEntityProvider.Instance, Scp372Insect.class);
    }
}
