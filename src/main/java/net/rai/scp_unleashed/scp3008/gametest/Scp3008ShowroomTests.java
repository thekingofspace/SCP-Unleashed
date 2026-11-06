package net.rai.scp_unleashed.scp3008.gametest;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Canvas;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Code;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Painter;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.block.Scp3008RafterBlock;
import net.rai.scp_unleashed.scp3008.world.Scp3008Catalogue;
import net.rai.scp_unleashed.scp3008.world.Scp3008Footprint;
import net.rai.scp_unleashed.scp3008.world.Scp3008Furnishing;
import net.rai.scp_unleashed.scp3008.world.Scp3008Showrooms;
import net.rai.scp_unleashed.scp3008.world.Scp3008Palette;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008ShowroomTests {
    public static final String DumpVariable = "SCP3008_PREVIEW_DUMP";
    public static final String BoxDumpVariable = "SCP3008_BOX_DUMP";
    public static final double Overhang = 1.01 / 16.0;
    public static final int[] BoxPlots = {Scp3008Plan.RackPlot, Scp3008Plan.StoragePlot, Scp3008Plan.MarketPlot, Scp3008Plan.FoodPlot, Scp3008Plan.GardenPlot,
            Scp3008Plan.ExitPlot, Scp3008Plan.RoomPlot};
    public static final int LowY = Scp3008Plan.FloorBase;
    public static final int HighY = Scp3008Plan.Stand + 13;

    private Scp3008ShowroomTests() {
    }

    public record Site(int Department, int Ux, int Uz, Scp3008Plan.Plot Plot) {
        public int minX() {
            return Scp3008Plan.originX(Ux);
        }

        public int maxX() {
            return Scp3008Plan.originX(Ux) + Scp3008Plan.Unit - 1;
        }

        public int minZ() {
            return Scp3008Plan.originZ(Uz) + Plot.Start() - 1;
        }

        public int maxZ() {
            return Scp3008Plan.originZ(Uz) + Plot.Start() + Plot.Length();
        }
    }

    public static int preferredPlot(int Department) {
        return Scp3008Plan.RoomPlot;
    }

    public static Site find(int Department, int Plot) {
        for (int Ring = 0; Ring < 80; Ring++) {
            for (int Ux = -Ring; Ux <= Ring; Ux++) {
                for (int Uz = -Ring; Uz <= Ring; Uz++) {
                    if (Math.max(Math.abs(Ux), Math.abs(Uz)) != Ring || Scp3008Plan.department(Ux, Uz) != Department || Scp3008Plan.isFoodCourt(Ux, Uz)
                            || Scp3008Plan.isExit(Ux, Uz)) {
                        continue;
                    }
                    for (Scp3008Plan.Plot Next : Scp3008Plan.plots(Ux, Uz)) {
                        if (Next.Type() == Plot) {
                            return new Site(Department, Ux, Uz, Next);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static Site anyPlot(int Type) {
        for (int Ring = 0; Ring < 80; Ring++) {
            for (int Ux = -Ring; Ux <= Ring; Ux++) {
                for (int Uz = -Ring; Uz <= Ring; Uz++) {
                    if (Math.max(Math.abs(Ux), Math.abs(Uz)) != Ring || Scp3008Plan.isExit(Ux, Uz) != (Type == Scp3008Plan.ExitPlot)) {
                        continue;
                    }
                    for (Scp3008Plan.Plot Next : Scp3008Plan.plots(Ux, Uz)) {
                        if (Next.Type() == Type) {
                            return new Site(Scp3008Plan.department(Ux, Uz), Ux, Uz, Next);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static List<Site> sites() {
        List<Site> Found = new ArrayList<>();
        for (int Department = 0; Department < Scp3008Plan.Departments.length; Department++) {
            Site Next = find(Department, preferredPlot(Department));
            if (Next == null) {
                Next = find(Department, Department == Scp3008Plan.Warehouse ? Scp3008Plan.RackPlot : Scp3008Plan.MarketPlot);
            }
            if (Next != null) {
                Found.add(Next);
            }
        }
        return Found;
    }

    private static final class LevelCanvas implements Scp3008Canvas {
        private final ServerLevel World;
        private final Site Area;
        private final int Top;

        private LevelCanvas(ServerLevel World, Site Area, int Top) {
            this.World = World;
            this.Area = Area;
            this.Top = Top;
        }

        @Override
        public int minX() {
            return Area.minX();
        }

        @Override
        public int minZ() {
            return Area.minZ();
        }

        @Override
        public int maxX() {
            return Area.maxX();
        }

        @Override
        public int maxZ() {
            return Area.maxZ();
        }

        @Override
        public void put(int X, int Y, int Z, int Code) {
            if (Y < LowY || Y > Top || Scp3008Code.decor(Code)) {
                return;
            }
            BlockState State = Scp3008Palette.state(Code);
            if (State != null) {
                World.setBlock(new BlockPos(X, Y, Z), State, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    public static void build(ServerLevel World, Site Area) {
        build(World, Area, HighY);
    }

    public static void build(ServerLevel World, Site Area, int Top) {
        for (BlockPos Pos : BlockPos.betweenClosed(Area.minX(), LowY, Area.minZ(), Area.maxX(), Top, Area.maxZ())) {
            if (!World.getBlockState(Pos).isAir()) {
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        Scp3008Painter.paint(Area.Ux(), Area.Uz(), new LevelCanvas(World, Area, Top));
        Scp3008Furnishing.decorateArea(World, Area.minX(), Area.minZ(), Area.maxX(), Area.maxZ());
    }

    public static List<String> scan(ServerLevel World, Site Area) {
        return scan(World, Area, HighY);
    }

    public static List<String> scan(ServerLevel World, Site Area, int Top) {
        Map<BlockPos, Integer> Pieces = new HashMap<>();
        if (Area.Plot().Type() == Scp3008Plan.RoomPlot) {
            for (Scp3008Showrooms.Placement Piece : Scp3008Showrooms.plot(Area.Ux(), Area.Uz(), Area.Plot())) {
                Pieces.put(Piece.Pos(), Piece.Piece());
            }
        }
        List<String> Lines = new ArrayList<>();
        Lines.add("{\"site\":\"" + Scp3008Plan.Departments[Area.Department()] + "\",\"ux\":" + Area.Ux() + ",\"uz\":" + Area.Uz() + ",\"plot\":" + Area.Plot().Type()
                + ",\"start\":" + Area.Plot().Start() + ",\"length\":" + Area.Plot().Length() + "}");
        for (BlockPos Pos : BlockPos.betweenClosed(Area.minX(), Scp3008Plan.FloorY, Area.minZ(), Area.maxX(), Top, Area.maxZ())) {
            BlockState State = World.getBlockState(Pos);
            if (State.isAir()) {
                continue;
            }
            Integer Piece = Pieces.get(Pos);
            Lines.add("{\"x\":" + Pos.getX() + ",\"y\":" + Pos.getY() + ",\"z\":" + Pos.getZ() + ",\"state\":\"" + BlockStateParser.serialize(State) + "\""
                    + (Piece == null ? "" : ",\"piece\":" + Piece) + "}");
        }
        return Lines;
    }

    private static boolean named(BlockState State, String Suffix) {
        return Scp3008Catalogue.endsWith(State.getBlock(), Suffix);
    }

    private static boolean has(List<Scp3008Showrooms.Placement> Placed, String Suffix) {
        return Placed.stream().anyMatch(Piece -> named(Piece.State(), Suffix));
    }

    private static boolean near(List<Scp3008Showrooms.Placement> Placed, String Anchor, String Partner) {
        for (Scp3008Showrooms.Placement First : Placed) {
            if (!named(First.State(), Anchor)) {
                continue;
            }
            for (Scp3008Showrooms.Placement Second : Placed) {
                if (named(Second.State(), Partner) && First.Pos().getY() == Second.Pos().getY() && First.Pos().distManhattan(Second.Pos()) <= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void expect(GameTestHelper Helper, List<Scp3008Showrooms.Placement> Placed, String Room, String... Suffixes) {
        for (String Suffix : Suffixes) {
            if (!Scp3008Catalogue.matching(-1, Suffix).isEmpty()) {
                Helper.assertTrue(has(Placed, Suffix), Room + " has no " + Suffix);
            }
        }
    }

    private static void pair(GameTestHelper Helper, List<Scp3008Showrooms.Placement> Placed, String Room, String Anchor, String Partner) {
        if (has(Placed, Anchor) && !Scp3008Catalogue.matching(-1, Partner).isEmpty()) {
            Helper.assertTrue(near(Placed, Anchor, Partner), Room + " has a " + Anchor + " without a " + Partner + " beside it");
        }
    }

    public static void check(GameTestHelper Helper, ServerLevel World, Site Area) {
        if (Area.Plot().Type() != Scp3008Plan.RoomPlot) {
            return;
        }
        String Room = Scp3008Plan.Departments[Area.Department()] + " room at unit " + Area.Ux() + ", " + Area.Uz();
        List<Scp3008Showrooms.Placement> Placed = Scp3008Showrooms.plot(Area.Ux(), Area.Uz(), Area.Plot());
        Helper.assertTrue(Placed.size() >= 12, Room + " only holds " + Placed.size() + " blocks");
        Map<Integer, Integer> Parts = new HashMap<>();
        Map<Integer, BlockState> Mains = new HashMap<>();
        Map<BlockPos, Scp3008Showrooms.Placement> At = new HashMap<>();
        for (Scp3008Showrooms.Placement Piece : Placed) {
            Helper.assertTrue(At.put(Piece.Pos(), Piece) == null, Room + " puts two pieces at " + Piece.Pos());
        }
        for (Scp3008Showrooms.Placement Piece : Placed) {
            BlockState There = World.getBlockState(Piece.Pos());
            Helper.assertTrue(There.equals(Piece.State()), Room + " lost " + Piece.State() + " at " + Piece.Pos() + ", found " + There);
            BlockState Updated = Block.updateFromNeighbourShapes(There, World, Piece.Pos());
            Helper.assertFalse(Updated.isAir(), Room + " piece " + There + " falls apart when its neighbours update");
            Parts.merge(Piece.Piece(), 1, Integer::sum);
            if (Piece.Main()) {
                Mains.put(Piece.Piece(), Piece.State());
            }
            BlockState Floor = World.getBlockState(Piece.Pos().atY(Scp3008Plan.FloorY));
            Helper.assertTrue(Floor.is(Scp3008Registry.Floor.get()), Room + " has no store floor under " + Piece.Pos());
            supported(Helper, World, Room, Piece, At);
            contained(Helper, World, Room, Piece, At);
        }
        rafters(Helper, World, Area, Room);
        for (Map.Entry<Integer, BlockState> Entry : Mains.entrySet()) {
            int Expected = Scp3008Footprint.parts(Entry.getValue()).size();
            Helper.assertTrue(Parts.get(Entry.getKey()) == Expected, Room + " placed " + Parts.get(Entry.getKey()) + " of " + Expected + " parts of " + Entry.getValue());
        }
        for (BlockPos Pos : BlockPos.betweenClosed(Area.minX(), Scp3008Plan.Stand, Area.minZ(), Area.maxX(), Scp3008Plan.Stand, Area.maxZ())) {
            Helper.assertFalse(World.getBlockState(Pos).is(BlockTags.WOOL_CARPETS), Room + " still lays loose carpet at " + Pos);
        }
        switch (Area.Department()) {
            case Scp3008Plan.Living -> {
                expect(Helper, Placed, Room, "sofa", "coffee_table", "armchair");
                pair(Helper, Placed, Room, "sofa", "side_table");
            }
            case Scp3008Plan.Dining -> {
                expect(Helper, Placed, Room, "dining_table");
                pair(Helper, Placed, Room, "dining_table", "dining_chair");
            }
            case Scp3008Plan.Kitchen -> expect(Helper, Placed, Room, "sink", "stove", "base_cabinet", "wall_cabinet");
            case Scp3008Plan.Bedroom -> pair(Helper, Placed, Room, "bed", "nightstand");
            case Scp3008Plan.Bathroom -> expect(Helper, Placed, Room, "bathtub", "toilet", "vanity");
            case Scp3008Plan.Office -> {
                expect(Helper, Placed, Room, "desk", "office_chair", "desk_lamp");
                pair(Helper, Placed, Room, "desk", "office_chair");
            }
            case Scp3008Plan.Children -> {
                expect(Helper, Placed, Room, "bunk_bed", "play_table");
                pair(Helper, Placed, Room, "play_table", "children_chair");
            }
            case Scp3008Plan.Lighting -> expect(Helper, Placed, Room, "floor_lamp", "pendant_lamp");
            case Scp3008Plan.Textiles -> expect(Helper, Placed, Room, "rug_rolls", "cushion_bin");
            case Scp3008Plan.Outdoor -> pair(Helper, Placed, Room, "garden_table", "garden_chair");
            case Scp3008Plan.Electronics -> {
                expect(Helper, Placed, Room, "tv_wall", "tv_bench", "phone_table");
                pair(Helper, Placed, Room, "computer_desk", "gaming_chair");
            }
            default -> expect(Helper, Placed, Room, "shelf_tall", "pallet_stack");
        }
    }

    private static void supported(GameTestHelper Helper, ServerLevel World, String Room, Scp3008Showrooms.Placement Piece,
                                  Map<BlockPos, Scp3008Showrooms.Placement> At) {
        BlockState There = Piece.State();
        BlockPos Pos = Piece.Pos();
        if (There.is(Scp3008Registry.CeilingHung)) {
            BlockState Above = World.getBlockState(Pos.above());
            Helper.assertTrue(Above.is(Scp3008Registry.Rafter.get()), Room + " hangs " + There + " at " + Pos + " with no beam above it");
            return;
        }
        if (There.is(Scp3008Registry.WallMounted) || There.is(Scp3008Registry.WallFrame.get())) {
            Direction Facing = Scp3008Footprint.facingOf(There);
            Helper.assertTrue(World.getBlockState(Pos.relative(Facing.getOpposite())).is(Scp3008Registry.DisplayWall.get()),
                    Room + " has a wall piece " + There + " with no wall behind it at " + Pos);
            return;
        }
        BlockPos Below = Pos.below();
        if (Below.getY() == Scp3008Plan.FloorY) {
            Helper.assertTrue(World.getBlockState(Below).is(Scp3008Registry.Floor.get()), Room + " stands " + There + " on " + World.getBlockState(Below));
            return;
        }
        Scp3008Showrooms.Placement Under = At.get(Below);
        Helper.assertTrue(Under != null, Room + " has " + There + " floating at " + Pos + " over " + World.getBlockState(Below));
        if (Under.Piece() != Piece.Piece()) {
            double Top = Under.State().getCollisionShape(World, Below).max(Direction.Axis.Y);
            Helper.assertTrue(Top >= Scp3008Showrooms.LowMin, Room + " stands " + There + " on " + Under.State() + " which has no top to hold it");
        }
    }

    private static void contained(GameTestHelper Helper, ServerLevel World, String Room, Scp3008Showrooms.Placement Piece,
                                  Map<BlockPos, Scp3008Showrooms.Placement> At) {
        VoxelShape Shape = Piece.State().getShape(World, Piece.Pos());
        if (Shape.isEmpty()) {
            return;
        }
        AABB Box = Shape.bounds();
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            double Reach = switch (Side) {
                case NORTH -> -Box.minZ;
                case SOUTH -> Box.maxZ - 1.0;
                case WEST -> -Box.minX;
                default -> Box.maxX - 1.0;
            };
            if (Reach <= 0.05) {
                continue;
            }
            BlockPos Next = Piece.Pos().relative(Side);
            Scp3008Showrooms.Placement Neighbour = At.get(Next);
            Helper.assertTrue(Neighbour == null ? World.getBlockState(Next).isAir() : Neighbour.Piece() == Piece.Piece(),
                    Room + " has " + Piece.State() + " reaching into " + World.getBlockState(Next) + " at " + Next);
        }
    }

    private static boolean onWall(ServerLevel World, BlockPos Pos) {
        return World.getBlockState(Pos.below()).is(Scp3008Registry.DisplayWall.get());
    }

    private static boolean intoWall(ServerLevel World, BlockPos Pos, BlockState State) {
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (State.getValue(Scp3008RafterBlock.arm(Side)) && World.getBlockState(Pos.relative(Side)).is(Scp3008Registry.DisplayWall.get())) {
                return true;
            }
        }
        return false;
    }

    public static void rafters(GameTestHelper Helper, ServerLevel World, Site Area, String Room) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Pos : BlockPos.betweenClosed(Area.minX(), Scp3008Painter.RafterY, Area.minZ() + 1, Area.maxX(), Scp3008Painter.RafterY, Area.maxZ() - 1)) {
            if (World.getBlockState(Pos).is(Scp3008Registry.Rafter.get())) {
                Found.add(Pos.immutable());
            }
        }
        Helper.assertTrue(Found.size() >= 4 * (Scp3008Painter.WallBack - Scp3008Painter.WallFront + 1), Room + " only has " + Found.size() + " rafters");
        Deque<BlockPos> Queue = new ArrayDeque<>();
        Set<BlockPos> Reached = new HashSet<>();
        for (BlockPos Pos : Found) {
            BlockState State = World.getBlockState(Pos);
            Helper.assertTrue(Block.updateFromNeighbourShapes(State, World, Pos).equals(State), Room + " has a rafter at " + Pos + " that changes shape when its neighbours update");
            Helper.assertTrue(Scp3008RafterBlock.arms(State) != 0, Room + " has a loose rafter at " + Pos);
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                BlockState Next = World.getBlockState(Pos.relative(Side));
                if (State.getValue(Scp3008RafterBlock.arm(Side))) {
                    Helper.assertTrue(Scp3008RafterBlock.reaches(Next, Side.getOpposite()) || Next.is(Scp3008Registry.DisplayWall.get()),
                            Room + " has a rafter at " + Pos + " reaching " + Side.getName() + " into " + Next);
                } else {
                    Helper.assertFalse(Scp3008RafterBlock.reaches(Next, Side.getOpposite()), Room + " has a rafter at " + Pos.relative(Side) + " meeting a gap");
                }
            }
            boolean Resting = onWall(World, Pos) || intoWall(World, Pos, State);
            for (Direction.Axis Axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                Direction Low = Direction.fromAxisAndDirection(Axis, Direction.AxisDirection.NEGATIVE);
                boolean Ends = State.getValue(Scp3008RafterBlock.arm(Low)) != State.getValue(Scp3008RafterBlock.arm(Low.getOpposite()));
                Direction Across = Low.getClockWise();
                boolean Junction = State.getValue(Scp3008RafterBlock.arm(Across)) || State.getValue(Scp3008RafterBlock.arm(Across.getOpposite()));
                Helper.assertTrue(!Ends || Resting || Junction, Room + " has a beam ending in mid-air at " + Pos);
            }
            if (Resting) {
                Queue.add(Pos);
                Reached.add(Pos);
            }
        }
        while (!Queue.isEmpty()) {
            BlockPos Pos = Queue.poll();
            BlockState State = World.getBlockState(Pos);
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                BlockPos Next = Pos.relative(Side);
                if (State.getValue(Scp3008RafterBlock.arm(Side)) && World.getBlockState(Next).is(Scp3008Registry.Rafter.get()) && Reached.add(Next)) {
                    Queue.add(Next);
                }
            }
        }
        Helper.assertTrue(Reached.size() == Found.size(), Room + " has " + (Found.size() - Reached.size()) + " rafters that never reach a wall");
    }

    @GameTest(template = Scp3008GameTests.Arena, timeoutTicks = 2400, batch = "scp3008_core_showrooms")
    public static void showroomsAreComplete(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<String> Lines = new ArrayList<>();
        List<Site> Found = sites();
        Helper.assertTrue(Found.size() == Scp3008Plan.Departments.length, "only " + Found.size() + " departments have a showroom nearby");
        for (Site Area : Found) {
            build(World, Area);
            check(Helper, World, Area);
            Lines.addAll(scan(World, Area));
        }
        dump(Helper, DumpVariable, Lines);
        Helper.succeed();
    }

    private static void dump(GameTestHelper Helper, String Variable, List<String> Lines) {
        String Target = System.getenv(Variable);
        if (Target != null && !Target.isBlank()) {
            try {
                Files.write(Path.of(Target), Lines, StandardCharsets.UTF_8);
            } catch (IOException Problem) {
                Helper.fail("could not write the preview dump: " + Problem.getMessage());
            }
        }
    }

    private static boolean box(BlockState State) {
        return State.is(Scp3008Registry.Crate.get()) || State.is(Scp3008Registry.Tray.get()) || State.is(Scp3008Registry.Shelf.get());
    }

    private static boolean standing(BlockState State) {
        return State.is(Scp3008Registry.FloorStanding) && !State.is(Scp3008Registry.WallMounted) && !State.is(Scp3008Registry.CeilingHung);
    }

    public static int boxes(GameTestHelper Helper, ServerLevel World, Site Area) {
        String Name = "plot " + Area.Plot().Type() + " at unit " + Area.Ux() + ", " + Area.Uz();
        int Checked = 0;
        for (BlockPos Pos : BlockPos.betweenClosed(Area.minX(), Scp3008Plan.Stand, Area.minZ(), Area.maxX(), HighY, Area.maxZ())) {
            BlockState State = World.getBlockState(Pos);
            if (standing(State) && !box(State)) {
                Checked++;
                BlockPos Below = Pos.below();
                BlockState Under = World.getBlockState(Below);
                if (Below.getY() == Scp3008Plan.FloorY) {
                    Helper.assertTrue(Under.is(Scp3008Registry.Floor.get()), Name + " stands " + State + " on " + Under + " at " + Pos);
                } else if (!Under.is(State.getBlock())) {
                    VoxelShape Holder = Under.getCollisionShape(World, Below);
                    Helper.assertTrue(!Holder.isEmpty() && Holder.max(Direction.Axis.Y) >= Scp3008Showrooms.LowMin, Name + " has " + State + " floating over " + Under + " at " + Pos);
                }
                continue;
            }
            if (!box(State)) {
                continue;
            }
            Checked++;
            VoxelShape Own = State.getCollisionShape(World, Pos);
            Helper.assertTrue(!Own.isEmpty() && Own.min(Direction.Axis.Y) < 0.001, Name + " has " + State + " hovering inside its block at " + Pos);
            BlockPos Below = Pos.below();
            BlockState Under = World.getBlockState(Below);
            if (Below.getY() == Scp3008Plan.FloorY) {
                Helper.assertTrue(Under.is(Scp3008Registry.Floor.get()), Name + " stands " + State + " on " + Under + " at " + Pos);
                continue;
            }
            VoxelShape Holder = Under.getCollisionShape(World, Below);
            Helper.assertTrue(!Holder.isEmpty() && Holder.max(Direction.Axis.Y) > 0.999, Name + " has " + State + " floating over " + Under + " at " + Pos);
            AABB Foot = Own.bounds();
            AABB Top = Holder.bounds();
            Helper.assertTrue(Top.minX <= Foot.minX + Overhang && Top.maxX >= Foot.maxX - Overhang && Top.minZ <= Foot.minZ + Overhang && Top.maxZ >= Foot.maxZ - Overhang,
                    Name + " has " + State + " hanging over the edge of " + Under + " at " + Pos);
        }
        return Checked;
    }

    @GameTest(template = Scp3008GameTests.Arena, batch = "scp3008_core_rafters")
    public static void raftersJoinLikeBeams(GameTestHelper Helper) {
        Scp3008RafterBlock Rafter = Scp3008Registry.Rafter.get();
        int Along = Scp3008RafterBlock.EastArm | Scp3008RafterBlock.WestArm;
        BlockPos First = new BlockPos(1, 2, 1);
        BlockPos Second = First.east();
        BlockPos Beside = First.south();
        Helper.setBlock(First, Rafter.withArms(Along));
        Helper.setBlock(Second, Rafter.withArms(Along));
        Helper.setBlock(Beside, Rafter.withArms(Along));
        Helper.assertTrue(Helper.getBlockState(First).equals(Rafter.withArms(Along)), "a straight rafter changed when a parallel rafter was set beside it");
        Helper.setBlock(Beside, Rafter.withArms(Along | Scp3008RafterBlock.NorthArm));
        Helper.assertTrue(Helper.getBlockState(First).getValue(Scp3008RafterBlock.South), "a rafter did not join a beam reaching into it");
        Helper.setBlock(Second, Blocks.AIR.defaultBlockState());
        Helper.assertFalse(Helper.getBlockState(First).getValue(Scp3008RafterBlock.East), "a rafter kept reaching into a removed beam");
        Helper.setBlock(Second, Scp3008Registry.DisplayWall.get().defaultBlockState());
        Helper.setBlock(First, Rafter.withArms(Scp3008RafterBlock.EastArm | Scp3008RafterBlock.SouthArm));
        Helper.setBlock(First.below(), Blocks.STONE.defaultBlockState());
        Helper.assertTrue(Helper.getBlockState(First).getValue(Scp3008RafterBlock.East), "a rafter let go of the wall it rests in");
        Helper.assertTrue(Helper.getBlockState(First).getShape(Helper.getLevel(), Helper.absolutePos(First)).max(Direction.Axis.Y) <= Scp3008RafterBlock.Thickness / 16.0 + 0.001,
                "a rafter does not sit at the bottom of its block");
        Helper.succeed();
    }

    @GameTest(template = Scp3008GameTests.Arena, timeoutTicks = 2400, batch = "scp3008_core_supports")
    public static void boxesRestOnSupports(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<Site> Found = new ArrayList<>();
        for (int Type : BoxPlots) {
            Site Next = anyPlot(Type);
            Helper.assertTrue(Next != null, "no plot of type " + Type + " nearby");
            Found.add(Next);
        }
        List<String> Lines = new ArrayList<>();
        for (Site Area : Found) {
            build(World, Area);
            Lines.addAll(scan(World, Area));
        }
        dump(Helper, BoxDumpVariable, Lines);
        int Checked = 0;
        for (Site Area : Found) {
            build(World, Area);
            Checked += boxes(Helper, World, Area);
        }
        Helper.assertTrue(Checked >= 40, "only " + Checked + " boxes were checked");
        Helper.succeed();
    }
}
