package net.rai.scp_unleashed.scp4666.integration;

import net.rai.scp_unleashed.scp4666.Scp4666YuleMan;
import net.rai.scp_unleashed.scp4666.block.Scp4666CellarDoorBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666FireplaceBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666HeaterBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp4666JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp4666JadeProvider.Instance, Scp4666YuleMan.class);
        Registration.registerBlockComponent(Scp4666CellarDoorJade.Instance, Scp4666CellarDoorBlock.class);
        Registration.registerBlockComponent(Scp4666HearthJade.Fireplace, Scp4666FireplaceBlock.class);
        Registration.registerBlockComponent(Scp4666HearthJade.Boiler, Scp4666HeaterBlock.class);
    }
}
