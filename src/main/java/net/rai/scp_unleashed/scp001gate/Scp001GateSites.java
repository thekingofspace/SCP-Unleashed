package net.rai.scp_unleashed.scp001gate;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.scp076.Scp076Sites;
import net.rai.scp_unleashed.scp5001.world.Scp5001Sites;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Scp001GateSites extends SavedData {
    public static final String DataName = "scp_unleashed_scp001_gate_site";
    public static final int Attempts = 64;
    public static final int Probe = 40;
    public static final int MaxRelief = 10;
    private static final SavedData.Factory<Scp001GateSites> DataFactory = new SavedData.Factory<>(Scp001GateSites::new, Scp001GateSites::load);

    private boolean Planned;
    private int CentreX;
    private int CentreZ;
    private int Ground;
    private Direction Front = Direction.SOUTH;
    private long Seed;
    private boolean Found;
    @Nullable
    private UUID Guardian;
    private final LongSet Done = new LongOpenHashSet();

    public static Scp001GateSites get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean planned() {
        return Planned;
    }

    public BlockPos centre() {
        return new BlockPos(CentreX, Ground, CentreZ);
    }

    public Direction front() {
        return Front;
    }

    public long seed() {
        return Seed;
    }

    public boolean found() {
        return Found;
    }

    public void markFound() {
        if (!Found) {
            Found = true;
            setDirty();
        }
    }

    @Nullable
    public UUID guardian() {
        return Guardian;
    }

    public void guardian(@Nullable UUID Id) {
        Guardian = Id;
        setDirty();
    }

    public Vec3 post() {
        BlockPos Centre = centre();
        return new Vec3(Centre.getX() + 0.5 + Front.getStepX() * Scp001GatePlan.GuardianAhead, Centre.getY() + Scp001GatePlan.GuardianLift,
                Centre.getZ() + 0.5 + Front.getStepZ() * Scp001GatePlan.GuardianAhead);
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
        List<ChunkPos> Found0 = new ArrayList<>();
        int Reach = Scp001GatePlan.PlainRadius + 2;
        for (int Cx = (CentreX - Reach) >> 4; Cx <= (CentreX + Reach) >> 4; Cx++) {
            for (int Cz = (CentreZ - Reach) >> 4; Cz <= (CentreZ + Reach) >> 4; Cz++) {
                Found0.add(new ChunkPos(Cx, Cz));
            }
        }
        return Found0;
    }

    public boolean covers(ChunkPos Pos) {
        if (!Planned) {
            return false;
        }
        int Reach = Scp001GatePlan.PlainRadius + 2;
        return Pos.getMaxBlockX() >= CentreX - Reach && Pos.getMinBlockX() <= CentreX + Reach && Pos.getMaxBlockZ() >= CentreZ - Reach && Pos.getMinBlockZ() <= CentreZ + Reach;
    }

    public ChunkPos centreChunk() {
        return new ChunkPos(centre());
    }

    public void plan(int X, int Z, int Y, Direction Facing, long PlanSeed) {
        Planned = true;
        CentreX = X;
        CentreZ = Z;
        Ground = Y;
        Front = Facing;
        Seed = PlanSeed;
        Found = false;
        Guardian = null;
        Done.clear();
        setDirty();
    }

    public void forget() {
        Planned = false;
        Found = false;
        Guardian = null;
        Done.clear();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putBoolean("Planned", Planned);
        Tag.putInt("X", CentreX);
        Tag.putInt("Z", CentreZ);
        Tag.putInt("Ground", Ground);
        Tag.putString("Front", Front.getSerializedName());
        Tag.putLong("Seed", Seed);
        Tag.putBoolean("Found", Found);
        if (Guardian != null) {
            Tag.putUUID("Guardian", Guardian);
        }
        Tag.put("Done", new LongArrayTag(Done.toLongArray()));
        return Tag;
    }

    public static Scp001GateSites load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp001GateSites Data = new Scp001GateSites();
        Data.Planned = Tag.getBoolean("Planned");
        Data.CentreX = Tag.getInt("X");
        Data.CentreZ = Tag.getInt("Z");
        Data.Ground = Tag.getInt("Ground");
        Direction Facing = Direction.byName(Tag.getString("Front"));
        Data.Front = Facing != null && Facing.getAxis().isHorizontal() ? Facing : Direction.SOUTH;
        Data.Seed = Tag.getLong("Seed");
        Data.Found = Tag.getBoolean("Found");
        Data.Guardian = Tag.hasUUID("Guardian") ? Tag.getUUID("Guardian") : null;
        for (long Each : Tag.getLongArray("Done")) {
            Data.Done.add(Each);
        }
        return Data;
    }

    public static Scp001GateSites ensure(MinecraftServer Server, BlockPos Origin, int MinDistance, int MaxDistance, RandomSource Random) {
        Scp001GateSites Data = get(Server);
        if (Data.planned()) {
            return Data;
        }
        ServerLevel World = Server.overworld();
        BlockPos Spot = pick(World, Origin, MinDistance, MaxDistance, Random);
        Direction Facing = Direction.Plane.HORIZONTAL.getRandomDirection(Random);
        Data.plan(Spot.getX(), Spot.getZ(), Spot.getY(), Facing, World.getSeed() ^ Scp001GatePlan.Salt ^ Spot.asLong());
        Scp001GateEmitter.planned(World, Data);
        return Data;
    }

    private static int base(ServerLevel World, int X, int Z) {
        return World.getChunkSource().getGenerator().getBaseHeight(X, Z, Heightmap.Types.OCEAN_FLOOR_WG, World, World.getChunkSource().randomState());
    }

    public static BlockPos pick(ServerLevel World, BlockPos Origin, int MinDistance, int MaxDistance, RandomSource Random) {
        BlockPos Fallback = null;
        int Sea = World.getSeaLevel();
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            BlockPos Target = LeadPlanner.pickTarget(World, Origin, MinDistance, MaxDistance, Random);
            int X = Target.getX();
            int Z = Target.getZ();
            int Low = Integer.MAX_VALUE;
            int High = Integer.MIN_VALUE;
            int Sum = 0;
            int Count = 0;
            boolean Land = true;
            for (int Dx = -Probe; Dx <= Probe; Dx += Probe) {
                for (int Dz = -Probe; Dz <= Probe; Dz += Probe) {
                    int Height = base(World, X + Dx, Z + Dz);
                    Low = Math.min(Low, Height);
                    High = Math.max(High, Height);
                    Sum += Height;
                    Count++;
                    Land &= Scp5001Sites.landBiome(World.getUncachedNoiseBiome(QuartPos.fromBlock(X + Dx), QuartPos.fromBlock(Sea + 2), QuartPos.fromBlock(Z + Dz)));
                }
            }
            int Average = Math.max(Sea + 1, Math.round(Sum / (float) Count));
            BlockPos Candidate = new BlockPos(X, Average, Z);
            if (Fallback == null) {
                Fallback = Candidate;
            }
            if (Land && High - Low <= MaxRelief && Low >= Sea && Scp5001Sites.farFromStrongholds(World, X, Z) && !Scp076Sites.villageNear(World, Candidate, 160)) {
                return Candidate;
            }
        }
        return Fallback;
    }

    public static boolean confirm(AnomalySpawners.Discovery Discovered) {
        Scp001GateSites Data = get(Discovered.World().getServer());
        if (!Data.planned()) {
            return false;
        }
        Data.markFound();
        return true;
    }
}
