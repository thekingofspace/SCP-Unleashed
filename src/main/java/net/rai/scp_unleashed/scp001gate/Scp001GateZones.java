package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp001GateZones {
    public static final double WarnRadius = 50.0;
    public static final double KillRadius = 45.0;
    public static final double FenceRadius = 48.0;
    public static final double AvoidRadius = 55.0;
    public static final double TeleportMargin = 4.0;
    public static final int Below = 24;
    public static final int Above = 160;
    public static final int Tiers = 4;
    private static final Map<ResourceKey<Level>, Map<Integer, BlockPos>> Active = new ConcurrentHashMap<>();

    private Scp001GateZones() {
    }

    public static void mark(Level World, int Id, BlockPos Centre) {
        Active.computeIfAbsent(World.dimension(), Key -> new ConcurrentHashMap<>()).put(Id, Centre.immutable());
    }

    public static void clear(Level World, int Id) {
        Map<Integer, BlockPos> Found = Active.get(World.dimension());
        if (Found != null) {
            Found.remove(Id);
        }
    }

    public static void reset() {
        Active.clear();
    }

    public static List<BlockPos> gates(Level World) {
        List<BlockPos> Found = new ArrayList<>();
        Map<Integer, BlockPos> Live = Active.get(World.dimension());
        if (Live != null) {
            Found.addAll(Live.values());
        }
        if (World instanceof ServerLevel Server && World.dimension() == Level.OVERWORLD) {
            Scp001GateSites Site = Scp001GateSites.get(Server.getServer());
            if (Site.planned() && !Found.contains(Site.centre())) {
                Found.add(Site.centre());
            }
        }
        return Found;
    }

    public static boolean live(Level World) {
        Map<Integer, BlockPos> Found = Active.get(World.dimension());
        return Found != null && !Found.isEmpty();
    }

    public static Optional<BlockPos> liveNearest(Level World, Vec3 Pos, double Radius) {
        Map<Integer, BlockPos> Found = Active.get(World.dimension());
        if (Found == null || Found.isEmpty()) {
            return Optional.empty();
        }
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Centre : Found.values()) {
            if (!band(Centre, Pos.y)) {
                continue;
            }
            double Distance = horizontal(Centre, Pos.x, Pos.z);
            if (Distance <= Radius && Distance < BestDistance) {
                Best = Centre;
                BestDistance = Distance;
            }
        }
        return Optional.ofNullable(Best);
    }

    public static double horizontal(BlockPos Centre, double X, double Z) {
        double Dx = X - (Centre.getX() + 0.5);
        double Dz = Z - (Centre.getZ() + 0.5);
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public static boolean band(BlockPos Centre, double Y) {
        return Y >= Centre.getY() - Below && Y <= Centre.getY() + Above;
    }

    public static boolean within(BlockPos Centre, Vec3 Pos, double Radius) {
        return band(Centre, Pos.y) && horizontal(Centre, Pos.x, Pos.z) <= Radius;
    }

    public static boolean inWarn(BlockPos Centre, Vec3 Pos) {
        return within(Centre, Pos, WarnRadius);
    }

    public static boolean inKill(BlockPos Centre, Vec3 Pos) {
        return within(Centre, Pos, KillRadius);
    }

    public static Optional<BlockPos> nearest(Level World, Vec3 Pos, double Radius) {
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Centre : gates(World)) {
            if (!band(Centre, Pos.y)) {
                continue;
            }
            double Distance = horizontal(Centre, Pos.x, Pos.z);
            if (Distance <= Radius && Distance < BestDistance) {
                Best = Centre;
                BestDistance = Distance;
            }
        }
        return Optional.ofNullable(Best);
    }

    public static boolean killing(Level World, Vec3 Pos) {
        return nearest(World, Pos, KillRadius).isPresent();
    }

    public static boolean forbidden(Level World, Vec3 Pos) {
        return nearest(World, Pos, KillRadius + TeleportMargin).isPresent();
    }

    public static int tier(double Distance) {
        if (Distance > WarnRadius) {
            return -1;
        }
        double Share = (WarnRadius - Distance) / (WarnRadius - KillRadius);
        return Math.max(0, Math.min(Tiers - 1, (int) Math.floor(Share * Tiers)));
    }
}
