package net.rai.scp_unleashed.scp4845.integration;

import net.rai.scp_unleashed.scp4845.block.Scp4845CounterBlock;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp4845JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp4845EmployeeJadeProvider.Instance, Scp4845Employee.class);
        Registration.registerBlockComponent(Scp4845CounterJadeProvider.Instance, Scp4845CounterBlock.class);
    }
}
