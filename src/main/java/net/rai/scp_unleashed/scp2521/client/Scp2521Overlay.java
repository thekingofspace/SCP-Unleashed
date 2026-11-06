package net.rai.scp_unleashed.scp2521.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp2521.Scp2521Registry;

public final class Scp2521Overlay {
    public static final ResourceLocation CinemaLayer = Scp2521Registry.location("scp2521_cinema");
    public static final float BarShare = 0.11f;

    private Scp2521Overlay() {
    }

    public static boolean hidesHud() {
        return Scp2521ClientScene.cinematic();
    }

    public static void cinema(GuiGraphics Graphics, DeltaTracker Delta) {
        if (!Scp2521ClientScene.cinematic()) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        float Dark = Scp2521ClientScene.darkness(Partial);
        if (Dark > 0.0f) {
            Graphics.fill(0, 0, Width, Height, ((int) (Dark * 255.0f) << 24));
        }
        float Bars = Scp2521ClientScene.bars(Partial);
        if (Bars > 0.0f && Client.player != null) {
            int Bar = Math.round(Height * BarShare * Bars);
            Graphics.fill(0, 0, Width, Bar, 0xFF000000);
            Graphics.fill(0, Height - Bar, Width, Height, 0xFF000000);
        }
    }
}
