package net.rai.scp_unleashed.scp001day.plan;

public final class Scp001DayHash {
    private Scp001DayHash() {
    }

    public static long mix(long Value) {
        Value += 0x9E3779B97F4A7C15L;
        Value = (Value ^ (Value >>> 30)) * 0xBF58476D1CE4E5B9L;
        Value = (Value ^ (Value >>> 27)) * 0x94D049BB133111EBL;
        return Value ^ (Value >>> 31);
    }

    public static long mix(int A, int B, int C, int Salt) {
        long Value = mix(((long) A * 0x632BE59BD9B4E019L) ^ ((long) B * 0x85157AF5L) ^ ((long) C * 0x2545F4914F6CDD1DL) ^ ((long) Salt << 40));
        return mix(Value ^ Salt);
    }

    public static double unit(int A, int B, int C, int Salt) {
        return (mix(A, B, C, Salt) >>> 11) * 0x1.0p-53;
    }

    public static int range(int A, int B, int C, int Salt, int Low, int High) {
        return Low + (int) Math.floor(unit(A, B, C, Salt) * (High - Low + 1));
    }

    public static boolean chance(int A, int B, int C, int Salt, double Odds) {
        return unit(A, B, C, Salt) < Odds;
    }
}
