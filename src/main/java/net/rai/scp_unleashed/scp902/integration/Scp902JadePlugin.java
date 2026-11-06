package net.rai.scp_unleashed.scp902.integration;

import net.rai.scp_unleashed.scp902.Scp902BoxBlock;
import net.rai.scp_unleashed.scp902.client.Scp902ClientScene;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp902JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp902BoxJadeProvider.Instance, Scp902BoxBlock.class);
        Registration.addRayTraceCallback((Hit, Found, Original) -> Scp902ClientScene.cinematic() ? null : Found);
        Registration.addBeforeRenderCallback((Box, Rect, Graphics, Looked) -> Scp902ClientScene.cinematic());
    }
}
