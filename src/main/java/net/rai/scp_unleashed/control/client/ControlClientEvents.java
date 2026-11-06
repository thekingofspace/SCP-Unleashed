package net.rai.scp_unleashed.control.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlRegistry;
import net.rai.scp_unleashed.control.ControllerItem;
import net.rai.scp_unleashed.control.network.ControlReleasePayload;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ControlClientEvents {
    public static final String KeyCategory = "key.categories.scp_unleashed.control";
    public static final KeyMapping ReleaseKey = new KeyMapping(ControllerItem.ReleaseKey, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, KeyCategory);
    public static final ResourceLocation OverlayLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "control_overlay");

    private ControlClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent Event) {
        Event.register(ReleaseKey);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(ControlRegistry.VacantBodyType.get(), VacantBodyRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, OverlayLayer, ControlOverlay::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        ControlOverlay.tick();
        VacantEyelids.tick();
        while (ReleaseKey.consumeClick()) {
            if (Game.player == null) {
                continue;
            }
            if (ControlClientState.isControlled(Game.player.getUUID())) {
                PacketDistributor.sendToServer(ControlReleasePayload.Instance);
            } else {
                Game.player.displayClientMessage(Component.translatable("message.scp_unleashed.control.not_linked").withStyle(ChatFormatting.GRAY), true);
            }
        }
        if (Game.level != null && !Game.isPaused()) {
            ControlPuppets.tick(Game.level);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void renderPlayer(RenderPlayerEvent.Pre Event) {
        if (ControlPuppets.isCapturing(Event.getEntity())) {
            return;
        }
        if (ControlPuppets.render(Event.getEntity(), Event.getPartialTick(), Event.getPoseStack(), Event.getMultiBufferSource(), Event.getPackedLight())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderedPlayer(RenderPlayerEvent.Post Event) {
        ControlPuppets.captured(Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void renderArm(RenderArmEvent Event) {
        if (ControlPuppets.renderArm(Event.getPlayer(), Event.getArm(), Event.getPoseStack(), Event.getMultiBufferSource(), Event.getPackedLight())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        ControlClientState.clear();
        ControlPuppets.clear();
        VacantEyelids.clear();
    }
}
