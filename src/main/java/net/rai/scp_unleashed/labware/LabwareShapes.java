package net.rai.scp_unleashed.labware;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class LabwareShapes {
    public static final VoxelShape PetriDish = Shapes.or(Block.box(2.0, 0.0, 4.0, 14.0, 4.0, 12.0), Block.box(4.0, 0.0, 2.0, 12.0, 4.0, 14.0));
    public static final VoxelShape TubeHolder = Shapes.or(Block.box(0.0, 0.0, 5.0, 16.0, 1.0, 11.0), Block.box(0.0, 1.0, 5.5, 16.0, 10.0, 10.5));
    public static final VoxelShape ViralStorage = Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0), Block.box(3.0, 3.0, 3.0, 13.0, 14.0, 13.0),
            Block.box(4.0, 14.0, 4.0, 12.0, 16.0, 12.0), Block.box(4.0, 4.0, 13.0, 12.0, 12.0, 16.0));
    public static final VoxelShape ViralStorageSupport = Shapes.or(ViralStorage, Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0));

    private LabwareShapes() {
    }

    public static Map<Direction, VoxelShape> horizontal(VoxelShape Base) {
        Map<Direction, VoxelShape> Found = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Found.put(Side, rotate(Base, Side));
        }
        return Found;
    }

    public static VoxelShape rotate(VoxelShape Shape, Direction Side) {
        if (Side == Direction.NORTH) {
            return Shape;
        }
        VoxelShape[] Turned = {Shapes.empty()};
        Shape.forAllBoxes((X0, Y0, Z0, X1, Y1, Z1) -> {
            double[] Low = turn(X0, Z0, Side);
            double[] High = turn(X1, Z1, Side);
            Turned[0] = Shapes.joinUnoptimized(Turned[0],
                    Shapes.box(Math.min(Low[0], High[0]), Y0, Math.min(Low[1], High[1]), Math.max(Low[0], High[0]), Y1, Math.max(Low[1], High[1])), BooleanOp.OR);
        });
        return Turned[0].optimize();
    }

    private static double[] turn(double X, double Z, Direction Side) {
        return switch (Side) {
            case SOUTH -> new double[]{1.0 - X, 1.0 - Z};
            case WEST -> new double[]{Z, 1.0 - X};
            case EAST -> new double[]{1.0 - Z, X};
            default -> new double[]{X, Z};
        };
    }
}
