package net.rai.scp_unleashed.scp012.client;

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
import net.rai.scp_unleashed.scp009.Scp009Doom;
import net.rai.scp_unleashed.scp012.Scp012Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class ScoreOverlay {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp012_score_overlay");
    public static final int TexelTarget = 100;
    public static final int FewestNotes = 4;
    public static final int MostNotes = 12;
    public static final int SlowSpawn = 18;
    public static final int FastSpawn = 6;
    public static final int SpawnJitter = 6;
    public static final int PopTicks = 3;
    public static final int FadeTicks = 12;
    public static final int FadeSteps = 4;
    public static final int ShortLinger = 34;
    public static final int LongLinger = 70;
    public static final int DripEvery = 10;
    public static final int DripLongest = 3;
    public static final float BigChance = 0.22f;
    public static final int Placements = 10;
    public static final float LeftEdge = 0.04f;
    public static final float RightEdge = 0.96f;
    public static final float TopEdge = 0.05f;
    public static final float BottomEdge = 0.8f;
    public static final float ClearX = 0.16f;
    public static final float ClearY = 0.18f;
    public static final int BodyColour = 0xA3101E;
    public static final int LightColour = 0xD8303F;
    public static final int DarkColour = 0x5E0710;
    public static final int PopColour = 0xF25A64;
    public static final int ShadowColour = 0x1C0206;
    public static final float ShadowAlpha = 0.42f;
    public static final float NoteAlpha = 0.9f;
    public static final int VignetteColour = 0x7A0814;
    public static final float[] RingAlphas = {0.34f, 0.22f, 0.12f, 0.05f};
    public static final int RingWidth = 2;
    public static final float CornerCut = 0.62f;
    public static final int PulsePeriod = 48;
    public static final float[] PulseLevels = {0.62f, 0.74f, 0.86f, 1.0f, 0.86f, 0.74f};
    public static final int IntensitySteps = 12;
    public static final int FadeInTicks = 16;
    public static final int FadeOutTicks = 30;
    public static final int[] Semitones = {0, 1, 5, 6, 7, 11, 12, 13, 17, 18, 19, 23, 24};
    public static final float NoteVolume = 0.22f;
    public static final float NoteVolumeGrowth = 0.14f;
    public static final float BellVolume = 0.12f;
    public static final float BellChance = 0.3f;
    private static final List<Note> Notes = new ArrayList<>();
    private static float Intensity;
    private static int SpawnCountdown;
    private static long Clock;
    private static long CachedKey = Long.MIN_VALUE;
    private static int[] Rects = new int[0];
    private static int RectCount;

    private ScoreOverlay() {
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
        boolean Active = Scp012ClientState.isExposed();
        Intensity = Active ? Math.min(1.0f, Intensity + 1.0f / FadeInTicks) : Math.max(0.0f, Intensity - 1.0f / FadeOutTicks);
        Iterator<Note> Living = Notes.iterator();
        while (Living.hasNext()) {
            Note Each = Living.next();
            Each.Age++;
            if (Each.Age >= Each.Life) {
                Living.remove();
            }
        }
        if (!Active) {
            SpawnCountdown = 0;
            return;
        }
        float Progress = Scp012ClientState.meter() / 100.0f;
        SpawnCountdown--;
        int Cap = Math.round(Mth.lerp(Progress, FewestNotes, MostNotes));
        if (SpawnCountdown <= 0 && Notes.size() < Cap) {
            RandomSource Random = User.getRandom();
            spawn(Client, Random, Progress);
            SpawnCountdown = Math.round(Mth.lerp(Progress, SlowSpawn, FastSpawn)) + Random.nextInt(SpawnJitter);
        }
    }

    private static void spawn(Minecraft Client, RandomSource Random, float Progress) {
        ScoreGlyphs.Glyph Shape = ScoreGlyphs.All.get(Random.nextInt(ScoreGlyphs.All.size()));
        int Scale = !Shape.Clef() && Random.nextFloat() < BigChance ? 2 : 1;
        Window Display = Client.getWindow();
        int Texel = texel(Display.getWidth(), Display.getHeight());
        float SpanX = Shape.Width() * Scale * Texel / (float) Display.getWidth();
        float SpanY = Shape.Height() * Scale * Texel / (float) Display.getHeight();
        float PadX = Texel / (float) Display.getWidth();
        float PadY = Texel / (float) Display.getHeight();
        for (int Attempt = 0; Attempt < Placements; Attempt++) {
            float X = LeftEdge + Random.nextFloat() * Math.max(0.0f, RightEdge - LeftEdge - SpanX);
            float Y = TopEdge + Random.nextFloat() * Math.max(0.0f, BottomEdge - TopEdge - SpanY);
            float CentreX = X + SpanX / 2.0f;
            float CentreY = Y + SpanY / 2.0f;
            if (Math.abs(CentreX - 0.5f) < ClearX + SpanX / 2.0f && Math.abs(CentreY - 0.5f) < ClearY + SpanY / 2.0f) {
                continue;
            }
            boolean Crowded = false;
            for (Note Other : Notes) {
                if (X < Other.X + Other.SpanX + PadX && Other.X < X + SpanX + PadX && Y < Other.Y + Other.SpanY + PadY && Other.Y < Y + SpanY + PadY) {
                    Crowded = true;
                    break;
                }
            }
            if (Crowded) {
                continue;
            }
            int Life = PopTicks + ShortLinger + Random.nextInt(LongLinger - ShortLinger + 1) + FadeTicks;
            Notes.add(new Note(Shape, X, Y, SpanX, SpanY, Scale, Life, Random.nextInt(DripEvery)));
            sound(Client, Random, Shape, Y + SpanY / 2.0f, Progress);
            return;
        }
    }

    private static void sound(Minecraft Client, RandomSource Random, ScoreGlyphs.Glyph Shape, float Height, float Progress) {
        if (Shape.Clef()) {
            Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp012Registry.NoteBell.get(), 0.5f + Random.nextInt(2) * 0.03f, BellVolume * 1.4f));
            return;
        }
        if (!Shape.Sounding()) {
            return;
        }
        float Rise = 1.0f - Mth.clamp((Height - TopEdge) / (BottomEdge - TopEdge), 0.0f, 1.0f);
        int Index = Mth.clamp(Math.round(Rise * (Semitones.length - 1)), 0, Semitones.length - 1);
        float Pitch = (float) Math.pow(2.0, (Semitones[Index] - 12) / 12.0);
        float Volume = NoteVolume + NoteVolumeGrowth * Progress;
        Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp012Registry.Note.get(), Pitch, Volume));
        if (Random.nextFloat() < BellChance) {
            float Clash = Math.min(2.0f, Pitch * (float) Math.pow(2.0, 1.0 / 12.0));
            Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp012Registry.NoteBell.get(), Clash, BellVolume));
        }
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
        if (User == null || Client.options.hideGui || Intensity <= 0.0f && Notes.isEmpty()) {
            return;
        }
        if (Scp009Doom.instance(User) != null) {
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
        int Step = Math.round(Intensity * pulse(Clock) * IntensitySteps);
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
        for (Note Each : Notes) {
            drawNote(Graphics, Each, Width, Height, Texel);
        }
        Pose.popPose();
    }

    public static float noteAlpha(int Age, int Life) {
        int Left = Life - Age;
        if (Left >= FadeTicks) {
            return 1.0f;
        }
        int Step = Math.max(0, Left) * FadeSteps / FadeTicks;
        return (Step + 1) / (float) (FadeSteps + 1);
    }

    public static int dripLength(Note Each) {
        int Grown = (Each.Age - PopTicks + Each.DripPhase) / DripEvery;
        return Mth.clamp(Grown, 0, DripLongest);
    }

    private static void drawNote(GuiGraphics Graphics, Note Each, int Width, int Height, int Texel) {
        ScoreGlyphs.Glyph Shape = Each.Shape;
        int Cell = Texel * Each.Scale;
        int Left = Math.round(Each.X * Width / Texel) * Texel;
        int Top = Math.round(Each.Y * Height / Texel) * Texel;
        float Alpha = noteAlpha(Each.Age, Each.Life) * NoteAlpha;
        boolean Popping = Each.Age < PopTicks;
        int Drip = Popping ? 0 : dripLength(Each);
        int Shadow = argb(Alpha * ShadowAlpha, ShadowColour);
        for (int Y = 0; Y < Shape.Height() + DripLongest; Y++) {
            int RunStart = -1;
            for (int X = 0; X <= Shape.Width(); X++) {
                boolean Filled = X < Shape.Width() && cellAt(Shape, X, Y, Drip) != 0;
                if (Filled && RunStart < 0) {
                    RunStart = X;
                } else if (!Filled && RunStart >= 0) {
                    Graphics.fill(Left + RunStart * Cell + Texel, Top + Y * Cell + Texel, Left + X * Cell + Texel, Top + (Y + 1) * Cell + Texel, Shadow);
                    RunStart = -1;
                }
            }
        }
        for (int Y = 0; Y < Shape.Height() + DripLongest; Y++) {
            int RunStart = 0;
            int RunColour = 0;
            for (int X = 0; X <= Shape.Width(); X++) {
                int Kind = X < Shape.Width() ? cellAt(Shape, X, Y, Drip) : 0;
                int Colour = Kind == 0 ? 0 : argb(Alpha, Popping ? PopColour : colourOf(Kind));
                if (Colour != RunColour) {
                    if (RunColour != 0) {
                        Graphics.fill(Left + RunStart * Cell, Top + Y * Cell, Left + X * Cell, Top + (Y + 1) * Cell, RunColour);
                    }
                    RunStart = X;
                    RunColour = Colour;
                }
            }
        }
    }

    public static int cellAt(ScoreGlyphs.Glyph Shape, int X, int Y, int Drip) {
        int Kind = Shape.at(X, Y);
        if (Kind != 0 || Drip <= 0) {
            return Kind;
        }
        for (int[] Origin : Shape.Drips()) {
            int Below = Y - Origin[1];
            if (Origin[0] == X && Below >= 1 && Below <= Drip) {
                return Below == Drip ? ScoreGlyphs.Dark : ScoreGlyphs.Body;
            }
        }
        return 0;
    }

    public static int colourOf(int Kind) {
        return switch (Kind) {
            case ScoreGlyphs.Light -> LightColour;
            case ScoreGlyphs.Dark -> DarkColour;
            default -> BodyColour;
        };
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
        Notes.clear();
        Intensity = 0.0f;
        SpawnCountdown = 0;
    }

    private static final class Note {
        private final ScoreGlyphs.Glyph Shape;
        private final float X;
        private final float Y;
        private final float SpanX;
        private final float SpanY;
        private final int Scale;
        private final int Life;
        private final int DripPhase;
        private int Age;

        private Note(ScoreGlyphs.Glyph Shape, float X, float Y, float SpanX, float SpanY, int Scale, int Life, int DripPhase) {
            this.Shape = Shape;
            this.X = X;
            this.Y = Y;
            this.SpanX = SpanX;
            this.SpanY = SpanY;
            this.Scale = Scale;
            this.Life = Life;
            this.DripPhase = DripPhase;
        }
    }
}
