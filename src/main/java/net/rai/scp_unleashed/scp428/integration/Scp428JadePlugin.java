package net.rai.scp_unleashed.scp428.integration;

import net.rai.scp_unleashed.scp428.Scp428Core;
import net.rai.scp_unleashed.scp428.Scp428Instance;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp428JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp428CoreProvider.Instance, Scp428Core.class);
        Registration.registerEntityComponent(Scp428InstanceProvider.Instance, Scp428Instance.class);
    }
}
