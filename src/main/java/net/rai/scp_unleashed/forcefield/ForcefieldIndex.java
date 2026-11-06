package net.rai.scp_unleashed.forcefield;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ForcefieldIndex {
    private static final Map<Level, Set<BlockPos>> Raised = new WeakHashMap<>();

    private ForcefieldIndex() {
    }

    public static void add(Level World, BlockPos Pos) {
        Raised.computeIfAbsent(World, Owner -> new HashSet<>()).add(Pos.immutable());
    }

    public static void remove(Level World, BlockPos Pos) {
        Set<BlockPos> Found = Raised.get(World);
        if (Found != null) {
            Found.remove(Pos);
        }
    }

    public static boolean anyNear(Level World, Vec3 Centre, double Reach) {
        Set<BlockPos> Found = Raised.get(World);
        if (Found == null || Found.isEmpty()) {
            return false;
        }
        double Vertical = Reach + ForcefieldEmitterBlockEntity.MaxGap + 1.0;
        for (BlockPos Pos : Found) {
            if (Math.abs(Pos.getX() + 0.5 - Centre.x) <= Reach + 1.0 && Math.abs(Pos.getZ() + 0.5 - Centre.z) <= Reach + 1.0
                    && Centre.y >= Pos.getY() - Reach && Centre.y <= Pos.getY() + Vertical) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        Raised.clear();
    }
}
