package net.rai.scp_unleashed.daevite.block;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public enum DaeviteDecor implements StringRepresentable {
    WarBanner("war_banner", new double[][]{{2, 0, 6.5, 14, 16, 9.5}}, new double[][]{{6.5, 0, 6.5, 9.5, 16, 9.5}}),
    Brazier("brazier", new double[][]{{2, 0, 2, 14, 12, 14}}, null),
    Idol("idol", new double[][]{{3, 0, 3, 13, 3, 13}, {4, 3, 5, 12, 15, 11}}, null),
    Rug("rug", new double[][]{{0, 0, 0, 16, 1, 16}}, null),
    TentCloth("tent_cloth", new double[][]{{0, 0, 0, 16, 16, 16}}, null),
    WeaponRack("weapon_rack", new double[][]{{1, 0, 5, 15, 16, 11}}, null),
    Lantern("lantern", new double[][]{{5, 0, 5, 11, 9, 11}}, null),
    BrassUrn("brass_urn", new double[][]{{4, 0, 4, 12, 15, 12}}, null),
    WarDrum("war_drum", new double[][]{{1.5, 0, 1.5, 14.5, 12, 14.5}}, null),
    SigilStone("sigil_stone", new double[][]{{0, 0, 0, 16, 16, 16}}, null),
    Tapestry("tapestry", new double[][]{{0, 0, 14, 16, 16, 16}}, null),
    Altar("altar", new double[][]{{0, 0, 0, 16, 12, 16}, {3, 12, 3, 13, 14, 13}}, null);

    public static final Codec<DaeviteDecor> KindCodec = StringRepresentable.fromEnum(DaeviteDecor::values);

    private final String Name;
    private final double[][] Outline;
    private final double[][] Solid;
    private final Map<Direction, VoxelShape> Outlines = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> Solids = new EnumMap<>(Direction.class);

    DaeviteDecor(String Name, double[][] Outline, double[][] Solid) {
        this.Name = Name;
        this.Outline = Outline;
        this.Solid = Solid == null ? Outline : Solid;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public String id() {
        return "daevite_" + Name;
    }

    public VoxelShape outline(Direction Facing) {
        return Outlines.computeIfAbsent(Facing, Side -> build(Outline, Side));
    }

    public VoxelShape solid(Direction Facing) {
        return Solids.computeIfAbsent(Facing, Side -> build(Solid, Side));
    }

    public static VoxelShape build(double[][] Boxes, Direction Facing) {
        VoxelShape Result = Shapes.empty();
        for (double[] Box : Boxes) {
            double[] Turned = turn(Box, Facing);
            Result = Shapes.or(Result, Shapes.box(Turned[0] / 16.0, Turned[1] / 16.0, Turned[2] / 16.0, Turned[3] / 16.0, Turned[4] / 16.0, Turned[5] / 16.0));
        }
        return Result.optimize();
    }

    public static double[] turn(double[] Box, Direction Facing) {
        double X0 = Box[0];
        double Z0 = Box[2];
        double X1 = Box[3];
        double Z1 = Box[5];
        return switch (Facing) {
            case SOUTH -> new double[]{16 - X1, Box[1], 16 - Z1, 16 - X0, Box[4], 16 - Z0};
            case EAST -> new double[]{16 - Z1, Box[1], X0, 16 - Z0, Box[4], X1};
            case WEST -> new double[]{Z0, Box[1], 16 - X1, Z1, Box[4], 16 - X0};
            default -> Box.clone();
        };
    }
}
