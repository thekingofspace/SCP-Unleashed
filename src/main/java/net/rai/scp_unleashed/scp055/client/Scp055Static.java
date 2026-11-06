package net.rai.scp_unleashed.scp055.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp055Static {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp055_static");
    public static final ResourceLocation NoiseId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp055_static_noise");
    public static final int TexelTarget = 96;
    public static final int[] Greys = {0x0E1012, 0x24272B, 0x3D4146, 0x5C6066, 0x80858B, 0xA7ACB1, 0xD0D4D8, 0xF0F2F4};
    public static final int[] Weights = {2, 4, 6, 7, 6, 4, 2, 1};
    public static final float CellShare = 0.6f;
    public static final int BarRows = 6;
    public static final int BarSpeed = 3;
    public static final int BarBoost = 2;
    public static final int DropoutPercent = 4;
    public static final int DropoutDim = 3;
    public static final int SaltSeed = 0x5A17;
    private static DynamicTexture Noise;
    private static int NoiseWidth;
    private static int NoiseHeight;
    private static int LastTick = Integer.MIN_VALUE;
    private static float LastLevel = -1.0f;

    private Scp055Static() {
    }

    public static int texel(int Width, int Height) {
        return Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
    }

    public static float unit(long Value) {
        return (Value >>> 40) / (float) (1L << 24);
    }

    public static float threshold(long Seed, int X, int Y) {
        float Cell = unit(Scp055Distortion.mix(Seed ^ SaltSeed, (long) Y * 65536L + X));
        float Row = unit(Scp055Distortion.mix(Seed ^ SaltSeed, -1L - Y));
        return CellShare * Cell + (1.0f - CellShare) * Row;
    }

    public static int greyIndex(long Seed, int Tick, int X, int Y, int Rows) {
        long Roll = Scp055Distortion.mix(Seed + Tick * 0x632BE59BD9B4E019L, (long) Y * 65536L + X);
        int Total = 0;
        for (int Weight : Weights) {
            Total += Weight;
        }
        int Pick = (int) Math.floorMod(Roll, (long) Total);
        int Index = 0;
        while (Pick >= Weights[Index]) {
            Pick -= Weights[Index];
            Index++;
        }
        int BarTop = Math.floorMod(Tick * BarSpeed, Rows + BarRows) - BarRows;
        if (Y >= BarTop && Y < BarTop + BarRows) {
            Index = Math.min(Greys.length - 1, Index + BarBoost);
        }
        long Line = Scp055Distortion.mix(Seed ^ 0x77L, (long) Tick * 4096L + Y);
        if (Math.floorMod(Line, 100L) < DropoutPercent) {
            Index = Math.max(0, Index - DropoutDim);
        }
        return Index;
    }

    private static void refresh(int Columns, int Rows, int Tick, float Level, long Seed) {
        if (Noise == null || NoiseWidth != Columns || NoiseHeight != Rows) {
            Noise = new DynamicTexture(Columns, Rows, false);
            NoiseWidth = Columns;
            NoiseHeight = Rows;
            Minecraft.getInstance().getTextureManager().register(NoiseId, Noise);
            LastTick = Integer.MIN_VALUE;
        }
        if (Tick == LastTick && Level == LastLevel) {
            return;
        }
        LastTick = Tick;
        LastLevel = Level;
        NativeImage Pixels = Noise.getPixels();
        if (Pixels == null) {
            return;
        }
        for (int Y = 0; Y < Rows; Y++) {
            for (int X = 0; X < Columns; X++) {
                if (threshold(Seed, X, Y) >= Level) {
                    Pixels.setPixelRGBA(X, Y, 0);
                    continue;
                }
                int Colour = Greys[greyIndex(Seed, Tick, X, Y, Rows)];
                int Red = Colour >> 16 & 255;
                int Green = Colour >> 8 & 255;
                int Blue = Colour & 255;
                Pixels.setPixelRGBA(X, Y, 0xFF000000 | Blue << 16 | Green << 8 | Red);
            }
        }
        Noise.upload();
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        float Level = Scp055ClientEpisode.staticLevel(Delta.getGameTimeDeltaPartialTick(false));
        if (Level <= 0.0f) {
            return;
        }
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        int Cell = texel(Width, Height);
        int Columns = (Width + Cell - 1) / Cell;
        int Rows = (Height + Cell - 1) / Cell;
        refresh(Columns, Rows, Scp055ClientEpisode.age(), Level, Scp055ClientEpisode.seed());
        float Scale = (float) Display.getGuiScale();
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(NoiseId, 0, 0, Columns * Cell, Rows * Cell, 0.0f, 0.0f, Columns, Rows, Columns, Rows);
        RenderSystem.disableBlend();
        Pose.popPose();
    }

    public static void release() {
        if (Noise != null) {
            Minecraft.getInstance().getTextureManager().release(NoiseId);
            Noise = null;
        }
        LastTick = Integer.MIN_VALUE;
        LastLevel = -1.0f;
    }
}
