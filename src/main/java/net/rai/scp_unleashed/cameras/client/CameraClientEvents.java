package net.rai.scp_unleashed.cameras.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.Input;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cameras.CameraRegistry;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class CameraClientEvents {
    public static final String KeyCategory = "key.categories.scp_unleashed.cameras";
    public static final IKeyConflictContext FeedContext = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return CameraClient.isActive();
        }

        @Override
        public boolean conflicts(IKeyConflictContext Other) {
            return Other == this;
        }
    };
    public static final KeyMapping NextKey = new KeyMapping("key.scp_unleashed.camera_next", FeedContext, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, KeyCategory);
    public static final KeyMapping PreviousKey = new KeyMapping("key.scp_unleashed.camera_previous", FeedContext, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, KeyCategory);
    public static final KeyMapping LeaveKey = new KeyMapping("key.scp_unleashed.camera_leave", FeedContext, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_BACKSPACE, KeyCategory);
    public static final ResourceLocation FeedLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "camera_feed");
    private static final Set<ResourceLocation> Hidden = Set.of(VanillaGuiLayers.CAMERA_OVERLAYS, VanillaGuiLayers.CROSSHAIR, VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.JUMP_METER, VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.AIR_LEVEL, VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaGuiLayers.SPECTATOR_TOOLTIP,
            VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.EFFECTS);

    private CameraClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent Event) {
        Event.register(NextKey);
        Event.register(PreviousKey);
        Event.register(LeaveKey);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(CameraRegistry.CameraEntity.get(), CameraRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(CameraRenderer.HeadModel);
        Event.register(CameraRenderer.LedModel);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, FeedLayer, CameraOverlay::render);
    }

    @SubscribeEvent
    public static void tickStart(ClientTickEvent.Pre Event) {
        CameraClient.tickStart();
    }

    @SubscribeEvent
    public static void tickEnd(ClientTickEvent.Post Event) {
        CameraClient.tickEnd();
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        CameraClient.frame(Event.getPartialTick());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent Event) {
        if (!CameraClient.isActive()) {
            return;
        }
        Input Held = Event.getInput();
        Held.forwardImpulse = 0.0f;
        Held.leftImpulse = 0.0f;
        Held.up = false;
        Held.down = false;
        Held.left = false;
        Held.right = false;
        Held.jumping = false;
        Held.shiftKeyDown = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered Event) {
        if (CameraClient.isActive()) {
            Event.setSwingHand(false);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scrolled(InputEvent.MouseScrollingEvent Event) {
        if (!CameraClient.isActive()) {
            return;
        }
        Event.setCanceled(true);
        if (Event.getScrollDeltaY() != 0.0) {
            CameraClient.requestSwitch(Event.getScrollDeltaY() > 0.0 ? -1 : 1);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hand(RenderHandEvent Event) {
        if (CameraClient.isActive()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void layer(RenderGuiLayerEvent.Pre Event) {
        if (CameraClient.isActive() && Hidden.contains(Event.getName())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fov(ViewportEvent.ComputeFov Event) {
        if (CameraClient.isActive() && Event.usedConfiguredFov()) {
            Event.setFOV(CameraClient.Fov);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void opening(ScreenEvent.Opening Event) {
        if (CameraClient.isActive() && Event.getNewScreen() instanceof PauseScreen && CameraClient.escapePressed()) {
            Event.setCanceled(true);
            CameraClient.requestExit();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void bodyTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity().level().isClientSide) {
            CameraClient.holdBody(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void loggedIn(ClientPlayerNetworkEvent.LoggingIn Event) {
        CameraClientWindow.close();
    }

    @SubscribeEvent
    public static void respawned(ClientPlayerNetworkEvent.Clone Event) {
        CameraClientWindow.close();
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        CameraClient.reset();
        CameraClientWindow.close();
    }
}
