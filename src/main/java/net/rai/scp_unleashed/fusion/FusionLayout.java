package net.rai.scp_unleashed.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class FusionLayout {
    public static final int Width = 5;
    public static final int Height = 3;
    public static final int Depth = 5;
    public static final int CoreCount = Width * Height * Depth;
    public static final int InputPart = CoreCount;
    public static final int OutputPart = CoreCount + 1;
    public static final int CellCount = CoreCount + 2;
    public static final int CentreColumn = 2;
    public static final int TerminalLayer = 2;
    public static final int OriginPart = index(CentreColumn, 0, 0);
    public static final int MasterPart = index(CentreColumn, 1, 2);
    public static final int InletPart = index(CentreColumn, 0, Depth - 1);
    public static final int ConsolePart = OriginPart;
    public static final int CrownPart = index(CentreColumn, Height - 1, 2);
    public static final int[] ViewportParts = {index(CentreColumn, 1, 0), index(CentreColumn, 1, Depth - 1), index(0, 1, 2), index(Width - 1, 1, 2)};
    public static final int[] SeamParts = {5, 6, 8, 9, 20, 24, 50, 54, 65, 66, 68, 69};
    public static final double PixelsPerBlock = 16.0;

    private FusionLayout() {
    }

    public static int index(int X, int Y, int Z) {
        return X + Y * Width + Z * Width * Height;
    }

    public static int column(int Part) {
        if (Part == InputPart) {
            return Width;
        }
        if (Part == OutputPart) {
            return -1;
        }
        return Part % Width;
    }

    public static int row(int Part) {
        return Part >= CoreCount ? 0 : Part / Width % Height;
    }

    public static int layer(int Part) {
        return Part >= CoreCount ? TerminalLayer : Part / (Width * Height);
    }

    public static boolean isPort(int Part) {
        return Part == InputPart || Part == OutputPart;
    }

    public static boolean isViewport(int Part) {
        for (int Viewport : ViewportParts) {
            if (Viewport == Part) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSeam(int Part) {
        for (int Seam : SeamParts) {
            if (Seam == Part) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGlowCell(int Part) {
        return isPort(Part) || isViewport(Part) || isSeam(Part) || Part == ConsolePart || Part == CrownPart;
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

    public static BlockPos offsetOf(int Part, Direction Front) {
        return turn(column(Part) - CentreColumn, row(Part), layer(Part), Front);
    }

    public static BlockPos cellPos(BlockPos Origin, Direction Front, int Part) {
        return Origin.offset(offsetOf(Part, Front));
    }

    public static BlockPos originOf(BlockPos CellPos, BlockState State) {
        return CellPos.subtract(offsetOf(State.getValue(FusionReactorBlock.Part), State.getValue(FusionReactorBlock.Facing)));
    }

    public static BlockPos masterOf(BlockPos CellPos, BlockState State) {
        return cellPos(originOf(CellPos, State), State.getValue(FusionReactorBlock.Facing), MasterPart);
    }

    public static Direction portFace(int Part, Direction Front) {
        if (Part == InputPart) {
            return toWorld(Direction.EAST, Front);
        }
        if (Part == OutputPart) {
            return toWorld(Direction.WEST, Front);
        }
        return toWorld(Direction.SOUTH, Front);
    }

    public static Vec3 toWorld(BlockPos Origin, Direction Front, double PixelX, double PixelY, double PixelZ) {
        double U = PixelX / PixelsPerBlock - CentreColumn;
        double V = PixelZ / PixelsPerBlock;
        double Y = PixelY / PixelsPerBlock;
        return switch (Front) {
            case EAST -> new Vec3(Origin.getX() + 1.0 - V, Origin.getY() + Y, Origin.getZ() + U);
            case SOUTH -> new Vec3(Origin.getX() + 1.0 - U, Origin.getY() + Y, Origin.getZ() + 1.0 - V);
            case WEST -> new Vec3(Origin.getX() + V, Origin.getY() + Y, Origin.getZ() + 1.0 - U);
            default -> new Vec3(Origin.getX() + U, Origin.getY() + Y, Origin.getZ() + V);
        };
    }
}
