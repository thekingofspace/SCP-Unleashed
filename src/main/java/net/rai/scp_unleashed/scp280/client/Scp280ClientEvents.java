package net.rai.scp_unleashed.scp280.client;

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
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp280.Scp280Events;
import net.rai.scp_unleashed.scp280.Scp280Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp280ClientEvents {
    private Scp280ClientEvents() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent Event) {
        Scp280Events.EyesHandler = Scp280ClientEyes::handle;
        Scp280Events.MeterHandler = Scp280Hud::accept;
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp280Registry.Shade.get(), Scp280ShadeRenderer::new);
        Event.registerEntityRenderer(Scp280Registry.Eyes.get(), Scp280EyesRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp280Registry.Wisp.get(), Scp280WispParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp280Hud.VignetteLayer, Scp280Hud::renderVignette);
        Event.registerAbove(VanillaGuiLayers.HOTBAR, Scp280Hud.IconLayer, Scp280Hud::renderIcon);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp280ClientEyes.tick(Client);
        Scp280Hud.tick(Client);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp280ClientEyes.reset();
        Scp280Hud.reset();
    }
}
