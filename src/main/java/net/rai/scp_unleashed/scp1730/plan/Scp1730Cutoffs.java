package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

public final class Scp1730Cutoffs {
    public static final String Type = "cutoff";
    public static final long Salt = 0x1730C077L;
    public static final String Lever = Scp1730Blocks.Mod + "door_lever";
    public static final String Button = Scp1730Blocks.Mod + "door_button";
    public static final List<String> Doors = List.of("containment_door", "sliding_door", "lcz_door", "hcz_door", "tall_blast_gate", "containment_tall_door");
    public static final int[] Widths = {3, 2, 2, 2, 2, 2};
    public static final int[] Travel = {40, 12, 12, 16, 41, 40};
    public static final int Height = 3;
    public static final int HallWidth = 3;
    public static final int[][] Choices = {{0, 0, 1, 1}, {0, 0, 2, 2}, {0, 0, 3, 4}, {0, 4, 5}};
    public static final int[][] Counts = {{2, 3}, {2, 4}, {3, 4}, {1, 2}};
    public static final int Gap = 14;
    public static final int QuickTravel = 20;
    public static final int QuickPulse = 3;
    public static final int SlowPulse = 4;
    public static final int StripeVariant = 4;
    public static final int ModeLevers = 0;
    public static final int ModeButtons = 1;
    public static final int ModeLeverFront = 2;
    public static final int ModeButtonFront = 3;
    public static final int LeverShare = 45;
    public static final int ButtonShare = 35;
    public static final int MixedShare = 10;
    public static final int RoomMargin = 2;
    public static final int PassageMargin = 6;
    public static final int OddMargin = 4;
    public static final int CriticalReach = 6;
    public static final int HallReach = 8;
    public static final int FixtureReach = 3;
    public static final int HazardReach = 5;
    public static final int BasinMargin = 2;
    public static final int RouteSlack = 12;
    public static final int DeepTries = 8;
    public static final Set<String> Critical = Set.of("pdevice", "puzzle", "pickup", "pseal", "dseal", "seal", "bioseal", "biocontrol", "control", "card");
    public static final Set<String> Hazards = Set.of("laser", "turret", "tesla", "crack", "symbol", "breach", "painter", "guardian", "thresher");
    public static final Set<String> PassageRooms = Set.of("stair_entry", "stair_exit", "elevator", "lobby");
    public static final Set<String> OddRooms = Set.of(Scp1730Sectors.BioWing, Scp1730Sectors.Reservoir, "sanctum", "pit", "tesla");
    public static final Set<String> Targets = Set.of("pdevice", "pickup", "puzzle", "loot", "setpiece", "safe", "card", "control", "biocontrol");
    public static final Set<String> Debris = Set.of(Scp1730Blocks.DragMarks, Scp1730Blocks.GlassShards, Scp1730Blocks.MouldPatch, Scp1730Blocks.PaperStack, Scp1730Blocks.Rubble,
            Scp1730Blocks.FallenPanel, Scp1730Blocks.CableTangle, Scp1730Blocks.OverturnedChair, Scp1730Blocks.DanglingPanel, Scp1730Blocks.HangingCables);
    private static final Map<Scp1730Plan, Gates> Known = Collections.synchronizedMap(new WeakHashMap<>());

    private Scp1730Cutoffs() {
    }

    record Frame(int Zone, int F, int Cx, int Cz, int Ax, int Az, int Lx, int Lz) {
        int x(int A, int Lat) {
            return Cx + Ax * A + Lx * Lat;
        }

        int z(int A, int Lat) {
            return Cz + Az * A + Lz * Lat;
        }

        Set<Long> plane() {
            Set<Long> Made = new HashSet<>();
            for (int Lat = -1; Lat <= 1; Lat++) {
                for (int Rise = 1; Rise <= Height; Rise++) {
                    Made.add(Scp1730Builder.key(x(0, Lat), F + Rise, z(0, Lat)));
                }
            }
            return Made;
        }
    }

    public record Cut(int Zone, int Kind, int Mode, boolean Open, boolean FillerBefore, boolean FrontHigh, boolean BackHigh, int X, int Y, int Z, int Front) {
        public int right() {
            return (Front + 3) % 4;
        }

        public int width() {
            return Widths[Kind];
        }

        public String name() {
            return Doors.get(Kind);
        }

