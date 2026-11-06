package net.rai.scp_unleashed.scp3008.world;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.block.Scp3008FixtureBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008ShapedBlock;
import net.rai.scp_unleashed.scp3008.furniture.electronics.Scp3008ElectronicsShowroom;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Hash;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Painter;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp3008Showrooms {
    public static final int Depth = Scp3008Painter.RoomDepth;
    public static final int Height = 4;
    public static final double LowMin = 0.7;
    public static final double LowMax = 0.86;
    public static final double FullTop = 0.95;
    private static final Logger Log = LogUtils.getLogger();
    private static final Set<String> Reported = ConcurrentHashMap.newKeySet();
    public static final int Kept = 512;
    private static final Map<String, List<Placement>> Cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<Placement>> Eldest) {
            return size() > Kept;
        }
    };

    public record Placement(BlockPos Pos, BlockState State, boolean Main, int Piece) {
    }

    private Scp3008Showrooms() {
    }

    public static void forget() {
        synchronized (Cache) {
            Cache.clear();
        }
    }

    public static List<Placement> plot(int Ux, int Uz, Scp3008Plan.Plot Next) {
        String Key = Ux + ":" + Uz + ":" + Next.Start();
        synchronized (Cache) {
            List<Placement> Found = Cache.get(Key);
            if (Found != null) {
                return Found;
            }
        }
        List<Placement> Made = new ArrayList<>(compose(Ux, Uz, Next, true));
        Made.addAll(compose(Ux, Uz, Next, false));
        List<Placement> Composed = List.copyOf(Made);
        synchronized (Cache) {
            Cache.put(Key, Composed);
        }
        return Composed;
    }

    public static List<Placement> compose(int Ux, int Uz, Scp3008Plan.Plot Next, boolean West) {
        Room Made = new Room(Ux, Uz, Next, West);
        Made.furnish();
        Made.dress();
        return Made.Placed;
    }

    private static void report(int Department, String Missing) {
        if (Reported.add(Department + ":" + Missing)) {
            Log.info("SCP-3008 showrooms in {} have no piece called {}", Scp3008Plan.Departments[Department], Missing);
        }
    }

    public static final class Room {
        public final int Department;
        public final int Width;
        public final int Middle;
        public final boolean West;
        public final Direction Front;
        public final Direction Back;
        public final Direction Right;
        public final Direction Left;
        public final List<Placement> Placed = new ArrayList<>();
        private final int X0;
        private final int Z0;
        private final int Start;
        private final int Salt;
        private final boolean[][][] Taken;
        private final double[][] Surface;
        private int Pieces;
        private int[] Last = new int[4];

        public Room(int Ux, int Uz, Scp3008Plan.Plot Next, boolean West) {
            this.Department = Scp3008Plan.cachedDepartment(Ux, Uz);
            this.Width = Next.Length() - 2;
            this.Middle = Width / 2;
            this.West = West;
            this.Front = West ? Direction.WEST : Direction.EAST;
            this.Back = Front.getOpposite();
            this.Right = Direction.SOUTH;
            this.Left = Direction.NORTH;
            this.X0 = Scp3008Plan.originX(Ux);
            this.Z0 = Scp3008Plan.originZ(Uz);
            this.Start = Next.Start();
            this.Salt = Scp3008Hash.mix(Next.Salt(), West ? 1 : 2, 3008);
            this.Taken = new boolean[Depth][Width][Height];
            this.Surface = new double[Depth][Width];
            this.Pieces = West ? 0 : 1000;
        }

        public BlockPos world(int D, int W, int H) {
            return new BlockPos(X0 + Scp3008Painter.roomX(West, D), Scp3008Plan.Stand + H, Z0 + Start + 1 + W);
        }

        private int[] local(BlockPos Pos) {
            int Dx = Pos.getX() - X0;
            int D = West ? Dx - Scp3008Plan.Lane : Scp3008Plan.Unit - 1 - Dx;
            return new int[]{D, Pos.getZ() - Z0 - Start - 1, Pos.getY() - Scp3008Plan.Stand};
        }

        private boolean inside(int D, int W, int H) {
            return D >= 0 && D < Depth && W >= 0 && W < Width && H >= 0 && H < Height;
        }

        public boolean free(int D, int W, int H) {
            return inside(D, W, H) && !Taken[D][W][H];
        }

        public boolean wallBehind(int D, int W, Direction Facing) {
            Direction Behind = Facing.getOpposite();
            if (Behind == Back) {
                return D == Depth - 1;
            }
            if (Behind == Left) {
                return W == 0 && D >= 5;
            }
            if (Behind == Right) {
                return W == Width - 1 && D >= 5;
            }
            return false;
        }

        private int salt(int Index) {
            return Scp3008Hash.mix(Salt, Index, 41);
        }

        public Block pick(int Index, String... Suffixes) {
            Block Found = Scp3008Catalogue.pick(Department, salt(Index), Suffixes);
            if (Found == null) {
                report(Department, Suffixes[0]);
            }
            return Found;
        }

        private BlockState faced(BlockState State, Direction Facing) {
            return Scp3008Furnishing.face(State, Facing);
        }

        public boolean put(Block Piece, int D, int W, int H, Direction Facing) {
            return Piece != null && place(faced(Piece.defaultBlockState(), Facing), D, W, H, 0.0);
        }

        public boolean put(BlockState State, int D, int W, int H) {
            return place(State, D, W, H, 0.0);
        }

        private boolean place(BlockState State, int D, int W, int H, double Top) {
            BlockPos Main = world(D, W, H);
            List<Scp3008Footprint.Part> Parts = Scp3008Footprint.parts(State);
            int MinD = Integer.MAX_VALUE;
            int MaxD = Integer.MIN_VALUE;
            int MinW = Integer.MAX_VALUE;
            int MaxW = Integer.MIN_VALUE;
            for (Scp3008Footprint.Part Next : Parts) {
                int[] Cell = local(Next.at(Main));
                if (!free(Cell[0], Cell[1], Cell[2])) {
                    return false;
                }
                MinD = Math.min(MinD, Cell[0]);
                MaxD = Math.max(MaxD, Cell[0]);
                MinW = Math.min(MinW, Cell[1]);
                MaxW = Math.max(MaxW, Cell[1]);
            }
            int Index = ++Pieces;
            boolean First = true;
            for (Scp3008Footprint.Part Next : Parts) {
                BlockPos Pos = Next.at(Main);
                int[] Cell = local(Pos);
                Taken[Cell[0]][Cell[1]][Cell[2]] = true;
                BlockState Made = Next.State();
                if (First && Top > 0.0) {
                    Made = lowered(Made, Top);
                }
                Placed.add(new Placement(Pos.immutable(), Made, First, Index));
                if (Cell[2] == 0) {
                    Surface[Cell[0]][Cell[1]] = topOf(Made);
                }
                First = false;
            }
            Last = new int[]{MinD, MaxD, MinW, MaxW};
            return true;
        }

        private static double topOf(BlockState State) {
            try {
                VoxelShape Shape = State.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                return Shape.isEmpty() ? 0.0 : Shape.max(Direction.Axis.Y);
            } catch (RuntimeException Problem) {
                return 0.0;
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static BlockState lowered(BlockState State, double Top) {
            for (Property<?> Candidate : State.getProperties()) {
                if (Candidate.getName().equals("lowered") && Candidate instanceof BooleanProperty Flag) {
                    return State.setValue((Property) Flag, (Comparable) (Top > LowMin && Top < LowMax));
                }
            }
            return State;
        }

        public boolean onTop(Block Piece, int D, int W, Direction Facing) {
            if (Piece == null || !inside(D, W, 1) || Taken[D][W][1] || !Taken[D][W][0]) {
                return false;
            }
            double Top = Surface[D][W];
            BlockState State = faced(Piece.defaultBlockState(), Facing);
            boolean Lowers = false;
            for (Property<?> Candidate : State.getProperties()) {
                Lowers |= Candidate.getName().equals("lowered");
            }
            if (!(Top >= FullTop || Lowers && Top > LowMin && Top < LowMax)) {
                return false;
            }
            return place(State, D, W, 1, Top);
        }

        public boolean wall(Block Piece, int D, int W, int H, Direction Facing) {
            return Piece != null && wallBehind(D, W, Facing) && put(Piece, D, W, H, Facing);
        }

        public int[] last() {
            return Last;
        }

        public void furnish() {
            switch (Department) {
                case Scp3008Plan.Living -> living();
                case Scp3008Plan.Dining -> dining();
                case Scp3008Plan.Kitchen -> kitchen();
                case Scp3008Plan.Bedroom -> bedroom();
                case Scp3008Plan.Bathroom -> bathroom();
                case Scp3008Plan.Office -> office();
                case Scp3008Plan.Children -> children();
                case Scp3008Plan.Lighting -> lighting();
                case Scp3008Plan.Textiles -> textiles();
                case Scp3008Plan.Outdoor -> outdoor();
                case Scp3008Plan.Electronics -> Scp3008ElectronicsShowroom.furnish(this);
                default -> warehouse();
            }
        }

        private void pendant(int W) {
            put(pick(90 + W, "pendant_lamp"), Scp3008Painter.CrossDepth, W, 3, Front);
        }

        private void lampOn(int Index, int D, int W) {
            String[] Order = Department == Scp3008Plan.Office || Department == Scp3008Plan.Bedroom ? new String[]{"desk_lamp", "table_lamp"}
                    : new String[]{"table_lamp", "desk_lamp"};
            for (String Suffix : Order) {
                Block Lamp = Scp3008Catalogue.pick(Department, salt(Index), Suffix);
                if (Lamp != null && onTop(Lamp, D, W, Front)) {
                    return;
                }
            }
        }

        private void living() {
            Block Sofa = pick(1, "sofa");
            if (!put(Sofa, 11, Middle, 0, Front)) {
                put(Sofa, 11, Middle + 1, 0, Front);
            }
            int[] Span = last();
            Block Side = pick(2, "side_table");
            put(Side, 11, Span[2] - 1, 0, Front);
            put(Side, 11, Span[3] + 1, 0, Front);
            put(pick(4, "coffee_table"), 8, Middle, 0, Front);
            Block Chair = pick(5, "armchair");
            put(Chair, 8, Middle - 3, 0, Right);
            put(Chair, 8, Middle + 3, 0, Left);
            put(pick(6, "bookcase", "cube_shelf"), 13, 0, 0, Front);
            put(pick(7, "tv_bench", "cube_shelf"), 13, Width - 1, 0, Front);
            if (!put(pick(8, "cube_shelf"), 13, Width - 2, 0, Front)) {
                put(pick(8, "cube_shelf"), 13, 1, 0, Front);
            }
            put(pick(9, "floor_lamp"), 11, Width - 1, 0, Front);
            corner(10);
            put(pick(13, "cube_shelf", "bookcase"), 6, 0, 0, Right);
            pendant(Middle);
        }

        private void corner(int Index) {
            Block Chair = pick(Index, "armchair");
            if (put(Chair, 3, 1, 0, Right)) {
                put(pick(Index + 1, "side_table"), 2, 1, 0, Front);
                put(pick(Index + 3, "floor_lamp"), 4, 0, 0, Right);
            }
        }

        private void diningSet(int Index, int D, int W, String TableSuffix, String ChairSuffix) {
            if (!put(pick(Index, TableSuffix), D, W, 0, Front)) {
                return;
            }
            int[] Span = last();
            Block Chair = pick(Index + 1, ChairSuffix, "chair");
            for (int Across = Span[2]; Across <= Span[3]; Across++) {
                put(Chair, D - 1, Across, 0, Back);
                put(Chair, D + 1, Across, 0, Front);
            }
        }

        private void dining() {
            Block Table = pick(1, "dining_table");
            if (!put(Table, 8, Middle, 0, Front)) {
                put(Table, 8, Middle + 1, 0, Front);
            }
            int[] Span = last();
            boolean Bench = Scp3008Hash.chance(salt(2), 0.35);
            Block Chair = pick(3, "dining_chair", "chair");
            Block Seat = Bench ? pick(4, "bench", "dining_chair") : Chair;
            for (int W = Span[2]; W <= Span[3]; W++) {
                put(Chair, 7, W, 0, Back);
                put(Seat, 9, W, 0, Front);
            }
            put(Chair, 8, Span[2] - 1, 0, Right);
            put(Chair, 8, Span[3] + 1, 0, Left);
            put(pick(5, "sideboard"), 13, Middle, 0, Front);
            put(pick(7, "display_cabinet"), 13, 1, 0, Front);
            put(pick(8, "display_cabinet", "sideboard"), 13, Width - 2, 0, Front);
            put(pick(9, "stool"), 12, Width - 1, 0, Left);
            diningSet(10, 4, Middle, "dining_table", "dining_chair");
            put(pick(12, "display_cabinet", "sideboard"), 6, 0, 0, Right);
            pendant(Middle);
        }

        private void kitchen() {
            put(pick(1, "fridge"), 13, 0, 0, Front);
            int Sink = Middle;
            int Stove = Math.min(Width - 2, Middle + 2);
            for (int W = 1; W < Width; W++) {
                Block Piece = W == Sink ? pick(2, "sink") : W == Stove ? pick(3, "stove") : pick(4, "base_cabinet");
                put(Piece, 13, W, 0, Front);
                if (W != Stove) {
                    wall(pick(5 + (W % 3 == 0 ? 1 : 0), W % 3 == 0 ? "wall_shelf" : "wall_cabinet", "wall_cabinet"), 13, W, 2, Front);
                }
            }
            for (int W = Middle - 1; W <= Middle + 1; W++) {
                put(pick(7, "island"), 9, W, 0, Front);
                put(pick(8, "stool"), 8, W, 0, Back);
            }
            put(pick(9, "cart"), 6, Width - 1, 0, Left);
            diningSet(10, 4, Middle, "dining_table", "dining_chair");
            pendant(Middle);
        }

        private void bedroom() {
            Block Bed = Scp3008Hash.chance(salt(1), 0.7) ? pick(1, "double_bed", "single_bed") : pick(1, "single_bed", "double_bed");
            if (!put(Bed, 12, Middle, 0, Front)) {
                put(Bed, 12, Middle + 1, 0, Front);
            }
            int[] Span = last();
            Block Stand0 = pick(2, "nightstand");
            int HeadD = Span[1];
            put(Stand0, HeadD, Span[2] - 1, 0, Front);
            put(Stand0, HeadD, Span[3] + 1, 0, Front);
            Block Wardrobe = pick(4, "wardrobe");
            if (!put(Wardrobe, 9, 0, 0, Right) && !put(Wardrobe, 7, 0, 0, Right)) {
                put(Wardrobe, 9, Width - 1, 0, Left);
            }
            put(pick(5, "chest_of_drawers"), 11, Width - 1, 0, Left);
            put(pick(6, "floor_mirror"), 6, Width - 1, 0, Left);
            put(pick(7, "clothes_rail"), 5, 0, 0, Right);
            put(pick(8, "bench"), Span[0] - 1, Middle, 0, Front);
            corner(10);
            pendant(Middle);
        }

        private void bathroom() {
            put(pick(1, "bathtub"), 12, 0, 0, Front);
            put(pick(2, "toilet"), 13, Math.min(3, Width - 1), 0, Front);
            int Basin = Math.min(Width - 3, Middle + 1);
            put(pick(3, "vanity"), 13, Basin, 0, Front);
            wall(pick(4, "mirror_cabinet"), 13, Basin, 1, Front);
            put(pick(5, "shower"), 13, Width - 1, 0, Front);
            put(pick(6, "towel_ladder"), 11, Width - 1, 0, Left);
            put(pick(7, "laundry_basket"), 9, 0, 0, Right);
            put(pick(8, "vanity"), 13, Basin + 1, 0, Front);
            if (put(pick(9, "vanity"), 7, 0, 0, Right)) {
                wall(pick(10, "mirror_cabinet"), 7, 0, 1, Right);
            }
            put(pick(11, "toilet"), 9, Width - 1, 0, Left);
            put(pick(12, "towel_ladder"), 6, Width - 1, 0, Left);
            put(pick(13, "laundry_basket"), 4, 0, 0, Right);
            pendant(Middle);
        }

        private void office() {
            Block Desk = pick(1, "desk");
            Block Chair = pick(2, "office_chair", "chair");
            for (int W : new int[]{2, Width - 2}) {
                if (put(Desk, 13, W, 0, Front)) {
                    put(Chair, 12, W, 0, Back);
                }
            }
            wall(pick(5, "whiteboard"), 13, Middle, 2, Front);
            put(pick(6, "filing_cabinet"), 13, Middle, 0, Front);
            if (put(pick(7, "drawer_unit"), 13, Middle - 1, 0, Front)) {
                lampOn(3, 13, Middle - 1);
            }
            Block Shelving = pick(8, "shelving");
            put(Shelving, 10, 0, 0, Right);
            put(Shelving, 10, Width - 1, 0, Left);
            if (put(Desk, 7, Middle, 0, Front)) {
                int[] Span = last();
                for (int W = Span[2]; W <= Span[3]; W++) {
                    put(Chair, 6, W, 0, Back);
                    put(Chair, 8, W, 0, Front);
                }
            }
            Block Filing = pick(9, "filing_cabinet");
            put(Filing, 6, 0, 0, Right);
            put(Filing, 5, 0, 0, Right);
            if (put(pick(10, "drawer_unit"), 6, Width - 1, 0, Left)) {
                lampOn(4, 6, Width - 1);
            }
            if (put(Desk, 3, 1, 0, Right)) {
                int[] Span = last();
                put(Chair, Span[0] + (Span[1] - Span[0]) / 2, 2, 0, Left);
            }
            pendant(Middle);
        }

        private void children() {
            put(pick(1, "bunk_bed"), 12, 0, 0, Front);
            put(pick(2, "toy_chest"), 13, 2, 0, Front);
            put(pick(3, "play_table"), 8, Middle, 0, Front);
            Block Chair = pick(4, "children_chair", "chair");
            put(Chair, 7, Middle, 0, Back);
            put(Chair, 9, Middle, 0, Front);
            put(Chair, 8, Middle - 1, 0, Right);
            put(Chair, 8, Middle + 1, 0, Left);
            put(pick(5, "rocking_elk"), 4, Width - 2, 0, Left);
            put(pick(6, "play_kitchen"), 13, Width - 1, 0, Front);
            put(pick(7, "toy_chest"), 11, Width - 1, 0, Left);
            put(pick(8, "bunk_bed"), 7, Width - 1, 0, Left);
            put(pick(9, "rocking_elk"), 4, 1, 0, Right);
            put(pick(10, "toy_chest"), 6, 0, 0, Right);
            pendant(Middle);
        }

        private void lighting() {
            wall(pick(1, "lamp_display"), 13, Middle, 1, Front);
            Block Floor = pick(2, "floor_lamp");
            put(Floor, 12, 1, 0, Front);
            put(Floor, 12, Width - 2, 0, Front);
            put(Floor, 9, 0, 0, Right);
            put(Floor, 9, Width - 1, 0, Left);
            Block Side = pick(3, "cube_shelf", "side_table");
            if (put(Side, 7, Middle - 2, 0, Front)) {
                lampOn(4, 7, Middle - 2);
            }
            if (put(Side, 7, Middle + 2, 0, Front)) {
                lampOn(5, 7, Middle + 2);
            }
            for (int W = 1; W < Width - 1; W += 2) {
                pendant(W);
            }
            for (int W = 1; W < Width - 1; W += 3) {
                put(Floor, 4, W, 0, Front);
            }
            for (int W = 2; W < Width - 1; W += 3) {
                if (put(Side, 10, W, 0, Front)) {
                    lampOn(20 + W, 10, W);
                }
            }
        }

        private void textiles() {
            Block Rolls = pick(1, "rug_rolls");
            for (int W = 0; W < 3; W++) {
                put(Rolls, 13, W, 0, Front);
            }
            Block Rack = pick(2, "curtain_rack");
            put(Rack, 13, Width - 1, 0, Front);
            put(Rack, 13, Width - 2, 0, Front);
            Block Bin = pick(3, "cushion_bin");
            put(Bin, 8, Middle - 1, 0, Front);
            put(Bin, 8, Middle + 1, 0, Front);
            Block Duvets = pick(4, "duvet_stack");
            put(Duvets, 10, 0, 0, Right);
            put(Duvets, 10, Width - 1, 0, Left);
            put(pick(5, "sofa"), 11, Middle, 0, Front);
            for (int D = 4; D <= 6; D += 2) {
                for (int W = 1; W < Width - 1; W += 2) {
                    put(W % 4 == 1 ? Bin : Duvets, D, W, 0, Front);
                }
            }
            for (int D = 6; D <= 12; D += 2) {
                put(Rolls, D, Width - 1, 0, Left);
            }
            pendant(Middle);
        }

        private void outdoor() {
            put(pick(1, "garden_table"), 8, Middle, 0, Front);
            Block Chair = pick(2, "garden_chair", "chair");
            put(Chair, 7, Middle, 0, Back);
            put(Chair, 9, Middle, 0, Front);
            put(Chair, 8, Middle - 1, 0, Right);
            put(Chair, 8, Middle + 1, 0, Left);
            put(pick(3, "parasol"), 10, Middle - 2, 0, Front);
            Block Pot = pick(4, "planter");
            put(Pot, 12, 0, 0, Front);
            put(Pot, 12, Width - 1, 0, Front);
            put(Pot, 13, Middle - 2, 0, Front);
            put(Pot, 13, Middle + 2, 0, Front);
            put(Scp3008Registry.Planter.get().defaultBlockState().setValue(Scp3008ShapedBlock.Variant, 3), 13, Middle, 0);
            put(Chair, 12, Middle - 1, 0, Front);
            put(Chair, 12, Middle + 1, 0, Front);
            diningSet(10, 4, Middle + 2, "garden_table", "garden_chair");
            put(pick(13, "parasol"), 3, 1, 0, Front);
        }

        private void warehouse() {
            Block Tall = pick(1, "shelf_tall");
            Block Short = pick(2, "shelf_short");
            for (int W = 0; W < Width; W++) {
                if (!put(W % 2 == 0 ? Tall : Short, 13, W, 0, Front)) {
                    put(Short, 13, W, 0, Front);
                }
            }
            Block Pallet = pick(3, "pallet_stack");
            put(Pallet, 9, 1, 0, Front);
            put(Pallet, 9, Width - 2, 0, Front);
            Block Packs = pick(4, "flatpack_stack");
            for (int W = Middle - 1; W <= Middle + 1; W++) {
                put(Packs, 10, W, 0, Front);
            }
            put(pick(5, "picking_trolley"), 6, Middle, 0, Front);
            put(pick(6, "aisle_sign"), 1, 0, 0, Front);
            for (int W = 1; W < Width - 1; W += 3) {
                put(Pallet, 4, W, 0, Front);
                put(Packs, 4, W + 1, 0, Front);
            }
            put(pick(7, "picking_trolley"), 7, Width - 2, 0, Left);
        }

        public void dress() {
            BlockState Plant = Scp3008Registry.Planter.get().defaultBlockState().setValue(Scp3008ShapedBlock.Variant, Scp3008Hash.pick(salt(70), 3));
            if (Department != Scp3008Plan.Warehouse) {
                put(Plant, 1, 0, 0);
                put(Scp3008Registry.Planter.get().defaultBlockState().setValue(Scp3008ShapedBlock.Variant, Scp3008Hash.pick(salt(71), 3)), 12, Width - 1, 0);
            }
            BlockState Tag = Scp3008Registry.PriceStand.get().defaultBlockState().setValue(Scp3008FixtureBlock.FACING, Front)
                    .setValue(Scp3008FixtureBlock.Variant, Scp3008Hash.pick(salt(72), 4));
            if (!put(Tag, 0, Width - 2, 0)) {
                put(Tag, 0, 1, 0);
            }
            put(Tag.setValue(Scp3008FixtureBlock.Variant, Scp3008Hash.pick(salt(73), 4)), 6, Middle + 1, 0);
            int Art = 0;
            for (int W : new int[]{Middle - 3, Middle + 3, Middle - 1, Middle + 1}) {
                if (Art < 2 && free(13, W, 2) && Department != Scp3008Plan.Warehouse && put(frame(Art), 13, W, 2)) {
                    Art++;
                }
            }
            for (int D : new int[]{10, 7}) {
                if (free(D, 0, 2) && Department != Scp3008Plan.Warehouse) {
                    put(frame(2).setValue(Scp3008FixtureBlock.FACING, Right), D, 0, 2);
                    break;
                }
            }
        }

        private BlockState frame(int Index) {
            return Scp3008Registry.WallFrame.get().defaultBlockState().setValue(Scp3008FixtureBlock.FACING, Front)
                    .setValue(Scp3008FixtureBlock.Variant, Scp3008Hash.pick(salt(80 + Index), 4));
        }
    }

}
