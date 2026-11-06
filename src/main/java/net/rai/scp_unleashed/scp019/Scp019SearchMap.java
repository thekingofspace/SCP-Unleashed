package net.rai.scp_unleashed.scp019;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp019SearchMap {
    public static final int Radius = Scp019PotBlockEntity.SearchRadius;
    public static final int Rise = 8;
    public static final int Sink = 10;
    public static final int MaxDrop = 3;
    public static final int FloodBudget = 40;
    public static final int PatchShift = 2;
    public static final int ClaimTicks = 600;
    public static final double Spread = 1.4;
    public static final double SpreadCap = 12.0;
    public static final byte Unseen = 0;
    public static final byte Seen = 1;

    public static final class Patch {
        private final long Key;
        private final LongArrayList Members = new LongArrayList();
        private int Remaining;
        private int Claimant = -1;
        private long ClaimedUntil;
        private double SumX;
        private double SumY;
        private double SumZ;

        private Patch(long Key) {
            this.Key = Key;
        }

        public Vec3 centre() {
            int Count = Math.max(1, Members.size());
            return new Vec3(SumX / Count + 0.5, SumY / Count, SumZ / Count + 0.5);
        }

        public long key() {
            return Key;
        }
    }

    private final BlockPos Origin;
    private final Long2ByteOpenHashMap Cells = new Long2ByteOpenHashMap();
    private final LongArrayFIFOQueue Frontier = new LongArrayFIFOQueue();
    private final Long2ObjectOpenHashMap<Patch> Patches = new Long2ObjectOpenHashMap<>();
    private final BlockPos.MutableBlockPos Probe = new BlockPos.MutableBlockPos();
    private int Remaining;
    private int Covered;
    private boolean Seeded;
    private boolean Flooded;
    private int Sweeps;

    public Scp019SearchMap(BlockPos Origin) {
        this.Origin = Origin.immutable();
        Cells.defaultReturnValue((byte) -1);
    }

    public int sweeps() {
        return Sweeps;
    }

    public int size() {
        return Cells.size();
    }

    public int remaining() {
        return Remaining;
    }

    public boolean flooded() {
        return Flooded;
    }

    public float progress() {
        int Total = Remaining + Covered;
        return Total <= 0 ? 0.0f : (float) Covered / Total;
    }

    public boolean finished() {
        return Seeded && Flooded && Remaining <= 0;
    }

    public void reset() {
        Cells.clear();
        Frontier.clear();
        Patches.clear();
        Remaining = 0;
        Covered = 0;
        Seeded = false;
        Flooded = false;
        Sweeps++;
    }

    public boolean inArea(double X, double Y, double Z) {
        double Dx = X - (Origin.getX() + 0.5);
        double Dz = Z - (Origin.getZ() + 0.5);
        double Dy = Y - Origin.getY();
        return Dx * Dx + Dz * Dz <= (double) Radius * Radius && Dy >= -Sink - 1 && Dy <= Rise + 2;
    }

    private boolean withinBounds(int X, int Y, int Z) {
        int Dx = X - Origin.getX();
        int Dz = Z - Origin.getZ();
        int Dy = Y - Origin.getY();
        return Dx * Dx + Dz * Dz <= Radius * Radius && Dy >= -Sink && Dy <= Rise;
    }

    public void tick(Level World) {
        if (!Seeded) {
            seed(World);
            return;
        }
        if (Flooded) {
            return;
        }
        int Budget = FloodBudget;
        while (Budget-- > 0 && !Frontier.isEmpty()) {
            expand(World, Frontier.dequeueLong());
        }
        if (Frontier.isEmpty()) {
            Flooded = true;
        }
    }

    private void seed(Level World) {
        Seeded = true;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            for (int Step = 1; Step <= 2; Step++) {
                BlockPos Around = Origin.relative(Side, Step);
                for (int Dy = 0; Dy >= -2; Dy--) {
                    BlockPos Cell = Around.offset(0, Dy, 0);
                    if (World.isLoaded(Cell) && standable(World, Cell.getX(), Cell.getY(), Cell.getZ())) {
                        add(Cell.getX(), Cell.getY(), Cell.getZ());
                        break;
                    }
                }
            }
        }
        if (Frontier.isEmpty()) {
            Flooded = true;
        }
    }

    private void expand(Level World, long Key) {
        int X = BlockPos.getX(Key);
        int Y = BlockPos.getY(Key);
        int Z = BlockPos.getZ(Key);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            int Nx = X + Side.getStepX();
            int Nz = Z + Side.getStepZ();
            if (!withinBounds(Nx, Y, Nz) || !World.isLoaded(Probe.set(Nx, Y, Nz))) {
                continue;
            }
            if (passable(World, Nx, Y, Nz)) {
                if (standable(World, Nx, Y, Nz)) {
                    add(Nx, Y, Nz);
                    continue;
                }
                for (int Drop = 1; Drop <= MaxDrop; Drop++) {
                    if (standable(World, Nx, Y - Drop, Nz)) {
                        add(Nx, Y - Drop, Nz);
                        break;
                    }
                    if (!passable(World, Nx, Y - Drop, Nz)) {
                        break;
                    }
                }
            } else if (passable(World, X, Y + 1, Z) && standable(World, Nx, Y + 1, Nz)) {
                add(Nx, Y + 1, Nz);
            }
        }
    }

    private void add(int X, int Y, int Z) {
        if (!withinBounds(X, Y, Z)) {
            return;
        }
        long Key = BlockPos.asLong(X, Y, Z);
        if (Cells.containsKey(Key)) {
            return;
        }
        Cells.put(Key, Unseen);
        Frontier.enqueue(Key);
        Remaining++;
        long PatchKey = patchKey(X, Z);
        Patch Owner = Patches.get(PatchKey);
        if (Owner == null) {
            Owner = new Patch(PatchKey);
            Patches.put(PatchKey, Owner);
        }
        Owner.Members.add(Key);
        Owner.Remaining++;
        Owner.SumX += X;
        Owner.SumY += Y;
        Owner.SumZ += Z;
    }

    private static long patchKey(int X, int Z) {
        return ((long) (X >> PatchShift) << 32) | ((Z >> PatchShift) & 0xFFFFFFFFL);
    }

    private boolean passable(Level World, int X, int Y, int Z) {
        Probe.set(X, Y, Z);
        BlockState State = World.getBlockState(Probe);
        return State.getCollisionShape(World, Probe).isEmpty() && State.getFluidState().isEmpty() && !hazard(State);
    }

    private boolean standable(Level World, int X, int Y, int Z) {
        if (!passable(World, X, Y, Z)) {
            return false;
        }
        Probe.set(X, Y - 1, Z);
        BlockState Ground = World.getBlockState(Probe);
        return !Ground.getCollisionShape(World, Probe).isEmpty() && !hazard(Ground) && !(Ground.getBlock() instanceof Scp019PotBlock);
    }

    private static boolean hazard(BlockState State) {
        return State.is(BlockTags.FIRE) || State.is(Blocks.CACTUS) || State.is(Blocks.MAGMA_BLOCK) || State.is(Blocks.SWEET_BERRY_BUSH) || State.is(Blocks.POWDER_SNOW)
                || State.is(BlockTags.CAMPFIRES) || State.is(Blocks.WITHER_ROSE) || State.is(Blocks.POINTED_DRIPSTONE) || State.is(Blocks.LAVA);
    }

    public boolean mark(long Key) {
        if (Cells.get(Key) != Unseen) {
            return false;
        }
        Cells.put(Key, Seen);
        Remaining--;
        Covered++;
        Patch Owner = Patches.get(patchKey(BlockPos.getX(Key), BlockPos.getZ(Key)));
        if (Owner != null) {
            Owner.Remaining--;
        }
        return true;
    }

    public boolean unseen(long Key) {
        return Cells.get(Key) == Unseen;
    }

    public int markAround(BlockPos Feet, int Reach) {
        int Count = 0;
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                for (int Dy = -1; Dy <= 1; Dy++) {
                    if (mark(BlockPos.asLong(Feet.getX() + Dx, Feet.getY() + Dy, Feet.getZ() + Dz))) {
                        Count++;
                    }
                }
            }
        }
        return Count;
    }

    public LongArrayList candidates(BlockPos Feet, double Range) {
        LongArrayList Found = new LongArrayList();
        int Span = (int) Math.ceil(Range) >> PatchShift;
        int Px = Feet.getX() >> PatchShift;
        int Pz = Feet.getZ() >> PatchShift;
        double Limit = Range * Range;
        for (int Dx = -Span - 1; Dx <= Span + 1; Dx++) {
            for (int Dz = -Span - 1; Dz <= Span + 1; Dz++) {
                Patch Owner = Patches.get(((long) (Px + Dx) << 32) | ((Pz + Dz) & 0xFFFFFFFFL));
                if (Owner == null || Owner.Remaining <= 0) {
                    continue;
                }
                for (int Index = 0; Index < Owner.Members.size(); Index++) {
                    long Key = Owner.Members.getLong(Index);
                    if (Cells.get(Key) != Unseen) {
                        continue;
                    }
                    double Ox = BlockPos.getX(Key) - Feet.getX();
                    double Oy = BlockPos.getY(Key) - Feet.getY();
                    double Oz = BlockPos.getZ(Key) - Feet.getZ();
                    if (Ox * Ox + Oy * Oy * 2.0 + Oz * Oz <= Limit) {
                        Found.add(Key);
                    }
                }
            }
        }
        Found.sort((First, Second) -> Double.compare(distance(First, Feet), distance(Second, Feet)));
        return Found;
    }

    private static double distance(long Key, BlockPos Feet) {
        double Ox = BlockPos.getX(Key) - Feet.getX();
        double Oy = BlockPos.getY(Key) - Feet.getY();
        double Oz = BlockPos.getZ(Key) - Feet.getZ();
        return Ox * Ox + Oy * Oy + Oz * Oz;
    }

    @Nullable
    public Patch claim(int Claimant, Vec3 From, List<Vec3> Others, long Now, RandomSource Random) {
        Patch Best = null;
        double BestScore = Double.MAX_VALUE;
        for (Patch Option : Patches.values()) {
            if (Option.Remaining <= 0) {
                continue;
            }
            if (Option.Claimant != -1 && Option.Claimant != Claimant && Option.ClaimedUntil > Now) {
                continue;
            }
            Vec3 Centre = Option.centre();
            double Score = Centre.distanceTo(From);
            double Nearest = SpreadCap;
            for (Vec3 Other : Others) {
                Nearest = Math.min(Nearest, Other.distanceTo(Centre));
            }
            Score -= Nearest * Spread;
            Score += Random.nextDouble() * 3.0;
            if (Score < BestScore) {
                BestScore = Score;
                Best = Option;
            }
        }
        if (Best != null) {
            release(Claimant);
            Best.Claimant = Claimant;
            Best.ClaimedUntil = Now + ClaimTicks;
        }
        return Best;
    }

    public void release(int Claimant) {
        for (Patch Option : Patches.values()) {
            if (Option.Claimant == Claimant) {
                Option.Claimant = -1;
            }
        }
    }

    @Nullable
    public BlockPos waypoint(Patch Option, RandomSource Random) {
        Vec3 Centre = Option.centre();
        List<Long> Open = new ArrayList<>();
        for (int Index = 0; Index < Option.Members.size(); Index++) {
            long Key = Option.Members.getLong(Index);
            if (Cells.get(Key) == Unseen) {
                Open.add(Key);
            }
        }
        if (Open.isEmpty()) {
            return null;
        }
        Open.sort((First, Second) -> Double.compare(spot(First).distanceToSqr(Centre), spot(Second).distanceToSqr(Centre)));
        long Chosen = Open.get(Random.nextInt(Math.min(3, Open.size())));
        return BlockPos.of(Chosen);
    }

    public void abandon(BlockPos Cell, int Claimant) {
        mark(Cell.asLong());
        for (Patch Option : Patches.values()) {
            if (Option.Claimant == Claimant) {
                Option.Claimant = -1;
            }
        }
    }

    private static Vec3 spot(long Key) {
        return new Vec3(BlockPos.getX(Key) + 0.5, BlockPos.getY(Key), BlockPos.getZ(Key) + 0.5);
    }
}
