package net.rai.scp_unleashed.scp3008.furniture.bedroom;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum Scp3008FurnitureBPiece {
    DoubleBed("scp3008_bedroom_double_bed", "bedroom", Mount.Floor, Feel.Wood, MapColor.WOOD, 2, 1, 2, Trick.Bed, 0, Opening.None,
            new double[][]{{0, 0, 0.5, 32, 10, 29.5}, {0, 0, 29.5, 32, 20, 32}}, null),
    SingleBed("scp3008_bedroom_single_bed", "bedroom", Mount.Floor, Feel.Wood, MapColor.SNOW, 1, 1, 2, Trick.Bed, 0, Opening.None,
            new double[][]{{0, 0, 0, 16, 14, 1.75}, {0, 0, 1.75, 16, 10.75, 30.25}, {0, 0, 30.25, 16, 20, 32}}, null),
    Wardrobe("scp3008_bedroom_wardrobe", "bedroom", Mount.Floor, Feel.Wood, MapColor.SNOW, 2, 2, 1, Trick.None, 27, Opening.Doors,
            new double[][]{{0, 0, 2, 32, 32, 16}}, null),
    ChestOfDrawers("scp3008_bedroom_chest_of_drawers", "bedroom", Mount.Floor, Feel.Wood, MapColor.SNOW, 1, 1, 1, Trick.None, 27, Opening.Drawers,
            new double[][]{{0, 0, 2, 16, 15, 16}}, null),
    Nightstand("scp3008_bedroom_nightstand", "bedroom", Mount.Floor, Feel.Wood, MapColor.COLOR_BLACK, 1, 1, 1, Trick.None, 9, Opening.Drawers,
            new double[][]{{2, 0, 3, 14, 13, 15.5}}, null),
    FloorMirror("scp3008_bedroom_floor_mirror", "bedroom", Mount.Floor, Feel.Wood, MapColor.SAND, 1, 2, 1, Trick.None, 0, Opening.None,
            new double[][]{{0.5, 0, 4, 15.5, 1.5, 13}, {1, 0, 7.75, 15, 29, 9.25}}, null),
    ClothesRail("scp3008_bedroom_clothes_rail", "bedroom", Mount.Floor, Feel.Metal, MapColor.COLOR_BLACK, 1, 2, 1, Trick.None, 0, Opening.None,
            new double[][]{{0.5, 0, 3, 15.5, 1, 13}, {0.75, 1, 5, 15.25, 28, 11}}, null),
    Vanity("scp3008_bathroom_vanity", "bathroom", Mount.Floor, Feel.Ceramic, MapColor.QUARTZ, 1, 1, 1, Trick.Sink, 9, Opening.Doors,
            new double[][]{{0, 0, 1.5, 16, 15.5, 16}}, null),
    Toilet("scp3008_bathroom_toilet", "bathroom", Mount.Floor, Feel.Ceramic, MapColor.QUARTZ, 1, 1, 1, Trick.Toilet, 0, Opening.None,
            new double[][]{{3.25, 0, 1.25, 12.75, 9.5, 13}, {2.25, 0, 12.25, 13.75, 18, 16}}, null),
    Bathtub("scp3008_bathroom_bathtub", "bathroom", Mount.Floor, Feel.Ceramic, MapColor.QUARTZ, 1, 1, 2, Trick.Bath, 0, Opening.None,
            new double[][]{{0, 0, 0, 16, 2.5, 32}, {0, 0, 0, 1.5, 9, 32}, {14.5, 0, 0, 16, 9, 32}, {0, 0, 0, 16, 9, 1.5}, {0, 0, 30.5, 16, 9, 32}}, null),
    Shower("scp3008_bathroom_shower", "bathroom", Mount.Floor, Feel.Glass, MapColor.SNOW, 1, 2, 1, Trick.None, 0, Opening.Glass,
            new double[][]{{0, 0, 0, 16, 1.5, 16}, {0, 0, 15, 16, 31, 16}, {0, 0, 0, 1, 31, 15}, {15.25, 0, 0, 16, 30, 16}, {0, 0, 0, 16, 30, 0.75}},
            new double[][]{{0, 0, 0, 16, 1.5, 16}, {0, 0, 15, 16, 31, 16}, {0, 0, 0, 1, 31, 15}, {15.25, 0, 0, 16, 30, 16}}),
    TowelLadder("scp3008_bathroom_towel_ladder", "bathroom", Mount.Floor, Feel.Wood, MapColor.SAND, 1, 2, 1, Trick.None, 0, Opening.None,
            new double[][]{{1.5, 0, 6, 14.5, 12, 11}, {1.5, 12, 9, 14.5, 28, 16}}, null),
    MirrorCabinet("scp3008_bathroom_mirror_cabinet", "bathroom", Mount.Wall, Feel.Wood, MapColor.SNOW, 1, 1, 1, Trick.None, 9, Opening.Doors,
            new double[][]{{0.5, 0.75, 8, 15.5, 16, 16}}, null),
    LaundryBasket("scp3008_bathroom_laundry_basket", "bathroom", Mount.Floor, Feel.Woven, MapColor.SAND, 1, 1, 1, Trick.None, 9, Opening.Lid,
            new double[][]{{2.5, 0, 2.5, 13.5, 14, 13.5}}, null),
    Desk("scp3008_office_desk", "office", Mount.Floor, Feel.Wood, MapColor.SNOW, 2, 1, 1, Trick.None, 0, Opening.None,
            new double[][]{{0, 12, 0.5, 32, 13, 15}, {0.75, 0, 1.5, 2.75, 12, 14.5}, {29.25, 0, 1.5, 31.25, 12, 14.5}}, null),
    OfficeChair("scp3008_office_chair", "office", Mount.Floor, Feel.Fabric, MapColor.COLOR_BLACK, 1, 1, 1, Trick.Chair, 0, Opening.None,
            new double[][]{{2, 0, 2, 14, 10, 13}, {2.5, 10, 12.5, 13.5, 24, 14.5}}, null),
    FilingCabinet("scp3008_office_filing_cabinet", "office", Mount.Floor, Feel.Metal, MapColor.METAL, 1, 1, 1, Trick.None, 27, Opening.Drawers,
            new double[][]{{1.75, 0, 2.5, 14.25, 16, 15.75}}, null),
    Shelving("scp3008_office_shelving", "office", Mount.Floor, Feel.Metal, MapColor.COLOR_BLACK, 1, 2, 1, Trick.None, 0, Opening.None,
            new double[][]{{0, 0, 2.5, 16, 30, 15.5}}, null),
    DrawerUnit("scp3008_office_drawer_unit", "office", Mount.Floor, Feel.Wood, MapColor.SAND, 1, 1, 1, Trick.None, 9, Opening.Drawers,
            new double[][]{{1.75, 0, 2, 14.25, 12.75, 15.25}}, null),
    DeskLamp("scp3008_office_desk_lamp", "office", Mount.Floor, Feel.Lamp, MapColor.COLOR_YELLOW, 1, 1, 1, Trick.Lamp, 0, Opening.None,
            new double[][]{{5, 0, 1, 11, 11, 14}}, new double[][]{{5, 0, 1, 11, 8, 14}}),
    Whiteboard("scp3008_office_whiteboard", "office", Mount.Wall, Feel.Metal, MapColor.SNOW, 2, 1, 1, Trick.None, 0, Opening.None,
            new double[][]{{0.5, 2.5, 13.25, 31.5, 15, 16}}, null);

    public enum Mount {
        Floor("scp3008_floor_standing"), Wall("scp3008_wall_mounted"), Ceiling("scp3008_ceiling_hung");

        public final String TagPath;

        Mount(String TagPath) {
            this.TagPath = TagPath;
        }
    }

    public enum Feel {
        Wood(SoundType.WOOD, 1.5f, 3.0f, "axe"), Fabric(SoundType.WOOL, 0.8f, 0.8f, "axe"), Ceramic(SoundType.DECORATED_POT, 1.5f, 4.0f, "pickaxe"),
        Metal(SoundType.METAL, 2.0f, 4.0f, "pickaxe"), Glass(SoundType.GLASS, 0.8f, 1.0f, "pickaxe"), Woven(SoundType.BAMBOO_WOOD, 0.8f, 1.0f, "axe"),
        Lamp(SoundType.LANTERN, 0.8f, 1.0f, "pickaxe");

        public final SoundType Sound;
        public final float Hardness;
        public final float Resistance;
        public final String Tool;

        Feel(SoundType Sound, float Hardness, float Resistance, String Tool) {
            this.Sound = Sound;
            this.Hardness = Hardness;
            this.Resistance = Resistance;
            this.Tool = Tool;
        }
    }

    public enum Trick {
        None, Bed, Chair, Toilet, Lamp, Bath, Sink
    }

    public enum Opening {
        None, Doors, Drawers, Lid, Glass
    }

    public final String Name;
    public final String Department;
    public final Mount Placement;
    public final Feel Finish;
    public final MapColor Colour;
    public final int Wide;
    public final int High;
    public final int Deep;
    public final Trick Action;
    public final int Slots;
    public final Opening Opens;
    private final List<Map<Direction, VoxelShape>> Outlines;
    private final List<Map<Direction, VoxelShape>> AltOutlines;

    Scp3008FurnitureBPiece(String Name, String Department, Mount Placement, Feel Finish, MapColor Colour, int Wide, int High, int Deep, Trick Action, int Slots,
                           Opening Opens, double[][] Boxes, double[][] AltBoxes) {
        this.Name = Name;
        this.Department = Department;
        this.Placement = Placement;
        this.Finish = Finish;
        this.Colour = Colour;
        this.Wide = Wide;
        this.High = High;
        this.Deep = Deep;
        this.Action = Action;
        this.Slots = Slots;
        this.Opens = Opens;
        this.Outlines = sliced(Boxes, Wide, High, Deep);
        this.AltOutlines = AltBoxes == null ? this.Outlines : sliced(AltBoxes, Wide, High, Deep);
    }

    public int parts() {
        return Wide * High * Deep;
    }

    public boolean multi() {
        return parts() > 1;
    }

    public boolean seats() {
        return Action == Trick.Bed || Action == Trick.Chair || Action == Trick.Toilet;
    }

    public boolean storage() {
        return Slots > 0;
    }

    public int cellX(int Part) {
        return Part % Wide;
    }

    public int cellY(int Part) {
        return (Part / Wide) % High;
    }

    public int cellZ(int Part) {
        return Part / (Wide * High);
    }

    public int partAt(int X, int Y, int Z) {
        if (X < 0 || Y < 0 || Z < 0 || X >= Wide || Y >= High || Z >= Deep) {
            return -1;
        }
        return X + Y * Wide + Z * Wide * High;
    }

    public VoxelShape shape(int Part, Direction Facing, boolean Alternate) {
        List<Map<Direction, VoxelShape>> Table = Alternate ? AltOutlines : Outlines;
        if (Part < 0 || Part >= Table.size()) {
            return Shapes.empty();
        }
        return Table.get(Part).get(Facing);
    }

    public static VoxelShape turned(Direction Facing, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return switch (Facing) {
            case SOUTH -> Block.box(16.0 - X1, Y0, 16.0 - Z1, 16.0 - X0, Y1, 16.0 - Z0);
            case WEST -> Block.box(Z0, Y0, 16.0 - X1, Z1, Y1, 16.0 - X0);
            case EAST -> Block.box(16.0 - Z1, Y0, X0, 16.0 - Z0, Y1, X1);
            default -> Block.box(X0, Y0, Z0, X1, Y1, Z1);
        };
    }

    private static List<Map<Direction, VoxelShape>> sliced(double[][] Boxes, int Wide, int High, int Deep) {
        List<Map<Direction, VoxelShape>> Made = new ArrayList<>();
        for (int Part = 0; Part < Wide * High * Deep; Part++) {
            int Cx = Part % Wide;
            int Cy = (Part / Wide) % High;
            int Cz = Part / (Wide * High);
            Map<Direction, VoxelShape> ByFacing = new EnumMap<>(Direction.class);
            for (Direction Facing : Direction.Plane.HORIZONTAL) {
                VoxelShape Shape = Shapes.empty();
                for (double[] Box : Boxes) {
                    double X0 = Math.max(Box[0], Cx * 16.0) - Cx * 16.0;
                    double X1 = Math.min(Box[3], Cx * 16.0 + 16.0) - Cx * 16.0;
                    double Y0 = Math.max(Box[1], Cy * 16.0) - Cy * 16.0;
                    double Y1 = Math.min(Box[4], Cy * 16.0 + (Cy == High - 1 ? 24.0 : 16.0)) - Cy * 16.0;
                    double Z0 = Math.max(Box[2], Cz * 16.0) - Cz * 16.0;
                    double Z1 = Math.min(Box[5], Cz * 16.0 + 16.0) - Cz * 16.0;
                    if (X1 - X0 <= 0.0 || Y1 - Y0 <= 0.0 || Z1 - Z0 <= 0.0) {
                        continue;
                    }
                    Shape = Shapes.or(Shape, turned(Facing, X0, Y0, Z0, X1, Y1, Z1));
                }
                ByFacing.put(Facing, Shape.optimize());
            }
            Made.add(ByFacing);
        }
        return Made;
    }
}
