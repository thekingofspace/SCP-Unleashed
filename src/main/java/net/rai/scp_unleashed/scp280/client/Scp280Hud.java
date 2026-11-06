package net.rai.scp_unleashed.scp280.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp280.Scp280Registry;
import net.rai.scp_unleashed.scp280.network.Scp280MeterPayload;

import java.util.Arrays;

public final class Scp280Hud {
    public static final ResourceLocation IconLayer = Scp280Registry.location("scp280_eye");
    public static final ResourceLocation VignetteLayer = Scp280Registry.location("scp280_vignette");
    public static final ResourceLocation Sheet = Scp280Registry.location("textures/gui/scp280/eye.png");
    public static final int Stages = 7;
    public static final int Looks = 3;
    public static final int IconWidth = 16;
    public static final int IconHeight = 10;
    public static final int SheetWidth = IconWidth * Stages;
    public static final int SheetHeight = IconHeight * Looks;
    public static final int Margin = 4;
    public static final float IconAlpha = 0.85f;
    public static final float FadeStep = 0.05f;
    public static final float FollowRate = 0.04f;
    public static final int BlinkTicks = 5;
    public static final int BlinkMin = 90;
    public static final int BlinkMax = 220;
    public static final int GlanceMin = 50;
    public static final int GlanceMax = 140;
    public static final float VignetteStart = 0.6f;
    public static final float VignetteMax = 0.7f;
    public static final int PulseTicks = 50;
    public static final int TexelTarget = 110;
    public static final int VignetteColour = 0x0A0612;
    public static final float[] RingAlphas = {0.42f, 0.3f, 0.2f, 0.12f, 0.06f, 0.03f};
    public static final int RingWidth = 2;
    public static final float CornerCut = 0.6f;
    public static final int IntensitySteps = 10;

    private static float Target;
    private static float Shown;
    private static float Fade;
    private static float FadeO;
    private static int Pulse;
    private static int Clock;
    private static int NextBlink = 120;
    private static int BlinkStart = -100;
    private static int NextGlance = 80;
    private static int Look = 1;
    private static long CachedKey = Long.MIN_VALUE;
    private static int[] Rects = new int[0];
    private static int RectCount;

    private Scp280Hud() {
    }

    public static void accept(Scp280MeterPayload Payload) {
        Target = Mth.clamp(Payload.Meter(), 0.0f, 1.0f);
        if (Payload.Cue() == Scp280MeterPayload.Reveal) {
            Pulse = PulseTicks;
            Shown = Math.max(Shown, 1.0f);
        } else if (Payload.Cue() == Scp280MeterPayload.Cleared) {
            Pulse = 0;
        }
    }

    public static float meter() {
        return Target;
    }

    public static float shown() {
        return Shown;
    }

    public static void set(float Meter, float Display, float Visible) {
        Target = Meter;
        Shown = Display;
        Fade = Visible;
        FadeO = Visible;
    }

    public static void tick(Minecraft Client) {
        if (Client.isPaused()) {
            return;
        }
        Clock++;
        FadeO = Fade;
        boolean Active = Target > 0.0f && Client.player != null && Client.level != null && Client.level.dimension().equals(Level.OVERWORLD);
        Fade = Active ? Math.min(1.0f, Fade + FadeStep) : Math.max(0.0f, Fade - FadeStep);
        Shown += (Target - Shown) * FollowRate;
        if (Math.abs(Target - Shown) < 0.002f) {
            Shown = Target;
        }
        if (Pulse > 0) {
            Pulse--;
        }
        if (Client.player == null) {
            return;
        }
        if (Clock >= NextBlink) {
            BlinkStart = Clock;
            NextBlink = Clock + BlinkMin + Client.player.getRandom().nextInt(BlinkMax - BlinkMin + 1);
        }
        if (Clock >= NextGlance) {
            int Roll = Client.player.getRandom().nextInt(4);
            Look = Roll == 0 ? 0 : Roll == 1 ? 2 : 1;
            NextGlance = Clock + GlanceMin + Client.player.getRandom().nextInt(GlanceMax - GlanceMin + 1);
        }
    }

