package net.rai.scp_unleashed.scp016.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp016.Scp016Brute;
import net.rai.scp_unleashed.scp016.Scp016Duelist;
import net.rai.scp_unleashed.scp016.Scp016Flyer;
import net.rai.scp_unleashed.scp016.Scp016Glutton;
import net.rai.scp_unleashed.scp016.Scp016Registry;
import net.rai.scp_unleashed.scp016.Scp016Skitter;
import net.rai.scp_unleashed.scp016.Scp016Slinger;
import net.rai.scp_unleashed.scp016.Scp016Swimmer;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp016ClientEvents {
    private Scp016ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp016Registry.SkitterType.get(), Context -> new Scp016Renderer<>(Context, new Scp016GeoModel<Scp016Skitter>("skitter", 25.0f, 50.0f), 0.55f));
        Event.registerEntityRenderer(Scp016Registry.BruteType.get(), Context -> new Scp016Renderer<>(Context, new Scp016GeoModel<Scp016Brute>("brute", 25.0f, 45.0f), 0.85f));
        Event.registerEntityRenderer(Scp016Registry.SlingerType.get(), Context -> new Scp016Renderer<>(Context, new Scp016GeoModel<Scp016Slinger>("slinger", 30.0f, 60.0f), 0.45f));
        Event.registerEntityRenderer(Scp016Registry.DuelistType.get(), Context -> new Scp016Renderer<>(Context, new Scp016GeoModel<Scp016Duelist>("duelist", 30.0f, 50.0f), 0.5f));
        Event.registerEntityRenderer(Scp016Registry.GluttonType.get(), Context -> new Scp016Renderer<>(Context, new Scp016GeoModel<Scp016Glutton>("glutton", 20.0f, 40.0f), 1.1f));
        Event.registerEntityRenderer(Scp016Registry.SwimmerType.get(), Context -> new Scp016Renderer<>(Context, new Scp016GeoModel<Scp016Swimmer>("swimmer", 20.0f, 30.0f), 0.5f));
        Event.registerEntityRenderer(Scp016Registry.FlyerType.get(), Context -> new Scp016Renderer<>(Context, new Scp016GeoModel<Scp016Flyer>("flyer", 25.0f, 45.0f), 0.6f));
        Event.registerEntityRenderer(Scp016Registry.ShardType.get(), Scp016ShardRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp016Registry.Fleck.get(), Scp016Particles.FleckProvider::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.ARMOR_LEVEL, Scp016Hud.LayerId, Scp016Hud::render);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp016ClientHooks.reset();
    }
}
