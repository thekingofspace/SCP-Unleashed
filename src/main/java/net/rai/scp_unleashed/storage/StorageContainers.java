package net.rai.scp_unleashed.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class StorageContainers {
    private StorageContainers() {
    }

    public static Optional<StorageContainerBlockEntity> find(BlockGetter World, BlockPos Pos) {
        return Optional.ofNullable(StorageContainerBlock.containerAt(World, Pos));
    }

    public static Optional<StorageView> view(BlockGetter World, BlockPos Pos) {
        return find(World, Pos).map(Found -> Found);
    }

    public static boolean holds(BlockGetter World, BlockPos Pos, Predicate<ItemStack> Matches) {
        return find(World, Pos).map(Found -> Found.hasContents() && Matches.test(Found.peekContents())).orElse(false);
    }

    public static List<StorageContainerBlockEntity> near(Level World, BlockPos Centre, int Radius) {
        List<StorageContainerBlockEntity> Found = new ArrayList<>();
        int MinX = SectionPos.blockToSectionCoord(Centre.getX() - Radius);
        int MaxX = SectionPos.blockToSectionCoord(Centre.getX() + Radius);
        int MinZ = SectionPos.blockToSectionCoord(Centre.getZ() - Radius);
        int MaxZ = SectionPos.blockToSectionCoord(Centre.getZ() + Radius);
        long Reach = (long) Radius * Radius;
        for (int ChunkX = MinX; ChunkX <= MaxX; ChunkX++) {
            for (int ChunkZ = MinZ; ChunkZ <= MaxZ; ChunkZ++) {
                if (!World.hasChunk(ChunkX, ChunkZ)) {
                    continue;
                }
                LevelChunk Chunk = World.getChunk(ChunkX, ChunkZ);
                for (BlockEntity Candidate : Chunk.getBlockEntities().values()) {
                    if (Candidate instanceof StorageContainerBlockEntity Locker && Locker.getBlockPos().distSqr(Centre) <= Reach) {
                        Found.add(Locker);
                    }
                }
            }
        }
        return Found;
    }
}
