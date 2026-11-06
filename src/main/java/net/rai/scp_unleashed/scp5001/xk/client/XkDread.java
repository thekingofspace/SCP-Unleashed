package net.rai.scp_unleashed.scp5001.xk.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkRegistry;
import net.rai.scp_unleashed.worldend.client.WorldEndOverlay;

public final class XkDread {
    public static final ResourceLocation Layer = Scp5001XkRegistry.location("scp5001_xk_dread");
    public static final double Range = 72.0;
    public static final int Band = 6;
    public static final int Eyes = 14;
    public static final int EyeSize = 16;
    private static float Intensity;
    private static float IntensityO;

    private XkDread() {
    }

    public static void tick(Minecraft Client) {
        IntensityO = Intensity;
        Player Viewer = Client.player;
        float Wanted = 0.0f;
        if (Viewer != null && YaldabaothClient.present() && Client.level != null && Client.level.dimension() == Level.OVERWORLD) {
            double Distance = YaldabaothClient.nearest(Viewer.getEyePosition(), 0.0f);
            Wanted = (float) Mth.clamp(1.0 - Distance / Range, 0.0, 1.0);
        }
        Intensity += Mth.clamp(Wanted - Intensity, -0.04f, 0.04f);
    }

    public static void reset() {
        Intensity = 0.0f;
        IntensityO = 0.0f;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Amount = Mth.lerp(Partial, IntensityO, Intensity);
        if (Amount <= 0.01f) {
            return;
        }
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Steps = 6;
        for (int Step = 0; Step < Steps; Step++) {
            int Inset = Step * Band;
            int Alpha = (int) (Amount * 150.0f * (1.0f - Step / (float) Steps));
            if (Alpha <= 0) {
                continue;
            }
            int Colour = (Alpha << 24) | 0x0A0204;
            Graphics.fill(Inset, Inset, Width - Inset, Inset + Band, Colour);
            Graphics.fill(Inset, Height - Inset - Band, Width - Inset, Height - Inset, Colour);
            Graphics.fill(Inset, Inset + Band, Inset + Band, Height - Inset - Band, Colour);
            Graphics.fill(Width - Inset - Band, Inset + Band, Width - Inset, Height - Inset - Band, Colour);
        }
        long Clock = YaldabaothClient.clock();
        float Time = Clock + Partial;
        RenderSystem.enableBlend();
        for (int Index = 0; Index < Eyes; Index++) {
            float Seed = WorldEndOverlay.hash(Index, 3, 91);
            float Flicker = (float) Math.sin(Time * (0.09f + Seed * 0.08f) + Index * 2.3f);
            float Shown = Amount * Mth.clamp(Flicker * 1.6f - 0.4f, 0.0f, 1.0f);
            if (Shown <= 0.05f) {
                continue;
            }
            int Edge = Index % 4;
            float Along = WorldEndOverlay.hash(Index, 5, 93);
            int Depth = 4 + (int) (WorldEndOverlay.hash(Index, 7, 95) * 18.0f);
            int X = switch (Edge) {
                case 0, 1 -> (int) (Along * (Width - EyeSize));
                case 2 -> Depth;
                default -> Width - EyeSize - Depth;
            };
            int Y = switch (Edge) {
                case 0 -> Depth;
                case 1 -> Height - EyeSize - Depth;
                default -> (int) (Along * (Height - EyeSize));
            };
            int Frame = Flicker > 0.85f ? 0 : Flicker > 0.6f ? 1 : 3;
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Shown);
            Graphics.blit(WorldEndOverlay.Eye, X, Y, EyeSize, EyeSize, Frame * 8.0f, 0.0f, 8, 8, 32, 8);
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
