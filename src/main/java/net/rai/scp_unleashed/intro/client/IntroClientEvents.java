package net.rai.scp_unleashed.intro.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.intro.IntroRegistry;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class IntroClientEvents {
    public static final String KeyCategory = "key.categories.scp_unleashed.intro";
    public static final IKeyConflictContext SceneContext = new IKeyConflictContext() {
        @Override
        public boolean isActive() {
            return IntroClient.playing();
        }

        @Override
        public boolean conflicts(IKeyConflictContext Other) {
            return Other == this;
        }
    };
    public static final KeyMapping SkipKey = new KeyMapping("key.scp_unleashed.intro_skip", SceneContext, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_ENTER, KeyCategory);
    public static final ResourceLocation SceneLayer = IntroRegistry.location("intro_scene");

    private IntroClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent Event) {
        Event.register(SkipKey);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(IntroRegistry.Scientist.get(), IntroScientistRenderer::new);
        Event.registerEntityRenderer(IntroRegistry.Staff.get(), DClassRenderer::new);
        Event.registerBlockEntityRenderer(IntroRegistry.GateCoreEntity.get(), IntroGateRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAboveAll(SceneLayer, IntroOverlay::render);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        IntroClient.tick();
        IntroHand.sync(IntroClient.handHidden());
    }

    @SubscribeEvent
    public static void tickStart(ClientTickEvent.Pre Event) {
        IntroHand.sync(IntroClient.handHidden());
        if (!IntroClient.active()) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        while (Client.options.keyInventory.consumeClick()) {
            continue;
        }
        while (Client.options.keyDrop.consumeClick()) {
            continue;
        }
        while (Client.options.keySwapOffhand.consumeClick()) {
            continue;
        }
        while (Client.options.keyTogglePerspective.consumeClick()) {
            continue;
        }
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        IntroClient.frame(Event.getPartialTick().getRealtimeDeltaTicks(), Event.getPartialTick().getGameTimeDeltaPartialTick(true));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent Event) {
        if (!IntroClient.active()) {
            return;
        }
        Input Held = Event.getInput();
        boolean Walk = IntroClient.walking();
        Held.forwardImpulse = Walk ? 1.0f : 0.0f;
        Held.leftImpulse = 0.0f;
        Held.up = Walk;
        Held.down = false;
        Held.left = false;
        Held.right = false;
        Held.jumping = false;
        Held.shiftKeyDown = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered Event) {
        if (!IntroClient.active()) {
            return;
        }
        Event.setSwingHand(false);
        Event.setCanceled(true);
        if (Event.isAttack() || Event.isUseItem()) {
            IntroClient.advance();
        }
    }

    @SubscribeEvent
    public static void turning(CalculatePlayerTurnEvent Event) {
        if (IntroClient.cameraHeld()) {
            Event.setMouseSensitivity(-1.0 / 3.0);
            Event.setCinematicCameraEnabled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hand(RenderHandEvent Event) {
        if (IntroClient.handHidden()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        IntroClient.reset();
        IntroHand.release();
    }
}
