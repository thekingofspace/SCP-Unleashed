package net.rai.scp_unleashed.food;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class FoodShapes {
    private FoodShapes() {
    }

    public static VoxelShape of(VoxelShape... Parts) {
        VoxelShape Shape = Shapes.empty();
        for (VoxelShape Part : Parts) {
            Shape = Shapes.or(Shape, Part);
        }
        return Shape.optimize();
    }

    public static Map<Direction, VoxelShape> horizontal(VoxelShape North) {
        Map<Direction, VoxelShape> Result = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Result.put(Side, turn(North, Side));
        }
        return Result;
    }

    private static VoxelShape turn(VoxelShape North, Direction Facing) {
        int Turns = switch (Facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        if (Turns == 0) {
            return North;
        }
        VoxelShape Result = Shapes.empty();
        for (AABB Box : North.toAabbs()) {
            double MinX = Box.minX;
            double MaxX = Box.maxX;
            double MinZ = Box.minZ;
            double MaxZ = Box.maxZ;
            for (int Step = 0; Step < Turns; Step++) {
                double TurnedMinX = 1.0 - MaxZ;
                double TurnedMaxX = 1.0 - MinZ;
                MinZ = MinX;
                MaxZ = MaxX;
                MinX = TurnedMinX;
                MaxX = TurnedMaxX;
            }
            Result = Shapes.or(Result, Shapes.box(MinX, Box.minY, MinZ, MaxX, Box.maxY, MaxZ));
        }
        return Result.optimize();
    }
}
