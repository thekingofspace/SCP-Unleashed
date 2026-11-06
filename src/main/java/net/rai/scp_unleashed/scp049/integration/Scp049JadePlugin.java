package net.rai.scp_unleashed.scp049.integration;

import net.rai.scp_unleashed.scp049.Scp049Cured;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import net.rai.scp_unleashed.scp049.Scp049TrayBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp049JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(Scp049DoctorProvider.Instance, Scp049Doctor.class);
        Registration.registerEntityDataProvider(Scp049CuredProvider.Instance, Scp049Cured.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp049DoctorProvider.Instance, Scp049Doctor.class);
        Registration.registerEntityComponent(Scp049CuredProvider.Instance, Scp049Cured.class);
        Registration.registerBlockComponent(Scp049TrayProvider.Instance, Scp049TrayBlock.class);
    }
}
