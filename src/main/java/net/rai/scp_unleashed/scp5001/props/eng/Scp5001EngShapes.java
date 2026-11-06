package net.rai.scp_unleashed.scp5001.props.eng;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class Scp5001EngShapes {
    private Scp5001EngShapes() {
    }

    public static Map<Direction, VoxelShape> horizontal(double[][] Boxes) {
        Map<Direction, VoxelShape> Made = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Made.put(Side, rotated(Boxes, Side));
        }
        return Made;
    }

    public static VoxelShape rotated(double[][] Boxes, Direction Side) {
        if (Boxes.length == 0) {
            return Block.box(4, 0, 4, 12, 2, 12);
        }
        VoxelShape Shape = Shapes.empty();
        for (double[] Box : Boxes) {
            double[] Low = turn(Box[0], Box[2], Side);
            double[] High = turn(Box[3], Box[5], Side);
            Shape = Shapes.or(Shape, Block.box(Math.min(Low[0], High[0]), Box[1], Math.min(Low[1], High[1]), Math.max(Low[0], High[0]), Box[4], Math.max(Low[1], High[1])));
        }
        return Shape.optimize();
    }

    private static double[] turn(double X, double Z, Direction Side) {
        return switch (Side) {
            case EAST -> new double[]{16 - Z, X};
            case SOUTH -> new double[]{16 - X, 16 - Z};
            case WEST -> new double[]{Z, 16 - X};
            default -> new double[]{X, Z};
        };
    }
}
