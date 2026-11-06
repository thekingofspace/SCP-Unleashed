package net.rai.scp_unleashed.scp335.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.client.ComputerScreen;
import net.rai.scp_unleashed.computer.client.apps.ImageView;
import net.rai.scp_unleashed.scp335.Scp335Image;
import net.rai.scp_unleashed.scp335.Scp335Phase;
import net.rai.scp_unleashed.scp335.Scp335Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp335ClientEvents {
    public static final ResourceLocation OverlayLayer = Scp335Registry.location("scp335_static");
    public static final double StillSensitivity = -0.2 / 0.6;

    private Scp335ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        ImageView.register(Scp335Image.Picture, Scp335ImagePainter::paint);
        Event.enqueueWork(Scp335Display::register);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp335Registry.Pixel.get(), Scp335PixelParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, OverlayLayer, Scp335Overlay::render);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void keys(ClientTickEvent.Pre Event) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player != null && Scp335ClientState.isLocked()) {
            Scp335ClientState.consumeKeys(Game);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp335ClientState.tick(Minecraft.getInstance());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movementInput(MovementInputUpdateEvent Event) {
        Scp335ClientState.steer(Event.getEntity(), Event.getInput());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerTurn(CalculatePlayerTurnEvent Event) {
        if (Scp335ClientState.isLocked()) {
            Event.setMouseSensitivity(StillSensitivity);
            Event.setCinematicCameraEnabled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered Event) {
        if (Scp335ClientState.isLocked()) {
            Event.setSwingHand(false);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scroll(InputEvent.MouseScrollingEvent Event) {
        if (Scp335ClientState.isLocked() && Minecraft.getInstance().screen == null) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void opening(ScreenEvent.Opening Event) {
        Scp335ClientState.Shown State = Scp335ClientState.local();
        if (State == null || !State.Phase.isLocked()) {
            return;
        }
        if (Event.getNewScreen() instanceof AbstractContainerScreen<?> || Event.getNewScreen() instanceof ComputerScreen && State.Phase != Scp335Phase.Glimpse) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderPlayer(RenderPlayerEvent.Pre Event) {
        if (Scp335ClientState.isHidden(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderHand(RenderHandEvent Event) {
        if (Scp335ClientState.isHidden(Minecraft.getInstance().player)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp335ClientState.clear();
    }

    @SubscribeEvent
    public static void respawned(ClientPlayerNetworkEvent.Clone Event) {
        if (Event.getOldPlayer().isDeadOrDying()) {
            Scp335ClientState.clearLocal();
        }
    }
}
