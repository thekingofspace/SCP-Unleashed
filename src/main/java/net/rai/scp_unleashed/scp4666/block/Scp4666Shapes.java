package net.rai.scp_unleashed.scp4666.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class Scp4666Shapes {
    private Scp4666Shapes() {
    }

    public static VoxelShape turned(Direction Facing, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Facing) {
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    public static Map<Direction, VoxelShape> table(double[][] Boxes) {
        Map<Direction, VoxelShape> Made = new EnumMap<>(Direction.class);
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (double[] Box : Boxes) {
                Shape = Shapes.or(Shape, turned(Facing, Box[0], Box[1], Box[2], Box[3], Box[4], Box[5]));
            }
            Made.put(Facing, Shape.optimize());
        }
        return Made;
    }
}
