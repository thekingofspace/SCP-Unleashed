package net.rai.scp_unleashed.z0001.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0001.Z0001Registry;

import java.util.Arrays;
import java.util.List;

public final class Z0001Dread {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0001_dread");
    public static final int TexelTarget = 110;
    public static final int VignetteColour = 0x07050A;
    public static final float[] RingAlphas = {0.34f, 0.24f, 0.15f, 0.08f, 0.04f};
    public static final int RingWidth = 2;
    public static final float CornerCut = 0.6f;
    public static final float BaseStrength = 0.35f;
    public static final float PulseBoost = 0.18f;
    public static final int PulseTicks = 4;
    public static final int IntensitySteps = 12;
    public static final int SlowBeat = 24;
    public static final int FastBeat = 11;
    public static final float QuietBeat = 0.28f;
    public static final float LoudBeat = 0.6f;
    public static final float SpellShare = 0.6f;
    public static final int WhisperRefresh = 30;
    public static final int Grace = 80;
    public static final int FadeOutTicks = 20;
    public static final int TendrilCount = 10;
    public static final int TendrilLength = 7;
    public static final float TendrilAlpha = 0.34f;
    public static final int RootSteps = 2;
    private static boolean Active;
    private static String Name = "";
    private static int Total;
    private static int Age;
    private static int NextBeat;
    private static int Beat;
    private static int Shown;
    private static int Fade;
    private static boolean Whispered;
    private static long CachedKey = Long.MIN_VALUE;
    private static int[] Rects = new int[0];
    private static int RectCount;
    private static int[][] Tendrils = new int[0][];

    private Z0001Dread() {
    }

    public static void begin(String Target, int Ticks) {
        Active = true;
        Name = Target;
        Total = Math.max(20, Ticks);
        Age = 0;
        NextBeat = 6;
        Beat = 0;
        Shown = 0;
        Fade = FadeOutTicks;
        Whispered = false;
        CachedKey = Long.MIN_VALUE;
        LocalPlayer User = Minecraft.getInstance().player;
        RandomSource Random = User != null ? User.getRandom() : RandomSource.create();
        Tendrils = new int[TendrilCount][];
        for (int Index = 0; Index < TendrilCount; Index++) {
            Tendrils[Index] = new int[]{Random.nextInt(4), Random.nextInt(1000), 3 + Random.nextInt(TendrilLength - 2), Random.nextInt(3) - 1};
        }
        sound(Z0001Registry.WhisperLayers, 0.95f, 0.5f);
    }

    public static boolean isActive() {
        return Active;
    }

    public static float intensity() {
        if (!Active && Fade <= 0) {
            return 0.0f;
        }
        float Rise = Mth.clamp(Age / (float) Total, 0.0f, 1.0f);
        float Out = Active ? 1.0f : Fade / (float) FadeOutTicks;
        return (0.25f + 0.75f * Rise) * Out;
    }

