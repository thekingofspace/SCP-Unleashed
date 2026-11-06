package net.rai.scp_unleashed.scp020.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Scp020Eyes {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp020_eyes");
    public static final int TexelTarget = 105;
    public static final int MinIntensity = 6;
    public static final int FewestEyes = 1;
    public static final int MostEyes = 7;
    public static final int SlowSpawn = 260;
    public static final int FastSpawn = 26;
    public static final int FirstSpawn = 50;
    public static final int OpenStep = 2;
    public static final int ShortLinger = 36;
    public static final int LongLinger = 150;
    public static final int LingerJitter = 40;
    public static final int BlinkEvery = 70;
    public static final int BlinkStep = 2;
    public static final int FadeTicks = 16;
    public static final int FadeSteps = 4;
    public static final int DartEvery = 45;
    public static final int DartTicks = 12;
    public static final float BigChance = 0.35f;
    public static final int Placements = 12;
    public static final float LeftEdge = 0.04f;
    public static final float RightEdge = 0.96f;
    public static final float TopEdge = 0.05f;
    public static final float BottomEdge = 0.76f;
    public static final float ClearX = 0.13f;
    public static final float ClearY = 0.15f;
    public static final float LookFar = 0.3f;
    public static final float LookNear = 0.1f;
    public static final float EyeAlpha = 0.94f;
    public static final float ShadowAlpha = 0.3f;
    public static final int LidColour = 0x6B7A40;
    public static final int LidLightColour = 0x7D8C4C;
    public static final int LidLowerColour = 0x56632F;
    public static final int CornerColour = 0x4F5B2B;
    public static final int LashColour = 0x5C6933;
    public static final int ScleraColour = 0xD8D5AA;
    public static final int ScleraShadeColour = 0xB4B286;
    public static final int IrisColour = 0x5B772C;
    public static final int IrisLightColour = 0x83A044;
    public static final int IrisRimColour = 0x3E5320;
    public static final int PupilColour = 0x1F2712;
    public static final int GlintColour = 0xF4F4DE;
    public static final int ShadowColour = 0x10140A;
    private static final List<Eye> Eyes = new ArrayList<>();
    private static int SpawnCountdown;
    private static boolean Active;

    private Scp020Eyes() {
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
        Iterator<Eye> Living = Eyes.iterator();
        while (Living.hasNext()) {
            Eye Each = Living.next();
            Each.tick(User.getRandom());
            if (Each.Age >= Each.Life) {
                Living.remove();
            }
        }
        int Intensity = Scp020ClientState.intensity();
        if (Intensity < MinIntensity) {
            Active = false;
            SpawnCountdown = 0;
            return;
        }
        RandomSource Random = User.getRandom();
        if (!Active) {
            Active = true;
            SpawnCountdown = FirstSpawn + Random.nextInt(FirstSpawn);
        }
        float Progress = Scp020ClientState.strength();
        SpawnCountdown--;
        int Cap = Math.round(Mth.lerp(Progress, FewestEyes, MostEyes));
        if (SpawnCountdown <= 0 && Eyes.size() < Cap) {
            spawn(Client, Random, Progress);
            int Interval = Math.round(Mth.lerp(Progress, SlowSpawn, FastSpawn));
            SpawnCountdown = Interval + Random.nextInt(Interval / 2 + 1);
        }
    }

    private static void spawn(Minecraft Client, RandomSource Random, float Progress) {
        Scp020EyeShapes.Shape Shape = Scp020EyeShapes.All.get(Random.nextInt(Scp020EyeShapes.All.size()));
        int Scale = Random.nextFloat() < BigChance * Progress ? 2 : 1;
        Window Display = Client.getWindow();
        int Texel = texel(Display.getWidth(), Display.getHeight());
        float SpanX = Shape.gridWidth() * Scale * Texel / (float) Display.getWidth();
        float SpanY = Shape.gridHeight() * Scale * Texel / (float) Display.getHeight();
        float PadX = 2 * Texel / (float) Display.getWidth();
        float PadY = 2 * Texel / (float) Display.getHeight();
        for (int Attempt = 0; Attempt < Placements; Attempt++) {
            float X = LeftEdge + Random.nextFloat() * Math.max(0.0f, RightEdge - LeftEdge - SpanX);
            float Y = TopEdge + Random.nextFloat() * Math.max(0.0f, BottomEdge - TopEdge - SpanY);
            float CentreX = X + SpanX / 2.0f;
            float CentreY = Y + SpanY / 2.0f;
            if (Math.abs(CentreX - 0.5f) < ClearX + SpanX / 2.0f && Math.abs(CentreY - 0.5f) < ClearY + SpanY / 2.0f) {
                continue;
            }
            boolean Crowded = false;
            for (Eye Other : Eyes) {
                if (X < Other.X + Other.SpanX + PadX && Other.X < X + SpanX + PadX && Y < Other.Y + Other.SpanY + PadY && Other.Y < Y + SpanY + PadY) {
                    Crowded = true;
                    break;
                }
            }
            if (Crowded) {
                continue;
            }
            int Linger = Math.round(Mth.lerp(Progress, ShortLinger, LongLinger)) + Random.nextInt(LingerJitter);
            Eyes.add(new Eye(Shape, X, Y, SpanX, SpanY, Scale, Linger, Random));
            return;
        }
    }

    public static int texel(int Width, int Height) {
        return Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
    }

    public static float fadeAlpha(int Age, int Life) {
        int Left = Life - Age;
        if (Left >= FadeTicks) {
            return 1.0f;
        }
        int Step = Math.max(0, Left) * FadeSteps / FadeTicks;
        return (Step + 1) / (float) (FadeSteps + 1);
    }

    public static int aperture(int Age, int BlinkAge) {
        int Opening = Age / OpenStep;
        if (Opening < Scp020EyeShapes.Open) {
            return Opening;
        }
        if (BlinkAge >= 0) {
            int Phase = BlinkAge / BlinkStep;
            if (Phase == 0 || Phase == 2) {
                return 1;
            }
            if (Phase == 1) {
                return 0;
            }
        }
        return Scp020EyeShapes.Open;
    }

    public static int look(float Offset, float Far, float Near, int Reach) {
        float Size = Math.abs(Offset);
        if (Size < Near) {
            return 0;
        }
        int Step = Size > Far ? Reach : 1;
        return Offset > 0 ? Step : -Step;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.options.hideGui || Eyes.isEmpty()) {
            return;
        }
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        int Texel = texel(Width, Height);
        float Scale = (float) Display.getGuiScale();
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        for (Eye Each : Eyes) {
            draw(Graphics, Each, Width, Height, Texel);
        }
        Pose.popPose();
    }

    private static void draw(GuiGraphics Graphics, Eye Each, int Width, int Height, int Texel) {
        Scp020EyeShapes.Shape Shape = Each.Shape;
        int[] Grid = Shape.build(aperture(Each.Age, Each.BlinkAge), Each.LookX, Each.LookY);
        int Columns = Shape.gridWidth();
        int Rows = Shape.gridHeight();
        int Cell = Texel * Each.Scale;
        int Left = Math.round(Each.X * Width / Texel) * Texel;
        int Top = Math.round(Each.Y * Height / Texel) * Texel;
        float Alpha = fadeAlpha(Each.Age, Each.Life) * EyeAlpha;
        int Shadow = argb(Alpha * ShadowAlpha, ShadowColour);
        for (int Y = 0; Y < Rows; Y++) {
            int RunStart = -1;
            for (int X = 0; X <= Columns; X++) {
                boolean Filled = X < Columns && Grid[Y * Columns + X] != Scp020EyeShapes.Empty;
                if (Filled && RunStart < 0) {
                    RunStart = X;
                } else if (!Filled && RunStart >= 0) {
                    Graphics.fill(Left + RunStart * Cell + Texel, Top + Y * Cell + Texel, Left + X * Cell + Texel, Top + (Y + 1) * Cell + Texel, Shadow);
                    RunStart = -1;
                }
            }
        }
        for (int Y = 0; Y < Rows; Y++) {
            int RunStart = 0;
            int RunColour = 0;
            for (int X = 0; X <= Columns; X++) {
                int Kind = X < Columns ? Grid[Y * Columns + X] : Scp020EyeShapes.Empty;
                int Colour = Kind == Scp020EyeShapes.Empty ? 0 : argb(Alpha, colourOf(Kind));
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

    public static int colourOf(int Kind) {
        return switch (Kind) {
            case Scp020EyeShapes.Lid -> LidColour;
            case Scp020EyeShapes.LidLight -> LidLightColour;
            case Scp020EyeShapes.LidLower -> LidLowerColour;
            case Scp020EyeShapes.Corner -> CornerColour;
            case Scp020EyeShapes.Lash -> LashColour;
            case Scp020EyeShapes.ScleraShade -> ScleraShadeColour;
            case Scp020EyeShapes.Iris -> IrisColour;
            case Scp020EyeShapes.IrisLight -> IrisLightColour;
            case Scp020EyeShapes.IrisRim -> IrisRimColour;
            case Scp020EyeShapes.Pupil -> PupilColour;
            case Scp020EyeShapes.Glint -> GlintColour;
            default -> ScleraColour;
        };
    }

    private static int argb(float Alpha, int Colour) {
        int A = Math.max(1, Math.min(255, Math.round(Alpha * 255.0f)));
        return A << 24 | Colour & 0xFFFFFF;
    }

    public static void reset() {
        Eyes.clear();
        SpawnCountdown = 0;
        Active = false;
    }

    private static final class Eye {
        private final Scp020EyeShapes.Shape Shape;
        private final float X;
        private final float Y;
        private final float SpanX;
        private final float SpanY;
        private final int Scale;
        private final int Life;
        private final int HomeX;
        private final int HomeY;
        private int Age;
        private int BlinkAge = -1;
        private int DartLeft;
        private int LookX;
        private int LookY;

        private Eye(Scp020EyeShapes.Shape Shape, float X, float Y, float SpanX, float SpanY, int Scale, int Linger, RandomSource Random) {
            this.Shape = Shape;
            this.X = X;
            this.Y = Y;
            this.SpanX = SpanX;
            this.SpanY = SpanY;
            this.Scale = Scale;
            this.Life = Scp020EyeShapes.Open * OpenStep + Linger + FadeTicks;
            this.HomeX = look(0.5f - (X + SpanX / 2.0f), LookFar, LookNear, Shape.lookReach());
            this.HomeY = Shape.Height() >= 5 ? look(0.45f - (Y + SpanY / 2.0f), LookFar, LookNear, 1) : 0;
            this.LookX = HomeX;
            this.LookY = HomeY;
        }

        private void tick(RandomSource Random) {
            Age++;
            if (BlinkAge >= 0) {
                BlinkAge++;
                if (BlinkAge >= BlinkStep * 3) {
                    BlinkAge = -1;
                }
            } else if (Age > Scp020EyeShapes.Open * OpenStep && Life - Age > FadeTicks && Random.nextInt(BlinkEvery) == 0) {
                BlinkAge = 0;
            }
            if (DartLeft > 0) {
                DartLeft--;
                if (DartLeft == 0) {
                    LookX = HomeX;
                    LookY = HomeY;
                }
            } else if (Random.nextInt(DartEvery) == 0) {
                DartLeft = DartTicks;
                int Reach = Shape.lookReach();
                LookX = Random.nextInt(Reach * 2 + 1) - Reach;
                LookY = Shape.Height() >= 5 ? Random.nextInt(3) - 1 : 0;
            }
        }
    }
}
