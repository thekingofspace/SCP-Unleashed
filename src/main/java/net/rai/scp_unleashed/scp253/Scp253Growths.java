package net.rai.scp_unleashed.scp253;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public final class Scp253Growths {
    public static final int Head = 0;
    public static final int Body = 1;
    public static final int RightArm = 2;
    public static final int LeftArm = 3;
    public static final int RightLeg = 4;
    public static final int LeftLeg = 5;
    public static final int Parts = 6;
    public static final int West = 0;
    public static final int East = 1;
    public static final int Top = 2;
    public static final int Bottom = 3;
    public static final int North = 4;
    public static final int South = 5;
    public static final int Count = 24;
    public static final float FirstAt = 0.05f;
    public static final float Spread = 0.93f;
    public static final float Curve = 1.15f;
    public static final float GrowSpan = 0.1f;
    public static final float SwellFrom = 0.55f;
    public static final float Swell = 0.3f;
    public static final float Step = 0.25f;
    public static final float Sunk = 0.3f;
    public static final int Variants = 4;
    public static final int[] PartPicks = {Body, Body, Body, Head, Head, RightArm, RightArm, LeftArm, LeftArm, RightLeg, RightLeg, LeftLeg, LeftLeg};
    public static final int[][] Faces = {
            {West, East, Top, South},
            {North, North, South, South, West, East},
            {West, West, North, South},
            {East, East, North, South},
            {North, South, West},
            {North, South, East}};
    public static final float[] VLow = {0.2f, 0.15f, 0.35f, 0.35f, 0.15f, 0.15f};
    public static final float[] VHigh = {0.8f, 0.85f, 0.9f, 0.9f, 0.85f, 0.85f};
    public static final float[] PartScale = {0.85f, 1.0f, 0.8f, 0.8f, 0.9f, 0.9f};
    public static final float[][] Wide = {
            {-4.0f, -8.0f, -4.0f, 4.0f, 0.0f, 4.0f},
            {-4.0f, 0.0f, -2.0f, 4.0f, 12.0f, 2.0f},
            {-3.0f, -2.0f, -2.0f, 1.0f, 10.0f, 2.0f},
            {-1.0f, -2.0f, -2.0f, 3.0f, 10.0f, 2.0f},
            {-2.0f, 0.0f, -2.0f, 2.0f, 12.0f, 2.0f},
            {-2.0f, 0.0f, -2.0f, 2.0f, 12.0f, 2.0f}};
    public static final float[][] Slim = {
            Wide[Head],
            Wide[Body],
            {-2.0f, -2.0f, -2.0f, 1.0f, 10.0f, 2.0f},
            {-1.0f, -2.0f, -2.0f, 2.0f, 10.0f, 2.0f},
            Wide[RightLeg],
            Wide[LeftLeg]};

    private Scp253Growths() {
    }

    public record Piece(float Du, float Dv, float Width, float Height, float Depth) {
    }

    public record Growth(int Part, int Face, float U, float V, float Size, float AppearAt, int Variant, List<Piece> Pieces) {
    }

    private static final class Mixer {
        private long State;

        private Mixer(long Seed) {
            State = Seed;
        }

        private long next() {
            State += 0x9E3779B97F4A7C15L;
            long Value = State;
            Value = (Value ^ (Value >>> 30)) * 0xBF58476D1CE4E5B9L;
            Value = (Value ^ (Value >>> 27)) * 0x94D049BB133111EBL;
            return Value ^ (Value >>> 31);
        }

        private float unit() {
            return (next() >>> 40) * 0x1.0p-24f;
        }

        private int below(int Bound) {
            return (int) ((next() >>> 33) % Bound);
        }
    }

    public static float appearAt(int Index) {
        return FirstAt + Spread * (float) Math.pow(Index / (float) (Count - 1), Curve);
    }

    public static int count(float Progress) {
        int Found = 0;
        for (int Index = 0; Index < Count; Index++) {
            if (appearAt(Index) <= Progress) {
                Found++;
            }
        }
        return Found;
    }

    public static List<Growth> layout(long Seed) {
        Mixer Random = new Mixer(Seed);
        List<Growth> Found = new ArrayList<>(Count);
        for (int Index = 0; Index < Count; Index++) {
            int Part = PartPicks[Random.below(PartPicks.length)];
            int[] Allowed = Faces[Part];
            int Face = Allowed[Random.below(Allowed.length)];
            boolean Shown = Index == 0 || Index == 2 || Index == 6;
            if (Shown) {
                Part = Index == 2 ? LeftArm : RightArm;
                Face = Index == 6 ? North : Part == RightArm ? West : East;
            }
            float U = 0.18f + 0.64f * Random.unit();
            float V = Shown ? 0.62f + 0.26f * Random.unit() : VLow[Part] + (VHigh[Part] - VLow[Part]) * Random.unit();
            float Size = (1.3f + 1.5f * Random.unit()) * (0.8f + 0.45f * Index / (Count - 1)) * PartScale[Part];
            int Variant = Random.below(Variants);
            List<Piece> Pieces = new ArrayList<>(3);
            Pieces.add(new Piece(0.0f, 0.0f, 1.0f, 1.0f, 0.85f));
            Pieces.add(new Piece((Random.unit() - 0.5f) * 0.5f, (Random.unit() - 0.5f) * 0.5f, 0.45f, 0.45f, 1.35f));
            if (Random.unit() < 0.65f) {
                int Side = Random.below(4);
                float Distance = 0.55f + 0.25f * Random.unit();
                float Jitter = (Random.unit() - 0.5f) * 0.4f;
                float Du = Side == 0 ? Distance : Side == 1 ? -Distance : Jitter;
                float Dv = Side == 2 ? Distance : Side == 3 ? -Distance : Jitter;
                Pieces.add(new Piece(Du, Dv, 0.55f, 0.55f, 0.55f));
            }
            Found.add(new Growth(Part, Face, U, V, Size, appearAt(Index), Variant, List.copyOf(Pieces)));
        }
        return List.copyOf(Found);
    }

    public static float scaleOf(Growth Item, float Progress) {
        if (Progress < Item.AppearAt()) {
            return 0.0f;
        }
        float Grown = Mth.clamp((Progress - Item.AppearAt()) / GrowSpan, 0.0f, 1.0f);
        float Swollen = 1.0f + Swell * Mth.clamp((Progress - SwellFrom) / (1.0f - SwellFrom), 0.0f, 1.0f);
        float Raw = Item.Size() * (0.4f + 0.6f * Grown) * Swollen;
        return Math.max(Step, Math.round(Raw / Step) * Step);
    }

    public static int normalAxis(int Face) {
        return Face <= East ? 0 : Face <= Bottom ? 1 : 2;
    }

    public static int uAxis(int Face) {
        return Face <= East ? 2 : 0;
    }

    public static int vAxis(int Face) {
        return Face == Top || Face == Bottom ? 2 : 1;
    }

    public static float sign(int Face) {
        return Face == West || Face == Top || Face == North ? -1.0f : 1.0f;
    }

    public static float[] box(Growth Item, Piece Bit, float Scale, float[] Bounds, float Lift) {
        int Normal = normalAxis(Item.Face());
        int Across = uAxis(Item.Face());
        int Along = vAxis(Item.Face());
        float Sign = sign(Item.Face());
        float Surface = Sign < 0.0f ? Bounds[Normal] : Bounds[Normal + 3];
        float HalfU = Bit.Width() * Scale * 0.5f;
        float HalfV = Bit.Height() * Scale * 0.5f;
        float CentreU = fit(Bounds[Across] + Item.U() * (Bounds[Across + 3] - Bounds[Across]) + Bit.Du() * Scale, Bounds[Across], Bounds[Across + 3], HalfU);
        float CentreV = fit(Bounds[Along] + Item.V() * (Bounds[Along + 3] - Bounds[Along]) + Bit.Dv() * Scale, Bounds[Along], Bounds[Along + 3], HalfV);
        float Depth = Bit.Depth() * Scale;
        float Inner = Surface + Sign * (Lift - Depth * Sunk);
        float Outer = Surface + Sign * (Lift + Depth * (1.0f - Sunk));
        float[] Result = new float[6];
        Result[Normal] = Math.min(Inner, Outer);
        Result[Normal + 3] = Math.max(Inner, Outer);
        Result[Across] = CentreU - HalfU;
        Result[Across + 3] = CentreU + HalfU;
        Result[Along] = CentreV - HalfV;
        Result[Along + 3] = CentreV + HalfV;
        return Result;
    }

    private static float fit(float Centre, float Low, float High, float Half) {
        if (High - Low <= Half * 2.0f) {
            return (Low + High) * 0.5f;
        }
        return Mth.clamp(Centre, Low + Half, High - Half);
    }
}
