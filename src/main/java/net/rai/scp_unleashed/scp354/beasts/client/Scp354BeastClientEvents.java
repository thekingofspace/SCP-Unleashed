package net.rai.scp_unleashed.scp354.beasts.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp354.beasts.Scp354Brute;
import net.rai.scp_unleashed.scp354.beasts.Scp354BeastRegistry;
import net.rai.scp_unleashed.scp354.beasts.Scp354Charger;
import net.rai.scp_unleashed.scp354.beasts.Scp354Crawler;
import net.rai.scp_unleashed.scp354.beasts.Scp354Leaper;
import net.rai.scp_unleashed.scp354.beasts.Scp354Wingbeast;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp354BeastClientEvents {
    private Scp354BeastClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp354BeastRegistry.Brute.get(), Context -> new Scp354BeastRenderer<>(Context, "scp354_brute", Scp354Brute.RenderScale, 1.5f));
        Event.registerEntityRenderer(Scp354BeastRegistry.Crawler.get(), Context -> new Scp354BeastRenderer<>(Context, "scp354_crawler", Scp354Crawler.RenderScale, 1.2f));
        Event.registerEntityRenderer(Scp354BeastRegistry.Wingbeast.get(), Context -> new Scp354BeastRenderer<>(Context, "scp354_wingbeast", Scp354Wingbeast.RenderScale, 1.0f));
        Event.registerEntityRenderer(Scp354BeastRegistry.Leaper.get(), Context -> new Scp354BeastRenderer<>(Context, "scp354_leaper", Scp354Leaper.RenderScale, 1.3f));
        Event.registerEntityRenderer(Scp354BeastRegistry.Charger.get(), Context -> new Scp354BeastRenderer<>(Context, "scp354_charger", Scp354Charger.RenderScale, 1.2f));
        Event.registerEntityRenderer(Scp354BeastRegistry.HurledEarth.get(), Scp354HurledEarthRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp354BeastRegistry.Drip.get(), Scp354BeastParticles.DripProvider::new);
        Event.registerSpriteSet(Scp354BeastRegistry.Splash.get(), Scp354BeastParticles.SplashProvider::new);
    }
}
