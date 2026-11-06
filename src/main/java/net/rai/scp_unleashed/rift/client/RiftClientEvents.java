package net.rai.scp_unleashed.rift.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rift.RiftRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class RiftClientEvents {
    private RiftClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(RiftRegistry.Rift.get(), RiftRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(RiftRegistry.RiftMote.get(), Sprites -> new RiftParticle.Provider(Sprites, RiftParticle.Style.Mote));
        Event.registerSpriteSet(RiftRegistry.RiftSpark.get(), Sprites -> new RiftParticle.Provider(Sprites, RiftParticle.Style.Spark));
        Event.registerSpriteSet(RiftRegistry.RiftShockwave.get(), Sprites -> new RiftParticle.Provider(Sprites, RiftParticle.Style.Shockwave));
    }
}
