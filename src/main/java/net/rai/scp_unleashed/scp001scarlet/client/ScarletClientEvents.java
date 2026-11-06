package net.rai.scp_unleashed.scp001scarlet.client;

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
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ScarletClientEvents {
    private ScarletClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp001ScarletRegistry.CultistType.get(), ScarletCultistRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, ScarletMeterHud.Layer, ScarletMeterHud::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        if (!Minecraft.getInstance().isPaused()) {
            ScarletClient.tick();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        ScarletSky.fog(Event);
    }

    @SubscribeEvent
    public static void stage(RenderLevelStageEvent Event) {
        ScarletSky.streaks(Event);
        ScarletBeam.render(Event);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        ScarletClient.reset();
    }
}
