package net.rai.scp_unleashed.scp860;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp860Builder {
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    public static final int Bottom = Scp860Forest.Ground - 3;
    public static final int Top = Scp860Forest.Ground + 30;
    public static final int Height = Top - Bottom + 1;
    public static final int ChunksPerTick = 4;
    public static final int NearChunks = 3;
    public static final int NearBudget = 6;
    public static final int StandinRadius = 3;
    public static final int ArrivalRadius = 2;
    public static final int TreeGrid = 4;
    public static final int TreeReach = 10;
    public static final double TrunkClear = 3.4;
    public static final double GiantClear = 4.8;
    public static final double DoorClear = 6.5;
    public static final int BarrierHeight = 14;
    public static final int Ground = 5;
    public static final int Log = 6;
    public static final int Door = 9;
    public static final int Leaf = 3;
    public static final int Vine = 2;
    public static final int Moss = 1;
    public static final int Plant = 0;
    public static final int Margin = 10;
    public static final int LeafReach = 5;
    private static final Map<MinecraftServer, Job> Jobs = new ConcurrentHashMap<>();

    private static final class Job {
        final long Seed;
        final Deque<ChunkPos> Queue = new ArrayDeque<>();

        Job(long Seed) {
            this.Seed = Seed;
        }
    }

    private Scp860Builder() {
    }

    public static boolean pending(MinecraftServer Server) {
        return Jobs.containsKey(Server);
    }

    public static int queued(MinecraftServer Server) {
        Job Work = Jobs.get(Server);
        return Work == null ? 0 : Work.Queue.size();
    }

    public static void forget(MinecraftServer Server) {
        Jobs.remove(Server);
    }

    public static void ensure(ServerLevel World) {
        Scp860State State = Scp860State.get(World.getServer());
        Job Work = Jobs.get(World.getServer());
        if (Work != null && Work.Seed == State.seed()) {
            return;
        }
        if (Work == null && State.built()) {
            return;
        }
        schedule(World);
    }

    public static void schedule(ServerLevel World) {
        MinecraftServer Server = World.getServer();
        Scp860State State = Scp860State.get(Server);
        Scp860Plan Layout = Scp860Forest.plan(Server);
        Job Work = new Job(State.seed());
        int BaseX = Scp860Forest.baseX();
        int BaseZ = Scp860Forest.baseZ();
        int ArrivalChunkX = Mth.floor(BaseX + Layout.ArrivalX) >> 4;
        int ArrivalChunkZ = Mth.floor(BaseZ + Layout.ArrivalZ) >> 4;
        List<ChunkPos> All = new ArrayList<>();
        for (int Cx = BaseX >> 4; Cx <= (BaseX + Scp860Forest.Size - 1) >> 4; Cx++) {
            for (int Cz = BaseZ >> 4; Cz <= (BaseZ + Scp860Forest.Size - 1) >> 4; Cz++) {
                if (Scp860Forest.standingIn() && Math.max(Math.abs(Cx - ArrivalChunkX), Math.abs(Cz - ArrivalChunkZ)) > StandinRadius) {
                    continue;
                }
                All.add(new ChunkPos(Cx, Cz));
            }
        }
        All.sort(Comparator.comparingInt(Chunk -> (Chunk.x - ArrivalChunkX) * (Chunk.x - ArrivalChunkX) + (Chunk.z - ArrivalChunkZ) * (Chunk.z - ArrivalChunkZ)));
        Work.Queue.addAll(All);
        Jobs.put(Server, Work);
        clearEntities(World);
    }

    public static AABB region() {
        int BaseX = Scp860Forest.baseX();
        int BaseZ = Scp860Forest.baseZ();
        return new AABB(BaseX - Scp860Forest.Margin, Bottom - 32, BaseZ - Scp860Forest.Margin, BaseX + Scp860Forest.Size + Scp860Forest.Margin, Top + 48,
                BaseZ + Scp860Forest.Size + Scp860Forest.Margin);
    }

    public static int clearEntities(ServerLevel World) {
        List<Entity> Doomed = World.getEntities((Entity) null, region(), Found -> !(Found instanceof Player));
        for (Entity Found : Doomed) {
            Found.discard();
        }
        return Doomed.size();
    }

    public static void tick(ServerLevel World) {
        MinecraftServer Server = World.getServer();
        Job Work = Jobs.get(Server);
        if (Work == null) {
            return;
        }
        Scp860State State = Scp860State.get(Server);
        if (Work.Seed != State.seed()) {
            schedule(World);
            Work = Jobs.get(Server);
        }
        Scp860Plan Layout = Scp860Forest.plan(Server);
        int Budget = NearBudget;
        for (ServerPlayer User : Scp860Forest.participants(World)) {
            int Cx = User.blockPosition().getX() >> 4;
            int Cz = User.blockPosition().getZ() >> 4;
            List<ChunkPos> Near = new ArrayList<>();
            for (ChunkPos Chunk : Work.Queue) {
                if (Math.max(Math.abs(Chunk.x - Cx), Math.abs(Chunk.z - Cz)) <= NearChunks) {
                    Near.add(Chunk);
                }
            }
            for (ChunkPos Chunk : Near) {
                if (Budget-- <= 0) {
                    break;
                }
                Work.Queue.remove(Chunk);
                build(World, Layout, Chunk);
            }
        }
        for (int Step = 0; Step < ChunksPerTick && !Work.Queue.isEmpty(); Step++) {
            build(World, Layout, Work.Queue.poll());
        }
        if (Work.Queue.isEmpty()) {
            Jobs.remove(Server);
            State.markBuilt(Work.Seed);
        }
    }

    public static void buildAround(ServerLevel World, double X, double Z, int Radius) {
        MinecraftServer Server = World.getServer();
        ensure(World);
        Job Work = Jobs.get(Server);
        if (Work == null) {
            return;
        }
        Scp860Plan Layout = Scp860Forest.plan(Server);
        int Cx = Mth.floor(X) >> 4;
        int Cz = Mth.floor(Z) >> 4;
        List<ChunkPos> Near = new ArrayList<>();
        for (ChunkPos Chunk : Work.Queue) {
            if (Math.max(Math.abs(Chunk.x - Cx), Math.abs(Chunk.z - Cz)) <= Radius) {
                Near.add(Chunk);
            }
        }
        for (ChunkPos Chunk : Near) {
            Work.Queue.remove(Chunk);
            build(World, Layout, Chunk);
        }
        if (Work.Queue.isEmpty()) {
            Jobs.remove(Server);
            Scp860State.get(Server).markBuilt(Work.Seed);
        }
    }

    public static void finishNow(ServerLevel World) {
        ensure(World);
        Job Work = Jobs.get(World.getServer());
        if (Work == null) {
            return;
        }
        Scp860Plan Layout = Scp860Forest.plan(World.getServer());
        while (!Work.Queue.isEmpty()) {
            build(World, Layout, Work.Queue.poll());
        }
        Jobs.remove(World.getServer());
        Scp860State.get(World.getServer()).markBuilt(Work.Seed);
    }

    private static final class Buffer {
        final int MinX;
        final int MinZ;
        final int Wide;
        final BlockState[] States;
        final int[] Ranks;

        Buffer(int MinX, int MinZ, int Wide) {
            this.MinX = MinX;
            this.MinZ = MinZ;
            this.Wide = Wide;
            States = new BlockState[Wide * Height * Wide];
            Ranks = new int[Wide * Height * Wide];
            Arrays.fill(Ranks, -1);
        }

        boolean holds(int X, int Y, int Z) {
            return X >= MinX && X < MinX + Wide && Z >= MinZ && Z < MinZ + Wide && Y >= Bottom && Y <= Top;
        }

        int slot(int X, int Y, int Z) {
            return ((X - MinX) * Height + (Y - Bottom)) * Wide + (Z - MinZ);
        }

        void put(int X, int Y, int Z, BlockState State, int Rank) {
            if (!holds(X, Y, Z)) {
                return;
            }
            int Slot = slot(X, Y, Z);
            if (Rank >= Ranks[Slot]) {
                States[Slot] = State;
                Ranks[Slot] = Rank;
            }
        }

        void clear(int X, int Y, int Z) {
            if (holds(X, Y, Z)) {
                int Slot = slot(X, Y, Z);
                States[Slot] = null;
                Ranks[Slot] = -1;
            }
        }

        boolean free(int X, int Y, int Z) {
            return holds(X, Y, Z) && Ranks[slot(X, Y, Z)] < Leaf;
        }

        BlockState get(int X, int Y, int Z) {
            BlockState Found = holds(X, Y, Z) ? States[slot(X, Y, Z)] : null;
            return Found == null ? Blocks.AIR.defaultBlockState() : Found;
        }
    }

    public static long mix(long Seed, long A, long B, long Salt) {
        long Hash = A * 341873128712L + B * 132897987541L + Salt * 2654435761L ^ Seed;
        Hash = (Hash ^ (Hash >>> 29)) * 0xBF58476D1CE4E5B9L;
        Hash = (Hash ^ (Hash >>> 32)) * 0x94D049BB133111EBL;
        return Hash ^ (Hash >>> 31);
    }

    public static double hash(long Seed, int X, int Z, int Salt) {
        return (mix(Seed, X, Z, Salt) & 0xFFFFFFL) / (double) 0xFFFFFFL;
    }

    public static double noise(long Seed, double X, double Z, int Salt) {
        int X0 = Mth.floor(X);
        int Z0 = Mth.floor(Z);
        double Fx = X - X0;
        double Fz = Z - Z0;
        double Sx = Fx * Fx * (3.0 - 2.0 * Fx);
        double Sz = Fz * Fz * (3.0 - 2.0 * Fz);
        double Near = Mth.lerp(Sx, hash(Seed, X0, Z0, Salt), hash(Seed, X0 + 1, Z0, Salt));
        double Far = Mth.lerp(Sx, hash(Seed, X0, Z0 + 1, Salt), hash(Seed, X0 + 1, Z0 + 1, Salt));
        return Mth.lerp(Sz, Near, Far);
    }

    public static int elevation(Scp860Plan Layout, int X, int Z) {
        int Raw = rawElevation(Layout, X, Z);
        if (Raw == 0) {
            return 0;
        }
        int Highest = 0;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Highest = Math.max(Highest, rawElevation(Layout, X + Side.getStepX(), Z + Side.getStepZ()));
        }
        return Highest >= Raw ? Raw : Math.max(0, Raw - 1);
    }

    private static int rawElevation(Scp860Plan Layout, int X, int Z) {
        double Gap = Layout.distance(X, Z);
        if (Gap <= 3.6 || Layout.nearDoor(X + 0.5, Z + 0.5, DoorClear)) {
            return 0;
        }
        double Rise = Mth.clamp((Gap - 3.6) / 5.0, 0.0, 1.0);
        double Field = noise(Layout.Seed, X / 9.0, Z / 9.0, 3) * 0.7 + noise(Layout.Seed, X / 4.0, Z / 4.0, 5) * 0.3;
        return Mth.clamp((int) Math.floor(Field * 2.9 * Rise), 0, 2);
    }

    private static BlockState leaves(boolean Dark) {
        return (Dark ? Blocks.DARK_OAK_LEAVES : Blocks.OAK_LEAVES).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1);
    }

    private static BlockState log(boolean Dark, Direction.Axis Axis) {
        return (Dark ? Blocks.DARK_OAK_LOG : Blocks.OAK_LOG).defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis);
    }

    private static void ground(Buffer Area, Scp860Plan Layout, int LocalX, int LocalZ, int X, int Z) {
        long Seed = Layout.Seed;
        int Raise = elevation(Layout, LocalX, LocalZ);
        int Surface = Scp860Forest.Ground + Raise;
        Area.put(X, Bottom, Z, Blocks.STONE.defaultBlockState(), Ground);
        for (int Y = Bottom + 1; Y < Surface; Y++) {
            Area.put(X, Y, Z, Blocks.DIRT.defaultBlockState(), Ground);
        }
        double Gap = Layout.distance(LocalX, LocalZ);
        double Roll = hash(Seed, LocalX, LocalZ, 11);
        double Patch = noise(Seed, LocalX / 3.0, LocalZ / 3.0, 13);
        BlockState Top;
        if (Gap <= 1.3) {
            Top = Patch > 0.78 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
        } else if (Gap <= 2.1) {
            Top = Roll < 0.4 ? Blocks.COARSE_DIRT.defaultBlockState() : Roll < 0.8 ? Blocks.DIRT_PATH.defaultBlockState()
                    : Roll < 0.9 ? Blocks.GRAVEL.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState();
        } else if (Gap <= 2.7) {
            Top = Roll < 0.6 ? Blocks.GRASS_BLOCK.defaultBlockState() : Roll < 0.85 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
        } else {
            Top = Patch > 0.86 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        }
        Area.put(X, Surface, Z, Top, Ground);
        int Above = Surface + 1;
        double Sprout = hash(Seed, LocalX, LocalZ, 17);
        if (Gap <= 2.1) {
            return;
        }
        if (Gap <= 2.7) {
            if (Sprout < 0.2 && Top.is(Blocks.GRASS_BLOCK)) {
                Area.put(X, Above, Z, Blocks.SHORT_GRASS.defaultBlockState(), Plant);
            }
            return;
        }
        if (Sprout < 0.34) {
            Area.put(X, Above, Z, Blocks.SHORT_GRASS.defaultBlockState(), Scp860Builder.Plant);
        } else if (Sprout < 0.48) {
            Area.put(X, Above, Z, Blocks.FERN.defaultBlockState(), Scp860Builder.Plant);
        } else if (Sprout < 0.54) {
            tall(Area, X, Above, Z, Blocks.TALL_GRASS.defaultBlockState());
        } else if (Sprout < 0.58) {
            tall(Area, X, Above, Z, Blocks.LARGE_FERN.defaultBlockState());
        } else if (Sprout < 0.61) {
            BlockState[] Flowers = {Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.DANDELION.defaultBlockState(),
                    Blocks.CORNFLOWER.defaultBlockState(), Blocks.LILY_OF_THE_VALLEY.defaultBlockState()};
            Area.put(X, Above, Z, Flowers[(int) (hash(Seed, LocalX, LocalZ, 19) * Flowers.length) % Flowers.length], Scp860Builder.Plant);
        } else if (Sprout < 0.66 && Patch > 0.6) {
            Area.put(X, Above, Z, Blocks.MOSS_CARPET.defaultBlockState(), Scp860Builder.Plant);
        }
    }

    private static void tall(Buffer Area, int X, int Y, int Z, BlockState Kind) {
        Area.put(X, Y, Z, Kind.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), Plant);
        Area.put(X, Y + 1, Z, Kind.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), Plant);
    }

    public static boolean treeSpot(Scp860Plan Layout, double X, double Z, double Clear) {
        if (X < 2.0 || Z < 2.0 || X > Scp860Forest.Size - 3 || Z > Scp860Forest.Size - 3) {
            return false;
        }
        return Layout.distance(X, Z) >= Clear && !Layout.nearDoor(X, Z, DoorClear);
    }

    private static void trees(Buffer Area, Scp860Plan Layout, int BaseX, int BaseZ) {
        long Seed = Layout.Seed;
        int LowX = Math.floorDiv(Area.MinX - BaseX - TreeReach, TreeGrid);
        int HighX = Math.floorDiv(Area.MinX - BaseX + Area.Wide + TreeReach, TreeGrid);
        int LowZ = Math.floorDiv(Area.MinZ - BaseZ - TreeReach, TreeGrid);
        int HighZ = Math.floorDiv(Area.MinZ - BaseZ + Area.Wide + TreeReach, TreeGrid);
        for (int Gx = LowX; Gx <= HighX; Gx++) {
            for (int Gz = LowZ; Gz <= HighZ; Gz++) {
                Random Dice = new Random(mix(Seed, Gx, Gz, 23));
                int Lx = Gx * TreeGrid + Dice.nextInt(TreeGrid);
                int Lz = Gz * TreeGrid + Dice.nextInt(TreeGrid);
                double Edge = Math.min(Math.min(Lx, Lz), Math.min(Scp860Forest.Size - 1 - Lx, Scp860Forest.Size - 1 - Lz));
                double Density = Edge < 12 ? 0.95 : 0.62 + 0.33 * noise(Seed, Lx / 21.0, Lz / 21.0, 29);
                if (Dice.nextDouble() > Density) {
                    continue;
                }
                double Kind = Dice.nextDouble();
                boolean Giant = Kind >= 0.6 && Kind < 0.72;
                if (!treeSpot(Layout, Lx + 0.5, Lz + 0.5, Giant ? GiantClear : TrunkClear)) {
                    continue;
                }
                int Base = Scp860Forest.Ground + elevation(Layout, Lx, Lz) + 1;
                int X = BaseX + Lx;
                int Z = BaseZ + Lz;
                if (Kind < 0.6) {
                    gnarled(Area, Dice, X, Base, Z);
                } else if (Giant) {
                    giant(Area, Dice, X, Base, Z);
                } else if (Kind < 0.8) {
                    snag(Area, Dice, X, Base, Z);
                } else {
                    bush(Area, Dice, X, Base, Z);
                }
            }
        }
    }

    private static void blob(Buffer Area, Random Dice, double Cx, int Cy, double Cz, double Radius, int Layers, boolean Dark, List<int[]> Placed) {
        int Reach = (int) Math.ceil(Radius);
        for (int Layer = 0; Layer < Layers; Layer++) {
            double Shrink = Layer == 0 ? 0.85 : Layer == Layers - 1 ? 0.55 : 1.0;
            double Here = Radius * Shrink;
            for (int Dx = -Reach; Dx <= Reach; Dx++) {
                for (int Dz = -Reach; Dz <= Reach; Dz++) {
                    double Gap = Math.sqrt(Dx * Dx + Dz * Dz);
                    if (Gap > Here + 0.3 || Gap > Here - 0.6 && Dice.nextFloat() < 0.4) {
                        continue;
                    }
                    int X = Mth.floor(Cx + Dx);
                    int Y = Cy + Layer;
                    int Z = Mth.floor(Cz + Dz);
                    Area.put(X, Y, Z, leaves(Dark && Dice.nextFloat() < 0.5), Leaf);
                    if (Layer == 0) {
                        Placed.add(new int[]{X, Y, Z});
                    }
                }
            }
        }
    }

    private static void vines(Buffer Area, Random Dice, List<int[]> Placed, float Chance, int Longest) {
        for (int[] Spot : Placed) {
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                if (Dice.nextFloat() >= Chance) {
                    continue;
                }
                int X = Spot[0] + Side.getStepX();
                int Z = Spot[2] + Side.getStepZ();
                if (!Area.free(X, Spot[1], Z)) {
                    continue;
                }
                BlockState Hanging = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(Side.getOpposite()), true);
                int Length = 1 + Dice.nextInt(Longest);
                for (int Step = 0; Step < Length; Step++) {
                    int Y = Spot[1] - Step;
                    if (!Area.free(X, Y, Z) || Area.get(X, Y - 1, Z).is(Blocks.GRASS_BLOCK) && Step > 0) {
                        break;
                    }
                    Area.put(X, Y, Z, Hanging, Vine);
                }
            }
        }
    }

    private static void moss(Buffer Area, Random Dice, int X, int Base, int Z, int Radius) {
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                if (Dice.nextFloat() < 0.35 && Area.free(X + Dx, Base, Z + Dz) && Area.get(X + Dx, Base - 1, Z + Dz).is(Blocks.GRASS_BLOCK)) {
                    Area.put(X + Dx, Base, Z + Dz, Blocks.MOSS_CARPET.defaultBlockState(), Moss);
                }
            }
        }
    }

    private static void path(Buffer Area, int[] At, int Nx, int Ny, int Nz, boolean Dark) {
        while (At[0] != Nx) {
            At[0] += Integer.signum(Nx - At[0]);
            Area.put(At[0], At[1], At[2], log(Dark, Direction.Axis.X), Log);
        }
        while (At[2] != Nz) {
            At[2] += Integer.signum(Nz - At[2]);
            Area.put(At[0], At[1], At[2], log(Dark, Direction.Axis.Z), Log);
        }
        while (At[1] != Ny) {
            At[1] += Integer.signum(Ny - At[1]);
            Area.put(At[0], At[1], At[2], log(Dark, Direction.Axis.Y), Log);
        }
    }

    public static boolean isLog(BlockState State) {
        return State.is(BlockTags.LOGS);
    }

    public static boolean isLeaf(BlockState State) {
        return State.is(BlockTags.LEAVES);
    }

    public static boolean isSoil(BlockState State) {
        return State.is(BlockTags.DIRT) || State.is(Blocks.DIRT_PATH) || State.is(Blocks.GRAVEL);
    }

    public static boolean isSmallPlant(BlockState State) {
        return State.is(Blocks.SHORT_GRASS) || State.is(Blocks.FERN) || State.is(BlockTags.SMALL_FLOWERS);
    }

    private static void settle(Buffer Area) {
        int Wide = Area.Wide;
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        boolean[] Held = new boolean[Area.States.length];
        int[] Reach = new int[Area.States.length];
        Arrays.fill(Reach, -1);
        for (int X = Area.MinX; X < Area.MinX + Wide; X++) {
            for (int Z = Area.MinZ; Z < Area.MinZ + Wide; Z++) {
                for (int Y = Bottom + 1; Y <= Top; Y++) {
                    BlockState Here = Area.get(X, Y, Z);
                    if (isLog(Here) && isSoil(Area.get(X, Y - 1, Z))) {
                        Held[Area.slot(X, Y, Z)] = true;
                        Queue.add(new int[]{X, Y, Z});
                    }
                }
            }
        }
        while (!Queue.isEmpty()) {
            int[] Spot = Queue.poll();
            for (Direction Side : Direction.values()) {
                int Nx = Spot[0] + Side.getStepX();
                int Ny = Spot[1] + Side.getStepY();
                int Nz = Spot[2] + Side.getStepZ();
                if (Area.holds(Nx, Ny, Nz) && !Held[Area.slot(Nx, Ny, Nz)] && isLog(Area.get(Nx, Ny, Nz))) {
                    Held[Area.slot(Nx, Ny, Nz)] = true;
                    Queue.add(new int[]{Nx, Ny, Nz});
                }
            }
        }
        for (int X = Area.MinX; X < Area.MinX + Wide; X++) {
            for (int Z = Area.MinZ; Z < Area.MinZ + Wide; Z++) {
                for (int Y = Bottom; Y <= Top; Y++) {
                    BlockState Here = Area.get(X, Y, Z);
                    if (isLog(Here) && !Held[Area.slot(X, Y, Z)]) {
                        Area.clear(X, Y, Z);
                    } else if (isLog(Here) || isLeaf(Here) && isSoil(Area.get(X, Y - 1, Z))) {
                        Reach[Area.slot(X, Y, Z)] = 0;
                        Queue.add(new int[]{X, Y, Z});
                    }
                }
            }
        }
        while (!Queue.isEmpty()) {
            int[] Spot = Queue.poll();
            int Next = Reach[Area.slot(Spot[0], Spot[1], Spot[2])] + 1;
            if (Next > LeafReach) {
                continue;
            }
            for (Direction Side : Direction.values()) {
                int Nx = Spot[0] + Side.getStepX();
                int Ny = Spot[1] + Side.getStepY();
                int Nz = Spot[2] + Side.getStepZ();
                if (Area.holds(Nx, Ny, Nz) && Reach[Area.slot(Nx, Ny, Nz)] < 0 && isLeaf(Area.get(Nx, Ny, Nz))) {
                    Reach[Area.slot(Nx, Ny, Nz)] = Next;
                    Queue.add(new int[]{Nx, Ny, Nz});
                }
            }
        }
        for (int X = Area.MinX; X < Area.MinX + Wide; X++) {
            for (int Z = Area.MinZ; Z < Area.MinZ + Wide; Z++) {
                for (int Y = Top; Y >= Bottom; Y--) {
                    BlockState Here = Area.get(X, Y, Z);
                    if (isLeaf(Here) && Reach[Area.slot(X, Y, Z)] < 0) {
                        Area.clear(X, Y, Z);
                    }
                }
            }
        }
        for (int X = Area.MinX; X < Area.MinX + Wide; X++) {
            for (int Z = Area.MinZ; Z < Area.MinZ + Wide; Z++) {
                for (int Y = Top; Y >= Bottom; Y--) {
                    BlockState Here = Area.get(X, Y, Z);
                    if (!Here.is(Blocks.VINE)) {
                        continue;
                    }
                    BlockState Kept = Here.setValue(VineBlock.UP, false);
                    boolean Any = false;
                    BlockState Above = Area.get(X, Y + 1, Z);
                    for (Direction Side : Direction.Plane.HORIZONTAL) {
                        BooleanProperty Face = VineBlock.getPropertyForFace(Side);
                        if (!Here.getValue(Face)) {
                            continue;
                        }
                        BlockState Behind = Area.get(X + Side.getStepX(), Y, Z + Side.getStepZ());
                        boolean Holds = isLog(Behind) || isLeaf(Behind) || Above.is(Blocks.VINE) && Above.getValue(Face);
                        Kept = Kept.setValue(Face, Holds);
                        Any |= Holds;
                    }
                    if (Any) {
                        Area.put(X, Y, Z, Kept, Vine);
                    } else {
                        Area.clear(X, Y, Z);
                    }
                }
            }
        }
        for (int X = Area.MinX; X < Area.MinX + Wide; X++) {
            for (int Z = Area.MinZ; Z < Area.MinZ + Wide; Z++) {
                for (int Y = Bottom + 1; Y <= Top; Y++) {
                    BlockState Here = Area.get(X, Y, Z);
                    BlockState Below = Area.get(X, Y - 1, Z);
                    if (isSmallPlant(Here) && !isSoil(Below) || Here.is(Blocks.MOSS_CARPET) && (Below.isAir() || isSmallPlant(Below) || Below.is(Blocks.MOSS_CARPET))) {
                        Area.clear(X, Y, Z);
                    } else if (Here.getBlock() instanceof DoublePlantBlock) {
                        boolean Lower = Here.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER;
                        boolean Whole = Lower ? isSoil(Below) && Area.get(X, Y + 1, Z).is(Here.getBlock()) : Below.is(Here.getBlock())
                                && Below.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER;
                        if (!Whole) {
                            Area.clear(X, Y, Z);
                            if (Lower && Area.get(X, Y + 1, Z).is(Here.getBlock())) {
                                Area.clear(X, Y + 1, Z);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void gnarled(Buffer Area, Random Dice, int X, int Base, int Z) {
        int Tall = 5 + Dice.nextInt(4);
        double Angle = Dice.nextDouble() * Math.PI * 2.0;
        double Lean = 1.0 + Dice.nextInt(2);
        int LastX = X;
        int LastZ = Z;
        for (int Y = 0; Y < Tall; Y++) {
            double Share = Math.pow(Y / (double) Tall, 1.3);
            int Px = X + (int) Math.round(Math.cos(Angle) * Lean * Share);
            int Pz = Z + (int) Math.round(Math.sin(Angle) * Lean * Share);
            if (Px != LastX && Pz != LastZ) {
                Area.put(Px, Base + Y, LastZ, log(false, Direction.Axis.Y), Log);
            }
            if (Px != LastX || Pz != LastZ) {
                Area.put(LastX, Base + Y, LastZ, log(false, Direction.Axis.Y), Log);
            }
            Area.put(Px, Base + Y, Pz, log(false, Direction.Axis.Y), Log);
            LastX = Px;
            LastZ = Pz;
        }
        List<int[]> Placed = new ArrayList<>();
        int Branches = 2 + Dice.nextInt(2);
        for (int Branch = 0; Branch < Branches; Branch++) {
            double Way = Dice.nextDouble() * Math.PI * 2.0;
            int StartY = Base + (int) (Tall * (0.5 + Dice.nextDouble() * 0.35));
            int Length = 2 + Dice.nextInt(2);
            double Bx = LastX;
            double Bz = LastZ;
            int By = StartY;
            Direction.Axis Axis = Math.abs(Math.cos(Way)) > Math.abs(Math.sin(Way)) ? Direction.Axis.X : Direction.Axis.Z;
            int[] At = {LastX, By, LastZ};
            for (int Step = 1; Step <= Length; Step++) {
                Bx += Math.cos(Way);
                Bz += Math.sin(Way);
                if (Step % 2 == 0) {
                    By++;
                }
                path(Area, At, Mth.floor(Bx + 0.5), By, Mth.floor(Bz + 0.5), false);
            }
            blob(Area, Dice, Bx + 0.5, By, Bz + 0.5, 2.0, 2, false, Placed);
        }
        blob(Area, Dice, LastX + 0.5, Base + Tall - 1, LastZ + 0.5, 3.0 + Dice.nextInt(2), 3, false, Placed);
        vines(Area, Dice, Placed, 0.22f, 4);
        moss(Area, Dice, X, Base, Z, 2);
    }

    private static void giant(Buffer Area, Random Dice, int X, int Base, int Z) {
        int Tall = 7 + Dice.nextInt(4);
        Direction Shift = Direction.Plane.HORIZONTAL.getRandomDirection(net.minecraft.util.RandomSource.create(Dice.nextLong()));
        int Kink = Tall / 2;
        for (int Y = 0; Y < Tall; Y++) {
            int Ox = Y >= Kink ? Shift.getStepX() : 0;
            int Oz = Y >= Kink ? Shift.getStepZ() : 0;
            for (int Dx = 0; Dx <= 1; Dx++) {
                for (int Dz = 0; Dz <= 1; Dz++) {
                    Area.put(X + Dx + Ox, Base + Y, Z + Dz + Oz, log(true, Direction.Axis.Y), Log);
                    if (Y == Kink) {
                        Area.put(X + Dx, Base + Y, Z + Dz, log(true, Direction.Axis.Y), Log);
                    }
                }
            }
        }
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (Dice.nextFloat() < 0.7f) {
                int Reach = 1 + Dice.nextInt(2);
                int Sx = X + (Side.getStepX() > 0 ? 1 : 0);
                int Sz = Z + (Side.getStepZ() > 0 ? 1 : 0);
                for (int Step = 1; Step <= Reach; Step++) {
                    Area.put(Sx + Side.getStepX() * Step, Base, Sz + Side.getStepZ() * Step, log(true, Side.getAxis()), Log);
                }
                if (Dice.nextBoolean()) {
                    Area.put(Sx + Side.getStepX(), Base + 1, Sz + Side.getStepZ(), log(true, Direction.Axis.Y), Log);
                }
            }
        }
        List<int[]> Placed = new ArrayList<>();
        int TopX = X + Shift.getStepX();
        int TopZ = Z + Shift.getStepZ();
        for (int Branch = 0; Branch < 3; Branch++) {
            double Way = Dice.nextDouble() * Math.PI * 2.0;
            int By = Base + Tall - 3 + Dice.nextInt(2);
            double Bx = TopX + 0.5;
            double Bz = TopZ + 0.5;
            Direction.Axis Axis = Math.abs(Math.cos(Way)) > Math.abs(Math.sin(Way)) ? Direction.Axis.X : Direction.Axis.Z;
            int[] At = {TopX, By, TopZ};
            for (int Step = 1; Step <= 3; Step++) {
                Bx += Math.cos(Way);
                Bz += Math.sin(Way);
                path(Area, At, Mth.floor(Bx + 0.5), By + Step / 2, Mth.floor(Bz + 0.5), true);
            }
            blob(Area, Dice, Bx + 0.5, By + 1, Bz + 0.5, 2.5, 2, true, Placed);
        }
        blob(Area, Dice, TopX + 1.0, Base + Tall - 1, TopZ + 1.0, 4.5, 4, true, Placed);
        vines(Area, Dice, Placed, 0.32f, 5);
        moss(Area, Dice, X, Base, Z, 3);
    }

    private static void snag(Buffer Area, Random Dice, int X, int Base, int Z) {
        int Tall = 4 + Dice.nextInt(3);
        double Angle = Dice.nextDouble() * Math.PI * 2.0;
        int LastX = X;
        int LastZ = Z;
        for (int Y = 0; Y < Tall; Y++) {
            int Px = X + (int) Math.round(Math.cos(Angle) * 2.0 * Y / Tall);
            int Pz = Z + (int) Math.round(Math.sin(Angle) * 2.0 * Y / Tall);
            if (Px != LastX && Pz != LastZ) {
                Area.put(Px, Base + Y, LastZ, log(false, Direction.Axis.Y), Log);
            }
            if (Px != LastX || Pz != LastZ) {
                Area.put(LastX, Base + Y, LastZ, log(false, Direction.Axis.Y), Log);
            }
            Area.put(Px, Base + Y, Pz, log(false, Direction.Axis.Y), Log);
            LastX = Px;
            LastZ = Pz;
            if (Y > 0 && Dice.nextFloat() < 0.3f) {
                Direction Side = Direction.Plane.HORIZONTAL.getRandomDirection(net.minecraft.util.RandomSource.create(Dice.nextLong()));
                int Vx = Px + Side.getStepX();
                int Vz = Pz + Side.getStepZ();
                if (Area.free(Vx, Base + Y, Vz)) {
                    Area.put(Vx, Base + Y, Vz, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(Side.getOpposite()), true), Vine);
                }
            }
        }
        if (Dice.nextBoolean()) {
            Direction Side = Direction.Plane.HORIZONTAL.getRandomDirection(net.minecraft.util.RandomSource.create(Dice.nextLong()));
            Area.put(LastX + Side.getStepX(), Base + Tall - 1, LastZ + Side.getStepZ(), log(false, Side.getAxis()), Log);
        }
        moss(Area, Dice, X, Base, Z, 1);
    }

    private static void bush(Buffer Area, Random Dice, int X, int Base, int Z) {
        int Radius = 1 + Dice.nextInt(2);
        int Tall = 1 + Dice.nextInt(2);
        boolean Azalea = Dice.nextFloat() < 0.3f;
        for (int Y = 0; Y < Tall; Y++) {
            for (int Dx = -Radius; Dx <= Radius; Dx++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    if (Dx * Dx + Dz * Dz > Radius * Radius + (Y == 0 ? 1 : -1) || Dice.nextFloat() < 0.15f) {
                        continue;
                    }
                    BlockState Kind = Azalea ? Blocks.AZALEA_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true).setValue(LeavesBlock.DISTANCE, 1) : leaves(false);
                    Area.put(X + Dx, Base + Y, Z + Dz, Kind, Leaf);
                }
            }
        }
    }

    private static void door(Buffer Area, BlockPos Master, Direction Facing) {
        for (int Part : Scp860DoorBlock.Halves) {
            BlockPos Cell = Scp860DoorBlock.cellPos(Master, Facing, Part);
            Area.put(Cell.getX(), Cell.getY(), Cell.getZ(), Scp860Registry.FarDoor.get().defaultBlockState().setValue(Scp860DoorBlock.Facing, Facing)
                    .setValue(Scp860DoorBlock.Part, Part).setValue(Scp860DoorBlock.Open, false), Door);
            for (int Y = Cell.getY() + 1; Y <= Cell.getY() + 2 && Part == Scp860DoorBlock.Upper; Y++) {
                Area.put(Cell.getX(), Y, Cell.getZ(), Blocks.AIR.defaultBlockState(), Door);
            }
        }
    }

    public static void build(ServerLevel World, Scp860Plan Layout, ChunkPos Chunk) {
        LevelChunk Loaded = World.getChunk(Chunk.x, Chunk.z);
        int BaseX = Scp860Forest.baseX();
        int BaseZ = Scp860Forest.baseZ();
        Buffer Area = new Buffer(Chunk.getMinBlockX() - Margin, Chunk.getMinBlockZ() - Margin, 16 + Margin * 2);
        for (int Dx = 0; Dx < Area.Wide; Dx++) {
            for (int Dz = 0; Dz < Area.Wide; Dz++) {
                int X = Area.MinX + Dx;
                int Z = Area.MinZ + Dz;
                int Lx = X - BaseX;
                int Lz = Z - BaseZ;
                if (Lx < 0 || Lz < 0 || Lx >= Scp860Forest.Size || Lz >= Scp860Forest.Size) {
                    continue;
                }
                ground(Area, Layout, Lx, Lz, X, Z);
                if (Lx == 0 || Lz == 0 || Lx == Scp860Forest.Size - 1 || Lz == Scp860Forest.Size - 1) {
                    for (int Y = Scp860Forest.Ground + 1; Y <= Scp860Forest.Ground + BarrierHeight; Y++) {
                        Area.put(X, Y, Z, Blocks.BARRIER.defaultBlockState(), Door);
                    }
                }
            }
        }
        trees(Area, Layout, BaseX, BaseZ);
        BlockPos Arrival = new BlockPos(BaseX + Layout.ArrivalDoorX, Scp860Forest.Ground + 1, BaseZ + Layout.ArrivalDoorZ);
        BlockPos Leaving = new BlockPos(BaseX + Layout.ExitDoorX, Scp860Forest.Ground + 1, BaseZ + Layout.ExitDoorZ);
        door(Area, Arrival, Layout.ArrivalFacing);
        door(Area, Leaving, Layout.ExitFacing);
        settle(Area);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = 0; Dx < 16; Dx++) {
            for (int Dz = 0; Dz < 16; Dz++) {
                for (int Y = Bottom; Y <= Top; Y++) {
                    int X = Chunk.getMinBlockX() + Dx;
                    int Z = Chunk.getMinBlockZ() + Dz;
                    BlockState Wanted = Area.get(X, Y, Z);
                    Cursor.set(X, Y, Z);
                    if (Loaded.getBlockState(Cursor) != Wanted) {
                        World.setBlock(Cursor, Wanted, Flags);
                    }
                }
            }
        }
        for (BlockPos Master : List.of(Arrival, Leaving)) {
            boolean Inside = Master.getX() >= Chunk.getMinBlockX() && Master.getX() <= Chunk.getMaxBlockX() && Master.getZ() >= Chunk.getMinBlockZ()
                    && Master.getZ() <= Chunk.getMaxBlockZ();
            if (Inside && World.getBlockEntity(Master) instanceof Scp860DoorBlockEntity Frame) {
                Frame.setMode(Master.equals(Arrival) ? Scp860DoorBlockEntity.Arrival : Scp860DoorBlockEntity.Exit);
            }
        }
    }
}
