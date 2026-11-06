package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

public final class Scp4666Overlay {
    public static final ResourceLocation LayerId = Scp4666Registry.location("scp4666_overlay");
    public static final ResourceLocation Sackcloth = Scp4666Registry.location("textures/gui/scp4666/sack.png");
    public static final int Steps = 8;
    public static final int Band = 6;
    public static final float BuryReach = 0.92f;
    public static final int BuryStep = 6;

    private Scp4666Overlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp4666ClientState.inside(Client) || Client.options.hideGui) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        float Wake = Scp4666ClientState.wake(Partial);
        float Held = Scp4666ClientState.held() ? 0.55f : 0.0f;
        float Edge = Math.max(Wake, Held);
        if (Edge > 0.01f) {
            vignette(Graphics, Width, Height, Edge, 0x05060A);
        }
        float Flash = Scp4666ClientState.flash(Partial);
        if (Flash > 0.01f) {
            int Alpha = Mth.clamp((int) (Flash * 110.0f), 0, 255);
            Graphics.fill(0, 0, Width, Height, Alpha << 24 | 0x6A0E0C);
            vignette(Graphics, Width, Height, Flash, 0x3A0806);
        }
        float Bury = Scp4666ClientState.bury(Partial);
        if (Bury > 0.01f) {
            snow(Graphics, Width, Height, Bury);
        }
        float Sack = Scp4666ClientState.sack(Partial);
        if (Sack > 0.01f) {
            int Alpha = Mth.clamp((int) (Sack * 255.0f), 0, 255);
            Graphics.setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, Sack * 1.4f));
            Graphics.blit(Sackcloth, 0, 0, 0, 0.0f, 0.0f, Width, Height, 64, 64);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            Graphics.fill(0, 0, Width, Height, Alpha << 24);
        }
    }

    private static void snow(GuiGraphics Graphics, int Width, int Height, float Share) {
        int Rise = (int) (Height * Share * BuryReach);
        int Rows = Math.max(1, Rise / BuryStep);
        for (int Row = 0; Row < Rows; Row++) {
            int Bottom = Height - Row * BuryStep;
            float Depth = 1.0f - (float) Row / Rows;
            int Alpha = Mth.clamp((int) ((0.55f + 0.45f * Depth) * 235.0f * Math.min(1.0f, Share * 2.0f)), 0, 255);
            int Wave = (Row * 7 % 5) * 2;
            Graphics.fill(-Wave, Bottom - BuryStep, Width + Wave, Bottom, Alpha << 24 | (Row == Rows - 1 ? 0xEEF2F8 : 0xD6DDE7));
        }
        if (Share > 0.8f) {
            int Alpha = Mth.clamp((int) ((Share - 0.8f) / 0.2f * 255.0f), 0, 255);
            Graphics.fill(0, 0, Width, Height, Alpha << 24 | 0x0A0C10);
        }
    }

    private static void vignette(GuiGraphics Graphics, int Width, int Height, float Strength, int Colour) {
        for (int Step = 0; Step < Steps; Step++) {
            float Share = 1.0f - (float) Step / Steps;
            int Alpha = Mth.clamp((int) (Strength * Share * Share * 230.0f), 0, 255);
            if (Alpha <= 0) {
                continue;
            }
            int Inset = Step * Band;
            int Fill = Alpha << 24 | Colour;
            Graphics.fill(Inset, Inset, Width - Inset, Inset + Band, Fill);
            Graphics.fill(Inset, Height - Inset - Band, Width - Inset, Height - Inset, Fill);
            Graphics.fill(Inset, Inset + Band, Inset + Band, Height - Inset - Band, Fill);
            Graphics.fill(Width - Inset - Band, Inset + Band, Width - Inset, Height - Inset - Band, Fill);
        }
    }
}
