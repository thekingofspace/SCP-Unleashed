package net.rai.scp_unleashed.scp173;

import net.minecraft.server.level.ServerLevel;

public final class Scp173Budget {
    public static final int PathsPerTick = 12;
    public static final int ClimbProbesPerTick = 2000;
    public static final int SeedsPerTick = 1;
    public static final int CloneChecksPerTick = 3;
    private static long Tick = Long.MIN_VALUE;
    private static int Paths;
    private static int Probes;
    private static int Clones;
    private static int Seeds;

    private Scp173Budget() {
    }

    private static void roll(ServerLevel World) {
        long Now = World.getServer().getTickCount();
        if (Now != Tick) {
            Tick = Now;
            Paths = PathsPerTick;
            Probes = ClimbProbesPerTick;
            Clones = CloneChecksPerTick;
            Seeds = SeedsPerTick;
        }
    }

    public static boolean takePath(ServerLevel World) {
        roll(World);
        if (Paths <= 0) {
            return false;
        }
        Paths--;
        return true;
    }

    public static boolean takeProbes(ServerLevel World, int Count) {
        roll(World);
        if (Probes < Count) {
            return false;
        }
        Probes -= Count;
        return true;
    }

    public static void refundProbes(int Count) {
        Probes += Math.max(0, Count);
    }

    public static boolean takeCloneCheck(ServerLevel World) {
        roll(World);
        if (Clones <= 0) {
            return false;
        }
        Clones--;
        return true;
    }

    public static boolean takeSeed(ServerLevel World) {
        roll(World);
        if (Seeds <= 0) {
            return false;
        }
        Seeds--;
        return true;
    }
}