    private static void sound(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch, float Volume) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Layer.get(), Pitch, Volume));
        }
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null) {
            reset();
            return;
        }
        if (!Active) {
            if (Fade > 0) {
                Fade--;
            }
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        if (User.isDeadOrDying() || User.isCreative() || User.isSpectator() || Age > Total + Grace) {
            end();
            return;
        }
        Age++;
        if (Beat > 0) {
            Beat--;
        }
        float Progress = Mth.clamp(Age / (float) Total, 0.0f, 1.0f);
        if (Age >= NextBeat) {
            float Volume = Mth.lerp(Progress, QuietBeat, LoudBeat);
            sound(Z0001Registry.HeartLayers, 0.96f + User.getRandom().nextFloat() * 0.06f, Volume);
            Beat = PulseTicks;
            NextBeat = Age + Math.round(Mth.lerp(Progress, SlowBeat, FastBeat));
        }
        if (!Whispered && Age >= Total / 2) {
            Whispered = true;
            sound(Z0001Registry.WhisperLayers, 0.85f, 0.4f);
        }
        int Letters = Mth.clamp(Mth.ceil(Name.length() * Math.min(1.0f, Age / (Total * SpellShare))), 1, Name.length());
        if (Letters != Shown || Age % WhisperRefresh == 0) {
            Shown = Letters;
            Client.gui.setOverlayMessage(Component.literal(Name.substring(0, Letters)).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
        }
    }

    private static void end() {
        Active = false;
        Fade = FadeOutTicks;
    }

    public static void reset() {
        Active = false;
        Fade = 0;
        Age = 0;
        Name = "";
    }

    public static int texel(int Width, int Height) {
        return Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        float Amount = intensity();
        if (Client.player == null || Client.options.hideGui || Amount <= 0.0f) {
            return;
        }
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        int Texel = texel(Width, Height);
        int Columns = (Width + Texel - 1) / Texel;
        int Rows = (Height + Texel - 1) / Texel;
        int OffsetX = (Width - Columns * Texel) / 2;
        int OffsetY = (Height - Rows * Texel) / 2;
        float Scale = (float) Display.getGuiScale();
        float Strength = Mth.clamp(Amount * (BaseStrength + (1.0f - BaseStrength) * Amount) + (Beat > 0 ? PulseBoost * Amount : 0.0f), 0.0f, 1.0f);
        int Step = Math.round(Strength * IntensitySteps);
        if (Step <= 0) {
            return;
        }
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        long Key = ((long) Width * 31L + Height) * 64L + Step;
        if (Key != CachedKey) {
            buildVignette(Columns, Rows, OffsetX, OffsetY, Texel, Step / (float) IntensitySteps);
            CachedKey = Key;
        }
        for (int Index = 0; Index < RectCount; Index++) {
            int Base = Index * 5;
            Graphics.fill(Rects[Base], Rects[Base + 1], Rects[Base + 2], Rects[Base + 3], Rects[Base + 4]);
        }
        drawTendrils(Graphics, Columns, Rows, OffsetX, OffsetY, Texel, Step / (float) IntensitySteps);
        Pose.popPose();
    }

    private static void drawTendrils(GuiGraphics Graphics, int Columns, int Rows, int OffsetX, int OffsetY, int Texel, float Strength) {
        int Colour = argb(TendrilAlpha * Strength, VignetteColour);
        for (int[] Tendril : Tendrils) {
            int Side = Tendril[0];
            int Length = Math.round(Tendril[2] * Strength);
            boolean Vertical = Side < 2;
            int Span = Vertical ? Columns : Rows;
            int Along = Mth.clamp(Math.round(Tendril[1] / 1000.0f * Span), 2, Math.max(2, Span - 3));
            for (int Step = 0; Step < Length; Step++) {
                int Drift = Tendril[3] * (Step / 3);
                int Column;
                int Row;
                if (Vertical) {
                    Column = Along + Drift;
                    Row = Side == 0 ? Step : Rows - 1 - Step;
                } else {
                    Row = Along + Drift;
                    Column = Side == 2 ? Step : Columns - 1 - Step;
                }
                int Thick = Step < RootSteps ? 2 : 1;
                int X = OffsetX + Column * Texel;
                int Y = OffsetY + Row * Texel;
                Graphics.fill(X, Y, X + Texel * (Vertical ? Thick : 1), Y + Texel * (Vertical ? 1 : Thick), Colour);
            }
        }
    }

    private static void buildVignette(int Columns, int Rows, int OffsetX, int OffsetY, int Texel, float Strength) {
        RectCount = 0;
        int Rings = RingAlphas.length;
        for (int Row = 0; Row < Rows; Row++) {
            int RunStart = 0;
            int RunColour = 0;
            for (int Column = 0; Column <= Columns; Column++) {
                int Colour = 0;
                if (Column < Columns) {
                    int Dx = Math.min(Column, Columns - 1 - Column);
                    int Dy = Math.min(Row, Rows - 1 - Row);
                    int Ring = (int) Math.min(Math.min(Dx, Dy), (Dx + Dy) * CornerCut) / RingWidth;
                    if (Ring < Rings) {
                        Colour = argb(RingAlphas[Ring] * Strength, VignetteColour);
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
