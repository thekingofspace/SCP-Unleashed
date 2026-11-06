package net.rai.scp_unleashed.scp151.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp151.Scp151Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class Scp151Overlay {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp151_drowning_overlay");
    public static final int TexelTarget = 110;
    public static final int FadeInTicks = 24;
    public static final int FadeOutTicks = 50;
    public static final int VignetteColour = 0x1A6E7A;
    public static final float[] RingAlphas = {0.34f, 0.22f, 0.12f, 0.06f};
    public static final int RingWidth = 2;
    public static final float CornerCut = 0.62f;
    public static final float BaseStrength = 0.55f;
    public static final float BreathlessStrength = 0.45f;
    public static final int PulsePeriod = 56;
    public static final float[] PulseLevels = {0.82f, 0.9f, 0.97f, 1.0f, 0.97f, 0.9f};
    public static final int IntensitySteps = 12;
    public static final int RimColour = 0xB4E4EE;
    public static final int ShineColour = 0xF0FCFF;
    public static final int FillColour = 0x4AA2B6;
    public static final float RimAlpha = 0.55f;
    public static final float ShineAlpha = 0.75f;
    public static final float FillAlpha = 0.14f;
    public static final int FewestBubbles = 3;
    public static final int MostBubbles = 11;
    public static final int SlowSpawn = 16;
    public static final int FastSpawn = 5;
    public static final int SpawnJitter = 5;
    public static final int ShortLife = 34;
    public static final int LongLife = 66;
    public static final int PopTicks = 3;
    public static final int FadeSteps = 3;
    public static final float EdgeBand = 0.2f;
    public static final float SpawnLow = 0.86f;
    public static final float SpawnSpread = 0.1f;
    public static final int WobblePeriod = 12;
    public static final int[] Wobble = {0, 1, 0, -1};
    public static final float EnterVolume = 0.35f;
    public static final String[] Small = {" ## ", "#ho#", "#oo#", " ## "};
    public static final String[] Medium = {" ### ", "#hoo#", "#hoo#", "#ooo#", " ### "};
    public static final String[] Large = {" #### ", "#hhoo#", "#hooo#", "#oooo#", "#oooo#", " #### "};
    public static final String[] Burst = {"#   #", " # # ", "     ", " # # ", "#   #"};
    public static final String[][] Glyphs = {Small, Medium, Large};
    public static final int[] RiseEvery = {3, 2, 2};
    private static final List<Bubble> Bubbles = new ArrayList<>();
    private static float Intensity;
    private static int SpawnCountdown;
    private static long Clock;
    private static long CachedKey = Long.MIN_VALUE;
    private static int[] Rects = new int[0];
    private static int RectCount;
    private static Scp151AmbienceSound Loop;

    private Scp151Overlay() {
    }

    public static float intensity() {
        return Intensity;
    }

    public static float breathless(LocalPlayer User) {
        return 1.0f - Mth.clamp(User.getAirSupply() / (float) User.getMaxAirSupply(), 0.0f, 1.0f);
    }

    public static void tick(Minecraft Client) {
        LocalPlayer User = Client.player;
        if (User == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        Clock++;
        boolean Active = Scp151ClientState.isGazing();
        Intensity = Active ? Math.min(1.0f, Intensity + 1.0f / FadeInTicks) : Math.max(0.0f, Intensity - 1.0f / FadeOutTicks);
        if (Scp151ClientState.consumeStart()) {
            Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp151Registry.GazeEnter.get(), 0.9f + User.getRandom().nextFloat() * 0.15f, EnterVolume));
        }
        if (Intensity > 0.0f && (Loop == null || !Client.getSoundManager().isActive(Loop))) {
            Loop = new Scp151AmbienceSound();
            Client.getSoundManager().play(Loop);
        }
        Iterator<Bubble> Living = Bubbles.iterator();
        while (Living.hasNext()) {
            Bubble Each = Living.next();
            Each.Age++;
            if (Each.Age >= Each.Life) {
                Living.remove();
            }
        }
        if (!Active) {
            SpawnCountdown = 0;
            return;
        }
        float Breathless = breathless(User);
        SpawnCountdown--;
        int Cap = Math.round(Mth.lerp(Breathless, FewestBubbles, MostBubbles));
        if (SpawnCountdown <= 0 && Bubbles.size() < Cap) {
            RandomSource Random = User.getRandom();
            spawn(Random);
            SpawnCountdown = Math.round(Mth.lerp(Breathless, SlowSpawn, FastSpawn)) + Random.nextInt(SpawnJitter);
        }
    }

    private static void spawn(RandomSource Random) {
        int Kind = Random.nextInt(10) < 5 ? 0 : Random.nextInt(10) < 7 ? 1 : 2;
        boolean Left = Random.nextBoolean();
        float Across = Random.nextFloat() * EdgeBand;
        float X = Left ? 0.02f + Across : 0.98f - Across;
        float Y = SpawnLow + Random.nextFloat() * SpawnSpread;
        int Life = ShortLife + Random.nextInt(LongLife - ShortLife + 1);
        Bubbles.add(new Bubble(Kind, X, Y, Life, Random.nextInt(Wobble.length)));
    }

    public static int texel(int Width, int Height) {
        return Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
    }

    public static float pulse(long Time) {
        int Index = (int) Math.floorMod(Time * PulseLevels.length / PulsePeriod, (long) PulseLevels.length);
        return PulseLevels[Index];
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User == null || Client.options.hideGui || Intensity <= 0.0f && Bubbles.isEmpty()) {
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
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        float Strength = Intensity * (BaseStrength + BreathlessStrength * breathless(User));
        int Step = Math.round(Strength * pulse(Clock) * IntensitySteps);
        if (Step > 0) {
            long Key = ((long) Width * 31L + Height) * 64L + Step;
            if (Key != CachedKey) {
                buildVignette(Columns, Rows, OffsetX, OffsetY, Texel, Step / (float) IntensitySteps);
                CachedKey = Key;
            }
            for (int Index = 0; Index < RectCount; Index++) {
                int Base = Index * 5;
                Graphics.fill(Rects[Base], Rects[Base + 1], Rects[Base + 2], Rects[Base + 3], Rects[Base + 4]);
            }
        }
        for (Bubble Each : Bubbles) {
            drawBubble(Graphics, Each, Columns, Rows, OffsetX, OffsetY, Texel);
        }
        Pose.popPose();
    }

    public static float bubbleAlpha(int Age, int Life) {
        int Left = Life - Age - PopTicks;
        float Fade = Left >= FadeSteps * 2 ? 1.0f : (Math.max(0, Left) / 2 + 1) / (float) (FadeSteps + 1);
        return Fade * Mth.clamp(Intensity * 1.4f, 0.0f, 1.0f);
    }

    private static void drawBubble(GuiGraphics Graphics, Bubble Each, int Columns, int Rows, int OffsetX, int OffsetY, int Texel) {
        boolean Popping = Each.Life - Each.Age <= PopTicks;
        String[] Glyph = Popping ? Burst : Glyphs[Each.Kind];
        int Rise = Each.Age / RiseEvery[Each.Kind];
        int Sway = Wobble[(Each.Age / WobblePeriod + Each.Phase) % Wobble.length];
        int Column = Math.round(Each.X * Columns) - Glyph[0].length() / 2 + Sway;
        int Row = Math.round(Each.Y * Rows) - Rise;
        float Alpha = Popping ? Mth.clamp(Intensity * 1.4f, 0.0f, 1.0f) : bubbleAlpha(Each.Age, Each.Life);
        if (Alpha <= 0.0f) {
            return;
        }
        for (int Y = 0; Y < Glyph.length; Y++) {
            String Line = Glyph[Y];
            for (int X = 0; X < Line.length(); X++) {
                int Colour = switch (Line.charAt(X)) {
                    case '#' -> argb(Alpha * RimAlpha, RimColour);
                    case 'h' -> argb(Alpha * ShineAlpha, ShineColour);
                    case 'o' -> argb(Alpha * FillAlpha, FillColour);
                    default -> 0;
                };
                if (Colour == 0) {
                    continue;
                }
                int Left = OffsetX + (Column + X) * Texel;
                int Top = OffsetY + (Row + Y) * Texel;
                Graphics.fill(Left, Top, Left + Texel, Top + Texel, Colour);
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

    public static void reset() {
        Bubbles.clear();
        Intensity = 0.0f;
        SpawnCountdown = 0;
        if (Loop != null) {
            Minecraft.getInstance().getSoundManager().stop(Loop);
            Loop = null;
        }
    }

    private static final class Bubble {
        private final int Kind;
        private final float X;
        private final float Y;
        private final int Life;
        private final int Phase;
        private int Age;

        private Bubble(int Kind, float X, float Y, int Life, int Phase) {
            this.Kind = Kind;
            this.X = X;
            this.Y = Y;
            this.Life = Life;
            this.Phase = Phase;
        }
    }
}
