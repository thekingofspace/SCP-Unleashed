package net.rai.scp_unleashed.scp001gate.client;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001gate.Scp001GateClientBridge;
import net.rai.scp_unleashed.scp001gate.Scp001GateRegistry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp001GateClientEvents {
    public static final float DustRed = 0.62f;
    public static final float DustGreen = 0.5f;
    public static final float DustBlue = 0.34f;
    public static final float FogShare = 0.55f;

    private Scp001GateClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Scp001GateClientBridge.install(Scp001GateEffects::guardianTick);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp001GateRegistry.GuardianType.get(), Scp001GateRenderer::new);
        Event.registerBlockEntityRenderer(Scp001GateRegistry.PortalEntity.get(), Scp001GatePortalRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp001GateOverlay.LayerId, Scp001GateOverlay::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp001GateEffects.tick();
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ShaderCompat.renderingShadowPass()) {
            return;
        }
        Scp001GateEffects.render(Event.getPoseStack(), Event.getCamera(), Event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        float Share = Scp001GateEffects.nearness() * FogShare;
        if (Share <= 0.01f) {
            return;
        }
        Event.setRed(Mth.lerp(Share, Event.getRed(), DustRed));
        Event.setGreen(Mth.lerp(Share, Event.getGreen(), DustGreen));
        Event.setBlue(Mth.lerp(Share, Event.getBlue(), DustBlue));
    }
}
