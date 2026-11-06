package net.rai.scp_unleashed.worldend.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class WorldEndClientEvents {
    public static final double SensitivityScale = 0.6;
    public static final double SensitivityFloor = 0.2;

    private WorldEndClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAboveAll(WorldEndOverlay.Layer, WorldEndOverlay::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        WorldEndClient.tick(Client);
        WorldEndGate.tick(Client);
    }

    @SubscribeEvent
    public static void clientTickStart(ClientTickEvent.Pre Event) {
        if (WorldEndGate.engaged()) {
            WorldEndGate.drain(Minecraft.getInstance().options);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void screenOpening(ScreenEvent.Opening Event) {
        if (WorldEndClient.cinematic() && (Event.getNewScreen() instanceof ChatScreen || Event.getNewScreen() instanceof AbstractContainerScreen<?>)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        WorldEndGate.release(Minecraft.getInstance());
        WorldEndClient.reset();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerTurn(CalculatePlayerTurnEvent Event) {
        if (!WorldEndClient.cinematic()) {
            return;
        }
        Event.setMouseSensitivity(-SensitivityFloor / SensitivityScale);
        Event.setCinematicCameraEnabled(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent Event) {
        if (!WorldEndClient.cinematic()) {
            return;
        }
        Event.getInput().forwardImpulse = 0.0f;
        Event.getInput().leftImpulse = 0.0f;
        Event.getInput().up = false;
        Event.getInput().down = false;
        Event.getInput().left = false;
        Event.getInput().right = false;
        Event.getInput().jumping = false;
        Event.getInput().shiftKeyDown = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        if (!WorldEndClient.cinematic()) {
            return;
        }
        float Partial = (float) Event.getPartialTick();
        Event.setYaw(WorldEndClient.yaw(Partial, Event.getYaw()));
        Event.setPitch(WorldEndClient.pitch(Partial, Event.getPitch()));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void hideHand(RenderHandEvent Event) {
        if (WorldEndClient.cinematic()) {
            Event.setCanceled(true);
        }
    }
}