    public static void reset() {
        Target = 0.0f;
        Shown = 0.0f;
        Fade = 0.0f;
        FadeO = 0.0f;
        Pulse = 0;
    }

    public static int stage(float Display) {
        if (Display <= 0.0f) {
            return 0;
        }
        if (Display < 0.08f) {
            return 0;
        }
        return Mth.clamp(1 + (int) ((Display - 0.08f) / 0.84f * (Stages - 1)), 1, Stages - 1);
    }

    private static boolean visible(Minecraft Client) {
        LocalPlayer User = Client.player;
        return User != null && !Client.options.hideGui && !User.isSpectator();
    }

    public static void renderIcon(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Alpha = Mth.lerp(Partial, FadeO, Fade) * IconAlpha;
        if (!visible(Client) || Alpha <= 0.02f) {
            return;
        }
        int Stage = stage(Shown);
        int Since = Clock - BlinkStart;
        if (Since >= 0 && Since < BlinkTicks) {
            Stage = Math.max(0, Stage - (Since < BlinkTicks / 2 + 1 ? 3 : 1));
        }
        int Row = Stage <= 1 ? 1 : Look;
        int X = Graphics.guiWidth() - IconWidth - Margin;
        int Y = Graphics.guiHeight() - IconHeight - Margin;
        drawIcon(Graphics, X, Y, Stage, Row, Alpha);
    }

    public static void drawIcon(GuiGraphics Graphics, int X, int Y, int Stage, int Row, float Alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Math.round(Alpha * 8.0f) / 8.0f);
        Graphics.blit(Sheet, X, Y, Stage * IconWidth, Row * IconHeight, IconWidth, IconHeight, SheetWidth, SheetHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    public static float vignette(float Partial) {
        float Base = Mth.clamp((Shown - VignetteStart) / (1.0f - VignetteStart), 0.0f, 1.0f) * VignetteMax;
        float Boost = Pulse > 0 ? (Pulse - Partial) / PulseTicks * 0.45f : 0.0f;
        return Mth.clamp((Base + Boost) * Mth.lerp(Partial, FadeO, Fade), 0.0f, 1.0f);
    }

    public static void renderVignette(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (!visible(Client)) {
            return;
        }
        float Strength = vignette(Delta.getGameTimeDeltaPartialTick(false));
        int Step = Math.round(Strength * IntensitySteps);
        if (Step <= 0) {
            return;
        }
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        int Texel = Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
        int Columns = (Width + Texel - 1) / Texel;
        int Rows = (Height + Texel - 1) / Texel;
        int OffsetX = (Width - Columns * Texel) / 2;
        int OffsetY = (Height - Rows * Texel) / 2;
        float Scale = (float) Display.getGuiScale();
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        long Key = ((long) Width * 31L + Height) * 64L + Step;
        if (Key != CachedKey) {
            build(Columns, Rows, OffsetX, OffsetY, Texel, Step / (float) IntensitySteps);
            CachedKey = Key;
        }
        for (int Index = 0; Index < RectCount; Index++) {
            int Base = Index * 5;
            Graphics.fill(Rects[Base], Rects[Base + 1], Rects[Base + 2], Rects[Base + 3], Rects[Base + 4]);
        }
        Pose.popPose();
    }

    private static void build(int Columns, int Rows, int OffsetX, int OffsetY, int Texel, float Strength) {
        RectCount = 0;
        int Reach = Math.max(1, Math.round(RingAlphas.length * (0.35f + 0.65f * Strength)));
        for (int Row = 0; Row < Rows; Row++) {
            int RunStart = 0;
            int RunColour = 0;
            for (int Column = 0; Column <= Columns; Column++) {
                int Colour = 0;
                if (Column < Columns) {
                    int Dx = Math.min(Column, Columns - 1 - Column);
                    int Dy = Math.min(Row, Rows - 1 - Row);
                    int Ring = (int) Math.min(Math.min(Dx, Dy), (Dx + Dy) * CornerCut) / RingWidth;
                    if (Ring < Reach) {
                        Colour = argb(RingAlphas[Ring] * Strength * 1.6f, VignetteColour);
                    }
                }
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
