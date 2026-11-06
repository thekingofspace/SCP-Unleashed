package net.rai.scp_unleashed.cutscene.client;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cutscene.Cutscene;
import net.rai.scp_unleashed.worldend.client.WorldEndClient;
import net.rai.scp_unleashed.intro.client.IntroClient;
import net.rai.scp_unleashed.scp2521.client.Scp2521Overlay;
import net.rai.scp_unleashed.scp902.client.Scp902Overlay;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class CutsceneClient {
    private static boolean Clamped;

    private CutsceneClient() {
    }

    @Nullable
    public static Cutscene active() {
        if (WorldEndClient.cinematic()) {
            return Cutscene.WorldEnd;
        }
        if (IntroClient.active()) {
            return Cutscene.Intro;
        }
        if (Scp902Overlay.hidesHud()) {
            return Cutscene.Scp902Trip;
        }
        if (Scp2521Overlay.hidesHud()) {
            return Cutscene.Scp2521Trip;
        }
        return null;
    }

    public static boolean playing() {
        return active() != null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void layer(RenderGuiLayerEvent.Pre Event) {
        if (Cutscene.hides(active(), Event.getName())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void clampLateHud(RenderGuiEvent.Post Event) {
        if (!Clamped && playing()) {
            GuiGraphics Graphics = Event.getGuiGraphics();
            Graphics.flush();
            Graphics.enableScissor(0, 0, 0, 0);
            Clamped = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void releaseLateHud(RenderGuiEvent.Post Event) {
        if (Clamped) {
            Event.getGuiGraphics().disableScissor();
            Clamped = false;
        }
    }
}
