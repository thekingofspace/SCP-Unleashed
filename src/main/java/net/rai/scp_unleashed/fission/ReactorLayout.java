package net.rai.scp_unleashed.fission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ReactorLayout {
    public static final int Width = 8;
    public static final int Height = 5;
    public static final int Last = Width - 1;
    public static final int Top = Height - 1;
    public static final int CellCount = Width * Height * Width;
    public static final int ClickAcross = 3;
    public static final int PortRow = 1;
    public static final int PowerPortDepth = 5;
    public static final int WastePortDepth = 2;
    public static final int GasPortDepth = 6;
    public static final int GasPortLeft = 1;
    public static final int GasPortRight = Last - 1;
    public static final int WindowLeft = 2;
    public static final int WindowRight = 5;
    public static final int WindowLow = 2;
    public static final int WindowHigh = 3;
    public static final int PanelRow = 1;

    private ReactorLayout() {
    }

    public static int across(Direction Facing, int X, int Z) {
        return switch (Facing) {
            case SOUTH -> X;
            case EAST -> Last - Z;
            case WEST -> Z;
            default -> Last - X;
        };
    }

    public static int depth(Direction Facing, int X, int Z) {
        return switch (Facing) {
            case SOUTH -> Last - Z;
            case EAST -> Last - X;
            case WEST -> X;
            default -> Z;
        };
    }

    public static int worldX(Direction Facing, int Across, int Depth) {
        return switch (Facing) {
            case SOUTH -> Across;
            case EAST -> Last - Depth;
            case WEST -> Depth;
            default -> Last - Across;
        };
    }

    public static int worldZ(Direction Facing, int Across, int Depth) {
        return switch (Facing) {
            case SOUTH -> Last - Depth;
            case EAST -> Last - Across;
            case WEST -> Across;
            default -> Depth;
        };
    }

    public static BlockPos originFor(BlockPos Clicked, Direction Facing) {
        return Clicked.offset(-worldX(Facing, ClickAcross, 0), 0, -worldZ(Facing, ClickAcross, 0));
    }

    public static BlockPos cellAt(BlockPos Origin, Direction Facing, int Across, int Row, int Depth) {
        return Origin.offset(worldX(Facing, Across, Depth), Row, worldZ(Facing, Across, Depth));
    }

    public static List<BlockPos> cells(BlockPos Origin) {
        List<BlockPos> Cells = new ArrayList<>(CellCount);
        for (int Row = 0; Row < Height; Row++) {
            for (int Z = 0; Z < Width; Z++) {
                for (int X = 0; X < Width; X++) {
                    Cells.add(Origin.offset(X, Row, Z));
                }
            }
        }
        return Cells;
    }

    public static boolean isEnclosed(int X, int Row, int Z) {
        return Row >= 1 && Row < Top && X >= 1 && X < Last && Z >= 1 && Z < Last;
    }

    public static boolean isWindow(Direction Facing, int X, int Row, int Z) {
        int Across = across(Facing, X, Z);
        return depth(Facing, X, Z) == 0 && Across >= WindowLeft && Across <= WindowRight && Row >= WindowLow && Row <= WindowHigh;
    }

    public static boolean isPanel(Direction Facing, int X, int Row, int Z) {
        int Across = across(Facing, X, Z);
        return depth(Facing, X, Z) == 0 && Across >= WindowLeft && Across <= WindowRight && Row == PanelRow;
    }

    public static boolean isSidePort(Direction Facing, int X, int Row, int Z, int PortDepth) {
        int Across = across(Facing, X, Z);
        return Row == PortRow && depth(Facing, X, Z) == PortDepth && (Across == 0 || Across == Last);
    }

    public static boolean isPowerPort(Direction Facing, int X, int Row, int Z) {
        return isSidePort(Facing, X, Row, Z, PowerPortDepth);
    }

    public static boolean isWastePort(Direction Facing, int X, int Row, int Z) {
        return isSidePort(Facing, X, Row, Z, WastePortDepth);
    }

    public static boolean isGasPort(Direction Facing, int X, int Row, int Z) {
        int Across = across(Facing, X, Z);
        return Row == Top && depth(Facing, X, Z) == GasPortDepth && (Across == GasPortLeft || Across == GasPortRight);
    }

    public static Direction sideOutward(Direction Facing, int Across) {
        return Across == 0 ? Facing.getClockWise() : Facing.getCounterClockWise();
    }

    @Nullable
    public static Direction portOutward(Direction Facing, int X, int Row, int Z) {
        if (isGasPort(Facing, X, Row, Z)) {
            return Direction.UP;
        }
        if (isPowerPort(Facing, X, Row, Z) || isWastePort(Facing, X, Row, Z)) {
            return sideOutward(Facing, across(Facing, X, Z));
        }
        return null;
    }

    public static List<BlockPos> gasPorts(BlockPos Origin, Direction Facing) {
        return List.of(cellAt(Origin, Facing, GasPortLeft, Top, GasPortDepth), cellAt(Origin, Facing, GasPortRight, Top, GasPortDepth));
    }

    public static Vec3 centre(BlockPos Origin) {
        return new Vec3(Origin.getX() + Width / 2.0, Origin.getY() + Height / 2.0, Origin.getZ() + Width / 2.0);
    }

    public static AABB bounds(BlockPos Origin) {
        return new AABB(Origin.getX(), Origin.getY(), Origin.getZ(), Origin.getX() + Width, Origin.getY() + Height, Origin.getZ() + Width);
    }

    public static Vec3 frontCorner(Direction Facing) {
        Direction Left = Facing.getClockWise();
        double Half = Width / 2.0;
        return new Vec3(Half + Facing.getStepX() * Half + Left.getStepX() * Half, 0.0, Half + Facing.getStepZ() * Half + Left.getStepZ() * Half);
    }

    public static Vec3 toWorld(BlockPos Origin, Direction Facing, double Right, double Up, double Out) {
        Vec3 Corner = frontCorner(Facing);
        Direction RightSide = Facing.getCounterClockWise();
        return new Vec3(
                Origin.getX() + Corner.x + RightSide.getStepX() * Right + Facing.getStepX() * Out,
                Origin.getY() + Up,
                Origin.getZ() + Corner.z + RightSide.getStepZ() * Right + Facing.getStepZ() * Out);
    }

    public static float yaw(Direction Facing) {
        return -Facing.toYRot();
    }

    public static boolean matches(BlockState State, Direction Facing, int X, int Row, int Z) {
        return State.getBlock() instanceof FissionReactorBlock
                && State.getValue(FissionReactorBlock.Facing) == Facing
                && State.getValue(FissionReactorBlock.CellX) == X
                && State.getValue(FissionReactorBlock.CellY) == Row
                && State.getValue(FissionReactorBlock.CellZ) == Z;
    }
}
