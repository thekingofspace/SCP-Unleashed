package net.rai.scp_unleashed.scp179.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SpaceScene {
    public static final ResourceLocation NoiseTexture = SkyScene.location("noise.png");
    public static final int NoiseSize = 64;
    public static final int NoiseFrames = 4;
    public static final int NoiseFrameTicks = 2;
    public static final int NoiseSeed = 1799;

    private SpaceScene() {
    }

    public static void noise(GuiGraphics Graphics, int X, int Y, int Width, int Height, float Ticks) {
        int Step = (int) Math.floor(Ticks / NoiseFrameTicks);
        int Frame = Math.floorMod(Step, NoiseFrames);
        int Ox = SkyScene.pick(NoiseSeed, Step, 5, NoiseSize);
        int Oy = SkyScene.pick(NoiseSeed, Step, 6, NoiseSize);
        for (int Ty = -Oy; Ty < Height; Ty += NoiseSize) {
            for (int Tx = -Ox; Tx < Width; Tx += NoiseSize) {
                int Left = Math.max(0, Tx);
                int Top = Math.max(0, Ty);
                int Right = Math.min(Width, Tx + NoiseSize);
                int Bottom = Math.min(Height, Ty + NoiseSize);
                if (Right > Left && Bottom > Top) {
                    Graphics.blit(NoiseTexture, X + Left, Y + Top, Frame * NoiseSize + (Left - Tx), Top - Ty, Right - Left, Bottom - Top, NoiseSize * NoiseFrames, NoiseSize);
                }
            }
        }
    }

    public static void empty(GuiGraphics Graphics, int X, int Y, int Width, int Height, float Ticks) {
        SkyScene.begin(Graphics, X, Y, Width, Height, Scp179View.Fov);
        Vector3f Eye = Scp179View.eye(Ticks);
        Matrix4f View = SkyScene.view(Eye, Scp179View.target());
        SkyScene.sky(View);
        SkyScene.stars(View, Eye, Scp179View.FigurePosition, Ticks);
        SkyScene.end();
    }
}
