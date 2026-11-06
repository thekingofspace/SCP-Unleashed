package net.rai.scp_unleashed.scp465;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.advmaterials.MagnetizedDoorBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;

import java.util.Arrays;

public final class Scp465Room {
    public static final int MaxSpan = 24;
    public static final int MinSpan = 5;
    public static final int MinHeight = 3;
    public static final int MinFloor = 20;
    private static final Direction[] Sides = Direction.values();

    public enum Verdict {
        Fits,
        Leaks,
        TooBig,
        TooSmall,
        Taken
    }

    public record Scan(Verdict Result, LongOpenHashSet Cells, LongOpenHashSet Open, LongOpenHashSet Kept, int FloorY, BlockPos Low, BlockPos High) {
        public boolean fits() {
            return Result == Verdict.Fits;
        }

        public static Scan refused(Verdict Result) {
            return new Scan(Result, new LongOpenHashSet(), new LongOpenHashSet(), new LongOpenHashSet(), 0, BlockPos.ZERO, BlockPos.ZERO);
        }

        public Scan withResult(Verdict Changed) {
            return new Scan(Changed, Cells, Open, Kept, FloorY, Low, High);
        }
    }

    private Scp465Room() {
    }

    public static boolean barrier(BlockState State) {
        if (State.getBlock() instanceof FenceGateBlock) {
            return !State.getValue(FenceGateBlock.OPEN);
        }
        return State.getBlock() instanceof CrossCollisionBlock || State.getBlock() instanceof WallBlock;
    }

    public static boolean sealed(Level World, BlockPos From, BlockState Here, Direction Side, BlockPos To, BlockState There) {
        if (barrier(There)) {
            return true;
        }
        if (blocksAcross(wallShape(World, From, Here), Side)) {
            return true;
        }
        return blocksAcross(wallShape(World, To, There), Side.getOpposite());
    }

    public static VoxelShape wallShape(Level World, BlockPos Pos, BlockState State) {
        if (State.getBlock() instanceof PanelDoorBlock || State.getBlock() instanceof MagnetizedDoorBlock) {
            return State.getShape(World, Pos);
        }
        return State.getCollisionShape(World, Pos);
    }

    public static boolean blocksAcross(VoxelShape Shape, Direction Side) {
        if (Shape.isEmpty()) {
            return false;
        }
        if (Block.isFaceFull(Shape, Side)) {
            return true;
        }
        if (Side.getAxis() == Direction.Axis.Y) {
            return false;
        }
        boolean AlongX = Side.getAxis() == Direction.Axis.X;
        VoxelShape Spread = Shapes.empty();
        for (AABB Part : Shape.toAabbs()) {
            double LowY = Math.max(0.0, Part.minY);
            double HighY = Math.min(1.0, Part.maxY);
            double LowSide = Math.max(0.0, AlongX ? Part.minZ : Part.minX);
            double HighSide = Math.min(1.0, AlongX ? Part.maxZ : Part.maxX);
            if (HighY <= LowY || HighSide <= LowSide) {
                continue;
            }
            VoxelShape Slab = AlongX ? Shapes.box(0.0, LowY, LowSide, 1.0, HighY, HighSide) : Shapes.box(LowSide, LowY, 0.0, HighSide, HighY, 1.0);
            Spread = Shapes.joinUnoptimized(Spread, Slab, BooleanOp.OR);
        }
        return Block.isFaceFull(Spread.optimize(), Side);
    }

    public static boolean solidCell(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return barrier(State) || State.isCollisionShapeFullBlock(World, Pos);
    }

    public static boolean underSky(Level World, BlockPos Pos) {
        return Pos.getY() >= World.getHeight(Heightmap.Types.MOTION_BLOCKING, Pos.getX(), Pos.getZ());
    }

    public static Scan scan(Level World, BlockPos Start) {
        return scan(World, Start, MaxSpan, MaxSpan);
    }

