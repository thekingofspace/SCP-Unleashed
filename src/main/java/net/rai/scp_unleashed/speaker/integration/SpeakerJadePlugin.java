package net.rai.scp_unleashed.speaker.integration;

import net.rai.scp_unleashed.speaker.SpeakerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SpeakerJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(SpeakerPortProvider.Instance, SpeakerBlock.class);
    }
}