        public int lo() {
            return FillerBefore ? -1 : 0;
        }

        public int[] cell(int Column, int Row, int Along) {
            int Right = right();
            return new int[]{X + Scp1730Layout.Dx[Right] * Column + Scp1730Layout.Dx[Front] * Along, Y + Row, Z + Scp1730Layout.Dz[Right] * Column + Scp1730Layout.Dz[Front] * Along};
        }

        public boolean door(int Column) {
            return Column >= 0 && Column < width();
        }

        public boolean filled() {
            return width() < HallWidth;
        }

        public int filler() {
            return FillerBefore ? -1 : width();
        }

        public int controlColumn(int Side) {
            if (!filled()) {
                return (Side > 0 ? FrontHigh : BackHigh) ? 2 : 0;
            }
            return FillerBefore ? 1 : 0;
        }

        public boolean lever(int Side) {
            return Mode == ModeLevers || Mode == ModeLeverFront && Side > 0 || Mode == ModeButtonFront && Side < 0;
        }

        public boolean levers() {
            return Mode != ModeButtons;
        }

        public int controlFacing(int Side) {
            return controlColumn(Side) == lo() ? right() : (right() + 2) % 4;
        }

        public int[] control(int Side) {
            return cell(controlColumn(Side), 1, Side);
        }

        public int[] support(int Side) {
            int Out = controlFacing(Side);
            int[] At = control(Side);
            return new int[]{At[0] - Scp1730Layout.Dx[Out], At[1], At[2] - Scp1730Layout.Dz[Out]};
        }

        public int side(int Px, int Pz) {
            return Integer.signum((Px - X) * Scp1730Layout.Dx[Front] + (Pz - Z) * Scp1730Layout.Dz[Front]);
        }

        public int pulse() {
            return Travel[Kind] <= QuickTravel ? QuickPulse : SlowPulse;
        }

        public String doorState(int Column, int Row) {
            return Scp1730Blocks.Mod + name() + "[facing=" + Scp1730Layout.Facings[Front] + ",open=" + Open + ",part=" + (Column + Row * width()) + "]";
        }

        public String controlState(int Side) {
            String Facing = Scp1730Layout.Facings[controlFacing(Side)];
            return lever(Side) ? Lever + "[face=wall,facing=" + Facing + ",powered=" + Open + "]" : Button + "[facing=" + Facing + ",powered=false,pulse=" + pulse() + "]";
        }

        public int data() {
            return Kind | Mode << 4 | (Open ? 1 : 0) << 6 | (FillerBefore ? 1 : 0) << 7 | (FrontHigh ? 1 : 0) << 8 | (BackHigh ? 1 : 0) << 9;
        }

        public Scp1730Fixture fixture() {
            return new Scp1730Fixture(Type, X, Y, Z, Scp1730Layout.Facings[Front], data(), Zone);
        }

        public int[] centre() {
            return cell(lo() + 1, 0, 0);
        }

        Frame frame() {
            int[] Middle = centre();
            int Right = right();
            return new Frame(Zone, Y - 1, Middle[0], Middle[2], Scp1730Layout.Dx[Front], Scp1730Layout.Dz[Front], Scp1730Layout.Dx[Right], Scp1730Layout.Dz[Right]);
        }

        public List<int[]> doorCells() {
            List<int[]> Made = new ArrayList<>();
            for (int Row = 0; Row < Height; Row++) {
                for (int Column = 0; Column < width(); Column++) {
                    Made.add(cell(Column, Row, 0));
                }
            }
            return Made;
        }

        public int[] stripe(int Side) {
            int[] Back = support(Side);
            return new int[]{Back[0], Back[1] + 1, Back[2]};
        }
    }

    public static Cut of(Scp1730Fixture Each) {
        int Data = Each.Data();
        return new Cut(Each.Zone(), Math.min(Data & 15, Doors.size() - 1), Data >> 4 & 3, (Data >> 6 & 1) == 1, (Data >> 7 & 1) == 1, (Data >> 8 & 1) == 1, (Data >> 9 & 1) == 1,
                Each.X(), Each.Y(), Each.Z(), Scp1730Builder.dirOf(Each.Facing()));
    }

    public static List<Cut> cuts(Scp1730Plan Plan) {
        List<Cut> Made = new ArrayList<>();
        for (Scp1730Fixture Each : Plan.fixtures(Type)) {
            Made.add(of(Each));
        }
        return Made;
    }

