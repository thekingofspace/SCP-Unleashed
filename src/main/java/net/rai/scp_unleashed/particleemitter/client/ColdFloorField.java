package net.rai.scp_unleashed.particleemitter.client;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class ColdFloorField {
    public static final int Radius = 10;
    public static final int MaxDrop = 6;
    public static final int MaxCells = 720;
    public static final double BandLow = 0.2;
    public static final double BandHigh = 0.55;
    public static final double CoreLow = 0.3;
    public static final double CoreHigh = 0.7;
    public static final double LaneLow = 0.2;
    public static final double LaneHigh = 0.8;
    public static final double FloorProbe = 0.85;
    public static final double LowestFloor = -0.15;
    public static final float DropCost = 0.35f;
    public static final float Diagonal = 1.4142f;
    public static final byte Solid = 0;
    public static final byte Open = 1;
    public static final byte Unknown = -1;
    private static final int[][] Steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final BlockState Unloaded = Blocks.VOID_AIR.defaultBlockState();

    private final Long2ObjectOpenHashMap<Cell> Cells;
    private final Cell[] All;
    private final Long2ByteOpenHashMap Rim;
    private final long[] WatchPos;
    private final BlockState[] WatchState;
    private final VoxelShape[] WatchShape;
    @Nullable
    private final Cell Landing;
    private final AABB Bounds;

    public record Cell(int X, int Y, int Z, double FloorY, int StepX, int StepZ, float Distance, boolean Grounded) {
    }

    private record Entry(Node Target, float Distance) {
    }

    private static final class Node {
        private final int X;
        private final int Y;
        private final int Z;
        private double FloorY;
        private float Distance = Float.MAX_VALUE;
        private boolean Grounded;
        private boolean Done;
        private int StepX;
        private int StepZ;

        private Node(int X, int Y, int Z) {
            this.X = X;
            this.Y = Y;
            this.Z = Z;
        }
    }

    private ColdFloorField(Long2ObjectOpenHashMap<Cell> Cells, Cell[] All, Long2ByteOpenHashMap Rim, Probe Look, @Nullable Cell Landing, AABB Bounds) {
        this.Cells = Cells;
        this.All = All;
        this.Rim = Rim;
        this.WatchPos = Look.Positions.toLongArray();
        this.WatchState = Look.States.toArray(new BlockState[0]);
        this.WatchShape = Look.Outlines.toArray(new VoxelShape[0]);
        this.Landing = Landing;
        this.Bounds = Bounds;
    }

    @Nullable
    public Cell at(int X, int Y, int Z) {
        return Cells.get(BlockPos.asLong(X, Y, Z));
    }

    @Nullable
    public Cell at(long Key) {
        return Cells.get(Key);
    }

    public Cell[] cells() {
        return All;
    }

    public byte rim(long Key) {
        return Rim.get(Key);
    }

    @Nullable
    public Cell landing() {
        return Landing;
    }

    public AABB bounds() {
        return Bounds;
    }

    public int cellCount() {
        return All.length;
    }

    public boolean stale(Level World) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Index = 0; Index < WatchPos.length; Index++) {
            Cursor.set(WatchPos[Index]);
            boolean Loaded = World.isLoaded(Cursor);
            BlockState Now = Loaded ? World.getBlockState(Cursor) : Unloaded;
            if (Now == WatchState[Index]) {
                continue;
            }
            VoxelShape Shape = Loaded ? Now.getCollisionShape(World, Cursor) : Shapes.block();
            if (Shape != WatchShape[Index]) {
                return true;
            }
            WatchState[Index] = Now;
        }
        return false;
    }

    public static ColdFloorField build(Level World, BlockPos Emitter) {
        Probe Look = new Probe(World, Emitter);
        Long2ObjectOpenHashMap<Node> Nodes = new Long2ObjectOpenHashMap<>();
        int X = Emitter.getX();
        int Z = Emitter.getZ();
        Node Start = null;
        for (int Drop = 0; Drop <= MaxDrop; Drop++) {
            int CellY = Emitter.getY() - Drop;
            if (Drop > 0 && !Look.passable(X, CellY, Z)) {
                break;
            }
            if (Look.supported(X, CellY, Z)) {
                Start = node(Nodes, X, CellY, Z);
                Start.Grounded = true;
                Start.FloorY = Look.floorTop(X, CellY, Z);
                Start.Distance = 0.0f;
                break;
            }
            Node Column = node(Nodes, X, CellY, Z);
            Column.Distance = 0.0f;
        }
        if (Start != null) {
            spread(Look, Nodes, Start);
        }
        return finish(Look, Nodes, Start);
    }

    private static void spread(Probe Look, Long2ObjectOpenHashMap<Node> Nodes, Node Start) {
        PriorityQueue<Entry> Queue = new PriorityQueue<>(Comparator.comparingDouble(Entry::Distance));
        Queue.add(new Entry(Start, 0.0f));
        float Limit = Radius + 0.5f;
        while (!Queue.isEmpty()) {
            Entry Next = Queue.poll();
            Node From = Next.Target();
            if (From.Done || Next.Distance() > From.Distance) {
                continue;
            }
            From.Done = true;
            for (int[] Step : Steps) {
                int Dx = Step[0];
                int Dz = Step[1];
                boolean Slanted = Dx != 0 && Dz != 0;
                int Bx = From.X + Dx;
                int Bz = From.Z + Dz;
                float Reach = From.Distance + (Slanted ? Diagonal : 1.0f);
                float Ox = Bx - Start.X;
                float Oz = Bz - Start.Z;
                if (Reach > Limit || Ox * Ox + Oz * Oz > Limit * Limit) {
                    continue;
                }
                if (Slanted) {
                    if (diagonalOpen(Look, From.X, From.Y, From.Z, Dx, Dz)) {
                        relax(Look, Nodes, Queue, Bx, From.Y, Bz, Reach);
                    }
                    continue;
                }
                if (!Look.stepOpen(From.X, From.Y, From.Z, Dx, Dz) || !Look.passable(Bx, From.Y, Bz)) {
                    continue;
                }
                if (Look.supported(Bx, From.Y, Bz)) {
                    relax(Look, Nodes, Queue, Bx, From.Y, Bz, Reach);
                } else {
                    ledge(Look, Nodes, Queue, From, Dx, Dz, Reach);
                }
            }
        }
    }

    private static boolean diagonalOpen(Probe Look, int X, int Y, int Z, int Dx, int Dz) {
        return Look.stepOpen(X, Y, Z, Dx, 0) && Look.passable(X + Dx, Y, Z) && Look.supported(X + Dx, Y, Z)
                && Look.stepOpen(X, Y, Z, 0, Dz) && Look.passable(X, Y, Z + Dz) && Look.supported(X, Y, Z + Dz)
                && Look.stepOpen(X + Dx, Y, Z, 0, Dz) && Look.stepOpen(X, Y, Z + Dz, Dx, 0)
                && Look.passable(X + Dx, Y, Z + Dz) && Look.supported(X + Dx, Y, Z + Dz);
    }

    private static void ledge(Probe Look, Long2ObjectOpenHashMap<Node> Nodes, PriorityQueue<Entry> Queue, Node From, int Dx, int Dz, float Reach) {
        int Bx = From.X + Dx;
        int Bz = From.Z + Dz;
        if (!airborne(Nodes, Bx, From.Y, Bz, Reach, Dx, Dz)) {
            return;
        }
        for (int Fall = 1; Fall <= MaxDrop; Fall++) {
            int CellY = From.Y - Fall;
            if (!Look.passable(Bx, CellY, Bz)) {
                return;
            }
            if (Look.supported(Bx, CellY, Bz)) {
                relax(Look, Nodes, Queue, Bx, CellY, Bz, Reach + DropCost * Fall);
                return;
            }
            if (!airborne(Nodes, Bx, CellY, Bz, Reach, 0, 0)) {
                return;
            }
        }
    }

    private static boolean airborne(Long2ObjectOpenHashMap<Node> Nodes, int X, int Y, int Z, float Reach, int StepX, int StepZ) {
        long Key = BlockPos.asLong(X, Y, Z);
        Node Target = Nodes.get(Key);
        if (Target == null) {
            if (Nodes.size() >= MaxCells) {
                return false;
            }
            Target = new Node(X, Y, Z);
            Nodes.put(Key, Target);
        }
        if (Target.Grounded) {
            return false;
        }
        if (Reach < Target.Distance) {
            Target.Distance = Reach;
            Target.StepX = StepX;
            Target.StepZ = StepZ;
        }
        return true;
    }

    private static void relax(Probe Look, Long2ObjectOpenHashMap<Node> Nodes, PriorityQueue<Entry> Queue, int X, int Y, int Z, float Reach) {
        long Key = BlockPos.asLong(X, Y, Z);
        Node Target = Nodes.get(Key);
        if (Target == null) {
            if (Nodes.size() >= MaxCells) {
                return;
            }
            Target = new Node(X, Y, Z);
            Nodes.put(Key, Target);
        }
        if (Target.Done || Reach >= Target.Distance) {
            return;
        }
        Target.Distance = Reach;
        Target.Grounded = true;
        Target.FloorY = Look.floorTop(X, Y, Z);
        Queue.add(new Entry(Target, Reach));
    }

    private static Node node(Long2ObjectOpenHashMap<Node> Nodes, int X, int Y, int Z) {
        long Key = BlockPos.asLong(X, Y, Z);
        Node Found = Nodes.get(Key);
        if (Found == null) {
            Found = new Node(X, Y, Z);
            Nodes.put(Key, Found);
        }
        return Found;
    }

    private static ColdFloorField finish(Probe Look, Long2ObjectOpenHashMap<Node> Nodes, @Nullable Node Start) {
        Long2ObjectOpenHashMap<Cell> Cells = new Long2ObjectOpenHashMap<>(Nodes.size());
        List<Cell> All = new ArrayList<>();
        Long2ByteOpenHashMap Rim = new Long2ByteOpenHashMap();
        Rim.defaultReturnValue(Unknown);
        Cell Landing = null;
        int MinX = Integer.MAX_VALUE;
        int MinY = Integer.MAX_VALUE;
        int MinZ = Integer.MAX_VALUE;
        int MaxX = Integer.MIN_VALUE;
        int MaxY = Integer.MIN_VALUE;
        int MaxZ = Integer.MIN_VALUE;
        for (Node Each : Nodes.values()) {
            Cell Made = new Cell(Each.X, Each.Y, Each.Z, Each.Grounded ? Each.FloorY : Each.Y, Each.StepX, Each.StepZ, Each.Distance, Each.Grounded);
            Cells.put(BlockPos.asLong(Each.X, Each.Y, Each.Z), Made);
            All.add(Made);
            if (Each == Start) {
                Landing = Made;
            }
            MinX = Math.min(MinX, Each.X);
            MinY = Math.min(MinY, Each.Y);
            MinZ = Math.min(MinZ, Each.Z);
            MaxX = Math.max(MaxX, Each.X);
            MaxY = Math.max(MaxY, Each.Y);
            MaxZ = Math.max(MaxZ, Each.Z);
        }
        for (Node Each : Nodes.values()) {
            if (!Each.Grounded) {
                continue;
            }
            for (int[] Step : Steps) {
                int Nx = Each.X + Step[0];
                int Nz = Each.Z + Step[1];
                long Key = BlockPos.asLong(Nx, Each.Y, Nz);
                if (Nodes.containsKey(Key) || Rim.containsKey(Key)) {
                    continue;
                }
                Rim.put(Key, Look.passable(Nx, Each.Y, Nz) ? Open : Solid);
            }
        }
        AABB Bounds = All.isEmpty() ? new AABB(0, 0, 0, 0, 0, 0) : new AABB(MinX - 1, MinY - 1, MinZ - 1, MaxX + 2, MaxY + 2, MaxZ + 2);
        return new ColdFloorField(Cells, All.toArray(new Cell[0]), Rim, Look, Landing, Bounds);
    }

    private static final class Probe {
        private final Level World;
        private final long Skip;
        private final Long2ObjectOpenHashMap<VoxelShape> Known = new Long2ObjectOpenHashMap<>();
        private final Map<VoxelShape, List<AABB>> Boxes = new IdentityHashMap<>();
        private final BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        private final LongArrayList Positions = new LongArrayList();
        private final List<BlockState> States = new ArrayList<>();
        private final List<VoxelShape> Outlines = new ArrayList<>();

        private Probe(Level World, BlockPos Emitter) {
            this.World = World;
            this.Skip = Emitter.asLong();
        }

        private VoxelShape shape(int X, int Y, int Z) {
            long Key = BlockPos.asLong(X, Y, Z);
            VoxelShape Found = Known.get(Key);
            if (Found != null) {
                return Found;
            }
            Cursor.set(X, Y, Z);
            BlockState State;
            VoxelShape Shape;
            if (World.isLoaded(Cursor)) {
                State = World.getBlockState(Cursor);
                Shape = State.getCollisionShape(World, Cursor);
            } else {
                State = Unloaded;
                Shape = Shapes.block();
            }
            Known.put(Key, Shape);
            if (Key != Skip) {
                Positions.add(Key);
                States.add(State);
                Outlines.add(Shape);
            }
            return Shape;
        }

        private boolean hits(int X, int Y, int Z, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
            VoxelShape Shape = shape(X, Y, Z);
            if (Shape.isEmpty()) {
                return false;
            }
            if (Shape == Shapes.block()) {
                return true;
            }
            for (AABB Box : Boxes.computeIfAbsent(Shape, VoxelShape::toAabbs)) {
                if (Box.minX < X1 && Box.maxX > X0 && Box.minY < Y1 && Box.maxY > Y0 && Box.minZ < Z1 && Box.maxZ > Z0) {
                    return true;
                }
            }
            return false;
        }

        private boolean passable(int X, int Y, int Z) {
            return !hits(X, Y, Z, CoreLow, BandLow, CoreLow, CoreHigh, BandHigh, CoreHigh);
        }

        private boolean supported(int X, int Y, int Z) {
            return hits(X, Y, Z, CoreLow, 0.0, CoreLow, CoreHigh, BandLow, CoreHigh) || hits(X, Y - 1, Z, CoreLow, FloorProbe, CoreLow, CoreHigh, 1.0, CoreHigh);
        }

        private double floorTop(int X, int Y, int Z) {
            VoxelShape Own = shape(X, Y, Z);
            if (hits(X, Y, Z, CoreLow, 0.0, CoreLow, CoreHigh, BandLow, CoreHigh)) {
                return Y + Math.min(Own.max(Direction.Axis.Y), BandLow);
            }
            VoxelShape Under = shape(X, Y - 1, Z);
            double Top = Under.isEmpty() ? 1.0 : Under.max(Direction.Axis.Y);
            return Y + Mth.clamp(Top - 1.0, LowestFloor, 0.0);
        }

        private boolean stepOpen(int X, int Y, int Z, int Dx, int Dz) {
            if (Dx != 0) {
                double NearLow = Dx > 0 ? 0.5 : 0.0;
                double FarLow = Dx > 0 ? 0.0 : 0.5;
                return !hits(X, Y, Z, NearLow, BandLow, LaneLow, NearLow + 0.5, BandHigh, LaneHigh)
                        && !hits(X + Dx, Y, Z, FarLow, BandLow, LaneLow, FarLow + 0.5, BandHigh, LaneHigh);
            }
            double NearLow = Dz > 0 ? 0.5 : 0.0;
            double FarLow = Dz > 0 ? 0.0 : 0.5;
            return !hits(X, Y, Z, LaneLow, BandLow, NearLow, LaneHigh, BandHigh, NearLow + 0.5)
                    && !hits(X, Y, Z + Dz, LaneLow, BandLow, FarLow, LaneHigh, BandHigh, FarLow + 0.5);
        }
    }
}
