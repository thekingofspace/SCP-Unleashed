package net.rai.scp_unleashed.cooler;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class CoolerIndex {
    private static final Map<Level, LongSet> Powered = Collections.synchronizedMap(new WeakHashMap<>());

    private CoolerIndex() {
    }

    public static void add(Level World, BlockPos Pos) {
        synchronized (Powered) {
            Powered.computeIfAbsent(World, Key -> new LongOpenHashSet()).add(Pos.asLong());
        }
    }

    public static void remove(Level World, BlockPos Pos) {
        synchronized (Powered) {
            LongSet Placed = Powered.get(World);
            if (Placed != null) {
                Placed.remove(Pos.asLong());
                if (Placed.isEmpty()) {
                    Powered.remove(World);
                }
            }
        }
    }

    public static void clear(Level World) {
        Powered.remove(World);
    }

    @Nullable
    public static BlockPos nearest(Level World, Vec3 Point, double Radius) {
        synchronized (Powered) {
            LongSet Placed = Powered.get(World);
            if (Placed == null) {
                return null;
            }
            double Best = Radius * Radius;
            BlockPos Found = null;
            for (long Packed : Placed) {
                double Dx = BlockPos.getX(Packed) + 0.5 - Point.x;
                double Dy = BlockPos.getY(Packed) + 0.5 - Point.y;
                double Dz = BlockPos.getZ(Packed) + 0.5 - Point.z;
                double Distance = Dx * Dx + Dy * Dy + Dz * Dz;
                if (Distance <= Best) {
                    Best = Distance;
                    Found = BlockPos.of(Packed);
                }
            }
            return Found;
        }
    }

    public static boolean within(Level World, Vec3 Point, double Radius) {
        return nearest(World, Point, Radius) != null;
    }

    public static boolean cooling(Level World, Vec3 Point) {
        for (BlockPos Cooler : all(World)) {
            if (Vec3.atCenterOf(Cooler).distanceToSqr(Point) > CoolerBlockEntity.Radius * CoolerBlockEntity.Radius) {
                continue;
            }
            double Reach = CoolerBlockEntity.effectiveRadius(World, Cooler);
            if (Vec3.atCenterOf(Cooler).distanceToSqr(Point) <= Reach * Reach) {
                return true;
            }
        }
        return false;
    }

    public static List<BlockPos> all(Level World) {
        synchronized (Powered) {
            LongSet Placed = Powered.get(World);
            List<BlockPos> Found = new ArrayList<>();
            if (Placed != null) {
                for (long Packed : Placed) {
                    Found.add(BlockPos.of(Packed));
                }
            }
            return Found;
        }
    }
}
