package net.rai.scp_unleashed.scp076;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.ArrayList;
import java.util.List;

public final class Scp076Sites {
    private Scp076Sites() {
    }

    public static boolean villageNear(ServerLevel World, BlockPos Centre, int Radius) {
        ChunkGeneratorStructureState State = World.getChunkSource().getGeneratorState();
        ChunkGenerator Generator = World.getChunkSource().getGenerator();
        Climate.Sampler Sampler = World.getChunkSource().randomState().sampler();
        int SampleY = QuartPos.fromBlock(Generator.getSeaLevel() + 4);
        ChunkPos Here = new ChunkPos(Centre);
        double Limit = (double) Radius * Radius;
        for (Holder<StructureSet> Entry : State.possibleStructureSets()) {
            StructureSet Set = Entry.value();
            if (!(Set.placement() instanceof RandomSpreadStructurePlacement Spread)) {
                continue;
            }
            List<Structure> Villages = new ArrayList<>();
            for (StructureSet.StructureSelectionEntry Choice : Set.structures()) {
                if (Choice.structure().is(StructureTags.VILLAGE)) {
                    Villages.add(Choice.structure().value());
                }
            }
            if (Villages.isEmpty()) {
                continue;
            }
            int Spacing = Math.max(1, Spread.spacing());
            int Rings = Mth.ceil(Radius / 16.0 / Spacing) + 1;
            for (int Rx = -Rings; Rx <= Rings; Rx++) {
                for (int Rz = -Rings; Rz <= Rings; Rz++) {
                    ChunkPos Candidate = Spread.getPotentialStructureChunk(State.getLevelSeed(), Here.x + Rx * Spacing, Here.z + Rz * Spacing);
                    BlockPos At = Spread.getLocatePos(Candidate);
                    double Dx = At.getX() - Centre.getX();
                    double Dz = At.getZ() - Centre.getZ();
                    if (Dx * Dx + Dz * Dz > Limit || !Spread.isStructureChunk(State, Candidate.x, Candidate.z)) {
                        continue;
                    }
                    Holder<Biome> Sampled = Generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(At.getX() + 8), SampleY, QuartPos.fromBlock(At.getZ() + 8), Sampler);
                    for (Structure Village : Villages) {
                        if (Village.biomes().contains(Sampled)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
