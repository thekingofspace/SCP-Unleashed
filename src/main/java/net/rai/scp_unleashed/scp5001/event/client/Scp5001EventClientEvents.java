package net.rai.scp_unleashed.scp5001.event.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp5001EventClientEvents {
    private Scp5001EventClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Scp5001Vessel.ClientTick = Scp5001Hallucinations::vesselTick;
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp5001EventRegistry.VesselType.get(), Scp5001VesselRenderer::new);
        Event.registerEntityRenderer(Scp5001EventRegistry.MimicType.get(), Scp5001MimicRenderer::new);
        Event.registerBlockEntityRenderer(Scp5001EventRegistry.RodEntity.get(), Scp5001RodRenderer::new);
        Event.registerBlockEntityRenderer(Scp5001EventRegistry.LaserEntity.get(), Scp5001LaserRenderer::new);
        Event.registerBlockEntityRenderer(Scp5001EventRegistry.GeneratorEntity.get(), Scp5001GeneratorRenderer::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp5001Hallucinations.LayerId, Scp5001Hallucinations::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp5001Hallucinations.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void hud(RenderGuiLayerEvent.Pre Event) {
        Scp5001Hallucinations.hud(Event);
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles Event) {
        Scp5001Hallucinations.camera(Event);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp5001Hallucinations.reset();
        Scp5001ClientState.reset();
    }
}
