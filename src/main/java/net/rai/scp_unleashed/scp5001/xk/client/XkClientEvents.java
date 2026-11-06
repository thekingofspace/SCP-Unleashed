package net.rai.scp_unleashed.scp5001.xk.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class XkClientEvents {
    private XkClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp5001XkRegistry.YaldabaothType.get(), YaldabaothEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(XkDread.Layer, XkDread::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.isPaused()) {
            return;
        }
        XkSky.tick();
        YaldabaothClient.tick();
        XkDread.tick(Client);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        XkSky.fog(Event);
    }

    @SubscribeEvent
    public static void stage(RenderLevelStageEvent Event) {
        YaldabaothSkyRenderer.stage(Event);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        XkSky.reset();
        YaldabaothClient.clear();
        XkDread.reset();
    }
}
