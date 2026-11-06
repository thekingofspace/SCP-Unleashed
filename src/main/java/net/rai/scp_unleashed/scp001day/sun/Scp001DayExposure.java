package net.rai.scp_unleashed.scp001day.sun;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class Scp001DayExposure {
    public static final float[] Heights = {0.05f, 0.3f, 0.55f, 0.8f, 0.96f};
    public static final float ShoulderHeight = 0.62f;
    public static final float ShoulderSpread = 0.9f;
    public static final int CacheLimit = 65536;
    public static final double Snap = 8.0;

    private static final Long2FloatOpenHashMap Cache = new Long2FloatOpenHashMap();
    private static Level CacheLevel;
    private static long CacheEpoch = Long.MIN_VALUE;
    private static long Traced;

    private Scp001DayExposure() {
    }

    public static int skyY(Level World) {
        return World.getMaxBuildHeight();
    }

    public static long traced() {
        return Traced;
    }

    public static double point(Level World, Scp001DayFarField Far, double X, double Y, double Z, SunVector Sun) {
        Traced++;
        return Scp001DayRay.transmit(new Scp001DayLevelField(World), Far, X, Y, Z, Sun, skyY(World));
    }

    public static float body(Level World, Scp001DayFarField Far, Entity Body, SunVector Sun) {
        double[] Points = new double[Samples];
        samples(World, Far, Body, Sun, Points);
        double Total = 0.0;
        for (double Value : Points) {
            Total += Value;
        }
        return (float) (Total / Samples);
    }

    public static final int Samples = 7;

    public static void samples(Level World, Scp001DayFarField Far, Entity Body, SunVector Sun, double[] Out) {
        Scp001DayLevelField Cells = new Scp001DayLevelField(World);
        int Sky = skyY(World);
        AABB Box = Body.getBoundingBox();
        double CentreX = (Box.minX + Box.maxX) * 0.5;
        double CentreZ = (Box.minZ + Box.maxZ) * 0.5;
        double Height = Box.getYsize();
        int Index = 0;
        for (float Share : Heights) {
            Out[Index++] = Scp001DayRay.transmit(Cells, Far, CentreX, Box.minY + Height * Share, CentreZ, Sun, Sky);
        }
        double Flat = Sun.horizontal();
        double SideX = Flat > 1.0e-6 ? -Sun.Z() / Flat : 1.0;
        double SideZ = Flat > 1.0e-6 ? Sun.X() / Flat : 0.0;
        double Half = Box.getXsize() * 0.5 * ShoulderSpread;
        for (int Side = -1; Side <= 1; Side += 2) {
            Out[Index++] = Scp001DayRay.transmit(Cells, Far, CentreX + SideX * Half * Side, Box.minY + Height * ShoulderHeight, CentreZ + SideZ * Half * Side, Sun, Sky);
        }
        Traced += Samples;
    }

    public static synchronized float cached(Level World, Scp001DayFarField Far, double X, double Y, double Z, long DayTime) {
        long Epoch = Scp001DaySun.epoch(DayTime);
        if (CacheLevel != World || CacheEpoch != Epoch || Cache.size() > CacheLimit) {
            Cache.clear();
            CacheLevel = World;
            CacheEpoch = Epoch;
        }
        long Qx = (long) Math.floor(X * Snap);
        long Qy = (long) Math.floor(Y * Snap);
        long Qz = (long) Math.floor(Z * Snap);
        long Key = ((Qx & 0x3FFFFFFL) << 38) | ((Qz & 0x3FFFFFFL) << 12) | (Qy & 0xFFFL);
        if (Cache.containsKey(Key)) {
            return Cache.get(Key);
        }
        float Value = (float) point(World, Far, (Qx + 0.5) / Snap, (Qy + 0.5) / Snap, (Qz + 0.5) / Snap, Scp001DaySun.direction(DayTime));
        Cache.put(Key, Value);
        return Value;
    }

    public static synchronized void forget() {
        Cache.clear();
        CacheLevel = null;
        CacheEpoch = Long.MIN_VALUE;
    }
}
