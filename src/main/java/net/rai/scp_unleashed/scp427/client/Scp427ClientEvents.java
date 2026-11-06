package net.rai.scp_unleashed.scp427.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp427.Scp427Registry;
import net.rai.scp_unleashed.scp427.integration.Scp427CuriosRenderer;
import net.rai.scp_unleashed.senses.Senses;
import software.bernie.geckolib.event.GeoRenderEvent;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp427ClientEvents {
    public static final ResourceLocation OpenProperty = Scp427Registry.location("scp427_open");
    public static final double SensitivityScale = 0.6;
    public static final double SensitivityFloor = 0.2;
    public static final float SluggishShare = 0.3f;
    public static final float ShakeRoll = 3.5f;
    public static final float ShakePitch = 1.6f;

    private Scp427ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Scp427Registry.Locket.get(), OpenProperty, Scp427ClientLocket::property);
            Scp427Display.register();
            if (Senses.curiosLoaded()) {
                Scp427CuriosRenderer.register();
            }
        });
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp427Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp427Registry.Flesh.get(), Scp427FleshRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp427Registry.MoteParticle.get(), Scp427MoteParticle.Provider::new);
        Event.registerSpriteSet(Scp427Registry.FleshParticle.get(), Scp427FleshParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(Scp427LocketExtensions.Instance, Scp427Registry.Locket.get());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp427Overlay.LayerId, Scp427Overlay::render);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.isPaused()) {
            return;
        }
        Scp427ClientState.tick();
        Scp427ClientLocket.tick(Client.level);
    }

    private static float sluggish(float Partial) {
        float Amount = Scp427ClientState.transform(Partial);
        if (Amount <= 0.0f) {
            return 1.0f;
        }
        float Ramp = Mth.clamp(Amount * 8.0f, 0.0f, 1.0f);
        return 1.0f - (1.0f - SluggishShare) * Ramp;
    }

    @SubscribeEvent
    public static void playerTurn(CalculatePlayerTurnEvent Event) {
        float Share = sluggish(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        if (Share >= 1.0f) {
            return;
        }
        double Base = Event.getMouseSensitivity() * SensitivityScale + SensitivityFloor;
        Event.setMouseSensitivity((Base * Math.cbrt(Share) - SensitivityFloor) / SensitivityScale);
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles Event) {
        float Partial = (float) Event.getPartialTick();
        float Amount = Scp427ClientState.transform(Partial);
        if (Amount <= 0.0f) {
            return;
        }
        float Clock = Scp427ClientState.clock(Partial);
        float Strength = 0.4f + 0.6f * Amount;
        Event.setRoll(Event.getRoll() + Mth.sin(Clock * 2.7f) * ShakeRoll * Strength);
        Event.setPitch(Event.getPitch() + Mth.sin(Clock * 3.3f + 1.0f) * ShakePitch * Strength);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void hideOvercome(RenderLivingEvent.Pre<?, ?> Event) {
        if (Scp427ClientState.vanished(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderLiving(RenderLivingEvent.Pre<?, ?> Event) {
        Scp427Convulsion.apply(Event.getEntity(), Event.getPoseStack(), Event.getPartialTick());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void renderLivingPost(RenderLivingEvent.Post<?, ?> Event) {
        Scp427Convulsion.undo(Event.getEntity(), Event.getPoseStack());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderGeo(GeoRenderEvent.Entity.Pre Event) {
        Scp427Convulsion.apply(Event.getEntity(), Event.getPoseStack(), Event.getPartialTick());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void renderGeoPost(GeoRenderEvent.Entity.Post Event) {
        Scp427Convulsion.forget(Event.getEntity());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp427ClientState.reset();
        Scp427ClientLocket.reset();
        Scp427Convulsion.reset();
    }
}
