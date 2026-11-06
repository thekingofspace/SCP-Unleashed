package net.rai.scp_unleashed.particleemitter.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.particleemitter.ParticleEmitterBlockEntity;
import net.rai.scp_unleashed.particleemitter.ParticleEmitterRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ParticleEmitterClientEvents {
    private ParticleEmitterClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        ParticleEmitterBlockEntity.setClientTicker(EmitterEffects::tick);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(ParticleEmitterRegistry.EmitterEntity.get(), ParticleEmitterRenderer::new);
    }
}
