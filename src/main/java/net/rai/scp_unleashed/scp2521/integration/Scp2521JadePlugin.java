package net.rai.scp_unleashed.scp2521.integration;

import net.rai.scp_unleashed.scp2521.client.Scp2521ClientScene;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp2521JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.addRayTraceCallback((Hit, Found, Original) -> Scp2521ClientScene.cinematic() ? null : Found);
        Registration.addBeforeRenderCallback((Box, Rect, Graphics, Looked) -> Scp2521ClientScene.cinematic());
    }
}
