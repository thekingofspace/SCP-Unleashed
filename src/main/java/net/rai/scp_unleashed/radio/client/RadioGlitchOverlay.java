package net.rai.scp_unleashed.radio.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Random;

public final class RadioGlitchOverlay {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "radio_glitch");
    public static final ResourceLocation Screen = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/scp079/screen.png");
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 128;
    public static final int Cell = 16;
    public static final int FaceWidth = 12;
    public static final int FaceHeight = 9;
    public static final int GlitchRow = 5;
    public static final int StaticRow = 7;
    public static final int Frames = 4;
    public static final int Bands = 5;
    public static final int Block = 4;
    public static final float PeakFace = 0.42f;
    public static final float PeakStatic = 0.3f;
    public static final int[] Greys = {0x1A1C1A, 0x525452, 0x5E605E, 0x646664};
    private static int Left;
    private static int Total;
    private static int Seed;

    private RadioGlitchOverlay() {
    }

    public static void start(int Ticks, int NewSeed) {
        Left = Math.max(Left, Ticks);
        Total = Math.max(1, Left);
        Seed = NewSeed;
    }

    public static void tick() {
        if (Left > 0) {
            Left--;
        }
    }

    public static boolean isActive() {
        return Left > 0;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Left <= 0 || Client.player == null || Client.options.hideGui) {
            return;
        }
        float Fade = Mth.clamp((Left - Delta.getGameTimeDeltaPartialTick(false)) / Math.min(10.0f, Total), 0.0f, 1.0f);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Step = (Total - Left) / 2;
        Random Roll = new Random(Seed * 31L + Step);
        RenderSystem.enableBlend();
        for (int Band = 0; Band < Bands; Band++) {
            int Rows = 1 + Roll.nextInt(3);
            int Y = Roll.nextInt(Math.max(1, Height / Block)) * Block;
            int Shift = (Roll.nextInt(9) - 4) * Block * 2;
            int Grey = Greys[Roll.nextInt(Greys.length)];
            int Alpha = Math.round(0.32f * Fade * 255.0f);
            Graphics.fill(Shift, Y, Width + Shift, Y + Rows * Block, Alpha << 24 | Grey);
            int Frame = Roll.nextInt(Frames);
            int Strip = Rows * Block;
            int Tiles = Width / (Strip * FaceWidth / FaceHeight) + 2;
            Graphics.setColor(1.0f, 1.0f, 1.0f, PeakStatic * Fade);
            for (int Tile = 0; Tile < Tiles; Tile++) {
                int TileWidth = Strip * FaceWidth / FaceHeight;
                Graphics.blit(Screen, Shift + Tile * TileWidth, Y, TileWidth, Strip, Frame * Cell, StaticRow * Cell, FaceWidth, FaceHeight, TextureWidth, TextureHeight);
            }
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        if ((Total - Left) % 3 != 1) {
            int Scale = Math.max(2, Height / 3 / FaceHeight);
            int FaceW = FaceWidth * Scale;
            int FaceH = FaceHeight * Scale;
            int Jitter = (Roll.nextInt(3) - 1) * Scale;
            int Frame = Math.floorMod(Seed + Step, Frames);
            Graphics.setColor(1.0f, 1.0f, 1.0f, PeakFace * Fade);
            Graphics.blit(Screen, (Width - FaceW) / 2 + Jitter, (Height - FaceH) / 2, FaceW, FaceH, Frame * Cell, GlitchRow * Cell, FaceWidth, FaceHeight, TextureWidth, TextureHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        RenderSystem.disableBlend();
    }
}
