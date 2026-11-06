package net.rai.scp_unleashed.scp5001.world.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class Scp5001WallFaces {
    public static final int Grid = 32;
    private static final double Thickness = 1.0 / Grid;
    private static final Map<String, String> Specs = new HashMap<>();
    private static final Map<BlockState, VoxelShape> Built = new IdentityHashMap<>();
    private static final String[] Table = {
            "scp5001_arch_ceiling||down:0,0,2,32;2,0,30,2;2,30,30,32;30,0,32,32/up:0,0,32,32/north:0,0,32,32/south:0,0,32,32/west:0,0,32,32/east:0,0,32,32",
            "scp5001_arch_ceiling_light||down:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/up:0,0,32,32/north:0,0,32,32/south:0,0,32,32/west:0,0,32,32/east:0,0,32,32",
            "scp5001_arch_chamber_floor||full",
            "scp5001_arch_chamber_wall||down:0,18,1,32;0,0,1,14;1,0,14,32;14,1,18,31;18,0,31,32;31,18,32,32;31,0,32,14/up:0,18,1,32;0,0,1,14;1,0,14,32;14,1,18,31;18,0,31,32;31,18,32,32;31,0,32,14/north:0,0,14,32;18,0,32,32/south:0,0,14,32;18,0,32,32/west:0,18,32,32;0,0,32,14/east:0,18,32,32;0,0,32,14",
            "scp5001_arch_floor||full",
            "scp5001_arch_floor_glyph||down:0,0,32,32/up:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/north:0,0,32,32/south:0,0,32,32/west:0,0,32,32/east:0,0,32,32",
            "scp5001_arch_floor_grate||down:0,0,32,32/up:0,25,32,31;0,1,32,7;0,17,32,23;0,9,32,15/north:0,0,6,24;6,0,10,28;10,0,22,24;22,0,26,28;26,0,32,24/south:0,0,6,24;6,0,10,28;10,0,22,24;22,0,26,28;26,0,32,24/west:0,0,24,32;28,25,32,31;28,1,32,7;28,17,32,23;28,9,32,15/east:0,0,24,32;28,25,32,31;28,1,32,7;28,17,32,23;28,9,32,15",
            "scp5001_arch_floor_hex||full",
            "scp5001_arch_floor_seam|axis=x|down:0,0,32,32/up:0,0,15,32;17,0,32,32/north:0,0,15,32;15,0,17,31;17,0,32,32/south:0,0,15,32;15,0,17,31;17,0,32,32/west:0,0,32,32/east:0,0,32,32",
            "scp5001_arch_floor_seam|axis=z|down:0,0,32,32/up:0,0,32,15;0,17,32,32/north:0,0,32,32/south:0,0,32,32/west:0,0,31,32;31,0,32,15;31,17,32,32/east:0,0,31,32;31,0,32,15;31,17,32,32",
            "scp5001_arch_graphite||full",
            "scp5001_arch_graphite_panel||down:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/up:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/north:0,0,4,32;28,0,32,32/south:0,0,4,32;28,0,32,32/west:0,0,32,4;0,28,32,32/east:0,0,32,4;0,28,32,32",
            "scp5001_arch_graphite_seam||down:0,0,32,32/up:0,0,32,32/north:0,18,32,32;0,0,32,14/south:0,18,32,32;0,0,32,14/west:0,0,14,32;18,0,32,32/east:0,0,14,32;18,0,32,32",
            "scp5001_arch_hatch|facing=north|down:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/up:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/north:0,0,4,32;28,0,32,32/south:0,0,4,32;28,0,32,32/west:0,0,32,4;0,28,32,32/east:0,0,32,4;0,28,32,32",
            "scp5001_arch_hatch|facing=east|down:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/up:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/north:0,0,4,32;28,0,32,32/south:0,0,4,32;28,0,32,32/west:0,0,32,4;0,28,32,32/east:0,0,32,4;0,28,32,32",
            "scp5001_arch_hatch|facing=south|down:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/up:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/north:0,0,4,32;28,0,32,32/south:0,0,4,32;28,0,32,32/west:0,0,32,4;0,28,32,32/east:0,0,32,4;0,28,32,32",
            "scp5001_arch_hatch|facing=west|down:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/up:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/north:0,0,4,32;28,0,32,32/south:0,0,4,32;28,0,32,32/west:0,0,32,4;0,28,32,32/east:0,0,32,4;0,28,32,32",
            "scp5001_arch_panel||down:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/up:0,0,1,4;0,28,1,32;1,26,2,32;1,0,2,6;2,0,4,32;4,1,6,31;6,2,26,30;26,1,28,31;28,0,30,32;30,26,31,32;30,0,31,6;31,0,32,4;31,28,32,32/north:0,0,4,32;28,0,32,32/south:0,0,4,32;28,0,32,32/west:0,0,32,4;0,28,32,32/east:0,0,32,4;0,28,32,32",
            "scp5001_arch_panel_glyph|glyph=0|down:0,0,32,32/up:0,0,32,32/north:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/south:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/west:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/east:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32",
            "scp5001_arch_panel_glyph|glyph=1|down:0,0,32,32/up:0,0,32,32/north:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/south:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/west:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/east:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32",
            "scp5001_arch_panel_glyph|glyph=2|down:0,0,32,32/up:0,0,32,32/north:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/south:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/west:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/east:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32",
            "scp5001_arch_panel_seam||down:0,0,32,32/up:0,0,32,32/north:0,24,32,32;0,0,32,20/south:0,24,32,32;0,0,32,20/west:0,0,20,32;24,0,32,32/east:0,0,20,32;24,0,32,32",
            "scp5001_arch_rib||down:0,18,2,32;0,0,2,14;2,0,14,32;14,2,18,30;18,0,30,32;30,18,32,32;30,0,32,14/up:0,18,2,32;0,0,2,14;2,0,14,32;14,2,18,30;18,0,30,32;30,18,32,32;30,0,32,14/north:0,0,14,32;18,0,32,32/south:0,0,14,32;18,0,32,32/west:0,18,32,32;0,0,32,14/east:0,18,32,32;0,0,32,14",
            "scp5001_arch_trim||down:0,0,32,32/up:0,0,32,32/north:0,10,32,32;0,0,32,6/south:0,10,32,32;0,0,32,6/west:0,0,6,32;10,0,32,32/east:0,0,6,32;10,0,32,32",
            "scp5001_arch_trim_amber||down:0,0,32,32/up:0,0,32,32/north:0,10,32,32;0,0,32,6/south:0,10,32,32;0,0,32,6/west:0,0,6,32;10,0,32,32/east:0,0,6,32;10,0,32,32",
            "scp5001_arch_vent|facing=north|down:0,0,32,32/up:0,0,32,32/north:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/south:0,0,32,32/west:0,0,32,32/east:0,0,32,32",
            "scp5001_arch_vent|facing=east|down:0,0,32,32/up:0,0,32,32/north:0,0,32,32/south:0,0,32,32/west:0,0,32,32/east:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32",
            "scp5001_arch_vent|facing=south|down:0,0,32,32/up:0,0,32,32/north:0,0,32,32/south:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/west:0,0,32,32/east:0,0,32,32",
            "scp5001_arch_vent|facing=west|down:0,0,32,32/up:0,0,32,32/north:0,0,32,32/south:0,0,32,32/west:0,0,4,32;4,0,28,4;4,28,28,32;28,0,32,32/east:0,0,32,32",
            "scp5001_shell|seam=false|full",
            "scp5001_shell|seam=true|down:0,0,32,32/up:0,0,32,32/north:0,18,32,32;0,0,32,14/south:0,18,32,32;0,0,32,14/west:0,0,14,32;18,0,32,32/east:0,0,14,32;18,0,32,32"
    };

    static {
        for (String Row : Table) {
            String[] Parts = Row.split("[|]", -1);
            Specs.put(Parts[0] + "|" + Parts[1], Parts[2]);
        }
    }

    private Scp5001WallFaces() {
    }

    public static String key(BlockState State) {
        List<String> Pairs = new ArrayList<>();
        for (Property<?> Each : State.getProperties()) {
            Pairs.add(Each.getName() + "=" + name(State, Each));
        }
        Pairs.sort(String::compareTo);
        return BuiltInRegistries.BLOCK.getKey(State.getBlock()).getPath() + "|" + String.join(",", Pairs);
    }

    private static <T extends Comparable<T>> String name(BlockState State, Property<T> Each) {
        return Each.getName(State.getValue(Each));
    }

    public static boolean known(BlockState State) {
        return Specs.containsKey(key(State));
    }

    public static VoxelShape occlusion(BlockState State) {
        synchronized (Built) {
            return Built.computeIfAbsent(State, Each -> build(Specs.get(key(Each))));
        }
    }

    static VoxelShape build(String Spec) {
        if (Spec == null || Spec.isEmpty()) {
            return Shapes.empty();
        }
        if (Spec.equals("full")) {
            return Shapes.block();
        }
        VoxelShape Made = Shapes.empty();
        for (String Side : Spec.split("/")) {
            String[] Halves = Side.split(":");
            for (String Rect : Halves[1].split(";")) {
                String[] Values = Rect.split(",");
                double A0 = Integer.parseInt(Values[0]) / (double) Grid;
                double B0 = Integer.parseInt(Values[1]) / (double) Grid;
                double A1 = Integer.parseInt(Values[2]) / (double) Grid;
                double B1 = Integer.parseInt(Values[3]) / (double) Grid;
                Made = Shapes.joinUnoptimized(Made, plate(Halves[0], A0, B0, A1, B1), BooleanOp.OR);
            }
        }
        return Made.optimize();
    }

    private static VoxelShape plate(String Side, double A0, double B0, double A1, double B1) {
        return switch (Side) {
            case "down" -> Shapes.box(A0, 0, B0, A1, Thickness, B1);
            case "up" -> Shapes.box(A0, 1 - Thickness, B0, A1, 1, B1);
            case "north" -> Shapes.box(A0, B0, 0, A1, B1, Thickness);
            case "south" -> Shapes.box(A0, B0, 1 - Thickness, A1, B1, 1);
            case "west" -> Shapes.box(0, A0, B0, Thickness, A1, B1);
            default -> Shapes.box(1 - Thickness, A0, B0, 1, A1, B1);
        };
    }
}
