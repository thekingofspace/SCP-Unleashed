package net.rai.scp_unleashed.scp106.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp106.Scp106Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp106ClientEvents {
    public static final ResourceLocation PocketEffects = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp106_pocket");
    public static final ResourceLocation VignetteLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp106_vignette");
    public static final ResourceLocation BlurLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp106_blur");

    private Scp106ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp106Registry.OldMan.get(), Scp106Renderer::new);
        Event.registerEntityRenderer(Scp106Registry.Restraint.get(), NoopRenderer::new);
        Event.registerEntityRenderer(Scp106Registry.Portal.get(), Scp106PortalRenderer::new);
        Event.registerBlockEntityRenderer(Scp106Registry.FemurBreakerEntity.get(), Scp106FemurRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(Scp106FemurRenderer.ArmModel);
        Event.register(Scp106FemurRenderer.LeverModel);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp106Registry.Goo.get(), Scp106Particles.GooProvider::new);
        Event.registerSpriteSet(Scp106Registry.Puddle.get(), Scp106Particles.PuddleProvider::new);
        Event.registerSpriteSet(Scp106Registry.Pool.get(), Scp106Particles.PoolProvider::new);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(PocketEffects, new Scp106PocketEffects());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(BlurLayer, Scp106PocketClient::renderBlur);
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, VignetteLayer, Scp106PocketClient::renderVignette);
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        if (Scp106PocketClient.inPocket(Minecraft.getInstance())) {
            Event.setRed(Scp106PocketClient.FogRed);
            Event.setGreen(Scp106PocketClient.FogGreen);
            Event.setBlue(Scp106PocketClient.FogBlue);
        }
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        if (!Scp106PocketClient.inPocket(Minecraft.getInstance())) {
            return;
        }
        float Far = Math.min(Event.getFarPlaneDistance(), Scp106PocketClient.fogFar((float) Event.getPartialTick()));
        Event.setNearPlaneDistance(Math.min(Event.getNearPlaneDistance(), 0.0f));
        Event.setFarPlaneDistance(Far);
        Event.setFogShape(FogShape.SPHERE);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp106PocketClient.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void selectMusic(SelectMusicEvent Event) {
        if (Scp106PocketClient.inPocket(Minecraft.getInstance())) {
            Event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp106PocketClient.reset();
    }
}
