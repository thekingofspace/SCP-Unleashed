package net.rai.scp_unleashed.scp3008.staff.integration;

import net.rai.scp_unleashed.scp3008.staff.Scp3008Staff;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp3008StaffJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp3008StaffJadeProvider.Instance, Scp3008Staff.class);
    }
}
