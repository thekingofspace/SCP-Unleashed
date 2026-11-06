package net.rai.scp_unleashed.scp076.integration;

import net.rai.scp_unleashed.scp076.Scp076Abel;
import net.rai.scp_unleashed.scp076.Scp076CoffinBlock;
import net.rai.scp_unleashed.scp076.Scp076DoorBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp076JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp076AbelProvider.Instance, Scp076Abel.class);
        Registration.registerBlockComponent(Scp076DoorProvider.Instance, Scp076DoorBlock.class);
        Registration.registerBlockComponent(Scp076CoffinProvider.Instance, Scp076CoffinBlock.class);
    }
}
