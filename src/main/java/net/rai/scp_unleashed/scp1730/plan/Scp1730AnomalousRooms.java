package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class Scp1730AnomalousRooms {
    public static final String Silent = "silent";
    public static final String Television = "television";
    public static final String Loop = "loop";
    public static final String Vault = "vault";
    public static final String VaultHall = "vault_hall";
    public static final String Lightless = "lightless";
    public static final String Slow = "slow";
    public static final String Drift = "drift";
    public static final String Echo = "echo";
    public static final List<String> Kinds = List.of(Silent, Television, Loop, Vault, Lightless, Slow, Drift, Echo);
    public static final int[] Weights = {6, 6, 4, 4, 4, 4, 4, 4};
    public static final int NoneChance = 20;
    public static final int MinRooms = 2;
    public static final int MaxRooms = 4;
    public static final int PerZone = 2;
    public static final int DriftHeight = 6;
    public static final int TelevisionInset = 3;
    public static final int[][] HallSizes = {{25, 41}, {21, 35}, {14, 61}, {14, 45}};
    public static final int HallHeight = 12;
    public static final int StubDepth = 6;
    public static final long Salt = 0x1730A11CL;
    public static final Set<String> Convertible = Set.of("small_office", "storage", "research", "collapsed");
    public static final Set<String> Duplicable = Set.of("office", "medical", "dorms", "armoury", "cafeteria");

    private Scp1730AnomalousRooms() {
    }

    public static boolean anomalous(String Type) {
        return Kinds.contains(Type) || VaultHall.equals(Type);
    }

    static int[] zonesFor(String Kind) {
        return switch (Kind) {
            case Television, Vault, Slow, Drift -> new int[]{1, 2};
            default -> new int[]{0, 1, 2};
        };
    }

    static boolean shapeFits(String Kind, Scp1730Layout.Room Made) {
        return switch (Kind) {
            case Television, Silent -> Made.W == 1 && Made.D == 1;
            case Vault -> Made.W == 1 && Made.D == 1 && Made.Doors.size() == 1;
            case Loop -> Made.Doors.size() >= 2 && (Made.W == 1 || Made.D == 1) && distinctSides(Made);
            default -> Made.W == 1 || Made.D == 1;
        };
    }

    static boolean distinctSides(Scp1730Layout.Room Made) {
        for (Scp1730Layout.Door First : Made.Doors) {
            for (Scp1730Layout.Door Second : Made.Doors) {
                if (First.Dir != Second.Dir) {
                    return true;
                }
            }
        }
        return false;
    }

    static void assign(Scp1730Builder B) {
        if (B.Fallback) {
            return;
        }
        Random Pick = new Random(Scp1730Builder.mix(B.Seed ^ Salt, 17));
        if (Pick.nextInt(100) < NoneChance) {
            return;
        }
        int Wanted = MinRooms + Pick.nextInt(MaxRooms - MinRooms + 1);
        List<String> Order = new ArrayList<>();
        List<String> Pool = new ArrayList<>(Kinds);
        List<Integer> Odds = new ArrayList<>();
        for (int Weight : Weights) {
            Odds.add(Weight);
        }
        while (!Pool.isEmpty()) {
            int Total = 0;
            for (int Each : Odds) {
                Total += Each;
            }
            int Roll = Pick.nextInt(Total);
            int Index = 0;
            while (Roll >= Odds.get(Index)) {
                Roll -= Odds.get(Index);
                Index++;
            }
            Order.add(Pool.remove(Index));
            Odds.remove(Index);
        }
        int[] PerZoneCount = new int[4];
        int Placed = 0;
        for (String Kind : Order) {
            if (Placed >= Wanted) {
                break;
            }
            int[] Zones = zonesFor(Kind).clone();
            for (int Index = Zones.length - 1; Index > 0; Index--) {
                int Swap = Pick.nextInt(Index + 1);
                int Held = Zones[Index];
                Zones[Index] = Zones[Swap];
                Zones[Swap] = Held;
            }
            for (int Zone : Zones) {
                if (PerZoneCount[Zone] >= PerZone) {
                    continue;
                }
                Scp1730Layout Plan = B.Layouts[Zone];
                List<Scp1730Layout.Room> Options = candidates(Plan, Kind);
                if (Options.isEmpty()) {
                    continue;
                }
                Scp1730Layout.Room Chosen = Options.get(Pick.nextInt(Options.size()));
                Scp1730Layout.Room Made = retype(Plan, Chosen, Kind);
                if (Kind.equals(Drift)) {
                    Made.Height = DriftHeight;
                }
                if (Kind.equals(Television)) {
                    Made.Bx0 = Made.x0() + TelevisionInset;
                    Made.Bz0 = Made.z0() + TelevisionInset;
                    Made.Bx1 = Made.x1() - TelevisionInset - 1;
                    Made.Bz1 = Made.z1() - TelevisionInset - 1;
                }
                PerZoneCount[Zone]++;
                Placed++;
                break;
            }
        }
    }

    static List<Scp1730Layout.Room> candidates(Scp1730Layout Plan, String Kind) {
        Map<String, Integer> Counts = new HashMap<>();
        for (Scp1730Layout.Room Each : Plan.Rooms) {
            Counts.merge(Each.Type, 1, Integer::sum);
        }
        List<Scp1730Layout.Room> Found = new ArrayList<>();
        for (Scp1730Layout.Room Each : Plan.Rooms) {
            if (Each == Plan.Entry || Each == Plan.Exit || Each.Type.startsWith("stair")) {
                continue;
            }
            boolean Spare = Convertible.contains(Each.Type) || Duplicable.contains(Each.Type) && Counts.getOrDefault(Each.Type, 0) >= 2;
            if (!Spare || !shapeFits(Kind, Each) || !spare(Plan, Each)) {
                continue;
            }
            Found.add(Each);
        }
        return Found;
    }

    static Scp1730Layout.Room retype(Scp1730Layout Plan, Scp1730Layout.Room Old, String Kind) {
        Scp1730Layout.Room Made = new Scp1730Layout.Room(Kind, Old.Sx, Old.Sz, Old.W, Old.D);
        Made.Doors.addAll(Old.Doors);
        Made.Floor = Old.Floor;
        Made.Height = Old.Height;
        Made.Id = Old.Id;
        Made.Bx0 = Old.Bx0;
        Made.Bz0 = Old.Bz0;
        Made.Bx1 = Old.Bx1;
        Made.Bz1 = Old.Bz1;
        Plan.Rooms.set(Old.Id, Made);
        return Made;
    }

    static boolean spare(Scp1730Layout Plan, Scp1730Layout.Room Without) {
        int Slots = Scp1730Layout.Slots;
        int Nodes = Slots * Slots + Plan.Rooms.size();
        boolean[] Seen = new boolean[Nodes];
        ArrayDeque<Integer> Queue = new ArrayDeque<>();
        int Start = Slots * Slots + Plan.Entry.Id;
        Seen[Start] = true;
        Queue.add(Start);
        while (!Queue.isEmpty()) {
            int Node = Queue.poll();
            if (Node >= Slots * Slots) {
                Scp1730Layout.Room Here = Plan.Rooms.get(Node - Slots * Slots);
                if (Here == Without) {
                    continue;
                }
                for (Scp1730Layout.Door Each : Here.Doors) {
                    int Next = (Each.Sx + Scp1730Layout.Dx[Each.Dir]) * Slots + Each.Sz + Scp1730Layout.Dz[Each.Dir];
                    if (!Seen[Next]) {
                        Seen[Next] = true;
                        Queue.add(Next);
                    }
                }
                continue;
            }
            int X = Node / Slots;
            int Z = Node % Slots;
            for (int Dir = 0; Dir < 4; Dir++) {
                if (!Plan.Links[X][Z][Dir]) {
                    continue;
                }
                int Nx = X + Scp1730Layout.Dx[Dir];
                int Nz = Z + Scp1730Layout.Dz[Dir];
                if (Nx < 0 || Nz < 0 || Nx >= Slots || Nz >= Slots) {
                    continue;
                }
                int Next;
                if (Plan.Owner[Nx][Nz] >= 0 && !Plan.Corridor[Nx][Nz]) {
                    Scp1730Layout.Room Target = Plan.Rooms.get(Plan.Owner[Nx][Nz]);
                    boolean HasDoor = false;
                    for (Scp1730Layout.Door Each : Target.Doors) {
                        if (Each.Sx == Nx && Each.Sz == Nz && Each.Dir == (Dir + 2) % 4) {
                            HasDoor = true;
                        }
                    }
                    if (!HasDoor) {
                        continue;
                    }
                    Next = Slots * Slots + Plan.Owner[Nx][Nz];
                } else {
                    Next = Nx * Slots + Nz;
                }
                if (!Seen[Next]) {
                    Seen[Next] = true;
                    Queue.add(Next);
                }
            }
        }
        for (Scp1730Layout.Room Each : Plan.Rooms) {
            if (Each != Without && !Seen[Slots * Slots + Each.Id]) {
                return false;
            }
        }
        return true;
    }

    public static Scp1730Blocks.Palette palette(String Type, int Zone, Scp1730Blocks.Palette Fallback) {
        return switch (Type) {
            case Silent, Vault, Television, VaultHall -> Scp1730Blocks.Office;
            case Slow -> Scp1730Blocks.Research;
            case Drift -> Scp1730Blocks.Power;
            default -> Fallback;
        };
    }

    static boolean furnish(Scp1730Builder B, Scp1730Rooms.Box R) {
        switch (R.Room.Type) {
            case Silent -> silent(B, R);
            case Television -> television(B, R);
            case Loop -> loop(B, R);
            case Vault -> vault(B, R);
            case Lightless -> lightless(B, R);
            case Slow -> slow(B, R);
            case Drift -> drift(B, R);
            case Echo -> echo(B, R);
            default -> {
                return false;
            }
        }
        return true;
    }

    static boolean backed(Scp1730Builder B, int X, int Y, int Z, String Facing) {
        int Dir = Scp1730Builder.dirOf(Facing);
        String Behind = B.Grid.get(X - Scp1730Layout.Dx[Dir], Y, Z - Scp1730Layout.Dz[Dir]);
        return Behind != null && !Scp1730Blocks.passable(Behind);
    }

    static String face(int Dir) {
        return Scp1730Layout.Facings[(Dir % 4 + 4) % 4];
    }

    static String mod(String Name) {
        return Scp1730Blocks.Mod + Name;
    }

    static void reskin(Scp1730Builder B, Scp1730Rooms.Box R, String Wall, String Floor, String Ceiling) {
        Scp1730Layout.Room Made = R.Room;
        for (int X = Made.Bx0; X <= Made.Bx1; X++) {
            for (int Z = Made.Bz0; Z <= Made.Bz1; Z++) {
                boolean Edge = X == Made.Bx0 || X == Made.Bx1 || Z == Made.Bz0 || Z == Made.Bz1;
                if (Edge && Wall != null) {
                    for (int Level = 1; Level <= R.H; Level++) {
                        String Here = B.Grid.get(X, R.F + Level, Z);
                        if (Scp1730Builder.isWallLike(Here) || Here != null && Here.endsWith("_wall_top")) {
                            B.Grid.set(X, R.F + Level, Z, Wall);
                        }
                    }
                }
                if (!Edge && Floor != null && R.Look.Floor.equals(B.Grid.get(X, R.F, Z))) {
                    B.Grid.set(X, R.F, Z, Floor);
                }
                if (Ceiling != null && R.Look.Ceiling.equals(B.Grid.get(X, R.F + R.H + 1, Z))) {
                    B.Grid.set(X, R.F + R.H + 1, Z, Ceiling);
                }
            }
        }
    }

    static int[] backWall(Scp1730Rooms.Box R) {
        Scp1730Layout.Door Main = R.door();
        boolean[] Taken = new boolean[4];
        for (Scp1730Layout.Door Each : R.Room.Doors) {
            Taken[Each.Dir] = true;
        }
        int Back = (Main.Dir + 2) % 4;
        if (Taken[Back]) {
            for (int Turn = 1; Turn < 4; Turn += 2) {
                if (!Taken[(Main.Dir + Turn) % 4]) {
                    Back = (Main.Dir + Turn) % 4;
                    break;
                }
            }
        }
        int Into = (Back + 2) % 4;
        int X = Back == 1 ? R.X1 : Back == 3 ? R.X0 : R.cx();
        int Z = Back == 2 ? R.Z1 : Back == 0 ? R.Z0 : R.cz();
        return new int[]{X, Z, Into};
    }

    static String foam(String Facing) {
        return mod("scp1730_acoustic_foam[facing=" + Facing + "]");
    }

    static void silent(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        Scp1730Layout.Room Made = R.Room;
        for (int X = Made.Bx0; X <= Made.Bx1; X++) {
            for (int Z = Made.Bz0; Z <= Made.Bz1; Z++) {
                String Inward = X == Made.Bx0 ? "east" : X == Made.Bx1 ? "west" : Z == Made.Bz0 ? "south" : Z == Made.Bz1 ? "north" : null;
                if (Inward != null) {
                    for (int Level = 1; Level <= R.H; Level++) {
                        String Here = B.Grid.get(X, R.F + Level, Z);
                        if (Scp1730Builder.isWallLike(Here) || Here != null && Here.endsWith("_wall_top")) {
                            B.Grid.set(X, R.F + Level, Z, foam(Inward));
                        }
                    }
                } else {
                    if (R.Look.Ceiling.equals(B.Grid.get(X, R.F + R.H + 1, Z))) {
                        B.Grid.set(X, R.F + R.H + 1, Z, foam("down"));
                    }
                    if (R.Look.Floor.equals(B.Grid.get(X, R.F, Z))) {
                        B.Grid.set(X, R.F, Z, mod("facility_rubber_matting"));
                    }
                }
            }
        }
        int[] Back = backWall(R);
        String Toward = face(Back[2]);
        int Lx = Back[2] % 2 == 0 ? 1 : 0;
        int Lz = Back[2] % 2 == 0 ? 0 : 1;
        int Dx = Scp1730Layout.Dx[Back[2]];
        int Dz = Scp1730Layout.Dz[Back[2]];
        for (int Side = -1; Side <= 1; Side++) {
            int X = Back[0] + Lx * Side;
            int Z = Back[1] + Lz * Side;
            if (R.put(X, R.F + 1, Z, mod("metal_desk[facing=" + Toward + ",left=" + (Side > -1) + ",right=" + (Side < 1) + "]"))) {
                String Top = Side == 0 ? mod("computer_terminal[facing=" + Toward + ",lit=false,screen=" + Rng.nextInt(10) + "]")
                        : Side < 0 ? mod("lab_analyzer[facing=" + Toward + ",running=false]") : mod("paper_stack[facing=" + Toward + ",variant=" + Rng.nextInt(6) + "]");
                R.put(X, R.F + 2, Z, Top);
            }
        }
        R.put(Back[0] + Dx, R.F + 1, Back[1] + Dz, mod("office_chair[facing=" + face(Back[2] + 2) + "]"));
        for (int Side = -1; Side <= 1; Side += 2) {
            R.put(Back[0] + Lx * Side * 3, R.F + 1, Back[1] + Lz * Side * 3, mod("scp1730_studio_monitor[facing=" + Toward + "]"));
        }
        int Cx = R.cx();
        int Cz = R.cz();
        String Away = face(Back[2] + 2);
        if (B.Grid.empty(Cx, R.F + 1, Cz)) {
            B.Grid.set(Cx, R.F + 1, Cz, mod("scp1730_mic_stand[facing=" + Away + "]"));
        }
        for (int Side = -1; Side <= 1; Side += 2) {
            int X = Cx - Dx * 3 + Lx * Side * 2;
            int Z = Cz - Dz * 3 + Lz * Side * 2;
            R.put(X, R.F + 1, Z, mod("scp1730_speaker_cabinet[facing=" + Toward + "]"));
        }
        for (int Step = 1; Step <= 3; Step++) {
            int X = Cx + Dx * Step;
            int Z = Cz + Dz * Step;
            if (Step == 2) {
                R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.CableTangle, face(Back[2] + 1)));
            }
            if (B.Grid.empty(X, R.F + R.H, Z)) {
                B.Grid.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Scp1730Blocks.CableTray, "down"));
            }
        }
        if (B.Grid.empty(Cx, R.F + R.H, Cz)) {
            B.Grid.set(Cx, R.F + R.H, Cz, mod("fluorescent_light[face=ceiling,facing=" + (Lx == 1 ? "east" : "north") + ",lit=true]"));
        }
        for (int Corner = 0; Corner < 2; Corner++) {
            int X = Corner == 0 ? R.X0 + 1 : R.X1 - 1;
            int Z = Corner == 0 ? R.Z0 + 1 : R.Z1 - 1;
            if (B.Grid.empty(X, R.F + R.H, Z)) {
                B.Grid.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Scp1730Blocks.EmergencyLight, "down"));
            }
        }
        R.put(Cx - Lx * 2 - Dx, R.F + 1, Cz - Lz * 2 - Dz, Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Back[2] + 1)));
        R.put(Cx + Lx * 3, R.F + 1, Cz + Lz * 3, mod("paper_stack[facing=" + face(Rng.nextInt(4)) + ",variant=" + Rng.nextInt(6) + "]"));
        Scp1730Layout.Door Main = R.door();
        int Ix = -Scp1730Layout.Dx[Main.Dir];
        int Iz = -Scp1730Layout.Dz[Main.Dir];
        for (int Step = 1; Step <= 3; Step++) {
            int X = R.Room.doorX(Main) + Ix * Step;
            int Z = R.Room.doorZ(Main) + Iz * Step;
            if (Step % 2 == 1 && B.Grid.empty(X, R.F + 1, Z) && !B.cleared(X, R.F + 1, Z)) {
                B.Grid.set(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.DragMarks, "up"));
            }
        }
    }

    static void television(Scp1730Builder B, Scp1730Rooms.Box R) {
        reskin(B, R, null, mod("facility_carpet_maroon"), null);
        int[] Back = backWall(R);
        String Toward = face(Back[2]);
        int Lx = Back[2] % 2 == 0 ? 1 : 0;
        int Lz = Back[2] % 2 == 0 ? 0 : 1;
        for (int Shift : new int[]{0, 1, -1, 2, -2}) {
            int X = Back[0] + Lx * Shift;
            int Z = Back[1] + Lz * Shift;
            if (!R.free(X, R.F + 1, Z) || !R.free(X, R.F + 2, Z)) {
                continue;
            }
            B.Grid.set(X, R.F + 1, Z, mod("conference_table[north=false,east=false,south=false,west=false]"));
            B.Grid.set(X, R.F + 2, Z, mod("scp1730_television[facing=" + Toward + ",screen=off]"));
            B.fixture("television", X, R.F + 2, Z, Toward, 0, R.Zone);
            B.keepClear(X + Scp1730Layout.Dx[Back[2]], R.F + 1, Z + Scp1730Layout.Dz[Back[2]]);
            break;
        }
        if (B.Grid.empty(R.cx(), R.F + R.H, R.cz())) {
            B.Grid.set(R.cx(), R.F + R.H, R.cz(), Scp1730Blocks.facing(Scp1730Blocks.BrokenLight, "down"));
        }
        for (int Index = 0; Index < 2; Index++) {
            B.decal(R.X0 + B.Rng.nextInt(R.X1 - R.X0 + 1), R.F, R.Z0 + B.Rng.nextInt(R.Z1 - R.Z0 + 1), "up", Scp1730Builder.Grime, R.Zone);
        }
    }

    static void loop(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        boolean AlongX = R.X1 - R.X0 >= R.Z1 - R.Z0;
        for (int Along = (AlongX ? R.X0 : R.Z0) + 2; Along <= (AlongX ? R.X1 : R.Z1) - 2; Along += 4) {
            for (int Side = -1; Side <= 1; Side += 2) {
                int X = AlongX ? Along : (Side < 0 ? R.X0 : R.X1);
                int Z = AlongX ? (Side < 0 ? R.Z0 : R.Z1) : Along;
                String Out = AlongX ? (Side < 0 ? "south" : "north") : (Side < 0 ? "east" : "west");
                if (R.put(X, R.F + 1, Z, mod("ez_waiting_chairs[facing=" + Out + "]")) && backed(B, X, R.F + 3, Z, Out)) {
                    R.put(X, R.F + 3, Z, mod("wall_clock[facing=" + Out + "]"));
                }
            }
            int Cx = AlongX ? Along : R.cx();
            int Cz = AlongX ? R.cz() : Along;
            if (B.Grid.empty(Cx, R.F + R.H, Cz) && B.Grid.get(Cx, R.F + R.H + 1, Cz) != null) {
                B.Grid.set(Cx, R.F + R.H, Cz, mod("fluorescent_light[face=ceiling,facing=" + (AlongX ? "east" : "north") + ",lit=true]"));
            }
        }
        for (Scp1730Layout.Door Each : R.Room.Doors) {
            int Cx = R.Room.doorX(Each) - Scp1730Layout.Dx[Each.Dir];
            int Cz = R.Room.doorZ(Each) - Scp1730Layout.Dz[Each.Dir];
            B.Grid.set(Cx, R.F, Cz, R.Look.marked(face(Each.Dir + 2)));
            B.decalExact(R.Room.doorX(Each) + (Each.Dir % 2 == 0 ? 2 : 0), R.F + 2, R.Room.doorZ(Each) + (Each.Dir % 2 == 0 ? 0 : 2), face(Each.Dir + 2), Scp1730Builder.ArrowDecal, 1 + Each.Dir % 4, R.Zone);
        }
        for (int Index = 0; Index < 4; Index++) {
            B.decal(R.X0 + Rng.nextInt(R.X1 - R.X0 + 1), R.F, R.Z0 + Rng.nextInt(R.Z1 - R.Z0 + 1), "up", Scp1730Builder.Footprints, R.Zone);
        }
    }

    static void vault(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        List<int[]> Cells = R.wallCells();
        for (int[] Cell : Cells) {
            String Out = face(Cell[2]);
            int Along = Cell[2] % 2 == 0 ? Cell[0] : Cell[1];
            if (Along % 4 == 1 && R.free(Cell[0], R.F + 1, Cell[1]) && R.free(Cell[0], R.F + 2, Cell[1]) && R.free(Cell[0], R.F + 3, Cell[1])) {
                B.Grid.set(Cell[0], R.F + 1, Cell[1], mod("office_records_cabinet_bottom[facing=" + Out + "]"));
                B.Grid.set(Cell[0], R.F + 2, Cell[1], mod("office_records_cabinet_middle[facing=" + Out + "]"));
                B.Grid.set(Cell[0], R.F + 3, Cell[1], mod("office_records_cabinet_top[facing=" + Out + "]"));
            } else if (Along % 4 == 3) {
                R.put(Cell[0], R.F + 1, Cell[1], mod("archive_boxes[facing=" + Out + "]"));
            } else if (Rng.nextBoolean()) {
                R.put(Cell[0], R.F + 1, Cell[1], mod("binder_shelf[facing=" + Out + "]"));
            }
        }
        R.put(R.cx(), R.F + 1, R.cz(), mod("office_desk[facing=north,left=false,right=false]"));
        R.put(R.cx(), R.F + 2, R.cz(), mod("desk_lamp[facing=north,lit=false]"));
        R.put(R.cx(), R.F + 1, R.cz() + 1, mod("office_chair[facing=north]"));
        R.lights(5);
        hall(B, R);
    }

    static void hall(Scp1730Builder B, Scp1730Rooms.Box R) {
        Scp1730Layout.Door Main = R.door();
        int Zone = R.Zone;
        int Floor = Scp1730Builder.Floors[Zone];
        int Top = Floor + HallHeight + 1;
        if (Top + 4 > Scp1730Grid.MaxY) {
            return;
        }
        int[][] Sum = new int[Scp1730Plan.Width + 1][Scp1730Plan.Depth + 1];
        for (int X = 0; X < Scp1730Plan.Width; X++) {
            for (int Z = 0; Z < Scp1730Plan.Depth; Z++) {
                int Busy = 0;
                for (int Y = Floor - 3; Y <= Top + 3; Y++) {
                    if (!B.Grid.empty(X, Y, Z)) {
                        Busy = 1;
                        break;
                    }
                }
                Sum[X + 1][Z + 1] = Busy + Sum[X][Z + 1] + Sum[X + 1][Z] - Sum[X][Z];
            }
        }
        int Cx = (R.Room.Bx0 + R.Room.Bx1) / 2;
        int Cz = (R.Room.Bz0 + R.Room.Bz1) / 2;
        for (int[] Size : HallSizes) {
            int[] Best = null;
            long BestScore = Long.MAX_VALUE;
            for (int Dir = 0; Dir < 4; Dir++) {
                boolean Long = Dir % 2 == 0;
                int Wx = Long ? Size[0] : Size[1];
                int Wz = Long ? Size[1] : Size[0];
                for (int X0 = 1; X0 + Wx + 1 < Scp1730Plan.Width; X0++) {
                    for (int Z0 = 1; Z0 + Wz + 1 < Scp1730Plan.Depth; Z0++) {
                        int X1 = X0 + Wx - 1;
                        int Z1 = Z0 + Wz - 1;
                        int Dx = (X0 + X1) / 2;
                        int Dz = (Z0 + Z1) / 2;
                        int DoorX = Dir == 1 ? X1 : Dir == 3 ? X0 : Dx;
                        int DoorZ = Dir == 2 ? Z1 : Dir == 0 ? Z0 : Dz;
                        int Sx0 = Math.min(X0, DoorX + Scp1730Layout.Dx[Dir] * StubDepth - 3);
                        int Sx1 = Math.max(X1, DoorX + Scp1730Layout.Dx[Dir] * StubDepth + 3);
                        int Sz0 = Math.min(Z0, DoorZ + Scp1730Layout.Dz[Dir] * StubDepth - 3);
                        int Sz1 = Math.max(Z1, DoorZ + Scp1730Layout.Dz[Dir] * StubDepth + 3);
                        if (Sx0 < 1 || Sz0 < 1 || Sx1 >= Scp1730Plan.Width - 1 || Sz1 >= Scp1730Plan.Depth - 1) {
                            continue;
                        }
                        int Ax = Sx0 - 1;
                        int Az = Sz0 - 1;
                        int Bx = Sx1 + 2;
                        int Bz = Sz1 + 2;
                        if (Sum[Bx][Bz] - Sum[Ax][Bz] - Sum[Bx][Az] + Sum[Ax][Az] != 0) {
                            continue;
                        }
                        long Score = (long) (Dx - Cx) * (Dx - Cx) + (long) (Dz - Cz) * (Dz - Cz) + (Dir == Main.Dir ? 0 : 40);
                        if (Score < BestScore) {
                            BestScore = Score;
                            Best = new int[]{X0, Z0, X1, Z1, Dir, DoorX, DoorZ};
                        }
                    }
                }
            }
            if (Best != null) {
                buildHall(B, Zone, Floor, Best);
                return;
            }
        }
    }

    static void buildHall(Scp1730Builder B, int Zone, int Floor, int[] Box) {
        Scp1730Grid G = B.Grid;
        Random Rng = B.Rng;
        int X0 = Box[0];
        int Z0 = Box[1];
        int X1 = Box[2];
        int Z1 = Box[3];
        int Dir = Box[4];
        int DoorX = Box[5];
        int DoorZ = Box[6];
        Scp1730Blocks.Palette Look = Scp1730Blocks.Office;
        int H = HallHeight;
        for (int X = X0; X <= X1; X++) {
            for (int Z = Z0; Z <= Z1; Z++) {
                G.set(X, Floor, Z, Look.Floor);
                G.set(X, Floor + H + 1, Z, Look.Ceiling);
                boolean Edge = X == X0 || X == X1 || Z == Z0 || Z == Z1;
                if (Edge) {
                    for (int Level = 1; Level <= H; Level++) {
                        G.set(X, Floor + Level, Z, Look.wall(Level, H));
                    }
                }
            }
        }
        int Lx = Dir % 2 == 0 ? 1 : 0;
        int Lz = Dir % 2 == 0 ? 0 : 1;
        int Ox = Scp1730Layout.Dx[Dir];
        int Oz = Scp1730Layout.Dz[Dir];
        Scp1730Blocks.Palette Hall = B.corridorPalette(Zone);
        for (int Step = 0; Step <= StubDepth; Step++) {
            for (int Side = -2; Side <= 2; Side++) {
                int X = DoorX + Ox * Step + Lx * Side;
                int Z = DoorZ + Oz * Step + Lz * Side;
                if (Step == 0 && Math.abs(Side) == 2) {
                    continue;
                }
                G.set(X, Floor, Z, Step == 0 ? Look.marked(face(Dir)) : Hall.Floor);
                G.set(X, Floor + 4, Z, Step == 0 ? Look.wall(4, H) : Hall.Ceiling);
                for (int Level = 1; Level <= 3; Level++) {
                    boolean Wall = Math.abs(Side) == 2 || Step == StubDepth;
                    G.set(X, Floor + Level, Z, Wall ? Hall.wall(Level, 3) : null);
                }
            }
        }
        for (int Side = -1; Side <= 1; Side++) {
            G.set(DoorX + Lx * Side - Ox, Floor, DoorZ + Lz * Side - Oz, Look.marked(face(Dir + 2)));
        }
        int Ix = -Ox;
        int Iz = -Oz;
        int Depth = Dir % 2 == 0 ? Z1 - Z0 - 1 : X1 - X0 - 1;
        int Span = Dir % 2 == 0 ? X1 - X0 - 1 : Z1 - Z0 - 1;
        int Half = Span / 2;
        for (int Row = 3; Row < Depth - 3; Row += 3) {
            for (int Across = -Half; Across <= Half; Across++) {
                if (Math.abs(Across) <= 2 || Math.abs(Across) >= Half) {
                    continue;
                }
                int X = DoorX + Ix * Row + Lx * Across;
                int Z = DoorZ + Iz * Row + Lz * Across;
                String Out = face(Dir);
                boolean Gap = Rng.nextInt(9) == 0;
                if (Gap) {
                    if (Rng.nextBoolean()) {
                        G.set(X, Floor + 1, Z, mod("archive_boxes[facing=" + face(Rng.nextInt(4)) + "]"));
                    }
                    continue;
                }
                int Stacks = 2 + (Rng.nextInt(4) == 0 ? 1 : 0);
                for (int Stack = 0; Stack < Stacks; Stack++) {
                    int Base = Floor + 1 + Stack * 3;
                    if (Base + 2 > Floor + H - 1) {
                        break;
                    }
                    G.set(X, Base, Z, mod("office_records_cabinet_bottom[facing=" + Out + "]"));
                    G.set(X, Base + 1, Z, mod("office_records_cabinet_middle[facing=" + Out + "]"));
                    G.set(X, Base + 2, Z, mod("office_records_cabinet_top[facing=" + Out + "]"));
                }
            }
            if (Row % 6 == 0) {
                for (int Across = -1; Across <= 1; Across += 2) {
                    int X = DoorX + Ix * Row + Lx * Across;
                    int Z = DoorZ + Iz * Row + Lz * Across;
                    String Lamp = Rng.nextInt(4) == 0 ? Scp1730Blocks.facing(Scp1730Blocks.DeadLight, "down") : mod("fluorescent_light[face=ceiling,facing=" + (Lx == 1 ? "north" : "east") + ",lit=true]");
                    G.set(X, Floor + H, Z, Lamp);
                }
            }
        }
        int EndX = DoorX + Ix * (Depth - 1);
        int EndZ = DoorZ + Iz * (Depth - 1);
        String Back = face(Dir);
        G.set(EndX, Floor + 1, EndZ, mod("office_desk[facing=" + Back + ",left=false,right=false]"));
        G.set(EndX, Floor + 2, EndZ, mod("desk_lamp[facing=" + Back + ",lit=true]"));
        G.set(EndX + Ox, Floor + 1, EndZ + Oz, mod("office_chair[facing=" + face(Dir + 2) + "]"));
        G.set(EndX + Lx * 2, Floor + 1, EndZ + Lz * 2, mod("paper_stack[facing=" + Back + ",variant=" + Rng.nextInt(6) + "]"));
        for (int Index = 0; Index < 6; Index++) {
            int Row = 2 + Rng.nextInt(Math.max(1, Depth - 4));
            int X = DoorX + Ix * Row + Lx * (Rng.nextInt(5) - 2);
            int Z = DoorZ + Iz * Row + Lz * (Rng.nextInt(5) - 2);
            if (G.empty(X, Floor + 1, Z)) {
                G.set(X, Floor + 1, Z, mod("paper_stack[facing=" + face(Rng.nextInt(4)) + ",variant=" + Rng.nextInt(6) + "]"));
            }
        }
        B.RoomInfos.add(new Scp1730RoomInfo(VaultHall, Zone, X0, Z0, X1, Z1, Floor, H, List.of(new int[]{DoorX, DoorZ, Dir})));
        B.fixture("vault_hall", DoorX, Floor + 1, DoorZ, face(Dir), 0, Zone);
    }

    static void lightless(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("supply_crate"), new int[]{0, 1, 2, 2}[R.Zone]);
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        for (int Index = 0; Index < 6 && Index < Cells.size(); Index++) {
            int[] Cell = Cells.get(Index);
            String Out = face(Cell[2]);
            switch (Index % 3) {
                case 0 -> R.put(Cell[0], R.F + 1, Cell[1], mod("steel_shelf[facing=" + Out + "]"));
                case 1 -> R.tall(Cell[0], Cell[1], mod("locker"), "facing=" + Out + ",open=true");
                default -> R.put(Cell[0], R.F + 1, Cell[1], mod("chemical_barrel[facing=" + Out + "]"));
            }
        }
        for (int X = R.X0 + 2; X <= R.X1 - 2; X += 4) {
            for (int Z = R.Z0 + 2; Z <= R.Z1 - 2; Z += 4) {
                if (B.Grid.empty(X, R.F + R.H, Z) && B.Grid.get(X, R.F + R.H + 1, Z) != null) {
                    B.Grid.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Scp1730Blocks.DeadLight, "down"));
                }
            }
        }
        R.put(R.cx() + 1, R.F + 1, R.cz(), Scp1730Blocks.facing(Scp1730Blocks.BodyBag, face(Rng.nextInt(4))));
        for (int Index = 0; Index < 5; Index++) {
            B.decal(R.X0 + Rng.nextInt(R.X1 - R.X0 + 1), R.F, R.Z0 + Rng.nextInt(R.Z1 - R.Z0 + 1), "up", B.pick(new int[]{Scp1730Builder.Scorch, Scp1730Builder.Footprints, Scp1730Builder.Grime}), R.Zone);
        }
        R.mess(4);
    }

    static void slow(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        List<int[]> Cells = R.wallCells();
        int Clocks = 0;
        for (int[] Cell : Cells) {
            int Along = Cell[2] % 2 == 0 ? Cell[0] : Cell[1];
            String Out = face(Cell[2]);
            if (Along % 3 == 0 && Clocks < 6 && backed(B, Cell[0], R.F + 3, Cell[1], Out) && R.put(Cell[0], R.F + 3, Cell[1], mod("wall_clock[facing=" + Out + "]"))) {
                Clocks++;
            }
            if (Along % 5 == 2) {
                R.put(Cell[0], R.F + 1, Cell[1], mod(Rng.nextBoolean() ? "research_bench_centrifuge" : "research_cryo_unit") + "[facing=" + Out + "]");
            } else if (Along % 5 == 4) {
                R.put(Cell[0], R.F + 1, Cell[1], mod("lab_shelf[facing=" + Out + "]"));
            }
        }
        for (int X = R.X0 + 3; X <= R.X1 - 3; X += 4) {
            for (int Wide = -1; Wide <= 1; Wide++) {
                if (R.put(X + Wide, R.F + 1, R.cz(), mod("lab_table[facing=north,left=" + (Wide > -1) + ",right=" + (Wide < 1) + "]")) && Rng.nextInt(3) == 0) {
                    R.put(X + Wide, R.F + 2, R.cz(), mod("lab_tube_holder[facing=north]"));
                }
            }
            R.put(X, R.F + 1, R.cz() + 1, mod("lab_stool[facing=north]"));
        }
        R.lights(5);
        R.mess(3);
    }

    static void drift(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        for (int Index = 0; Index < 8 && Index < Cells.size(); Index++) {
            int[] Cell = Cells.get(Index);
            String Out = face(Cell[2]);
            if (Index % 2 == 0) {
                R.stack(Cell[0], Cell[1], mod("hcz_hazard_crate"), Out, 1 + Rng.nextInt(3));
            } else {
                R.stack(Cell[0], Cell[1], mod("hcz_pipe_manifold"), Out, R.H);
            }
        }
        for (int Index = 0; Index < 6; Index++) {
            int X = R.X0 + 1 + Rng.nextInt(Math.max(1, R.X1 - R.X0 - 1));
            int Z = R.Z0 + 1 + Rng.nextInt(Math.max(1, R.Z1 - R.Z0 - 1));
            int Length = 1 + Rng.nextInt(3);
            for (int Level = 0; Level < Length; Level++) {
                if (!R.put(X, R.F + R.H - Level, Z, Scp1730Blocks.Chain)) {
                    break;
                }
            }
        }
        R.put(R.cx(), R.F + 1, R.cz(), Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Rng.nextInt(4))));
        R.lights(5);
        R.mess(4);
    }

    static void echo(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        boolean AlongX = R.X1 - R.X0 >= R.Z1 - R.Z0;
        int Cx = R.cx();
        int Cz = R.cz();
        int Reach = Math.max(1, Math.min(3, ((AlongX ? R.X1 - R.X0 : R.Z1 - R.Z0) - 6) / 2));
        for (int Along = -Reach; Along <= Reach; Along++) {
            int X = AlongX ? Cx + Along : Cx;
            int Z = AlongX ? Cz : Cz + Along;
            String Props = AlongX ? "north=false,south=false,west=" + (Along > -Reach) + ",east=" + (Along < Reach) : "west=false,east=false,north=" + (Along > -Reach) + ",south=" + (Along < Reach);
            if (R.put(X, R.F + 1, Z, mod("conference_table[" + Props + "]"))) {
                for (int Side = -1; Side <= 1; Side += 2) {
                    int Sx = AlongX ? X : X + Side;
                    int Sz = AlongX ? Z + Side : Z;
                    String Facing = AlongX ? (Side < 0 ? "south" : "north") : (Side < 0 ? "east" : "west");
                    if (Rng.nextInt(5) == 0) {
                        R.put(Sx, R.F + 1, Sz, Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Rng.nextInt(4))));
                    } else {
                        R.put(Sx, R.F + 1, Sz, mod("office_chair[facing=" + Facing + "]"));
                    }
                }
                if (Rng.nextInt(3) == 0) {
                    R.put(X, R.F + 2, Z, mod("paper_stack[facing=north,variant=" + Rng.nextInt(6) + "]"));
                }
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Wall = 0;
        for (int[] Cell : Cells) {
            if (Wall >= 4) {
                break;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Wall) {
                case 0 -> R.tall(Cell[0], Cell[1], mod("whiteboard"), "facing=" + Out + ",drawing=" + Rng.nextInt(4));
                case 1 -> backed(B, Cell[0], R.F + 2, Cell[1], Out) && R.put(Cell[0], R.F + 2, Cell[1], mod("wall_clock[facing=" + Out + "]"));
                case 2 -> R.tall(Cell[0], Cell[1], mod("coat_rack"), "facing=" + Out);
                default -> R.put(Cell[0], R.F + 1, Cell[1], mod("potted_plant[facing=" + Out + "]"));
            };
            if (Done) {
                Wall++;
            }
        }
        R.lights(5);
        R.mess(2);
    }
}
