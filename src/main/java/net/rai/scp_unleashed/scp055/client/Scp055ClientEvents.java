package net.rai.scp_unleashed.scp055.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp055ClientEvents {
    private Scp055ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp055Display::register);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp055Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp055Distortion.LayerId, Scp055Distortion::render);
        Event.registerBelow(VanillaGuiLayers.CHAT, Scp055Static.LayerId, Scp055Static::render);
    }

    @SubscribeEvent
    public static void clientTickPre(ClientTickEvent.Pre Event) {
        Scp055ClientEpisode.drainKeys(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void clientTickPost(ClientTickEvent.Post Event) {
        Scp055ClientEpisode.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent Event) {
        Scp055ClientEpisode.steer(Event.getEntity(), Event.getInput());
    }

    @SubscribeEvent
    public static void playerTurn(CalculatePlayerTurnEvent Event) {
        if (Scp055ClientEpisode.controlling()) {
            Event.setMouseSensitivity(Scp055ClientEpisode.Frozen);
            Event.setCinematicCameraEnabled(false);
        }
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered Event) {
        if (Scp055ClientEpisode.controlling()) {
            Event.setSwingHand(false);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent Event) {
        if (Scp055ClientEpisode.controlling()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov Event) {
        float Scale = Scp055ClientEpisode.fovScale((float) Event.getPartialTick());
        if (Scale != 1.0f) {
            Event.setFOV(Event.getFOV() * Scale);
        }
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles Event) {
        float Roll = Scp055ClientEpisode.roll((float) Event.getPartialTick());
        if (Roll != 0.0f) {
            Event.setRoll(Event.getRoll() + Roll);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp055ClientEpisode.reset();
        Scp055Distortion.release();
        Scp055Static.release();
    }
}
