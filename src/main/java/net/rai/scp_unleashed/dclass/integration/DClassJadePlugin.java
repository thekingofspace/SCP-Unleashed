package net.rai.scp_unleashed.dclass.integration;

import net.rai.scp_unleashed.dclass.CloningMachineBlock;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.theodore.Theodore;
import net.rai.scp_unleashed.dclass.theodore.TheodoreAmalgamation;
import net.rai.scp_unleashed.dclass.theodore.integration.AmalgamationJadeProvider;
import net.rai.scp_unleashed.dclass.theodore.integration.TheodoreJadeProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class DClassJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(CloningJadeProvider.Instance, CloningMachineBlock.class);
        Registration.registerEntityDataProvider(DClassJadeProvider.Instance, DClass.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(CloningJadeProvider.Instance, CloningMachineBlock.class);
        Registration.registerEntityComponent(DClassJadeProvider.Instance, DClass.class);
        Registration.registerEntityComponent(TheodoreJadeProvider.Instance, Theodore.class);
        Registration.registerEntityComponent(AmalgamationJadeProvider.Instance, TheodoreAmalgamation.class);
    }
}
