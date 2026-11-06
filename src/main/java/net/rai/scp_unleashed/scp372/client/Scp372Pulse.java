package net.rai.scp_unleashed.scp372.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp372.Scp372Registry;

public final class Scp372Pulse {
    public static final ResourceLocation LayerId = Scp372Registry.location("scp372_pulse");
    public static final int Cell = 8;
    public static final int Length = 16;
    public static final float RingStart = 1.15f;
    public static final float RingEnd = 0.3f;
    public static final float Band = 0.16f;
    public static final float EdgeWash = 0.35f;
    public static final float Floor = 0.08f;
    public static final int[] Bayer = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
    public static final int Red = 120;
    public static final int Green = 232;
    public static final int Blue = 236;
    private static long Clock;
    private static long StartTick = -1;
    private static float Power = 1.0f;

    private Scp372Pulse() {
    }

    public static void start(float Strength) {
        StartTick = Clock;
        Power = Math.max(0.0f, Math.min(1.0f, Strength));
    }

    public static void reset() {
        StartTick = -1;
    }

    public static boolean active() {
        return StartTick >= 0;
    }

    public static void tick(Minecraft Game) {
        Clock++;
        if (StartTick >= 0 && (Game.player == null || Clock - StartTick > Length)) {
            reset();
        }
    }

    public static float progress(float PartialTick) {
        return StartTick < 0 ? 1.0f : Math.min(1.0f, (Clock - StartTick + PartialTick) / Length);
    }

    public static float smooth(float Value) {
        float Clamped = Math.max(0.0f, Math.min(1.0f, Value));
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static float envelope(float Progress) {
        return (float) Math.sin(Math.PI * Math.max(0.0f, Math.min(1.0f, Progress))) * (Progress < 0.15f ? smooth(Progress / 0.15f) : 1.0f);
    }

    public static float intensity(float Distance, float Progress) {
        float Radius = RingStart + (RingEnd - RingStart) * smooth(Progress);
        float Ring = Math.max(0.0f, 1.0f - Math.abs(Distance - Radius) / Band);
        float Wash = EdgeWash * smooth((Distance - 0.55f) / 0.5f);
        float Value = Math.min(1.0f, Math.max(Ring, Wash) * envelope(Progress) * Power);
        return Value < Floor ? 0.0f : Value;
    }

    public static int alphaLevel(int Column, int Row, float Value) {
        float Threshold = (Bayer[(Row & 3) * 4 + (Column & 3)] + 0.5f) / 16.0f;
        float Scaled = Value * 4.0f;
        int Base = (int) Math.floor(Scaled);
        return Math.min(4, Base + (Scaled - Base > Threshold ? 1 : 0));
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        if (StartTick < 0) {
            return;
        }
        float Progress = progress(Delta.getGameTimeDeltaPartialTick(true));
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Columns = (Width + Cell - 1) / Cell;
        int Rows = (Height + Cell - 1) / Cell;
        float CentreX = Columns * 0.5f;
        float CentreY = Rows * 0.5f;
        float Reach = (float) Math.sqrt(CentreX * CentreX + CentreY * CentreY);
        for (int Row = 0; Row < Rows; Row++) {
            for (int Column = 0; Column < Columns; Column++) {
                float Dx = Column + 0.5f - CentreX;
                float Dy = Row + 0.5f - CentreY;
                float Distance = (float) Math.sqrt(Dx * Dx + Dy * Dy) / Reach;
                int Level = alphaLevel(Column, Row, intensity(Distance, Progress));
                if (Level <= 0) {
                    continue;
                }
                int Alpha = Math.round(Level * 0.16f * 255.0f);
                Graphics.fill(Column * Cell, Row * Cell, Column * Cell + Cell, Row * Cell + Cell, (Alpha << 24) | (Red << 16) | (Green << 8) | Blue);
            }
        }
    }
}