    public static Scp1730Blocks.Palette look(int Zone) {
        return switch (Zone) {
            case 0 -> Scp1730Blocks.Ez;
            case 1 -> Scp1730Blocks.Lcz;
            case 2 -> Scp1730Blocks.Hcz;
            default -> Scp1730Blocks.Containment;
        };
    }

    static boolean solid(String State) {
        return State != null && !Scp1730Blocks.passable(State) && !Scp1730Blocks.water(State);
    }

    static boolean floor(String State) {
        return solid(State) && !State.contains("stairs") && !State.contains("slab") && !State.contains("railing");
    }

    static boolean clearable(String State) {
        if (State == null) {
            return true;
        }
        int Bracket = State.indexOf('[');
        return Debris.contains(Bracket < 0 ? State : State.substring(0, Bracket));
    }

    static boolean signLike(String State) {
        return State != null && (State.contains("sign") || State.contains("plaque"));
    }

    static void place(Scp1730Builder B) {
        if (B.Fallback) {
            return;
        }
        Random Pick = new Random(Scp1730Builder.mix(B.Seed ^ Salt, 23));
        for (int Zone = 0; Zone < 4; Zone++) {
            zone(B, Zone, Pick);
        }
    }

    static void zone(Scp1730Builder B, int Zone, Random Pick) {
        int F = Scp1730Builder.Floors[Zone];
        Scp1730Blocks.Palette Look = look(Zone);
        Scp1730RoomDressing.Cells G = B.Grid::get;
        List<Scp1730Fixture> Marks = relevant(B.Fixtures, Zone);
        List<Frame> Options = new ArrayList<>();
        for (int X = 3; X < Scp1730Plan.Width - 3; X++) {
            for (int Z = 3; Z < Scp1730Plan.Depth - 3; Z++) {
                if (!clearable(G.at(X, F + 1, Z)) || !floor(G.at(X, F, Z))) {
                    continue;
                }
                for (int Axis = 0; Axis < 2; Axis++) {
                    Frame Each = Axis == 0 ? new Frame(Zone, F, X, Z, 1, 0, 0, 1) : new Frame(Zone, F, X, Z, 0, 1, 1, 0);
                    if (fits(G, Each, Look) && !B.nearStairs(Zone, X, Z) && forbidden(G, Marks, B.RoomInfos, B.Basins, B.Pits, Each, Set.of()) == null) {
                        Options.add(Each);
                    }
                }
            }
        }
        if (Options.isEmpty()) {
            return;
        }
        Collections.shuffle(Options, Pick);
        int Wanted = Counts[Zone][0] + Pick.nextInt(Counts[Zone][1] - Counts[Zone][0] + 1);
        Route Timed = timed(G, B.Fixtures, Zone, F);
        Deep Way = Zone == Scp1730Puzzles.DeepZone ? deep(G, B.Fixtures, B.Entries[Zone], Zone, F) : null;
        List<Frame> Chosen = new ArrayList<>();
        int Tries = 0;
        for (Frame Each : Options) {
            if (Chosen.size() >= Wanted || Way != null && Tries >= DeepTries) {
                break;
            }
            if (crowded(Each, Chosen) || Timed != null && Timed.crosses(Each)) {
                continue;
            }
            if (Zone == Scp1730Puzzles.DeepZone) {
                if (Way == null) {
                    break;
                }
                Tries++;
                if (!Way.clear(Each.plane())) {
                    continue;
                }
            }
            Chosen.add(Each);
            build(B, decide(Each, Zone, Pick));
        }
    }

    static boolean crowded(Frame Each, List<Frame> Chosen) {
        for (Frame Other : Chosen) {
            if (Math.max(Math.abs(Other.Cx() - Each.Cx()), Math.abs(Other.Cz() - Each.Cz())) < Gap) {
                return true;
            }
        }
        return false;
    }

