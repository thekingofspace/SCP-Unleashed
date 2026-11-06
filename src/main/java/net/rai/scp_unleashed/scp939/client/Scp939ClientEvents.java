package net.rai.scp_unleashed.scp939.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp939.Scp939Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp939ClientEvents {
    public static final float RollDegrees = 1.6f;

    private Scp939ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp939Registry.Scp939Type.get(), Scp939Renderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(Scp939Registry.CompressorMenu.get(), GasCompressorScreen::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(Scp939Vision.LayerId, Scp939Vision::renderOverlay);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp939ClientState.tick(Client);
        Scp939Vision.tick(Client);
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Scp939Vision.renderLevel(Event.getPartialTick().getGameTimeDeltaPartialTick(false), Event.getPartialTick().getGameTimeDeltaTicks());
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        float Wobble = Scp939Vision.wobble();
        Minecraft Client = Minecraft.getInstance();
        if (Wobble <= 0.001f || Client.level == null) {
            return;
        }
        double Time = (Client.level.getGameTime() + Event.getPartialTick()) / 20.0;
        Event.setRoll(Event.getRoll() + (float) (Mth.sin((float) (Time * 1.3)) * RollDegrees * Wobble));
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp939ClientState.reset();
        Scp939Vision.reset();
    }
}
