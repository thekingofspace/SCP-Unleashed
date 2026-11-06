package net.rai.scp_unleashed.daevite.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.DaeviteRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class DaeviteClientEvents {
    private DaeviteClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(DaeviteRegistry.Knight.get(), DaeviteKnightRenderer::new);
        Event.registerEntityRenderer(DaeviteRegistry.Overseer.get(), DaeviteOverseerRenderer::new);
        Event.registerEntityRenderer(DaeviteRegistry.Bolt.get(), DaeviteBoltRenderer::new);
        Event.registerEntityRenderer(DaeviteRegistry.Circle.get(), DaeviteCircleRenderer::new);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        DaeviteSkyClient.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        float Strength = DaeviteSkyClient.strength((float) Event.getPartialTick());
        if (Strength <= 0.001f) {
            return;
        }
        Event.setRed(Mth.lerp(Strength, Event.getRed(), DaeviteSkyClient.FogRed));
        Event.setGreen(Mth.lerp(Strength, Event.getGreen(), DaeviteSkyClient.FogGreen));
        Event.setBlue(Mth.lerp(Strength, Event.getBlue(), DaeviteSkyClient.FogBlue));
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        float Strength = DaeviteSkyClient.strength((float) Event.getPartialTick());
        if (Strength <= 0.001f) {
            return;
        }
        float Far = Event.getFarPlaneDistance();
        if (Event.getMode() == FogRenderer.FogMode.FOG_SKY) {
            float Target = Mth.lerp(Strength, Far, DaeviteSkyClient.SkyFar);
            Event.setFarPlaneDistance(Target);
            Event.setNearPlaneDistance(Math.min(Event.getNearPlaneDistance(), Target * 0.1f));
        } else {
            float Target = Mth.lerp(Strength * 0.75f, Far, Math.min(Far, DaeviteSkyClient.LandFar));
            Event.setFarPlaneDistance(Target);
            Event.setNearPlaneDistance(Math.min(Event.getNearPlaneDistance(), Target * 0.35f));
        }
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        DaeviteSkyClient.reset();
    }
}
