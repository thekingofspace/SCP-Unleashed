package net.rai.scp_unleashed.scp2521.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.client.DossierScreen;
import net.rai.scp_unleashed.scp2521.Scp2521Registry;
import net.rai.scp_unleashed.scp2521.Scp2521Void;
import net.rai.scp_unleashed.scp2521.network.Scp2521ReadPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp2521ClientEvents {
    public static final double SensitivityScale = 0.6;
    public static final double SensitivityFloor = 0.2;
    private static DossierScreen Reported;

    private Scp2521ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp2521Registry.CreatureType.get(), Scp2521Renderer::new);
        Event.registerEntityRenderer(Scp2521Registry.TendrilType.get(), Scp2521TendrilRenderer::new);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(Scp2521Registry.VoidEffects, new Scp2521VoidEffects());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAboveAll(Scp2521Overlay.CinemaLayer, Scp2521Overlay::cinema);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp2521ClientScene.tick(Client);
        Scp2521ClientGate.tick(Client);
    }

    @SubscribeEvent
    public static void clientTickStart(ClientTickEvent.Pre Event) {
        if (Scp2521ClientGate.engaged()) {
            Scp2521ClientGate.drain(Minecraft.getInstance().options);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void screenOpening(ScreenEvent.Opening Event) {
        if (Scp2521ClientScene.cinematic() && (Event.getNewScreen() instanceof ChatScreen || Event.getNewScreen() instanceof AbstractContainerScreen<?>)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void screenShown(ScreenEvent.Init.Post Event) {
        if (Event.getScreen() instanceof DossierScreen Dossier && Dossier != Reported && Scp2521Registry.DocumentId.equals(Dossier.shownDocument())
                && Minecraft.getInstance().getConnection() != null) {
            Reported = Dossier;
            PacketDistributor.sendToServer(Scp2521ReadPayload.Instance);
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp2521ClientScene.reset();
        Scp2521ClientGate.release(Minecraft.getInstance());
        Reported = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerTurn(CalculatePlayerTurnEvent Event) {
        if (!Scp2521ClientScene.cinematic()) {
            return;
        }
        Event.setMouseSensitivity(-SensitivityFloor / SensitivityScale);
        Event.setCinematicCameraEnabled(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent Event) {
        if (!Scp2521ClientScene.cinematic()) {
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
        if (!Scp2521ClientScene.steering()) {
            return;
        }
        float Partial = (float) Event.getPartialTick();
        Event.setYaw(Scp2521ClientScene.yaw(Partial));
        Event.setPitch(Scp2521ClientScene.pitch(Partial));
        Event.setRoll(Scp2521ClientScene.roll(Partial));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void hideHand(RenderHandEvent Event) {
        if (Scp2521ClientScene.cinematic()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        if (!Scp2521Void.inside(Minecraft.getInstance().level) || Event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        Event.setRed(0.0f);
        Event.setGreen(0.0f);
        Event.setBlue(0.0f);
    }
}
