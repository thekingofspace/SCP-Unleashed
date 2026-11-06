package net.rai.scp_unleashed.scp003;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public final class Scp003Shapes {
    public static final Map<Direction, VoxelShape> PlateBase = facings(new double[][]{{1.0, 0.0, 0.0, 15.0, 3.0, 16.0}});
    public static final Map<Direction, VoxelShape> PlateSeated = facings(new double[][]{{1.0, 0.0, 0.0, 15.0, 3.0, 16.0}, {4.0, 3.0, 1.5, 12.0, 5.8, 14.5}});
    public static final Map<Direction, VoxelShape> BoardBase = facings(new double[][]{{4.0, 0.0, 1.5, 12.0, 1.0, 14.5}});
    public static final Map<Direction, VoxelShape> BoardOutline = facings(new double[][]{{4.0, 0.0, 1.5, 12.0, 3.8, 14.5}});

    private Scp003Shapes() {
    }

    private static Map<Direction, VoxelShape> facings(double[][] Boxes) {
        Map<Direction, VoxelShape> Found = new EnumMap<>(Direction.class);
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            VoxelShape Shape = Shapes.empty();
            for (double[] Box : Boxes) {
                boolean Turned = Facing.getAxis() == Direction.Axis.X;
                Shape = Shapes.or(Shape, Turned ? Block.box(Box[2], Box[1], Box[0], Box[5], Box[4], Box[3]) : Block.box(Box[0], Box[1], Box[2], Box[3], Box[4], Box[5]));
            }
            Found.put(Facing, Shape.optimize());
        }
        return Found;
    }

    public static VoxelShape of(Map<Direction, VoxelShape> Table, Direction Facing) {
        return Table.getOrDefault(Facing, Table.get(Direction.NORTH));
    }
}
