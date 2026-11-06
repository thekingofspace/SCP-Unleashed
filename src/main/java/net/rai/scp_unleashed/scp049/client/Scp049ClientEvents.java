package net.rai.scp_unleashed.scp049.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp049.Scp049Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp049ClientEvents {
    private Scp049ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp049Registry.Doctor.get(), Scp049Renderer::new);
        Event.registerEntityRenderer(Scp049Registry.Cured.get(), Scp049CuredRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp049Registry.Miasma.get(), Sprites -> new Scp049Particle.Provider(Sprites, Scp049Particle.Style.Miasma));
        Event.registerSpriteSet(Scp049Registry.Spore.get(), Sprites -> new Scp049Particle.Provider(Sprites, Scp049Particle.Style.Spore));
        Event.registerSpriteSet(Scp049Registry.Glint.get(), Sprites -> new Scp049Particle.Provider(Sprites, Scp049Particle.Style.Glint));
        Event.registerSpriteSet(Scp049Registry.Glow.get(), Sprites -> new Scp049Particle.Provider(Sprites, Scp049Particle.Style.Glow));
        Event.registerSpriteSet(Scp049Registry.CureSpore.get(), Sprites -> new Scp049Particle.Provider(Sprites, Scp049Particle.Style.CureSpore));
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp049Skins.reset();
        Scp049Mimicry.reset();
    }
}
