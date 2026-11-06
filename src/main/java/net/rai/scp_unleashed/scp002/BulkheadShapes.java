package net.rai.scp_unleashed.scp002;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class BulkheadShapes {
    public static final double[][] LowerFrame = {{-4.0, 0.0, -2.0, 1.0, 16.0, 1.0}, {15.0, 0.0, -2.0, 20.0, 16.0, 1.0}, {1.0, 0.0, -2.0, 15.0, 2.0, 5.0}, {-1.5, 2.5, -4.0, 0.5, 4.5, -2.0}, {-1.5, 6.5, -4.0, 0.5, 8.5, -2.0}};
    public static final double[][] UpperFrame = {{-4.0, 0.0, -2.0, 1.0, 15.0, 1.0}, {15.0, 0.0, -2.0, 20.0, 15.0, 1.0}, {-2.0, 15.0, -2.0, 8.0, 20.0, 1.0}, {8.0, 15.0, -2.0, 18.0, 20.0, 1.0}, {-4.0, 15.0, -2.0, -2.0, 17.0, 1.0}, {18.0, 15.0, -2.0, 20.0, 17.0, 1.0}, {1.0, 13.0, -2.0, 3.0, 15.0, 0.5}, {1.0, 11.0, -2.0, 2.0, 13.0, 0.5}, {13.0, 13.0, -2.0, 15.0, 15.0, 0.5}, {14.0, 11.0, -2.0, 15.0, 13.0, 0.5}, {-1.5, 6.5, -4.0, 0.5, 8.5, -2.0}, {-1.5, 10.5, -4.0, 0.5, 12.5, -2.0}};
    public static final double[][] LowerLeaf = {{1.0, 2.0, 0.5, 15.0, 16.0, 5.0}, {13.5, 6.0, -1.0, 16.5, 8.0, 0.5}, {-1.1, 4.5, -3.6, 0.1, 6.5, -2.4}, {-0.5, 4.5, -8.0, 1.0, 6.5, -3.6}, {1.0, 4.5, -8.0, 2.5, 6.5, -7.5}};
    public static final double[][] UpperLeaf = {{1.0, 0.0, 0.5, 15.0, 11.0, 5.0}, {2.0, 11.0, 0.5, 14.0, 13.0, 5.0}, {3.0, 13.0, 0.5, 13.0, 15.0, 5.0}, {13.5, 9.0, -1.0, 16.5, 11.0, 0.5}, {6.0, 8.0, -1.5, 10.0, 9.0, 0.5}, {6.0, 0.0, -1.5, 10.0, 1.0, 0.5}, {4.0, 2.0, -1.5, 5.0, 7.0, 0.5}, {11.0, 2.0, -1.5, 12.0, 7.0, 0.5}, {5.0, 7.0, -1.5, 6.0, 8.0, 0.5}, {10.0, 7.0, -1.5, 11.0, 8.0, 0.5}, {5.0, 1.0, -1.5, 6.0, 2.0, 0.5}, {10.0, 1.0, -1.5, 11.0, 2.0, 0.5}, {5.0, 4.0, -1.0, 11.0, 5.0, 0.5}, {7.5, 1.0, -1.25, 8.5, 8.0, 0.5}, {7.0, 3.5, -2.5, 9.0, 5.5, 0.5}, {-1.1, 8.5, -3.6, 0.1, 10.5, -2.4}, {-0.5, 8.5, -8.0, 1.0, 10.5, -3.6}, {1.0, 8.5, -8.0, 2.5, 10.5, -7.5}};
    private static final Map<Direction, VoxelShape> LowerOpen = build(LowerFrame, new double[0][]);
    private static final Map<Direction, VoxelShape> UpperOpen = build(UpperFrame, new double[0][]);
    private static final Map<Direction, VoxelShape> LowerShut = build(LowerFrame, LowerLeaf);
    private static final Map<Direction, VoxelShape> UpperShut = build(UpperFrame, UpperLeaf);

    private BulkheadShapes() {
    }

    private static Map<Direction, VoxelShape> build(double[][] Frame, double[][] Leaf) {
        Map<Direction, VoxelShape> Built = new EnumMap<>(Direction.class);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            VoxelShape Combined = Shapes.empty();
            for (double[][] Group : new double[][][]{Frame, Leaf}) {
                for (double[] Box : Group) {
                    Combined = Shapes.or(Combined, FleshFixtureBlock.rotated(Side, Box[0], Box[1], Box[2], Box[3], Box[4], Box[5]));
                }
            }
            Built.put(Side, Combined.optimize());
        }
        return Built;
    }

    public static VoxelShape outline(Direction Facing, DoubleBlockHalf Half, boolean Opened) {
        Map<Direction, VoxelShape> Chosen = Half == DoubleBlockHalf.LOWER ? Opened ? LowerOpen : LowerShut : Opened ? UpperOpen : UpperShut;
        return Chosen.get(Facing);
    }
}
