package net.rai.scp_unleashed.scp5001.world.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;

public final class Scp5001Layout {
    public static final int ChamberRadius = 20;
    public static final int ChamberWall = 22;
    public static final int GalleryRadius = 26;
    public static final int GalleryWall = 27;
    public static final int MaintenanceNear = 2;
    public static final int MaintenanceFar = 4;
    public static final int OuterNear = 6;
    public static final int OuterFar = 19;
    public static final int HallwayNear = 21;
    public static final int HallwayFar = 24;
    public static final int InnerNear = 26;
    public static final int MinimumCells = 34;
    public static final int Samples = 1440;
    public static final int Wall = -1;

    private final Scp5001Plan Plan;
    private final Random Random;
    final int[] Hallway = new int[Scp5001Plan.Levels];
    final int[] Maintenance = new int[Scp5001Plan.Levels];
    final int[] Gallery = new int[Scp5001Plan.Levels];
    final int[][] Spokes = new int[Scp5001Plan.Levels][4];
    final List<Scp5001Room> Stairs = new ArrayList<>();
    final List<Scp5001Room> Generators = new ArrayList<>();
    final Map<Integer, int[]> StairFrames = new HashMap<>();
    final Set<Integer> BandRooms = new HashSet<>();
    Scp5001Room Engine;
    Scp5001Room Control;
    Scp5001Room Emergency;
    Scp5001Room Airlock;
    Scp5001Room Checkpoint;

    Scp5001Layout(Scp5001Plan Plan) {
        this.Plan = Plan;
        this.Random = Plan.Random;
    }

    public static int oct(int Dx, int Dz) {
        int A = Math.abs(Dx);
        int B = Math.abs(Dz);
        return Math.max(Math.max(A, B), (int) Math.ceil((A + B) / 1.41421356));
    }

    static int x(int Dx) {
        return Scp5001Plan.CentreX + Dx;
    }

    static int z(int Dz) {
        return Scp5001Plan.CentreZ + Dz;
    }

    static int band(int X, int Z) {
        return Scp5001Plan.band(X, Z);
    }

    static int octAt(int X, int Z) {
        return oct(X - Scp5001Plan.CentreX, Z - Scp5001Plan.CentreZ);
    }

    static boolean innerBand(int X, int Z) {
        return band(X, Z) >= InnerNear && octAt(X, Z) > GalleryWall;
    }

    static boolean outerBand(int X, int Z) {
        int Band = band(X, Z);
        return Band >= OuterNear && Band <= OuterFar && octAt(X, Z) > GalleryWall;
    }

    Scp5001Room add(String Kind, String Type, int Level, int Levels) {
        Scp5001Room Made = new Scp5001Room(Plan.Rooms.size() + 1, Kind, Type, Level, Levels, 0, 0, 0, 0);
        Plan.Rooms.add(Made);
        return Made;
    }

    private static boolean inGrid(int X, int Z) {
        return X >= 0 && X < Scp5001Plan.Width && Z >= 0 && Z < Scp5001Plan.Depth;
    }

    private int at(int Level, int X, int Z) {
        if (!inGrid(X, Z)) {
            return Wall;
        }
        return Plan.Region[Level][X * Scp5001Plan.Depth + Z];
    }

    private void put(int Level, int X, int Z, int Value) {
        if (inGrid(X, Z)) {
            Plan.Region[Level][X * Scp5001Plan.Depth + Z] = Value;
        }
    }

    private static boolean usable(int X, int Z) {
        return inGrid(X, Z) && band(X, Z) >= MaintenanceNear;
    }

    private void claim(Scp5001Room Owner, List<int[]> Cells) {
        for (int Level = Owner.Level; Level < Owner.Level + Owner.Levels; Level++) {
            for (int[] Cell : Cells) {
                put(Level, Cell[0], Cell[1], Owner.Id);
            }
            for (int[] Cell : Cells) {
                for (int Dx = -1; Dx <= 1; Dx++) {
                    for (int Dz = -1; Dz <= 1; Dz++) {
                        int X = Cell[0] + Dx;
                        int Z = Cell[1] + Dz;
                        if (inGrid(X, Z) && at(Level, X, Z) == 0) {
                            put(Level, X, Z, Wall);
                        }
                    }
                }
            }
        }
        bounds(Owner);
    }

