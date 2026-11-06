package net.rai.scp_unleashed.scp087.integration;

import net.rai.scp_unleashed.scp087.Scp087DoorBlock;
import net.rai.scp_unleashed.scp087.Scp087Hunter;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp087JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp087DoorProvider.Instance, Scp087DoorBlock.class);
        Registration.registerEntityComponent(Scp087HunterProvider.Instance, Scp087Hunter.class);
    }
}
