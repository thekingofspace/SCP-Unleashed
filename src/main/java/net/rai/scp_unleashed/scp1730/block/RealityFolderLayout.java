package net.rai.scp_unleashed.scp1730.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class RealityFolderLayout {
    public static final int Width = 5;
    public static final int Height = 6;
    public static final int Depth = 5;
    public static final int Half = 2;
    public static final int CellCount = Width * Height * Depth;
    public static final int MasterPart = part(Half, 0, Half);
    public static final List<AABB> Hull = List.of(
            new AABB(0, 0, 0, 80, 8, 80),
            new AABB(18, 8, 18, 62, 60, 62),
            new AABB(28, 60, 28, 52, 94, 52),
            new AABB(0, 8, 0, 21, 80, 21),
            new AABB(59, 8, 0, 80, 80, 21),
            new AABB(0, 8, 59, 21, 80, 80),
            new AABB(59, 8, 59, 80, 80, 80),
            new AABB(17, 8, 6, 63, 18, 18),
            new AABB(17, 8, 62, 63, 18, 74),
            new AABB(6, 8, 17, 18, 18, 63),
            new AABB(62, 8, 17, 74, 18, 63));
    private static final VoxelShape[] Shapes0 = new VoxelShape[CellCount];

    static {
        for (int Part = 0; Part < CellCount; Part++) {
            Shapes0[Part] = cellShape(column(Part), row(Part), layer(Part));
        }
    }

    private RealityFolderLayout() {
    }

    public static int part(int Column, int Row, int Layer) {
        return Column + Row * Width + Layer * Width * Height;
    }

    public static int column(int Part) {
        return Part % Width;
    }

    public static int row(int Part) {
        return Part / Width % Height;
    }

    public static int layer(int Part) {
        return Part / (Width * Height);
    }

    public static BlockPos cellPos(BlockPos Master, int Part) {
        return Master.offset(column(Part) - Half, row(Part), layer(Part) - Half);
    }

    public static BlockPos masterOf(BlockPos Cell, int Part) {
        return Cell.offset(Half - column(Part), -row(Part), Half - layer(Part));
    }

    public static List<BlockPos> cells(BlockPos Master) {
        List<BlockPos> Cells = new ArrayList<>(CellCount);
        for (int Part = 0; Part < CellCount; Part++) {
            Cells.add(cellPos(Master, Part));
        }
        return Cells;
    }

    public static VoxelShape shape(int Part) {
        return Shapes0[Math.floorMod(Part, CellCount)];
    }

    public static VoxelShape shape(int Part, Direction Facing) {
        Direction Side = portSide(Part, Facing);
        return Side == null ? shape(Part) : Shapes.or(shape(Part), portShape(Side));
    }

    public static Direction portSide(int Part, Direction Facing) {
        if (row(Part) != 0 || !Facing.getAxis().isHorizontal()) {
            return null;
        }
        for (Direction Side : new Direction[]{Facing.getOpposite(), Facing.getClockWise(), Facing.getCounterClockWise()}) {
            if (column(Part) == Half + Side.getStepX() * Half && layer(Part) == Half + Side.getStepZ() * Half) {
                return Side;
            }
        }
        return null;
    }

    public static List<Integer> ports(Direction Facing) {
        List<Integer> Found = new ArrayList<>();
        for (Direction Side : new Direction[]{Facing.getOpposite(), Facing.getClockWise(), Facing.getCounterClockWise()}) {
            Found.add(part(Half + Side.getStepX() * Half, 0, Half + Side.getStepZ() * Half));
        }
        return Found;
    }

    public static VoxelShape portShape(Direction Side) {
        return switch (Side) {
            case SOUTH -> Block.box(3, 0, 9, 13, 14, 16);
            case EAST -> Block.box(9, 0, 3, 16, 14, 13);
            case WEST -> Block.box(0, 0, 3, 7, 14, 13);
            default -> Block.box(3, 0, 0, 13, 14, 7);
        };
    }

    private static VoxelShape cellShape(int Column, int Row, int Layer) {
        double CellX = Column * 16.0;
        double CellY = Row * 16.0;
        double CellZ = Layer * 16.0;
        VoxelShape Shape = Shapes.empty();
        for (AABB Piece : Hull) {
            double X0 = Math.max(Piece.minX, CellX) - CellX;
            double Y0 = Math.max(Piece.minY, CellY) - CellY;
            double Z0 = Math.max(Piece.minZ, CellZ) - CellZ;
            double X1 = Math.min(Piece.maxX, CellX + 16.0) - CellX;
            double Y1 = Math.min(Piece.maxY, CellY + 16.0) - CellY;
            double Z1 = Math.min(Piece.maxZ, CellZ + 16.0) - CellZ;
            if (X1 > X0 && Y1 > Y0 && Z1 > Z0) {
                Shape = Shapes.or(Shape, Block.box(X0, Y0, Z0, X1, Y1, Z1));
            }
        }
        return Shape.optimize();
    }
}