    static Cut decide(Frame Each, int Zone, Random Pick) {
        int[] Options = Choices[Zone];
        int Kind = Options[Pick.nextInt(Options.length)];
        int Roll = Pick.nextInt(100);
        int Mode = Roll < LeverShare ? ModeLevers : Roll < LeverShare + ButtonShare ? ModeButtons : Roll < LeverShare + ButtonShare + MixedShare ? ModeLeverFront : ModeButtonFront;
        boolean Open = Mode != ModeButtons && Pick.nextBoolean();
        boolean Flip = Pick.nextBoolean();
        int Front = Scp1730Builder.dirOf(Scp1730Builder.facingOf(Flip ? -Each.Ax() : Each.Ax(), Flip ? -Each.Az() : Each.Az()));
        boolean Narrow = Widths[Kind] < HallWidth;
        boolean FillerBefore = Narrow && Pick.nextBoolean();
        boolean FrontHigh = Pick.nextBoolean();
        boolean BackHigh = Pick.nextBoolean();
        int Right = (Front + 3) % 4;
        int Shift = FillerBefore ? 0 : -1;
        return new Cut(Zone, Kind, Mode, Open, FillerBefore, !Narrow && FrontHigh, !Narrow && BackHigh, Each.Cx() + Scp1730Layout.Dx[Right] * Shift, Each.F() + 1,
                Each.Cz() + Scp1730Layout.Dz[Right] * Shift, Front);
    }

    static void build(Scp1730Builder B, Cut Made) {
        Scp1730Blocks.Palette Look = look(Made.Zone());
        Frame Each = Made.frame();
        for (int A = -2; A <= 2; A++) {
            for (int Lat = -1; Lat <= 1; Lat++) {
                for (int Rise = 1; Rise <= Height; Rise++) {
                    if (Math.abs(A) <= 1 || Rise < Height) {
                        B.Grid.set(Each.x(A, Lat), Each.F() + Rise, Each.z(A, Lat), null);
                    }
                }
            }
        }
        for (int Row = 0; Row < Height; Row++) {
            for (int Column = 0; Column < Made.width(); Column++) {
                int[] At = Made.cell(Column, Row, 0);
                B.Grid.set(At[0], At[1], At[2], Made.doorState(Column, Row));
            }
            if (Made.filled()) {
                int[] At = Made.cell(Made.filler(), Row, 0);
                B.Grid.set(At[0], At[1], At[2], Look.wall(Row + 1, Scp1730Builder.CorridorHeight));
            }
        }
        for (int Side = -1; Side <= 1; Side += 2) {
            int[] At = Made.control(Side);
            B.Grid.set(At[0], At[1], At[2], Made.controlState(Side));
            int[] Stripe = Made.stripe(Side);
            B.decalExact(Stripe[0], Stripe[1], Stripe[2], Scp1730Layout.Facings[Made.controlFacing(Side)], Scp1730Builder.HazardDecal, StripeVariant, Made.Zone());
        }
        B.Fixtures.add(Made.fixture());
    }

