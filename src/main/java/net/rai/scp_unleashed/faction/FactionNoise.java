package net.rai.scp_unleashed.faction;

public final class FactionNoise {
    private FactionNoise() {
    }

    public static long mix(long Seed, int X, int Y, int Z) {
        long Hash = Seed;
        Hash ^= X * 0x9E3779B97F4A7C15L;
        Hash ^= Y * 0xC2B2AE3D27D4EB4FL;
        Hash ^= Z * 0x165667B19E3779F9L;
        Hash = (Hash ^ (Hash >>> 30)) * 0xBF58476D1CE4E5B9L;
        Hash = (Hash ^ (Hash >>> 27)) * 0x94D049BB133111EBL;
        return Hash ^ (Hash >>> 31);
    }

    public static double unit(long Seed, int X, int Y, int Z) {
        return (mix(Seed, X, Y, Z) >>> 11) * 0x1.0p-53;
    }
}
