package net.rai.scp_unleashed.scp031;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp031Villages {
    public static final int LocalRadius = 80;
    public static final int LocalHeight = 32;
    public static final double HiddenDistance = 16.0;
    public static final double ViewCone = 0.45;
    public static final int SpotAttempts = 8;

    private Scp031Villages() {
    }

    public static Optional<ChunkPos> nearest(ServerLevel World, BlockPos Origin, int MaxDistance) {
        ChunkGeneratorStructureState State = World.getChunkSource().getGeneratorState();
        ChunkGenerator Generator = World.getChunkSource().getGenerator();
        Climate.Sampler Sampler = World.getChunkSource().randomState().sampler();
        int SampleY = QuartPos.fromBlock(Generator.getSeaLevel() + 4);
        ChunkPos Here = new ChunkPos(Origin);
        ChunkPos Best = null;
        double BestDistance = (double) MaxDistance * MaxDistance;
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
            int Rings = Mth.ceil(MaxDistance / 16.0 / Spacing) + 1;
            for (int Rx = -Rings; Rx <= Rings; Rx++) {
                for (int Rz = -Rings; Rz <= Rings; Rz++) {
                    ChunkPos Candidate = Spread.getPotentialStructureChunk(State.getLevelSeed(), Here.x + Rx * Spacing, Here.z + Rz * Spacing);
                    BlockPos At = Spread.getLocatePos(Candidate);
                    double Dx = At.getX() - Origin.getX();
                    double Dz = At.getZ() - Origin.getZ();
                    double Distance = Dx * Dx + Dz * Dz;
                    if (Distance >= BestDistance || !World.getWorldBorder().isWithinBounds(At) || !Spread.isStructureChunk(State, Candidate.x, Candidate.z)) {
                        continue;
                    }
                    Holder<Biome> Sampled = Generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(At.getX() + 8), SampleY, QuartPos.fromBlock(At.getZ() + 8), Sampler);
                    for (Structure Village : Villages) {
                        if (Village.biomes().contains(Sampled)) {
                            Best = Candidate;
                            BestDistance = Distance;
                            break;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(Best);
    }

    public static BlockPos locate(ServerLevel World, ChunkPos Start) {
        for (Holder<StructureSet> Entry : World.getChunkSource().getGeneratorState().possibleStructureSets()) {
            if (Entry.value().placement() instanceof RandomSpreadStructurePlacement Spread
                    && Entry.value().structures().stream().anyMatch(Choice -> Choice.structure().is(StructureTags.VILLAGE))) {
                return Spread.getLocatePos(Start);
            }
        }
        return Start.getWorldPosition();
    }

    public static Optional<BlockPos> centre(ServerLevel World, int ChunkX, int ChunkZ) {
        if (!World.hasChunk(ChunkX, ChunkZ)) {
            return Optional.empty();
        }
        LevelChunk Chunk = World.getChunk(ChunkX, ChunkZ);
        Registry<Structure> Structures = World.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Map.Entry<Structure, StructureStart> Entry : Chunk.getAllStarts().entrySet()) {
            if (Entry.getValue().isValid() && Structures.wrapAsHolder(Entry.getKey()).is(StructureTags.VILLAGE)) {
                return Optional.of(Entry.getValue().getBoundingBox().getCenter());
            }
        }
        return Optional.empty();
    }

    public static int populate(ServerLevel World, BlockPos Centre, @Nullable ServerPlayer Reader, int Count) {
        RandomSource Random = World.getRandom();
        AABB Area = new AABB(Centre).inflate(LocalRadius, LocalHeight, LocalRadius);
        List<Villager> Found = World.getEntitiesOfClass(Villager.class, Area, Villager::isAlive);
        Map<Villager, Double> Scores = new HashMap<>();
        for (Villager Local : Found) {
            Scores.put(Local, (Reader == null ? 0.0 : score(Reader, Local.position())) + Random.nextDouble() * 4.0);
        }
        List<Villager> Locals = new ArrayList<>(Found);
        Locals.sort(Comparator.comparingDouble(Local -> -Scores.get(Local)));
        int Spawned = 0;
        for (int Index = 0; Index < Count; Index++) {
            BlockPos Anchor = Locals.isEmpty() ? Centre : Locals.get(Index % Locals.size()).blockPosition();
            int Inner = Locals.isEmpty() ? 4 : 1;
            int Outer = Locals.isEmpty() ? 18 : 5;
            Optional<BlockPos> Spot = spot(World, Anchor, Reader, Inner, Outer, Random);
            if (Spot.isEmpty() && !Locals.isEmpty()) {
                Spot = spot(World, Centre, Reader, 4, 18, Random);
            }
            if (Spot.isEmpty()) {
                continue;
            }
            Vec3 Place = Vec3.atBottomCenterOf(Spot.get());
            float Yaw = Random.nextFloat() * 360.0f;
            if (Scp031Mimic.spawnAt(World, Place, Yaw).isPresent()) {
                Spawned++;
            }
        }
        return Spawned;
    }

    private static double score(ServerPlayer Reader, Vec3 Where) {
        double Distance = Reader.position().distanceTo(Where);
        double Score = Math.min(Distance, 48.0);
        if (Distance < HiddenDistance) {
            Score -= 40.0;
        }
        if (inView(Reader, Where)) {
            Score -= 12.0;
        }
        return Score;
    }

    private static boolean inView(ServerPlayer Reader, Vec3 Where) {
        Vec3 Toward = Where.add(0.0, 1.0, 0.0).subtract(Reader.getEyePosition());
        double Length = Toward.length();
        return Length < 1.0e-3 || Toward.scale(1.0 / Length).dot(Reader.getViewVector(1.0f)) > ViewCone;
    }

    private static Optional<BlockPos> spot(ServerLevel World, BlockPos Anchor, @Nullable ServerPlayer Reader, int Inner, int Outer, RandomSource Random) {
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < SpotAttempts; Attempt++) {
            Optional<BlockPos> Found = LeadPlanner.standableNear(World, Anchor, Inner, Outer, 4, Random);
            if (Found.isEmpty() || !Scp031Mimic.roomFor(World, Found.get())) {
                continue;
            }
            Vec3 Where = Vec3.atBottomCenterOf(Found.get());
            if (Reader == null || Reader.position().distanceTo(Where) >= HiddenDistance && !inView(Reader, Where)) {
                return Found;
            }
            if (Fallback == null || Reader.position().distanceTo(Where) >= HiddenDistance) {
                Fallback = Found.get();
            }
        }
        return Optional.ofNullable(Fallback);
    }
}