    static boolean fits(Scp1730RoomDressing.Cells G, Frame Each, Scp1730Blocks.Palette Look) {
        int F = Each.F();
        String Middle = Look.wall(2, Scp1730Builder.CorridorHeight);
        if (!Middle.equals(G.at(Each.x(0, -2), F + 2, Each.z(0, -2))) || !Middle.equals(G.at(Each.x(0, 2), F + 2, Each.z(0, 2)))) {
            return false;
        }
        for (int A = -2; A <= 2; A++) {
            for (int Lat = -1; Lat <= 1; Lat++) {
                if (!floor(G.at(Each.x(A, Lat), F, Each.z(A, Lat)))) {
                    return false;
                }
                for (int Rise = 1; Rise <= Height; Rise++) {
                    if ((Math.abs(A) <= 1 || Rise < Height) && !clearable(G.at(Each.x(A, Lat), F + Rise, Each.z(A, Lat)))) {
                        return false;
                    }
                }
            }
            for (int Lat = -2; Lat <= 2; Lat += 4) {
                for (int Rise = 1; Rise <= Height; Rise++) {
                    String Wall = G.at(Each.x(A, Lat), F + Rise, Each.z(A, Lat));
                    if (Math.abs(A) <= 1 ? !Look.wall(Rise, Scp1730Builder.CorridorHeight).equals(Wall) : !solid(Wall)) {
                        return false;
                    }
                }
            }
        }
        for (int A = -1; A <= 1; A++) {
            for (int Lat = -2; Lat <= 2; Lat++) {
                String Top = G.at(Each.x(A, Lat), F + Height + 1, Each.z(A, Lat));
                if (A == 0 && Math.abs(Lat) <= 1 ? !Look.Ceiling.equals(Top) : !solid(Top)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean marker(Scp1730Fixture Mark) {
        int Kind = Mark.Data() >> 4;
        return Kind == Scp1730Builder.HazardDecal || Kind == Scp1730Builder.ZoneDecal || Kind == Scp1730Builder.ArrowDecal || Kind == Scp1730Builder.Emblem;
    }

    static List<Scp1730Fixture> relevant(List<Scp1730Fixture> Fixtures, int Zone) {
        List<Scp1730Fixture> Made = new ArrayList<>();
        for (Scp1730Fixture Mark : Fixtures) {
            if (Mark.Zone() == Zone && !Mark.Type().equals(Type) && (!Mark.Type().equals("decal") || !Mark.Facing().equals("up") && marker(Mark))) {
                Made.add(Mark);
            }
        }
        return Made;
    }

    static int margin(String Kind) {
        if (PassageRooms.contains(Kind)) {
            return PassageMargin;
        }
        if (OddRooms.contains(Kind) || Scp1730AnomalousRooms.Kinds.contains(Kind)) {
            return OddMargin;
        }
        return RoomMargin;
    }

    static String forbidden(Scp1730RoomDressing.Cells G, List<Scp1730Fixture> Fixtures, List<Scp1730RoomInfo> Rooms, List<Scp1730Basin> Basins, List<Scp1730Pit> Pits, Frame Each,
                            Set<Long> Own) {
        int MinX = Math.min(Each.x(-2, -2), Each.x(2, 2));
        int MaxX = Math.max(Each.x(-2, -2), Each.x(2, 2));
        int MinZ = Math.min(Each.z(-2, -2), Each.z(2, 2));
        int MaxZ = Math.max(Each.z(-2, -2), Each.z(2, 2));
        for (Scp1730RoomInfo Area : Rooms) {
            if (Area.Zone() != Each.Zone()) {
                continue;
            }
            int Margin = margin(Area.Type());
            if (MaxX >= Area.MinX() - Margin && MinX <= Area.MaxX() + Margin && MaxZ >= Area.MinZ() - Margin && MinZ <= Area.MaxZ() + Margin) {
                return "too close to the " + Area.Type() + " room";
            }
        }
        for (Scp1730Basin Pool : Basins) {
            if (Pool.Zone() == Each.Zone() && MaxX >= Pool.MinX() - BasinMargin && MinX <= Pool.MaxX() + BasinMargin && MaxZ >= Pool.MinZ() - BasinMargin
                    && MinZ <= Pool.MaxZ() + BasinMargin) {
                return "beside a " + Pool.Kind() + " basin";
            }
        }
        for (Scp1730Pit Hole : Pits) {
            if (Hole.Zone() == Each.Zone() && MaxX >= Hole.MinX() - BasinMargin && MinX <= Hole.MaxX() + BasinMargin && MaxZ >= Hole.MinZ() - BasinMargin && MinZ <= Hole.MaxZ() + BasinMargin) {
                return "beside a pit";
            }
        }
        int Cy = Each.F() + 1;
        for (Scp1730Fixture Mark : Fixtures) {
            if (Mark.Zone() != Each.Zone() || Mark.Type().equals(Type) || Math.abs(Mark.Y() - Cy) > 8) {
                continue;
            }
            int Far = Math.max(Math.abs(Mark.X() - Each.Cx()), Math.abs(Mark.Z() - Each.Cz()));
            if (Mark.Type().equals("decal")) {
                if (marker(Mark) && !Mark.Facing().equals("up") && Far <= FixtureReach && !Own.contains(Scp1730Builder.key(Mark.X(), Mark.Y(), Mark.Z()))) {
                    return "beside a wall sign decal";
                }
                continue;
            }
            int Reach = Critical.contains(Mark.Type()) ? CriticalReach : Mark.Type().equals("hall") ? HallReach : Hazards.contains(Mark.Type()) ? HazardReach : FixtureReach;
            if (Far <= Reach) {
                return "too close to a " + Mark.Type();
            }
        }
        for (int A = -3; A <= 3; A++) {
            for (int Lat = -2; Lat <= 2; Lat++) {
                for (int Rise = 1; Rise <= Height + 1; Rise++) {
                    if (signLike(G.at(Each.x(A, Lat), Each.F() + Rise, Each.z(A, Lat)))) {
                        return "beside a sign";
                    }
                }
            }
        }
        return null;
    }

    record Route(Scp1730PuzzleFlood From, Scp1730PuzzleFlood To, int Span) {
        boolean crosses(Frame Each) {
            for (int Lat = -1; Lat <= 1; Lat++) {
                int X = Each.x(0, Lat);
                int Z = Each.z(0, Lat);
                int There = From.distance(X, Each.F() + 1, Z, 0);
                int Back = To.distance(X, Each.F() + 1, Z, 0);
                if (There >= 0 && Back >= 0 && There + Back <= Span + RouteSlack) {
                    return true;
                }
            }
            return false;
        }
    }

    static Route timed(Scp1730RoomDressing.Cells G, List<Scp1730Fixture> Fixtures, int Zone, int F) {
        boolean Dual = false;
        Scp1730Fixture First = null;
        Scp1730Fixture Second = null;
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Zone() != Zone) {
                continue;
            }
            if (Each.Type().equals("puzzle") && Each.Data() == Scp1730Puzzles.DualKey) {
                Dual = true;
            }
            if (Each.Type().equals("pdevice") && Scp1730Puzzles.role(Each.Data()) == Scp1730Puzzles.RoleKeyA) {
                First = Each;
            }
            if (Each.Type().equals("pdevice") && Scp1730Puzzles.role(Each.Data()) == Scp1730Puzzles.RoleKeyB) {
                Second = Each;
            }
        }
        if (!Dual || First == null || Second == null) {
            return null;
        }
        Scp1730PuzzleFlood From = front(G, First, F);
        Scp1730PuzzleFlood To = front(G, Second, F);
        int Span = From.distance(Second.X(), F + 1, Second.Z(), Scp1730PuzzlePlan.Near);
        return Span < 0 ? null : new Route(From, To, Span);
    }

    static Scp1730PuzzleFlood front(Scp1730RoomDressing.Cells G, Scp1730Fixture Device, int F) {
        int Dir = Scp1730Builder.dirOf(Device.Facing());
        return new Scp1730PuzzleFlood(G, F - 2, F + 10, Set.of()).flood(Device.X() + Scp1730Layout.Dx[Dir] + 0.5, F + 1, Device.Z() + Scp1730Layout.Dz[Dir] + 0.5);
    }

    record Deep(Scp1730RoomDressing.Cells G, Scp1730Spot Entry, int F, Set<Long> Gate, List<int[]> Goals) {
        boolean clear(Set<Long> Shut) {
            Set<Long> Closed = new HashSet<>(Gate);
            Closed.addAll(Shut);
            Scp1730PuzzleFlood Flow = new Scp1730PuzzleFlood(G, F - 2, F + 10, Set.of(), Closed).flood(Entry.X(), Entry.Y(), Entry.Z());
            for (int[] Goal : Goals) {
                if (!Flow.near(Goal[0], Goal[1], Goal[2], Goal[3])) {
                    return false;
                }
            }
            return true;
        }
    }

    static Deep deep(Scp1730RoomDressing.Cells G, List<Scp1730Fixture> Fixtures, Scp1730Spot Entry, int Zone, int F) {
        if (Entry == null) {
            return null;
        }
        Set<Long> Gate = new HashSet<>();
        Scp1730Fixture Middle = null;
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Zone() == Zone && Each.Type().equals("dseal")) {
                Gate.add(Scp1730Builder.key(Each.X(), Each.Y(), Each.Z()));
                if (Each.Data() == 4) {
                    Middle = Each;
                }
            }
        }
        List<int[]> Wanted = new ArrayList<>();
        if (Middle != null) {
            int Dir = Scp1730Builder.dirOf(Middle.Facing());
            Wanted.add(new int[]{Middle.X() + Scp1730Layout.Dx[Dir], F + 1, Middle.Z() + Scp1730Layout.Dz[Dir], 1});
        }
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Zone() == Zone && Targets.contains(Each.Type())) {
                Wanted.add(new int[]{Each.X(), F + 1, Each.Z(), 2});
            }
        }
        Scp1730PuzzleFlood Base = new Scp1730PuzzleFlood(G, F - 2, F + 10, Set.of(), Gate).flood(Entry.X(), Entry.Y(), Entry.Z());
        List<int[]> Goals = new ArrayList<>();
        for (int[] Goal : Wanted) {
            if (Base.near(Goal[0], Goal[1], Goal[2], Goal[3])) {
                Goals.add(Goal);
            }
        }
        return new Deep(G, Entry, F, Gate, Goals);
    }

    public static Gates gates(Scp1730Plan Plan) {
        synchronized (Known) {
            return Known.computeIfAbsent(Plan, Gates::new);
        }
    }

    public static Scp1730RoomDressing.Cells through(Scp1730Plan Plan) {
        Gates Found = gates(Plan);
        return (X, Y, Z) -> Found.through(Plan, X, Y, Z);
    }

    static boolean works(Scp1730Plan Plan, Cut Each, int Side) {
        int[] At = Each.control(Side);
        String State = Plan.stateAt(At[0], At[1], At[2]);
        String Wanted = Each.lever(Side) ? Lever : Button;
        if (State == null || !State.startsWith(Wanted + "[") || !Scp1730Layout.Facings[Each.controlFacing(Side)].equals(Scp1730RoomDressing.prop(State, "facing"))) {
            return false;
        }
        if (Each.lever(Side) && !"wall".equals(Scp1730RoomDressing.prop(State, "face"))) {
            return false;
        }
        int[] Back = Each.support(Side);
        if (!solid(Plan.stateAt(Back[0], Back[1], Back[2]))) {
            return false;
        }
        int Column = Each.controlColumn(Side);
        if (!Each.door(Column)) {
            return false;
        }
        int[] LeafAt = Each.cell(Column, 1, 0);
        String LeafState = Plan.stateAt(LeafAt[0], LeafAt[1], LeafAt[2]);
        return LeafState != null && LeafState.startsWith(Scp1730Blocks.Mod + Each.name() + "[");
    }

    public static final class Gates {
        private final Map<Long, Integer> Plane = new HashMap<>();
        private final Set<Long> Controls = new HashSet<>();
        private final List<Cut> All;
        private final boolean[][] Working;

        Gates(Scp1730Plan Plan) {
            All = Scp1730Cutoffs.cuts(Plan);
            Working = new boolean[All.size()][2];
            for (int Index = 0; Index < All.size(); Index++) {
                Cut Each = All.get(Index);
                Working[Index][0] = Scp1730Cutoffs.works(Plan, Each, -1);
                Working[Index][1] = Scp1730Cutoffs.works(Plan, Each, 1);
                for (int[] Cell : Each.doorCells()) {
                    Plane.put(Scp1730Builder.key(Cell[0], Cell[1], Cell[2]), Index);
                }
                for (int Side = -1; Side <= 1; Side += 2) {
                    int[] At = Each.control(Side);
                    Controls.add(Scp1730Builder.key(At[0], At[1], At[2]));
                }
            }
        }

        public boolean any() {
            return !All.isEmpty();
        }

        public List<Cut> cuts() {
            return All;
        }

        public boolean works(int Index, int Side) {
            return Working[Index][Side > 0 ? 1 : 0];
        }

        boolean candidate(String State) {
            return State != null && !All.isEmpty() && (State.contains("part=") || State.startsWith(Scp1730Blocks.Mod + "door_"));
        }

        public String at(Scp1730Plan Plan, int X, int Y, int Z) {
            String State = Plan.stateAt(X, Y, Z);
            if (!candidate(State)) {
                return State;
            }
            long Key = Scp1730Builder.key(X, Y, Z);
            if (Controls.contains(Key)) {
                return null;
            }
            Integer Index = Plane.get(Key);
            return Index != null && (works(Index, -1) || works(Index, 1)) ? null : State;
        }

        public String through(Scp1730Plan Plan, int X, int Y, int Z) {
            String State = Plan.stateAt(X, Y, Z);
            if (!candidate(State)) {
                return State;
            }
            long Key = Scp1730Builder.key(X, Y, Z);
            if (Controls.contains(Key)) {
                return null;
            }
            Integer Index = Plane.get(Key);
            return Index != null && works(Index, -1) && works(Index, 1) ? null : State;
        }

        public boolean enter(int FromX, int FromZ, int X, int Y, int Z) {
            if (All.isEmpty()) {
                return true;
            }
            Integer Index = Plane.get(Scp1730Builder.key(X, Y, Z));
            if (Index == null) {
                return true;
            }
            int Side = All.get(Index).side(FromX, FromZ);
            return Side == 0 || works(Index, Side);
        }
    }
}
