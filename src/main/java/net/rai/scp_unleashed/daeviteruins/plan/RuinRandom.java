package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.List;

public final class RuinRandom {
    private final long Seed;
    private long State;

    public RuinRandom(long Seed) {
        this.Seed = mix(Seed);
        this.State = this.Seed;
    }

    public long seed() {
        return Seed;
    }

    public RuinRandom fork(long Salt) {
        return new RuinRandom(Seed ^ mix(Salt * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L));
    }

    public static long mix(long Value) {
        long Z = Value + 0x9E3779B97F4A7C15L;
        Z = (Z ^ (Z >>> 30)) * 0xBF58476D1CE4E5B9L;
        Z = (Z ^ (Z >>> 27)) * 0x94D049BB133111EBL;
        return Z ^ (Z >>> 31);
    }

    public long nextLong() {
        State += 0x9E3779B97F4A7C15L;
        long Z = State;
        Z = (Z ^ (Z >>> 30)) * 0xBF58476D1CE4E5B9L;
        Z = (Z ^ (Z >>> 27)) * 0x94D049BB133111EBL;
        return Z ^ (Z >>> 31);
    }

    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    public int nextInt(int Bound) {
        if (Bound <= 1) {
            return 0;
        }
        return (int) Math.floorMod(nextLong(), (long) Bound);
    }

    public int range(int Low, int High) {
        return Low + nextInt(Math.max(1, High - Low + 1));
    }

    public boolean chance(double Probability) {
        return nextDouble() < Probability;
    }

    public <T> T pick(List<T> Options) {
        return Options.get(nextInt(Options.size()));
    }

    public int pick(int... Options) {
        return Options[nextInt(Options.length)];
    }

    public double at(int X, int Y, int Z) {
        long Hash = mix(Seed ^ mix(X * 0x1F1F1F1FL ^ mix(Y * 0x2E2E2E2EL ^ mix(Z * 0x3D3D3D3DL))));
        return (Hash >>> 11) * 0x1.0p-53;
    }

    public double at(int X, int Y, int Z, int Salt) {
        long Hash = mix(Seed ^ Salt * 0x51ED27B3L ^ mix(X * 0x1F1F1F1FL ^ mix(Y * 0x2E2E2E2EL ^ mix(Z * 0x3D3D3D3DL))));
        return (Hash >>> 11) * 0x1.0p-53;
    }

    public double smooth(double X, double Z, double Scale, int Salt) {
        double Fx = X / Scale;
        double Fz = Z / Scale;
        int X0 = (int) Math.floor(Fx);
        int Z0 = (int) Math.floor(Fz);
        double Tx = Fx - X0;
        double Tz = Fz - Z0;
        Tx = Tx * Tx * (3 - 2 * Tx);
        Tz = Tz * Tz * (3 - 2 * Tz);
        double A = at(X0, Salt, Z0, Salt);
        double B = at(X0 + 1, Salt, Z0, Salt);
        double C = at(X0, Salt, Z0 + 1, Salt);
        double D = at(X0 + 1, Salt, Z0 + 1, Salt);
        double Top = A + (B - A) * Tx;
        double Bottom = C + (D - C) * Tx;
        return Top + (Bottom - Top) * Tz;
    }

    public double smooth3(double X, double Y, double Z, double Scale, int Salt) {
        double Low = smooth(X, Z + Math.floor(Y / Scale) * 131.0, Scale, Salt);
        double High = smooth(X, Z + (Math.floor(Y / Scale) + 1) * 131.0, Scale, Salt);
        double T = Y / Scale - Math.floor(Y / Scale);
        T = T * T * (3 - 2 * T);
        return Low + (High - Low) * T;
    }
}
