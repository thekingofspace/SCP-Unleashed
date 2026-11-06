package net.rai.scp_unleashed.scp087.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp087.Scp087Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp087ClientEvents {
    public static final ResourceLocation StairwellEffects = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp087_stairwell");
    public static final ResourceLocation VignetteLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp087_vignette");

    private Scp087ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp087Registry.Face.get(), Scp087FaceRenderer::new);
        Event.registerEntityRenderer(Scp087Registry.Hunter.get(), Scp087HunterRenderer::new);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(StairwellEffects, new Scp087StairwellEffects());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, VignetteLayer, Scp087Vignette::render);
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        if (Scp087ClientState.inStairwell(Minecraft.getInstance())) {
            Event.setRed(0.0f);
            Event.setGreen(0.0f);
            Event.setBlue(0.0f);
        }
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        if (!Scp087ClientState.inStairwell(Minecraft.getInstance())) {
            return;
        }
        float Far = Math.min(Event.getFarPlaneDistance(), Scp087ClientState.fog((float) Event.getPartialTick()));
        Event.setNearPlaneDistance(Math.min(Event.getNearPlaneDistance(), 0.0f));
        Event.setFarPlaneDistance(Far);
        Event.setFogShape(FogShape.SPHERE);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        if (Event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            Scp087SectionRefresh.run(Event.getCamera());
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp087ClientState.tick(Client);
        Scp087Drone.tickAll(Client);
    }

    @SubscribeEvent
    public static void selectMusic(SelectMusicEvent Event) {
        if (Scp087ClientState.inStairwell(Minecraft.getInstance())) {
            Event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp087ClientState.reset();
        Scp087SectionRefresh.reset();
        Scp087Drone.stopAll(Minecraft.getInstance());
    }
}
