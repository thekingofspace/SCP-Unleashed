package net.rai.scp_unleashed.scp5001.mekhane.integration;

import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyConsoleBlock;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyIntakeBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class MekhaneJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(MekhaneJade.Intake, Scp5001EmergencyIntakeBlock.class);
        Registration.registerBlockComponent(MekhaneJade.Console, Scp5001EmergencyConsoleBlock.class);
    }
}
