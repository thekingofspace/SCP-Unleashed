package net.rai.scp_unleashed.z0002.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
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
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0002.Z0002Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Z0002ClientEvents {
    private Z0002ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Z0002Display::register);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Z0002Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Z0002Registry.Shrine.get(), Z0002ShrineRenderer::new);
        Event.registerEntityRenderer(Z0002Registry.Slash.get(), Z0002SlashRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Z0002Registry.Vapour.get(), Z0002Particles.VapourProvider::new);
        Event.registerSpriteSet(Z0002Registry.Spark.get(), Z0002Particles.SparkProvider::new);
        Event.registerSpriteSet(Z0002Registry.Shard.get(), Z0002Particles.ShardProvider::new);
        Event.registerSpriteSet(Z0002Registry.Mote.get(), Z0002Particles.MoteProvider::new);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(Z0002FingerExtensions.Instance, Z0002Registry.Finger.get());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Z0002Warning.LayerId, Z0002Warning::render);
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        float Strength = Z0002ClientDomain.strength((float) Event.getPartialTick());
        if (Strength <= 0.0f) {
            return;
        }
        Event.setRed(Mth.lerp(Strength, Event.getRed(), Z0002ClientDomain.FogRed));
        Event.setGreen(Mth.lerp(Strength, Event.getGreen(), Z0002ClientDomain.FogGreen));
        Event.setBlue(Mth.lerp(Strength, Event.getBlue(), Z0002ClientDomain.FogBlue));
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        float Strength = Z0002ClientDomain.strength((float) Event.getPartialTick());
        if (Strength <= 0.0f) {
            return;
        }
        float Far = Mth.lerp(Strength, Event.getFarPlaneDistance(), Math.min(Event.getFarPlaneDistance(), Z0002ClientDomain.FogFar));
        float Near = Mth.lerp(Strength, Event.getNearPlaneDistance(), Math.min(Event.getNearPlaneDistance(), Z0002ClientDomain.FogNear));
        Event.setNearPlaneDistance(Near);
        Event.setFarPlaneDistance(Far);
        Event.setFogShape(FogShape.SPHERE);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Z0002ClientDomain.tick(Minecraft.getInstance());
        Z0002ClientChannel.tick(Minecraft.getInstance());
        Z0002Warning.tick();
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Z0002ClientDomain.reset();
        Z0002ClientChannel.reset();
        Z0002Warning.reset();
    }
}
