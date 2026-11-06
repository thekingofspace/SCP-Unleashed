package net.rai.scp_unleashed.scp323.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
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
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.scp323.Scp323Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp323ClientEvents {
    private Scp323ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp323Display::register);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp323Registry.Skull.get(), Scp323SkullRenderer::new);
        Event.registerEntityRenderer(Scp323Registry.Beast.get(), Scp323BeastRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(Scp323HornShape.LayerLocation, Scp323HornShape::create);
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        for (PlayerSkin.Model Skin : Event.getSkins()) {
            EntityRenderer<?> Found = Event.getSkin(Skin);
            if (Found instanceof LivingEntityRenderer<?, ?> Living && Living.getModel() instanceof PlayerModel<?>) {
                LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> Player = (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) Living;
                Player.addLayer(new Scp323HornLayer(Player, Event.getEntityModels()));
            }
        }
        for (EntityType<?> Type : Event.getEntityTypes()) {
            EntityRenderer<?> Found = Event.getRenderer(Type);
            if (Found instanceof DClassRenderer<?> Geo) {
                attach(Geo, Event);
            }
        }
    }

    private static <T extends DClassLike> void attach(DClassRenderer<T> Renderer, EntityRenderersEvent.AddLayers Event) {
        Renderer.addRenderLayer(new Scp323GeoHornLayer<>(Renderer, Event.getEntityModels()));
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp323Overlay.WatchLayerId, Scp323Overlay::renderWatch);
        Event.registerAbove(Scp323Overlay.WatchLayerId, Scp323Overlay.LureLayerId, Scp323Overlay::renderLure);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        Scp323ClientWatch.hideHand(Minecraft.getInstance());
        if (!Scp323ClientWatch.watching()) {
            Scp323ClientLure.frame(Event.getPartialTick());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void turn(CalculatePlayerTurnEvent Event) {
        if (Scp323ClientWatch.watching()) {
            Event.setMouseSensitivity(Scp323ClientWatch.FrozenSensitivity);
            Event.setCinematicCameraEnabled(false);
            return;
        }
        Event.setMouseSensitivity(Scp323ClientLure.sensitivity(Event.getMouseSensitivity()));
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Pre Event) {
        if (Event.getEntity() instanceof LocalPlayer User && User == Minecraft.getInstance().player && !Scp323ClientWatch.watching()) {
            Scp323ClientLure.drift(User);
        }
    }

    @SubscribeEvent
    public static void clientTickPre(ClientTickEvent.Pre Event) {
        Scp323ClientWatch.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void clientTickPost(ClientTickEvent.Post Event) {
        Scp323ClientLure.tick(Minecraft.getInstance());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent Event) {
        Scp323ClientWatch.freeze(Event.getInput());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered Event) {
        if (Scp323ClientWatch.watching()) {
            Event.setCanceled(true);
            Event.setSwingHand(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void scroll(InputEvent.MouseScrollingEvent Event) {
        if (Scp323ClientWatch.watching() && Minecraft.getInstance().screen == null) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent Event) {
        if (Scp323ClientWatch.watching()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void guiLayer(RenderGuiLayerEvent.Pre Event) {
        if (Scp323ClientWatch.watching() && VanillaGuiLayers.CROSSHAIR.equals(Event.getName())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderPlayer(RenderPlayerEvent.Pre Event) {
        if (Scp323ClientBonds.hidden(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp323ClientLure.reset();
        Scp323ClientWatch.reset();
        Scp323ClientBonds.clear();
    }
}
