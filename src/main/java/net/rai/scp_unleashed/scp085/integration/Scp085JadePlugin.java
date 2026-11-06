package net.rai.scp_unleashed.scp085.integration;

import net.rai.scp_unleashed.scp085.Scp085SheetBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp085JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp085JadeProvider.Instance, Scp085SheetBlock.class);
    }
}
