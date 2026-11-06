package net.rai.scp_unleashed.scp472.integration;

import net.rai.scp_unleashed.scp472.Scp472BloodstoneBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp472JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp472StoneJadeProvider.Instance, Scp472BloodstoneBlock.class);
    }
}
