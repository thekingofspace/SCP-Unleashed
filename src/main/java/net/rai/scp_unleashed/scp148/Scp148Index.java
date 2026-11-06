package net.rai.scp_unleashed.scp148;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp148Index {
    private static final Map<Level, Long2ObjectOpenHashMap<LongSet>> Levels = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Level, Long2ObjectOpenHashMap<LongSet>> Powered = Collections.synchronizedMap(new WeakHashMap<>());

    private Scp148Index() {
    }

    public static void add(Level World, BlockPos Pos) {
        put(Levels, World, Pos);
    }

    public static void remove(Level World, BlockPos Pos) {
        take(Levels, World, Pos);
    }

    public static void addPowered(Level World, BlockPos Pos) {
        put(Powered, World, Pos);
    }

    public static void removePowered(Level World, BlockPos Pos) {
        take(Powered, World, Pos);
    }

    public static void clear(Level World) {
        Levels.remove(World);
        Powered.remove(World);
    }

    @Nullable
    public static BlockPos nearest(Level World, Vec3 Point, double Radius) {
        return search(Levels.get(World), Point, Radius);
    }

    @Nullable
    public static BlockPos nearestPowered(Level World, Vec3 Point, double Radius) {
        return search(Powered.get(World), Point, Radius);
    }

    public static boolean within(Level World, Vec3 Point, double Radius) {
        return nearest(World, Point, Radius) != null;
    }

    public static boolean withinPowered(Level World, Vec3 Point, double Radius) {
        return nearestPowered(World, Point, Radius) != null;
    }

    public static List<BlockPos> all(Level World) {
        return list(Levels.get(World));
    }

    public static List<BlockPos> allPowered(Level World) {
        return list(Powered.get(World));
    }

    public static int count(Level World) {
        return total(Levels.get(World));
    }

    public static int countPowered(Level World) {
        return total(Powered.get(World));
    }

    private static void put(Map<Level, Long2ObjectOpenHashMap<LongSet>> Index, Level World, BlockPos Pos) {
        Index.computeIfAbsent(World, Key -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(ChunkPos.asLong(Pos), Key -> new LongOpenHashSet())
                .add(Pos.asLong());
    }

    private static void take(Map<Level, Long2ObjectOpenHashMap<LongSet>> Index, Level World, BlockPos Pos) {
        Long2ObjectOpenHashMap<LongSet> Chunks = Index.get(World);
        if (Chunks == null) {
            return;
        }
        long Key = ChunkPos.asLong(Pos);
        LongSet Placed = Chunks.get(Key);
        if (Placed == null) {
            return;
        }
        Placed.remove(Pos.asLong());
        if (Placed.isEmpty()) {
            Chunks.remove(Key);
        }
    }

    @Nullable
    private static BlockPos search(@Nullable Long2ObjectOpenHashMap<LongSet> Chunks, Vec3 Point, double Radius) {
        if (Chunks == null || Chunks.isEmpty()) {
            return null;
        }
        int MinX = SectionPos.blockToSectionCoord(Point.x - Radius);
        int MaxX = SectionPos.blockToSectionCoord(Point.x + Radius);
        int MinZ = SectionPos.blockToSectionCoord(Point.z - Radius);
        int MaxZ = SectionPos.blockToSectionCoord(Point.z + Radius);
        Closest Nearest = new Closest(Point, Radius);
        if (Chunks.size() < (long) (MaxX - MinX + 1) * (MaxZ - MinZ + 1)) {
            for (Long2ObjectMap.Entry<LongSet> Entry : Chunks.long2ObjectEntrySet()) {
                int ChunkX = ChunkPos.getX(Entry.getLongKey());
                int ChunkZ = ChunkPos.getZ(Entry.getLongKey());
                if (ChunkX >= MinX && ChunkX <= MaxX && ChunkZ >= MinZ && ChunkZ <= MaxZ) {
                    Nearest.scan(Entry.getValue());
                }
            }
            return Nearest.found();
        }
        for (int ChunkX = MinX; ChunkX <= MaxX; ChunkX++) {
            for (int ChunkZ = MinZ; ChunkZ <= MaxZ; ChunkZ++) {
                LongSet Placed = Chunks.get(ChunkPos.asLong(ChunkX, ChunkZ));
                if (Placed != null) {
                    Nearest.scan(Placed);
                }
            }
        }
        return Nearest.found();
    }

    private static List<BlockPos> list(@Nullable Long2ObjectOpenHashMap<LongSet> Chunks) {
        List<BlockPos> Found = new ArrayList<>();
        if (Chunks == null) {
            return Found;
        }
        for (LongSet Placed : Chunks.values()) {
            for (long Packed : Placed) {
                Found.add(BlockPos.of(Packed));
            }
        }
        return Found;
    }

    private static int total(@Nullable Long2ObjectOpenHashMap<LongSet> Chunks) {
        if (Chunks == null) {
            return 0;
        }
        int Total = 0;
        for (LongSet Placed : Chunks.values()) {
            Total += Placed.size();
        }
        return Total;
    }

    private static final class Closest {
        private final Vec3 Point;
        private double Best;
        private BlockPos Found;

        private Closest(Vec3 Point, double Radius) {
            this.Point = Point;
            this.Best = Radius * Radius;
        }

        private void scan(LongSet Placed) {
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
        }

        @Nullable
        private BlockPos found() {
            return Found;
        }
    }
}
