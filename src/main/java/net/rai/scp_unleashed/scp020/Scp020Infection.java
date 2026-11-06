package net.rai.scp_unleashed.scp020;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class Scp020Infection {
    public static final double FaceLift = 0.08;
    private static final Map<ResourceKey<Level>, LongOpenHashSet> Tracked = new HashMap<>();

    private Scp020Infection() {
    }

    public interface FaceVisitor {
        void visit(BlockPos Host, Direction Side);
    }

    @Nullable
    public static LevelChunk chunkAt(Level World, int ChunkX, int ChunkZ) {
        return World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
    }

    @Nullable
    public static Scp020Growth colonyIn(@Nullable LevelChunk Chunk) {
        if (Chunk == null) {
            return null;
        }
        Scp020Growth Colony = Chunk.getExistingDataOrNull(Scp020Registry.Growth.get());
        return Colony == null || Colony.isEmpty() ? null : Colony;
    }

    @Nullable
    public static Scp020Growth colonyAt(Level World, BlockPos Pos) {
        return colonyIn(chunkAt(World, SectionPos.blockToSectionCoord(Pos.getX()), SectionPos.blockToSectionCoord(Pos.getZ())));
    }

    public static int facesAt(Level World, BlockPos Host) {
        Scp020Growth Colony = colonyAt(World, Host);
        return Colony == null ? 0 : Colony.facesAt(Host.asLong());
    }

    public static boolean has(Level World, BlockPos Host, Direction Side) {
        return Scp020Growth.hasFace(facesAt(World, Host), Side);
    }

    public static boolean holds(BlockGetter World, BlockPos Host, Direction Side) {
        BlockState State = World.getBlockState(Host);
        if (State.isAir()) {
            return false;
        }
        return Block.isFaceFull(State.getBlockSupportShape(World, Host), Side) || Block.isFaceFull(State.getCollisionShape(World, Host), Side);
    }

    public static boolean isOpen(BlockGetter World, BlockPos Host, Direction Side) {
        BlockPos Cell = Host.relative(Side);
        BlockState State = World.getBlockState(Cell);
        if (State.isAir()) {
            return true;
        }
        return State.getFluidState().isEmpty() && !State.isFaceSturdy(World, Cell, Side.getOpposite());
    }

    public static boolean canTake(Level World, BlockPos Host, Direction Side) {
        BlockPos Cell = Host.relative(Side);
        return World.isLoaded(Host) && World.isLoaded(Cell) && World.getBlockState(Cell).isAir() && holds(World, Host, Side);
    }

    public static Vec3 faceCentre(BlockPos Host, Direction Side) {
        double Out = 0.5 + FaceLift;
        return Vec3.atCenterOf(Host).add(Side.getStepX() * Out, Side.getStepY() * Out, Side.getStepZ() * Out);
    }

    public static boolean infect(ServerLevel World, BlockPos Host, Direction Side) {
        LevelChunk Chunk = World.getChunkAt(Host);
        Scp020Growth Colony = Chunk.getData(Scp020Registry.Growth.get());
        if (!Colony.add(Host.asLong(), Side)) {
            return false;
        }
        Scp020MoldCensus.get(World).change(1);
        changed(World, Chunk);
        return true;
    }

    public static boolean cure(ServerLevel World, BlockPos Host, Direction Side) {
        LevelChunk Chunk = chunkAt(World, SectionPos.blockToSectionCoord(Host.getX()), SectionPos.blockToSectionCoord(Host.getZ()));
        Scp020Growth Colony = colonyIn(Chunk);
        if (Colony == null || !Colony.remove(Host.asLong(), Side)) {
            return false;
        }
        Scp020MoldCensus.get(World).change(-1);
        changed(World, Chunk);
        return true;
    }

    public static int clear(ServerLevel World, BlockPos Host) {
        LevelChunk Chunk = chunkAt(World, SectionPos.blockToSectionCoord(Host.getX()), SectionPos.blockToSectionCoord(Host.getZ()));
        Scp020Growth Colony = colonyIn(Chunk);
        if (Colony == null) {
            return 0;
        }
        int Removed = Colony.removeAll(Host.asLong());
        if (Removed > 0) {
            Scp020MoldCensus.get(World).change(-Removed);
            changed(World, Chunk);
        }
        return Removed;
    }

    public static int prune(ServerLevel World, BlockPos Host) {
        int Faces = facesAt(World, Host);
        int Removed = 0;
        if (Faces == 0) {
            return 0;
        }
        for (Direction Side : Direction.values()) {
            if (Scp020Growth.hasFace(Faces, Side) && !holds(World, Host, Side) && cure(World, Host, Side)) {
                Removed++;
            }
        }
        return Removed;
    }

    public static void forEachIn(Level World, BlockPos Low, BlockPos High, FaceVisitor Visitor) {
        int MinX = Math.min(Low.getX(), High.getX());
        int MinY = Math.min(Low.getY(), High.getY());
        int MinZ = Math.min(Low.getZ(), High.getZ());
        int MaxX = Math.max(Low.getX(), High.getX());
        int MaxY = Math.max(Low.getY(), High.getY());
        int MaxZ = Math.max(Low.getZ(), High.getZ());
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int ChunkX = MinX >> 4; ChunkX <= MaxX >> 4; ChunkX++) {
            for (int ChunkZ = MinZ >> 4; ChunkZ <= MaxZ >> 4; ChunkZ++) {
                Scp020Growth Colony = colonyIn(chunkAt(World, ChunkX, ChunkZ));
                if (Colony == null) {
                    continue;
                }
                for (long Host : Colony.hostArray()) {
                    Cursor.set(Host);
                    if (Cursor.getX() < MinX || Cursor.getX() > MaxX || Cursor.getY() < MinY || Cursor.getY() > MaxY || Cursor.getZ() < MinZ || Cursor.getZ() > MaxZ) {
                        continue;
                    }
                    int Mask = Colony.facesAt(Host);
                    BlockPos Found = Cursor.immutable();
                    for (Direction Side : Direction.values()) {
                        if (Scp020Growth.hasFace(Mask, Side)) {
                            Visitor.visit(Found, Side);
                        }
                    }
                }
            }
        }
    }

    public static int countNear(Level World, BlockPos Centre, int Radius) {
        int[] Count = {0};
        forEachIn(World, Centre.offset(-Radius, -Radius, -Radius), Centre.offset(Radius, Radius, Radius), (Host, Side) -> Count[0]++);
        return Count[0];
    }

    public static boolean anyNear(Level World, BlockPos Centre, int Radius) {
        boolean[] Found = {false};
        forEachIn(World, Centre.offset(-Radius, -Radius, -Radius), Centre.offset(Radius, Radius, Radius), (Host, Side) -> Found[0] = true);
        return Found[0];
    }

    public static int clearIn(ServerLevel World, BlockPos Centre, int Radius, FaceVisitor Killed) {
        int[] Count = {0};
        forEachIn(World, Centre.offset(-Radius, -Radius, -Radius), Centre.offset(Radius, Radius, Radius), (Host, Side) -> {
            if (cure(World, Host, Side)) {
                Count[0]++;
                Killed.visit(Host, Side);
            }
        });
        return Count[0];
    }

    public static void changed(ServerLevel World, LevelChunk Chunk) {
        Chunk.setUnsaved(true);
        Scp020Spores.touch();
        long Key = Chunk.getPos().toLong();
        Scp020Growth Colony = Chunk.getExistingDataOrNull(Scp020Registry.Growth.get());
        if (Colony == null || Colony.isEmpty()) {
            Chunk.removeData(Scp020Registry.Growth.get());
            untrack(World, Key);
            return;
        }
        Tracked.computeIfAbsent(World.dimension(), Dimension -> new LongOpenHashSet()).add(Key);
    }

    public static void chunkLoaded(ServerLevel World, LevelChunk Chunk) {
        if (colonyIn(Chunk) != null) {
            Tracked.computeIfAbsent(World.dimension(), Dimension -> new LongOpenHashSet()).add(Chunk.getPos().toLong());
        }
    }

    public static void chunkUnloaded(ServerLevel World, ChunkPos Pos) {
        LongOpenHashSet Chunks = Tracked.get(World.dimension());
        if (Chunks != null) {
            Chunks.remove(Pos.toLong());
        }
    }

    public static long[] tracked(ServerLevel World) {
        LongOpenHashSet Chunks = Tracked.get(World.dimension());
        return Chunks == null ? new long[0] : Chunks.toLongArray();
    }

    public static void untrack(ServerLevel World, long Key) {
        LongOpenHashSet Chunks = Tracked.get(World.dimension());
        if (Chunks != null) {
            Chunks.remove(Key);
        }
    }

    public static void clearAll() {
        Tracked.clear();
    }
}
