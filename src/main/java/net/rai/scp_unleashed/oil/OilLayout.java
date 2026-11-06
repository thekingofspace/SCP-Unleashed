package net.rai.scp_unleashed.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.wire.OutletMounts;

import java.util.ArrayList;
import java.util.List;

public final class OilLayout {
    private final int Width;
    private final int Height;
    private final int Depth;
    private final int PlacePart;
    private final List<AABB> Boxes;
    private final List<AABB> Sockets;
    private final VoxelShape[] CellShapes;
    private final VoxelShape[] SupportShapes;

    public OilLayout(int Width, int Height, int Depth, int PlacePart, List<AABB> Boxes, List<AABB> Sockets) {
        this.Width = Width;
        this.Height = Height;
        this.Depth = Depth;
        this.PlacePart = PlacePart;
        List<AABB> Solid = new ArrayList<>(Boxes);
        Solid.addAll(Sockets);
        this.Boxes = List.copyOf(Solid);
        this.Sockets = List.copyOf(Sockets);
        int Count = cellCount();
        this.CellShapes = new VoxelShape[4 * Count];
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            for (int Part = 0; Part < Count; Part++) {
                CellShapes[Front.get2DDataValue() * Count + Part] = cellShape(Front, column(Part), row(Part), layer(Part));
            }
        }
        this.SupportShapes = new VoxelShape[4 * Count];
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            for (int Part = 0; Part < Count; Part++) {
                SupportShapes[Front.get2DDataValue() * Count + Part] = supportShape(Front, Part);
            }
        }
    }

    public int getWidth() {
        return Width;
    }

    public int getHeight() {
        return Height;
    }

    public int getDepth() {
        return Depth;
    }

    public int getPlacePart() {
        return PlacePart;
    }

    public BlockPos masterFromPlaced(BlockPos Placed, Direction Front) {
        return originOf(Placed, Front, PlacePart);
    }

    public int cellCount() {
        return Width * Height * Depth;
    }

    public int part(int Column, int Row, int Layer) {
        return Column + Row * Width + Layer * Width * Height;
    }

    public int column(int Part) {
        return Part % Width;
    }

    public int row(int Part) {
        return Part / Width % Height;
    }

    public int layer(int Part) {
        return Part / (Width * Height);
    }

    public static BlockPos turn(int X, int Y, int Z, Direction Front) {
        return switch (Front) {
            case EAST -> new BlockPos(-Z, Y, X);
            case SOUTH -> new BlockPos(-X, Y, -Z);
            case WEST -> new BlockPos(Z, Y, -X);
            default -> new BlockPos(X, Y, Z);
        };
    }

    public BlockPos cellPos(BlockPos Origin, Direction Front, int Part) {
        return Origin.offset(turn(column(Part), row(Part), layer(Part), Front));
    }

    public BlockPos originOf(BlockPos Cell, Direction Front, int Part) {
        return Cell.subtract(turn(column(Part), row(Part), layer(Part), Front));
    }

    public List<BlockPos> cells(BlockPos Origin, Direction Front) {
        List<BlockPos> Cells = new ArrayList<>(cellCount());
        for (int Part = 0; Part < cellCount(); Part++) {
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

    public Vec3 centre(BlockPos Origin, Direction Front) {
        return toWorld(Origin, Front, Width * 8.0, Height * 8.0, Depth * 8.0);
    }

    public AABB bounds(BlockPos Origin, Direction Front) {
        AABB Box = new AABB(Origin);
        for (BlockPos Cell : cells(Origin, Front)) {
            Box = Box.minmax(new AABB(Cell));
        }
        return Box;
    }

    public static Direction localSide(Direction Front, Direction Side) {
        if (Side.getAxis() == Direction.Axis.Y) {
            return Side;
        }
        Direction Local = Side;
        Direction Turn = Front;
        while (Turn != Direction.NORTH) {
            Local = Local.getCounterClockWise();
            Turn = Turn.getCounterClockWise();
        }
        return Local;
    }

    public VoxelShape shape(Direction Front, int Part) {
        int Count = cellCount();
        return CellShapes[Front.get2DDataValue() * Count + Math.floorMod(Part, Count)];
    }

    public VoxelShape support(Direction Front, int Part) {
        int Count = cellCount();
        return SupportShapes[Front.get2DDataValue() * Count + Math.floorMod(Part, Count)];
    }

    private VoxelShape supportShape(Direction Front, int Part) {
        VoxelShape Shape = shape(Front, Part);
        for (Direction Side : OutletMounts.faces(Sockets, Width, Height, Depth, column(Part), row(Part), layer(Part))) {
            AABB Plate = OutletMounts.plate(Side);
            Shape = Shapes.or(Shape, turnedBox(Front, Plate.minX, Plate.minY, Plate.minZ, Plate.maxX, Plate.maxY, Plate.maxZ));
        }
        return Shape.optimize();
    }

    private static VoxelShape turnedBox(Direction Front, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Front) {
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    private VoxelShape cellShape(Direction Front, int Column, int Row, int Layer) {
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
        return Shape.isEmpty() ? Shapes.empty() : Shape.optimize();
    }
}
