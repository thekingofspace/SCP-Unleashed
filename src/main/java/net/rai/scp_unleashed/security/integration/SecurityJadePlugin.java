package net.rai.scp_unleashed.security.integration;

import net.rai.scp_unleashed.security.MotionDetectorBlock;
import net.rai.scp_unleashed.security.SentryTurretBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SecurityJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(TurretJadeProvider.Instance, SentryTurretBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(TurretJadeProvider.Instance, SentryTurretBlock.class);
        Registration.registerBlockComponent(DetectorJadeProvider.Instance, MotionDetectorBlock.class);
    }
}
