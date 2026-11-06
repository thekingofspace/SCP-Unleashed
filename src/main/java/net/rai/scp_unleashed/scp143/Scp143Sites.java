package net.rai.scp_unleashed.scp143;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.scp143.network.Scp143SitePayload;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp143Sites {
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int BiomeAttempts = 160;
    public static final int MaxCandidates = 10;
    public static final double GoodScore = 2.5;
    public static final int InnerRing = 7;
    public static final int OuterRing = 12;
    public static final int StrictSlope = 3;
    public static final int LenientSlope = 6;
    public static final int VillageRadius = 192;
    public static final int StructureRadius = 80;
    public static final int VisitedTicks = 1200;
    public static final int Footprint = 12;
    public static final int LoadedRadius = 18;
    public static final int SearchAttempts = 48;
    public static final int StrictSearch = 24;
    public static final int LenientSearch = 40;
    public static final int Tolerance = 3;
    public static final int CheckTicks = 20;
    private static final Logger Log = LogUtils.getLogger();

    private Scp143Sites() {
    }

    private record Candidate(int X, int Z, int Top, double Score) {
    }

    public static boolean openBiome(Holder<Biome> Found) {
        if (Found.is(Tags.Biomes.IS_SNOWY) || Found.is(Tags.Biomes.IS_FOREST) || Found.is(Tags.Biomes.IS_DENSE_VEGETATION) || Found.is(Tags.Biomes.IS_MOUNTAIN_PEAK)) {
            return false;
        }
        return Found.is(Biomes.PLAINS) || Found.is(Biomes.SUNFLOWER_PLAINS) || Found.is(Biomes.MEADOW) || Found.is(Biomes.SAVANNA) || Found.is(Biomes.SAVANNA_PLATEAU)
                || Found.is(Tags.Biomes.IS_PLAINS) || Found.is(Tags.Biomes.IS_SAVANNA);
    }

    public static boolean dryBiome(Holder<Biome> Found) {
        return !Found.is(BiomeTags.IS_OCEAN) && !Found.is(BiomeTags.IS_DEEP_OCEAN) && !Found.is(BiomeTags.IS_RIVER) && !Found.is(BiomeTags.IS_BEACH)
                && !Found.is(Tags.Biomes.IS_SWAMP) && !Found.is(Tags.Biomes.IS_MOUNTAIN_PEAK);
    }

    private static Holder<Biome> biomeAt(ServerLevel World, int X, int Y, int Z) {
        return World.getUncachedNoiseBiome(QuartPos.fromBlock(X), QuartPos.fromBlock(Y), QuartPos.fromBlock(Z));
    }

    private static int baseHeight(ServerLevel World, int X, int Z, Heightmap.Types Kind) {
        return World.getChunkSource().getGenerator().getBaseHeight(X, Z, Kind, World, World.getChunkSource().randomState());
    }

    public static BlockPos plan(ServerLevel World, BlockPos Origin, int MinDistance, int MaxDistance, RandomSource Random) {
        int Loaded = (World.getServer().getPlayerList().getViewDistance() + 2) * 16;
        int Low = Math.max(MinDistance, Loaded);
        int High = Math.max(Low + 96, MaxDistance);
        int Sample = World.getSeaLevel() + 6;
        List<int[]> Open = new ArrayList<>();
        List<int[]> Dry = new ArrayList<>();
        int[] First = null;
        for (int Attempt = 0; Attempt < BiomeAttempts && Open.size() < MaxCandidates; Attempt++) {
            BlockPos Target = LeadPlanner.pickTarget(World, Origin, Low, High, Random);
            if (World.getChunkSource().hasChunk(Target.getX() >> 4, Target.getZ() >> 4)) {
                continue;
            }
            int[] Column = {Target.getX(), Target.getZ()};
            if (First == null) {
                First = Column;
            }
            Holder<Biome> Found = biomeAt(World, Column[0], Sample, Column[1]);
            if (openBiome(Found)) {
                Open.add(Column);
            } else if (dryBiome(Found) && Dry.size() < MaxCandidates) {
                Dry.add(Column);
            }
        }
        Optional<Candidate> Best = choose(World, Open, Strict);
        if (Best.isEmpty()) {
            Best = choose(World, Dry, Lenient);
        }
        if (Best.isEmpty()) {
            Best = choose(World, Open, Lenient);
        }
        if (Best.isPresent()) {
            return new BlockPos(Best.get().X(), Best.get().Top(), Best.get().Z());
        }
        int[] Fallback = First != null ? First : new int[]{Origin.getX() + Low, Origin.getZ()};
        int Top = Math.max(World.getSeaLevel() + 1, baseHeight(World, Fallback[0], Fallback[1], Heightmap.Types.OCEAN_FLOOR_WG));
        return new BlockPos(Fallback[0], Top, Fallback[1]);
    }

    private static Optional<Candidate> choose(ServerLevel World, List<int[]> Columns, int Mode) {
        List<Candidate> Passed = new ArrayList<>();
        for (int[] Column : Columns) {
            int X = Column[0];
            int Z = Column[1];
            int Top = baseHeight(World, X, Z, Heightmap.Types.OCEAN_FLOOR_WG);
            int Surface = baseHeight(World, X, Z, Heightmap.Types.WORLD_SURFACE_WG);
            if (Surface > Top || Top <= World.getMinBuildHeight() + 4 || Top + 24 >= World.getMaxBuildHeight()) {
                continue;
            }
            if (Mode == Strict && !openBiome(biomeAt(World, X, Top, Z))) {
                continue;
            }
            int Low = Top;
            int High = Top;
            boolean Wet = false;
            for (int Index = 0; Index < 8; Index++) {
                double Angle = Index * Math.PI / 4.0;
                int Ix = X + 1 + (int) Math.round(Math.cos(Angle) * InnerRing);
                int Iz = Z + 1 + (int) Math.round(Math.sin(Angle) * InnerRing);
                int Inner = baseHeight(World, Ix, Iz, Heightmap.Types.OCEAN_FLOOR_WG);
                if (Index % 2 == 0) {
                    Wet |= baseHeight(World, Ix, Iz, Heightmap.Types.WORLD_SURFACE_WG) > Inner;
                }
                Low = Math.min(Low, Inner);
                High = Math.max(High, Inner);
            }
            int Slope = High - Low;
            if (Wet && Mode == Strict || Slope > (Mode == Strict ? StrictSlope : LenientSlope)) {
                continue;
            }
            double Outer = 0.0;
            for (int Index = 0; Index < 8; Index++) {
                double Angle = Index * Math.PI / 4.0 + Math.PI / 8.0;
                Outer += baseHeight(World, X + 1 + (int) Math.round(Math.cos(Angle) * OuterRing), Z + 1 + (int) Math.round(Math.sin(Angle) * OuterRing),
                        Heightmap.Types.OCEAN_FLOOR_WG);
            }
            BlockPos Spot = new BlockPos(X, Top, Z);
            if (structureNear(World, Spot, VillageRadius, StructureRadius)) {
                continue;
            }
            double Rise = Top - Outer / 8.0;
            double Score = Math.max(-2.0, Math.min(4.0, Rise)) * 1.5 - Slope;
            if (World.getUncachedNoiseBiome(QuartPos.fromBlock(X), QuartPos.fromBlock(Top), QuartPos.fromBlock(Z)).is(Biomes.MEADOW)) {
                Score += 0.5;
            }
            Passed.add(new Candidate(X, Z, Top, Score));
            if (Score >= GoodScore && !visited(World, X, Z)) {
                return Optional.of(Passed.get(Passed.size() - 1));
            }
        }
        Passed.sort((A, B) -> Double.compare(B.Score(), A.Score()));
        for (int Index = 0; Index < Math.min(3, Passed.size()); Index++) {
            Candidate Each = Passed.get(Index);
            if (!visited(World, Each.X(), Each.Z())) {
                return Optional.of(Each);
            }
        }
        return Passed.isEmpty() ? Optional.empty() : Optional.of(Passed.get(0));
    }

    private static boolean visited(ServerLevel World, int X, int Z) {
        try {
            Optional<CompoundTag> Saved = World.getChunkSource().chunkMap.read(new ChunkPos(X >> 4, Z >> 4)).join();
            return Saved.isPresent() && Saved.get().getLong("InhabitedTime") > VisitedTicks;
        } catch (RuntimeException Problem) {
            return false;
        }
    }

    public static boolean structureNear(ServerLevel World, BlockPos Centre, int Village, int Other) {
        ChunkGeneratorStructureState State = World.getChunkSource().getGeneratorState();
        ChunkGenerator Generator = World.getChunkSource().getGenerator();
        Climate.Sampler Sampler = World.getChunkSource().randomState().sampler();
        int SampleY = QuartPos.fromBlock(Generator.getSeaLevel() + 4);
        ChunkPos Here = new ChunkPos(Centre);
        for (Holder<StructureSet> Entry : State.possibleStructureSets()) {
            StructureSet Set = Entry.value();
            if (!(Set.placement() instanceof RandomSpreadStructurePlacement Spread)) {
                continue;
            }
            List<Structure> Villages = new ArrayList<>();
            List<Structure> Others = new ArrayList<>();
            for (StructureSet.StructureSelectionEntry Choice : Set.structures()) {
                if (Choice.structure().is(StructureTags.VILLAGE)) {
                    Villages.add(Choice.structure().value());
                } else if (Choice.structure().value().step() == GenerationStep.Decoration.SURFACE_STRUCTURES) {
                    Others.add(Choice.structure().value());
                }
            }
            if (Villages.isEmpty() && Others.isEmpty()) {
                continue;
            }
            int Radius = Villages.isEmpty() ? Other : Village;
            int Spacing = Math.max(1, Spread.spacing());
            int Rings = Mth.ceil(Radius / 16.0 / Spacing) + 1;
            for (int Rx = -Rings; Rx <= Rings; Rx++) {
                for (int Rz = -Rings; Rz <= Rings; Rz++) {
                    ChunkPos Candidate = Spread.getPotentialStructureChunk(State.getLevelSeed(), Here.x + Rx * Spacing, Here.z + Rz * Spacing);
                    BlockPos At = Spread.getLocatePos(Candidate);
                    double Dx = At.getX() - Centre.getX();
                    double Dz = At.getZ() - Centre.getZ();
                    double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                    if (Distance > Radius || !Spread.isStructureChunk(State, Candidate.x, Candidate.z)) {
                        continue;
                    }
                    Holder<Biome> Sampled = Generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(At.getX() + 8), SampleY, QuartPos.fromBlock(At.getZ() + 8), Sampler);
                    for (Structure Town : Villages) {
                        if (Distance <= Village && Town.biomes().contains(Sampled)) {
                            return true;
                        }
                    }
                    for (Structure Landmark : Others) {
                        if (Distance <= Other && Landmark.biomes().contains(Sampled)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean areaLoaded(ServerLevel World, BlockPos Centre, int Radius) {
        for (int Cx = (Centre.getX() - Radius) >> 4; Cx <= (Centre.getX() + Radius) >> 4; Cx++) {
            for (int Cz = (Centre.getZ() - Radius) >> 4; Cz <= (Centre.getZ() + Radius) >> 4; Cz++) {
                if (!World.getChunkSource().hasChunk(Cx, Cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean structureAt(ServerLevel World, BlockPos Base) {
        BoundingBox Area = new BoundingBox(Base.getX() - Footprint, Base.getY() - 4, Base.getZ() - Footprint, Base.getX() + Footprint, Base.getY() + 18, Base.getZ() + Footprint);
        for (int Cx = Area.minX() >> 4; Cx <= Area.maxX() >> 4; Cx++) {
            for (int Cz = Area.minZ() >> 4; Cz <= Area.maxZ() >> 4; Cz++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(Cx, Cz);
                if (Chunk == null) {
                    continue;
                }
                for (Map.Entry<Structure, LongSet> Entry : Chunk.getAllReferences().entrySet()) {
                    boolean[] Hit = {false};
                    World.structureManager().fillStartsForStructure(Entry.getKey(), Entry.getValue(), Start -> {
                        if (Start.getBoundingBox().intersects(Area)) {
                            Hit[0] = true;
                        }
                    });
                    if (Hit[0]) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean crowded(ServerLevel World, BlockPos Base) {
        int MinX = Base.getX() - Footprint;
        int MaxX = Base.getX() + Footprint;
        int MinZ = Base.getZ() - Footprint;
        int MaxZ = Base.getZ() + Footprint;
        for (int Cx = MinX >> 4; Cx <= MaxX >> 4; Cx++) {
            for (int Cz = MinZ >> 4; Cz <= MaxZ >> 4; Cz++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(Cx, Cz);
                if (Chunk == null) {
                    continue;
                }
                for (BlockEntity Entity : Chunk.getBlockEntities().values()) {
                    BlockPos Pos = Entity.getBlockPos();
                    if (Pos.getX() >= MinX && Pos.getX() <= MaxX && Pos.getZ() >= MinZ && Pos.getZ() <= MaxZ && Pos.getY() >= Base.getY() - 4 && Pos.getY() <= Base.getY() + 18) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean wetAt(ServerLevel World, int X, int Ground, int Z) {
        return !World.getFluidState(new BlockPos(X, Ground, Z)).isEmpty() || !World.getFluidState(new BlockPos(X, Ground + 1, Z)).isEmpty();
    }

    private static int slopeAround(ServerLevel World, BlockPos Base) {
        int Low = Integer.MAX_VALUE;
        int High = Integer.MIN_VALUE;
        for (int Dx = -3; Dx <= 4; Dx += 2) {
            for (int Dz = -3; Dz <= 4; Dz += 2) {
                int Ground = Scp143Trees.ground(World, Base.getX() + Dx, Base.getZ() + Dz);
                Low = Math.min(Low, Ground);
                High = Math.max(High, Ground);
            }
        }
        return High - Low;
    }

    private static boolean fits(ServerLevel World, BlockPos Base, int Mode) {
        int Ground = Scp143Trees.ground(World, Base.getX(), Base.getZ());
        if (Math.abs(Ground + 1 - Base.getY()) > Tolerance) {
            return false;
        }
        if (Mode != Forced && wetAt(World, Base.getX(), Ground, Base.getZ())) {
            return false;
        }
        if (Mode == Strict && slopeAround(World, Base) > StrictSlope || Mode == Lenient && slopeAround(World, Base) > LenientSlope) {
            return false;
        }
        return Mode == Forced || !structureAt(World, Base) && !crowded(World, Base);
    }

    private static Optional<BlockPos> search(ServerLevel World, BlockPos Centre, int Radius, int Mode, RandomSource Random) {
        BlockPos Best = null;
        double BestScore = Double.MAX_VALUE;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Reach = 4.0 + Random.nextDouble() * (Radius - 4.0);
            int X = Centre.getX() + (int) Math.round(Math.cos(Angle) * Reach);
            int Z = Centre.getZ() + (int) Math.round(Math.sin(Angle) * Reach);
            if (!areaLoaded(World, new BlockPos(X, 0, Z), LoadedRadius)) {
                continue;
            }
            BlockPos Base = new BlockPos(X, Scp143Trees.ground(World, X, Z) + 1, Z);
            if (!fits(World, Base, Mode)) {
                continue;
            }
            double Score = slopeAround(World, Base) * 3.0 + Reach * 0.1;
            if (Score < BestScore) {
                BestScore = Score;
                Best = Base;
            }
        }
        return Optional.ofNullable(Best);
    }

    public static BlockPos settle(ServerLevel World, BlockPos Planned, RandomSource Random) {
        if (fits(World, Planned, Strict)) {
            return Planned;
        }
        Optional<BlockPos> Found = search(World, Planned, StrictSearch, Strict, Random);
        if (Found.isEmpty()) {
            Found = search(World, Planned, LenientSearch, Lenient, Random);
        }
        if (Found.isPresent()) {
            return Found.get();
        }
        int Ground = Scp143Trees.ground(World, Planned.getX(), Planned.getZ());
        if (Math.abs(Ground + 1 - Planned.getY()) <= Tolerance) {
            return Planned;
        }
        return new BlockPos(Planned.getX(), Math.max(Ground + 1, World.getSeaLevel() + 1), Planned.getZ());
    }

    public static void tick(MinecraftServer Server) {
        ServerLevel World = Server.overworld();
        if (World.getGameTime() % CheckTicks != 0) {
            return;
        }
        Scp143World Data = Scp143World.get(Server);
        BlockPos Planned = Data.planned();
        if (Data.isBuilt() || Planned == null || !areaLoaded(World, Planned, LoadedRadius)) {
            return;
        }
        build(World, Data, Planned);
    }

    public static BlockPos build(ServerLevel World, Scp143World Data, BlockPos Planned) {
        BlockPos Base = settle(World, Planned, World.getRandom());
        try {
            Scp143Trees.placeGrand(World, Base);
        } catch (RuntimeException Problem) {
            Log.error("SCP-143 could not be built at {}", Base, Problem);
            Data.failed();
            return null;
        }
        Data.built(Base);
        PacketDistributor.sendToPlayersInDimension(World, new Scp143SitePayload(true, Base));
        if (!Base.equals(Planned)) {
            Scp143GroveCondition.refreshAll(World.getServer());
        }
        return Base;
    }

    public static BlockPos placeHere(ServerLevel World, BlockPos Base) {
        Scp143World Data = Scp143World.get(World.getServer());
        Scp143Trees.placeGrand(World, Base);
        Data.plan(Base);
        Data.built(Base);
        PacketDistributor.sendToPlayersInDimension(World, new Scp143SitePayload(true, Base));
        Scp143GroveCondition.refreshAll(World.getServer());
        return Base;
    }

    public static boolean confirm(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World().getServer().overworld();
        Scp143World Data = Scp143World.get(World.getServer());
        if (!Data.isBuilt()) {
            BlockPos Planned = Data.planned();
            if (Planned == null || !areaLoaded(World, Planned, LoadedRadius) || build(World, Data, Planned) == null) {
                return false;
            }
        }
        return true;
    }
}
