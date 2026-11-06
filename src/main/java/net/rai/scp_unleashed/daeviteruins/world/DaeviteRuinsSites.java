package net.rai.scp_unleashed.daeviteruins.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.daeviteruins.plan.RuinPages;
import net.rai.scp_unleashed.daeviteruins.plan.RuinPlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class DaeviteRuinsSites {
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int PresetAttempts = 64;
    public static final int Apart = 112;
    public static final int SearchAttempts = 28;
    public static final int GrowthDepth = 40;
    public static final int BorderMargin = 48;
    public static final int VillageClearance = 160;

    public record Pick(BlockPos Origin, Direction Front, int Score) {
    }

    private DaeviteRuinsSites() {
    }

    public static boolean acceptableBiome(Holder<Biome> Found) {
        return !Found.is(BiomeTags.IS_OCEAN) && !Found.is(BiomeTags.IS_DEEP_OCEAN) && !Found.is(BiomeTags.IS_RIVER) && !Found.is(BiomeTags.IS_BEACH)
                && !Found.is(Tags.Biomes.IS_SWAMP) && !Found.is(Tags.Biomes.IS_MOUNTAIN_PEAK) && !Found.is(Tags.Biomes.IS_MUSHROOM);
    }

    public static BlockPos preset(ServerLevel World, BlockPos Origin, int Page, RandomSource Random, List<DaeviteRuin> Others) {
        int Low = RuinPages.minDistance(Page);
        int High = RuinPages.maxDistance(Page);
        WorldBorder Border = World.getWorldBorder();
        int BestX = Origin.getX() + Low;
        int BestZ = Origin.getZ();
        int BestScore = Integer.MAX_VALUE;
        for (int Attempt = 0; Attempt < PresetAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Distance = Low + Random.nextDouble() * (High - Low);
            int X = Origin.getX() + Mth.floor(Math.cos(Angle) * Distance);
            int Z = Origin.getZ() + Mth.floor(Math.sin(Angle) * Distance);
            if (!Border.isWithinBounds(X, Z) || Border.getDistanceToBorder(X, Z) < BorderMargin) {
                continue;
            }
            int Score = 0;
            if (World.getChunkSource().hasChunk(X >> 4, Z >> 4)) {
                Score += 4;
            }
            Holder<Biome> Found = World.getUncachedNoiseBiome(QuartPos.fromBlock(X), QuartPos.fromBlock(World.getSeaLevel() + 4), QuartPos.fromBlock(Z));
            if (!acceptableBiome(Found)) {
                Score += 10;
            }
            for (DaeviteRuin Other : Others) {
                BlockPos There = Other.where();
                double Dx = There.getX() - X;
                double Dz = There.getZ() - Z;
                if (Dx * Dx + Dz * Dz < (double) Apart * Apart) {
                    Score += 20;
                }
            }
            if (Score < BestScore && villageNear(World, new BlockPos(X, 0, Z), VillageClearance)) {
                Score += 12;
            }
            if (Score < BestScore) {
                BestScore = Score;
                BestX = X;
                BestZ = Z;
                if (Score == 0) {
                    break;
                }
            }
        }
        int Y;
        if (World.getChunkSource().getChunkNow(BestX >> 4, BestZ >> 4) != null) {
            Y = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BestX, BestZ);
        } else {
            Y = World.getChunkSource().getGenerator().getBaseHeight(BestX, BestZ, Heightmap.Types.WORLD_SURFACE_WG, World, World.getChunkSource().randomState());
        }
        return new BlockPos(BestX, Math.max(World.getSeaLevel(), Y), BestZ);
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

    public static int ground(Level World, int X, int Z) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z) - 1, Z);
        int Floor = Math.max(World.getMinBuildHeight(), Cursor.getY() - GrowthDepth);
        while (Cursor.getY() > Floor && DaeviteRuinsPalette.growthLike(World.getBlockState(Cursor)) && World.getFluidState(Cursor).isEmpty()) {
            Cursor.move(Direction.DOWN);
        }
        return Cursor.getY();
    }

    public static Direction toward(BlockPos From, BlockPos To) {
        int Dx = To.getX() - From.getX();
        int Dz = To.getZ() - From.getZ();
        if (Dx == 0 && Dz == 0) {
            return Direction.SOUTH;
        }
        return Direction.getNearest(Dx, 0, Dz);
    }

    private static int[] footprint(RuinPlan Plan, BlockPos Origin, Direction Front) {
        int[] Low = Plan.low();
        int[] High = Plan.high();
        int MinX = Integer.MAX_VALUE;
        int MinZ = Integer.MAX_VALUE;
        int MaxX = Integer.MIN_VALUE;
        int MaxZ = Integer.MIN_VALUE;
        for (int[] Corner : new int[][]{{Low[0], Low[2]}, {High[0], Low[2]}, {Low[0], High[2]}, {High[0], High[2]}}) {
            BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, Corner[0], 0, Corner[1]);
            MinX = Math.min(MinX, Pos.getX());
            MinZ = Math.min(MinZ, Pos.getZ());
            MaxX = Math.max(MaxX, Pos.getX());
            MaxZ = Math.max(MaxZ, Pos.getZ());
        }
        return new int[]{MinX, MinZ, MaxX, MaxZ};
    }

    private static boolean loaded(ServerLevel World, int[] Box) {
        for (int ChunkX = (Box[0] - 2) >> 4; ChunkX <= (Box[2] + 2) >> 4; ChunkX++) {
            for (int ChunkZ = (Box[1] - 2) >> 4; ChunkZ <= (Box[3] + 2) >> 4; ChunkZ++) {
                if (World.getChunkSource().getChunkNow(ChunkX, ChunkZ) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean footprintLoaded(ServerLevel World, RuinPlan Plan, BlockPos Origin, Direction Front) {
        return loaded(World, footprint(Plan, Origin, Front));
    }

    public static Optional<Pick> find(ServerLevel World, RuinPlan Plan, BlockPos Around, BlockPos Toward, int Spread, int Mode, RandomSource Random) {
        int[] Low = Plan.low();
        int[] High = Plan.high();
        int Size = Math.max(High[0] - Low[0], High[2] - Low[2]);
        int SlopeLimit = Mode == Strict ? 3 + Size / 12 : Mode == Lenient ? 8 + Size / 6 : Integer.MAX_VALUE;
        List<Pick> Scored = new ArrayList<>();
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            int X = Around.getX();
            int Z = Around.getZ();
            if (Attempt > 0) {
                double Angle = Random.nextDouble() * Mth.TWO_PI;
                double Distance = Math.sqrt(Random.nextDouble()) * Spread;
                X += Mth.floor(Math.cos(Angle) * Distance);
                Z += Mth.floor(Math.sin(Angle) * Distance);
            }
            BlockPos Column = new BlockPos(X, 0, Z);
            Direction Front = toward(Column, Toward);
            int[] Box = footprint(Plan, Column, Front);
            if (!loaded(World, Box)) {
                continue;
            }
            List<Integer> Heights = new ArrayList<>();
            int Wet = 0;
            for (int Sx = Box[0]; Sx <= Box[2]; Sx += 3) {
                for (int Sz = Box[1]; Sz <= Box[3]; Sz += 3) {
                    int Top = ground(World, Sx, Sz);
                    BlockPos Surface = new BlockPos(Sx, Top, Sz);
                    if (!World.getFluidState(Surface).isEmpty() || !World.getFluidState(Surface.above()).isEmpty()) {
                        Wet++;
                    }
                    Heights.add(Top);
                }
            }
            if (Heights.isEmpty()) {
                continue;
            }
            int WetLimit = Mode == Strict ? 0 : Mode == Lenient ? Heights.size() / 4 : Heights.size();
            if (Wet > WetLimit) {
                continue;
            }
            Integer[] Sorted = Heights.toArray(new Integer[0]);
            Arrays.sort(Sorted);
            int Median = Sorted[Sorted.length / 2];
            int Slope = Sorted[Sorted.length - 1] - Sorted[0];
            if (Slope > SlopeLimit || Median + Low[1] <= World.getMinBuildHeight() + 2 || Median + High[1] >= World.getMaxBuildHeight() - 2) {
                continue;
            }
            int Score = Slope * 2 + Wet * 4 + (int) (Math.sqrt((double) (X - Around.getX()) * (X - Around.getX()) + (double) (Z - Around.getZ()) * (Z - Around.getZ())) / 6);
            Scored.add(new Pick(new BlockPos(X, Median, Z), Front, Score));
        }
        Scored.sort(Comparator.comparingInt(Pick::Score));
        for (Pick Option : Scored) {
            if (safe(World, Plan, Option.Origin(), Option.Front())) {
                return Optional.of(Option);
            }
        }
        return Optional.empty();
    }

    public static boolean safe(ServerLevel World, RuinPlan Plan, BlockPos Origin, Direction Front) {
        int[] Box = footprint(Plan, Origin, Front);
        int[] Low = Plan.low();
        int[] High = Plan.high();
        AABB Area = new AABB(Box[0] - 2, Origin.getY() + Low[1] - 2, Box[1] - 2, Box[2] + 3, Origin.getY() + High[1] + 3, Box[3] + 3);
        if (!World.getEntitiesOfClass(Player.class, Area, Player::isAlive).isEmpty()) {
            return false;
        }
        for (int ChunkX = Box[0] >> 4; ChunkX <= Box[2] >> 4; ChunkX++) {
            for (int ChunkZ = Box[1] >> 4; ChunkZ <= Box[3] >> 4; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    return false;
                }
                for (BlockEntity Entity : Chunk.getBlockEntities().values()) {
                    BlockPos Pos = Entity.getBlockPos();
                    if (Area.contains(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5)) {
                        return false;
                    }
                }
            }
        }
        int Reach = Math.max(Box[2] - Box[0], Box[3] - Box[1]) / 2 + 16;
        BlockPos Middle = new BlockPos((Box[0] + Box[2]) / 2, Origin.getY(), (Box[1] + Box[3]) / 2);
        if (World.getPoiManager().getCountInRange(Type -> Type.is(PoiTypeTags.VILLAGE) || Type.is(PoiTypes.NETHER_PORTAL) || Type.is(PoiTypes.LODESTONE)
                || Type.is(PoiTypes.HOME), Middle, Reach, PoiManager.Occupancy.ANY) > 0) {
            return false;
        }
        for (int[] Corner : new int[][]{{Box[0], Box[1]}, {Box[2], Box[1]}, {Box[0], Box[3]}, {Box[2], Box[3]}, {Middle.getX(), Middle.getZ()}}) {
            BlockPos Probe = new BlockPos(Corner[0], Origin.getY(), Corner[1]);
            if (World.structureManager().getStructureWithPieceAt(Probe, StructureTags.VILLAGE).isValid()) {
                return false;
            }
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (long Key : Plan.keys()) {
            BlockPos Pos = DaeviteRuinsPalette.toWorld(Origin, Front, RuinPlan.keyX(Key), RuinPlan.keyY(Key), RuinPlan.keyZ(Key));
            Cursor.set(Pos);
            if (World.isOutsideBuildHeight(Cursor)) {
                continue;
            }
            BlockState There = World.getBlockState(Cursor);
            if (!DaeviteRuinsPalette.natural(There)) {
                return false;
            }
        }
        return true;
    }
}
