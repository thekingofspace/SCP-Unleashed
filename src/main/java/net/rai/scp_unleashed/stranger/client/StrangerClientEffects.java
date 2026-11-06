package net.rai.scp_unleashed.stranger.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;

public final class StrangerClientEffects {
    public static final ResourceLocation Vignette = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/stranger/vanish_vignette.png");
    public static final int VignetteWidth = 64;
    public static final int VignetteHeight = 36;
    public static final int Duration = 44;
    public static final int PeakTick = 4;
    public static final float FlickerAlpha = 0.6f;
    private static int Age = -1;
    private static int Tint = 0x2A2140;

    private StrangerClientEffects() {
    }

    public static void reset() {
        Age = -1;
    }

    public static void vanish(int Colour) {
        Age = 0;
        Tint = Colour;
    }

    public static void tick(Minecraft Client) {
        if (Age < 0) {
            return;
        }
        Age++;
        if (Age > Duration || Client.player == null) {
            Age = -1;
        }
    }

    public static boolean isFlickerFrame(int Tick) {
        return Tick == 1 || Tick == 2 || Tick == 6;
    }

    public static float vignetteStrength(float Time) {
        float Strength = Time < PeakTick ? Time / PeakTick : 1.0f - (Time - PeakTick) / (Duration - PeakTick);
        return Math.round(Mth.clamp(Strength, 0.0f, 1.0f) * 6.0f) / 6.0f;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        if (Age < 0) {
            return;
        }
        float Time = Age + Delta.getGameTimeDeltaPartialTick(false);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        float Red = (Tint >> 16 & 255) / 255.0f;
        float Green = (Tint >> 8 & 255) / 255.0f;
        float Blue = (Tint & 255) / 255.0f;
        float Strength = vignetteStrength(Time);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (Strength > 0.0f) {
            Graphics.setColor(Red, Green, Blue, Strength);
            Graphics.blit(Vignette, 0, 0, Width, Height, 0.0f, 0.0f, VignetteWidth, VignetteHeight, VignetteWidth, VignetteHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (isFlickerFrame(Age)) {
            int Alpha = Math.round(FlickerAlpha * 255.0f);
            Graphics.fill(0, 0, Width, Height, Alpha << 24 | (Tint & 0xFFFFFF));
        }
        RenderSystem.disableBlend();
    }
}
