package net.rai.scp_unleashed.scp294.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp294.Scp294Registry;

public final class Scp294Radiance {
    public static final ResourceLocation LayerId = Scp294Registry.location("scp294_radiance");
    public static final int Cell = 6;
    public static final int HoldTicks = 60;
    public static final int FadeTicks = 20;
    public static final float FullWhite = 0.96f;
    public static final float Floor = 0.06f;
    public static final float CentreHeight = 0.36f;
    public static final int Rays = 8;
    public static final int[] Bayer = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
    private static long Clock;
    private static long StartTick = -1;
    private static int Length = 1;
    private static long FadeFrom = -1;

    private Scp294Radiance() {
    }

    public static void start(int Ticks) {
        StartTick = Clock;
        Length = Math.max(1, Ticks);
        FadeFrom = -1;
    }

    public static void reset() {
        StartTick = -1;
        FadeFrom = -1;
    }

    public static boolean active() {
        return StartTick >= 0;
    }

    public static void tick(Minecraft Game) {
        Clock++;
        if (StartTick < 0) {
            return;
        }
        if (Game.player == null) {
            reset();
            return;
        }
        boolean Gone = Game.player.isDeadOrDying() || Game.player.isRemoved();
        if (FadeFrom < 0 && (Gone || Clock - StartTick > Length + HoldTicks)) {
            FadeFrom = Clock;
        }
        if (FadeFrom >= 0 && Clock - FadeFrom > FadeTicks) {
            reset();
        }
    }

    public static float progress(float PartialTick) {
        if (StartTick < 0) {
            return 0.0f;
        }
        return Math.min(1.0f, (Clock - StartTick + PartialTick) / Length);
    }

    public static float fade(float PartialTick) {
        if (FadeFrom < 0) {
            return 1.0f;
        }
        return Math.max(0.0f, 1.0f - (Clock - FadeFrom + PartialTick) / FadeTicks);
    }

    public static float smooth(float Value) {
        float Clamped = Math.max(0.0f, Math.min(1.0f, Value));
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static float intensity(int Column, int Row, int Columns, int Rows0, float Progress) {
        float CentreX = Columns * 0.5f;
        float CentreY = Rows0 * CentreHeight;
        float Dx = Column + 0.5f - CentreX;
        float Dy = Row + 0.5f - CentreY;
        float Far = Math.max(CentreY, Rows0 - CentreY);
        float Reach = (float) Math.sqrt(CentreX * CentreX + Far * Far);
        float Distance = (float) Math.sqrt(Dx * Dx + Dy * Dy) / Math.max(1.0f, Reach);
        float Radius = 0.08f + (float) Math.pow(smooth(Progress), 1.5) * 1.05f;
        float Core = Math.max(0.0f, Math.min(1.0f, (Radius - Distance) / 0.2f + 0.1f)) * (0.5f + 0.5f * Progress);
        float Angle = (float) Math.atan2(Dy, Dx) + Progress * 0.9f;
        float Spoke = (float) Math.pow(Math.max(0.0, Math.cos(Angle * Rays * 0.5f)), 5.0);
        float Ray = Spoke * Math.max(0.0f, Math.min(1.0f, (Radius * 2.0f + 0.1f - Distance) / 0.5f)) * Math.min(1.0f, Progress * 3.0f) * (0.45f + 0.4f * Progress);
        float Wash = Progress * Progress * Progress * Progress * 0.85f;
        float Value = Math.max(0.0f, Math.min(1.0f, Math.max(Core, Ray) + Wash));
        return Value < Floor ? 0.0f : Value;
    }

    public static int alphaLevel(int Column, int Row, float Value) {
        float Threshold = (Bayer[(Row & 3) * 4 + (Column & 3)] + 0.5f) / 16.0f;
        float Scaled = Value * 4.0f;
        int Base = (int) Math.floor(Scaled);
        return Math.min(4, Base + (Scaled - Base > Threshold ? 1 : 0));
    }

    public static int colourOf(int Level, float Fade) {
        int Alpha = Math.round(Level * 0.25f * 255.0f * Fade);
        int Red = 255;
        int Green = Level >= 3 ? 255 : 250;
        int Blue = Level >= 3 ? 252 : 236;
        return (Alpha << 24) | (Red << 16) | (Green << 8) | Blue;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        if (StartTick < 0) {
            return;
        }
        float PartialTick = Delta.getGameTimeDeltaPartialTick(true);
        float Progress = progress(PartialTick);
        float Fade = fade(PartialTick);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        if (Progress >= FullWhite) {
            Graphics.fill(0, 0, Width, Height, colourOf(4, Fade));
            return;
        }
        int Columns = (Width + Cell - 1) / Cell;
        int Rows0 = (Height + Cell - 1) / Cell;
        for (int Row = 0; Row < Rows0; Row++) {
            for (int Column = 0; Column < Columns; Column++) {
                int Level = alphaLevel(Column, Row, intensity(Column, Row, Columns, Rows0, Progress));
                if (Level > 0) {
                    Graphics.fill(Column * Cell, Row * Cell, Column * Cell + Cell, Row * Cell + Cell, colourOf(Level, Fade));
                }
            }
        }
    }
}
