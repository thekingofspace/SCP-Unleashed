package net.rai.scp_unleashed.scp3008.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class Scp3008Shapes {
    private Scp3008Shapes() {
    }

    public static VoxelShape boxes(double[][] Boxes) {
        VoxelShape Found = Shapes.empty();
        for (double[] Box : Boxes) {
            Found = Shapes.or(Found, Block.box(Box[0], Box[1], Box[2], Box[3], Box[4], Box[5]));
        }
        return Found.optimize();
    }

    public static double[] turn(double[] Box, Direction Facing) {
        double[][] Corners = {{Box[0], Box[2]}, {Box[3], Box[5]}};
        double MinX = 16;
        double MinZ = 16;
        double MaxX = 0;
        double MaxZ = 0;
        for (double[] Corner : Corners) {
            double X = Corner[0];
            double Z = Corner[1];
            double Tx;
            double Tz;
            switch (Facing) {
                case EAST -> {
                    Tx = 16 - Z;
                    Tz = X;
                }
                case SOUTH -> {
                    Tx = 16 - X;
                    Tz = 16 - Z;
                }
                case WEST -> {
                    Tx = Z;
                    Tz = 16 - X;
                }
                default -> {
                    Tx = X;
                    Tz = Z;
                }
            }
            MinX = Math.min(MinX, Tx);
            MaxX = Math.max(MaxX, Tx);
            MinZ = Math.min(MinZ, Tz);
            MaxZ = Math.max(MaxZ, Tz);
        }
        return new double[]{MinX, Box[1], MinZ, MaxX, Box[4], MaxZ};
    }

    public static Map<Direction, VoxelShape> facings(double[][] North) {
        Map<Direction, VoxelShape> Found = new EnumMap<>(Direction.class);
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            double[][] Turned = new double[North.length][];
            for (int Index = 0; Index < North.length; Index++) {
                Turned[Index] = turn(North[Index], Facing);
            }
            Found.put(Facing, boxes(Turned));
        }
        return Found;
    }
}
