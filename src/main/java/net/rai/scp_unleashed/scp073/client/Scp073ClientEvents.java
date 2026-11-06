package net.rai.scp_unleashed.scp073.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp073.Scp073Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp073ClientEvents {
    private Scp073ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp073Registry.Cain.get(), Scp073Renderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp073Registry.Glint.get(), Scp073Particles.GlintProvider::new);
        Event.registerSpriteSet(Scp073Registry.Flash.get(), Scp073Particles.FlashProvider::new);
        Event.registerSpriteSet(Scp073Registry.Leaf.get(), Scp073Particles.LeafProvider::new);
    }
}
