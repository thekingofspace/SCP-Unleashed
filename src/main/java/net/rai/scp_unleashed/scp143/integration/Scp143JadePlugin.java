package net.rai.scp_unleashed.scp143.integration;

import net.rai.scp_unleashed.scp143.Scp143LeavesBlock;
import net.rai.scp_unleashed.scp143.Scp143LogBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp143JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp143JadeProvider.Instance, Scp143LogBlock.class);
        Registration.registerBlockComponent(Scp143JadeProvider.Instance, Scp143LeavesBlock.class);
    }
}
