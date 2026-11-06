package net.rai.scp_unleashed.z0003.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0003.Z0003Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Z0003ClientEvents {
    public static final ResourceLocation GlowProperty = Z0003Registry.location("z0003_glow");

    private Z0003ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(Z0003Registry.Device.get(), GlowProperty, Z0003Glow::property);
            Z0003Display.register();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Z0003Registry.DeviceType.get(), Z0003DeviceRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Z0003Glow.GlowModels) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Z0003Blackout.LayerId, Z0003Blackout::render);
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Z0003FieldTint.LayerId, Z0003FieldTint::render);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void entityTick(EntityTickEvent.Pre Event) {
        if (Event.getEntity().level().isClientSide && Z0003StatueClient.tick(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        if (Event.getLevel().isClientSide()) {
            Z0003StatueClient.forget(Event.getEntity().getId());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void clientTick(ClientTickEvent.Pre Event) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null) {
            return;
        }
        Z0003StatueClient.clientTick(Game);
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof LocalPlayer User) {
            Z0003StatueClient.holdLocal(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent Event) {
        Z0003StatueClient.steer(Event.getInput());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerTurn(CalculatePlayerTurnEvent Event) {
        if (Z0003StatueClient.isLocked()) {
            Event.setMouseSensitivity(Z0003StatueClient.FrozenSensitivity);
            Event.setCinematicCameraEnabled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered Event) {
        if (Z0003StatueClient.isLocked()) {
            Event.setSwingHand(false);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scroll(InputEvent.MouseScrollingEvent Event) {
        if (Z0003StatueClient.isLocked() && Minecraft.getInstance().screen == null) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void opening(ScreenEvent.Opening Event) {
        if (Z0003StatueClient.isLocked() && Event.getNewScreen() instanceof AbstractContainerScreen<?>) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent Event) {
        if (Z0003StatueClient.isLocked()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void guiLayer(RenderGuiLayerEvent.Pre Event) {
        if (Z0003StatueClient.isLocked() && VanillaGuiLayers.CROSSHAIR.equals(Event.getName())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Z0003StatueClient.reset();
        Z0003FieldTint.reset();
        Z0003FieldPass.reset();
    }
}
