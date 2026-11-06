package net.rai.scp_unleashed.scp5001.mekhane;

import net.minecraft.world.phys.Vec3;

public final class MekhaneFlight {
    public static final double Drift = 14.0;
    public static final double[][] Keys = {
            {0, 0, 60, 0},
            {60, 18, 68, 16},
            {120, 25, 76, -6},
            {180, 8, 84, -24},
            {240, -18, 88, -16},
            {300, -24, 92, 8},
            {360, -30, 96, 34},
            {430, 10, 104, 44},
            {500, 44, 96, 20},
            {560, 40, 84, -14},
            {620, 20, 76, -34},
            {700, -16, 86, -40},
            {760, -62, 96, -20},
            {820, -64, 108, 34},
            {880, -18, 118, 70},
            {940, 40, 128, 58},
            {1000, 70, 134, 0},
            {1060, 44, 140, -52},
            {1100, 0, 142, -66},
            {1160, -20, 110, -40},
            {1220, -36, 86, -6},
            {1300, -20, 90, 30},
            {1380, 18, 100, 40},
            {1440, 40, 108, 14},
            {1500, 34, 112, -14},
            {1530, 26, 94, -8},
            {1560, 14, 50, 0},
            {1590, 8, 20, 4},
            {1630, 6, 24, 6},
            {1700, 0, 56, 0},
            {1800, -8, 96, -10},
            {1950, -4, 88, 6},
            {2100, 6, 82, 10},
            {2250, 8, 86, 0},
            {2350, 2, 96, -6},
            {2400, -4, 102, -10},
            {2480, 0, 124, -4},
            {2560, 0, 136, 0},
            {2640, 10, 138, 8},
            {2720, 14, 141, -6},
            {2800, 2, 143, -15},
            {2880, -12, 141, -7},
            {2960, -11, 138, 8},
            {3060, 0, 136, 10},
            {3160, 5, 131, 4},
            {3240, 0, 128, 0},
            {3300, 0, 132, 0},
            {3560, 0, 132, 0},
    };

    private MekhaneFlight() {
    }

    private static double[] key(int Index) {
        return Keys[Math.max(0, Math.min(Keys.length - 1, Index))];
    }

    private static double slope(int Index, int Axis) {
        if (Index <= 0 || Index >= Keys.length - 1) {
            return 0.0;
        }
        double[] Before = key(Index - 1);
        double[] After = key(Index + 1);
        double Span = After[0] - Before[0];
        if (Span <= 0.0) {
            return 0.0;
        }
        return (After[Axis] - Before[Axis]) / Span;
    }

    private static double hermite(double P0, double M0, double P1, double M1, double Span, double T) {
        double T2 = T * T;
        double T3 = T2 * T;
        return (2.0 * T3 - 3.0 * T2 + 1.0) * P0 + (T3 - 2.0 * T2 + T) * Span * M0 + (-2.0 * T3 + 3.0 * T2) * P1 + (T3 - T2) * Span * M1;
    }

    public static Vec3 path(double Tick) {
        double Time = Math.max(0.0, Math.min(Keys[Keys.length - 1][0], Tick));
        int Index = 0;
        while (Index < Keys.length - 2 && Keys[Index + 1][0] <= Time) {
            Index++;
        }
        double[] B = key(Index);
        double[] C = key(Index + 1);
        double Span = Math.max(1.0, C[0] - B[0]);
        double Share = Math.max(0.0, Math.min(1.0, (Time - B[0]) / Span));
        double X = hermite(B[1], slope(Index, 1), C[1], slope(Index + 1, 1), Span, Share);
        double Y = hermite(B[2], slope(Index, 2), C[2], slope(Index + 1, 2), Span, Share);
        double Z = hermite(B[3], slope(Index, 3), C[3], slope(Index + 1, 3), Span, Share);
        double Fade = MekhaneTimeline.smooth((float) Math.min(1.0, Time / 200.0));
        X += Drift * Math.sin(Time / 860.0) * Fade;
        Z += Drift * (Math.cos(Time / 640.0) - 1.0) * Fade;
        Y += MekhaneTimeline.AscendHeight * MekhaneTimeline.rise((int) Time);
        return new Vec3(X, Y, Z);
    }

    public static Vec3 velocity(double Tick) {
        return path(Tick + 1.0).subtract(path(Tick - 1.0)).scale(0.5);
    }

    public static Vec3 acceleration(double Tick) {
        return path(Tick + 2.0).subtract(path(Tick).scale(2.0)).add(path(Tick - 2.0)).scale(0.25);
    }
}
