package net.rai.scp_unleashed.scp5001.event.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class Scp5001Shapes {
    private Scp5001Shapes() {
    }

    public static Map<Direction, VoxelShape> rotations(VoxelShape North) {
        Map<Direction, VoxelShape> Made = new EnumMap<>(Direction.class);
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            Made.put(Facing, rotate(North, Facing));
        }
        return Made;
    }

    public static VoxelShape rotate(VoxelShape North, Direction Facing) {
        VoxelShape[] Result = {Shapes.empty()};
        North.forAllBoxes((X0, Y0, Z0, X1, Y1, Z1) -> {
            double[] A = turn(X0, Z0, Facing);
            double[] B = turn(X1, Z1, Facing);
            Result[0] = Shapes.or(Result[0], Shapes.box(Math.min(A[0], B[0]), Y0, Math.min(A[1], B[1]), Math.max(A[0], B[0]), Y1, Math.max(A[1], B[1])));
        });
        return Result[0].optimize();
    }

    private static double[] turn(double X, double Z, Direction Facing) {
        return switch (Facing) {
            case SOUTH -> new double[]{1.0 - X, 1.0 - Z};
            case EAST -> new double[]{1.0 - Z, X};
            case WEST -> new double[]{Z, 1.0 - X};
            default -> new double[]{X, Z};
        };
    }
}
