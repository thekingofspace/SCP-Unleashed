package net.rai.scp_unleashed.dclass.duty.integration;

import net.rai.scp_unleashed.dclass.DClass;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class DClassDutyJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(DClassDutyJadeProvider.Instance, DClass.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(DClassDutyJadeProvider.Instance, DClass.class);
    }
}
