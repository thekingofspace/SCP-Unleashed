package net.rai.scp_unleashed.scp472;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class Scp472Stones {
    private static final Map<Level, Set<BlockPos>> Placed = Collections.synchronizedMap(new WeakHashMap<>());

    private Scp472Stones() {
    }

    public record Nearest(BlockPos Pos, double Distance) {
        public Vec3 centre() {
            return Vec3.atCenterOf(Pos);
        }
    }

    public static void add(Level World, BlockPos Pos) {
        Placed.computeIfAbsent(World, Key -> new LinkedHashSet<>()).add(Pos.immutable());
    }

    public static void remove(Level World, BlockPos Pos) {
        Set<BlockPos> Known = Placed.get(World);
        if (Known != null) {
            Known.remove(Pos);
            if (Known.isEmpty()) {
                Placed.remove(World);
            }
        }
    }

    public static List<BlockPos> of(Level World) {
        Set<BlockPos> Known = Placed.get(World);
        return Known == null ? List.of() : List.copyOf(Known);
    }

    public static boolean any(Level World) {
        Set<BlockPos> Known = Placed.get(World);
        return Known != null && !Known.isEmpty();
    }

    @Nullable
    public static Nearest nearest(Level World, Vec3 From, double Within) {
        return nearest(World, of(World), From, Within);
    }

    @Nullable
    public static Nearest nearest(Level World, List<BlockPos> Stones, Vec3 From, double Within) {
        BlockPos Best = null;
        double BestSquare = Within * Within;
        for (BlockPos Pos : Stones) {
            double Square = Vec3.atCenterOf(Pos).distanceToSqr(From);
            if (Square <= BestSquare && World.isLoaded(Pos) && World.getBlockState(Pos).getBlock() instanceof Scp472BloodstoneBlock) {
                Best = Pos;
                BestSquare = Square;
            }
        }
        return Best == null ? null : new Nearest(Best, Math.sqrt(BestSquare));
    }
}
