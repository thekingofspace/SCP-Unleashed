package net.rai.scp_unleashed.warhead.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.warhead.WarheadRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class WarheadClientEvents {
    public static final ResourceLocation AlertLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "warhead_alert");
    public static final ResourceLocation PlateLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "warhead_plate");

    private WarheadClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(WarheadRegistry.PanelEntity.get(), WarheadPanelRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : WarheadPanelRenderer.PartModels) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, AlertLayer, WarheadHud::renderAlert);
        Event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, PlateLayer, WarheadHud::renderPlate);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        WarheadAlarm.tick(Client);
        WarheadEffects.tick(Client);
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        WarheadEffects.shake(Event);
    }

    @SubscribeEvent
    public static void beforeGui(RenderGuiEvent.Pre Event) {
        WarheadHud.resetBossBars();
    }

    @SubscribeEvent
    public static void bossBar(CustomizeGuiOverlayEvent.BossEventProgress Event) {
        WarheadHud.bossBar(Event.getY(), Event.getIncrement());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        WarheadAlarm.reset();
        WarheadEffects.reset();
    }
}
