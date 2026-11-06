package net.rai.scp_unleashed.scp372.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp372.Scp372Events;
import net.rai.scp_unleashed.scp372.Scp372Glimpse;
import net.rai.scp_unleashed.scp372.Scp372Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp372ClientEvents {
    private Scp372ClientEvents() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent Event) {
        Scp372Glimpse.ClientViewer = () -> Minecraft.getInstance().player;
        Scp372Events.GlimpseHandler = Scp372ClientGlimpses::receive;
        Scp372Events.PulseHandler = Payload -> Scp372Pulse.start(Payload.Strength());
        Scp372Events.HushHandler = Payload -> Scp372ClientGlimpses.hush();
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp372Registry.Insect.get(), Scp372Renderer::new);
        Event.registerEntityRenderer(Scp372Registry.Glimpse.get(), Scp372GlimpseRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp372Registry.Flare.get(), Scp372Particles.FlareProvider::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAboveAll(Scp372Pulse.LayerId, Scp372Pulse::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        Scp372ClientGlimpses.tick(Game);
        Scp372Pulse.tick(Game);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp372ClientGlimpses.reset();
        Scp372Pulse.reset();
    }
}
