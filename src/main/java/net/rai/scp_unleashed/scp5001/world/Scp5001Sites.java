package net.rai.scp_unleashed.scp5001.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.scp5001.world.plan.Scp5001Plan;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp5001Sites extends SavedData {
    public static final String DataName = "scp_unleashed_scp5001_site";
    public static final int Attempts = 96;
    public static final int StrongholdClearance = 540;
    public static final int StructureClearance = 260;
    public static final int Burial = 17;
    public static final int Bedrock = 6;
    public static final int LegacyVersion = 1;
    public static final int LegacyHalf = 95;
    public static final int LegacyWidth = 191;
    public static final int LegacyHeight = 68;
    public static final int TellOffset = 2;
    private static final SavedData.Factory<Scp5001Sites> DataFactory = new SavedData.Factory<>(Scp5001Sites::new, Scp5001Sites::load);

    private boolean Planned;
    private int CentreX;
    private int CentreZ;
    private int Bottom;
    private long Seed;
    private int ShaftTop = -1;
    private int Version = Scp5001Plan.Version;
    private boolean Told;
    private final LongSet Done = new LongOpenHashSet();

    public static Scp5001Sites get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean planned() {
        return Planned;
    }

    public long seed() {
        return Seed;
    }

    public BlockPos origin() {
        if (legacy()) {
            return new BlockPos(CentreX - LegacyHalf, Bottom, CentreZ - LegacyHalf);
        }
        return new BlockPos(CentreX - Scp5001Plan.CentreX, Bottom, CentreZ - Scp5001Plan.CentreZ);
    }

    public boolean legacy() {
        return Planned && Version < Scp5001Plan.Version;
    }

    public int version() {
        return Version;
    }

    public BlockPos centre() {
        return new BlockPos(CentreX, Bottom, CentreZ);
    }

    public BoundingBox bounds() {
        BlockPos Low = origin();
        if (legacy()) {
            return new BoundingBox(Low.getX(), Low.getY(), Low.getZ(), Low.getX() + LegacyWidth - 1, Low.getY() + LegacyHeight - 1, Low.getZ() + LegacyWidth - 1);
        }
        return new BoundingBox(Low.getX(), Low.getY(), Low.getZ(), Low.getX() + Scp5001Plan.Width - 1, Low.getY() + Scp5001Plan.Height - 1, Low.getZ() + Scp5001Plan.Depth - 1);
    }

    public BlockPos door() {
        BlockPos Low = origin();
        if (legacy()) {
            int Top = ShaftTop >= 0 ? ShaftTop : LegacyHeight + 17;
            return new BlockPos(CentreX, Bottom + Top, CentreZ - LegacyHalf - 18);
        }
        return new BlockPos(Low.getX() + Scp5001Plan.CentreX, Low.getY() + Scp5001Plan.entryY(), Low.getZ());
    }

    public BlockPos target() {
        BlockPos Door = door();
        return legacy() ? Door : Door.above(Scp5001Plan.EntryHeight / 2).north();
    }

    public BlockPos tell() {
        BlockPos Low = origin();
        return new BlockPos(Low.getX() + Scp5001Plan.CentreX, Low.getY(), Low.getZ() - TellOffset);
    }

    public boolean told() {
        return Told;
    }

    public void tellPlaced() {
        Told = true;
        setDirty();
    }

    public boolean done(long Chunk) {
        return Done.contains(Chunk);
    }

    public void finish(long Chunk) {
        if (Done.add(Chunk)) {
            setDirty();
        }
    }

    public int finished() {
        return Done.size();
    }

    public List<ChunkPos> chunks() {
        BoundingBox Box = bounds();
        java.util.ArrayList<ChunkPos> Found = new java.util.ArrayList<>();
        for (int Cx = Box.minX() >> 4; Cx <= Box.maxX() >> 4; Cx++) {
            for (int Cz = Box.minZ() >> 4; Cz <= Box.maxZ() >> 4; Cz++) {
                Found.add(new ChunkPos(Cx, Cz));
            }
        }
        return Found;
    }

    public boolean inside(BlockPos Pos) {
        return Planned && bounds().isInside(Pos);
    }

    public void plan(int X, int Z, int Y, long PlanSeed) {
        Planned = true;
        CentreX = X;
        CentreZ = Z;
        Bottom = Y;
        Seed = PlanSeed;
        ShaftTop = -1;
        Version = Scp5001Plan.Version;
        Told = false;
        Done.clear();
        setDirty();
    }

    public void forget() {
        Planned = false;
        ShaftTop = -1;
        Version = Scp5001Plan.Version;
        Told = false;
        Done.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putBoolean("Planned", Planned);
        Tag.putInt("X", CentreX);
        Tag.putInt("Z", CentreZ);
        Tag.putInt("Bottom", Bottom);
        Tag.putLong("Seed", Seed);
        Tag.putInt("ShaftTop", ShaftTop);
        Tag.putInt("Version", Version);
        Tag.putBoolean("Told", Told);
        Tag.put("Done", new LongArrayTag(Done.toLongArray()));
        return Tag;
    }

    public static Scp5001Sites load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp5001Sites Data = new Scp5001Sites();
        Data.Planned = Tag.getBoolean("Planned");
        Data.CentreX = Tag.getInt("X");
        Data.CentreZ = Tag.getInt("Z");
        Data.Bottom = Tag.getInt("Bottom");
        Data.Seed = Tag.getLong("Seed");
        Data.ShaftTop = Tag.contains("ShaftTop") ? Tag.getInt("ShaftTop") : -1;
        Data.Version = Tag.contains("Version") ? Tag.getInt("Version") : LegacyVersion;
        Data.Told = Tag.getBoolean("Told");
        for (long Each : Tag.getLongArray("Done")) {
            Data.Done.add(Each);
        }
        return Data;
    }

    public static Scp5001Sites ensure(MinecraftServer Server, BlockPos Origin, int MinDistance, int MaxDistance, RandomSource Random) {
        Scp5001Sites Data = get(Server);
        if (Data.planned()) {
            return Data;
        }
        ServerLevel World = Server.overworld();
        Candidate Found = pick(World, Origin, MinDistance, MaxDistance, Random);
        long PlanSeed = World.getSeed() ^ Scp5001Plan.Salt;
        Data.plan(Found.X(), Found.Z(), Found.Bottom(), PlanSeed);
        Scp5001Emitter.planned(World, Data);
        return Data;
    }

    public record Candidate(int X, int Z, int Bottom, int Mode) {
    }

    private static int base(ServerLevel World, int X, int Z, Heightmap.Types Kind) {
        return World.getChunkSource().getGenerator().getBaseHeight(X, Z, Kind, World, World.getChunkSource().randomState());
    }

    public static boolean landBiome(Holder<Biome> Found) {
        return !Found.is(BiomeTags.IS_OCEAN) && !Found.is(BiomeTags.IS_DEEP_OCEAN) && !Found.is(BiomeTags.IS_RIVER);
    }

    public static Candidate pick(ServerLevel World, BlockPos Origin, int MinDistance, int MaxDistance, RandomSource Random) {
        Candidate Lenient = null;
        Candidate Fallback = null;
        Candidate Last = null;
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            BlockPos Target = LeadPlanner.pickTarget(World, Origin, MinDistance, MaxDistance, Random);
            int X = Target.getX();
            int Z = Target.getZ();
            int Lowest = Integer.MAX_VALUE;
            boolean Land = true;
            int Sea = World.getSeaLevel() + 2;
            for (int Dx = -Scp5001Plan.CentreX - 8; Dx <= Scp5001Plan.CentreX + 8; Dx += 32) {
                for (int Dz = -Scp5001Plan.CentreZ - 8; Dz <= Scp5001Plan.CentreZ + 8; Dz += 24) {
                    Lowest = Math.min(Lowest, base(World, X + Dx, Z + Dz, Heightmap.Types.OCEAN_FLOOR_WG));
                    Land &= landBiome(World.getUncachedNoiseBiome(QuartPos.fromBlock(X + Dx), QuartPos.fromBlock(Sea), QuartPos.fromBlock(Z + Dz)));
                }
            }
            int DoorZ = Z - Scp5001Plan.CentreZ - TellOffset;
            boolean Dry = base(World, X, DoorZ, Heightmap.Types.WORLD_SURFACE_WG) <= base(World, X, DoorZ, Heightmap.Types.OCEAN_FLOOR_WG);
            int Bottom = Lowest - Burial - Scp5001Plan.Height;
            boolean Deep = Bottom >= World.getMinBuildHeight() + Bedrock;
            int Clamped = Math.max(World.getMinBuildHeight() + Bedrock, Bottom);
            boolean Far = farFromStrongholds(World, X, Z);
            if (Far && Fallback == null) {
                Fallback = new Candidate(X, Z, Clamped, 2);
            }
            if (Last == null) {
                Last = new Candidate(X, Z, Clamped, 3);
            }
            if (!Deep || !Far) {
                continue;
            }
            if (Lenient == null) {
                Lenient = new Candidate(X, Z, Bottom, 1);
            }
            if (Land && Dry && !structureNear(World, X, Z)) {
                return new Candidate(X, Z, Bottom, 0);
            }
        }
        return Lenient != null ? Lenient : Fallback != null ? Fallback : Last;
    }

    public static boolean farFromStrongholds(ServerLevel World, int X, int Z) {
        ChunkGeneratorStructureState State = World.getChunkSource().getGeneratorState();
        for (Holder<StructureSet> Entry : State.possibleStructureSets()) {
            StructurePlacement Placement = Entry.value().placement();
            if (!(Placement instanceof ConcentricRingsStructurePlacement Rings)) {
                continue;
            }
            List<ChunkPos> Positions = State.getRingPositionsFor(Rings);
            if (Positions == null) {
                continue;
            }
            for (ChunkPos Each : Positions) {
                double Dx = Each.getMiddleBlockX() - X;
                double Dz = Each.getMiddleBlockZ() - Z;
                if (Dx * Dx + Dz * Dz < (double) StrongholdClearance * StrongholdClearance) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean structureNear(ServerLevel World, int X, int Z) {
        ChunkGeneratorStructureState State = World.getChunkSource().getGeneratorState();
        ChunkGenerator Generator = World.getChunkSource().getGenerator();
        Climate.Sampler Sampler = World.getChunkSource().randomState().sampler();
        ChunkPos Here = new ChunkPos(new BlockPos(X, 0, Z));
        for (Holder<StructureSet> Entry : State.possibleStructureSets()) {
            StructureSet Set = Entry.value();
            if (!(Set.placement() instanceof RandomSpreadStructurePlacement Spread)) {
                continue;
            }
            for (StructureSet.StructureSelectionEntry Choice : Set.structures()) {
                boolean City = Choice.structure().is(BuiltinStructures.ANCIENT_CITY);
                boolean Trial = Choice.structure().is(BuiltinStructures.TRIAL_CHAMBERS);
                if (!City && !Trial) {
                    continue;
                }
                Structure Kind = Choice.structure().value();
                int SampleY = QuartPos.fromBlock(City ? -27 : -30);
                int Spacing = Math.max(1, Spread.spacing());
                int Rings = Mth.ceil(StructureClearance / 16.0 / Spacing) + 1;
                for (int Rx = -Rings; Rx <= Rings; Rx++) {
                    for (int Rz = -Rings; Rz <= Rings; Rz++) {
                        ChunkPos Candidate = Spread.getPotentialStructureChunk(State.getLevelSeed(), Here.x + Rx * Spacing, Here.z + Rz * Spacing);
                        double Dx = Candidate.getMiddleBlockX() - X;
                        double Dz = Candidate.getMiddleBlockZ() - Z;
                        if (Dx * Dx + Dz * Dz > (double) StructureClearance * StructureClearance || !Spread.isStructureChunk(State, Candidate.x, Candidate.z)) {
                            continue;
                        }
                        Holder<Biome> Sampled = Generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(Candidate.getMiddleBlockX()), SampleY,
                                QuartPos.fromBlock(Candidate.getMiddleBlockZ()), Sampler);
                        if (Kind.biomes().contains(Sampled)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean confirm(AnomalySpawners.Discovery Found) {
        return get(Found.World().getServer()).planned();
    }

    @Nullable
    public static BoundingBox siteBounds(MinecraftServer Server) {
        Scp5001Sites Data = get(Server);
        return Data.planned() ? Data.bounds() : null;
    }
}
