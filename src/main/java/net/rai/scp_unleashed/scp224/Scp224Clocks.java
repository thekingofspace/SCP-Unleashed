package net.rai.scp_unleashed.scp224;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp224Clocks {
    private static final Map<ResourceKey<Level>, Set<Scp224ClockBlockEntity>> Loaded = new HashMap<>();

    private Scp224Clocks() {
    }

    public static synchronized void add(Scp224ClockBlockEntity Clock) {
        Level World = Clock.getLevel();
        if (World == null || World.isClientSide) {
            return;
        }
        Loaded.computeIfAbsent(World.dimension(), Key -> new LinkedHashSet<>()).add(Clock);
    }

    public static synchronized void remove(Scp224ClockBlockEntity Clock) {
        for (Set<Scp224ClockBlockEntity> Each : Loaded.values()) {
            Each.remove(Clock);
        }
    }

    public static synchronized List<Scp224ClockBlockEntity> in(Level World) {
        Set<Scp224ClockBlockEntity> Found = Loaded.get(World.dimension());
        return Found == null ? List.of() : new ArrayList<>(Found);
    }

    public static synchronized boolean any(Level World) {
        Set<Scp224ClockBlockEntity> Found = Loaded.get(World.dimension());
        return Found != null && !Found.isEmpty();
    }

    public static void changed(Level World, BlockPos Pos) {
        if (World.isClientSide || !any(World)) {
            return;
        }
        for (Scp224ClockBlockEntity Clock : in(World)) {
            if (Clock.isRemoved()) {
                continue;
            }
            if (Clock.roomTouches(Pos)) {
                Clock.markRoomDirty();
            }
        }
    }

    public static Scp224ClockBlockEntity nearest(Level World, Vec3 From, double Radius) {
        Scp224ClockBlockEntity Best = null;
        double BestDistance = Radius * Radius;
        for (Scp224ClockBlockEntity Clock : in(World)) {
            if (Clock.isRemoved()) {
                continue;
            }
            double Distance = Vec3.atCenterOf(Clock.getBlockPos()).distanceToSqr(From);
            if (Distance <= BestDistance) {
                BestDistance = Distance;
                Best = Clock;
            }
        }
        return Best;
    }

    public static synchronized void clear() {
        Loaded.clear();
    }
}
