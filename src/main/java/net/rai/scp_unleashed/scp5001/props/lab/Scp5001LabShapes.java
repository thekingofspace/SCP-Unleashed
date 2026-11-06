package net.rai.scp_unleashed.scp5001.props.lab;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class Scp5001LabShapes {
    private Scp5001LabShapes() {
    }

    public static Map<Direction, VoxelShape> horizontal(int[][] Boxes) {
        Map<Direction, VoxelShape> Made = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Made.put(Side, rotated(Boxes, Side));
        }
        return Made;
    }

    public static VoxelShape rotated(int[][] Boxes, Direction Side) {
        if (Boxes.length == 0) {
            return Block.box(4, 0, 4, 12, 2, 12);
        }
        VoxelShape Result = Shapes.empty();
        for (int[] Each : Boxes) {
            int X0 = Each[0];
            int Y0 = Each[1];
            int Z0 = Each[2];
            int X1 = Each[3];
            int Y1 = Each[4];
            int Z1 = Each[5];
            VoxelShape Part = switch (Side) {
                case EAST -> Block.box(16 - Z1, Y0, X0, 16 - Z0, Y1, X1);
                case SOUTH -> Block.box(16 - X1, Y0, 16 - Z1, 16 - X0, Y1, 16 - Z0);
                case WEST -> Block.box(Z0, Y0, 16 - X1, Z1, Y1, 16 - X0);
                default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
            };
            Result = Shapes.or(Result, Part);
        }
        return Result.optimize();
    }

    public static boolean low(int[][] Boxes) {
        for (int[] Each : Boxes) {
            if (Each[4] > 3) {
                return false;
            }
        }
        return true;
    }
}
