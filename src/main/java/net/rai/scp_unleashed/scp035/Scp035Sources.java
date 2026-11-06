package net.rai.scp_unleashed.scp035;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class Scp035Sources {
    public static final int RefreshInterval = 10;
    public static final int StaleAfter = 45;
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> Windows = new HashMap<>();

    private Scp035Sources() {
    }

    public static void mark(ServerLevel World, BlockPos Pos) {
        Windows.computeIfAbsent(World.dimension(), Key -> new HashMap<>()).put(Pos.immutable(), World.getGameTime());
    }

    public static void forget(ServerLevel World, BlockPos Pos) {
        Map<BlockPos, Long> Found = Windows.get(World.dimension());
        if (Found != null) {
            Found.remove(Pos);
        }
    }

    public static List<BlockPos> near(ServerLevel World, Vec3 Centre, double Range) {
        Map<BlockPos, Long> Found = Windows.get(World.dimension());
        if (Found == null || Found.isEmpty()) {
            return List.of();
        }
        long Now = World.getGameTime();
        double Reach = (Range + 2.0) * (Range + 2.0);
        List<BlockPos> Result = new ArrayList<>();
        Iterator<Map.Entry<BlockPos, Long>> Entries = Found.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<BlockPos, Long> Entry = Entries.next();
            if (Now - Entry.getValue() > StaleAfter || Entry.getValue() > Now) {
                Entries.remove();
                continue;
            }
            if (Vec3.atCenterOf(Entry.getKey()).distanceToSqr(Centre) <= Reach) {
                Result.add(Entry.getKey());
            }
        }
        return Result;
    }

    public static void clear() {
        Windows.clear();
    }
}
