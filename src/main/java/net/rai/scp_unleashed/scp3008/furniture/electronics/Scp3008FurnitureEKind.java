package net.rai.scp_unleashed.scp3008.furniture.electronics;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum Scp3008FurnitureEKind {
    TvBench("tv_bench", Mount.Floor, Stuff.Wood, Scp3008FurnitureEShapes.TvBench, new Spec().cells(Layouts.Quad).rows(2).screen(4)),
    WallTv("wall_tv", Mount.Wall, Stuff.Metal, Scp3008FurnitureEShapes.WallTv, new Spec().cells(Layouts.Pair).screen(4)),
    TvWall("tv_wall", Mount.Wall, Stuff.Wood, Scp3008FurnitureEShapes.TvWall, new Spec().cells(Layouts.Six).screen(5)),
    FloorSpeaker("floor_speaker", Mount.Floor, Stuff.Wood, Scp3008FurnitureEShapes.FloorSpeaker, new Spec().screen(1)),
    SoundbarSet("soundbar_set", Mount.Floor, Stuff.Wood, Scp3008FurnitureEShapes.SoundbarSet, new Spec().screen(1)),
    RecordPlayer("record_player", Mount.Floor, Stuff.Wood, Scp3008FurnitureEShapes.RecordPlayer, new Spec().screen(1)),
    Radio("radio", Mount.Floor, Stuff.Wood, Scp3008FurnitureEShapes.Radio, new Spec().screen(1)),
    ComputerDesk("computer_desk", Mount.Floor, Stuff.Wood, Scp3008FurnitureEShapes.ComputerDesk, new Spec().cells(Layouts.Quad).screen(4)),
    LaptopStand("laptop_stand", Mount.Floor, Stuff.Metal, Scp3008FurnitureEShapes.LaptopStand, new Spec().screen(3)),
    PhoneTable("phone_table", Mount.Floor, Stuff.Wood, Scp3008FurnitureEShapes.PhoneTable, new Spec().cells(Layouts.Pair).screen(3)),
    HeadphoneWall("headphone_wall", Mount.Wall, Stuff.Wood, Scp3008FurnitureEShapes.HeadphoneWall, new Spec().cells(Layouts.Pair)),
    SmartSpeaker("smart_speaker", Mount.Floor, Stuff.Wood, Scp3008FurnitureEShapes.SmartSpeaker, new Spec().screen(2)),
    RouterShelf("router_shelf", Mount.Wall, Stuff.Wood, Scp3008FurnitureEShapes.RouterShelf, new Spec().screen(1)),
    ConsoleKiosk("console_kiosk", Mount.Floor, Stuff.Wood, Scp3008FurnitureEShapes.ConsoleKiosk, new Spec().cells(Layouts.Tall).screen(4)),
    ChargingStation("charging_station", Mount.Floor, Stuff.Metal, Scp3008FurnitureEShapes.ChargingStation, new Spec().cells(Layouts.Tall).rows(1)),
    ChargerLamp("charger_lamp", Mount.Floor, Stuff.Metal, Scp3008FurnitureEShapes.ChargerLamp, new Spec().lamp(13)),
    BoxedStack("boxed_stack", Mount.Floor, Stuff.Card, Scp3008FurnitureEShapes.BoxedStack, new Spec().rows(1)),
    CableWall("cable_wall", Mount.Wall, Stuff.Wood, Scp3008FurnitureEShapes.CableWall, new Spec().cells(Layouts.Pair)),
    GondolaEnd("gondola_end", Mount.Floor, Stuff.Metal, Scp3008FurnitureEShapes.GondolaEnd, new Spec().cells(Layouts.Tall).rows(2)),
    GamingChair("gaming_chair", Mount.Floor, Stuff.Fabric, Scp3008FurnitureEShapes.GamingChair, new Spec().seat(10.0 / 16.0));

    public static final String Department = "electronics";

    private static final class Layouts {
        private static final int[][] Single = {{0, 0, 0}};
        private static final int[][] Pair = {{0, 0, 0}, {1, 0, 0}};
        private static final int[][] Tall = {{0, 0, 0}, {0, 1, 0}};
        private static final int[][] Quad = {{0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {1, 1, 0}};
        private static final int[][] Six = {{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 1, 0}, {1, 1, 0}, {2, 1, 0}};
    }

    public enum Mount {
        Floor("scp3008_floor_standing"), Wall("scp3008_wall_mounted"), Ceiling("scp3008_ceiling_hung");

        public final String TagName;

        Mount(String TagName) {
            this.TagName = TagName;
        }
    }

    public enum Stuff {
        Wood, Metal, Fabric, Card
    }

    public final String Id;
    public final Mount Placement;
    public final Stuff Material;
    public final boolean Seat;
    public final double SeatTop;
    public final int Lamp;
    public final int Screen;
    public final int Rows;
    public final int[][] Cells;
    private final List<Map<Direction, VoxelShape>> Shaped;

    Scp3008FurnitureEKind(String Name, Mount Placement, Stuff Material, double[][][] Boxes, Spec Plan) {
        this.Id = "scp3008_electronics_" + Name;
        this.Placement = Placement;
        this.Material = Material;
        this.Seat = Plan.SeatTop > 0.0;
        this.SeatTop = Plan.SeatTop;
        this.Lamp = Plan.Lamp;
        this.Screen = Plan.Screen;
        this.Rows = Plan.Rows;
        this.Cells = Plan.Cells;
        this.Shaped = new ArrayList<>();
        for (double[][] Part : Boxes) {
            Shaped.add(table(Part));
        }
        if (Shaped.size() != Cells.length) {
            throw new IllegalStateException(Id + " has " + Shaped.size() + " shapes for " + Cells.length + " cells");
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

    public boolean screen() {
        return Screen > 0;
    }

    public boolean lamp() {
        return Lamp > 0;
    }

    public boolean holds(int Part) {
        return storage() && Cells[Part][1] == 0;
    }

    public boolean switches(int Part) {
        return screen() && !holds(Part);
    }

    public VoxelShape shape(int Part, Direction Facing) {
        return Shaped.get(Math.max(0, Math.min(Part, Shaped.size() - 1))).get(Facing);
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
        private double SeatTop;
        private int Lamp;
        private int Screen;
        private int Rows;
        private int[][] Cells = Layouts.Single;

        private Spec seat(double Top) {
            SeatTop = Top;
            return this;
        }

        private Spec lamp(int Level) {
            Lamp = Level;
            return this;
        }

        private Spec screen(int Level) {
            Screen = Level;
            return this;
        }

        private Spec rows(int Count) {
            Rows = Count;
            return this;
        }

        private Spec cells(int[][] Layout) {
            Cells = Layout;
            return this;
        }
    }
}
