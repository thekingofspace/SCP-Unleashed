package net.rai.scp_unleashed.scp3008.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public final class Scp3008Overlay {
    public static final ResourceLocation FadeLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp3008_fade");
    public static final ResourceLocation NightLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp3008_night");
    public static final float ShaderNight = 0.8f;

    private Scp3008Overlay() {
    }

    public static void fade(GuiGraphics Graphics, DeltaTracker Delta) {
        float Level = Scp3008ClientState.fadeLevel(Delta.getGameTimeDeltaPartialTick(false));
        if (Level <= 0.004f) {
            return;
        }
        int Alpha = Mth.clamp((int) (Level * 255.0f), 0, 255);
        Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), Alpha << 24);
    }

    public static void night(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp3008ClientState.inside(Client) || !ShaderCompat.useFallback()) {
            return;
        }
        float Dark = 1.0f - Scp3008ClientState.brightness(Delta.getGameTimeDeltaPartialTick(false));
        if (Dark <= 0.004f) {
            return;
        }
        int Alpha = Mth.clamp((int) (Dark * ShaderNight * 255.0f), 0, 255);
        Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), Alpha << 24 | 0x02030A);
    }
}
