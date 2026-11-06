package net.rai.scp_unleashed.scp001gate;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;

public final class Scp001GateEmitter {
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final long BudgetNanos = 6_000_000L;
    public static final int RowsPerStep = 4;
    public static final int TerrainSteps = 16 / RowsPerStep;
    public static final int GateStep = TerrainSteps;
    public static final int DecorStep = TerrainSteps + 1;
    public static final int RestoreDelay = 200;
    private static final Logger Log = LogUtils.getLogger();
    private static final LinkedHashSet<Long> Pending = new LinkedHashSet<>();
    private static Job Current;
    private static int Missing;

    private static final class Job {
        final long Chunk;
        int Step;

        Job(long Chunk) {
            this.Chunk = Chunk;
        }
    }

    private Scp001GateEmitter() {
    }

    public static synchronized void clear() {
        Pending.clear();
        Current = null;
        Missing = 0;
    }

    public static synchronized int pending() {
        return Pending.size() + (Current != null ? 1 : 0);
    }

    public static synchronized void planned(ServerLevel World, Scp001GateSites Data) {
        for (ChunkPos Each : Data.chunks()) {
            if (World.hasChunk(Each.x, Each.z) && !Data.done(Each.toLong())) {
                Pending.add(Each.toLong());
            }
        }
    }

    public static synchronized void chunkLoaded(ServerLevel World, ChunkPos Pos) {
        if (World.dimension() != Level.OVERWORLD) {
            return;
        }
        Scp001GateSites Data = Scp001GateSites.get(World.getServer());
        if (!Data.planned() || Data.done(Pos.toLong()) || !Data.covers(Pos)) {
            return;
        }
        Pending.add(Pos.toLong());
    }

    public static synchronized void tick(MinecraftServer Server) {
        ServerLevel World = Server.overworld();
        Scp001GateSites Data = Scp001GateSites.get(Server);
        if (!Data.planned()) {
            return;
        }
        if (World.getGameTime() % 20 == 11) {
            keep(World, Data);
        }
        long Start = System.nanoTime();
        while (System.nanoTime() - Start < BudgetNanos) {
            if (Current == null) {
                Current = next(World, Data);
                if (Current == null) {
                    return;
                }
            }
            ChunkPos Pos = new ChunkPos(Current.Chunk);
            if (!World.hasChunk(Pos.x, Pos.z)) {
                Current = null;
                continue;
            }
            if (step(World, Data, Pos, Current.Step)) {
                Data.finish(Current.Chunk);
                settle(World, Data, Pos);
                Current = null;
            } else {
                Current.Step++;
            }
        }
    }

    private static Job next(ServerLevel World, Scp001GateSites Data) {
        while (!Pending.isEmpty()) {
            Long Chunk = Pending.iterator().next();
            Pending.remove(Chunk);
            ChunkPos Pos = new ChunkPos(Chunk);
            if (!Data.done(Chunk) && World.hasChunk(Pos.x, Pos.z)) {
                return new Job(Chunk);
            }
        }
        return null;
    }

    public static void emitChunk(ServerLevel World, Scp001GateSites Data, ChunkPos Pos) {
        int Step = 0;
        while (!step(World, Data, Pos, Step)) {
            Step++;
        }
        Data.finish(Pos.toLong());
        settle(World, Data, Pos);
    }

    public static int settle(ServerLevel World, Scp001GateSites Data, ChunkPos Pos) {
        if (!World.areEntitiesLoaded(Pos.toLong())) {
            return 0;
        }
        BlockPos Centre = Data.centre();
        AABB Area = new AABB(Pos.getMinBlockX(), Centre.getY() - Scp001GateZones.Below, Pos.getMinBlockZ(), Pos.getMaxBlockX() + 1, Centre.getY() + Scp001GateZones.Above,
                Pos.getMaxBlockZ() + 1);
        return Scp001GateStrike.clear(World, Centre, Area);
    }

    public static boolean step(ServerLevel World, Scp001GateSites Data, ChunkPos Pos, int Step) {
        LevelChunk Chunk = World.getChunk(Pos.x, Pos.z);
        if (Step < TerrainSteps) {
            terrain(World, Data, Chunk, Pos, Step * RowsPerStep, Step * RowsPerStep + RowsPerStep - 1);
            return false;
        }
        if (Step == GateStep) {
            gate(World, Data, Pos);
            return false;
        }
        decor(World, Data, Pos);
        return true;
    }

