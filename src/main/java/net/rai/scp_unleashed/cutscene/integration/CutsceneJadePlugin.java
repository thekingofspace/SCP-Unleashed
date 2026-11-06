package net.rai.scp_unleashed.cutscene.integration;

import net.rai.scp_unleashed.cutscene.client.CutsceneClient;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class CutsceneJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.addRayTraceCallback((Hit, Accessor, Original) -> CutsceneClient.playing() ? null : Accessor);
        Registration.addBeforeRenderCallback((Box, Rect, Graphics, Accessor) -> CutsceneClient.playing());
    }
}
