package net.rai.scp_unleashed.scp902.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import net.rai.scp_unleashed.scp902.Scp902Wastes;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp902ClientEvents {
    public static final ResourceLocation OpenProperty = Scp902Registry.location("scp902_open");
    public static final float FogStart = 6.0f;
    public static final float FogEnd = 92.0f;
    public static final float FogWarmth = 0.32f;
    public static final double SensitivityScale = 0.6;
    public static final double SensitivityFloor = 0.2;

    private Scp902ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(Scp902Registry.BoxItem.get(), OpenProperty, Scp902BoxItemRenderer::property));
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp902Models.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent Event) {
        Event.registerReloadListener((ResourceManagerReloadListener) Manager -> {
            Scp902Models.reload();
            Scp902Overlay.reset();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp902Registry.BoxEntity.get(), Scp902BoxRenderer::new);
        Event.registerEntityRenderer(Scp902Registry.Copy.get(), Scp902CopyRenderer::new);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer Drawer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (Drawer == null) {
                    Drawer = new Scp902BoxItemRenderer();
                }
                return Drawer;
            }
        }, Scp902Registry.BoxItem.get());
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(Scp902Registry.location("the_wastes"), new Scp902WastesEffects());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(Scp902Overlay.GradeLayer, Scp902Overlay::grade);
        Event.registerBelow(VanillaGuiLayers.CHAT, Scp902Overlay.CinemaLayer, Scp902Overlay::cinema);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp902ClientScene.tick(Client);
        Scp902Overlay.tick();
        Scp902ClientGate.tick(Client);
    }

    @SubscribeEvent
    public static void clientTickStart(ClientTickEvent.Pre Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Scp902ClientGate.engaged()) {
            Scp902ClientGate.drain(Client.options);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void screenOpening(ScreenEvent.Opening Event) {
        if (Scp902ClientScene.cinematic() && Event.getNewScreen() instanceof ChatScreen) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || !Scp902ClientScene.inScene()) {
            return;
        }
        Scp902Overhead.render(Event.getPoseStack(), Event.getCamera().getPosition(), Event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp902ClientScene.reset();
        Scp902Overlay.discard();
        Scp902ClientGate.release(Minecraft.getInstance());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void playerTurn(CalculatePlayerTurnEvent Event) {
        if (!Scp902ClientScene.locked()) {
            return;
        }
        Event.setMouseSensitivity(-SensitivityFloor / SensitivityScale);
        Event.setCinematicCameraEnabled(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        float Partial = (float) Event.getPartialTick();
        if (!Scp902ClientScene.inScene() || !Scp902Wastes.inside(Minecraft.getInstance().level)) {
            return;
        }
        Event.setYaw(Scp902Wastes.ArrivalYaw);
        Event.setRoll(Scp902ClientScene.roll(Partial));
        Event.setPitch(Scp902Wastes.ArrivalPitch + Scp902ClientScene.pitch(Partial));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void hideHand(RenderHandEvent Event) {
        if (Scp902ClientScene.cinematic()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp902Wastes.inside(Client.level) || Event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        Vector3f Look = Event.getCamera().getLookVector();
        Vector3f Sun = Scp902Sky.sunDirection();
        float Facing = (float) Math.pow(Math.max(0.0f, Look.x * Sun.x + Look.z * Sun.z), 3.0);
        Vec3 Base = Scp902Sky.Horizon.lerp(Scp902Sky.Ember, Mth.clamp(Facing * FogWarmth, 0.0f, 1.0f));
        Event.setRed((float) Base.x);
        Event.setGreen((float) Base.y);
        Event.setBlue((float) Base.z);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp902Wastes.inside(Client.level) || Event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float Far = Math.min(Event.getFarPlaneDistance(), FogEnd);
        Event.setNearPlaneDistance(FogStart);
        Event.setFarPlaneDistance(Far);
        Event.setCanceled(true);
    }
}
