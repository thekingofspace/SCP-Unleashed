package net.rai.scp_unleashed.scp3008.furniture.children;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum Scp3008FurnitureCKind {
    BunkBed("scp3008_children_bunk_bed", "children", Mount.Floor, Stuff.Wood, new Spec()
            .cells(new int[][]{{0, 0, 0}, {0, 1, 0}, {0, 0, 1}, {0, 1, 1}})
            .boxes(0, box(0, 0, 0, 16, 8.5, 16), box(0, 0, 0, 2, 16, 2), box(14, 0, 0, 16, 16, 2), box(4, 0, 0, 12, 16, 1))
            .boxes(1, box(0, 0.5, 0, 16, 6.5, 16), box(0, 0, 0, 2, 14, 2), box(14, 0, 0, 16, 14, 2), box(0, 4, 0, 1.5, 11, 16), box(14.5, 9.5, 0, 16, 11, 16),
                    box(4, 0, 0, 12, 12, 1))
            .boxes(2, box(0, 0, 0, 16, 8.5, 16), box(0, 0, 14, 16, 13, 16), box(0, 0, 14, 2, 16, 16), box(14, 0, 14, 16, 16, 16))
            .boxes(3, box(0, 0.5, 0, 16, 6.5, 16), box(0, 0, 14, 16, 12, 16), box(0, 4, 0, 1.5, 11, 16), box(14.5, 4, 0, 16, 11, 16))),
    ToyChest("scp3008_children_toy_chest", "children", Mount.Floor, Stuff.Wood, new Spec().rows(2)
            .boxes(0, box(0.5, 0, 2.5, 15.5, 12.5, 13.5))
            .opened(0, box(1, 0, 3, 15, 11, 13), box(0.5, 11, 13, 15.5, 22, 14.5))),
    PlayTable("scp3008_children_play_table", "children", Mount.Floor, Stuff.Wood, new Spec()
            .boxes(0, box(1, 7.5, 1, 15, 10, 15), box(2, 0, 2, 4, 7.5, 4), box(12, 0, 2, 14, 7.5, 4), box(2, 0, 12, 4, 7.5, 14), box(12, 0, 12, 14, 7.5, 14))),
    ChildChair("scp3008_children_chair", "children", Mount.Floor, Stuff.Wood, new Spec().seat()
            .boxes(0, box(2.5, 0, 2.5, 13.5, 8.5, 12.5), box(3, 0, 11, 13, 16, 13))),
    RockingElk("scp3008_children_rocking_elk", "children", Mount.Floor, Stuff.Wood, new Spec().seat()
            .boxes(0, box(3, 0, 0, 13, 10.5, 16), box(5.5, 10.5, 0, 10.5, 17, 5), box(2, 17, 1, 14, 21.5, 3.5))),
    PlayKitchen("scp3008_children_play_kitchen", "children", Mount.Floor, Stuff.Wood, new Spec().opens()
            .boxes(0, box(0, 0, 2, 16, 12, 16), box(0.5, 12, 14.5, 15.5, 24, 16), box(1, 20, 11.5, 15, 21, 14.5))
            .opened(0, box(0, 0, 2, 16, 12, 16), box(0.5, 12, 14.5, 15.5, 24, 16), box(1, 20, 11.5, 15, 21, 14.5), box(1.5, 0, 0, 8, 2, 2))),
    PendantLamp("scp3008_lighting_pendant_lamp", "lighting", Mount.Ceiling, Stuff.Glass, new Spec().lamp(14)
            .boxes(0, box(2.5, 0.5, 2.5, 13.5, 9.5, 13.5), box(7, 9.5, 7, 9, 16, 9))),
    FloorLamp("scp3008_lighting_floor_lamp", "lighting", Mount.Floor, Stuff.Wood, new Spec().lamp(0, 15)
            .cells(new int[][]{{0, 0, 0}, {0, 1, 0}})
            .boxes(0, box(2.5, 0, 2.5, 13.5, 16, 13.5))
            .boxes(1, box(6, 0, 6, 10, 5, 10), box(2.5, 5, 2.5, 13.5, 15, 13.5))),
    TableLamp("scp3008_lighting_table_lamp", "lighting", Mount.Floor, Stuff.Glass, new Spec().lamp(12)
            .boxes(0, box(3, 0, 3, 13, 11, 13))),
    LampDisplay("scp3008_lighting_lamp_display", "lighting", Mount.Wall, Stuff.Wood, new Spec().lamp(11, 11, 11, 11)
            .cells(new int[][]{{0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {1, 1, 0}})
            .boxes(0, box(0, 0, 13.5, 16, 16, 16), box(1, 2, 9, 16, 3, 13.5), box(8, 5.5, 7, 13, 12.5, 13.5), box(5, 14.5, 6, 11, 16, 12))
            .boxes(1, box(0, 0, 13.5, 16, 16, 16), box(0, 2, 9, 15, 3, 13.5), box(4, 3, 9.5, 10, 10, 13.5), box(11, 3, 10, 14, 8.5, 13.5))
            .boxes(2, box(0, 0, 13.5, 16, 16, 16), box(5, 0, 6, 11, 3.5, 12), box(7.5, 2.5, 10.5, 8.5, 4.5, 13.5))
            .boxes(3, box(0, 0, 13.5, 16, 16, 16), box(7.5, 8, 6, 8.5, 9, 13.5), box(4, 0.5, 6, 12, 4.5, 14))),
    RugRolls("scp3008_textiles_rug_rolls", "textiles", Mount.Floor, Stuff.Fabric, new Spec()
            .boxes(0, box(1, 0, 1, 15, 16, 15), box(2, 16, 2, 12, 24, 12))),
    CushionBin("scp3008_textiles_cushion_bin", "textiles", Mount.Floor, Stuff.Fabric, new Spec().rows(2)
            .boxes(0, box(0.75, 0, 0.75, 15.25, 10, 15.25), box(3.5, 10, 4.5, 12, 15, 14))),
    CurtainRack("scp3008_textiles_curtain_rack", "textiles", Mount.Floor, Stuff.Metal, new Spec()
            .boxes(0, box(0.5, 0, 2, 15.5, 1, 14), box(1, 0, 6.5, 15, 24, 9.5))),
    DuvetStack("scp3008_textiles_duvet_stack", "textiles", Mount.Floor, Stuff.Fabric, new Spec()
            .boxes(0, box(1, 0, 2, 15, 15.5, 14), box(10, 15.5, 9.5, 15.5, 21.5, 10))),
    GardenChair("scp3008_outdoor_garden_chair", "outdoor", Mount.Floor, Stuff.Wood, new Spec().seat()
            .boxes(0, box(1.5, 0, 1.5, 14.5, 9, 14.5), box(1.5, 9, 2, 3.5, 12, 13), box(12.5, 9, 2, 14.5, 12, 13), box(3, 8, 12.5, 13, 20.5, 15.5))),
    Parasol("scp3008_outdoor_parasol", "outdoor", Mount.Floor, Stuff.Wood, new Spec().opens().startsOpen()
            .cells(new int[][]{{0, 0, 0}, {0, 1, 0}})
            .boxes(0, box(3, 0, 3, 13, 2, 13), box(7, 2, 7, 9, 16, 9))
            .boxes(1, box(6, 0, 6, 10, 15, 10))
            .opened(0, box(3, 0, 3, 13, 2, 13), box(7, 2, 7, 9, 16, 9))
            .opened(1, box(7, 0, 7, 9, 6.5, 9), box(0, 6.5, 0, 16, 15, 16))),
    Planter("scp3008_outdoor_planter", "outdoor", Mount.Floor, Stuff.Wood, new Spec()
            .boxes(0, box(1.75, 0, 1.75, 14.25, 11, 14.25), box(2.5, 11, 2.5, 13.5, 23.5, 13.5))),
    GardenTable("scp3008_outdoor_garden_table", "outdoor", Mount.Floor, Stuff.Wood, new Spec()
            .boxes(0, box(0.5, 13, 0.5, 15.5, 14.5, 15.5), box(1.5, 0, 1.5, 3.5, 13, 3.5), box(12.5, 0, 1.5, 14.5, 13, 3.5), box(1.5, 0, 12.5, 3.5, 13, 14.5),
                    box(12.5, 0, 12.5, 14.5, 13, 14.5), box(6, 14.5, 6, 10, 21, 10))),
    FlatpackStack("scp3008_warehouse_flatpack_stack", "warehouse", Mount.Floor, Stuff.Card, new Spec().rows(2)
            .boxes(0, box(0.5, 0, 1, 15.5, 14, 15))),
    PalletStack("scp3008_warehouse_pallet_stack", "warehouse", Mount.Floor, Stuff.Wood, new Spec()
            .boxes(0, box(0, 0, 0, 16, 14, 16))),
    PickingTrolley("scp3008_warehouse_picking_trolley", "warehouse", Mount.Floor, Stuff.Metal, new Spec().rows(1)
            .boxes(0, box(1, 0, 1, 15, 7, 15), box(5, 7, 3.5, 11, 15, 8), box(2, 3, 12.75, 14, 22.25, 14.25))),
    AisleSign("scp3008_warehouse_aisle_sign", "warehouse", Mount.Floor, Stuff.Metal, new Spec().aisle()
            .boxes(0, box(3, 0, 3, 13, 2, 13), box(7, 2, 7, 9, 12, 9), box(0, 12, 6.75, 16, 24, 9.25)));

    public static final int AisleCount = 24;
    public static final List<String> Departments = List.of("living", "dining", "kitchen", "bedroom", "bathroom", "office", "children", "lighting", "textiles", "outdoor",
            "warehouse");

    public enum Mount {
        Floor("scp3008_floor_standing"), Wall("scp3008_wall_mounted"), Ceiling("scp3008_ceiling_hung");

        public final String TagName;

        Mount(String TagName) {
            this.TagName = TagName;
        }
    }

    public enum Stuff {
        Wood, Fabric, Metal, Glass, Card
    }

    public final String Id;
    public final String Department;
    public final Mount Placement;
    public final Stuff Material;
    public final boolean Seat;
    public final boolean Lamp;
    public final boolean Opens;
    public final boolean StartsOpen;
    public final boolean Aisle;
    public final int Rows;
    public final int[] Glow;
    public final int[][] Cells;
    private final List<Map<Direction, VoxelShape>> Closed;
    private final List<Map<Direction, VoxelShape>> Unfolded;

    Scp3008FurnitureCKind(String Id, String Department, Mount Placement, Stuff Material, Spec Plan) {
        this.Id = Id;
        this.Department = Department;
        this.Placement = Placement;
        this.Material = Material;
        this.Seat = Plan.Seat;
        this.Lamp = Plan.Glow != null;
        this.Opens = Plan.Opens;
        this.StartsOpen = Plan.StartsOpen;
        this.Aisle = Plan.Aisle;
        this.Rows = Plan.Rows;
        this.Cells = Plan.Cells;
        this.Glow = Plan.Glow == null ? new int[Plan.Cells.length] : Plan.Glow;
        this.Closed = new ArrayList<>();
        this.Unfolded = new ArrayList<>();
        for (int Index = 0; Index < Plan.Cells.length; Index++) {
            double[][] Shut = Plan.Boxes.get(Index);
            double[][] Wide = Plan.Opened.getOrDefault(Index, Shut);
            Closed.add(table(Shut));
            Unfolded.add(table(Wide));
        }
    }

    public int parts() {
        return Cells.length;
    }

    public boolean multiblock() {
        return Cells.length > 1;
    }

    public boolean storage() {
        return Rows > 0;
    }

    public boolean hasOpen() {
        return Opens || Rows > 0;
    }

    public VoxelShape shape(int Part, Direction Facing, boolean IsOpen) {
        List<Map<Direction, VoxelShape>> Source = IsOpen ? Unfolded : Closed;
        return Source.get(Math.max(0, Math.min(Part, Source.size() - 1))).get(Facing);
    }

    private static double[] box(double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return new double[]{X0, Y0, Z0, X1, Y1, Z1};
    }

    public static VoxelShape turned(Direction Facing, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Facing) {
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    private static Map<Direction, VoxelShape> table(double[][] Boxes) {
        Map<Direction, VoxelShape> Made = new EnumMap<>(Direction.class);
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            VoxelShape Combined = Shapes.empty();
            for (double[] Entry : Boxes) {
                Combined = Shapes.or(Combined, turned(Facing, Entry[0], Entry[1], Entry[2], Entry[3], Entry[4], Entry[5]));
            }
            Made.put(Facing, Combined.optimize());
        }
        return Made;
    }

    private static final class Spec {
        private boolean Seat;
        private boolean Opens;
        private boolean StartsOpen;
        private boolean Aisle;
        private int Rows;
        private int[] Glow;
        private int[][] Cells = {{0, 0, 0}};
        private final Map<Integer, double[][]> Boxes = new java.util.HashMap<>();
        private final Map<Integer, double[][]> Opened = new java.util.HashMap<>();

        private Spec seat() {
            Seat = true;
            return this;
        }

        private Spec opens() {
            Opens = true;
            return this;
        }

        private Spec startsOpen() {
            StartsOpen = true;
            return this;
        }

        private Spec aisle() {
            Aisle = true;
            return this;
        }

        private Spec rows(int Count) {
            Rows = Count;
            return this;
        }

        private Spec lamp(int... Levels) {
            Glow = Levels;
            return this;
        }

        private Spec cells(int[][] Layout) {
            Cells = Layout;
            return this;
        }

        private Spec boxes(int Part, double[]... Entries) {
            Boxes.put(Part, Entries);
            return this;
        }

        private Spec opened(int Part, double[]... Entries) {
            Opened.put(Part, Entries);
            return this;
        }
    }
}
