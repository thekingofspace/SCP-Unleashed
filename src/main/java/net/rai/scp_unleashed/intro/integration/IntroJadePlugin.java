package net.rai.scp_unleashed.intro.integration;

import net.rai.scp_unleashed.intro.client.IntroClient;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class IntroJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.addRayTraceCallback((Hit, Accessor, Original) -> IntroClient.active() ? null : Accessor);
        Registration.addBeforeRenderCallback((Box, Rect, Graphics, Accessor) -> IntroClient.active());
    }
}
