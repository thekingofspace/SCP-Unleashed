package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class Scp1730PuzzlePlan {
    public static final String Panel = Scp1730Blocks.Mod + "scp1730_lockdown_panel";
    public static final String Junction = Scp1730Blocks.Mod + "scp1730_junction_box";
    public static final String Terminal = Scp1730Blocks.Mod + "scp1730_terminal";
    public static final String Clue = Scp1730Blocks.Mod + "scp1730_clue_board";
    public static final String Switch = Scp1730Blocks.Mod + "scp1730_switch";
    public static final String Valve = Scp1730Blocks.Mod + "scp1730_valve";
    public static final String VentGrate = Scp1730Blocks.Mod + "scp1730_vent_grate";
    public static final String Cable = Scp1730Blocks.Mod + "scp1730_cable_junction";
    public static final String Hatch = Scp1730Blocks.Mod + "scp1730_disposal_hatch";
    public static final String Generator = Scp1730Blocks.Mod + "scp1730_emergency_generator";
    public static final String Pickup = Scp1730Blocks.Mod + "scp1730_pickup";
    public static final String Pylon = Scp1730Blocks.Mod + "scp1730_glyph_pylon";
    public static final String Sluice = Scp1730Blocks.Mod + "scp1730_sluice_wheel";
    public static final List<String> WallDevices = List.of(Panel, Junction, Terminal, Clue, Switch, Valve, Cable, Hatch, Sluice);
    public static final List<String> Deep = List.of("holding_cell", "collapsed");
    public static final int Near = 2;
    public static final int Touch = 1;
    public static final int MinShaft = 2;
    public static final int MaxShaft = 5;
    public static final int FanDepth = 4;
    public static final double WalkSpeed = 4.0;
    public static final int WindowMargin = 12;
    public static final List<String> Offices = List.of("office", "small_office", "records", "conference", "comms", "cafeteria", "storage", "elevator", "checkpoint", "security",
            "holding_cell", "breaker");
    public static final List<String> Light = List.of("medical", "dorms", "research", "storage", "storage_bay", "decon", "cell_block", "test_chamber", "small_office", "breaker",
            "holding_cell");
    public static final List<String> Heavy = List.of("armoury", "storage", "server", "cooling", "pump_room", "incinerator", "catwalk_room", "heavy_cell", "generator", "holding_cell",
            "collapsed");

    record Change(int X, int Y, int Z, String Old) {
    }

    record Target(int X, int Y, int Z, int Mode) {
    }

    final Scp1730Builder B;
    final Scp1730Grid G;
    final int Zone;
    final int F;
    final Random Rng;
    final List<Change> Changes = new ArrayList<>();
    final List<Long> Cleared = new ArrayList<>();
    final List<Scp1730Fixture> Pending = new ArrayList<>();
    final List<Scp1730Fixture> Dropped = new ArrayList<>();
    final Set<Long> Used = new HashSet<>();
    final List<int[]> Sites = new ArrayList<>();
    final Set<Scp1730Layout.Room> Taken = new HashSet<>();
    final Set<Long> Locked = new HashSet<>();
    final Set<Long> Gate = new HashSet<>();
    final List<Target> Targets = new ArrayList<>();
    Scp1730Fixture Middle;
    int[] PanelAt;
    String PanelFacing;
    Scp1730Fixture OldReader;
    Scp1730Layout.Room Last;

    Scp1730PuzzlePlan(Scp1730Builder B, int Zone) {
        this.B = B;
        this.G = B.Grid;
        this.Zone = Zone;
        this.F = Scp1730Builder.Floors[Zone];
        this.Rng = new Random(Scp1730Builder.mix(B.Seed ^ Scp1730Puzzles.Salt, 40 + Zone));
    }

    public static boolean passable(String State) {
        if (State == null || !State.startsWith(Scp1730Blocks.Mod + "scp1730_")) {
            return false;
        }
        for (String Each : WallDevices) {
            if (State.startsWith(Each + "[") || State.equals(Each)) {
                return true;
            }
        }
        return State.startsWith(Pickup) || State.startsWith(VentGrate) && State.contains("open=true");
    }

    public static boolean lockedDoor(Scp1730Plan Plan, int X, int Y, int Z) {
        for (Scp1730Fixture Each : Plan.fixtures("pseal")) {
            if (Each.X() == X && Each.Y() == Y && Each.Z() == Z) {
                return true;
            }
        }
        return false;
    }

    public static String objective(Scp1730Builder B, int Zone) {
        if (Zone < Scp1730Puzzles.Zones) {
            for (Scp1730Fixture Each : B.Fixtures) {
                if (Each.Type().equals("puzzle") && Each.Zone() == Zone) {
                    return Scp1730Puzzles.Ids[Each.Data()];
                }
            }
        }
        return Scp1730Builder.Objectives[Zone];
    }

    static void place(Scp1730Builder B) {
        for (int Zone = 0; Zone < Scp1730Puzzles.Zones; Zone++) {
            new Scp1730PuzzlePlan(B, Zone).run();
        }
    }

    void run() {
        for (Scp1730Fixture Each : B.Fixtures) {
            if (Each.Type().equals("seal") && Each.Zone() == Zone && Each.Data() == 4) {
                Middle = Each;
            }
        }
        if (Zone == Scp1730Puzzles.DeepZone && !gate()) {
            throw new IllegalStateException("the deep zone has no sanctum door");
        }
        if (Middle == null || !panelSpot()) {
            throw new IllegalStateException("zone " + Zone + " has no exit for a puzzle");
        }
        List<int[]> Keep = baseline();
        for (int Puzzle : Scp1730Puzzles.order(B.Seed, Zone)) {
            begin();
            boolean Made;
            try {
                Made = build(Puzzle) && verify(Keep);
            } catch (IllegalStateException Problem) {
                Made = false;
            }
            if (Made) {
                commit(Puzzle);
                return;
            }
            undo();
        }
        throw new IllegalStateException("zone " + Zone + " fits no puzzle");
    }

    boolean gate() {
        for (Scp1730Layout.Room Each : B.Layouts[Zone].Rooms) {
            if (!Each.Type.equals("sanctum") || Each.Doors.isEmpty()) {
                continue;
            }
            Scp1730Layout.Door Door = Each.Doors.get(0);
            int Cx = Each.doorX(Door);
            int Cz = Each.doorZ(Door);
            int Lx = Door.Dir % 2 == 0 ? 1 : 0;
            int Lz = Door.Dir % 2 == 0 ? 0 : 1;
            for (int Side = -1; Side <= 1; Side++) {
                for (int Level = 1; Level <= 3; Level++) {
                    if (!G.empty(Cx + Lx * Side, F + Level, Cz + Lz * Side)) {
                        return false;
                    }
                    Gate.add(Scp1730Builder.key(Cx + Lx * Side, F + Level, Cz + Lz * Side));
                }
            }
            Middle = new Scp1730Fixture("dseal", Cx, F + 2, Cz, Scp1730Layout.Facings[Door.Dir], 4, Zone);
            return true;
        }
        return false;
    }

    int[] doorSpot(int Cx, int Cz, int Dir) {
        int Lx = Dir % 2 == 0 ? 1 : 0;
        int Lz = Dir % 2 == 0 ? 0 : 1;
        int Ox = Scp1730Layout.Dx[Dir];
        int Oz = Scp1730Layout.Dz[Dir];
        for (int Side = -1; Side <= 1; Side += 2) {
            int Qx = Cx + Ox + Lx * Side;
            int Qz = Cz + Oz + Lz * Side;
            int Bx = Cx + Ox + Lx * Side * 2;
            int Bz = Cz + Oz + Lz * Side * 2;
            if (G.empty(Qx, F + 2, Qz) && G.empty(Qx, F + 1, Qz) && Scp1730Builder.isWallLike(G.get(Bx, F + 2, Bz)) && !signBeside(Qx, F + 2, Qz)) {
                return new int[]{Qx, Qz, Scp1730Builder.dirOf(Scp1730Builder.facingOf(-Lx * Side, -Lz * Side))};
            }
        }
        int[] Spot = B.beside(Cx, Cz, Lx, Lz, Ox, Oz, F + 2, 1);
        return Spot == null ? null : new int[]{Spot[0], Spot[1], Dir};
    }

    boolean panelSpot() {
        int Dir = Scp1730Builder.dirOf(Middle.Facing());
        int Lx = Dir % 2 == 0 ? 1 : 0;
        int Lz = Dir % 2 == 0 ? 0 : 1;
        int Ox = Scp1730Layout.Dx[Dir];
        int Oz = Scp1730Layout.Dz[Dir];
        if (Zone == Scp1730Puzzles.DeepZone) {
            int[] Spot = doorSpot(Middle.X(), Middle.Z(), Dir);
            if (Spot == null) {
                return false;
            }
            PanelAt = new int[]{Spot[0], Spot[1]};
            PanelFacing = Scp1730Layout.Facings[Spot[2]];
            return true;
        }
        for (Scp1730Fixture Each : B.Fixtures) {
            if (Each.Type().equals("control") && Each.Zone() == Zone && Each.Data() == 0 && Each.Y() == F + 2 && Math.abs(Each.X() - Middle.X()) <= 3
                    && Math.abs(Each.Z() - Middle.Z()) <= 3) {
                OldReader = Each;
                PanelAt = new int[]{Each.X(), Each.Z()};
                PanelFacing = Each.Facing();
                return true;
            }
        }
        int[] Spot = B.beside(Middle.X(), Middle.Z(), Lx, Lz, Ox, Oz, F + 2, 1);
        if (Spot == null) {
            Spot = B.niche(Middle.X(), Middle.Z(), Lx, Lz, Ox, Oz, F, 1, B.corridorPalette(Zone));
        }
        if (Spot == null) {
            return false;
        }
        PanelAt = new int[]{Spot[0], Spot[1]};
        PanelFacing = Scp1730Builder.facingOf(Ox, Oz);
        return true;
    }

    Scp1730RoomDressing.Cells source() {
        return G::get;
    }

    Scp1730PuzzleFlood flood(Set<Long> Open) {
        Scp1730Spot Entry = Zone == 0 ? B.Arrival : B.Entries[Zone];
        return new Scp1730PuzzleFlood(source(), F - 2, F + 10, Open, Gate).flood(Entry.X(), Entry.Y(), Entry.Z());
    }

    Set<Long> wingSeals() {
        Set<Long> Made = new HashSet<>();
        for (Scp1730Fixture Each : B.Fixtures) {
            if (Each.Type().equals("bioseal")) {
                Made.add(Scp1730Builder.key(Each.X(), Each.Y(), Each.Z()));
            }
        }
        return Made;
    }

    List<int[]> baseline() {
        Scp1730PuzzleFlood Closed = flood(Set.of());
        Scp1730PuzzleFlood Wing = flood(wingSeals());
        List<int[]> Keep = new ArrayList<>();
        for (Scp1730Fixture Each : B.Fixtures) {
            if (Each.Zone() != Zone) {
                continue;
            }
            boolean Walk = Each.Type().equals("control") || Each.Type().equals("card") || Each.Type().equals("biocontrol");
            boolean Wide = Each.Type().equals("loot") || Each.Type().equals("safe") || Each.Type().equals("setpiece");
            int Y = Walk ? F + 1 : Each.Y();
            if (Walk && Closed.near(Each.X(), Y, Each.Z(), Near)) {
                Keep.add(new int[]{Each.X(), Y, Each.Z(), 0});
            } else if (Wide && Wing.near(Each.X(), Y, Each.Z(), Near)) {
                Keep.add(new int[]{Each.X(), Y, Each.Z(), 1});
            }
        }
        int Dir = Scp1730Builder.dirOf(Middle.Facing());
        Keep.add(new int[]{Middle.X() + Scp1730Layout.Dx[Dir], F + 1, Middle.Z() + Scp1730Layout.Dz[Dir], 0});
        return Keep;
    }

    void begin() {
        Changes.clear();
        Cleared.clear();
        Pending.clear();
        Dropped.clear();
        Used.clear();
        Sites.clear();
        Taken.clear();
        Locked.clear();
        Targets.clear();
        Last = null;
        Sites.add(new int[]{Middle.X(), Middle.Z()});
    }

    void set(int X, int Y, int Z, String State) {
        Changes.add(new Change(X, Y, Z, G.get(X, Y, Z)));
        G.set(X, Y, Z, State);
    }

    void clear(int X, int Y, int Z) {
        long Key = Scp1730Builder.key(X, Y, Z);
        if (!B.Clear.contains(Key)) {
            B.Clear.add(Key);
            Cleared.add(Key);
        }
    }

    void fixture(String Type, int X, int Y, int Z, String Facing, int Data) {
        Pending.add(new Scp1730Fixture(Type, X, Y, Z, Facing, Data, Zone));
    }

    void undo() {
        for (int Index = Changes.size() - 1; Index >= 0; Index--) {
            Change Each = Changes.get(Index);
            G.set(Each.X(), Each.Y(), Each.Z(), Each.Old());
        }
        for (Long Each : Cleared) {
            B.Clear.remove(Each);
        }
        Changes.clear();
        Cleared.clear();
    }

    void commit(int Puzzle) {
        B.Fixtures.removeAll(Dropped);
        B.Fixtures.addAll(Pending);
        if (Zone == Scp1730Puzzles.DeepZone) {
            int Dir = Scp1730Builder.dirOf(Middle.Facing());
            int Lx = Dir % 2 == 0 ? 1 : 0;
            int Lz = Dir % 2 == 0 ? 0 : 1;
            int Index = 0;
            for (int Side = -1; Side <= 1; Side++) {
                for (int Level = 1; Level <= 3; Level++) {
                    B.Fixtures.add(new Scp1730Fixture("dseal", Middle.X() + Lx * Side, F + Level, Middle.Z() + Lz * Side, Middle.Facing(), Index++, Zone));
                }
            }
        }
        B.Fixtures.add(new Scp1730Fixture("puzzle", PanelAt[0], F + 2, PanelAt[1], PanelFacing, Puzzle, Zone));
    }

    boolean verify(List<int[]> Keep) {
        Scp1730PuzzleFlood Closed = flood(Set.of());
        Scp1730PuzzleFlood Opened = Locked.isEmpty() ? Closed : flood(Locked);
        Set<Long> Wide = wingSeals();
        Wide.addAll(Locked);
        Scp1730PuzzleFlood Wing = flood(Wide);
        for (int[] Each : Keep) {
            if (!(Each[3] == 0 ? Closed : Wing).near(Each[0], Each[1], Each[2], Near)) {
                return false;
            }
        }
        for (Target Each : Targets) {
            if (inWing(Each.X(), Each.Z())) {
                return false;
            }
            boolean Walked = Closed.near(Each.X(), F + 1, Each.Z(), Touch);
            if (Each.Mode() == 0 && !Walked) {
                return false;
            }
            if (Each.Mode() == 1 && (Walked || !Opened.near(Each.X(), F + 1, Each.Z(), Touch))) {
                return false;
            }
        }
        return true;
    }

    boolean inWing(int X, int Z) {
        for (Scp1730RoomInfo Each : B.RoomInfos) {
            if (Each.Type().equals(Scp1730Sectors.BioWing) && Each.Zone() == Zone && X >= Each.MinX() && X <= Each.MaxX() && Z >= Each.MinZ() && Z <= Each.MaxZ()) {
                return true;
            }
        }
        return false;
    }

    boolean build(int Puzzle) {
        panel(Puzzle);
        return switch (Puzzle) {
            case Scp1730Puzzles.Keycard -> keycard();
            case Scp1730Puzzles.Fuses -> fuses();
            case Scp1730Puzzles.Override -> override();
            case Scp1730Puzzles.DualKey -> dualKey();
            case Scp1730Puzzles.Vent -> vent();
            case Scp1730Puzzles.Splice -> splice();
            case Scp1730Puzzles.Relays -> relays();
            case Scp1730Puzzles.Sample -> sample();
            case Scp1730Puzzles.Cells -> cells();
            case Scp1730Puzzles.Generator -> generator();
            case Scp1730Puzzles.Coolant -> coolant();
            case Scp1730Puzzles.PowerCycle -> powerCycle();
            case Scp1730Puzzles.Pylons -> pylons();
            case Scp1730Puzzles.Sluices -> sluices();
            default -> false;
        };
    }

    void panel(int Puzzle) {
        set(PanelAt[0], F + 2, PanelAt[1], Panel + "[facing=" + PanelFacing + ",lit=0,steps=" + Scp1730Puzzles.steps(Puzzle) + "]");
        if (OldReader != null) {
            Dropped.add(OldReader);
        }
        fixture("control", PanelAt[0], F + 2, PanelAt[1], PanelFacing, 0);
    }

    boolean usable(Scp1730Layout.Room Room) {
        String Type = Room.Type;
        return !Type.startsWith("stair") && !Type.equals("lobby") && !Type.equals(Scp1730Sectors.BioWing) && !Scp1730AnomalousRooms.anomalous(Type) && !Type.equals("sanctum")
                && !Type.equals("pit") && !Type.equals("tesla");
    }

    int[] centre(Scp1730Layout.Room Room) {
        return new int[]{(Room.Bx0 + Room.Bx1) / 2, (Room.Bz0 + Room.Bz1) / 2};
    }

    int spread(Scp1730Layout.Room Room) {
        int[] At = centre(Room);
        int Best = Integer.MAX_VALUE;
        for (int[] Each : Sites) {
            Best = Math.min(Best, Math.abs(Each[0] - At[0]) + Math.abs(Each[1] - At[1]));
        }
        return Best;
    }

    List<Scp1730Layout.Room> rooms(List<String> Types, boolean Fresh) {
        List<Scp1730Layout.Room> Found = new ArrayList<>();
        for (Scp1730Layout.Room Each : B.Layouts[Zone].Rooms) {
            if (usable(Each) && Types.contains(Each.Type) && (!Fresh || !Taken.contains(Each))) {
                Found.add(Each);
            }
        }
        for (int Index = Found.size() - 1; Index > 0; Index--) {
            int Swap = Rng.nextInt(Index + 1);
            Scp1730Layout.Room Held = Found.get(Index);
            Found.set(Index, Found.get(Swap));
            Found.set(Swap, Held);
        }
        Found.sort((First, Second) -> Integer.compare(spread(Second) / 12, spread(First) / 12));
        return Found;
    }

    List<Scp1730Layout.Room> preferred(List<String> Wanted, List<String> Fallback) {
        List<Scp1730Layout.Room> Made = new ArrayList<>(rooms(Wanted, true));
        for (Scp1730Layout.Room Each : rooms(Fallback, true)) {
            if (!Made.contains(Each)) {
                Made.add(Each);
            }
        }
        return Made;
    }

    void site(Scp1730Layout.Room Room) {
        Taken.add(Room);
        Sites.add(centre(Room));
        Last = Room;
    }

    boolean signBeside(int X, int Y, int Z) {
        for (int Dir = 0; Dir < 4; Dir++) {
            String Side = G.get(X + Scp1730Layout.Dx[Dir], Y, Z + Scp1730Layout.Dz[Dir]);
            if (Side != null && Side.startsWith(Scp1730Blocks.Mod + "scp1730_sign_")) {
                return true;
            }
        }
        return false;
    }

    boolean busy(int X, int Z) {
        for (int Ox = -1; Ox <= 1; Ox++) {
            for (int Oz = -1; Oz <= 1; Oz++) {
                if (Used.contains(Scp1730Builder.key(X + Ox, 0, Z + Oz))) {
                    return true;
                }
            }
        }
        return false;
    }

    int[] wallSpot(Scp1730Rooms.Box R) {
        List<int[]> Cells = R.wallCells();
        for (int Index = Cells.size() - 1; Index > 0; Index--) {
            int Swap = Rng.nextInt(Index + 1);
            int[] Held = Cells.get(Index);
            Cells.set(Index, Cells.get(Swap));
            Cells.set(Swap, Held);
        }
        for (int[] Cell : Cells) {
            int X = Cell[0];
            int Z = Cell[1];
            int Dir = Cell[2];
            int Wx = X - Scp1730Layout.Dx[Dir];
            int Wz = Z - Scp1730Layout.Dz[Dir];
            if (!R.free(X, F + 2, Z) || !G.empty(X, F + 1, Z) || busy(X, Z) || !Scp1730Builder.isWallLike(G.get(Wx, F + 2, Wz)) || Scp1730Sectors.nearDoor(R, X, Z, 2)
                    || signBeside(X, F + 2, Z) || !G.empty(X + Scp1730Layout.Dx[Dir], F + 1, Z + Scp1730Layout.Dz[Dir]) || !G.empty(X + Scp1730Layout.Dx[Dir], F + 2, Z + Scp1730Layout.Dz[Dir])) {
                continue;
            }
            if (!Scp1730Builder.isWallLike(G.get(Wx, F + 1, Wz)) && !Scp1730Builder.backed(G.get(Wx, F + 1, Wz))) {
                continue;
            }
            return new int[]{X, Z, Dir};
        }
        return null;
    }

    boolean device(Scp1730Layout.Room Room, int Role, int Extra, String Block, String Props, int Mode) {
        Scp1730Rooms.Box R = B.Rooms.new Box(Room, Zone);
        int[] Spot = wallSpot(R);
        if (Spot == null) {
            return false;
        }
        String Out = Scp1730Layout.Facings[Spot[2]];
        set(Spot[0], F + 2, Spot[1], Block + "[facing=" + Out + (Props.isEmpty() ? "" : "," + Props) + "]");
        fixture("pdevice", Spot[0], F + 2, Spot[1], Out, Scp1730Puzzles.data(Role, Extra));
        mark(Spot[0], Spot[1], Spot[2], Mode);
        return true;
    }

    void mark(int X, int Z, int Dir, int Mode) {
        Used.add(Scp1730Builder.key(X, 0, Z));
        clear(X, F + 1, Z);
        clear(X + Scp1730Layout.Dx[Dir], F + 1, Z + Scp1730Layout.Dz[Dir]);
        clear(X + Scp1730Layout.Dx[Dir], F + 2, Z + Scp1730Layout.Dz[Dir]);
        Targets.add(new Target(X, F + 1, Z, Mode));
    }

    boolean deviceIn(List<Scp1730Layout.Room> Options, int Role, int Extra, String Block, String Props, int Mode) {
        for (Scp1730Layout.Room Each : Options) {
            if (device(Each, Role, Extra, Block, Props, Mode)) {
                site(Each);
                return true;
            }
        }
        return false;
    }

    static String shortId(String State) {
        int Cut = State.indexOf('[');
        String Id = Cut < 0 ? State : State.substring(0, Cut);
        return Id.startsWith(Scp1730Blocks.Mod) ? Id.substring(Scp1730Blocks.Mod.length()) : Id;
    }

    boolean pickup(Scp1730Layout.Room Room, int Item, int Index, int Mode) {
        Scp1730Rooms.Box R = B.Rooms.new Box(Room, Zone);
        List<int[]> Surfaces = new ArrayList<>();
        for (int X = R.X0; X <= R.X1; X++) {
            for (int Z = R.Z0; Z <= R.Z1; Z++) {
                String Below = G.get(X, F + 1, Z);
                if (Below == null || !G.empty(X, F + 2, Z) || busy(X, Z) || signBeside(X, F + 2, Z)) {
                    continue;
                }
                String Id = shortId(Below);
                if (Scp1730RoomDressing.Desks.contains(Id) || Scp1730RoomDressing.Tables.contains(Id)) {
                    Surfaces.add(new int[]{X, Z});
                }
            }
        }
        for (int Pass = Surfaces.size() - 1; Pass > 0; Pass--) {
            int Swap = Rng.nextInt(Pass + 1);
            int[] Held = Surfaces.get(Pass);
            Surfaces.set(Pass, Surfaces.get(Swap));
            Surfaces.set(Swap, Held);
        }
        for (int[] Each : Surfaces) {
            int Dir = Rng.nextInt(4);
            boolean Open = false;
            for (int Turn = 0; Turn < 4 && !Open; Turn++) {
                int Nx = Each[0] + Scp1730Layout.Dx[(Dir + Turn) % 4];
                int Nz = Each[1] + Scp1730Layout.Dz[(Dir + Turn) % 4];
                if (G.empty(Nx, F + 1, Nz) && G.empty(Nx, F + 2, Nz) && B.standable(Nx, F, Nz)) {
                    Open = true;
                    Dir = (Dir + Turn) % 4;
                }
            }
            if (!Open) {
                continue;
            }
            String Out = Scp1730Layout.Facings[Dir];
            set(Each[0], F + 2, Each[1], Pickup + "[facing=" + Out + ",item=" + Scp1730Puzzles.Items[Item] + "]");
            fixture("pickup", Each[0], F + 2, Each[1], Out, Scp1730Puzzles.pickup(Item, Index));
            Used.add(Scp1730Builder.key(Each[0], 0, Each[1]));
            Targets.add(new Target(Each[0], F + 1, Each[1], Mode));
            return true;
        }
        List<int[]> Cells = R.wallCells();
        for (int Pass = Cells.size() - 1; Pass > 0; Pass--) {
            int Swap = Rng.nextInt(Pass + 1);
            int[] Held = Cells.get(Pass);
            Cells.set(Pass, Cells.get(Swap));
            Cells.set(Swap, Held);
        }
        for (int[] Cell : Cells) {
            int X = Cell[0];
            int Z = Cell[1];
            int Ix = X + Scp1730Layout.Dx[Cell[2]];
            int Iz = Z + Scp1730Layout.Dz[Cell[2]];
            String Out = Scp1730Layout.Facings[Cell[2]];
            String Desk = Scp1730Blocks.Mod + "metal_desk[facing=" + Out + ",left=false,right=false]";
            if (!R.free(X, F + 1, Z) || !R.free(X, F + 2, Z) || !G.empty(Ix, F + 1, Iz) || !G.empty(Ix, F + 2, Iz) || !B.standable(Ix, F, Iz) || !B.standable(X, F, Z)
                    || Scp1730Sectors.nearDoor(R, X, Z, 1) || busy(X, Z) || signBeside(X, F + 2, Z)) {
                continue;
            }
            boolean Crowded = false;
            for (int Dir = 0; Dir < 4; Dir++) {
                String Side = G.get(X + Scp1730Layout.Dx[Dir], F + 1, Z + Scp1730Layout.Dz[Dir]);
                Crowded |= Side != null && Scp1730SupportCheck.Joined.contains(shortId(Side));
            }
            if (Crowded) {
                continue;
            }
            set(X, F + 1, Z, Desk);
            if (!Scp1730RoomDressing.joined(G::get, X, F + 1, Z, Desk).equals(Desk)) {
                set(X, F + 1, Z, null);
                continue;
            }
            set(X, F + 2, Z, Pickup + "[facing=" + Out + ",item=" + Scp1730Puzzles.Items[Item] + "]");
            fixture("pickup", X, F + 2, Z, Out, Scp1730Puzzles.pickup(Item, Index));
            Used.add(Scp1730Builder.key(X, 0, Z));
            clear(Ix, F + 1, Iz);
            clear(Ix, F + 2, Iz);
            Targets.add(new Target(X, F + 1, Z, Mode));
            return true;
        }
        return false;
    }

    boolean pickupIn(List<Scp1730Layout.Room> Options, int Item, int Index, int Mode) {
        for (Scp1730Layout.Room Each : Options) {
            if (pickup(Each, Item, Index, Mode)) {
                site(Each);
                return true;
            }
        }
        return false;
    }

    boolean machine(Scp1730Layout.Room Room, int Role, String Block, String Props) {
        Scp1730Rooms.Box R = B.Rooms.new Box(Room, Zone);
        List<int[]> Cells = R.wallCells();
        for (int Pass = Cells.size() - 1; Pass > 0; Pass--) {
            int Swap = Rng.nextInt(Pass + 1);
            int[] Held = Cells.get(Pass);
            Cells.set(Pass, Cells.get(Swap));
            Cells.set(Swap, Held);
        }
        for (int[] Cell : Cells) {
            int X = Cell[0];
            int Z = Cell[1];
            int Ix = X + Scp1730Layout.Dx[Cell[2]];
            int Iz = Z + Scp1730Layout.Dz[Cell[2]];
            if (!R.free(X, F + 1, Z) || !R.free(X, F + 2, Z) || !G.empty(Ix, F + 1, Iz) || !G.empty(Ix, F + 2, Iz) || !B.standable(Ix, F, Iz) || !B.standable(X, F, Z)
                    || Scp1730Sectors.nearDoor(R, X, Z, 2) || busy(X, Z) || signBeside(X, F + 1, Z) || signBeside(X, F + 2, Z)) {
                continue;
            }
            String Out = Scp1730Layout.Facings[Cell[2]];
            set(X, F + 1, Z, Block + "[facing=" + Out + (Props.isEmpty() ? "" : "," + Props) + "]");
            fixture("pdevice", X, F + 1, Z, Out, Scp1730Puzzles.data(Role, 0));
            Used.add(Scp1730Builder.key(X, 0, Z));
            clear(Ix, F + 1, Iz);
            clear(Ix, F + 2, Iz);
            Targets.add(new Target(Ix, F + 1, Iz, 0));
            site(Room);
            return true;
        }
        return false;
    }

    Scp1730Fixture find(String Type, int Data) {
        for (Scp1730Fixture Each : B.Fixtures) {
            if (Each.Type().equals(Type) && Each.Zone() == Zone && Each.Data() == Data) {
                return Each;
            }
        }
        return null;
    }

    Scp1730Layout.Room roomAt(int X, int Z) {
        for (Scp1730Layout.Room Each : B.Layouts[Zone].Rooms) {
            if (X >= Each.Bx0 && X <= Each.Bx1 && Z >= Each.Bz0 && Z <= Each.Bz1) {
                return Each;
            }
        }
        return null;
    }

    void dropCard() {
        Scp1730Fixture Card = find("card", 0);
        if (Card != null) {
            set(Card.X(), Card.Y(), Card.Z(), Scp1730Blocks.PaperStack + "[facing=" + Card.Facing() + ",variant=" + Rng.nextInt(6) + "]");
            Dropped.add(Card);
        }
    }

    void dropRelease() {
        Scp1730Fixture Lever = find("control", 1);
        if (Lever != null) {
            set(Lever.X(), Lever.Y(), Lever.Z(), null);
            Dropped.add(Lever);
        }
    }

    boolean keycard() {
        Scp1730Fixture Card = find("card", 0);
        if (Card == null) {
            return false;
        }
        Targets.add(new Target(Card.X(), F + 1, Card.Z(), 0));
        Scp1730Layout.Room Home = roomAt(Card.X(), Card.Z());
        if (Home != null) {
            site(Home);
        }
        for (Scp1730Layout.Room Each : rooms(List.of("records", "storage", "small_office", "office", "conference", "comms", "breaker", "cafeteria", "elevator"), true)) {
            if (Each.Doors.size() != 1 || !quiet(Each)) {
                continue;
            }
            int Mark = Changes.size();
            int Pend = Pending.size();
            int Aim = Targets.size();
            if (lockRoom(Each) && pickup(Each, Scp1730Puzzles.ItemOverride, 0, 1)) {
                site(Each);
                return true;
            }
            while (Changes.size() > Mark) {
                Change Last = Changes.removeLast();
                G.set(Last.X(), Last.Y(), Last.Z(), Last.Old());
            }
            while (Pending.size() > Pend) {
                Pending.removeLast();
            }
            while (Targets.size() > Aim) {
                Targets.removeLast();
            }
            Locked.clear();
        }
        return false;
    }

    boolean quiet(Scp1730Layout.Room Room) {
        for (Scp1730Fixture Each : B.Fixtures) {
            if (Each.Zone() != Zone || Each.X() < Room.Bx0 || Each.X() > Room.Bx1 || Each.Z() < Room.Bz0 || Each.Z() > Room.Bz1) {
                continue;
            }
            String Type = Each.Type();
            if (Type.equals("control") || Type.equals("card") || Type.equals("biocontrol") || Type.equals("safe") || Type.equals("setpiece") || Type.equals("painter")) {
                return false;
            }
        }
        return true;
    }

    boolean lockRoom(Scp1730Layout.Room Room) {
        Scp1730Layout.Door Door = Room.Doors.get(0);
        int Cx = Room.doorX(Door);
        int Cz = Room.doorZ(Door);
        int Lx = Door.Dir % 2 == 0 ? 1 : 0;
        int Lz = Door.Dir % 2 == 0 ? 0 : 1;
        int Ox = Scp1730Layout.Dx[Door.Dir];
        int Oz = Scp1730Layout.Dz[Door.Dir];
        int[] Reader = doorSpot(Cx, Cz, Door.Dir);
        if (Reader == null) {
            return false;
        }
        String Facing = Scp1730Layout.Facings[Reader[2]];
        String Axis = Door.Dir % 2 == 0 ? "x" : "z";
        int Index = 0;
        for (int Side = -1; Side <= 1; Side++) {
            for (int Level = 1; Level <= 3; Level++) {
                int X = Cx + Lx * Side;
                int Z = Cz + Lz * Side;
                set(X, F + Level, Z, Scp1730Blocks.with(Scp1730Blocks.Seal, "axis=" + Axis));
                Locked.add(Scp1730Builder.key(X, F + Level, Z));
                fixture("pseal", X, F + Level, Z, Scp1730Layout.Facings[Door.Dir], Index++);
            }
        }
        set(Reader[0], F + 2, Reader[1], Scp1730Blocks.with(Scp1730Blocks.Control, "facing=" + Facing + ",kind=reader,active=false"));
        fixture("pdevice", Reader[0], F + 2, Reader[1], Facing, Scp1730Puzzles.data(Scp1730Puzzles.RoleLockerReader, 0));
        Used.add(Scp1730Builder.key(Reader[0], 0, Reader[1]));
        Targets.add(new Target(Reader[0], F + 1, Reader[1], 0));
        return true;
    }

    boolean fuses() {
        dropCard();
        if (!deviceIn(preferred(List.of("breaker"), Offices), Scp1730Puzzles.RoleJunction, 0, Junction, "fuses=0", 0)) {
            return false;
        }
        for (int Index = 0; Index < 3; Index++) {
            if (!pickupIn(rooms(Offices, true), Scp1730Puzzles.ItemFuse, Index, 0) && !pickupIn(rooms(Offices, false), Scp1730Puzzles.ItemFuse, Index, 0)) {
                return false;
            }
        }
        return true;
    }

    boolean override() {
        dropCard();
        int First = Rng.nextInt(Scp1730Puzzles.Glyphs);
        int Second = (First + 1 + Rng.nextInt(Scp1730Puzzles.Glyphs - 1)) % Scp1730Puzzles.Glyphs;
        if (!deviceIn(preferred(List.of("security"), Offices), Scp1730Puzzles.RoleTerminalA, First, Terminal, "accepted=false,glyph=0", 0)) {
            return false;
        }
        Scp1730Layout.Room Security = Last;
        if (!deviceIn(preferred(List.of("checkpoint"), Offices), Scp1730Puzzles.RoleTerminalB, Second, Terminal, "accepted=false,glyph=0", 0)) {
            return false;
        }
        if (!deviceIn(rooms(List.of("office", "records", "conference", "small_office", "cafeteria", "comms", "elevator", "storage", "breaker"), true), Scp1730Puzzles.RoleClueA, First,
                Clue, "clue=" + First + ",mark=0", 0)) {
            return false;
        }
        return Security != null && device(Security, Scp1730Puzzles.RoleClueB, Second, Clue, "clue=" + Second + ",mark=0", 0);
    }

    boolean dualKey() {
        dropCard();
        if (!pickupIn(preferred(List.of("security"), Offices), Scp1730Puzzles.ItemKey, 0, 0)) {
            return false;
        }
        if (!deviceIn(preferred(List.of("checkpoint"), Offices), Scp1730Puzzles.RoleKeyA, 0, Switch, "on=false,role=key", 0)) {
            return false;
        }
        Scp1730Fixture First = Pending.getLast();
        if (!deviceIn(rooms(Offices, true), Scp1730Puzzles.RoleKeyB, 0, Switch, "on=false,role=key", 0)) {
            return false;
        }
        Scp1730Fixture Second = Pending.getLast();
        Scp1730PuzzleFlood From = new Scp1730PuzzleFlood(source(), F - 2, F + 10, Set.of());
        int Ax = First.X() + Scp1730Layout.Dx[Scp1730Builder.dirOf(First.Facing())];
        int Az = First.Z() + Scp1730Layout.Dz[Scp1730Builder.dirOf(First.Facing())];
        From.flood(Ax + 0.5, F + 1, Az + 0.5);
        int Walk = From.distance(Second.X(), F + 1, Second.Z(), Near);
        if (Walk < 0) {
            return false;
        }
        int Seconds = Math.max(15, Math.min(60, (int) Math.ceil(Walk / WalkSpeed) + WindowMargin));
        Pending.set(Pending.size() - 1, new Scp1730Fixture("pdevice", Second.X(), Second.Y(), Second.Z(), Second.Facing(), Scp1730Puzzles.data(Scp1730Puzzles.RoleKeyB, Seconds), Zone));
        return true;
    }

    boolean vent() {
        dropRelease();
        if (!deviceIn(preferred(List.of("breaker"), Light), Scp1730Puzzles.RoleInterlock, 0, Switch, "on=false,role=interlock", 0)) {
            return false;
        }
        Scp1730PuzzleFlood Closed = flood(Set.of());
        List<int[]> Options = new ArrayList<>();
        for (int X = 4; X < Scp1730Plan.Width - 4; X++) {
            for (int Z = 4; Z < Scp1730Plan.Depth - 4; Z++) {
                if (!Scp1730Builder.isWallLike(G.get(X, F + 1, Z)) || !Scp1730Builder.isWallLike(G.get(X, F + 2, Z)) || inWing(X, Z)) {
                    continue;
                }
                for (int Dir = 0; Dir < 4; Dir++) {
                    int Fx = X - Scp1730Layout.Dx[Dir];
                    int Fz = Z - Scp1730Layout.Dz[Dir];
                    if (Closed.seen(Fx, F + 1, Fz) && G.empty(Fx, F + 2, Fz) && !B.cleared(Fx, F + 1, Fz)) {
                        Options.add(new int[]{X, Z, Dir});
                    }
                }
            }
        }
        for (int Index = Options.size() - 1; Index > 0; Index--) {
            int Swap = Rng.nextInt(Index + 1);
            int[] Held = Options.get(Index);
            Options.set(Index, Options.get(Swap));
            Options.set(Swap, Held);
        }
        for (int[] Each : Options) {
            for (int Length = MinShaft; Length <= MaxShaft; Length++) {
                if (fanRoom(Each[0], Each[1], Each[2], Length, true)) {
                    fanRoom(Each[0], Each[1], Each[2], Length, false);
                    return true;
                }
            }
        }
        return false;
    }

    boolean fanRoom(int Gx, int Gz, int Dir, int Length, boolean Probe) {
        int Ax = Scp1730Layout.Dx[Dir];
        int Az = Scp1730Layout.Dz[Dir];
        int Lx = Dir % 2 == 0 ? 1 : 0;
        int Lz = Dir % 2 == 0 ? 0 : 1;
        int Back = Length + 2 + FanDepth;
        if (Probe) {
            for (int Depth = 1; Depth <= Back + 1; Depth++) {
                int Half = Depth <= Length ? 2 : 3;
                for (int Lane = -Half; Lane <= Half; Lane++) {
                    int X = Gx + Ax * Depth + Lx * Lane;
                    int Z = Gz + Az * Depth + Lz * Lane;
                    if (X < 1 || Z < 1 || X >= Scp1730Plan.Width - 1 || Z >= Scp1730Plan.Depth - 1 || inWing(X, Z)) {
                        return false;
                    }
                    for (int Y = F - 1; Y <= F + 5; Y++) {
                        if (G.get(X, Y, Z) != null) {
                            return false;
                        }
                    }
                }
            }
            return G.get(Gx, F, Gz) != null && !Scp1730Blocks.passable(G.get(Gx, F, Gz));
        }
        Scp1730Blocks.Palette Look = Scp1730Blocks.Power;
        String Out = Scp1730Builder.facingOf(-Ax, -Az);
        String In = Scp1730Builder.facingOf(Ax, Az);
        set(Gx, F + 1, Gz, VentGrate + "[facing=" + Out + ",open=false]");
        fixture("pdevice", Gx, F + 1, Gz, Out, Scp1730Puzzles.data(Scp1730Puzzles.RoleVent, Length));
        Used.add(Scp1730Builder.key(Gx, 0, Gz));
        Targets.add(new Target(Gx - Ax, F + 1, Gz - Az, 0));
        for (int Depth = 1; Depth <= Length; Depth++) {
            for (int Lane = -1; Lane <= 1; Lane++) {
                int X = Gx + Ax * Depth + Lx * Lane;
                int Z = Gz + Az * Depth + Lz * Lane;
                set(X, F, Z, Look.Floor);
                set(X, F + 2, Z, Look.WallMiddle);
                set(X, F + 1, Z, Lane == 0 ? null : Look.WallBottom);
            }
            int X = Gx + Ax * Depth;
            int Z = Gz + Az * Depth;
            set(X, F, Z, Scp1730Blocks.Grate);
        }
        int Front = Length + 1;
        for (int Depth = Front; Depth <= Back; Depth++) {
            for (int Lane = -2; Lane <= 2; Lane++) {
                int X = Gx + Ax * Depth + Lx * Lane;
                int Z = Gz + Az * Depth + Lz * Lane;
                boolean Wall = Depth == Front || Depth == Back || Math.abs(Lane) == 2;
                set(X, F, Z, Wall ? Look.Floor : Lane == 0 ? Scp1730Blocks.Grate : Look.Floor);
                set(X, F + 4, Z, Look.Ceiling);
                for (int Level = 1; Level <= 3; Level++) {
                    set(X, F + Level, Z, Wall ? Look.wall(Level, 3) : null);
                }
            }
        }
        set(Gx + Ax * Front, F + 1, Gz + Az * Front, VentGrate + "[facing=" + In + ",open=true]");
        fixture("pdevice", Gx + Ax * Front, F + 1, Gz + Az * Front, In, Scp1730Puzzles.data(Scp1730Puzzles.RoleVentInner, Length));
        int Fan = Back - 1;
        int Sx = Gx + Ax * Fan;
        int Sz = Gz + Az * Fan;
        set(Sx, F + 2, Sz, Switch + "[facing=" + Out + ",on=true,role=fan]");
        fixture("pdevice", Sx, F + 2, Sz, Out, Scp1730Puzzles.data(Scp1730Puzzles.RoleFan, 0));
        Used.add(Scp1730Builder.key(Sx, 0, Sz));
        for (int Lane = -1; Lane <= 1; Lane += 2) {
            for (int Depth = Front + 2; Depth <= Front + 3; Depth++) {
                int Vx = Gx + Ax * Depth + Lx * Lane * 2;
                int Vz = Gz + Az * Depth + Lz * Lane * 2;
                set(Vx, F + 2, Vz, VentGrate + "[facing=" + Scp1730Builder.facingOf(-Lx * Lane, -Lz * Lane) + ",open=false]");
            }
            int Cx = Gx + Ax * (Front + 1) + Lx * Lane;
            int Cz = Gz + Az * (Front + 1) + Lz * Lane;
            set(Cx, F + 3, Cz, Scp1730Blocks.facing(Scp1730Blocks.EmergencyLight, Scp1730Builder.facingOf(-Lx * Lane, -Lz * Lane)));
        }
        set(Sx + Lx, F + 1, Sz + Lz, Scp1730Blocks.facing(Scp1730Blocks.Conduit, Out));
        set(Sx, F + 3, Sz, Scp1730Blocks.facing(Scp1730Blocks.SignVoltage, Out));
        set(Gx + Ax * (Front + 1), F + 3, Gz + Az * (Front + 1), Scp1730Blocks.facing(Scp1730Blocks.HangingCables, "down"));
        Targets.add(new Target(Sx - Ax, F + 1, Sz - Az, 2));
        return true;
    }

    boolean splice() {
        dropRelease();
        for (int Index = 0; Index < 2; Index++) {
            if (!pickupIn(rooms(Light, true), Scp1730Puzzles.ItemSplice, Index, 0)) {
                return false;
            }
        }
        if (!deviceIn(rooms(Light, true), Scp1730Puzzles.RoleCableA, 0, Cable, "fixed=false", 0)) {
            return false;
        }
        return deviceIn(preferred(List.of("holding_cell"), Light), Scp1730Puzzles.RoleCableB, 0, Cable, "fixed=false", 0);
    }

    boolean relays() {
        dropRelease();
        int Order = Rng.nextInt(Scp1730Puzzles.Orders.length);
        if (!deviceIn(preferred(List.of("test_chamber", "cell_block"), Light), Scp1730Puzzles.RoleChart, Order, Clue, "clue=" + (Scp1730Puzzles.ChartBase + Order) + ",mark=0", 0)) {
            return false;
        }
        String[] Roles = {"relay_red", "relay_yellow", "relay_blue"};
        int[] Codes = {Scp1730Puzzles.RoleRelayRed, Scp1730Puzzles.RoleRelayYellow, Scp1730Puzzles.RoleRelayBlue};
        for (int Index = 0; Index < 3; Index++) {
            if (!deviceIn(rooms(Light, true), Codes[Index], Order, Switch, "on=false,role=" + Roles[Index], 0)) {
                return false;
            }
        }
        return true;
    }

    boolean sample() {
        Scp1730Fixture Lever = find("control", 1);
        if (Lever == null) {
            return false;
        }
        Targets.add(new Target(Lever.X(), F + 1, Lever.Z(), 0));
        fixture("pdevice", Lever.X(), Lever.Y(), Lever.Z(), Lever.Facing(), Scp1730Puzzles.data(Scp1730Puzzles.RoleFlush, 0));
        Scp1730Layout.Room Chamber = roomAt(Lever.X(), Lever.Z());
        Used.add(Scp1730Builder.key(Lever.X(), 0, Lever.Z()));
        boolean Fitted = Chamber != null && device(Chamber, Scp1730Puzzles.RoleHatch, 0, Hatch, "loaded=false", 0);
        if (Chamber != null) {
            site(Chamber);
        }
        if (!Fitted && !deviceIn(rooms(Light, true), Scp1730Puzzles.RoleHatch, 0, Hatch, "loaded=false", 0)) {
            return false;
        }
        return pickupIn(preferred(List.of("medical", "research"), Light), Scp1730Puzzles.ItemSample, 0, 0);
    }

    boolean cells() {
        int[] Roles = {Scp1730Puzzles.RoleLockA, Scp1730Puzzles.RoleLockB, Scp1730Puzzles.RoleLockC};
        List<String> First = List.of("holding_cell");
        for (int Index = 0; Index < 3; Index++) {
            List<String> Wanted = Index < 2 ? First : List.of("heavy_cell");
            if (!deviceIn(preferred(Wanted, List.of("heavy_cell", "holding_cell", "armoury", "storage", "server", "collapsed")), Roles[Index], 0, Switch, "on=false,role=cell_lock", 0)) {
                return false;
            }
        }
        return true;
    }

    boolean generator() {
        boolean Placed = false;
        for (Scp1730Layout.Room Each : preferred(List.of("generator"), Heavy)) {
            if (machine(Each, Scp1730Puzzles.RoleGenerator, Generator, "fuel=0,running=false")) {
                Placed = true;
                break;
            }
        }
        if (!Placed) {
            return false;
        }
        for (int Index = 0; Index < 2; Index++) {
            if (!pickupIn(rooms(Heavy, true), Scp1730Puzzles.ItemFuel, Index, 0) && !pickupIn(rooms(Heavy, false), Scp1730Puzzles.ItemFuel, Index, 0)) {
                return false;
            }
        }
        return true;
    }

    boolean coolant() {
        if (!deviceIn(preferred(List.of(Scp1730Sectors.Reservoir), List.of()), Scp1730Puzzles.RoleInflow, 0, Valve, "high=true,open=true,role=inflow", 0)
                && !deviceIn(rooms(List.of("pump_room", "cooling"), true), Scp1730Puzzles.RoleInflow, 0, Valve, "high=true,open=true,role=inflow", 0)) {
            return false;
        }
        if (!deviceIn(preferred(List.of("pump_room", "cooling"), List.of("generator", "server", "storage", "incinerator", "armoury")), Scp1730Puzzles.RoleBypass, 0, Valve,
                "high=true,open=false,role=bypass", 0)) {
            return false;
        }
        return deviceIn(rooms(Heavy, true), Scp1730Puzzles.RoleDrain, 0, Valve, "high=true,open=false,role=drain", 0);
    }

    boolean powerCycle() {
        Scp1730Fixture Breaker = null;
        for (Scp1730Fixture Each : B.Fixtures) {
            if (Each.Type().equals("control") && Each.Zone() == Zone && Each.Data() == 2) {
                Scp1730Layout.Room Home = roomAt(Each.X(), Each.Z());
                if (Home != null && Home.Type.equals("generator")) {
                    Breaker = Each;
                }
            }
        }
        if (Breaker == null) {
            return false;
        }
        Targets.add(new Target(Breaker.X(), F + 1, Breaker.Z(), 0));
        fixture("pdevice", Breaker.X(), Breaker.Y(), Breaker.Z(), Breaker.Facing(), Scp1730Puzzles.data(Scp1730Puzzles.RoleBreaker, 0));
        Scp1730Layout.Room Home = roomAt(Breaker.X(), Breaker.Z());
        if (Home != null) {
            site(Home);
        }
        return deviceIn(preferred(List.of("server"), Heavy), Scp1730Puzzles.RoleController, 0, Switch, "on=false,role=controller", 0);
    }

    int[] hallSpot(boolean Floor) {
        Scp1730PuzzleFlood Closed = flood(Set.of());
        List<int[]> Options = new ArrayList<>();
        for (int X = 2; X < Scp1730Plan.Width - 2; X++) {
            for (int Z = 2; Z < Scp1730Plan.Depth - 2; Z++) {
                if (roomAt(X, Z) != null || busy(X, Z) || !Closed.seen(X, F + 1, Z) || inWing(X, Z) || Math.abs(X - Middle.X()) + Math.abs(Z - Middle.Z()) < 5
                        || signBeside(X, Floor ? F + 1 : F + 2, Z)) {
                    continue;
                }
                if (Floor ? !G.empty(X, F + 1, Z) || B.cleared(X, F + 1, Z) || !G.empty(X, F + 2, Z) : !G.empty(X, F + 2, Z) || B.cleared(X, F + 2, Z)) {
                    continue;
                }
                for (int Dir = 0; Dir < 4; Dir++) {
                    int Wx = X - Scp1730Layout.Dx[Dir];
                    int Wz = Z - Scp1730Layout.Dz[Dir];
                    int Fx = X + Scp1730Layout.Dx[Dir];
                    int Fz = Z + Scp1730Layout.Dz[Dir];
                    if (Scp1730Builder.isWallLike(G.get(Wx, F + 1, Wz)) && Scp1730Builder.isWallLike(G.get(Wx, F + 2, Wz)) && Closed.seen(Fx, F + 1, Fz)) {
                        Options.add(new int[]{X, Z, Dir});
                        break;
                    }
                }
            }
        }
        if (Options.isEmpty()) {
            return null;
        }
        for (int Index = Options.size() - 1; Index > 0; Index--) {
            int Swap = Rng.nextInt(Index + 1);
            int[] Held = Options.get(Index);
            Options.set(Index, Options.get(Swap));
            Options.set(Swap, Held);
        }
        int[] Best = null;
        int Far = -1;
        for (int Index = 0; Index < Math.min(Options.size(), 24); Index++) {
            int[] Each = Options.get(Index);
            int Gap = Integer.MAX_VALUE;
            for (int[] Site : Sites) {
                Gap = Math.min(Gap, Math.abs(Site[0] - Each[0]) + Math.abs(Site[1] - Each[1]));
            }
            if (Gap > Far) {
                Far = Gap;
                Best = Each;
            }
        }
        return Best;
    }

    boolean hallDevice(int Role, int Extra, String Block, String Props) {
        int[] Spot = hallSpot(false);
        if (Spot == null) {
            return false;
        }
        String Out = Scp1730Layout.Facings[Spot[2]];
        set(Spot[0], F + 2, Spot[1], Block + "[facing=" + Out + (Props.isEmpty() ? "" : "," + Props) + "]");
        fixture("pdevice", Spot[0], F + 2, Spot[1], Out, Scp1730Puzzles.data(Role, Extra));
        mark(Spot[0], Spot[1], Spot[2], 0);
        Sites.add(new int[]{Spot[0], Spot[1]});
        return true;
    }

    boolean hallMachine(int Role, int Extra, String Block, String Props) {
        int[] Spot = hallSpot(true);
        if (Spot == null) {
            return false;
        }
        String Out = Scp1730Layout.Facings[Spot[2]];
        set(Spot[0], F + 1, Spot[1], Block + "[facing=" + Out + (Props.isEmpty() ? "" : "," + Props) + "]");
        fixture("pdevice", Spot[0], F + 1, Spot[1], Out, Scp1730Puzzles.data(Role, Extra));
        Used.add(Scp1730Builder.key(Spot[0], 0, Spot[1]));
        int Fx = Spot[0] + Scp1730Layout.Dx[Spot[2]];
        int Fz = Spot[1] + Scp1730Layout.Dz[Spot[2]];
        clear(Fx, F + 1, Fz);
        clear(Fx, F + 2, Fz);
        Targets.add(new Target(Fx, F + 1, Fz, 0));
        Sites.add(new int[]{Spot[0], Spot[1]});
        return true;
    }

    boolean machineIn(List<Scp1730Layout.Room> Options, int Role, int Extra, String Block, String Props) {
        for (Scp1730Layout.Room Each : Options) {
            if (machine(Each, Role, Block, Props)) {
                Pending.set(Pending.size() - 1, retag(Pending.getLast(), Role, Extra));
                return true;
            }
        }
        return false;
    }

    Scp1730Fixture retag(Scp1730Fixture Old, int Role, int Extra) {
        return new Scp1730Fixture(Old.Type(), Old.X(), Old.Y(), Old.Z(), Old.Facing(), Scp1730Puzzles.data(Role, Extra), Zone);
    }

    boolean pylons() {
        int[] Pylons = {Scp1730Puzzles.RolePylonA, Scp1730Puzzles.RolePylonB, Scp1730Puzzles.RolePylonC};
        int[] Tablets = {Scp1730Puzzles.RoleTabletA, Scp1730Puzzles.RoleTabletB, Scp1730Puzzles.RoleTabletC};
        for (int Index = 0; Index < 3; Index++) {
            int Glyph = Rng.nextInt(Scp1730Puzzles.Glyphs);
            int Start = (Glyph + 1 + Rng.nextInt(Scp1730Puzzles.Glyphs - 1)) % Scp1730Puzzles.Glyphs;
            String Props = "glyph=" + Start + ",lit=false,mark=" + (Index + 1);
            if (!machineIn(rooms(Deep, true), Pylons[Index], Glyph, Pylon, Props) && !hallMachine(Pylons[Index], Glyph, Pylon, Props)) {
                return false;
            }
            String Tablet = "clue=" + Glyph + ",mark=" + (Index + 1);
            if (!deviceIn(rooms(Deep, true), Tablets[Index], Glyph, Clue, Tablet, 0) && !hallDevice(Tablets[Index], Glyph, Clue, Tablet)) {
                return false;
            }
        }
        return true;
    }

    boolean sluices() {
        int[] Roles = {Scp1730Puzzles.RoleSluiceA, Scp1730Puzzles.RoleSluiceB};
        for (int Role : Roles) {
            if (!deviceIn(rooms(Deep, true), Role, 0, Sluice, "open=false,turn=0", 0) && !hallDevice(Role, 0, Sluice, "open=false,turn=0")) {
                return false;
            }
        }
        return true;
    }
}
