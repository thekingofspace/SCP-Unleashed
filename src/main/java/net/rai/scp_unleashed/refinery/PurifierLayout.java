package net.rai.scp_unleashed.refinery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PurifierLayout {
    public static final int Width = 2;
    public static final int Height = 2;
    public static final int Depth = 2;
    public static final int CellCount = Width * Height * Depth;
    public static final int MasterPart = 0;
    public static final int PanelPart = part(0, 0, 0);
    public static final int DrumPart = part(1, 0, 0);
    public static final int WaterPart = part(1, 0, 1);
    public static final int WastePart = part(0, 0, 1);
    public static final List<AABB> Boxes = List.of(
            new AABB(0, 0, 0, 32, 18, 32),
            new AABB(1, 18, 17, 31, 31, 31),
            new AABB(17, 18, 1, 31, 32, 15),
            new AABB(3, 18, 3, 13, 30, 14),
            new AABB(13, 19, 6, 19, 23, 10));
    public static final List<AABB> Hull = List.of(
            new AABB(0, 0, 0, 32, 16, 32),
            new AABB(0, 16, 0, 16, 28, 16),
            new AABB(16, 16, 0, 32, 32, 16),
            new AABB(0, 16, 16, 32, 30, 32));
    private static final VoxelShape[] CellShapes = new VoxelShape[4 * CellCount];
    private static final VoxelShape[] HullShapes = new VoxelShape[4 * CellCount];

    static {
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            for (int Part = 0; Part < CellCount; Part++) {
                CellShapes[slot(Front, Part)] = cellShape(Boxes, Front, column(Part), row(Part), layer(Part));
                HullShapes[slot(Front, Part)] = cellShape(Hull, Front, column(Part), row(Part), layer(Part));
            }
        }
    }

    private PurifierLayout() {
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

    private static int slot(Direction Front, int Part) {
        return Front.get2DDataValue() * CellCount + Part;
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
        return Origin.offset(turn(column(Part), row(Part), layer(Part), Front));
    }

    public static BlockPos originOf(BlockPos Cell, Direction Front, int Part) {
        return Cell.subtract(turn(column(Part), row(Part), layer(Part), Front));
    }

    public static List<BlockPos> cells(BlockPos Origin, Direction Front) {
        List<BlockPos> Cells = new ArrayList<>(CellCount);
        for (int Part = 0; Part < CellCount; Part++) {
            Cells.add(cellPos(Origin, Front, Part));
        }
        return Cells;
    }

    public static Vec3 toWorld(BlockPos Origin, Direction Front, double X, double Y, double Z) {
        double Ox = X / 16.0 - 0.5;
        double Oz = Z / 16.0 - 0.5;
        double[] Turned = switch (Front) {
            case EAST -> new double[]{-Oz, Ox};
            case SOUTH -> new double[]{-Ox, -Oz};
            case WEST -> new double[]{Oz, -Ox};
            default -> new double[]{Ox, Oz};
        };
        return new Vec3(Origin.getX() + 0.5 + Turned[0], Origin.getY() + Y / 16.0, Origin.getZ() + 0.5 + Turned[1]);
    }

    public static Vec3 centre(BlockPos Origin, Direction Front) {
        return toWorld(Origin, Front, Width * 8.0, Height * 8.0, Depth * 8.0);
    }

    @Nullable
    public static Direction portSide(Direction Front, int Part) {
        if (Part == WaterPart) {
            return Front.getClockWise();
        }
        if (Part == WastePart) {
            return Front.getCounterClockWise();
        }
        return null;
    }

    public static VoxelShape shape(Direction Front, int Part) {
        return CellShapes[slot(Front, Math.floorMod(Part, CellCount))];
    }

    public static VoxelShape hull(Direction Front, int Part) {
        return HullShapes[slot(Front, Math.floorMod(Part, CellCount))];
    }

    private static VoxelShape turnedBox(Direction Front, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Front) {
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    private static VoxelShape cellShape(List<AABB> Pieces, Direction Front, int Column, int Row, int Layer) {
        double CellX = Column * 16.0;
        double CellY = Row * 16.0;
        double CellZ = Layer * 16.0;
        VoxelShape Shape = Shapes.empty();
        for (AABB Piece : Pieces) {
            double X0 = Math.max(Piece.minX, CellX) - CellX;
            double Y0 = Math.max(Piece.minY, CellY) - CellY;
            double Z0 = Math.max(Piece.minZ, CellZ) - CellZ;
            double X1 = Math.min(Piece.maxX, CellX + 16.0) - CellX;
            double Y1 = Math.min(Piece.maxY, CellY + 16.0) - CellY;
            double Z1 = Math.min(Piece.maxZ, CellZ + 16.0) - CellZ;
            if (X1 > X0 && Y1 > Y0 && Z1 > Z0) {
                Shape = Shapes.or(Shape, turnedBox(Front, X0, Y0, Z0, X1, Y1, Z1));
            }
        }
        return Shape.isEmpty() ? Block.box(0, 0, 0, 16, 2, 16) : Shape.optimize();
    }
}
