package net.rai.scp_unleashed.z0002.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.z0002.Z0002Registry;

public final class Z0002Warning {
    public static final ResourceLocation LayerId = Z0002Registry.location("z0002_warning");
    public static final int Bands = 6;
    public static final int BandWidth = 5;
    public static final float Peak = 0.42f;
    public static final int Colour = 0x7A0006;
    public static final int FadeTicks = 12;
    private static int Left;
    private static int Total;
    private static int Age;

    private Z0002Warning() {
    }

    public static void begin(int Ticks) {
        Left = Math.max(Left, Ticks);
        Total = Math.max(Total, Ticks);
        Age = 0;
    }

    public static void tick() {
        if (Left > 0) {
            Left--;
            Age++;
        } else {
            Total = 0;
        }
    }

    public static void reset() {
        Left = 0;
        Total = 0;
        Age = 0;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        if (Left <= 0 || Minecraft.getInstance().options.hideGui) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Fade = Mth.clamp((Age + Partial) / FadeTicks, 0.0f, 1.0f) * Mth.clamp((Left - Partial) / FadeTicks, 0.0f, 1.0f);
        float Beat = 0.65f + 0.35f * Mth.sin((Age + Partial) * 0.55f);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        for (int Band = 0; Band < Bands; Band++) {
            float Strength = Peak * Fade * Beat * (1.0f - Band / (float) Bands);
            int Alpha = Mth.clamp((int) (Strength * 255.0f), 0, 255);
            if (Alpha <= 0) {
                continue;
            }
            int Argb = Alpha << 24 | Colour;
            int Inner = Band * BandWidth;
            int Outer = Inner + BandWidth;
            Graphics.fill(Inner, Inner, Width - Inner, Outer, Argb);
            Graphics.fill(Inner, Height - Outer, Width - Inner, Height - Inner, Argb);
            Graphics.fill(Inner, Outer, Outer, Height - Outer, Argb);
            Graphics.fill(Width - Outer, Outer, Width - Inner, Height - Outer, Argb);
        }
    }
}
