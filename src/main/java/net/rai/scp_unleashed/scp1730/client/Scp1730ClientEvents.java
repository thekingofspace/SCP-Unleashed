package net.rai.scp_unleashed.scp1730.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp1730ClientEvents {
    public static final ResourceLocation FacilityEffects = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp1730_facility");
    public static final ResourceLocation AuraLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp1730_aura");
    public static final ResourceLocation PulseLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp1730_anchor_pulse");

    private Scp1730ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp1730Registry.PainterType.get(), Context -> new Scp1730EntityRenderer<>(Context, "scp1730_1", "look", 1.0f, 0.4f, true));
        Event.registerEntityRenderer(Scp1730Registry.GuardianType.get(), Context -> new Scp1730EntityRenderer<>(Context, "scp1730_2", null, 1.65f, 2.4f, true));
        Event.registerEntityRenderer(Scp1730Registry.LeechType.get(), Context -> new Scp1730EntityRenderer<>(Context, "scp1730_leech", null, 0.45f, 0.14f, true));
        Event.registerEntityRenderer(Scp1730Registry.TentacleType.get(), Context -> new Scp1730EntityRenderer<>(Context, "scp1730_tentacle", null, 1.0f, 0.0f, false));
        Event.registerEntityRenderer(Scp1730Registry.AnchorType.get(), ThrownItemRenderer::new);
        Event.registerEntityRenderer(Scp1730Registry.FieldType.get(), Scp1730FieldRenderer::new);
        Event.registerBlockEntityRenderer(Scp1730Registry.TurretEntity.get(), Context -> new Scp1730BlockRenderer<>(Context, "turret", 0.5, 0.5, 64));
        Event.registerBlockEntityRenderer(Scp1730Registry.ThresherEntity.get(), Context -> new Scp1730BlockRenderer<>(Context, "thresher", 3.0, 7.5, 128));
        Event.registerBlockEntityRenderer(Scp1730Registry.FolderEntity.get(), Context -> new Scp1730BlockRenderer<>(Context, "reality_folder", 2.6, 6.2, 128));
        Event.registerBlockEntityRenderer(Scp1730Registry.LaserEntity.get(), Scp1730LaserRenderer::new);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new Scp1730AnchorClientExtensions(), Scp1730Registry.Anchor.get());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp1730Registry.GlyphParticle.get(), Scp1730Particles.GlyphProvider::new);
        Event.registerSpriteSet(Scp1730Registry.AnchorParticle.get(), Scp1730Particles.AnchorProvider::new);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(FacilityEffects, new Scp1730FacilityEffects());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, AuraLayer, Scp1730ClientAura::overlay);
        Event.registerAbove(AuraLayer, PulseLayer, Scp1730AnchorPulse::overlay);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp1730ClientHooks.tick();
        Scp1730ClientAura.tick();
        Scp1730AnchorPulse.tick();
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        Scp1730ClientHooks.pullFrame();
    }

    @SubscribeEvent
    public static void turning(CalculatePlayerTurnEvent Event) {
        if (!Scp1730ClientHooks.holding()) {
            return;
        }
        double Speed = Event.getMouseSensitivity() * 0.6 + 0.2;
        double Reduced = Speed * Math.cbrt(Scp1730ClientHooks.ControlShare);
        Event.setMouseSensitivity(Math.max(0.0, (Reduced - 0.2) / 0.6));
    }

    @SubscribeEvent
    public static void angles(ViewportEvent.ComputeCameraAngles Event) {
        float Shake = Scp1730ClientHooks.shakeStrength();
        if (Shake <= 0.0f) {
            return;
        }
        double Time = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) + Event.getPartialTick();
        float Amount = Shake * 1.8f;
        Event.setYaw(Event.getYaw() + (float) (Math.sin(Time * 1.7) * 0.6 + Math.sin(Time * 3.9) * 0.4) * Amount);
        Event.setPitch(Event.getPitch() + (float) (Math.cos(Time * 2.3) * 0.5 + Math.sin(Time * 5.1) * 0.5) * Amount);
        Event.setRoll(Event.getRoll() + (float) Math.sin(Time * 1.3) * Amount * 0.8f);
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double Seconds = (Client.level.getGameTime() % 24000L + Partial) / 20.0;
        Scp1730ClientAura.render(Event.getModelViewMatrix(), Event.getProjectionMatrix(), Event.getCamera().getPosition(), Partial, Mth.frac(Seconds / 600.0) * 600.0);
        Scp1730AnchorPulse.render(Event.getModelViewMatrix(), Event.getProjectionMatrix(), Event.getCamera().getPosition());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp1730ClientHooks.reset();
        Scp1730ClientAura.reset();
        Scp1730AnchorPulse.reset();
    }
}
