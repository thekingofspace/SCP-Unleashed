package net.rai.scp_unleashed.utility;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class UtilityShapes {
    public static final int[] Quarters = {0, 90, 180, 270};

    private UtilityShapes() {
    }

    public static Direction turn(Direction Side, int TurnX, int TurnY) {
        Direction Result = Side;
        for (int Step = 0; Step < TurnX / 90; Step++) {
            Result = Result.getClockWise(Direction.Axis.X);
        }
        for (int Step = 0; Step < TurnY / 90; Step++) {
            Result = Result.getClockWise(Direction.Axis.Y);
        }
        return Result;
    }

    public static int horizontalTurn(Direction Front) {
        return ((Front.get2DDataValue() + 2) % 4) * 90;
    }

    public static int[] facingTurn(Direction Facing) {
        for (int TurnX : Quarters) {
            for (int TurnY : Quarters) {
                if (turn(Direction.UP, TurnX, TurnY) == Facing) {
                    return new int[]{TurnX, TurnY};
                }
            }
        }
        return new int[]{0, 0};
    }

    public static VoxelShape box(double[] Part, int TurnX, int TurnY) {
        return box(Part, 0, TurnX, TurnY);
    }

    public static VoxelShape box(double[] Part, int PreTurnY, int TurnX, int TurnY) {
        double[] Low = {Part[0] - 8, Part[1] - 8, Part[2] - 8};
        double[] High = {Part[3] - 8, Part[4] - 8, Part[5] - 8};
        for (int Step = 0; Step < PreTurnY / 90; Step++) {
            Low = turnedY(Low);
            High = turnedY(High);
        }
        for (int Step = 0; Step < TurnX / 90; Step++) {
            Low = turnedX(Low);
            High = turnedX(High);
        }
        for (int Step = 0; Step < TurnY / 90; Step++) {
            Low = turnedY(Low);
            High = turnedY(High);
        }
        return Shapes.box((Math.min(Low[0], High[0]) + 8) / 16, (Math.min(Low[1], High[1]) + 8) / 16, (Math.min(Low[2], High[2]) + 8) / 16,
                (Math.max(Low[0], High[0]) + 8) / 16, (Math.max(Low[1], High[1]) + 8) / 16, (Math.max(Low[2], High[2]) + 8) / 16);
    }

    public static VoxelShape union(int TurnX, int TurnY, double[]... Parts) {
        return union(0, TurnX, TurnY, Parts);
    }

    public static VoxelShape union(int PreTurnY, int TurnX, int TurnY, double[]... Parts) {
        VoxelShape Shape = Shapes.empty();
        for (double[] Part : Parts) {
            Shape = Shapes.or(Shape, box(Part, PreTurnY, TurnX, TurnY));
        }
        return Shape.optimize();
    }

    public static Map<Direction, VoxelShape> horizontal(double[]... Parts) {
        Map<Direction, VoxelShape> Result = new EnumMap<>(Direction.class);
        for (Direction Front : Direction.Plane.HORIZONTAL) {
            Result.put(Front, union(0, horizontalTurn(Front), Parts));
        }
        return Result;
    }

    public static Map<Direction, VoxelShape> facing(double[]... Parts) {
        Map<Direction, VoxelShape> Result = new EnumMap<>(Direction.class);
        for (Direction Facing : Direction.values()) {
            int[] Turn = facingTurn(Facing);
            Result.put(Facing, union(Turn[0], Turn[1], Parts));
        }
        return Result;
    }

    private static double[] turnedX(double[] Point) {
        return new double[]{Point[0], Point[2], -Point[1]};
    }

    private static double[] turnedY(double[] Point) {
        return new double[]{-Point[2], Point[1], Point[0]};
    }
}
