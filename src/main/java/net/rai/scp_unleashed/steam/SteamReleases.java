package net.rai.scp_unleashed.steam;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SteamReleases {
    public static final int UnitsPerWisp = 40;
    public static final int MaxWisps = 6;
    public static final double Spread = 0.12;
    public static final double Drift = 0.025;

    private SteamReleases() {
    }

    public static void lowPressure(ServerLevel World, Vec3 Point, int Amount) {
        int Count = Mth.clamp(Amount / UnitsPerWisp + 1, 1, MaxWisps);
        World.sendParticles(SteamRegistry.SteamPlume.get(), Point.x, Point.y, Point.z, Count, Spread, Spread * 0.6, Spread, Drift);
    }
}
