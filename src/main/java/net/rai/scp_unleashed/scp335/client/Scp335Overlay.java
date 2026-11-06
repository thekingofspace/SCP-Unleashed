package net.rai.scp_unleashed.scp335.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp335.Scp335Registry;

public final class Scp335Overlay {
    public static final ResourceLocation Texture = Scp335Registry.location("textures/gui/scp335/static.png");
    public static final int Tile = 64;
    public static final int Frames = 8;
    public static final int Scale = 2;
    public static final int MaxTears = 6;

    private Scp335Overlay() {
    }

    public static int mix(int Value) {
        int Mixed = Value * 0x9E3779B1;
        Mixed ^= Mixed >>> 15;
        Mixed *= 0x85EBCA77;
        Mixed ^= Mixed >>> 13;
        return Mixed & 0x7FFFFFFF;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        float Strength = Scp335ClientState.overlay(Delta.getGameTimeDeltaPartialTick(false));
        if (Strength <= 0.01f) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        int Step = Game.level == null ? 0 : (int) (Game.level.getGameTime() & 0xFFFFFF);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Size = Tile * Scale;
        int Frame = mix(Step) % Frames;
        int OffsetX = (mix(Step * 3 + 1) % Tile) * Scale;
        int OffsetY = (mix(Step * 5 + 2) % Tile) * Scale;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, Strength));
        for (int Y = -OffsetY; Y < Height; Y += Size) {
            for (int X = -OffsetX; X < Width; X += Size) {
                Graphics.blit(Texture, X, Y, Size, Size, 0.0f, Frame * Tile, Tile, Tile, Tile, Tile * Frames);
            }
        }
        int Tears = Math.round(Strength * MaxTears);
        for (int Index = 0; Index < Tears; Index++) {
            int Seed = mix(Step * 17 + Index * 101);
            if (Seed % 3 == 0) {
                continue;
            }
            int Band = (2 + Seed % 4) * Scale;
            int Top = (Seed >>> 4) % Math.max(1, Height - Band);
            int Shift = ((Seed >>> 12) % 9 - 4) * Scale * 4;
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, Strength * 1.6f));
            for (int X = -Size + Shift % Size; X < Width; X += Size) {
                Graphics.blit(Texture, X, Top, Size, Band, 0.0f, ((Frame + 3) % Frames) * Tile + (Seed >>> 8) % (Tile - Band / Scale), Tile, Band / Scale, Tile,
                        Tile * Frames);
            }
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
