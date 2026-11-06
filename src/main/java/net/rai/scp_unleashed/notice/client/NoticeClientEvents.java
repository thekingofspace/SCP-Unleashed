package net.rai.scp_unleashed.notice.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class NoticeClientEvents {
    public static final ResourceLocation NoticeLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "notices");

    private NoticeClientEvents() {
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.TITLE, NoticeLayer, NoticeHud::render);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent Event) {
        Event.registerReloadListener((ResourceManagerReloadListener) Manager -> NoticeIcons.clear());
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        NoticeHud.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void beforeGui(RenderGuiEvent.Pre Event) {
        NoticeHud.beginFrame();
    }

    @SubscribeEvent
    public static void bossBar(CustomizeGuiOverlayEvent.BossEventProgress Event) {
        NoticeHud.bossBar(Event.getY());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        NoticeHud.reset();
    }
}
