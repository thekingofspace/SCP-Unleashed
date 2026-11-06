package net.rai.scp_unleashed.scp106;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class Scp106FemurLayout {
    public static final int Columns = 2;
    public static final int Rows = 2;
    public static final int CellCount = Columns * Rows;
    public static final int MasterPart = 0;
    public static final double SeatHeight = 9.0 / 16.0;
    public static final double SeatBack = 0.19;
    public static final float PivotX = -4.0f;
    public static final float PivotY = 28.0f;
    public static final float PivotZ = 3.5f;
    public static final float LeverX = -9.0f;
    public static final float LeverY = 15.0f;
    public static final float LeverZ = 0.0f;
    public static final List<AABB> Boxes = List.of(
            new AABB(16, 0, 0, 32, 2, 16),
            new AABB(18, 2, 7, 30, 9, 15),
            new AABB(18, 9, 13, 30, 26, 15),
            new AABB(20, 26, 13, 28, 30, 15),
            new AABB(16, 9, 8, 18, 12, 14),
            new AABB(30, 9, 8, 32, 12, 14),
            new AABB(19, 2, 0, 29, 6, 7),
            new AABB(0, 0, 2, 14, 3, 14),
            new AABB(3, 3, 5, 11, 28, 11),
            new AABB(2, 26, 4, 12, 31, 12));
    private static final VoxelShape[] CellShapes = new VoxelShape[4 * CellCount];

    static {
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            for (int Part = 0; Part < CellCount; Part++) {
                CellShapes[Front.get2DDataValue() * CellCount + Part] = cellShape(Front, column(Part), row(Part));
            }
        }
    }

    private Scp106FemurLayout() {
    }

    public static int part(int Column, int Row) {
        return Column + Row * Columns;
    }

    public static int column(int Part) {
        return Part % Columns;
    }

    public static int row(int Part) {
        return Part / Columns;
    }

    public static boolean isChair(int Part) {
        return column(Part) == 0;
    }

    public static BlockPos turn(int X, int Y, int Z, Direction Front) {
        return switch (Front) {
            case EAST -> new BlockPos(-Z, Y, X);
            case SOUTH -> new BlockPos(-X, Y, -Z);
            case WEST -> new BlockPos(Z, Y, -X);
            default -> new BlockPos(X, Y, Z);
        };
    }

    public static BlockPos cellPos(BlockPos Origin, Direction Front, int Part) {
        return Origin.offset(turn(-column(Part), row(Part), 0, Front));
    }

    public static BlockPos originOf(BlockPos Cell, Direction Front, int Part) {
        return Cell.subtract(turn(-column(Part), row(Part), 0, Front));
    }

    public static List<BlockPos> cells(BlockPos Origin, Direction Front) {
        List<BlockPos> Cells = new ArrayList<>(CellCount);
        for (int Part = 0; Part < CellCount; Part++) {
            Cells.add(cellPos(Origin, Front, Part));
        }
        return Cells;
    }

    public static Vec3 seatSpot(BlockPos Origin, Direction Front) {
        Direction Back = Front.getOpposite();
        return new Vec3(Origin.getX() + 0.5 + Back.getStepX() * SeatBack, Origin.getY() + SeatHeight, Origin.getZ() + 0.5 + Back.getStepZ() * SeatBack);
    }

    public static Vec3 frontSpot(BlockPos Origin, Direction Front) {
        return Vec3.atBottomCenterOf(Origin.relative(Front));
    }

    public static VoxelShape shape(Direction Front, int Part) {
        return CellShapes[Front.get2DDataValue() * CellCount + Math.floorMod(Part, CellCount)];
    }

    private static VoxelShape turnedBox(Direction Front, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Front) {
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    private static VoxelShape cellShape(Direction Front, int Column, int Row) {
        double CellX = (1 - Column) * 16.0;
        double CellY = Row * 16.0;
        VoxelShape Shape = Shapes.empty();
        for (AABB Piece : Boxes) {
            double X0 = Math.max(Piece.minX, CellX) - CellX;
            double Y0 = Math.max(Piece.minY, CellY) - CellY;
            double X1 = Math.min(Piece.maxX, CellX + 16.0) - CellX;
            double Y1 = Math.min(Piece.maxY, CellY + 16.0) - CellY;
            double Z0 = Math.max(Piece.minZ, 0.0);
            double Z1 = Math.min(Piece.maxZ, 16.0);
            if (X1 > X0 && Y1 > Y0 && Z1 > Z0) {
                Shape = Shapes.or(Shape, turnedBox(Front, X0, Y0, Z0, X1, Y1, Z1));
            }
        }
        return Shape.isEmpty() ? Block.box(0, 0, 0, 16, 2, 16) : Shape.optimize();
    }
}
