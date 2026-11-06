package net.rai.scp_unleashed.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.wire.OutletMounts;

import java.util.ArrayList;
import java.util.List;

public record MachineLayout(int Width, int Height, int Depth, int Master) {
    public static final double Pixels = 16.0;
    public static final Direction[] Facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public static MachineLayout of(int Width, int Height, int Depth, int MasterX, int MasterY, int MasterZ) {
        return new MachineLayout(Width, Height, Depth, MasterX + MasterY * Width + MasterZ * Width * Height);
    }

    public int count() {
        return Width * Height * Depth;
    }

    public int centre() {
        return Width / 2;
    }

    public int index(int X, int Y, int Z) {
        return X + Y * Width + Z * Width * Height;
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

    public int origin() {
        return index(centre(), 0, 0);
    }

    public static BlockPos turn(int X, int Y, int Z, Direction Front) {
        return switch (Front) {
            case EAST -> new BlockPos(-Z, Y, X);
            case SOUTH -> new BlockPos(-X, Y, -Z);
            case WEST -> new BlockPos(Z, Y, -X);
            default -> new BlockPos(X, Y, Z);
        };
    }

    public static Direction toWorld(Direction Local, Direction Front) {
        return switch (Local) {
            case NORTH -> Front;
            case SOUTH -> Front.getOpposite();
            case EAST -> Front.getClockWise();
            case WEST -> Front.getCounterClockWise();
            default -> Local;
        };
    }

    public BlockPos offsetOf(int Part, Direction Front) {
        return turn(column(Part) - centre(), row(Part), layer(Part), Front);
    }

    public BlockPos cellPos(BlockPos Origin, Direction Front, int Part) {
        return Origin.offset(offsetOf(Part, Front));
    }

    public BlockPos originOf(BlockPos CellPos, int Part, Direction Front) {
        return CellPos.subtract(offsetOf(Part, Front));
    }

    public BlockPos masterOf(BlockPos CellPos, int Part, Direction Front) {
        return cellPos(originOf(CellPos, Part, Front), Front, Master);
    }

    public List<BlockPos> cells(BlockPos Origin, Direction Front) {
        List<BlockPos> Found = new ArrayList<>(count());
        Found.add(Origin);
        for (int Part = 0; Part < count(); Part++) {
            if (Part != origin()) {
                Found.add(cellPos(Origin, Front, Part));
            }
        }
        return Found;
    }

    public Vec3 point(BlockPos CellPos, int Part, Direction Front, double PixelX, double PixelY, double PixelZ) {
        BlockPos Origin = originOf(CellPos, Part, Front);
        double U = PixelX / Pixels - centre();
        double V = PixelZ / Pixels;
        double Y = PixelY / Pixels;
        return switch (Front) {
            case EAST -> new Vec3(Origin.getX() + 1.0 - V, Origin.getY() + Y, Origin.getZ() + U);
            case SOUTH -> new Vec3(Origin.getX() + 1.0 - U, Origin.getY() + Y, Origin.getZ() + 1.0 - V);
            case WEST -> new Vec3(Origin.getX() + V, Origin.getY() + Y, Origin.getZ() + 1.0 - U);
            default -> new Vec3(Origin.getX() + U, Origin.getY() + Y, Origin.getZ() + V);
        };
    }

    public VoxelShape[][] shapes(List<AABB> Boxes) {
        VoxelShape[][] Built = new VoxelShape[Facings.length][count()];
        for (int Part = 0; Part < count(); Part++) {
            double X0 = column(Part) * Pixels;
            double Y0 = row(Part) * Pixels;
            double Z0 = layer(Part) * Pixels;
            List<AABB> Local = new ArrayList<>();
            for (AABB Box : Boxes) {
                double MinX = Math.max(Box.minX, X0) - X0;
                double MinY = Math.max(Box.minY, Y0) - Y0;
                double MinZ = Math.max(Box.minZ, Z0) - Z0;
                double MaxX = Math.min(Box.maxX, X0 + Pixels) - X0;
                double MaxY = Math.min(Box.maxY, Y0 + Pixels) - Y0;
                double MaxZ = Math.min(Box.maxZ, Z0 + Pixels) - Z0;
                if (MaxX > MinX && MaxY > MinY && MaxZ > MinZ) {
                    Local.add(new AABB(MinX / Pixels, MinY / Pixels, MinZ / Pixels, MaxX / Pixels, MaxY / Pixels, MaxZ / Pixels));
                }
            }
            for (int Turn = 0; Turn < Facings.length; Turn++) {
                VoxelShape Shape = Shapes.empty();
                for (AABB Box : Local) {
                    Shape = Shapes.or(Shape, Shapes.create(turned(Box, Turn)));
                }
                Built[Turn][Part] = Shape.optimize();
            }
        }
        return Built;
    }

    public VoxelShape[][] supports(List<AABB> Boxes) {
        VoxelShape[][] Built = new VoxelShape[Facings.length][count()];
        for (int Part = 0; Part < count(); Part++) {
            List<Direction> Mounts = OutletMounts.faces(Boxes, Width, Height, Depth, column(Part), row(Part), layer(Part));
            for (int Turn = 0; Turn < Facings.length; Turn++) {
                VoxelShape Shape = Shapes.empty();
                for (Direction Side : Mounts) {
                    Shape = Shapes.or(Shape, Shapes.create(turned(OutletMounts.plate(Side), Turn, Pixels)));
                }
                Built[Turn][Part] = Shape.optimize();
            }
        }
        return Built;
    }

    private static AABB turned(AABB Box, int Turn) {
        AABB Turned = Box;
        for (int Step = 0; Step < Turn; Step++) {
            Turned = new AABB(1.0 - Turned.maxZ, Turned.minY, Turned.minX, 1.0 - Turned.minZ, Turned.maxY, Turned.maxX);
        }
        return Turned;
    }

    private static AABB turned(AABB PixelBox, int Turn, double Scale) {
        return turned(new AABB(PixelBox.minX / Scale, PixelBox.minY / Scale, PixelBox.minZ / Scale, PixelBox.maxX / Scale, PixelBox.maxY / Scale, PixelBox.maxZ / Scale), Turn);
    }

    public static int turnOf(Direction Front) {
        return switch (Front) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    public record Port(int Part, Direction Local) {
    }

    public static AABB pixels(double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return new AABB(X0, Y0, Z0, X1, Y1, Z1);
    }
}
