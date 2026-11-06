package net.rai.scp_unleashed.scp020;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp020Migration {
    public static final int ChunksPerTick = 8;
    private static final Map<ResourceKey<Level>, Set<Long>> Pending = new ConcurrentHashMap<>();

    private Scp020Migration() {
    }

    public static boolean holdsLegacy(LevelChunk Chunk) {
        for (LevelChunkSection Section : Chunk.getSections()) {
            if (!Section.hasOnlyAir() && Section.maybeHas(Scp020MoldBlock::isLegacy)) {
                return true;
            }
        }
        return false;
    }

    public static void chunkLoaded(ServerLevel World, LevelChunk Chunk) {
        if (holdsLegacy(Chunk)) {
            Pending.computeIfAbsent(World.dimension(), Key -> ConcurrentHashMap.newKeySet()).add(Chunk.getPos().toLong());
        }
    }

    public static void tick(ServerLevel World) {
        Set<Long> Waiting = Pending.get(World.dimension());
        if (Waiting == null || Waiting.isEmpty()) {
            return;
        }
        int Done = 0;
        Iterator<Long> Cursor = Waiting.iterator();
        while (Cursor.hasNext() && Done < ChunksPerTick) {
            long Key = Cursor.next();
            Cursor.remove();
            LevelChunk Chunk = Scp020Infection.chunkAt(World, ChunkPos.getX(Key), ChunkPos.getZ(Key));
            if (Chunk != null) {
                convertChunk(World, Chunk);
                Done++;
            }
        }
    }

    public static int convertChunk(ServerLevel World, LevelChunk Chunk) {
        List<BlockPos> Found = new ArrayList<>();
        LevelChunkSection[] Sections = Chunk.getSections();
        ChunkPos Pos = Chunk.getPos();
        for (int Index = 0; Index < Sections.length; Index++) {
            LevelChunkSection Section = Sections[Index];
            if (Section.hasOnlyAir() || !Section.maybeHas(Scp020MoldBlock::isLegacy)) {
                continue;
            }
            int BaseY = SectionPos.sectionToBlockCoord(Chunk.getSectionYFromSectionIndex(Index));
            for (int Y = 0; Y < 16; Y++) {
                for (int Z = 0; Z < 16; Z++) {
                    for (int X = 0; X < 16; X++) {
                        if (Scp020MoldBlock.isLegacy(Section.getBlockState(X, Y, Z))) {
                            Found.add(new BlockPos(Pos.getMinBlockX() + X, BaseY + Y, Pos.getMinBlockZ() + Z));
                        }
                    }
                }
            }
        }
        for (BlockPos Cell : Found) {
            convert(World, Cell, World.getBlockState(Cell));
        }
        return Found.size();
    }

    public static int convert(ServerLevel World, BlockPos Cell, BlockState State) {
        if (!Scp020MoldBlock.isLegacy(State)) {
            return 0;
        }
        Scp020MoldCensus.get(World).change(-Scp020MoldBlock.faceCount(State));
        int Moved = 0;
        for (Direction Toward : Direction.values()) {
            if (!MultifaceBlock.hasFace(State, Toward)) {
                continue;
            }
            BlockPos Host = Cell.relative(Toward);
            Direction Side = Toward.getOpposite();
            if (Scp020Infection.holds(World, Host, Side) && Scp020Infection.infect(World, Host, Side)) {
                Moved++;
            }
        }
        World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        return Moved;
    }

    public static void clearAll() {
        Pending.clear();
    }
}
