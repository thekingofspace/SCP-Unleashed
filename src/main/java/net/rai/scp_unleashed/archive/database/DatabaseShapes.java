package net.rai.scp_unleashed.archive.database;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class DatabaseShapes {
    private DatabaseShapes() {
    }

    public static Map<Direction, VoxelShape> turnedShapes(double[][] Boxes) {
        Map<Direction, VoxelShape> Turned = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (double[] Box : Boxes) {
                Shape = Shapes.or(Shape, turned(Side, Box));
            }
            Turned.put(Side, Shape.optimize());
        }
        return Turned;
    }

    private static VoxelShape turned(Direction Side, double[] Box) {
        double[] First = turnPoint(Side, Box[0], Box[2]);
        double[] Second = turnPoint(Side, Box[3], Box[5]);
        return Block.box(Math.min(First[0], Second[0]), Box[1], Math.min(First[1], Second[1]), Math.max(First[0], Second[0]), Box[4], Math.max(First[1], Second[1]));
    }

    private static double[] turnPoint(Direction Side, double X, double Z) {
        return switch (Side) {
            case EAST -> new double[]{16 - Z, X};
            case SOUTH -> new double[]{16 - X, 16 - Z};
            case WEST -> new double[]{Z, 16 - X};
            default -> new double[]{X, Z};
        };
    }
}
