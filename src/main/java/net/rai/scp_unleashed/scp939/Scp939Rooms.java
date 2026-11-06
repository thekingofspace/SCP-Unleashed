package net.rai.scp_unleashed.scp939;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp465.Scp465Room;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp939Rooms {
    public static final int SpanAcross = 32;
    public static final int SpanUp = 12;
    public static final int KeepTicks = 100;
    public static final int RefuseTicks = 200;
    public static final int RefuseCell = 4;
    public static final int MaxKept = 64;
    private static final Map<ResourceKey<Level>, List<Room>> Kept = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long2LongOpenHashMap> Refused = new HashMap<>();

    public record Room(LongOpenHashSet Cells, BlockPos Low, BlockPos High, long Made) {
        public boolean holds(BlockPos Pos) {
            return Cells.contains(Pos.asLong());
        }

        public AABB box() {
            return new AABB(Low.getX(), Low.getY(), Low.getZ(), High.getX() + 1, High.getY() + 1, High.getZ() + 1);
        }

        public int width() {
            return High.getX() - Low.getX() + 1;
        }

        public int depth() {
            return High.getZ() - Low.getZ() + 1;
        }

        public int height() {
            return High.getY() - Low.getY() + 1;
        }
    }

    private Scp939Rooms() {
    }

    public static boolean accepted(Scp465Room.Verdict Result) {
        return Result == Scp465Room.Verdict.Fits || Result == Scp465Room.Verdict.TooSmall;
    }

    public static Scp465Room.Scan scan(Level World, BlockPos Start) {
        return Scp465Room.scan(World, Start, SpanAcross, SpanUp);
    }

    public static Optional<Room> roomAt(ServerLevel World, BlockPos Start) {
        long Now = World.getGameTime();
        List<Room> Rooms = Kept.computeIfAbsent(World.dimension(), Key -> new ArrayList<>());
        Iterator<Room> Each = Rooms.iterator();
        while (Each.hasNext()) {
            Room Old = Each.next();
            if (Now - Old.Made() > KeepTicks || Now < Old.Made()) {
                Each.remove();
                continue;
            }
            if (Old.Cells().contains(Start.asLong())) {
                return Optional.of(Old);
            }
        }
        Long2LongOpenHashMap Misses = Refused.computeIfAbsent(World.dimension(), Key -> new Long2LongOpenHashMap());
        long Cell = BlockPos.asLong(Math.floorDiv(Start.getX(), RefuseCell), Math.floorDiv(Start.getY(), RefuseCell), Math.floorDiv(Start.getZ(), RefuseCell));
        if (Misses.containsKey(Cell) && Now - Misses.get(Cell) <= RefuseTicks && Now >= Misses.get(Cell)) {
            return Optional.empty();
        }
        Optional<Room> Found = fresh(World, Start);
        if (Found.isEmpty()) {
            if (Misses.size() > 4096) {
                Misses.clear();
            }
            Misses.put(Cell, Now);
            return Found;
        }
        if (Rooms.size() >= MaxKept) {
            Rooms.remove(0);
        }
        Rooms.add(Found.get());
        return Found;
    }

    public static Optional<Room> fresh(ServerLevel World, BlockPos Start) {
        if (!World.isLoaded(Start) || Scp465Room.solidCell(World, Start)) {
            return Optional.empty();
        }
        Scp465Room.Scan Result = scan(World, Start);
        if (!accepted(Result.Result()) || Result.Cells().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Room(Result.Cells(), Result.Low(), Result.High(), World.getGameTime()));
    }

    public static void forget(ServerLevel World) {
        Kept.remove(World.dimension());
        Refused.remove(World.dimension());
    }

    public static void forgetAll() {
        Kept.clear();
        Refused.clear();
    }

    public static void forgetNear(ServerLevel World, BlockPos Pos) {
        List<Room> Rooms = Kept.get(World.dimension());
        if (Rooms != null) {
            Rooms.removeIf(Each -> Each.box().inflate(1.0).contains(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5));
        }
        Long2LongOpenHashMap Misses = Refused.get(World.dimension());
        if (Misses != null) {
            Misses.clear();
        }
    }

    public static List<Scp939> members(ServerLevel World, Room Found) {
        return World.getEntitiesOfClass(Scp939.class, Found.box().inflate(0.5), Each -> Each.isAlive() && Found.holds(Each.blockPosition()));
    }

    public static float sources(ServerLevel World, Room Found) {
        float Total = 0.0f;
        for (Scp939 Each : members(World, Found)) {
            Total += Each.isBaby() ? 0.5f : 1.0f;
        }
        return Total;
    }

    public static float haze(ServerLevel World, Room Found) {
        float Best = 0.0f;
        for (Scp939 Each : members(World, Found)) {
            Best = Math.max(Best, Each.haze());
        }
        return Best;
    }

    public static void scrub(ServerLevel World, Room Found, float Amount) {
        for (Scp939 Each : members(World, Found)) {
            Each.scrub(Amount);
        }
    }

    @Nullable
    public static BlockPos openNeighbour(ServerLevel World, BlockPos Pos) {
        if (!Scp465Room.solidCell(World, Pos.above())) {
            return Pos.above();
        }
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (!Scp465Room.solidCell(World, Pos.relative(Side))) {
                return Pos.relative(Side);
            }
        }
        return null;
    }

    public static Optional<Room> roomAround(ServerLevel World, BlockPos Pos) {
        BlockPos Open = openNeighbour(World, Pos);
        return Open == null ? Optional.empty() : roomAt(World, Open);
    }
}
