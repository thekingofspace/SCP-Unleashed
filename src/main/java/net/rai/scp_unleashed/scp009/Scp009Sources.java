package net.rai.scp_unleashed.scp009;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp009Sources {
    public static final int Phase = 11;
    public static final int MaxSightChecks = 48;
    public static final int NearestTried = 2;
    public static final int ScansPerTick = 16;
    private static final Map<ResourceKey<Level>, Map<Long, LongOpenHashSet>> RedIce = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<Long, List<Vec3>>> Loose = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<UUID>> Carriers = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> Pending = new ConcurrentHashMap<>();

    private Scp009Sources() {
    }

    private static long chunkKey(double X, double Z) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(X), SectionPos.blockToSectionCoord(Z));
    }

    public static void offer(ServerLevel World, Vec3 Point) {
        Loose.computeIfAbsent(World.dimension(), Key -> new HashMap<>()).computeIfAbsent(chunkKey(Point.x, Point.z), Key -> new ArrayList<>()).add(Point);
    }

    public static void carrier(LivingEntity Bearer) {
        if (Bearer.level() instanceof ServerLevel World) {
            Carriers.computeIfAbsent(World.dimension(), Key -> new HashSet<>()).add(Bearer.getUUID());
        }
    }

    public static void placed(ServerLevel World, BlockPos Pos) {
        RedIce.computeIfAbsent(World.dimension(), Key -> new HashMap<>()).computeIfAbsent(ChunkPos.asLong(Pos), Key -> new LongOpenHashSet()).add(Pos.asLong());
    }

    public static void removed(ServerLevel World, BlockPos Pos) {
        Map<Long, LongOpenHashSet> Chunks = RedIce.get(World.dimension());
        if (Chunks == null) {
            return;
        }
        long Key = ChunkPos.asLong(Pos);
        LongOpenHashSet Blocks = Chunks.get(Key);
        if (Blocks != null && Blocks.remove(Pos.asLong()) && Blocks.isEmpty()) {
            Chunks.remove(Key);
        }
    }

    public static void chunkLoaded(LevelAccessor World, ChunkPos Pos) {
        if (World instanceof ServerLevel Server) {
            Pending.computeIfAbsent(Server.dimension(), Key -> ConcurrentHashMap.newKeySet()).add(Pos.toLong());
        }
    }

    public static void chunkUnloaded(LevelAccessor World, ChunkPos Pos) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        Map<Long, LongOpenHashSet> Chunks = RedIce.get(Server.dimension());
        if (Chunks != null) {
            Chunks.remove(Pos.toLong());
        }
        Set<Long> Waiting = Pending.get(Server.dimension());
        if (Waiting != null) {
            Waiting.remove(Pos.toLong());
        }
    }

    public static int redIceCount(ServerLevel World) {
        Map<Long, LongOpenHashSet> Chunks = RedIce.get(World.dimension());
        int Total = 0;
        if (Chunks != null) {
            for (LongOpenHashSet Blocks : Chunks.values()) {
                Total += Blocks.size();
            }
        }
        return Total;
    }

    public static void clear() {
        RedIce.clear();
        Loose.clear();
        Carriers.clear();
        Pending.clear();
    }

    public static void tick(ServerLevel World) {
        scanPending(World);
        if (Math.floorMod(World.getGameTime() + Phase, (long) Scp009Exposure.Interval) != 0L) {
            return;
        }
        collectCarriers(World);
        Map<Long, LongOpenHashSet> Blocks = RedIce.get(World.dimension());
        Map<Long, List<Vec3>> Points = Loose.remove(World.dimension());
        Set<Long> Keys = new HashSet<>();
        if (Blocks != null) {
            Keys.addAll(Blocks.keySet());
        }
        if (Points != null) {
            Keys.addAll(Points.keySet());
        }
        for (Long Key : Keys) {
            exposeCluster(World, Blocks == null ? null : Blocks.get(Key), Points == null ? null : Points.get(Key));
        }
    }

    private static void collectCarriers(ServerLevel World) {
        Set<UUID> Ids = Carriers.get(World.dimension());
        if (Ids == null || Ids.isEmpty()) {
            return;
        }
        Iterator<UUID> Each = Ids.iterator();
        while (Each.hasNext()) {
            Entity Found = World.getEntity(Each.next());
            if (!(Found instanceof LivingEntity Bearer) || !Bearer.isAlive() || !Scp009Doom.carries(Bearer)) {
                Each.remove();
                continue;
            }
            offer(World, Bearer.getBoundingBox().getCenter());
        }
    }

    private static void scanPending(ServerLevel World) {
        Set<Long> Waiting = Pending.get(World.dimension());
        if (Waiting == null || Waiting.isEmpty()) {
            return;
        }
        Iterator<Long> Each = Waiting.iterator();
        int Done = 0;
        while (Each.hasNext() && Done < ScansPerTick) {
            long Key = Each.next();
            Each.remove();
            Done++;
            scanChunk(World, Key);
        }
    }

    private static void scanChunk(ServerLevel World, long Key) {
        int ChunkX = ChunkPos.getX(Key);
        int ChunkZ = ChunkPos.getZ(Key);
        if (!World.hasChunk(ChunkX, ChunkZ)) {
            return;
        }
        LevelChunk Chunk = World.getChunk(ChunkX, ChunkZ);
        Block Ice = Scp009Registry.RedIce.get();
        LevelChunkSection[] Sections = Chunk.getSections();
        LongOpenHashSet Found = null;
        for (int Index = 0; Index < Sections.length; Index++) {
            LevelChunkSection Section = Sections[Index];
            if (Section.hasOnlyAir() || !Section.maybeHas(State -> State.is(Ice))) {
                continue;
            }
            int BaseY = SectionPos.sectionToBlockCoord(Chunk.getSectionYFromSectionIndex(Index));
            for (int Y = 0; Y < 16; Y++) {
                for (int Z = 0; Z < 16; Z++) {
                    for (int X = 0; X < 16; X++) {
                        if (Section.getBlockState(X, Y, Z).is(Ice)) {
                            if (Found == null) {
                                Found = new LongOpenHashSet();
                            }
                            Found.add(BlockPos.asLong(SectionPos.sectionToBlockCoord(ChunkX) + X, BaseY + Y, SectionPos.sectionToBlockCoord(ChunkZ) + Z));
                        }
                    }
                }
            }
        }
        Map<Long, LongOpenHashSet> Chunks = RedIce.computeIfAbsent(World.dimension(), Name -> new HashMap<>());
        if (Found == null) {
            Chunks.remove(Key);
        } else {
            Chunks.put(Key, Found);
        }
    }

    private static void exposeCluster(ServerLevel World, @Nullable LongOpenHashSet Blocks, @Nullable List<Vec3> Points) {
        int BlockCount = Blocks == null ? 0 : Blocks.size();
        int PointCount = Points == null ? 0 : Points.size();
        int Total = BlockCount + PointCount;
        if (Total == 0) {
            return;
        }
        double[] Xs = new double[Total];
        double[] Ys = new double[Total];
        double[] Zs = new double[Total];
        int Slot = 0;
        if (Blocks != null) {
            LongIterator Each = Blocks.iterator();
            while (Each.hasNext()) {
                long Packed = Each.nextLong();
                Xs[Slot] = BlockPos.getX(Packed) + 0.5;
                Ys[Slot] = BlockPos.getY(Packed) + 0.5;
                Zs[Slot] = BlockPos.getZ(Packed) + 0.5;
                Slot++;
            }
        }
        if (Points != null) {
            for (Vec3 Point : Points) {
                Xs[Slot] = Point.x;
                Ys[Slot] = Point.y;
                Zs[Slot] = Point.z;
                Slot++;
            }
        }
        double MinX = Double.MAX_VALUE;
        double MinY = Double.MAX_VALUE;
        double MinZ = Double.MAX_VALUE;
        double MaxX = -Double.MAX_VALUE;
        double MaxY = -Double.MAX_VALUE;
        double MaxZ = -Double.MAX_VALUE;
        for (int Index = 0; Index < Total; Index++) {
            MinX = Math.min(MinX, Xs[Index]);
            MinY = Math.min(MinY, Ys[Index]);
            MinZ = Math.min(MinZ, Zs[Index]);
            MaxX = Math.max(MaxX, Xs[Index]);
            MaxY = Math.max(MaxY, Ys[Index]);
            MaxZ = Math.max(MaxZ, Zs[Index]);
        }
        AABB Box = new AABB(MinX, MinY, MinZ, MaxX, MaxY, MaxZ).inflate(Scp009Exposure.Radius);
        List<LivingEntity> Found = World.getEntitiesOfClass(LivingEntity.class, Box, Scp009Exposure::isVulnerable);
        if (Found.isEmpty()) {
            return;
        }
        Block Ice = Scp009Registry.RedIce.get();
        double Reach = Scp009Exposure.Radius * Scp009Exposure.Radius;
        int Size = Found.size();
        int Start = (int) Math.floorMod(World.getGameTime() / Scp009Exposure.Interval, (long) Size);
        int Checks = 0;
        int[] Nearest = new int[NearestTried];
        double[] Distances = new double[NearestTried];
        for (int Step = 0; Step < Size && Checks < MaxSightChecks; Step++) {
            LivingEntity Target = Found.get((Start + Step) % Size);
            Vec3 Body = Target.getBoundingBox().getCenter();
            int Kept = 0;
            for (int Index = 0; Index < Total; Index++) {
                double Dx = Xs[Index] - Body.x;
                double Dy = Ys[Index] - Body.y;
                double Dz = Zs[Index] - Body.z;
                double Distance = Dx * Dx + Dy * Dy + Dz * Dz;
                if (Distance > Reach) {
                    continue;
                }
                int Place;
                if (Kept < NearestTried) {
                    Place = Kept;
                    Kept++;
                } else if (Distance < Distances[NearestTried - 1]) {
                    Place = NearestTried - 1;
                } else {
                    continue;
                }
                Nearest[Place] = Index;
                Distances[Place] = Distance;
                while (Place > 0 && Distances[Place] < Distances[Place - 1]) {
                    int SwapIndex = Nearest[Place];
                    double SwapDistance = Distances[Place];
                    Nearest[Place] = Nearest[Place - 1];
                    Distances[Place] = Distances[Place - 1];
                    Nearest[Place - 1] = SwapIndex;
                    Distances[Place - 1] = SwapDistance;
                    Place--;
                }
            }
            for (int Pick = 0; Pick < Kept && Checks < MaxSightChecks; Pick++) {
                Checks++;
                int Index = Nearest[Pick];
                Vec3 From = new Vec3(Xs[Index], Ys[Index], Zs[Index]);
                if (Scp009Exposure.sees(World, From, Target, Pos -> World.getBlockState(Pos).is(Ice))) {
                    Scp009Doom.doom(Target);
                    break;
                }
            }
        }
    }
}
