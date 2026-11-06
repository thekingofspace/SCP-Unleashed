package net.rai.scp_unleashed.senses.client;

import net.minecraft.util.Mth;

public final class BlindfoldLeak {
    public static final float BottomBand = 0.1f;
    public static final float NoseBand = 0.08f;
    public static final float TopBand = 0.05f;
    public static final float SideBand = 0.035f;
    public static final float Wobble = 0.018f;
    public static final int Block = 4;
    public static final int Steps = 3;
    public static final float Peak = 0.9f;
    public static final float ShadePeak = 0.38f;
    public static final float CreaseWidth = 3.0f;
    public static final float ValleyWidth = 11.0f;
    public static final float ReferenceRows = 128.0f;
    public static final float[][] Folds = {
            {-1.0f, -0.2f, -0.1f, 0.56f, 0.02f},
            {-1.0f, 0.03f, 0.01f, 0.44f, -0.015f},
            {-1.0f, 0.23f, 0.13f, 0.32f, 0.01f},
            {1.0f, -0.24f, -0.13f, 0.5f, 0.02f},
            {1.0f, -0.01f, 0.0f, 0.4f, -0.015f},
            {1.0f, 0.21f, 0.14f, 0.3f, 0.01f}
    };

    private BlindfoldLeak() {
    }

    public static float at(int X, int Y, int Wide, int Tall) {
        int Bottom = edgeStep(Tall - 0.5f - Y, Tall * bottomWidth(X, Wide, Tall) + Tall * Wobble * blockNoise(X, 11), Steps);
        int Top = edgeStep(Y + 0.5f, Tall * TopBand + Tall * Wobble * 0.6f * blockNoise(X, 23), Steps - 1);
        int Side = edgeStep(Math.min(X + 0.5f, Wide - 0.5f - X), Tall * SideBand + Tall * Wobble * 0.5f * blockNoise(Y, 37), Steps - 1);
        int Crease = 0;
        for (float[] Fold : Folds) {
            float[] Found = fold(X, Y, Wide, Tall, Fold);
            if (Found == null) {
                continue;
            }
            float Width = Math.max(1.0f, Math.round(CreaseWidth * Found[1] * scale(Tall)));
            if (Found[0] >= 0.0f && Found[0] < Width) {
                Crease = Math.max(Crease, 1);
            }
        }
        int Level = Math.max(Math.max(Bottom, Crease), Math.max(Top, Side));
        return Mth.clamp(Level / (float) Steps, 0.0f, 1.0f) * Peak;
    }

    public static float shade(int X, int Y, int Wide, int Tall) {
        int Level = 0;
        for (float[] Fold : Folds) {
            float[] Found = fold(X, Y, Wide, Tall, Fold);
            if (Found == null || Found[0] >= 0.0f) {
                continue;
            }
            float Width = Math.max(1.0f, Math.round(ValleyWidth * Found[1] * scale(Tall)));
            if (-Found[0] <= Width) {
                Level = Math.max(Level, -Found[0] <= Math.max(1.0f, Math.round(Width * 0.4f)) ? 2 : 1);
            }
        }
        return Level / 2.0f * ShadePeak;
    }

    private static float scale(int Tall) {
        return Math.max(1.0f, Tall / ReferenceRows);
    }

    private static float bottomWidth(int X, int Wide, int Tall) {
        float Off = Math.abs(X + 0.5f - Wide / 2.0f) / Tall;
        float Nose = (float) Math.exp(-Math.pow((Off - 0.11f) / 0.07f, 2.0));
        float Bridge = (float) Math.exp(-Math.pow(Off / 0.035f, 2.0));
        return BottomBand + NoseBand * Nose - BottomBand * 0.7f * Bridge;
    }

    private static int edgeStep(float Distance, float Width, int Most) {
        if (Width <= 0.0f || Distance >= Width) {
            return 0;
        }
        int Band = Mth.floor(Distance / Width * Steps);
        return Math.min(Most, Steps - Band);
    }

    private static float[] fold(int X, int Y, int Wide, int Tall, float[] Line) {
        float Px = (X + 0.5f - Wide / 2.0f) / Tall;
        float Py = (Y + 0.5f - Tall / 2.0f) / Tall;
        float StartX = Line[0] * (Wide / 2.0f / Tall + 0.02f);
        float Dx = -Line[0] * Line[3];
        float Dy = Line[2] - Line[1];
        float Length = Dx * Dx + Dy * Dy;
        float Along = ((Px - StartX) * Dx + (Py - Line[1]) * Dy) / Length;
        if (Along <= 0.0f || Along >= 1.0f) {
            return null;
        }
        float Across = ((Dx * (Py - Line[1]) - Dy * (Px - StartX)) / (float) Math.sqrt(Length) * Math.signum(Dx) - Line[4] * 4.0f * Along * (1.0f - Along)) * Tall;
        return new float[]{Across, 1.0f - Along};
    }

    private static float blockNoise(int Along, int Seed) {
        return hash(Math.floorDiv(Along, Block), Seed) * 2.0f - 1.0f;
    }

    private static float hash(int Value, int Seed) {
        int Mixed = Value * 374761393 + Seed * 668265263;
        Mixed = (Mixed ^ Mixed >>> 13) * 1274126177;
        Mixed ^= Mixed >>> 16;
        return (Mixed & 0xFFFF) / 65535.0f;
    }
}