    public static Scan scan(Level World, BlockPos Start, int SpanAcross, int SpanUp) {
        LongOpenHashSet Open = new LongOpenHashSet();
        LongArrayFIFOQueue Queue = new LongArrayFIFOQueue();
        Open.add(Start.asLong());
        Queue.enqueue(Start.asLong());
        int MinX = Start.getX();
        int MinY = Start.getY();
        int MinZ = Start.getZ();
        int MaxX = MinX;
        int MaxY = MinY;
        int MaxZ = MinZ;
        BlockPos.MutableBlockPos From = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos To = new BlockPos.MutableBlockPos();
        if (underSky(World, Start)) {
            return Scan.refused(Verdict.Leaks);
        }
        while (!Queue.isEmpty()) {
            From.set(Queue.dequeueLong());
            BlockState Here = World.getBlockState(From);
            for (Direction Side : Sides) {
                To.setWithOffset(From, Side);
                long Next = To.asLong();
                if (Open.contains(Next)) {
                    continue;
                }
                if (World.isOutsideBuildHeight(To) || !World.isLoaded(To)) {
                    return Scan.refused(Verdict.Leaks);
                }
                BlockState There = World.getBlockState(To);
                if (sealed(World, From, Here, Side, To, There)) {
                    continue;
                }
                if (underSky(World, To)) {
                    return Scan.refused(Verdict.Leaks);
                }
                Open.add(Next);
                Queue.enqueue(Next);
                MinX = Math.min(MinX, To.getX());
                MinY = Math.min(MinY, To.getY());
                MinZ = Math.min(MinZ, To.getZ());
                MaxX = Math.max(MaxX, To.getX());
                MaxY = Math.max(MaxY, To.getY());
                MaxZ = Math.max(MaxZ, To.getZ());
                if (MaxX - MinX + 1 > SpanAcross || MaxY - MinY + 1 > SpanUp || MaxZ - MinZ + 1 > SpanAcross) {
                    return Scan.refused(Verdict.TooBig);
                }
            }
        }
        BlockPos Low = new BlockPos(MinX, MinY, MinZ);
        BlockPos High = new BlockPos(MaxX, MaxY, MaxZ);
        LongOpenHashSet Cells = new LongOpenHashSet(Open);
        Cells.addAll(interiorSolids(World, Open, Low, High));
        LongOpenHashSet Kept = new LongOpenHashSet();
        BlockPos.MutableBlockPos Column = Start.mutable();
        while (Cells.contains(Column.asLong())) {
            Kept.add(Column.asLong());
            Column.move(Direction.DOWN);
        }
        int FloorY = floorLevel(Cells, Low, High);
        Scan Found = new Scan(Verdict.Fits, Cells, Open, Kept, FloorY, Low, High);
        return roomy(Found) ? Found : Found.withResult(Verdict.TooSmall);
    }

    private static LongOpenHashSet interiorSolids(Level World, LongOpenHashSet Open, BlockPos Low, BlockPos High) {
        int SizeX = High.getX() - Low.getX() + 1;
        int SizeY = High.getY() - Low.getY() + 1;
        int SizeZ = High.getZ() - Low.getZ() + 1;
        int[] ColumnTop = new int[SizeX * SizeZ];
        int[] ColumnBottom = new int[SizeX * SizeZ];
        int[] RowLowX = new int[SizeY * SizeZ];
        int[] RowHighX = new int[SizeY * SizeZ];
        int[] RowLowZ = new int[SizeX * SizeY];
        int[] RowHighZ = new int[SizeX * SizeY];
        Arrays.fill(ColumnTop, Integer.MIN_VALUE);
        Arrays.fill(ColumnBottom, Integer.MAX_VALUE);
        Arrays.fill(RowLowX, Integer.MAX_VALUE);
        Arrays.fill(RowHighX, Integer.MIN_VALUE);
        Arrays.fill(RowLowZ, Integer.MAX_VALUE);
        Arrays.fill(RowHighZ, Integer.MIN_VALUE);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (long Packed : Open) {
            Cursor.set(Packed);
            int X = Cursor.getX() - Low.getX();
            int Y = Cursor.getY() - Low.getY();
            int Z = Cursor.getZ() - Low.getZ();
            int Column = X * SizeZ + Z;
            ColumnTop[Column] = Math.max(ColumnTop[Column], Y);
            ColumnBottom[Column] = Math.min(ColumnBottom[Column], Y);
            int RowX = Y * SizeZ + Z;
            RowLowX[RowX] = Math.min(RowLowX[RowX], X);
            RowHighX[RowX] = Math.max(RowHighX[RowX], X);
            int RowZ = X * SizeY + Y;
            RowLowZ[RowZ] = Math.min(RowLowZ[RowZ], Z);
            RowHighZ[RowZ] = Math.max(RowHighZ[RowZ], Z);
        }
        LongOpenHashSet Candidates = new LongOpenHashSet();
        for (int X = 0; X < SizeX; X++) {
            for (int Y = 0; Y < SizeY; Y++) {
                for (int Z = 0; Z < SizeZ; Z++) {
                    Cursor.set(Low.getX() + X, Low.getY() + Y, Low.getZ() + Z);
                    long Packed = Cursor.asLong();
                    if (Open.contains(Packed)) {
                        continue;
                    }
                    int Column = X * SizeZ + Z;
                    boolean Above = ColumnTop[Column] > Y;
                    boolean Below = ColumnBottom[Column] < Y;
                    int RowX = Y * SizeZ + Z;
                    boolean AcrossX = RowLowX[RowX] < X && RowHighX[RowX] > X;
                    int RowZ = X * SizeY + Y;
                    boolean AcrossZ = RowLowZ[RowZ] < Z && RowHighZ[RowZ] > Z;
                    if (Above || Below || AcrossX || AcrossZ) {
                        Candidates.add(Packed);
                    }
                }
            }
        }
        LongOpenHashSet Accepted = new LongOpenHashSet();
        BlockPos.MutableBlockPos Neighbour = new BlockPos.MutableBlockPos();
        for (long Packed : Candidates) {
            Cursor.set(Packed);
            boolean Safe = true;
            for (Direction Side : Sides) {
                Neighbour.setWithOffset(Cursor, Side);
                long Near = Neighbour.asLong();
                if (Open.contains(Near) || Candidates.contains(Near)) {
                    continue;
                }
                if (!solidCell(World, Neighbour)) {
                    Safe = false;
                    break;
                }
            }
            if (Safe) {
                Accepted.add(Packed);
            }
        }
        return Accepted;
    }

