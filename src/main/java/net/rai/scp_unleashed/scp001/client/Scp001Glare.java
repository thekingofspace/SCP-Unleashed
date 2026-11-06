package net.rai.scp_unleashed.scp001.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class Scp001Glare {
    public static final int HoldTicks = 6;
    public static final int BurnColour = 0x9CC8F0;
    public static final int Block = 8;
    public static final int[] Rings = {7, 5, 3};
    private static float Strength;
    private static int Total;
    private static int Left;
    private static int LeftO;

    private Scp001Glare() {
    }

    public static void start(float Value, int Ticks) {
        if (Value * Math.max(1, Ticks) >= current(1.0f) * Math.max(1, Left)) {
            Strength = Mth.clamp(Value, 0.0f, 1.0f);
            Total = Math.max(1, Ticks);
            Left = Total;
            LeftO = Total;
        }
    }

    public static void tick() {
        LeftO = Left;
        if (Left > 0) {
            Left--;
        }
    }

    public static void reset() {
        Strength = 0.0f;
        Left = 0;
        LeftO = 0;
        Total = 0;
    }

    public static float current(float Partial) {
        if (Total <= 0) {
            return 0.0f;
        }
        float Remaining = Mth.lerp(Partial, LeftO, Left);
        float Elapsed = Total - Remaining;
        if (Elapsed <= HoldTicks) {
            return Strength;
        }
        float Fade = Mth.clamp(Remaining / Math.max(1.0f, Total - HoldTicks), 0.0f, 1.0f);
        return Strength * Fade * Fade * (3.0f - 2.0f * Fade);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        float Level = current(Delta.getGameTimeDeltaPartialTick(false));
        if (Level <= 0.004f || Minecraft.getInstance().player == null) {
            return;
        }
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int White = (int) (Math.min(0.97f, Level * 1.05f) * 255.0f);
        Graphics.fill(0, 0, Width, Height, White << 24 | 0xFFFBF2);
        float Burn = Level < 0.85f ? Mth.clamp(Level * 1.4f, 0.0f, 0.75f) : 0.0f;
        if (Burn > 0.02f) {
            int CentreX = Width / 2;
            int CentreY = Height / 2;
            for (int Index = 0; Index < Rings.length; Index++) {
                int Half = Rings[Index] * Block / 2;
                int Alpha = (int) (Burn * (0.35f + 0.25f * Index) * 255.0f);
                for (int Step = 0; Step < Rings[Index]; Step++) {
                    int Inset = stepInset(Step, Rings[Index]) * Block;
                    int Top = CentreY - Half + Step * Block;
                    Graphics.fill(CentreX - Half + Inset, Top, CentreX + Half - Inset, Top + Block, Alpha << 24 | BurnColour);
                }
            }
        }
        RenderSystem.disableBlend();
    }

    private static int stepInset(int Step, int Count) {
        int Edge = Math.min(Step, Count - 1 - Step);
        return Edge == 0 ? Math.max(1, Count / 4) : Edge == 1 && Count > 4 ? 1 : 0;
    }
}