    private static boolean loose(BlockState State) {
        return State.isAir() || State.canBeReplaced() || State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES) || State.is(BlockTags.FLOWERS) || State.is(Blocks.SNOW)
                || State.is(BlockTags.ICE) || !State.getFluidState().isEmpty() || State.is(Blocks.BAMBOO) || State.is(Blocks.CACTUS) || State.is(Blocks.SUGAR_CANE)
                || State.is(BlockTags.SAPLINGS) || State.is(Blocks.BROWN_MUSHROOM_BLOCK) || State.is(Blocks.RED_MUSHROOM_BLOCK) || State.is(Blocks.MUSHROOM_STEM)
                || State.is(BlockTags.BEEHIVES) || State.is(Blocks.COCOA) || State.is(Blocks.VINE) || State.is(Blocks.MOSS_CARPET) || State.is(BlockTags.WOOL_CARPETS)
                || State.is(Blocks.PUMPKIN) || State.is(Blocks.MELON) || State.is(Blocks.POWDER_SNOW);
    }

    public static int natural(LevelChunk Chunk, ServerLevel World, int X, int Z, BlockPos.MutableBlockPos Cursor) {
        int Y = Chunk.getHeight(Heightmap.Types.WORLD_SURFACE, X & 15, Z & 15);
        int Floor = World.getMinBuildHeight() + 1;
        while (Y > Floor) {
            BlockState State = Chunk.getBlockState(Cursor.set(X, Y, Z));
            if (!loose(State)) {
                return Y;
            }
            Y--;
        }
        return Y;
    }

    private static void write(ServerLevel World, LevelChunk Chunk, BlockPos Pos, BlockState State) {
        BlockState Old = Chunk.getBlockState(Pos);
        if (Old == State) {
            return;
        }
        if (Old.hasBlockEntity()) {
            World.removeBlockEntity(Pos);
        }
        World.setBlock(Pos, State, Flags);
    }

    private static void terrain(ServerLevel World, Scp001GateSites Data, LevelChunk Chunk, ChunkPos Pos, int FromRow, int ToRow) {
        BlockPos Centre = Data.centre();
        long Seed = Data.seed();
        int Flat = Centre.getY() - 1;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        BlockState Air = Blocks.AIR.defaultBlockState();
        for (int Row = FromRow; Row <= ToRow; Row++) {
            int Z = Pos.getMinBlockZ() + Row;
            for (int Column = 0; Column < 16; Column++) {
                int X = Pos.getMinBlockX() + Column;
                int Dx = X - Centre.getX();
                int Dz = Z - Centre.getZ();
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                if (Distance > Scp001GatePlan.PlainRadius) {
                    continue;
                }
                int Ground = natural(Chunk, World, X, Z, Cursor);
                int Top = Scp001GatePlan.target(Dx, Dz, Ground, Flat, Seed);
                int Ceiling = Chunk.getHeight(Heightmap.Types.WORLD_SURFACE, X & 15, Z & 15) + 1;
                if (Distance <= Scp001GatePlan.ClearRadius || Top < Ground) {
                    for (int Y = Top + 1; Y <= Math.max(Ceiling, Top + 2); Y++) {
                        write(World, Chunk, Cursor.set(X, Y, Z), Air);
                    }
                }
                for (int Y = Ground + 1; Y < Top; Y++) {
                    write(World, Chunk, Cursor.set(X, Y, Z), Blocks.DIRT.defaultBlockState());
                }
                BlockState Surface = Scp001GatePlan.top(Dx, Dz, Seed);
                if (Surface == null) {
                    if (Top != Ground || !Chunk.getBlockState(Cursor.set(X, Top, Z)).isFaceSturdy(World, Cursor, Direction.UP)) {
                        write(World, Chunk, Cursor.set(X, Top, Z), Blocks.GRASS_BLOCK.defaultBlockState());
                    }
                    continue;
                }
                write(World, Chunk, Cursor.set(X, Top, Z), Surface);
                for (int Depth0 = 1; Depth0 <= 3; Depth0++) {
                    BlockState Below = Chunk.getBlockState(Cursor.set(X, Top - Depth0, Z));
                    if (loose(Below) || Below.is(BlockTags.DIRT) || Below.is(Blocks.GRASS_BLOCK) || Below.is(BlockTags.SAND)) {
                        write(World, Chunk, Cursor.set(X, Top - Depth0, Z), Scp001GatePlan.under(Depth0));
                    }
                }
            }
        }
    }

    private static void gate(ServerLevel World, Scp001GateSites Data, ChunkPos Pos) {
        BlockPos Centre = Data.centre();
        Direction Front = Data.front();
        long Seed = Data.seed();
        for (int X = Scp001GatePlan.MinX; X <= Scp001GatePlan.MaxX; X++) {
            for (int Z = Scp001GatePlan.MinZ; Z <= Scp001GatePlan.MaxZ; Z++) {
                BlockPos Column = Scp001GatePlan.toWorld(Centre, Front, X, 0, Z);
                if (Column.getX() >> 4 != Pos.x || Column.getZ() >> 4 != Pos.z) {
                    continue;
                }
                for (int Y = Scp001GatePlan.MinY; Y <= Scp001GatePlan.MaxY; Y++) {
                    Scp001GatePlan.Kind Type = Scp001GatePlan.cell(X, Y, Z, Seed);
                    BlockPos At = Column.atY(Centre.getY() + Y);
                    if (Type == null) {
                        if (Y >= 0) {
                            write(World, World.getChunkAt(At), At, Blocks.AIR.defaultBlockState());
                        }
                        continue;
                    }
                    write(World, World.getChunkAt(At), At, Scp001GatePlan.state(Type, Front));
                    if (Type == Scp001GatePlan.Kind.Portal && World.getBlockEntity(At) instanceof Scp001GatePortalBlockEntity Portal) {
                        Portal.bind(Centre, Front);
                    }
                }
            }
        }
    }

    private static void decor(ServerLevel World, Scp001GateSites Data, ChunkPos Pos) {
        BlockPos Centre = Data.centre();
        Direction Front = Data.front();
        long Seed = Data.seed();
        for (Scp001GatePlan.Piece Each : Scp001GatePlan.rubble(Seed)) {
            BlockPos At = Scp001GatePlan.toWorld(Centre, Front, Each.X(), Each.Y(), Each.Z());
            if (At.getX() >> 4 == Pos.x && At.getZ() >> 4 == Pos.z) {
                write(World, World.getChunkAt(At), At, Scp001GatePlan.state(Each.Type(), Front));
            }
        }
        List<Scp001GatePlan.Piece> Bones = Scp001GatePlan.remains(Seed);
        for (int Index = 0; Index < Bones.size(); Index++) {
            Scp001GatePlan.Piece Each = Bones.get(Index);
            BlockPos Column = Centre.offset(Each.X(), 0, Each.Z());
            if (Column.getX() >> 4 != Pos.x || Column.getZ() >> 4 != Pos.z) {
                continue;
            }
            int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Column.getX(), Column.getZ());
            Block Kind = Scp001GatePlan.remainsBlock(Index);
            BlockState State = Kind == Blocks.SKELETON_SKULL ? Kind.defaultBlockState().setValue(SkullBlock.ROTATION, Index * 3 % 16)
                    : Kind.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Index % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z);
            BlockPos At = Column.atY(Kind == Blocks.SKELETON_SKULL ? Top : Top - 1);
            write(World, World.getChunkAt(At), At, State);
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        LevelChunk Chunk = World.getChunk(Pos.x, Pos.z);
        for (int Column = 0; Column < 16; Column++) {
            for (int Row = 0; Row < 16; Row++) {
                int X = Pos.getMinBlockX() + Column;
                int Z = Pos.getMinBlockZ() + Row;
                int Dx = X - Centre.getX();
                int Dz = Z - Centre.getZ();
                if (!Scp001GatePlan.deadBush(Dx, Dz, Seed)) {
                    continue;
                }
                int Top = Chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Column, Row);
                BlockState Ground = Chunk.getBlockState(Cursor.set(X, Top - 1, Z));
                if (Ground.is(BlockTags.DIRT) || Ground.is(Blocks.PACKED_MUD)) {
                    write(World, Chunk, Cursor.set(X, Top, Z), Blocks.DEAD_BUSH.defaultBlockState());
                }
            }
        }
    }

    private static void keep(ServerLevel World, Scp001GateSites Data) {
        ChunkPos Home = Data.centreChunk();
        if (!Data.done(Home.toLong()) || !World.hasChunk(Home.x, Home.z) || !World.areEntitiesLoaded(Home.toLong())) {
            Missing = 0;
            return;
        }
        Vec3 Post = Data.post();
        if (Data.guardian() != null) {
            Entity Known = World.getEntity(Data.guardian());
            if (Known instanceof Scp001GateGuardian && Known.isAlive()) {
                Missing = 0;
                return;
            }
        }
        List<Scp001GateGuardian> Near = World.getEntitiesOfClass(Scp001GateGuardian.class, new net.minecraft.world.phys.AABB(Post, Post).inflate(16.0));
        if (!Near.isEmpty()) {
            Data.guardian(Near.get(0).getUUID());
            Missing = 0;
            return;
        }
        Missing += 20;
        if (Data.guardian() != null && Missing < RestoreDelay) {
            return;
        }
        Scp001GateGuardian Made = Scp001GateGuardian.post(World, Data.centre(), Data.front(), Post, Data.guardian() == null);
        if (Made != null) {
            Data.guardian(Made.getUUID());
            Log.info("SCP-001-1 took its post at {}", Made.blockPosition());
        }
        Missing = 0;
    }
}
