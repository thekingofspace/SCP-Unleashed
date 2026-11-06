package net.rai.scp_unleashed.scp002.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp002.MeteorEntity;
import net.rai.scp_unleashed.scp002.Scp002Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp002ClientEvents {
    public static final ModelLayerLocation MeteorLayer = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp002_meteor"), "main");
    public static final ResourceLocation OverlayLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp002_overlay");

    private Scp002ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        MeteorEntity.setClientHook(Scp002ClientEffects::meteorTick);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp002Registry.Meteor.get(), MeteorRenderer::new);
        Event.registerBlockEntityRenderer(Scp002Registry.DoorEntity.get(), BulkheadDoorRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : BulkheadDoorRenderer.AllModels) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(MeteorLayer, MeteorRenderer::createRock);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp002Registry.Ember.get(), EmberParticle.Provider::new);
        Event.registerSpriteSet(Scp002Registry.Tendril.get(), TendrilParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, OverlayLayer, Scp002ClientEffects::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp002ClientEffects.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        Scp002ClientEffects.shake(Event);
    }

    @SubscribeEvent
    public static void fieldOfView(ViewportEvent.ComputeFov Event) {
        Scp002ClientEffects.fov(Event);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp002ClientEffects.reset();
    }
}
