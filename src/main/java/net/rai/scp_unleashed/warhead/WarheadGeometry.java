package net.rai.scp_unleashed.warhead;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public final class WarheadGeometry {
    public static final int OriginColumn = 1;
    public static final int WarheadWidth = 3;
    public static final int WarheadHeight = 2;
    public static final int WarheadDepth = 2;
    public static final int WarheadCells = WarheadWidth * WarheadHeight * WarheadDepth;
    public static final int WarheadOrigin = 1;
    public static final int PanelWidth = 2;
    public static final int PanelHeight = 2;
    public static final int PanelCells = PanelWidth * PanelHeight;
    public static final int PanelOrigin = 1;
    public static final Vec3 WarheadCentre = new Vec3(24.0, 18.0, 16.0);
    public static final List<AABB> WarheadBoxes = List.of(
            new AABB(1, 10, 5, 39, 26, 27),
            new AABB(5, 7, 9, 39, 29, 23),
            new AABB(39, 5, 3, 47, 31, 29),
            new AABB(10, 2, 1, 14, 16, 31),
            new AABB(34, 2, 1, 38, 16, 31),
            new AABB(3, 0, 2, 45, 2, 6),
            new AABB(3, 0, 26, 45, 2, 30),
            new AABB(19, 2, 1, 29, 8, 4));
    public static final AABB KeycardBox = new AABB(24, 3, 1.5, 29, 12, 4);
    public static final AABB LeverBox = new AABB(17, 3, -1.5, 22, 12, 4);
    public static final AABB CoverClosedBox = new AABB(8, 3, -0.5, 16, 11, 1.2);
    public static final AABB CoverOpenBox = new AABB(8, 0.5, -8.5, 16, 4.5, 1.0);
    public static final AABB ButtonBox = new AABB(9, 4, 1, 15, 10, 4);
    public static final AABB AbortBox = new AABB(3, 5, 1.5, 7, 9, 4);
    public static final Vec3 LeverPivot = new Vec3(19.5, 7.5, 3.5);
    public static final Vec3 CoverHinge = new Vec3(12.0, 3.0, 0.5);
    public static final Vec3 ButtonOrigin = new Vec3(12.0, 7.0, 4.0);
    public static final Vec3 AbortOrigin = new Vec3(5.0, 7.0, 4.0);
    public static final float LeverAngle = 40.0f;
    public static final float CoverOpenAngle = -105.0f;
    public static final float ButtonTravel = 1.0f;
    public static final float AbortTravel = 0.75f;
    public static final float FaceDepth = 3.95f;
    public static final float LedDepth = 1.95f;
    public static final int[] LampLefts = {17, 20, 23, 26};
    public static final int LampRow = 21;
    public static final int[] DigitLefts = {4, 9};
    public static final int DigitBase = 18;
    public static final float LcdLeft = 4.0f;
    public static final float LcdBase = 14.3f;
    public static final float LcdHeight = 1.5f;
    public static final float LedU = 5.0f;
    public static final float LedV = 10.0f;

    private WarheadGeometry() {
    }

    public static int column(int Index, int Width) {
        return Index % Width;
    }

    public static int row(int Index, int Width, int Height) {
        return Index / Width % Height;
    }

    public static int layer(int Index, int Width, int Height) {
        return Index / (Width * Height);
    }

    public static BlockPos turn(int X, int Y, int Z, Direction Front) {
        return switch (Front) {
            case EAST -> new BlockPos(-Z, Y, X);
            case SOUTH -> new BlockPos(-X, Y, -Z);
            case WEST -> new BlockPos(Z, Y, -X);
            default -> new BlockPos(X, Y, Z);
        };
    }

    public static BlockPos offsetOf(int Index, int Width, int Height, Direction Front) {
        return turn(column(Index, Width) - OriginColumn, row(Index, Width, Height), layer(Index, Width, Height), Front);
    }

    public static BlockPos cellPos(BlockPos Origin, Direction Front, int Index, int Width, int Height) {
        return Origin.offset(offsetOf(Index, Width, Height, Front));
    }

    public static BlockPos originOf(BlockPos Cell, Direction Front, int Index, int Width, int Height) {
        return Cell.subtract(offsetOf(Index, Width, Height, Front));
    }

    public static float yaw(Direction Front) {
        return 180.0f - Front.toYRot();
    }

    private static double[] intoModel(double X, double Z, Direction Front) {
        return switch (Front) {
            case EAST -> new double[]{Z, -X};
            case SOUTH -> new double[]{-X, -Z};
            case WEST -> new double[]{-Z, X};
            default -> new double[]{X, Z};
        };
    }

    private static double[] outOfModel(double X, double Z, Direction Front) {
        return switch (Front) {
            case EAST -> new double[]{-Z, X};
            case SOUTH -> new double[]{-X, -Z};
            case WEST -> new double[]{Z, -X};
            default -> new double[]{X, Z};
        };
    }

    public static Vec3 toStructure(Vec3 Point, BlockPos Origin, Direction Front) {
        double[] Model = intoModel(Point.x - (Origin.getX() + 0.5), Point.z - (Origin.getZ() + 0.5), Front);
        return new Vec3((Model[0] + 0.5 + OriginColumn) * 16.0, (Point.y - Origin.getY()) * 16.0, (Model[1] + 0.5) * 16.0);
    }

    public static Vec3 toStructureDirection(Vec3 Heading, Direction Front) {
        double[] Model = intoModel(Heading.x, Heading.z, Front);
        return new Vec3(Model[0], Heading.y, Model[1]);
    }

    public static Vec3 toWorld(Vec3 Point, BlockPos Origin, Direction Front) {
        double[] Placed = outOfModel(Point.x / 16.0 - 0.5 - OriginColumn, Point.z / 16.0 - 0.5, Front);
        return new Vec3(Origin.getX() + 0.5 + Placed[0], Origin.getY() + Point.y / 16.0, Origin.getZ() + 0.5 + Placed[1]);
    }

    public static double rayHit(AABB Box, Vec3 Start, Vec3 Heading) {
        double Near = -Double.MAX_VALUE;
        double Far = Double.MAX_VALUE;
        double[] Low = {Box.minX, Box.minY, Box.minZ};
        double[] High = {Box.maxX, Box.maxY, Box.maxZ};
        double[] From = {Start.x, Start.y, Start.z};
        double[] Step = {Heading.x, Heading.y, Heading.z};
        for (int Axis = 0; Axis < 3; Axis++) {
            if (Math.abs(Step[Axis]) < 1.0E-9) {
                if (From[Axis] < Low[Axis] || From[Axis] > High[Axis]) {
                    return -1.0;
                }
                continue;
            }
            double First = (Low[Axis] - From[Axis]) / Step[Axis];
            double Second = (High[Axis] - From[Axis]) / Step[Axis];
            Near = Math.max(Near, Math.min(First, Second));
            Far = Math.min(Far, Math.max(First, Second));
        }
        if (Near > Far || Far < 0.0) {
            return -1.0;
        }
        return Math.max(Near, 0.0);
    }

    public static VoxelShape turnedBox(Direction Front, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Front) {
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    public static VoxelShape cellShape(List<AABB> Boxes, Direction Front, int Column, int Row, int Layer) {
        double CellX = Column * 16.0;
        double CellY = Row * 16.0;
        double CellZ = Layer * 16.0;
        VoxelShape Shape = Shapes.empty();
        for (AABB Piece : Boxes) {
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
        return Shape.optimize();
    }
}
