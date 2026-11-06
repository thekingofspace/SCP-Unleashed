package net.rai.scp_unleashed.scp4666;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp4666.block.Scp4666FrozenGatewayBlock;

import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp4666Builder {
    public static final long Seed = 46661225L;
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    public static final int ChunksPerTick = 3;
    public static final double CandidateSpacing = 3.0;
    public static final double TreeGap = 4.4;
    public static final int Neighbourhood = 2;
    public static final double CrownMargin = 3.5;
    public static final double DensityLow = 0.45;
    public static final double DensityHigh = 0.95;
    public static final int MaxDepth = 4;
    public static final int DriftReach = 2;
    public static final double PuddleLevel = 0.93;
    public static final int ClearAbove = 26;
    public static final int StoneBelow = 6;
    public static final double PathSweep = 1.5;
    public static final int ShelterDepth = 2;
    public static final int ThawPerTick = 4;
    public static final int SketchMargin = 6;
    private static final Map<MinecraftServer, Job> Jobs = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Planned> Plans = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Set<Long>> Melting = new ConcurrentHashMap<>();
    private static final Logger Log = LogUtils.getLogger();

    private static final class Job {
        final Deque<ChunkPos> Terrain = new ArrayDeque<>();
        final Deque<ChunkPos> Trees = new ArrayDeque<>();
        boolean Finished;
    }

    private Scp4666Builder() {
    }

    public static boolean pending(MinecraftServer Server) {
        return Jobs.containsKey(Server);
    }

    public static void schedule(ServerLevel World) {
        Job Work = new Job();
        for (ChunkPos Chunk : chunks(Scp4666Map.get(World.getServer()))) {
            Work.Terrain.add(Chunk);
            Work.Trees.add(Chunk);
        }
        Jobs.put(World.getServer(), Work);
        clearEntities(World);
    }

    public static void forget(MinecraftServer Server) {
        Jobs.remove(Server);
        Plans.remove(Server);
        Melting.remove(Server);
    }

    public static void tick(ServerLevel World) {
        Job Work = Jobs.get(World.getServer());
        if (Work == null) {
            return;
        }
        for (int Step = 0; Step < ChunksPerTick; Step++) {
            if (!advance(World, Work)) {
                break;
            }
        }
    }

    public static void buildNow(ServerLevel World) {
        if (!Jobs.containsKey(World.getServer())) {
            schedule(World);
        }
        Job Work = Jobs.get(World.getServer());
        while (Work != null && advance(World, Work)) {
            Work = Jobs.get(World.getServer());
        }
    }

    private static boolean advance(ServerLevel World, Job Work) {
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        if (!Work.Terrain.isEmpty()) {
            ChunkPos Next = Work.Terrain.poll();
            World.getChunk(Next.x, Next.z);
            terrain(new LevelCanvas(World), Layout, Next);
            return true;
        }
        if (!Work.Trees.isEmpty()) {
            trees(new LevelCanvas(World), Layout, Work.Trees.poll());
            return true;
        }
        if (!Work.Finished) {
            Work.Finished = true;
            finish(World, Layout);
            Jobs.remove(World.getServer());
            Scp4666State.get(World.getServer()).markBuilt(Scp4666World.Signature);
        }
        return false;
    }

    public static int migrate(ServerLevel World) {
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        HolderLookup<Block> Lookup = World.holderLookup(Registries.BLOCK);
        int From = Scp4666State.get(World.getServer()).built();
        int Changed = 0;
        for (Scp4666Map.Patched Entry : Layout.Patch) {
            if (Entry.Since() <= From) {
                continue;
            }
            try {
                BlockState Wanted = BlockStateParser.parseForBlock(Lookup, Entry.State(), false).blockState();
                BlockState Found = World.getBlockState(Entry.Pos());
                if (Entry.Fill() && !Found.isAir()) {
                    continue;
                }
                if (Found != Wanted) {
                    World.setBlock(Entry.Pos(), Wanted, Flags);
                    Changed++;
                }
            } catch (CommandSyntaxException Problem) {
                Log.warn("SCP-4666 cabin patch has a bad state {}", Entry.State());
            }
        }
        Changed += thaw(World);
        Scp4666State State = Scp4666State.get(World.getServer());
        State.markBuilt(Scp4666World.Signature);
        Scp4666Heat.refresh(World, Layout, State);
        return Changed;
    }

    public interface Canvas {
        BlockState getBlockState(BlockPos Pos);

        boolean setBlock(BlockPos Pos, BlockState State, int Flags);
    }

    public record LevelCanvas(ServerLevel World) implements Canvas {
        @Override
        public BlockState getBlockState(BlockPos Pos) {
            return World.getBlockState(Pos);
        }

        @Override
        public boolean setBlock(BlockPos Pos, BlockState State, int Flags) {
            return World.setBlock(Pos, State, Flags);
        }
    }

    public static final class Sketch implements Canvas {
        private final int MinX;
        private final int MinY;
        private final int MinZ;
        private final int SizeX;
        private final int SizeY;
        private final int SizeZ;
        private final BlockState[] Cells;

        public Sketch(int MinX, int MinY, int MinZ, int MaxX, int MaxY, int MaxZ) {
            this.MinX = MinX;
            this.MinY = MinY;
            this.MinZ = MinZ;
            this.SizeX = MaxX - MinX + 1;
            this.SizeY = MaxY - MinY + 1;
            this.SizeZ = MaxZ - MinZ + 1;
            this.Cells = new BlockState[SizeX * SizeY * SizeZ];
        }

        private int index(BlockPos Pos) {
            int X = Pos.getX() - MinX;
            int Y = Pos.getY() - MinY;
            int Z = Pos.getZ() - MinZ;
            if (X < 0 || Y < 0 || Z < 0 || X >= SizeX || Y >= SizeY || Z >= SizeZ) {
                return -1;
            }
            return (Y * SizeZ + Z) * SizeX + X;
        }

        @Override
        public BlockState getBlockState(BlockPos Pos) {
            int Index = index(Pos);
            BlockState Found = Index < 0 ? null : Cells[Index];
            return Found == null ? Blocks.AIR.defaultBlockState() : Found;
        }

        @Override
        public boolean setBlock(BlockPos Pos, BlockState State, int Flags) {
            int Index = index(Pos);
            if (Index >= 0) {
                Cells[Index] = State;
            }
            return Index >= 0;
        }
    }

    public static Sketch sketch(ServerLevel World) {
        Planned Found = cabin(World);
        if (Found.Woods()[0] != null) {
            return Found.Woods()[0];
        }
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        int Reach = Scp4666World.RegionRadius + SketchMargin;
        Sketch Made = new Sketch(Layout.Centre.getX() - Reach, Layout.Ground, Layout.Centre.getZ() - Reach, Layout.Centre.getX() + Reach,
                Layout.Ground + ClearAbove + 2, Layout.Centre.getZ() + Reach);
        List<ChunkPos> Chunks = chunks(Layout);
        for (ChunkPos Chunk : Chunks) {
            terrain(Made, Layout, Chunk);
        }
        for (ChunkPos Chunk : Chunks) {
            trees(Made, Layout, Chunk);
        }
        Found.Woods()[0] = Made;
        return Made;
    }

    public static List<ChunkPos> chunks(Scp4666Map Layout) {
        List<ChunkPos> Found = new ArrayList<>();
        int Low = Math.floorDiv(-Scp4666World.RegionRadius, 16);
        int High = Math.floorDiv(Scp4666World.RegionRadius, 16);
        int Cx = Layout.Centre.getX() >> 4;
        int Cz = Layout.Centre.getZ() >> 4;
        for (int X = Low; X <= High; X++) {
            for (int Z = Low; Z <= High; Z++) {
                Found.add(new ChunkPos(Cx + X, Cz + Z));
            }
        }
        return Found;
    }

    public record Planned(Map<BlockPos, BlockState> Cells, BlockPos Low, BlockPos High, Sketch[] Woods) {
        public boolean holds(BlockPos Pos) {
            return Pos.getX() >= Low.getX() && Pos.getX() <= High.getX() && Pos.getY() >= Low.getY() && Pos.getY() <= High.getY() && Pos.getZ() >= Low.getZ() && Pos.getZ() <= High.getZ();
        }
    }

    public static Planned cabin(ServerLevel World) {
        Planned Found = Plans.get(World.getServer());
        if (Found != null) {
            return Found;
        }
        Map<BlockPos, BlockState> Cells = new HashMap<>();
        BlockPos Origin = Scp4666Map.get(World.getServer()).TemplateOrigin;
        BlockPos Far = Origin;
        Optional<StructureTemplate> Template = World.getStructureManager().get(Scp4666Map.Template);
        if (Template.isPresent()) {
            Far = Origin.offset(Template.get().getSize()).offset(-1, -1, -1);
            CompoundTag Saved = Template.get().save(new CompoundTag());
            HolderLookup<Block> Lookup = World.holderLookup(Registries.BLOCK);
            List<BlockState> Palette = new ArrayList<>();
            for (Tag Raw : Saved.getList("palette", Tag.TAG_COMPOUND)) {
                Palette.add(NbtUtils.readBlockState(Lookup, (CompoundTag) Raw));
            }
            for (Tag Raw : Saved.getList("blocks", Tag.TAG_COMPOUND)) {
                CompoundTag Row = (CompoundTag) Raw;
                ListTag Spot = Row.getList("pos", Tag.TAG_INT);
                int Index = Row.getInt("state");
                if (Spot.size() == 3 && Index >= 0 && Index < Palette.size()) {
                    Cells.put(Origin.offset(Spot.getInt(0), Spot.getInt(1), Spot.getInt(2)), Palette.get(Index));
                }
            }
        }
        Found = new Planned(Cells, Origin, Far, new Sketch[1]);
        Plans.put(World.getServer(), Found);
        return Found;
    }

    public static int designedSnow(ServerLevel World, Scp4666Map Layout, BlockPos Pos) {
        BlockState Wanted = cabin(World).Cells().get(Pos);
        if (Wanted == null) {
            Wanted = sketch(World).getBlockState(Pos);
        }
        return Wanted.is(Blocks.SNOW) ? Wanted.getValue(SnowLayerBlock.LAYERS) : Wanted.is(Blocks.SNOW_BLOCK) ? 8 : 0;
    }

    public static int excess(ServerLevel World, Scp4666Map Layout, BlockPos Pos) {
        BlockState Found = World.getBlockState(Pos);
        int Layers = Found.is(Blocks.SNOW) ? Found.getValue(SnowLayerBlock.LAYERS) : Found.is(Blocks.SNOW_BLOCK) ? 8 : 0;
        return Layers == 0 ? 0 : Math.max(0, Layers - designedSnow(World, Layout, Pos));
    }

    private static int sweep(ServerLevel World, Scp4666Map Layout, BlockPos.MutableBlockPos Probe) {
        if (excess(World, Layout, Probe) == 0) {
            return 0;
        }
        int Wanted = designedSnow(World, Layout, Probe);
        World.setBlock(Probe, Wanted == 0 ? Blocks.AIR.defaultBlockState() : snow(Wanted), Flags);
        return 1;
    }

    public static boolean covered(Scp4666Map Layout, int X, int Z) {
        return Math.abs(X - Layout.Centre.getX()) <= Scp4666World.RegionRadius + SketchMargin - 1 && Math.abs(Z - Layout.Centre.getZ()) <= Scp4666World.RegionRadius + SketchMargin - 1;
    }

    public static int thawChunk(ServerLevel World, ChunkPos Chunk) {
        if (!World.hasChunk(Chunk.x, Chunk.z)) {
            return 0;
        }
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        BlockPos.MutableBlockPos Probe = new BlockPos.MutableBlockPos();
        int Changed = 0;
        for (int X = Chunk.getMinBlockX(); X <= Chunk.getMaxBlockX(); X++) {
            for (int Z = Chunk.getMinBlockZ(); Z <= Chunk.getMaxBlockZ(); Z++) {
                if (!covered(Layout, X, Z)) {
                    continue;
                }
                for (int Y = Layout.Ground + 1; Y <= Layout.Ground + ClearAbove + 1; Y++) {
                    Changed += sweep(World, Layout, Probe.set(X, Y, Z));
                }
            }
        }
        return Changed;
    }

    public static int thaw(ServerLevel World) {
        int Changed = 0;
        for (ChunkPos Chunk : chunks(Scp4666Map.get(World.getServer()))) {
            Changed += thawChunk(World, Chunk);
        }
        return Changed;
    }

    public static void queueThaw(MinecraftServer Server, ChunkPos Chunk) {
        Melting.computeIfAbsent(Server, Key -> ConcurrentHashMap.newKeySet()).add(Chunk.toLong());
    }

    public static void queueAll(ServerLevel World) {
        for (ChunkPos Chunk : chunks(Scp4666Map.get(World.getServer()))) {
            queueThaw(World.getServer(), Chunk);
        }
    }

    public static int queued(MinecraftServer Server) {
        Set<Long> Found = Melting.get(Server);
        return Found == null ? 0 : Found.size();
    }

    public static void thawTick(ServerLevel World) {
        Set<Long> Waiting = Melting.get(World.getServer());
        if (Waiting == null || Waiting.isEmpty() || pending(World.getServer())) {
            return;
        }
        int Done = 0;
        for (Long Packed : new ArrayList<>(Waiting)) {
            if (Done >= ThawPerTick) {
                break;
            }
            ChunkPos Chunk = new ChunkPos(Packed);
            Waiting.remove(Packed);
            if (World.hasChunk(Chunk.x, Chunk.z)) {
                thawChunk(World, Chunk);
                Done++;
            }
        }
    }

    public static void clearEntities(ServerLevel World) {
        List<Entity> Doomed = new ArrayList<>();
        for (Entity Found : World.getAllEntities()) {
            if (!(Found instanceof Player) && (!Scp4666World.standingIn() || Scp4666World.nearSite(World.getServer(), Found.blockPosition()))) {
                Doomed.add(Found);
            }
        }
        for (Entity Found : Doomed) {
            Found.discard();
        }
    }

    private static boolean inRegion(Scp4666Map Layout, int X, int Z) {
        return Math.abs(X - Layout.Centre.getX()) <= Scp4666World.RegionRadius && Math.abs(Z - Layout.Centre.getZ()) <= Scp4666World.RegionRadius;
    }

    private static void set(Canvas World, int X, int Y, int Z, BlockState State) {
        BlockPos Pos = new BlockPos(X, Y, Z);
        if (World.getBlockState(Pos) != State) {
            World.setBlock(Pos, State, Flags);
        }
    }

    public static int snowDepth(Scp4666Map Layout, int X, int Z) {
        if (onPath(Layout, X, Z, 1.3)) {
            return 1;
        }
        double Field = noise(X / 13.0, Z / 13.0, 11) * 0.7 + noise(X / 5.0, Z / 5.0, 23) * 0.3;
        int Depth = 1 + (int) Math.floor(Field * 3.4);
        if (onPath(Layout, X, Z, 2.4)) {
            Depth = Math.min(Depth, 2);
        }
        Depth = Math.min(Depth, shelter(Layout, X, Z));
        return Math.max(1, Math.min(MaxDepth, Depth));
    }

    public static int shelter(Scp4666Map Layout, int X, int Z) {
        int Dx = Math.max(0, Math.max(Layout.TemplateOrigin.getX() - X, X - Layout.TemplateFar.getX()));
        int Dz = Math.max(0, Math.max(Layout.TemplateOrigin.getZ() - Z, Z - Layout.TemplateFar.getZ()));
        return ShelterDepth + Math.max(Dx, Dz) / 2;
    }

    public static boolean nearCabin(Scp4666Map Layout, int X, int Z, int Reach) {
        return X >= Layout.CabinMinX - Reach && X <= Layout.CabinMaxX + Reach && Z >= Layout.CabinMinZ - Reach && Z <= Layout.CabinMaxZ + Reach;
    }

    public static double noise(double X, double Z, int Salt) {
        int X0 = Mth.floor(X);
        int Z0 = Mth.floor(Z);
        double Fx = X - X0;
        double Fz = Z - Z0;
        double Sx = Fx * Fx * (3.0 - 2.0 * Fx);
        double Sz = Fz * Fz * (3.0 - 2.0 * Fz);
        double Near = Mth.lerp(Sx, lattice(X0, Z0, Salt), lattice(X0 + 1, Z0, Salt));
        double Far = Mth.lerp(Sx, lattice(X0, Z0 + 1, Salt), lattice(X0 + 1, Z0 + 1, Salt));
        return Mth.lerp(Sz, Near, Far);
    }

    public static double lattice(int X, int Z, int Salt) {
        long Hash = X * 341873128712L + Z * 132897987541L + Salt * 2654435761L ^ Seed;
        Hash = (Hash ^ (Hash >>> 29)) * 0xBF58476D1CE4E5B9L;
        Hash ^= Hash >>> 32;
        return (Hash & 0xFFFFFFL) / (double) 0xFFFFFFL;
    }

    public static boolean onPath(Scp4666Map Layout, int X, int Z, double Width) {
        Vec3 Point = new Vec3(X + 0.5, 0.0, Z + 0.5);
        for (int Index = 0; Index + 1 < Layout.Path.size(); Index++) {
            BlockPos A = Layout.Path.get(Index);
            BlockPos B = Layout.Path.get(Index + 1);
            Vec3 First = new Vec3(A.getX() + 0.5, 0.0, A.getZ() + 0.5);
            Vec3 Second = new Vec3(B.getX() + 0.5, 0.0, B.getZ() + 0.5);
            Vec3 Along = Second.subtract(First);
            double Length = Along.lengthSqr();
            double Share = Length < 1.0E-6 ? 0.0 : Math.max(0.0, Math.min(1.0, Point.subtract(First).dot(Along) / Length));
            if (First.add(Along.scale(Share)).distanceToSqr(Point) <= Width * Width) {
                return true;
            }
        }
        return false;
    }

    private static void terrain(Canvas World, Scp4666Map Layout, ChunkPos Chunk) {
        int Ground = Layout.Ground;
        BlockState Stone = Blocks.STONE.defaultBlockState();
        BlockState Dirt = Blocks.DIRT.defaultBlockState();
        BlockState Grass = Blocks.GRASS_BLOCK.defaultBlockState().setValue(SnowyDirtBlock.SNOWY, true);
        BlockState Air = Blocks.AIR.defaultBlockState();
        for (int Dx = 0; Dx < 16; Dx++) {
            for (int Dz = 0; Dz < 16; Dz++) {
                int X = Chunk.getMinBlockX() + Dx;
                int Z = Chunk.getMinBlockZ() + Dz;
                if (!inRegion(Layout, X, Z)) {
                    for (int Y = Ground - StoneBelow; Y <= Ground + ClearAbove; Y++) {
                        set(World, X, Y, Z, Air);
                    }
                    continue;
                }
                for (int Y = Ground - StoneBelow; Y < Ground - 1; Y++) {
                    set(World, X, Y, Z, Stone);
                }
                set(World, X, Ground - 1, Z, Dirt);
                set(World, X, Ground, Z, Grass);
                set(World, X, Ground + 1, Z, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, snowDepth(Layout, X, Z)));
                for (int Y = Ground + 2; Y <= Ground + ClearAbove; Y++) {
                    set(World, X, Y, Z, Air);
                }
            }
        }
    }

    public static boolean treeFree(Scp4666Map Layout, double X, double Z) {
        double Dx = X - (Layout.Centre.getX() + 0.5);
        double Dz = Z - (Layout.Centre.getZ() + 0.5);
        double FromCentre = Math.sqrt(Dx * Dx + Dz * Dz);
        if (FromCentre < Scp4666World.CabinClearing + CrownMargin) {
            return false;
        }
        if (Math.abs(FromCentre - Scp4666World.StalkRadius) < Scp4666World.StalkHalfWidth + 1.0) {
            return false;
        }
        if (Layout.Spawn.distanceToSqr(X, Layout.Spawn.y, Z) < Scp4666World.SpawnClearing * Scp4666World.SpawnClearing) {
            return false;
        }
        Vec3 Gate = Vec3.atCenterOf(Layout.GateOrigin);
        if (Math.abs(Gate.x - X) < 4.0 && Math.abs(Gate.z - Z) < 3.0) {
            return false;
        }
        return !onPath(Layout, (int) Math.floor(X), (int) Math.floor(Z), 3.2);
    }

    private record Candidate(double X, double Z, double Priority, boolean Live) {
    }

    private static Candidate candidate(Scp4666Map Layout, int Gx, int Gz) {
        RandomSource Random = RandomSource.create(Seed ^ (Gx * 73856093L) ^ (Gz * 19349663L));
        double X = (Gx + Random.nextDouble()) * CandidateSpacing;
        double Z = (Gz + Random.nextDouble()) * CandidateSpacing;
        double Priority = Random.nextDouble();
        double Density = DensityLow + (DensityHigh - DensityLow) * noise(X / 17.0, Z / 17.0, 5);
        boolean Live = Random.nextDouble() < Density && inRegion(Layout, Mth.floor(X), Mth.floor(Z)) && treeFree(Layout, X, Z);
        return new Candidate(X, Z, Priority, Live);
    }

    private static boolean chosen(Scp4666Map Layout, int Gx, int Gz, Candidate Own) {
        if (!Own.Live()) {
            return false;
        }
        for (int Dx = -Neighbourhood; Dx <= Neighbourhood; Dx++) {
            for (int Dz = -Neighbourhood; Dz <= Neighbourhood; Dz++) {
                if (Dx == 0 && Dz == 0) {
                    continue;
                }
                Candidate Other = candidate(Layout, Gx + Dx, Gz + Dz);
                double Gx2 = Other.X() - Own.X();
                double Gz2 = Other.Z() - Own.Z();
                if (Other.Live() && Other.Priority() > Own.Priority() && Gx2 * Gx2 + Gz2 * Gz2 < TreeGap * TreeGap) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void trees(Canvas World, Scp4666Map Layout, ChunkPos Chunk) {
        int Low = Mth.floor(Chunk.getMinBlockX() / CandidateSpacing) - 1;
        int High = Mth.floor(Chunk.getMaxBlockX() / CandidateSpacing) + 1;
        int Back = Mth.floor(Chunk.getMinBlockZ() / CandidateSpacing) - 1;
        int Front = Mth.floor(Chunk.getMaxBlockZ() / CandidateSpacing) + 1;
        for (int Gx = Low; Gx <= High; Gx++) {
            for (int Gz = Back; Gz <= Front; Gz++) {
                Candidate Own = candidate(Layout, Gx, Gz);
                int X = Mth.floor(Own.X());
                int Z = Mth.floor(Own.Z());
                if (X < Chunk.getMinBlockX() || X > Chunk.getMaxBlockX() || Z < Chunk.getMinBlockZ() || Z > Chunk.getMaxBlockZ() || !chosen(Layout, Gx, Gz, Own)) {
                    continue;
                }
                RandomSource Random = RandomSource.create(Seed ^ (Gx * 83492791L) ^ (Gz * 29791L) ^ 77L);
                float Kind = Random.nextFloat();
                if (Kind < 0.7f) {
                    spruce(World, Layout, X, Z, 9 + Random.nextInt(7), Random);
                } else if (Kind < 0.84f) {
                    spruce(World, Layout, X, Z, 5 + Random.nextInt(3), Random);
                } else if (Kind < 0.9f) {
                    stump(World, Layout, X, Z, Random);
                } else if (Kind < 0.95f) {
                    boulder(World, Layout, X, Z, Random);
                } else {
                    fallen(World, Layout, X, Z, Random);
                }
            }
        }
        puddles(World, Layout, Chunk);
    }

    private static BlockState leaves() {
        return Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1);
    }

    private static BlockState snow(int Layers) {
        return Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, Math.max(1, Math.min(MaxDepth + 1, Layers)));
    }

    private static boolean open(Canvas World, int X, int Y, int Z) {
        BlockState Found = World.getBlockState(new BlockPos(X, Y, Z));
        return Found.isAir() || Found.is(Blocks.SNOW);
    }

    private static void deepen(Canvas World, int X, int Y, int Z) {
        BlockPos Pos = new BlockPos(X, Y, Z);
        BlockState Found = World.getBlockState(Pos);
        if (Found.is(Blocks.SNOW)) {
            World.setBlock(Pos, snow(Found.getValue(SnowLayerBlock.LAYERS) + 1), Flags);
        }
    }

    private static void spruce(Canvas World, Scp4666Map Layout, int X, int Z, int Height, RandomSource Random) {
        int Base = Layout.Ground + 1;
        BlockState Log = Blocks.SPRUCE_LOG.defaultBlockState();
        for (int Y = Base; Y < Base + Height; Y++) {
            set(World, X, Y, Z, Log);
        }
        int Bottom = Base + Math.max(2, Height / 4);
        int Top = Base + Height;
        int Radius = Height >= 9 ? 3 : 2;
        for (int Y = Bottom; Y <= Top; Y++) {
            double Share = (double) (Y - Bottom) / Math.max(1, Top - Bottom);
            int Reach = (int) Math.round(Radius * (1.0 - Share) + ((Y - Bottom) % 2 == 0 ? 0.4 : -0.3));
            Reach = Math.max(Y >= Top - 1 ? 0 : 1, Reach);
            for (int Dx = -Reach; Dx <= Reach; Dx++) {
                for (int Dz = -Reach; Dz <= Reach; Dz++) {
                    if (Math.abs(Dx) + Math.abs(Dz) > Reach + (Reach > 1 ? 1 : 0)) {
                        continue;
                    }
                    if (Dx == 0 && Dz == 0 && Y < Top) {
                        continue;
                    }
                    if (open(World, X + Dx, Y, Z + Dz)) {
                        World.setBlock(new BlockPos(X + Dx, Y, Z + Dz), leaves(), Flags);
                    }
                }
            }
        }
        set(World, X, Top + 1, Z, leaves());
        int Reach = Radius + 1;
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                for (int Y = Top + 2; Y >= Bottom; Y--) {
                    BlockPos Spot = new BlockPos(X + Dx, Y, Z + Dz);
                    BlockState Found = World.getBlockState(Spot);
                    if (Found.is(Blocks.SPRUCE_LEAVES)) {
                        if (World.getBlockState(Spot.above()).isAir()) {
                            World.setBlock(Spot.above(), snow(1 + Random.nextInt(2)), Flags);
                        }
                        break;
                    }
                    if (!Found.isAir() && !Found.is(Blocks.SNOW)) {
                        break;
                    }
                }
            }
        }
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            deepen(World, X + Side.getStepX(), Base, Z + Side.getStepZ());
        }
    }

    private static void stump(Canvas World, Scp4666Map Layout, int X, int Z, RandomSource Random) {
        int Base = Layout.Ground + 1;
        if (!open(World, X, Base, Z) || !open(World, X, Base + 1, Z)) {
            return;
        }
        boolean Tall = Random.nextFloat() < 0.35f;
        set(World, X, Base, Z, Blocks.SPRUCE_LOG.defaultBlockState());
        if (Tall) {
            set(World, X, Base + 1, Z, Blocks.SPRUCE_LOG.defaultBlockState());
        }
        set(World, X, Base + (Tall ? 2 : 1), Z, snow(2));
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            deepen(World, X + Side.getStepX(), Base, Z + Side.getStepZ());
        }
    }

    private static void boulder(Canvas World, Scp4666Map Layout, int X, int Z, RandomSource Random) {
        int Base = Layout.Ground + 1;
        for (int Dx = 0; Dx <= 1; Dx++) {
            for (int Dz = 0; Dz <= 1; Dz++) {
                if (!open(World, X + Dx, Base, Z + Dz) || !open(World, X + Dx, Base + 1, Z + Dz)) {
                    return;
                }
            }
        }
        BlockState[] Stones = {Blocks.STONE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState()};
        int Main = Random.nextInt(2);
        for (int Dx = 0; Dx <= 1; Dx++) {
            for (int Dz = 0; Dz <= 1; Dz++) {
                if (Dx + Dz == 2 && Random.nextBoolean()) {
                    set(World, X + Dx, Base, Z + Dz, snow(3));
                    continue;
                }
                set(World, X + Dx, Base, Z + Dz, Random.nextFloat() < 0.7f ? Stones[Main] : Stones[2 + Random.nextInt(2)]);
                set(World, X + Dx, Base + 1, Z + Dz, snow(2 + Random.nextInt(2)));
            }
        }
        int Peak = Random.nextInt(2);
        set(World, X + Peak, Base + 1, Z, Stones[Main]);
        set(World, X + Peak, Base + 2, Z, snow(3));
        for (int Dx = -1; Dx <= 2; Dx++) {
            for (int Dz = -1; Dz <= 2; Dz++) {
                if (Dx < 0 || Dx > 1 || Dz < 0 || Dz > 1) {
                    deepen(World, X + Dx, Base, Z + Dz);
                }
            }
        }
    }

    private static void fallen(Canvas World, Scp4666Map Layout, int X, int Z, RandomSource Random) {
        int Base = Layout.Ground + 1;
        boolean AlongX = Random.nextBoolean();
        int Length = 3 + Random.nextInt(3);
        for (int Index = 0; Index < Length; Index++) {
            int Px = X + (AlongX ? Index : 0);
            int Pz = Z + (AlongX ? 0 : Index);
            if (!treeFree(Layout, Px + 0.5, Pz + 0.5) || !open(World, Px, Base, Pz) || !open(World, Px, Base + 1, Pz)) {
                return;
            }
        }
        BlockState Log = Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, AlongX ? Direction.Axis.X : Direction.Axis.Z);
        for (int Index = 0; Index < Length; Index++) {
            int Px = X + (AlongX ? Index : 0);
            int Pz = Z + (AlongX ? 0 : Index);
            set(World, Px, Base, Pz, Log);
            set(World, Px, Base + 1, Pz, snow(Index == 0 || Index == Length - 1 ? 1 : 2));
            deepen(World, Px + (AlongX ? 0 : 1), Base, Pz + (AlongX ? 1 : 0));
        }
    }

    private static void puddles(Canvas World, Scp4666Map Layout, ChunkPos Chunk) {
        int Base = Layout.Ground + 1;
        for (int X = Chunk.getMinBlockX(); X <= Chunk.getMaxBlockX(); X++) {
            for (int Z = Chunk.getMinBlockZ(); Z <= Chunk.getMaxBlockZ(); Z++) {
                if (!inRegion(Layout, X, Z) || !treeFree(Layout, X + 0.5, Z + 0.5)) {
                    continue;
                }
                BlockPos Spot = new BlockPos(X, Base, Z);
                if (!World.getBlockState(Spot).is(Blocks.SNOW) || !World.getBlockState(Spot.above()).isAir() || !World.getBlockState(Spot.below()).is(Blocks.GRASS_BLOCK)) {
                    continue;
                }
                if (noise(X / 6.0, Z / 6.0, 51) > PuddleLevel) {
                    set(World, X, Layout.Ground, Z, Blocks.ICE.defaultBlockState());
                    set(World, X, Base, Z, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void finish(ServerLevel World, Scp4666Map Layout) {
        Optional<StructureTemplate> Found = World.getStructureManager().get(Scp4666Map.Template);
        if (Found.isPresent()) {
            StructureTemplate Cabin = Found.get();
            Cabin.placeInWorld(World, Layout.TemplateOrigin, Layout.TemplateOrigin, new StructurePlaceSettings().setKnownShape(true), RandomSource.create(Seed), Block.UPDATE_CLIENTS);
        }
        Scp4666FrozenGatewayBlock.assemble(World, Layout.GateOrigin, Layout.GateFacing);
        BlockPos Chimney = Layout.Chimney;
        if (World.getBlockState(Chimney).isAir()) {
            World.setBlock(Chimney, Blocks.CAMPFIRE.defaultBlockState(), Flags);
        }
        Scp4666Heat.refresh(World, Layout, Scp4666State.get(World.getServer()));
        clearEntities(World);
    }
}
