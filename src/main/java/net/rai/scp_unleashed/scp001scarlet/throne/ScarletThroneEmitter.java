package net.rai.scp_unleashed.scp001scarlet.throne;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.LinkedHashSet;
import java.util.List;

public final class ScarletThroneEmitter {
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final long BudgetNanos = 6_000_000L;
    public static final int RowsPerStep = 2;
    public static final int TerrainSteps = 16 / RowsPerStep;
    private static final LinkedHashSet<Long> Pending = new LinkedHashSet<>();
    private static Job Current;

    private static final class Job {
        final long Chunk;
        int Step;

        Job(long Chunk) {
            this.Chunk = Chunk;
        }
    }

    private ScarletThroneEmitter() {
    }

    public static synchronized void clear() {
        Pending.clear();
        Current = null;
    }

    public static synchronized int pending() {
        return Pending.size() + (Current != null ? 1 : 0);
    }

    public static synchronized void planned(ServerLevel World, ScarletThroneSite Data) {
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
        ScarletThroneSite Data = ScarletThroneSite.get(World.getServer());
        if (!Data.planned() || Data.rising() || Data.done(Pos.toLong()) || !Data.covers(Pos)) {
            return;
        }
        Pending.add(Pos.toLong());
    }

    public static synchronized void tick(MinecraftServer Server) {
        ServerLevel World = Server.overworld();
        ScarletThroneSite Data = ScarletThroneSite.get(Server);
        if (!Data.planned() || Data.rising()) {
            return;
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
                Current = null;
            } else {
                Current.Step++;
            }
        }
    }

    private static Job next(ServerLevel World, ScarletThroneSite Data) {
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

    public static void emitChunk(ServerLevel World, ScarletThroneSite Data, ChunkPos Pos) {
        int Step = 0;
        while (!step(World, Data, Pos, Step)) {
            Step++;
        }
        Data.finish(Pos.toLong());
    }

    public static void emitAll(ServerLevel World, ScarletThroneSite Data) {
        for (ChunkPos Each : Data.chunks()) {
            if (!Data.done(Each.toLong())) {
                World.getChunk(Each.x, Each.z);
                emitChunk(World, Data, Each);
            }
        }
    }

    public static boolean step(ServerLevel World, ScarletThroneSite Data, ChunkPos Pos, int Step) {
        LevelChunk Chunk = World.getChunk(Pos.x, Pos.z);
        if (Step < TerrainSteps) {
            terrain(World, Data, Chunk, Pos, Step * RowsPerStep, Step * RowsPerStep + RowsPerStep - 1);
            return false;
        }
        structures(World, Data, Pos);
        return true;
    }

    public static boolean loose(BlockState State) {
        return State.isAir() || State.canBeReplaced() || State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES) || State.is(BlockTags.FLOWERS) || !State.getFluidState().isEmpty()
                || State.is(Blocks.SNOW) || State.is(BlockTags.ICE) || State.is(Blocks.BAMBOO) || State.is(Blocks.CACTUS) || State.is(Blocks.SUGAR_CANE)
                || State.is(BlockTags.SAPLINGS) || State.is(Blocks.BROWN_MUSHROOM_BLOCK) || State.is(Blocks.RED_MUSHROOM_BLOCK) || State.is(Blocks.MUSHROOM_STEM)
                || State.is(BlockTags.BEEHIVES) || State.is(Blocks.COCOA) || State.is(Blocks.VINE) || State.is(Blocks.PUMPKIN) || State.is(Blocks.MELON)
                || State.is(Blocks.POWDER_SNOW);
    }

    private static int natural(LevelChunk Chunk, ServerLevel World, int X, int Z, BlockPos.MutableBlockPos Cursor) {
        int Y = Chunk.getHeight(Heightmap.Types.WORLD_SURFACE, X & 15, Z & 15);
        int Bottom = World.getMinBuildHeight() + 1;
        while (Y > Bottom) {
            if (!loose(Chunk.getBlockState(Cursor.set(X, Y, Z)))) {
                return Y;
            }
            Y--;
        }
        return Y;
    }

    public static void write(ServerLevel World, LevelChunk Chunk, BlockPos Pos, BlockState State) {
        BlockState Old = Chunk.getBlockState(Pos);
        if (Old == State) {
            return;
        }
        if (Old.hasBlockEntity()) {
            ScarletThroneSpill.spill(World, Pos.immutable(), Old);
            World.removeBlockEntity(Pos);
        }
        World.setBlock(Pos, State, Flags);
    }

    private static void terrain(ServerLevel World, ScarletThroneSite Data, LevelChunk Chunk, ChunkPos Pos, int FromRow, int ToRow) {
        BlockPos Centre = Data.centre();
        long Seed = Data.seed();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Row = FromRow; Row <= ToRow; Row++) {
            int Z = Pos.getMinBlockZ() + Row;
            for (int Column = 0; Column < 16; Column++) {
                column(World, Chunk, Centre, Seed, Pos.getMinBlockX() + Column, Z, Cursor);
            }
        }
    }

    public static void column(ServerLevel World, LevelChunk Chunk, BlockPos Centre, long Seed, int X, int Z, BlockPos.MutableBlockPos Cursor) {
        int Floor = Centre.getY() - 1;
        BlockState Air = Blocks.AIR.defaultBlockState();
        int Dx = X - Centre.getX();
        int Dz = Z - Centre.getZ();
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Distance > ScarletThrone.RimRadius) {
            return;
        }
        int Ground = natural(Chunk, World, X, Z, Cursor);
        int Top = ScarletThrone.surface(Dx, Dz, Ground, Floor);
        int Ceiling = Math.max(Chunk.getHeight(Heightmap.Types.WORLD_SURFACE, X & 15, Z & 15) + 1, Top + 2);
        if (Distance <= ScarletThrone.FlatRadius + 1) {
            Ceiling = Math.max(Ceiling, Centre.getY() + ScarletThrone.ClearAbove);
        }
        for (int Y = Top + 1; Y <= Ceiling; Y++) {
            write(World, Chunk, Cursor.set(X, Y, Z), Air);
        }
        int Lowest = Math.min(Ground, Top) - 4;
        for (int Y = Lowest; Y < Top; Y++) {
            BlockState Here = Chunk.getBlockState(Cursor.set(X, Y, Z));
            if (Y > Ground || loose(Here)) {
                write(World, Chunk, Cursor, ScarletThrone.rim(Dx, Dz, Top - Y, Seed));
            }
        }
        BlockState Surface = Distance <= ScarletThrone.FloorRadius ? ScarletThrone.floor(Dx, Dz, Seed) : ScarletThrone.rim(Dx, Dz, 0, Seed);
        write(World, Chunk, Cursor.set(X, Top, Z), Surface);
    }

    public static void piece(ServerLevel World, BlockPos Centre, ScarletThrone.Piece Each, BlockPos.MutableBlockPos Cursor) {
        Cursor.set(Centre.getX() + Each.X(), Centre.getY() + Each.Y(), Centre.getZ() + Each.Z());
        write(World, World.getChunkAt(Cursor), Cursor.immutable(), Each.State());
    }

    private static void structures(ServerLevel World, ScarletThroneSite Data, ChunkPos Pos) {
        BlockPos Centre = Data.centre();
        ScarletThrone.Plan Made = ScarletThrone.plan(Data.seed());
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Shift = -1; Shift <= 1; Shift++) {
            for (int Turn = -1; Turn <= 1; Turn++) {
                List<ScarletThrone.Piece> Pieces = Made.ByChunk().get(ScarletThrone.chunkKey(Pos.getMinBlockX() - Centre.getX() + Shift * 16, Pos.getMinBlockZ() - Centre.getZ() + Turn * 16));
                if (Pieces == null) {
                    continue;
                }
                for (ScarletThrone.Piece Each : Pieces) {
                    int X = Centre.getX() + Each.X();
                    int Z = Centre.getZ() + Each.Z();
                    if (X >> 4 != Pos.x || Z >> 4 != Pos.z) {
                        continue;
                    }
                    Cursor.set(X, Centre.getY() + Each.Y(), Z);
                    write(World, World.getChunkAt(Cursor), Cursor.immutable(), Each.State());
                }
            }
        }
    }
}
