package net.rai.scp_unleashed.scp3008.plan;

public final class Scp3008Hash {
    public static final long Salt = 0x3008_5CA1_AB1E_0001L;

    private Scp3008Hash() {
    }

    public static int mix(int A, int B, int C) {
        long Value = A * 0x9E3779B97F4A7C15L ^ B * 0xC2B2AE3D27D4EB4FL ^ C * 0x165667B19E3779F9L ^ Salt;
        Value ^= Value >>> 33;
        Value *= 0xFF51AFD7ED558CCDL;
        Value ^= Value >>> 33;
        Value *= 0xC4CEB9FE1A85EC53L;
        Value ^= Value >>> 33;
        return (int) Value;
    }

    public static int mix(int A, int B, int C, int D) {
        return mix(mix(A, B, C), D, 0x51ED27);
    }

    public static int pick(int Hash, int Bound) {
        return Math.floorMod(Hash, Bound);
    }

    public static double unit(int Hash) {
        return (Hash >>> 8) / 16777216.0;
    }

    public static boolean chance(int Hash, double Share) {
        return unit(Hash) < Share;
    }

    private static double smooth(double Value) {
        return Value * Value * (3.0 - 2.0 * Value);
    }

    public static double noise(double X, double Z, int Channel) {
        int Gx = (int) Math.floor(X);
        int Gz = (int) Math.floor(Z);
        double Fx = smooth(X - Gx);
        double Fz = smooth(Z - Gz);
        double A = unit(mix(Gx, Gz, Channel));
        double B = unit(mix(Gx + 1, Gz, Channel));
        double C = unit(mix(Gx, Gz + 1, Channel));
        double D = unit(mix(Gx + 1, Gz + 1, Channel));
        double Top = A + (B - A) * Fx;
        double Bottom = C + (D - C) * Fx;
        return Top + (Bottom - Top) * Fz;
    }
}
