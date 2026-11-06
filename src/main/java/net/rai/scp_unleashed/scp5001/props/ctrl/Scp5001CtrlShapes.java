package net.rai.scp_unleashed.scp5001.props.ctrl;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class Scp5001CtrlShapes {
    private Scp5001CtrlShapes() {
    }

    public static Map<Direction, VoxelShape> build(int[][] Boxes) {
        Map<Direction, VoxelShape> Shaped = new EnumMap<>(Direction.class);
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (int[] Box : Boxes) {
                double[] Turned = turn(Box, Facing);
                Shape = Shapes.or(Shape, Block.box(Turned[0], Turned[1], Turned[2], Turned[3], Turned[4], Turned[5]));
            }
            Shaped.put(Facing, Shape.isEmpty() ? Block.box(4, 0, 4, 12, 1, 12) : Shape.optimize());
        }
        return Shaped;
    }

    public static double[] turn(int[] Box, Direction Facing) {
        double X0 = Box[0];
        double Y0 = Box[1];
        double Z0 = Box[2];
        double X1 = Box[3];
        double Y1 = Box[4];
        double Z1 = Box[5];
        return switch (Facing) {
            case EAST -> new double[]{16 - Z1, Y0, X0, 16 - Z0, Y1, X1};
            case SOUTH -> new double[]{16 - X1, Y0, 16 - Z1, 16 - X0, Y1, 16 - Z0};
            case WEST -> new double[]{Z0, Y0, 16 - X1, Z1, Y1, 16 - X0};
            default -> new double[]{X0, Y0, Z0, X1, Y1, Z1};
        };
    }
}