    public static int floorLevel(LongOpenHashSet Cells, BlockPos Low, BlockPos High) {
        int[] Counts = new int[High.getY() - Low.getY() + 1];
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (long Packed : Cells) {
            Cursor.set(Packed);
            if (!Cells.contains(BlockPos.offset(Packed, Direction.DOWN))) {
                int Index = Cursor.getY() - Low.getY();
                if (Index >= 0 && Index < Counts.length) {
                    Counts[Index]++;
                }
            }
        }
        int Best = 0;
        for (int Index = 1; Index < Counts.length; Index++) {
            if (Counts[Index] > Counts[Best]) {
                Best = Index;
            }
        }
        return Low.getY() + Best;
    }

    public static int headroom(LongOpenHashSet Cells, int X, int FloorY, int Z) {
        int Height = 0;
        while (Height <= MaxSpan && Cells.contains(BlockPos.asLong(X, FloorY + Height, Z))) {
            Height++;
        }
        return Height;
    }

    public static boolean floorCell(LongOpenHashSet Cells, int X, int FloorY, int Z) {
        return Cells.contains(BlockPos.asLong(X, FloorY, Z)) && !Cells.contains(BlockPos.asLong(X, FloorY - 1, Z));
    }

    private static boolean roomy(Scan Found) {
        int Floor = 0;
        int Tall = 0;
        int LowX = Integer.MAX_VALUE;
        int HighX = Integer.MIN_VALUE;
        int LowZ = Integer.MAX_VALUE;
        int HighZ = Integer.MIN_VALUE;
        for (int X = Found.Low().getX(); X <= Found.High().getX(); X++) {
            for (int Z = Found.Low().getZ(); Z <= Found.High().getZ(); Z++) {
                if (!floorCell(Found.Cells(), X, Found.FloorY(), Z)) {
                    continue;
                }
                int Height = headroom(Found.Cells(), X, Found.FloorY(), Z);
                if (Height < 2) {
                    continue;
                }
                Floor++;
                if (Height >= MinHeight) {
                    Tall++;
                }
                LowX = Math.min(LowX, X);
                HighX = Math.max(HighX, X);
                LowZ = Math.min(LowZ, Z);
                HighZ = Math.max(HighZ, Z);
            }
        }
        return Floor >= MinFloor && Tall >= MinFloor && HighX - LowX + 1 >= MinSpan && HighZ - LowZ + 1 >= MinSpan;
    }
}
