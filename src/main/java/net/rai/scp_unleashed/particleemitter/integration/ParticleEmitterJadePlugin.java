package net.rai.scp_unleashed.particleemitter.integration;

import net.rai.scp_unleashed.particleemitter.ParticleEmitterBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ParticleEmitterJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(ParticleEmitterJadeProvider.Instance, ParticleEmitterBlock.class);
    }
}
