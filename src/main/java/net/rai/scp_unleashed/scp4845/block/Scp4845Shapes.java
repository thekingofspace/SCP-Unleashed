package net.rai.scp_unleashed.scp4845.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class Scp4845Shapes {
    public static final double[][] Full = {{0, 0, 0, 16, 16, 16}};
    public static final double[][] Counter = {{0, 0, 0, 16, 16, 16}};
    public static final double[][] Register = {{0, 0, 0, 16, 16, 16}, {4.5, 16, 7.5, 11.5, 25, 12.5}, {10, 16, 2.5, 14, 22.5, 4.25}, {1, 16, 9, 4, 21, 13}};
    public static final double[][] SodaFountain = {{0, 0, 0, 16, 14, 16}, {0, 14, 5, 16, 30, 16}, {0.5, 14, 2.5, 15.5, 15.25, 5}};
    public static final double[][] Condiments = {{0, 0, 0.5, 16, 14, 16}, {1, 14, 1.5, 15, 16, 8.5}, {0.5, 14, 9.5, 15.5, 16, 16}, {6, 16, 10.5, 15, 22.5, 14.5}};
    public static final double[][] Booth = {{0, 0, 0.25, 16, 8, 13.5}, {0, 0, 11.5, 16, 22, 16}};
    public static final double[][] Table = {{0.5, 12.5, 0.5, 15.5, 14, 15.5}, {7, 0, 7, 9, 12.5, 9}, {3, 0, 3, 13, 1.75, 13}};
    public static final double[][] Chair = {{2, 0, 1.5, 14, 8, 13}, {2.5, 0, 12.5, 13.5, 18.5, 14.5}};
    public static final double[][] Grill = {{0, 0, 0, 16, 14, 16}, {0, 14, 14.5, 16, 21.5, 16}, {0, 14, 8, 16, 17, 14.5}};
    public static final double[][] Fryer = {{0, 0, 0, 16, 14, 16}, {0, 14, 14.5, 16, 24, 16}, {1, 14, 11, 15, 20.5, 14.5}};
    public static final double[][] PrepTable = {{0, 0, 0, 16, 14, 16}, {0, 14, 6, 16, 15.5, 16}, {0.5, 15.5, 6.5, 15.5, 24.25, 16}};
    public static final double[][] Sink = {{0.5, 0, 1, 15.5, 14, 15.5}, {0.5, 14, 13, 15.5, 19.5, 15.5}, {7.5, 14, 9, 8.5, 22, 13.5}};
    public static final double[][] MopBucket = {{2.75, 0, 2.25, 13.25, 9.5, 14.25}, {6.5, 9.25, 5, 9.5, 28, 7}};
    public static final double[][] Shelf = {{0.25, 0, 1.25, 15.75, 16, 14.75}};
    public static final double[][] Desk = {{0, 12, 0, 16, 13, 16}, {0, 0, 0.25, 2, 12, 15.75}, {14, 0, 0.25, 16, 12, 15.75}, {2, 3, 13.5, 14, 12, 14.5}, {3, 13, 9.5, 13, 22, 15.75}};
    public static final double[][] OfficeChair = {{0.75, 0, 0.75, 15.25, 2.25, 15.25}, {2.5, 2.25, 1.25, 13.5, 8, 12.75}, {2.75, 6.25, 12.25, 13.25, 22, 14.5}};
    public static final double[][] TrashCan = {{0.5, 0, 1, 15.5, 16.5, 15.5}};
    public static final double[][] DriveWindow = {{0, 0, 0, 1.5, 16, 16}, {14.5, 0, 0, 16, 16, 16}, {1.5, 12, 0, 14.5, 16, 16}, {1.5, 0, 0, 14.5, 3, 16}, {1.5, 3, 1.5, 14.5, 12, 3.25}};
    public static final double[][] Speaker = {{2.5, 0, 4.5, 13.5, 1, 11.5}, {5, 1, 6, 11, 11, 10}, {0.5, 11, 3.5, 15.5, 28, 10.5}};
    public static final double[][] Panel = {{0, 0, 14, 16, 16, 16}};

    private Scp4845Shapes() {
    }

    public static Map<Direction, VoxelShape> facings(double[][] North) {
        Map<Direction, VoxelShape> Made = new EnumMap<>(Direction.class);
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (double[] Box : North) {
                double[] Turned = turn(Box, Facing);
                double X1 = clip(Turned[0]);
                double Y1 = clip(Turned[1]);
                double Z1 = clip(Turned[2]);
                double X2 = clip(Turned[3]);
                double Y2 = Math.min(24.0, Math.max(0.0, Turned[4]));
                double Z2 = clip(Turned[5]);
                if (X2 - X1 < 0.01 || Y2 - Y1 < 0.01 || Z2 - Z1 < 0.01) {
                    continue;
                }
                Shape = Shapes.or(Shape, Shapes.box(X1 / 16.0, Y1 / 16.0, Z1 / 16.0, X2 / 16.0, Y2 / 16.0, Z2 / 16.0));
            }
            Made.put(Facing, Shape.optimize());
        }
        return Made;
    }

    public static Map<Direction, VoxelShape> collisions(Map<Direction, VoxelShape> Outline) {
        Map<Direction, VoxelShape> Made = new EnumMap<>(Direction.class);
        VoxelShape Cell = Shapes.block();
        for (Map.Entry<Direction, VoxelShape> Entry : Outline.entrySet()) {
            Made.put(Entry.getKey(), Shapes.join(Entry.getValue(), Cell, BooleanOp.AND).optimize());
        }
        return Made;
    }

    private static double clip(double Value) {
        return Math.min(16.0, Math.max(0.0, Value));
    }

    public static double[] turn(double[] Box, Direction Facing) {
        double X1 = Box[0];
        double Y1 = Box[1];
        double Z1 = Box[2];
        double X2 = Box[3];
        double Y2 = Box[4];
        double Z2 = Box[5];
        return switch (Facing) {
            case EAST -> new double[]{16 - Z2, Y1, X1, 16 - Z1, Y2, X2};
            case SOUTH -> new double[]{16 - X2, Y1, 16 - Z2, 16 - X1, Y2, 16 - Z1};
            case WEST -> new double[]{Z1, Y1, 16 - X2, Z2, Y2, 16 - X1};
            default -> new double[]{X1, Y1, Z1, X2, Y2, Z2};
        };
    }
}
