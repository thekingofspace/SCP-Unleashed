package net.rai.scp_unleashed.scp173;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class Scp173ResidueIndex extends SavedData {
    public static final String DataName = "scp_unleashed_scp173_residue";
    private static final SavedData.Factory<Scp173ResidueIndex> DataFactory = new SavedData.Factory<>(Scp173ResidueIndex::new, Scp173ResidueIndex::load);
    private final Long2ObjectOpenHashMap<LongOpenHashSet> Chunks = new Long2ObjectOpenHashMap<>();
    private int Total;

    public static Scp173ResidueIndex get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    private static long chunkOf(long Packed) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(BlockPos.getX(Packed)), SectionPos.blockToSectionCoord(BlockPos.getZ(Packed)));
    }

    public int total() {
        return Total;
    }

    public void add(BlockPos Pos) {
        long Packed = Pos.asLong();
        if (Chunks.computeIfAbsent(chunkOf(Packed), Key -> new LongOpenHashSet()).add(Packed)) {
            Total++;
            setDirty();
        }
    }

    public void remove(BlockPos Pos) {
        long Packed = Pos.asLong();
        long Chunk = chunkOf(Packed);
        LongOpenHashSet Found = Chunks.get(Chunk);
        if (Found != null && Found.remove(Packed)) {
            Total--;
            if (Found.isEmpty()) {
                Chunks.remove(Chunk);
            }
            setDirty();
        }
    }

    public List<BlockPos> near(ServerLevel World, BlockPos Centre, int Radius, int Height) {
        List<BlockPos> Found = new ArrayList<>();
        LongArrayList Stale = new LongArrayList();
        int MinChunkX = SectionPos.blockToSectionCoord(Centre.getX() - Radius);
        int MaxChunkX = SectionPos.blockToSectionCoord(Centre.getX() + Radius);
        int MinChunkZ = SectionPos.blockToSectionCoord(Centre.getZ() - Radius);
        int MaxChunkZ = SectionPos.blockToSectionCoord(Centre.getZ() + Radius);
        for (int ChunkX = MinChunkX; ChunkX <= MaxChunkX; ChunkX++) {
            for (int ChunkZ = MinChunkZ; ChunkZ <= MaxChunkZ; ChunkZ++) {
                LongOpenHashSet Entries = Chunks.get(ChunkPos.asLong(ChunkX, ChunkZ));
                if (Entries == null) {
                    continue;
                }
                for (long Packed : Entries) {
                    int X = BlockPos.getX(Packed);
                    int Y = BlockPos.getY(Packed);
                    int Z = BlockPos.getZ(Packed);
                    if (Math.abs(X - Centre.getX()) > Radius || Math.abs(Z - Centre.getZ()) > Radius || Math.abs(Y - Centre.getY()) > Height) {
                        continue;
                    }
                    BlockPos Pos = new BlockPos(X, Y, Z);
                    if (World.isLoaded(Pos) && !Scp173Residue.isResidue(World.getBlockState(Pos))) {
                        Stale.add(Packed);
                        continue;
                    }
                    Found.add(Pos);
                }
            }
        }
        for (long Packed : Stale) {
            remove(BlockPos.of(Packed));
        }
        return Found;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        LongArrayList All = new LongArrayList(Total);
        for (LongOpenHashSet Entries : Chunks.values()) {
            All.addAll(Entries);
        }
        Tag.putLongArray("Residue", All.toLongArray());
        return Tag;
    }

    public static Scp173ResidueIndex load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp173ResidueIndex Data = new Scp173ResidueIndex();
        for (long Packed : Tag.getLongArray("Residue")) {
            if (Data.Chunks.computeIfAbsent(chunkOf(Packed), Key -> new LongOpenHashSet()).add(Packed)) {
                Data.Total++;
            }
        }
        return Data;
    }
}
