package net.rai.scp_unleashed.scp610;

import net.minecraft.server.MinecraftServer;

public final class Scp610Limits {
    public static final int RadiusCap = 64;
    public static final double BiomassBase = 500.0;
    public static final double BiomassPerFlesh = 0.25;
    public static final double BiomassCeiling = 1.0e9;
    public static final int FrontierDepth = 24;
    private static volatile boolean Unlimited;

    private Scp610Limits() {
    }

    public static void refresh(MinecraftServer Server) {
        Unlimited = Scp610Rules.unlimited(Server);
    }

    public static boolean unlimited() {
        return Unlimited;
    }

    public static int fleshCap() {
        return Unlimited ? Integer.MAX_VALUE : Scp610Config.maxFleshPerHive();
    }

    public static int chamberCap() {
        return Unlimited ? Integer.MAX_VALUE : Scp610Config.maxChamberBlocksPerHive();
    }

    public static int radiusCap() {
        return Unlimited ? Integer.MAX_VALUE : RadiusCap;
    }

    public static double biomassCap(int Flesh) {
        return Unlimited ? BiomassCeiling : BiomassBase + Flesh * BiomassPerFlesh;
    }
}
