package net.rai.scp_unleashed.scp093.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp093.Scp093Registry;

public final class Scp093Overlay {
    public static final ResourceLocation LayerId = Scp093Registry.location("scp093_overlay");
    public static final ResourceLocation Haze = Scp093Registry.location("textures/gui/scp093/haze.png");
    public static final ResourceLocation Figures = Scp093Registry.location("textures/gui/scp093/figures.png");
    public static final ResourceLocation Face = Scp093Registry.location("textures/gui/scp093/face.png");
    public static final int HazeWidth = 64;
    public static final int HazeHeight = 36;
    public static final int FigureWidth = 32;
    public static final int FigureHeight = 48;
    public static final int FiguresWidth = 96;
    public static final int FaceWidth = 32;
    public static final int FaceHeight = 40;
    public static final int Amber = 0xE59A3C;

    private Scp093Overlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        float Strength = Scp093ClientState.intensity(Partial);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (Strength > 0.01f) {
            float Pulse = 0.85f + 0.15f * Mth.sin((Scp093ClientState.ticks() + Partial) * 0.11f);
            Graphics.setColor(1.0f, 1.0f, 1.0f, Mth.clamp((0.3f + 0.6f * Strength) * Pulse, 0.0f, 1.0f));
            Graphics.blit(Haze, 0, 0, Width, Height, 0.0f, 0.0f, HazeWidth, HazeHeight, HazeWidth, HazeHeight);
            for (Scp093ClientState.Flicker Shown : Scp093ClientState.flickers()) {
                float Age = (Scp093ClientState.ticks() - Shown.Born() + Partial) / Math.max(1, Shown.Life());
                float Alpha = Mth.clamp(Shown.Alpha() * (1.0f - Age * 0.7f), 0.0f, 1.0f);
                Graphics.setColor(1.0f, 0.86f, 0.62f, Alpha);
                if (Shown.Kind() >= Scp093ClientState.Torsos) {
                    int Tall = Math.round(Height * Shown.Scale());
                    int Wide = Tall * FaceWidth / FaceHeight;
                    int Left = Math.round(Shown.X() * Width) - Wide / 2;
                    int Top = Math.round(Shown.Y() * Height) - Tall / 2;
                    Graphics.blit(Face, Left, Top, Wide, Tall, 0.0f, 0.0f, FaceWidth, FaceHeight, FaceWidth, FaceHeight);
                } else {
                    int Tall = Math.round(Height * Shown.Scale());
                    int Wide = Tall * FigureWidth / FigureHeight;
                    int Left = Math.round(Shown.X() * Width) - Wide / 2;
                    int Top = Math.round(Shown.Y() * Height) - Tall / 2;
                    Graphics.blit(Figures, Left, Top, Wide, Tall, Shown.Kind() * FigureWidth, 0.0f, FigureWidth, FigureHeight, FiguresWidth, FigureHeight);
                }
            }
            float Claim = Scp093ClientState.claimShare(Partial);
            if (Claim > 0.0f) {
                Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                Graphics.fill(0, 0, Width, Height, (Math.round(Claim * Claim * 210.0f) << 24) | 0x1A0E06);
                float Flicker = 0.8f + 0.2f * Mth.sin((Scp093ClientState.ticks() + Partial) * 1.7f);
                Graphics.setColor(0.72f, 0.52f, 0.36f, (0.2f + 0.45f * Claim) * Flicker);
                int Tall = Math.round(Height * (0.6f + 0.5f * Claim));
                int Wide = Tall * FaceWidth / FaceHeight;
                Graphics.blit(Face, (Width - Wide) / 2, (Height - Tall) / 2, Wide, Tall, 0.0f, 0.0f, FaceWidth, FaceHeight, FaceWidth, FaceHeight);
            }
        }
        float Flash = Scp093ClientState.flash(Partial);
        if (Flash > 0.002f) {
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            Graphics.fill(0, 0, Width, Height, (Math.round(Flash * 235.0f) << 24) | Amber);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
