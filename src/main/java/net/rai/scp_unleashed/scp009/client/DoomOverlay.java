package net.rai.scp_unleashed.scp009.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp009.Scp009Doom;

import java.util.Arrays;

public final class DoomOverlay {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp009_doom_overlay");
    public static final int TexelTarget = 90;
    public static final int Steps = 16;
    public static final float MinReach = 0.035f;
    public static final float MaxReach = 0.33f;
    public static final float CornerCut = 0.62f;
    public static final int ToothWidth = 3;
    public static final float ToothBase = 1.0f;
    public static final float ToothGrowth = 4.0f;
    public static final int[] RimColours = {0xC81C30, 0x9C0F22, 0x700818, 0x48040E};
    public static final float[] RimAlphas = {0.36f, 0.52f, 0.66f, 0.80f};
    public static final int GlintColour = 0xFF6E7E;
    public static final int GlintEvery = 7;
    public static final int WashColour = 0x9A0A1E;
    public static final float WashStep = 0.024f;
    public static final float BeatBoost = 0.08f;
    private static long CachedKey = Long.MIN_VALUE;
    private static int[] Rects = new int[0];
    private static int RectCount;

    private DoomOverlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.options.hideGui) {
            return;
        }
        MobEffectInstance Instance = Scp009Doom.instance(User);
        if (Instance == null) {
            return;
        }
        float Progress = Scp009Doom.progress(Instance);
        int Step = Math.min(Steps - 1, (int) (Progress * Steps));
        boolean Beat = DoomPulse.isBeating();
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        long Key = (((long) Width * 31L + Height) * 31L + Step) * 2L + (Beat ? 1L : 0L);
        if (Key != CachedKey) {
            build(Width, Height, Step, Beat);
            CachedKey = Key;
        }
        float Scale = (float) Display.getGuiScale();
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        for (int Index = 0; Index < RectCount; Index++) {
            int Base = Index * 5;
            Graphics.fill(Rects[Base], Rects[Base + 1], Rects[Base + 2], Rects[Base + 3], Rects[Base + 4]);
        }
        Pose.popPose();
    }

    public static int texel(int Width, int Height) {
        return Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
    }

    private static void build(int Width, int Height, int Step, boolean Beat) {
        RectCount = 0;
        int Texel = texel(Width, Height);
        int Columns = (Width + Texel - 1) / Texel;
        int Rows = (Height + Texel - 1) / Texel;
        int OffsetX = (Width - Columns * Texel) / 2;
        int OffsetY = (Height - Rows * Texel) / 2;
        float Progress = (Step + 0.5f) / Steps;
        float Reach = (MinReach + (MaxReach - MinReach) * Progress) * Math.min(Columns, Rows) + (Beat ? 1.0f : 0.0f);
        float Tooth = ToothBase + ToothGrowth * Progress;
        float Band = Math.max(1.0f, Reach / 4.0f);
        float AlphaScale = 0.62f + 0.38f * Progress + (Beat ? BeatBoost : 0.0f);
        float Wash = (Step / 2) * WashStep + (Beat ? WashStep : 0.0f);
        if (Wash > 0.0f) {
            add(0, 0, Width, Height, argb(Wash, WashColour));
        }
        for (int Row = 0; Row < Rows; Row++) {
            int RunStart = 0;
            int RunColour = 0;
            for (int Column = 0; Column <= Columns; Column++) {
                int Colour = Column < Columns ? cell(Column, Row, Columns, Rows, Reach, Tooth, Band, AlphaScale) : 0;
                if (Colour != RunColour) {
                    if (RunColour != 0) {
                        add(OffsetX + RunStart * Texel, OffsetY + Row * Texel, OffsetX + Column * Texel, OffsetY + (Row + 1) * Texel, RunColour);
                    }
                    RunStart = Column;
                    RunColour = Colour;
                }
            }
        }
    }

    private static int cell(int Column, int Row, int Columns, int Rows, float Reach, float Tooth, float Band, float AlphaScale) {
        int Dx = Math.min(Column, Columns - 1 - Column);
        int Dy = Math.min(Row, Rows - 1 - Row);
        float Edge = Math.min(Math.min(Dx, Dy), (Dx + Dy) * CornerCut);
        boolean Sideways = Dx < Dy;
        int Along = Sideways ? Row : Column;
        int Side = Sideways ? (Column < Columns / 2 ? 0 : 1) : (Row < Rows / 2 ? 2 : 3);
        float Inside = Reach + jag(Along, Side) * Tooth - Edge;
        if (Inside <= 0.0f) {
            return 0;
        }
        int Ring = Math.min(RimColours.length - 1, (int) (Inside / Band));
        int Colour = RimColours[Ring];
        if (Ring == 0 && Math.floorMod(hash(Column, Row), GlintEvery) == 0) {
            Colour = GlintColour;
        }
        return argb(Math.min(0.92f, RimAlphas[Ring] * AlphaScale), Colour);
    }

    private static float jag(int Along, int Side) {
        int Group = Math.floorDiv(Along, ToothWidth);
        int Offset = Math.floorMod(Along, ToothWidth);
        float Height = (Math.floorMod(hash(Group, Side * 7919), 5)) / 4.0f;
        float Middle = (ToothWidth - 1) / 2.0f;
        float Shape = 1.0f - Math.abs(Offset - Middle) / (Middle + 1.0f);
        return Height * Shape;
    }

    private static int hash(int A, int B) {
        int Value = A * 73856093 ^ B * 19349663;
        Value ^= Value >>> 13;
        Value *= 0x5BD1E995;
        Value ^= Value >>> 15;
        return Value;
    }

    private static int argb(float Alpha, int Colour) {
        int A = Math.max(1, Math.min(255, Math.round(Alpha * 255.0f)));
        return A << 24 | Colour & 0xFFFFFF;
    }

    private static void add(int X0, int Y0, int X1, int Y1, int Colour) {
        int Base = RectCount * 5;
        if (Base + 5 > Rects.length) {
            Rects = Arrays.copyOf(Rects, Math.max(64, Rects.length * 2));
        }
        Rects[Base] = X0;
        Rects[Base + 1] = Y0;
        Rects[Base + 2] = X1;
        Rects[Base + 3] = Y1;
        Rects[Base + 4] = Colour;
        RectCount++;
    }
}