    void bounds(Scp5001Room Owner) {
        int X0 = Integer.MAX_VALUE;
        int Z0 = Integer.MAX_VALUE;
        int X1 = Integer.MIN_VALUE;
        int Z1 = Integer.MIN_VALUE;
        int Count = 0;
        for (int X = 0; X < Scp5001Plan.Width; X++) {
            for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                if (at(Owner.Level, X, Z) == Owner.Id) {
                    X0 = Math.min(X0, X);
                    Z0 = Math.min(Z0, Z);
                    X1 = Math.max(X1, X);
                    Z1 = Math.max(Z1, Z);
                    Count++;
                }
            }
        }
        if (X0 <= X1) {
            Owner.setBounds(X0, Z0, X1, Z1);
        }
        Owner.Cells = Count;
    }

    void lay() {
        for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
            Hallway[Level] = add(Scp5001Plan.KindCorridor, "hallway", Level, 1).Id;
            Scp5001Room Maint = add(Scp5001Plan.KindCorridor, "maintenance", Level, 1);
            Maint.Dark = true;
            Maintenance[Level] = Maint.Id;
            Gallery[Level] = add(Scp5001Plan.KindGallery, "gallery", Level, 1).Id;
            for (int Direction = 0; Direction < 4; Direction++) {
                Spokes[Level][Direction] = add(Scp5001Plan.KindCorridor, "spoke", Level, 1).Id;
            }
        }
        specials();
        corridors();
        for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
            partition(Level, true);
            slices(Level, -1);
            slices(Level, 1);
        }
        walls();
        types();
        for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
            for (int Index = 0; Index < Plan.Region[Level].length; Index++) {
                if (Plan.Region[Level][Index] == Wall) {
                    Plan.Region[Level][Index] = 0;
                }
            }
        }
        for (Scp5001Room Each : Plan.Rooms) {
            bounds(Each);
        }
        doors();
        repair();
    }

    private void repair() {
        for (int Pass = 0; Pass < 6; Pass++) {
            Set<Integer> Reached = new HashSet<>();
            for (Scp5001Room Each : Plan.Rooms) {
                if (Each.corridor()) {
                    Reached.add(Each.Id);
                }
            }
            boolean Grew = true;
            while (Grew) {
                Grew = false;
                for (Scp5001Room Each : Plan.Rooms) {
                    for (Scp5001Door Door : Each.Doors) {
                        boolean From = Reached.contains(Door.From());
                        boolean To = Reached.contains(Door.To());
                        if (From != To) {
                            Reached.add(Door.From());
                            Reached.add(Door.To());
                            Grew = true;
                        }
                    }
                }
            }
            boolean Fixed = false;
            for (Scp5001Room Each : Plan.Rooms) {
                if (Reached.contains(Each.Id) || Each.Type.equals("void") || Each.Cells == 0) {
                    continue;
                }
                Set<Integer> Near = new HashSet<>();
                for (int X = Each.X0 - 2; X <= Each.X1 + 2; X++) {
                    for (int Z = Each.Z0 - 2; Z <= Each.Z1 + 2; Z++) {
                        int Id = at(Each.Level, X, Z);
                        if (Id > 0 && Id != Each.Id && Reached.contains(Id)) {
                            Near.add(Id);
                        }
                    }
                }
                for (int Target : Near) {
                    if (door(Each, Each.Level, true, Target, false, 1)) {
                        Fixed = true;
                        break;
                    }
                }
            }
            if (!Fixed) {
                return;
            }
        }
    }

    private List<int[]> cells(BiPredicate<Integer, Integer> Test) {
        List<int[]> Found = new ArrayList<>();
        for (int X = 0; X < Scp5001Plan.Width; X++) {
            for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                if (usable(X, Z) && Test.test(X - Scp5001Plan.CentreX, Z - Scp5001Plan.CentreZ)) {
                    Found.add(new int[]{X, Z});
                }
            }
        }
        return Found;
    }

    private static int[] stairFrame(int Axis, int Sign, int Low) {
        int Outer = -1;
        int Inner = -1;
        for (int Along = 25; Along < 80; Along++) {
            boolean Fits = true;
            for (int Across = Low; Across < Low + 7; Across++) {
                int Dx = Axis == 0 ? Across : Sign * Along;
                int Dz = Axis == 0 ? Sign * Along : Across;
                if (!outerBand(x(Dx), z(Dz))) {
                    Fits = false;
                    break;
                }
            }
            if (Fits) {
                if (Inner < 0) {
                    Inner = Along;
                }
                Outer = Along;
            } else if (Inner >= 0) {
                break;
            }
        }
        return new int[]{Axis, Sign, Low, Inner, Inner};
    }

    static int[] stairCell(int[] Frame, int U, int V) {
        int Axis = Frame[0];
        int Sign = Frame[1];
        int Across = Frame[2] + U;
        int Along = Frame[4] + V;
        int Dx = Axis == 0 ? Across : Sign * Along;
        int Dz = Axis == 0 ? Sign * Along : Across;
        return new int[]{x(Dx), z(Dz)};
    }

    static double wrap(double Angle) {
        double Value = Angle;
        while (Value > Math.PI) {
            Value -= Math.PI * 2.0;
        }
        while (Value < -Math.PI) {
            Value += Math.PI * 2.0;
        }
        return Value;
    }

    private void specials() {
        int[][] Frames = {stairFrame(0, -1, 11), stairFrame(0, 1, -10), stairFrame(1, 1, 4), stairFrame(1, -1, -10)};
        for (int[] Frame : Frames) {
            Scp5001Room Core = add(Scp5001Plan.KindStair, "stair", 0, Scp5001Plan.Levels);
            List<int[]> Cells = new ArrayList<>();
            for (int V = 0; V < 14; V++) {
                for (int U = 0; U < 7; U++) {
                    Cells.add(stairCell(Frame, U, V));
                }
            }
            claim(Core, Cells);
            StairFrames.put(Core.Id, Frame);
            Stairs.add(Core);
        }
        double[][] Halls = {{-Math.PI / 4.0, 0}, {Math.PI * 3.0 / 4.0, 0}, {-Math.PI * 3.0 / 4.0, 1}, {Math.PI / 4.0, 1}};
        for (double[] Hall : Halls) {
            Scp5001Room Made = add(Scp5001Plan.KindHall, "generator", (int) Hall[1], 2);
            double Centre = Hall[0];
            claim(Made, cells((Dx, Dz) -> outerBand(x(Dx), z(Dz)) && Math.abs(wrap(Scp5001Hull.angle(Dx, Dz) - Centre)) <= 0.2));
            Generators.add(Made);
        }
        Engine = add(Scp5001Plan.KindHall, "engine", 0, 2);
        claim(Engine, cells((Dx, Dz) -> Dx > 0 && innerBand(x(Dx), z(Dz))));
        Airlock = add(Scp5001Plan.KindRoom, "airlock", Scp5001Plan.EntryLevel, 1);
        claim(Airlock, cells((Dx, Dz) -> Math.abs(Dx) <= 5 && Dz >= -53 && Dz <= -45));
        Checkpoint = add(Scp5001Plan.KindRoom, "checkpoint", Scp5001Plan.EntryLevel, 1);
        claim(Checkpoint, cells((Dx, Dz) -> Math.abs(Dx) <= 8 && Dz >= -43 && Dz <= -30 && band(x(Dx), z(Dz)) <= OuterFar));
        Control = add(Scp5001Plan.KindRoom, "control", 2, 1);
        claim(Control, cells((Dx, Dz) -> Dx < 0 && Math.abs(Dz) <= 10 && oct(Dx, Dz) > ChamberWall && band(x(Dx), z(Dz)) >= InnerNear));
        Emergency = add(Scp5001Plan.KindRoom, "emergency", 2, 1);
        claim(Emergency, cells((Dx, Dz) -> Dx <= -24 && Dz <= -13 && innerBand(x(Dx), z(Dz))));
    }

    private void corridors() {
        for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
            for (int X = 0; X < Scp5001Plan.Width; X++) {
                for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                    if (!usable(X, Z) || at(Level, X, Z) != 0) {
                        continue;
                    }
                    int Dx = X - Scp5001Plan.CentreX;
                    int Dz = Z - Scp5001Plan.CentreZ;
                    int Band = band(X, Z);
                    int Octagon = oct(Dx, Dz);
                    int Owner;
                    if (Octagon <= ChamberWall) {
                        continue;
                    } else if (Octagon <= GalleryRadius) {
                        Owner = Gallery[Level];
                    } else if (Band >= MaintenanceNear && Band <= MaintenanceFar) {
                        Owner = Maintenance[Level];
                    } else if (Band >= HallwayNear && Band <= HallwayFar) {
                        Owner = Hallway[Level];
                    } else {
                        Owner = spokeAt(Level, Dx, Dz, Band, Octagon);
                    }
                    if (Owner != 0) {
                        put(Level, X, Z, Owner);
                    }
                }
            }
        }
    }

    private int spokeAt(int Level, int Dx, int Dz, int Band, int Octagon) {
        int[][] Axes = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int Direction = 0; Direction < 4; Direction++) {
            int Along = Axes[Direction][0] * Dx + Axes[Direction][1] * Dz;
            int Across = Axes[Direction][0] == 0 ? Dx : Dz;
            if (Math.abs(Across) > 2 || Along <= 0) {
                continue;
            }
            if (Octagon >= GalleryWall && Band >= OuterNear - 1) {
                return Spokes[Level][Direction];
            }
        }
        return 0;
    }

    private static int bucket(int X, int Z) {
        double Angle = Scp5001Hull.angle(X - Scp5001Plan.CentreX, Z - Scp5001Plan.CentreZ);
        return Math.floorMod((int) Math.floor((Angle + Math.PI) / (Math.PI * 2.0) * Samples), Samples);
    }

    private void partition(int Level, boolean Outer) {
        boolean[] Open = new boolean[Samples];
        Map<Integer, List<int[]>> ByBucket = new HashMap<>();
        for (int X = 0; X < Scp5001Plan.Width; X++) {
            for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                if (at(Level, X, Z) == 0 && (Outer ? outerBand(X, Z) : innerBand(X, Z))) {
                    int Bucket = bucket(X, Z);
                    Open[Bucket] = true;
                    ByBucket.computeIfAbsent(Bucket, Key -> new ArrayList<>()).add(new int[]{X, Z});
                }
            }
        }
        if (ByBucket.isEmpty()) {
            return;
        }
        int Start = 0;
        while (Start < Samples && Open[Start]) {
            Start++;
        }
        if (Start == Samples) {
            Start = 0;
        }
        int Index = 0;
        while (Index < Samples) {
            if (!Open[(Start + Index) % Samples]) {
                Index++;
                continue;
            }
            List<Integer> Run = new ArrayList<>();
            while (Index < Samples && Open[(Start + Index) % Samples]) {
                Run.add((Start + Index) % Samples);
                Index++;
            }
            carveRun(Level, Outer, Run, ByBucket);
        }
    }

    private void carveRun(int Level, boolean Outer, List<Integer> Run, Map<Integer, List<int[]>> ByBucket) {
        double Step = Math.PI * 2.0 / Samples;
        double[] Length = new double[Run.size()];
        double Total = 0.0;
        for (int Index = 0; Index < Run.size(); Index++) {
            double Angle = -Math.PI + (Run.get(Index) + 0.5) * Step;
            double A = Outer ? Scp5001Hull.RadiusX - 12.5 : 40.0;
            double B = Outer ? Scp5001Hull.RadiusZ - 12.5 : 30.0;
            Total += Math.sqrt(Math.pow(A * Math.sin(Angle), 2) + Math.pow(B * Math.cos(Angle), 2)) * Step;
            Length[Index] = Total;
        }
        double Low = Outer ? 7.5 : 10.0;
        double High = Outer ? 11.5 : 15.0;
        double Mark = 0.0;
        int From = 0;
        while (From < Run.size()) {
            double Wanted = Low + Random.nextDouble() * (High - Low);
            if (Total - Mark - Wanted < Low) {
                Wanted = Total - Mark;
            }
            int To = From;
            while (To < Run.size() - 1 && Length[To] - Mark < Wanted) {
                To++;
            }
            Scp5001Room Made = add(Scp5001Plan.KindRoom, Outer ? "b" : "i", Level, 1);
            Scp5001Room Back = Outer && Random.nextDouble() < 0.75 ? add(Scp5001Plan.KindRoom, "b", Level, 1) : null;
            for (int Index = From; Index <= To; Index++) {
                for (int[] Cell : ByBucket.getOrDefault(Run.get(Index), List.of())) {
                    boolean Rear = Back != null && band(Cell[0], Cell[1]) <= 12;
                    put(Level, Cell[0], Cell[1], Rear ? Back.Id : Made.Id);
                }
            }
            BandRooms.add(Made.Id);
            if (Back != null) {
                BandRooms.add(Back.Id);
            }
            Mark = Length[To];
            From = To + 1;
        }
    }

    private void slices(int Level, int Side) {
        Map<Integer, List<int[]>> ByRow = new HashMap<>();
        for (int X = 0; X < Scp5001Plan.Width; X++) {
            for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                int Dx = X - Scp5001Plan.CentreX;
                if (Integer.signum(Dx) == Side && at(Level, X, Z) == 0 && innerBand(X, Z) && Math.abs(Dx) >= 20) {
                    ByRow.computeIfAbsent(Z, Key -> new ArrayList<>()).add(new int[]{X, Z});
                }
            }
        }
        if (ByRow.isEmpty()) {
            return;
        }
        List<Integer> Rows = new ArrayList<>(ByRow.keySet());
        Rows.sort(Integer::compare);
        int Index = 0;
        while (Index < Rows.size()) {
            int Start = Index;
            while (Index + 1 < Rows.size() && Rows.get(Index + 1) == Rows.get(Index) + 1) {
                Index++;
            }
            int First = Rows.get(Start);
            int Last = Rows.get(Index);
            int Position = First;
            while (Position <= Last) {
                int Remaining = Last - Position + 1;
                int Height = 8 + Random.nextInt(4);
                if (Remaining - Height < 7) {
                    Height = Remaining;
                }
                Scp5001Room Made = add(Scp5001Plan.KindRoom, "i", Level, 1);
                for (int Z = Position; Z < Position + Height; Z++) {
                    for (int[] Cell : ByRow.getOrDefault(Z, List.of())) {
                        put(Level, Cell[0], Cell[1], Made.Id);
                    }
                }
                BandRooms.add(Made.Id);
                Position += Height;
            }
            Index++;
        }
    }

    private void walls() {
        for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
            List<int[]> Walls = new ArrayList<>();
            for (int X = 0; X < Scp5001Plan.Width; X++) {
                for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                    int Own = at(Level, X, Z);
                    if (Own <= 0 || !BandRooms.contains(Own)) {
                        continue;
                    }
                    for (int[] Step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int Other = at(Level, X + Step[0], Z + Step[1]);
                        if (Other == Own || Other == 0 || Other == Wall) {
                            continue;
                        }
                        if (!BandRooms.contains(Other) || Other < Own) {
                            Walls.add(new int[]{X, Z});
                            break;
                        }
                    }
                }
            }
            for (int[] Cell : Walls) {
                put(Level, Cell[0], Cell[1], Wall);
            }
        }
        for (int Id : BandRooms) {
            Scp5001Room Room = Plan.room(Id);
            bounds(Room);
            if (Room.Cells < MinimumCells) {
                for (int X = 0; X < Scp5001Plan.Width; X++) {
                    for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                        if (at(Room.Level, X, Z) == Id) {
                            put(Room.Level, X, Z, Wall);
                        }
                    }
                }
                Room.Type = "void";
                Room.Cells = 0;
            }
        }
    }

    private void types() {
        String[][] Pools = {
                {"substation", "storage", "coolant", "workshop", "storage", "drone_bay", "substation", "coolant", "workshop"},
                {"lab", "quarters", "lab", "medbay", "lab", "quarters", "specimen_vault", "lounge", "galley", "lab"},
                {"office", "lab", "server", "records", "conference", "office", "quarters", "lab", "comms", "lounge"}};
        String[][] InnerPools = {{"coolant", "monitoring", "workshop"}, {"field_lab", "observation", "monitoring"}, {"monitoring", "observation", "field_lab"}};
        String[][] Must = {{"storage", "drone_bay", "substation", "coolant"}, {"mess", "medbay", "galley", "specimen_vault", "quarters"},
                {"archive", "server", "comms", "armory", "security", "records"}};
        for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
            List<Scp5001Room> Band = new ArrayList<>();
            for (Scp5001Room Each : Plan.Rooms) {
                if (Each.Level != Level || !Each.Kind.equals(Scp5001Plan.KindRoom)) {
                    continue;
                }
                if (Each.Type.equals("b")) {
                    Band.add(Each);
                } else if (Each.Type.equals("i")) {
                    Each.Type = InnerPools[Level][Random.nextInt(InnerPools[Level].length)];
                }
            }
            Band.sort(Comparator.comparingInt((Scp5001Room Room) -> -Room.Cells).thenComparingInt(Room -> Room.Id));
            int Next = 0;
            for (String Type : Must[Level]) {
                if (Next < Band.size()) {
                    Band.get(Next).Type = Type;
                    Next++;
                }
            }
            for (int Index = Next; Index < Band.size(); Index++) {
                Band.get(Index).Type = Pools[Level][Random.nextInt(Pools[Level].length)];
            }
            for (Scp5001Room Each : Band) {
                Each.Dark = Each.Type.equals("storage") && Random.nextInt(3) == 0 || Each.Type.equals("workshop") && Random.nextInt(2) == 0;
            }
        }
    }

    private boolean corridorId(int Id) {
        Scp5001Room Found = Plan.room(Id);
        return Found != null && Found.corridor();
    }

    boolean door(Scp5001Room Owner, int Level, boolean Open, int Target, boolean CorridorOnly) {
        return door(Owner, Level, Open, Target, CorridorOnly, 3);
    }

    boolean door(Scp5001Room Owner, int Level, boolean Open, int Target, boolean CorridorOnly, int Spacing) {
        int Fy = Scp5001Plan.floorY(Level) + 1;
        List<int[]> Options = new ArrayList<>();
        int[][] Steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        double Cx = (Owner.X0 + Owner.X1) / 2.0;
        double Cz = (Owner.Z0 + Owner.Z1) / 2.0;
        for (int X = Owner.X0; X <= Owner.X1; X++) {
            for (int Z = Owner.Z0; Z <= Owner.Z1; Z++) {
                if (at(Level, X, Z) != Owner.Id || !plate(Owner, X, Z)) {
                    continue;
                }
                for (int[] Step : Steps) {
                    int Wx = X + Step[0];
                    int Wz = Z + Step[1];
                    int Fx = Wx + Step[0];
                    int Fz = Wz + Step[1];
                    int Far = at(Level, Fx, Fz);
                    if (at(Level, Wx, Wz) != 0 || Far <= 0 || Far == Owner.Id) {
                        continue;
                    }
                    if (Target != 0 && Far != Target || Target == 0 && CorridorOnly && !corridorId(Far)) {
                        continue;
                    }
                    int Px = Step[1] != 0 ? 1 : 0;
                    int Pz = Step[0] != 0 ? 1 : 0;
                    if (at(Level, Wx + Px, Wz + Pz) != 0 || at(Level, X + Px, Z + Pz) != Owner.Id || at(Level, Fx + Px, Fz + Pz) != Far) {
                        continue;
                    }
                    if (!Scp5001Plan.interiorCell(Wx, Fy + 3, Wz) || !Scp5001Plan.interiorCell(Wx + Px, Fy + 3, Wz + Pz) || octAt(Wx, Wz) <= ChamberWall
                            || octAt(Wx + Px, Wz + Pz) <= ChamberWall) {
                        continue;
                    }
                    double Score = (Wx - Cx) * (Wx - Cx) + (Wz - Cz) * (Wz - Cz) + Random.nextDouble() * 6.0;
                    Options.add(new int[]{Wx, Wz, Px, Far, (int) (Score * 100)});
                }
            }
        }
        Options.sort(Comparator.comparingInt(Option -> Option[4]));
        for (int[] Best : Options) {
            boolean Clash = false;
            for (Scp5001Room Any : Plan.Rooms) {
                for (Scp5001Door Other : Any.Doors) {
                    if (Other.Y() == Fy && Math.abs(Other.X() - Best[0]) <= Spacing && Math.abs(Other.Z() - Best[1]) <= Spacing) {
                        Clash = true;
                        break;
                    }
                }
                if (Clash) {
                    break;
                }
            }
            if (Clash) {
                continue;
            }
            Owner.Doors.add(new Scp5001Door(Best[0], Fy, Best[1], Best[2] == 1, Open, Owner.Id, Best[3]));
            return true;
        }
        return false;
    }

    private boolean plate(Scp5001Room Owner, int X, int Z) {
        int[] Frame = StairFrames.get(Owner.Id);
        if (Frame == null) {
            return true;
        }
        int Along = Frame[0] == 0 ? Frame[1] * (Z - Scp5001Plan.CentreZ) : Frame[1] * (X - Scp5001Plan.CentreX);
        return Along - Frame[4] <= 2;
    }

    private void doors() {
        for (Scp5001Room Each : new ArrayList<>(Plan.Rooms)) {
            if (Each.corridor() || Each.Type.equals("void")) {
                continue;
            }
            if (Each.Kind.equals(Scp5001Plan.KindStair)) {
                for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
                    if (!door(Each, Level, true, Hallway[Level], true)) {
                        door(Each, Level, true, 0, true);
                    }
                }
                continue;
            }
            if (Each.Kind.equals(Scp5001Plan.KindHall)) {
                for (int Level = Each.Level; Level < Each.Level + Each.Levels; Level++) {
                    boolean Made = door(Each, Level, true, Hallway[Level], true);
                    if (Each.Type.equals("engine")) {
                        Made |= door(Each, Level, true, Gallery[Level], true);
                    } else if (Level == Each.Level) {
                        door(Each, Level, true, Maintenance[Level], true);
                    }
                    if (!Made) {
                        door(Each, Level, true, 0, true);
                    }
                }
                continue;
            }
            switch (Each.Type) {
                case "control" -> {
                    door(Each, Each.Level, true, Hallway[Each.Level], true);
                    door(Each, Each.Level, true, Emergency.Id, false);
                    door(Each, Each.Level, true, Gallery[Each.Level], true);
                    door(Each, Each.Level, true, Gallery[Each.Level], true);
                    continue;
                }
                case "emergency" -> {
                    if (!door(Each, Each.Level, true, Hallway[Each.Level], true)) {
                        door(Each, Each.Level, true, 0, true);
                    }
                    continue;
                }
                case "airlock" -> {
                    door(Each, Each.Level, false, Checkpoint.Id, false);
                    continue;
                }
                case "checkpoint" -> {
                    door(Each, Each.Level, true, Hallway[Each.Level], true);
                    continue;
                }
                default -> {
                }
            }
            boolean Secure = Each.Type.equals("armory") || Each.Type.equals("archive") || Each.Type.equals("server") || Each.Type.equals("specimen_vault");
            boolean Open = !Secure && Random.nextInt(5) != 0;
            boolean Outer = band((Each.X0 + Each.X1) / 2, (Each.Z0 + Each.Z1) / 2) <= OuterFar + 2;
            int First = Outer ? Hallway[Each.Level] : Random.nextInt(2) == 0 ? Gallery[Each.Level] : Hallway[Each.Level];
            boolean Made = door(Each, Each.Level, Open, First, true);
            if (!Made) {
                Made = door(Each, Each.Level, Open, 0, true);
            }
            if (Made && Outer && Random.nextInt(3) == 0) {
                door(Each, Each.Level, Open, Maintenance[Each.Level], true);
            }
            if (!Made) {
                Made = door(Each, Each.Level, Open, 0, false);
            }
            if (!Made) {
                Made = door(Each, Each.Level, Open, 0, true, 1);
            }
            if (!Made) {
                door(Each, Each.Level, Open, 0, false, 1);
            }
        }
    }
}
