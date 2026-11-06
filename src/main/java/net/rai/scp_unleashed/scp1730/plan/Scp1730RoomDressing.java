package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Scp1730RoomDressing {
    public static final String[] SmallKinds = {"manager", "break", "locker", "janitor"};

    private Scp1730RoomDressing() {
    }

    static String route(Scp1730Builder B, Scp1730Rooms.Box R) {
        if (Scp1730AnomalousRooms.furnish(B, R) || Scp1730Sectors.furnish(B, R) || Scp1730Variants.furnish(B, R)) {
            return "";
        }
        switch (R.Room.Type) {
            case "office" -> office(B, R);
            case "small_office" -> smallOffice(B, R);
            case "research" -> lab(B, R);
            case "medical" -> ward(B, R);
            case "dorms" -> dorms(B, R);
            default -> {
                return R.Room.Type;
            }
        }
        return "";
    }

    public static final java.util.Set<String> Desks = java.util.Set.of("office_desk", "metal_desk", "lab_bench_desk", "lab_table", "reception_desk");
    public static final java.util.Set<String> Couches = java.util.Set.of("couch_grey", "couch_blue", "couch_leather");
    public static final java.util.Set<String> Tables = java.util.Set.of("conference_table", "cafeteria_table");

    static String prop(String State, String Key) {
        int Open = State.indexOf('[');
        if (Open < 0) {
            return null;
        }
        for (String Pair : State.substring(Open + 1, State.length() - 1).split(",")) {
            int Equals = Pair.indexOf('=');
            if (Equals > 0 && Pair.substring(0, Equals).equals(Key)) {
                return Pair.substring(Equals + 1);
            }
        }
        return null;
    }

    static String with(String State, String Key, String Value) {
        int Open = State.indexOf('[');
        if (Open < 0) {
            return State + "[" + Key + "=" + Value + "]";
        }
        StringBuilder Made = new StringBuilder(State.substring(0, Open + 1));
        boolean Found = false;
        boolean First = true;
        for (String Pair : State.substring(Open + 1, State.length() - 1).split(",")) {
            int Equals = Pair.indexOf('=');
            String Name = Equals > 0 ? Pair.substring(0, Equals) : Pair;
            Made.append(First ? "" : ",").append(Name.equals(Key) ? Key + "=" + Value : Pair);
            Found |= Name.equals(Key);
            First = false;
        }
        if (!Found) {
            Made.append(",").append(Key).append("=").append(Value);
        }
        return Made.append("]").toString();
    }

    static String shortId(String State) {
        int Cut = State.indexOf('[');
        String Id = Cut < 0 ? State : State.substring(0, Cut);
        return Id.startsWith(Scp1730Blocks.Mod) ? Id.substring(Scp1730Blocks.Mod.length()) : Id;
    }

    public interface Cells {
        String at(int X, int Y, int Z);
    }

    public static String joined(Cells G, int X, int Y, int Z, String State) {
        String Id = shortId(State);
        if (Tables.contains(Id)) {
            String Made = State;
            for (int Dir = 0; Dir < 4; Dir++) {
                String Other = G.at(X + Scp1730Layout.Dx[Dir], Y, Z + Scp1730Layout.Dz[Dir]);
                Made = with(Made, Scp1730Layout.Facings[Dir], String.valueOf(Other != null && shortId(Other).equals(Id)));
            }
            return Made;
        }
        boolean Desk = Desks.contains(Id);
        if (!Desk && !Couches.contains(Id)) {
            return State;
        }
        String Facing = prop(State, "facing");
        if (Facing == null) {
            return State;
        }
        int Dir = Scp1730Builder.dirOf(Facing);
        int LeftDir = Desk ? (Dir + 1) % 4 : (Dir + 3) % 4;
        int RightDir = (LeftDir + 2) % 4;
        String Left = G.at(X + Scp1730Layout.Dx[LeftDir], Y, Z + Scp1730Layout.Dz[LeftDir]);
        String Right = G.at(X + Scp1730Layout.Dx[RightDir], Y, Z + Scp1730Layout.Dz[RightDir]);
        boolean JoinsLeft = Left != null && shortId(Left).equals(Id) && Facing.equals(prop(Left, "facing"));
        boolean JoinsRight = Right != null && shortId(Right).equals(Id) && Facing.equals(prop(Right, "facing"));
        return with(with(State, "left", String.valueOf(JoinsLeft)), "right", String.valueOf(JoinsRight));
    }

    public static void settle(Scp1730Builder B, Scp1730Rooms.Box R) {
        Scp1730Grid G = B.Grid;
        Scp1730Layout.Room Made = R.Room;
        for (int X = Made.Bx0; X <= Made.Bx1; X++) {
            for (int Z = Made.Bz0; Z <= Made.Bz1; Z++) {
                for (int Y = R.F + R.H; Y >= R.F + 1; Y--) {
                    String State = G.get(X, Y, Z);
                    if (State == null) {
                        continue;
                    }
                    String Id = State.indexOf('[') < 0 ? State : State.substring(0, State.indexOf('['));
                    if (Id.equals("minecraft:chain")) {
                        int Up = Y + 1;
                        while (G.get(X, Up, Z) != null && G.get(X, Up, Z).startsWith("minecraft:chain")) {
                            Up++;
                        }
                        if (!Scp1730SupportCheck.solid(G.get(X, Up, Z))) {
                            G.set(X, Y, Z, null);
                        }
                        continue;
                    }
                    if (Scp1730SupportCheck.Hanging.contains(Id) && !State.contains("facing=up") && !Scp1730SupportCheck.solid(G.get(X, Y + 1, Z))) {
                        G.set(X, Y, Z, null);
                        continue;
                    }
                    String Joined = joined(G::get, X, Y, Z, State);
                    if (!Joined.equals(State)) {
                        G.set(X, Y, Z, Joined);
                    }
                }
            }
        }
    }

    static String mod(String Name) {
        return Scp1730Blocks.Mod + Name;
    }

    static String face(int Dir) {
        return Scp1730Layout.Facings[(Dir % 4 + 4) % 4];
    }

    static List<int[]> wall(Scp1730Rooms.Box R, int Dir) {
        List<int[]> Found = new ArrayList<>();
        for (int[] Cell : R.wallCells()) {
            if (Cell[2] == Dir) {
                Found.add(Cell);
            }
        }
        return Found;
    }

    static boolean doorOn(Scp1730Rooms.Box R, int Side) {
        for (Scp1730Layout.Door Each : R.Room.Doors) {
            if (Each.Dir == Side) {
                return true;
            }
        }
        return false;
    }

    static boolean tower(Scp1730Builder B, Scp1730Rooms.Box R, int X, int Z, String Base, String Facing) {
        if (R.H < 3 || !R.free(X, R.F + 1, Z) || !R.free(X, R.F + 2, Z) || !R.free(X, R.F + 3, Z)) {
            return false;
        }
        B.Grid.set(X, R.F + 1, Z, mod(Base + "_bottom[facing=" + Facing + "]"));
        B.Grid.set(X, R.F + 2, Z, mod(Base + "_middle[facing=" + Facing + "]"));
        B.Grid.set(X, R.F + 3, Z, mod(Base + "_top[facing=" + Facing + "]"));
        return true;
    }

    static boolean pair(Scp1730Builder B, Scp1730Rooms.Box R, int X, int Z, String Base, String Facing) {
        if (!R.free(X, R.F + 1, Z) || !R.free(X, R.F + 2, Z)) {
            return false;
        }
        B.Grid.set(X, R.F + 1, Z, mod(Base + "_bottom[facing=" + Facing + "]"));
        B.Grid.set(X, R.F + 2, Z, mod(Base + "_top[facing=" + Facing + "]"));
        return true;
    }

    static boolean backed(Scp1730Builder B, int[] Cell, int Y) {
        String Behind = B.Grid.get(Cell[0] - Scp1730Layout.Dx[Cell[2]], Y, Cell[1] - Scp1730Layout.Dz[Cell[2]]);
        return Behind != null && !Scp1730Blocks.passable(Behind);
    }

    static boolean wallProp(Scp1730Builder B, Scp1730Rooms.Box R, int[] Cell, String Name) {
        return backed(B, Cell, R.F + 2) && R.put(Cell[0], R.F + 2, Cell[1], mod(Name + "[facing=" + face(Cell[2]) + "]"));
    }

    static void office(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("filing_cabinet"), 0);
        R.breach(1, false, false);
        boolean AlongX = R.X1 - R.X0 >= R.Z1 - R.Z0;
        int Across0 = AlongX ? R.Z0 : R.X0;
        int Across1 = AlongX ? R.Z1 : R.X1;
        int Along0 = AlongX ? R.X0 : R.Z0;
        int Along1 = AlongX ? R.X1 : R.Z1;
        for (int Row = Across0 + 2; Row + 1 <= Across1 - 2; Row += 5) {
            for (int Half = 0; Half < 2; Half++) {
                int Line = Row + Half;
                String Facing = AlongX ? (Half == 0 ? "north" : "south") : (Half == 0 ? "west" : "east");
                int Seat = Half == 0 ? -1 : 1;
                for (int Along = Along0 + 2; Along <= Along1 - 2; Along++) {
                    int X = AlongX ? Along : Line;
                    int Z = AlongX ? Line : Along;
                    int Slot = (Along - Along0 - 2) % 4;
                    if (Slot == 3 && Rng.nextInt(3) == 0) {
                        continue;
                    }
                    boolean Left = Along > Along0 + 2 && Slot != 0;
                    boolean Right = Along < Along1 - 2 && Slot != 3;
                    if (!R.put(X, R.F + 1, Z, mod("office_desk[facing=" + Facing + ",left=" + Left + ",right=" + Right + "]"))) {
                        continue;
                    }
                    int Roll = Rng.nextInt(6);
                    String Top = Roll < 2 ? mod("crt_monitor[facing=" + Facing + ",lit=" + (Rng.nextInt(4) == 0) + ",screen=" + Rng.nextInt(10) + "]")
                            : Roll == 2 ? mod("paper_stack[facing=" + Facing + ",variant=" + Rng.nextInt(6) + "]") : Roll == 3 ? mod("desk_lamp[facing=" + Facing + ",lit=false]") : null;
                    if (Top != null) {
                        R.put(X, R.F + 2, Z, Top);
                    }
                    int Sx = AlongX ? X : X + Seat;
                    int Sz = AlongX ? Z + Seat : Z;
                    if (Rng.nextInt(4) == 0) {
                        R.put(Sx, R.F + 1, Sz, Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Rng.nextInt(4))));
                    } else if (Rng.nextInt(5) > 0) {
                        R.put(Sx, R.F + 1, Sz, mod("office_chair[facing=" + Scp1730Blocks.opposite(Facing) + "]"));
                    }
                }
            }
        }
        dressOfficeWalls(B, R);
        vignette(B, R);
        R.lights(5);
        R.mess(4);
    }

    static void dressOfficeWalls(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Towers = 0;
        int Small = 0;
        int Hung = 0;
        String[] Hangings = {"office_wall_calendar", "wall_clock", "office_notice_board", "office_notice_board"};
        for (int[] Cell : Cells) {
            String Out = face(Cell[2]);
            if (Towers < 4 && tower(B, R, Cell[0], Cell[1], "office_records_cabinet", Out)) {
                Towers++;
                continue;
            }
            if (Small < 6) {
                boolean Done = switch (Small) {
                    case 0 -> R.put(Cell[0], R.F + 1, Cell[1], mod("office_photocopier[facing=" + Out + "]"));
                    case 1 -> R.tall(Cell[0], Cell[1], mod("water_cooler"), "facing=" + Out);
                    case 2 -> R.put(Cell[0], R.F + 1, Cell[1], mod("office_shredder[facing=" + Out + "]"));
                    case 3 -> R.tall(Cell[0], Cell[1], mod("coat_rack"), "facing=" + Out);
                    case 4 -> R.put(Cell[0], R.F + 1, Cell[1], mod("potted_plant[facing=" + Out + "]"));
                    default -> R.put(Cell[0], R.F + 1, Cell[1], mod("binder_shelf[facing=" + Out + "]"));
                };
                if (Done) {
                    Small++;
                    continue;
                }
            }
            if (Hung < 4 && R.free(Cell[0], R.F + 1, Cell[1]) && R.free(Cell[0], R.F + 2, Cell[1])) {
                wallProp(B, R, Cell, Hangings[Hung % Hangings.length]);
                Hung++;
            }
        }
        if (Rng.nextBoolean()) {
            for (int[] Cell : Cells) {
                if (R.tall(Cell[0], Cell[1], mod("whiteboard"), "facing=" + face(Cell[2]) + ",drawing=" + Rng.nextInt(4))) {
                    break;
                }
            }
        }
    }

    static void smallOffice(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        int[][] Weights = {{4, 3, 2, 0}, {1, 3, 3, 2}, {0, 2, 3, 3}, {0, 1, 2, 3}};
        int[] Odds = Weights[Math.min(3, R.Zone)];
        int Total = 0;
        for (int Each : Odds) {
            Total += Each;
        }
        int Roll = Rng.nextInt(Total);
        int Kind = 0;
        while (Roll >= Odds[Kind]) {
            Roll -= Odds[Kind];
            Kind++;
        }
        switch (SmallKinds[Kind]) {
            case "manager" -> manager(B, R);
            case "break" -> breakRoom(B, R);
            case "locker" -> lockerRoom(B, R);
            default -> janitor(B, R);
        }
        vignette(B, R);
        R.lights(5);
        R.mess(3);
    }

    static int[] farWall(Scp1730Rooms.Box R) {
        Scp1730Layout.Door Main = R.door();
        int Back = (Main.Dir + 2) % 4;
        if (doorOn(R, Back)) {
            for (int Turn = 1; Turn < 4; Turn += 2) {
                if (!doorOn(R, (Main.Dir + Turn) % 4)) {
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

    static void manager(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("filing_cabinet"), 0);
        int[] Back = farWall(R);
        int Dx = Scp1730Layout.Dx[Back[2]];
        int Dz = Scp1730Layout.Dz[Back[2]];
        int Lx = Back[2] % 2 == 0 ? 1 : 0;
        int Lz = Back[2] % 2 == 0 ? 0 : 1;
        String Toward = face(Back[2]);
        for (int Side = 0; Side <= 1; Side++) {
            int X = Back[0] + Dx * 2 + Lx * Side;
            int Z = Back[1] + Dz * 2 + Lz * Side;
            if (R.put(X, R.F + 1, Z, mod("office_desk[facing=" + Toward + ",left=" + (Side == 1) + ",right=" + (Side == 0) + "]"))) {
                R.put(X, R.F + 2, Z, Side == 0 ? mod("crt_monitor[facing=" + Toward + ",lit=false,screen=" + Rng.nextInt(10) + "]") : mod("desk_lamp[facing=" + Toward + ",lit=false]"));
            }
            R.put(X + Dx, R.F + 1, Z + Dz, mod("office_chair[facing=" + face(Back[2] + 2) + "]"));
        }
        R.put(Back[0] + Dx, R.F + 1, Back[1] + Dz, mod("office_chair[facing=" + Toward + "]"));
        for (int[] Cell : wall(R, Back[2])) {
            String Out = face(Cell[2]);
            if ((Cell[0] + Cell[1]) % 3 == 0) {
                tower(B, R, Cell[0], Cell[1], "office_records_cabinet", Out);
            } else {
                R.put(Cell[0], R.F + 1, Cell[1], mod("binder_shelf[facing=" + Out + "]"));
            }
        }
        List<int[]> Couch = wall(R, Back[2] % 2 == 0 ? 1 : 2);
        int Seats = 0;
        for (int Index = 1; Index < Couch.size() - 1 && Seats < 3; Index++) {
            int[] Cell = Couch.get(Index);
            if (R.put(Cell[0], R.F + 1, Cell[1], mod("couch_leather[facing=" + face(Cell[2]) + ",left=" + (Seats > 0) + ",right=" + (Seats < 2) + ",shape=straight]"))) {
                Seats++;
            } else if (Seats > 0) {
                break;
            }
        }
        dressOfficeWalls(B, R);
    }

    static void breakRoom(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        int[] Back = farWall(R);
        int Index = 0;
        for (int[] Cell : wall(R, Back[2])) {
            String Out = face(Cell[2]);
            int Slot = Index++ % 6;
            switch (Slot) {
                case 0 -> R.tall(Cell[0], Cell[1], mod("vending_machine"), "facing=" + Out);
                case 1 -> R.put(Cell[0], R.F + 1, Cell[1], mod("mini_fridge[facing=" + Out + ",open=" + (Rng.nextInt(3) == 0) + "]"));
                case 5 -> R.tall(Cell[0], Cell[1], mod("water_cooler"), "facing=" + Out);
                default -> {
                    if (R.put(Cell[0], R.F + 1, Cell[1], mod("metal_desk[facing=" + Out + ",left=" + (Slot > 2) + ",right=" + (Slot < 4) + "]"))) {
                        R.put(Cell[0], R.F + 2, Cell[1], Slot == 2 ? mod("coffee_machine[facing=" + Out + ",lit=false]") : Slot == 3 ? mod("microwave[facing=" + Out + ",lit=false]")
                                : mod("paper_stack[facing=" + Out + ",variant=" + Rng.nextInt(6) + "]"));
                    }
                }
            }
        }
        int Seats = 0;
        List<int[]> Couch = wall(R, Back[2] % 2 == 0 ? 1 : 2);
        for (int Each = 1; Each < Couch.size() - 1 && Seats < 4; Each++) {
            int[] Cell = Couch.get(Each);
            if (R.put(Cell[0], R.F + 1, Cell[1], mod("couch_grey[facing=" + face(Cell[2]) + ",left=" + (Seats > 0) + ",right=" + (Seats < 3) + ",shape=straight]"))) {
                Seats++;
            } else if (Seats > 0) {
                break;
            }
        }
        int Tx = R.cx() + 1;
        int Tz = R.cz() + 1;
        for (int Ox = 0; Ox <= 1; Ox++) {
            for (int Oz = 0; Oz <= 1; Oz++) {
                String Props = "north=" + (Oz == 1) + ",south=" + (Oz == 0) + ",west=" + (Ox == 1) + ",east=" + (Ox == 0);
                R.put(Tx + Ox, R.F + 1, Tz + Oz, mod("conference_table[" + Props + "]"));
            }
        }
        String[] Facing = {"south", "south", "north", "north"};
        int[][] Chairs = {{Tx, Tz - 1}, {Tx + 1, Tz - 1}, {Tx, Tz + 2}, {Tx + 1, Tz + 2}};
        for (int Chair = 0; Chair < Chairs.length; Chair++) {
            if (Rng.nextInt(4) == 0) {
                R.put(Chairs[Chair][0], R.F + 1, Chairs[Chair][1], Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Rng.nextInt(4))));
            } else {
                R.put(Chairs[Chair][0], R.F + 1, Chairs[Chair][1], mod("office_chair[facing=" + Facing[Chair] + "]"));
            }
        }
        for (int[] Cell : R.wallCells()) {
            if (R.free(Cell[0], R.F + 1, Cell[1]) && R.free(Cell[0], R.F + 2, Cell[1]) && Rng.nextInt(6) == 0) {
                wallProp(B, R, Cell, Rng.nextBoolean() ? "office_notice_board" : "wall_clock");
            }
        }
        R.put(R.X0 + 1, R.F + 1, R.Z1 - 1, mod("trash_can[facing=north]"));
    }

    static void lockerRoom(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("locker"), R.Zone >= 2 ? 1 : 0);
        for (int[] Cell : R.wallCells()) {
            String Out = face(Cell[2]);
            int Along = Cell[2] % 2 == 0 ? Cell[0] : Cell[1];
            if (Along % 4 == 3) {
                R.tall(Cell[0], Cell[1], mod("coat_rack"), "facing=" + Out);
            } else {
                R.tall(Cell[0], Cell[1], mod("locker"), "facing=" + Out + ",open=" + (Rng.nextInt(4) == 0));
            }
        }
        boolean AlongX = R.X1 - R.X0 >= R.Z1 - R.Z0;
        for (int Along = (AlongX ? R.X0 : R.Z0) + 2; Along <= (AlongX ? R.X1 : R.Z1) - 2; Along++) {
            int X = AlongX ? Along : R.cx();
            int Z = AlongX ? R.cz() : Along;
            if (Rng.nextInt(7) == 0) {
                R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.OverturnedBench, AlongX ? "north" : "east"));
            } else {
                R.put(X, R.F + 1, Z, mod("ez_waiting_chairs[facing=" + (AlongX ? (Along % 2 == 0 ? "north" : "south") : (Along % 2 == 0 ? "east" : "west")) + "]"));
            }
        }
        for (int Screen = 0; Screen < 2; Screen++) {
            int X = Screen == 0 ? R.X0 + 2 : R.X1 - 2;
            int Z = Screen == 0 ? R.Z1 - 2 : R.Z0 + 2;
            R.tall(X, Z, mod("privacy_screen"), "facing=" + (Screen == 0 ? "north" : "south"));
        }
    }

    static void janitor(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("supply_crate"), new int[]{0, 0, 1, 1}[Math.min(3, R.Zone)]);
        for (int[] Cell : R.wallCells()) {
            String Out = face(Cell[2]);
            switch (Rng.nextInt(6)) {
                case 0, 1 -> R.put(Cell[0], R.F + 1, Cell[1], mod("steel_shelf[facing=" + Out + "]"));
                case 2 -> R.put(Cell[0], R.F + 1, Cell[1], mod("chemical_barrel[facing=" + Out + "]"));
                case 3 -> R.put(Cell[0], R.F + 1, Cell[1], mod("chemical_shelf[facing=" + Out + "]"));
                case 4 -> R.put(Cell[0], R.F + 1, Cell[1], mod("supply_crate[facing=" + Out + ",open=" + Rng.nextBoolean() + "]"));
                default -> {
                }
            }
        }
        for (int Index = 0; Index < 4; Index++) {
            int X = R.X0 + 1 + Rng.nextInt(Math.max(1, R.X1 - R.X0 - 1));
            int Z = R.Z0 + 1 + Rng.nextInt(Math.max(1, R.Z1 - R.Z0 - 1));
            R.put(X, R.F + 1, Z, mod((Index % 2 == 0 ? "mop_bucket" : "wet_floor_sign") + "[facing=" + face(Rng.nextInt(4)) + "]"));
        }
        for (int Index = 0; Index < 3; Index++) {
            B.decal(R.X0 + Rng.nextInt(R.X1 - R.X0 + 1), R.F, R.Z0 + Rng.nextInt(R.Z1 - R.Z0 + 1), "up", Scp1730Builder.Puddle, R.Zone);
        }
    }

    static void lab(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("medical_cabinet"), 1);
        R.breach(1, false, R.Zone > 0);
        int[] Back = farWall(R);
        int Hoods = 0;
        for (int[] Cell : wall(R, Back[2])) {
            if (Hoods < 4 && tower(B, R, Cell[0], Cell[1], "research_fume_hood", face(Cell[2]))) {
                Hoods++;
            }
        }
        boolean AlongX = R.X1 - R.X0 >= R.Z1 - R.Z0;
        int Across0 = AlongX ? R.Z0 : R.X0;
        int Across1 = AlongX ? R.Z1 : R.X1;
        int Along0 = AlongX ? R.X0 : R.Z0;
        int Along1 = AlongX ? R.X1 : R.Z1;
        String[] Kit = {"research_microscope_station", "research_bench_centrifuge", "lab_tube_holder", "lab_analyzer"};
        for (int Side = 0; Side < 4; Side++) {
            if (Side == Back[2] || Side == (Back[2] + 2) % 4) {
                continue;
            }
            List<int[]> Run = wall(R, Side);
            for (int Index = 1; Index < Run.size() - 1; Index++) {
                int[] Cell = Run.get(Index);
                String Facing = face(Cell[2]);
                if (!R.put(Cell[0], R.F + 1, Cell[1], mod("lab_bench_desk[facing=" + Facing + ",left=" + (Index > 1) + ",right=" + (Index < Run.size() - 2) + "]"))) {
                    continue;
                }
                bench(B, R, Cell[0], Cell[1], Facing, Kit, Cell[0] + Scp1730Layout.Dx[Cell[2]], Cell[1] + Scp1730Layout.Dz[Cell[2]]);
            }
        }
        for (int Row = Across0 + 2; Row <= Across1 - 2; Row++) {
            int Placed = 0;
            String Facing = AlongX ? "south" : "east";
            for (int Along = Along0 + 3; Along <= Along1 - 3; Along++) {
                int X = AlongX ? Along : Row;
                int Z = AlongX ? Row : Along;
                if (R.free(AlongX ? X : X - 1, R.F + 1, AlongX ? Z - 1 : Z) && R.free(AlongX ? X : X + 1, R.F + 1, AlongX ? Z + 1 : Z)
                        && R.put(X, R.F + 1, Z, mod("lab_bench_desk[facing=" + Facing + ",left=" + (Placed > 0) + ",right=true]"))) {
                    bench(B, R, X, Z, Facing, Kit, AlongX ? X : X + 1, AlongX ? Z + 1 : Z);
                    Placed++;
                } else if (Placed > 0) {
                    break;
                }
            }
            if (Placed >= 3) {
                break;
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= 9) {
                break;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Count) {
                case 0 -> pair(B, R, Cell[0], Cell[1], "research_eyewash_station", Out);
                case 1 -> pair(B, R, Cell[0], Cell[1], "research_specimen_fridge", Out);
                case 2 -> R.put(Cell[0], R.F + 1, Cell[1], mod("research_cryo_unit[facing=" + Out + "]"));
                case 3 -> R.put(Cell[0], R.F + 1, Cell[1], mod("biohazard_bin[facing=" + Out + "]"));
                case 4 -> R.tall(Cell[0], Cell[1], mod("whiteboard"), "facing=" + Out + ",drawing=" + Rng.nextInt(4));
                default -> R.put(Cell[0], R.F + 1, Cell[1], mod((Count % 2 == 0 ? "chemical_shelf" : "lab_shelf") + "[facing=" + Out + "]"));
            };
            if (Done) {
                Count++;
            }
        }
        vignette(B, R);
        R.lights(5);
        R.mess(3);
    }

    static void bench(Scp1730Builder B, Scp1730Rooms.Box R, int X, int Z, String Facing, String[] Kit, int Sx, int Sz) {
        Random Rng = B.Rng;
        int Pick = Rng.nextInt(Kit.length + 2);
        if (Pick < Kit.length) {
            String Item = Kit[Pick];
            R.put(X, R.F + 2, Z, mod(Item + (Item.equals("lab_analyzer") ? "[facing=" + Facing + ",running=false]" : "[facing=" + Facing + "]")));
        } else if (Pick == Kit.length) {
            R.put(X, R.F + 2, Z, mod("lab_petri_dish[cultured=" + Rng.nextBoolean() + "]"));
        }
        if (Rng.nextInt(3) > 0) {
            R.put(Sx, R.F + 1, Sz, mod("lab_stool[facing=" + Scp1730Blocks.opposite(Facing) + "]"));
        }
    }

    static void ward(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("medical_cabinet"), 1);
        if (R.Zone >= 1) {
            R.wallContainer(mod("medical_cabinet"), 1);
        }
        R.breach(1, false, true);
        boolean AlongZ = R.Z1 - R.Z0 > R.X1 - R.X0;
        List<int[]> Beds = new ArrayList<>();
        for (int[] Cell : R.wallCells()) {
            boolean LongWall = AlongZ ? Cell[2] == 1 || Cell[2] == 3 : Cell[2] == 0 || Cell[2] == 2;
            if (!LongWall) {
                continue;
            }
            int Along = AlongZ ? Cell[1] : Cell[0];
            String Out = face(Cell[2]);
            int Hx = Cell[0] + Scp1730Layout.Dx[Cell[2]];
            int Hz = Cell[1] + Scp1730Layout.Dz[Cell[2]];
            if (Along % 3 == 1 && R.free(Cell[0], R.F + 1, Cell[1]) && R.free(Hx, R.F + 1, Hz)) {
                if (Rng.nextInt(6) == 0) {
                    R.put(Hx, R.F + 1, Hz, Scp1730Blocks.facing(Scp1730Blocks.Gurney, face(Cell[2] + 1)));
                    continue;
                }
                String Facing = face(Cell[2] + 2);
                B.Grid.set(Hx, R.F + 1, Hz, mod("hospital_bed[facing=" + Facing + ",part=foot]"));
                B.Grid.set(Cell[0], R.F + 1, Cell[1], mod("hospital_bed[facing=" + Facing + ",part=head]"));
                Beds.add(Cell);
                if (backed(B, Cell, R.F + 2)) {
                    R.put(Cell[0], R.F + 2, Cell[1], mod("medical_headwall_bottom[facing=" + Out + "]"));
                }
                if (Rng.nextBoolean()) {
                    B.decal(Hx, R.F, Hz, "up", Scp1730Builder.Blood, R.Zone);
                }
            } else if (Along % 3 == 2) {
                if (Rng.nextBoolean()) {
                    R.tall(Cell[0], Cell[1], mod("iv_stand"), "facing=" + Out);
                } else {
                    R.put(Cell[0], R.F + 1, Cell[1], mod("vitals_monitor[facing=" + Out + ",lit=" + (Rng.nextInt(4) == 0) + "]"));
                }
            }
        }
        for (int[] Bed : Beds) {
            if (Rng.nextInt(4) > 0) {
                bay(B, R, Bed);
            }
        }
        Scp1730Layout.Door Main = R.door();
        int Ix = -Scp1730Layout.Dx[Main.Dir];
        int Iz = -Scp1730Layout.Dz[Main.Dir];
        int Lx = Main.Dir % 2 == 0 ? 1 : 0;
        int Lz = Main.Dir % 2 == 0 ? 0 : 1;
        int Sx = R.Room.doorX(Main) + Ix * 2 + Lx * 3;
        int Sz = R.Room.doorZ(Main) + Iz * 2 + Lz * 3;
        String Desk = Scp1730Builder.facingOf(-Lx, -Lz);
        for (int Step = 0; Step < 3; Step++) {
            int X = Sx + Ix * Step;
            int Z = Sz + Iz * Step;
            if (!curtained(B, X, R.F + 1, Z) && R.put(X, R.F + 1, Z, mod("reception_desk[facing=" + Desk + ",left=" + (Step > 0) + ",right=" + (Step < 2) + ",shape=straight]")) && Step == 1) {
                R.put(X, R.F + 2, Z, mod("computer_terminal[facing=" + Desk + ",lit=false,screen=" + Rng.nextInt(10) + "]"));
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= 6) {
                break;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Count) {
                case 0 -> R.put(Cell[0], R.F + 1, Cell[1], mod("medical_scrub_sink_bottom[facing=" + Out + "]"));
                case 1 -> R.put(Cell[0], R.F + 1, Cell[1], mod("medical_cart[facing=" + Out + "]"));
                case 2 -> R.put(Cell[0], R.F + 1, Cell[1], mod("wheelchair[facing=" + face(Cell[2] + 1) + "]"));
                case 3 -> R.put(Cell[0], R.F + 1, Cell[1], mod("medical_supply_shelf[facing=" + Out + "]"));
                case 4 -> R.free(Cell[0], R.F + 1, Cell[1]) && backed(B, Cell, R.F + 2) && R.put(Cell[0], R.F + 2, Cell[1], mod("medical_defib_cabinet[facing=" + Out + "]"));
                default -> R.put(Cell[0], R.F + 1, Cell[1], mod("biohazard_bin[facing=" + Out + "]"));
            };
            if (Done) {
                Count++;
            }
        }
        vignette(B, R);
        R.lights(5);
        R.mess(4);
    }

    static boolean curtained(Scp1730Builder B, int X, int Y, int Z) {
        for (int Ox = -1; Ox <= 1; Ox++) {
            for (int Oz = -1; Oz <= 1; Oz++) {
                String Other = B.Grid.get(X + Ox, Y, Z + Oz);
                if (Other != null && (Other.startsWith(Scp1730SupportCheck.Curtain) || Other.startsWith(mod("hospital_bed")))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean curtain(Scp1730Builder B, Scp1730Rooms.Box R, int X, int Z) {
        if (R.H < 3 || !Scp1730SupportCheck.solid(B.Grid.get(X, R.F, Z)) || !Scp1730SupportCheck.solid(B.Grid.get(X, R.F + R.H + 1, Z))) {
            return false;
        }
        for (int Level = 1; Level <= R.H; Level++) {
            if (!R.free(X, R.F + Level, Z)) {
                return false;
            }
        }
        return true;
    }

    static void hang(Scp1730Builder B, Scp1730Rooms.Box R, int X, int Z, String Facing) {
        for (int Level = 1; Level <= R.H; Level++) {
            String Part = Level == 1 ? "bottom" : Level == R.H ? "top" : "middle";
            B.Grid.set(X, R.F + Level, Z, mod("medical_curtain_" + Part + "[facing=" + Facing + "]"));
        }
    }

    static void bay(Scp1730Builder B, Scp1730Rooms.Box R, int[] Bed) {
        int Ix = Scp1730Layout.Dx[Bed[2]];
        int Iz = Scp1730Layout.Dz[Bed[2]];
        int Lx = Bed[2] % 2 == 0 ? 1 : 0;
        int Lz = Bed[2] % 2 == 0 ? 0 : 1;
        List<int[]> Cells = new ArrayList<>();
        for (int Depth = 0; Depth <= 2; Depth++) {
            Cells.add(new int[]{Bed[0] + Lx * 2 + Ix * Depth, Bed[1] + Lz * 2 + Iz * Depth, 0});
        }
        Cells.add(new int[]{Bed[0] + Lx + Ix * 2, Bed[1] + Lz + Iz * 2, 1});
        for (int[] Cell : Cells) {
            if (!curtain(B, R, Cell[0], Cell[1])) {
                return;
            }
        }
        for (int[] Cell : Cells) {
            hang(B, R, Cell[0], Cell[1], Cell[2] == 0 ? face(Bed[2] + 1) : face(Bed[2]));
        }
    }

    static void dorms(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("locker"), 0);
        R.breach(1, false, true);
        for (int[] Cell : R.wallCells()) {
            int Along = Cell[2] % 2 == 0 ? Cell[0] : Cell[1];
            String Facing = face(Cell[2]);
            int Ex = Cell[0] + Scp1730Layout.Dx[Cell[2]];
            int Ez = Cell[1] + Scp1730Layout.Dz[Cell[2]];
            if (Along % 3 == 0 && R.free(Cell[0], R.F + 1, Cell[1]) && R.free(Ex, R.F + 1, Ez)) {
                B.Grid.set(Cell[0], R.F + 1, Cell[1], mod("lcz_cell_bunk[facing=" + Facing + ",pair=main]"));
                B.Grid.set(Ex, R.F + 1, Ez, mod("lcz_cell_bunk[facing=" + Facing + ",pair=extension]"));
            } else if (Along % 3 == 1) {
                R.tall(Cell[0], Cell[1], mod("locker"), "facing=" + Facing + ",open=" + (Rng.nextInt(3) == 0));
            } else if (Rng.nextInt(3) == 0) {
                R.tall(Cell[0], Cell[1], mod("coat_rack"), "facing=" + Facing);
            }
        }
        boolean AlongX = R.X1 - R.X0 >= R.Z1 - R.Z0;
        int Centre = AlongX ? R.cz() : R.cx();
        int Low = AlongX ? R.Z0 : R.X0;
        int High = AlongX ? R.Z1 : R.X1;
        for (int Mid : new int[]{Centre - 4, Centre + 3}) {
            if (Mid - 1 < Low + 2 || Mid + 2 > High - 2) {
                continue;
            }
            for (int Along = (AlongX ? R.X0 : R.Z0) + 3; Along <= (AlongX ? R.X1 : R.Z1) - 3; Along += 2) {
                for (int Side = 0; Side <= 1; Side++) {
                    int X = AlongX ? Along : Mid + Side;
                    int Z = AlongX ? Mid + Side : Along;
                    int Ex = AlongX ? X : X + (Side == 0 ? -1 : 1);
                    int Ez = AlongX ? Z + (Side == 0 ? -1 : 1) : Z;
                    String Facing = AlongX ? (Side == 0 ? "north" : "south") : (Side == 0 ? "west" : "east");
                    if (R.free(X, R.F + 1, Z) && R.free(Ex, R.F + 1, Ez)) {
                        B.Grid.set(X, R.F + 1, Z, mod("lcz_cell_bunk[facing=" + Facing + ",pair=main]"));
                        B.Grid.set(Ex, R.F + 1, Ez, mod("lcz_cell_bunk[facing=" + Facing + ",pair=extension]"));
                    }
                }
            }
        }
        for (int[] Cell : R.wallCells()) {
            if (R.free(Cell[0], R.F + 1, Cell[1]) && R.free(Cell[0], R.F + 2, Cell[1]) && wallProp(B, R, Cell, "lcz_notice_board")) {
                break;
            }
        }
        vignette(B, R);
        R.lights(5);
        R.mess(8);
    }

    static void vignette(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        if (Rng.nextInt(100) < (R.Zone == 0 ? 45 : 20)) {
            return;
        }
        switch (Rng.nextInt(4)) {
            case 0 -> barricade(B, R);
            case 1 -> lastStand(B, R);
            case 2 -> dragged(B, R);
            default -> sealed(B, R);
        }
    }

    static void barricade(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        Scp1730Layout.Door Main = R.door();
        int Ix = -Scp1730Layout.Dx[Main.Dir];
        int Iz = -Scp1730Layout.Dz[Main.Dir];
        int Lx = Main.Dir % 2 == 0 ? 1 : 0;
        int Lz = Main.Dir % 2 == 0 ? 0 : 1;
        int Cx = R.Room.doorX(Main);
        int Cz = R.Room.doorZ(Main);
        String Across = face(Main.Dir + 1);
        String[] Junk = {Scp1730Blocks.OverturnedDesk, Scp1730Blocks.Barricade, Scp1730Blocks.OverturnedChair, Scp1730Blocks.FallenPanel};
        for (int Side = -1; Side <= 1; Side += 2) {
            for (int Depth = 1; Depth <= 2; Depth++) {
                R.put(Cx + Ix * Depth + Lx * Side * 2, R.F + 1, Cz + Iz * Depth + Lz * Side * 2, Scp1730Blocks.facing(Junk[Rng.nextInt(Junk.length)], Across));
            }
        }
        R.put(Cx + Ix * 3 + Lx * 3, R.F + 1, Cz + Iz * 3 + Lz * 3, mod("filing_cabinet[facing=" + face(Main.Dir) + ",open=true]"));
        for (int Side = -1; Side <= 1; Side += 2) {
            B.decal(Cx + Lx * Side * 2, R.F + 2, Cz + Lz * Side * 2, face(Main.Dir + 2), Scp1730Builder.Claws, R.Zone);
        }
        B.decal(Cx + Ix, R.F, Cz + Iz, "up", Scp1730Builder.Footprints, R.Zone);
    }

    static void lastStand(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        int[] Back = farWall(R);
        int Dx = Scp1730Layout.Dx[Back[2]];
        int Dz = Scp1730Layout.Dz[Back[2]];
        int Lx = Back[2] % 2 == 0 ? 1 : 0;
        int Lz = Back[2] % 2 == 0 ? 0 : 1;
        String Toward = face(Back[2]);
        int Bx = Back[0] + Dx * 3;
        int Bz = Back[1] + Dz * 3;
        for (int Side = -2; Side <= 2; Side++) {
            if (Side == 0 && Rng.nextBoolean()) {
                continue;
            }
            R.put(Bx + Lx * Side, R.F + 1, Bz + Lz * Side, Scp1730Blocks.facing(Scp1730Blocks.Sandbags, Toward));
        }
        R.put(Bx - Dx + Lx * 2, R.F + 1, Bz - Dz + Lz * 2, Scp1730Blocks.facing(Scp1730Blocks.AmmoCrate, Toward));
        R.put(Bx - Dx - Lx, R.F + 1, Bz - Dz - Lz, Scp1730Blocks.facing(Scp1730Blocks.BodyBag, face(Back[2] + 1)));
        B.decal(Bx - Dx, R.F, Bz - Dz, "up", Scp1730Builder.Scorch, R.Zone);
        B.decal(Bx, R.F, Bz, "up", Scp1730Builder.Blood, R.Zone);
        Scp1730Layout.Door Main = R.door();
        int Mx = Main.Dir % 2 == 0 ? 1 : 0;
        int Mz = Main.Dir % 2 == 0 ? 0 : 1;
        for (int Side = -3; Side <= 3; Side += 2) {
            B.decal(R.Room.doorX(Main) + Mx * Side, R.F + 1 + Rng.nextInt(2), R.Room.doorZ(Main) + Mz * Side, face(Main.Dir + 2), Scp1730Builder.Bullets, R.Zone);
        }
    }

    static void dragged(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        for (int[] Cell : Cells) {
            int Wx = Cell[0] - Scp1730Layout.Dx[Cell[2]];
            int Wz = Cell[1] - Scp1730Layout.Dz[Cell[2]];
            if (!Scp1730Builder.isWallLike(B.Grid.get(Wx, R.F + 1, Wz)) || !R.free(Cell[0], R.F + 1, Cell[1])) {
                continue;
            }
            B.Grid.set(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.BrokenVent, face(Cell[2])));
            B.decal(Wx, R.F + 2, Wz, face(Cell[2]), Scp1730Builder.Claws, R.Zone);
            int X = R.cx();
            int Z = R.cz();
            for (int Steps = 1; Steps < 24 && (X != Cell[0] || Z != Cell[1]); Steps++) {
                if (X != Cell[0] && (Z == Cell[1] || Rng.nextBoolean())) {
                    X += Integer.signum(Cell[0] - X);
                } else {
                    Z += Integer.signum(Cell[1] - Z);
                }
                if (X == Cell[0] && Z == Cell[1]) {
                    break;
                }
                if (Steps % 3 == 0) {
                    B.decal(X, R.F, Z, "up", Scp1730Builder.Blood, R.Zone);
                } else if (B.Grid.empty(X, R.F + 1, Z)) {
                    B.Grid.set(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.DragMarks, "up"));
                }
            }
            return;
        }
    }

    static void sealed(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        for (int Side = 0; Side < 4; Side++) {
            int Into = (Side + 2) % 4;
            if (doorOn(R, Side) || R.H < 3) {
                continue;
            }
            List<int[]> Cells = wall(R, Into);
            if (Cells.size() < 5) {
                continue;
            }
            int[] Middle = Cells.get(Cells.size() / 2);
            int Wx = Middle[0] - Scp1730Layout.Dx[Into];
            int Wz = Middle[1] - Scp1730Layout.Dz[Into];
            int Lx = Into % 2 == 0 ? 1 : 0;
            int Lz = Into % 2 == 0 ? 0 : 1;
            boolean Wall = true;
            for (int Level = 1; Level <= 3; Level++) {
                String Here = B.Grid.get(Wx, R.F + Level, Wz);
                Wall &= Scp1730Builder.isWallLike(Here) || Here != null && Here.endsWith("_wall_top");
            }
            if (!Wall || !R.free(Middle[0], R.F + 1, Middle[1])) {
                continue;
            }
            for (int Level = 1; Level <= 3; Level++) {
                B.Grid.set(Wx, R.F + Level, Wz, Scp1730Blocks.facing(Scp1730Blocks.HeavyDoor, face(Into)));
            }
            for (int Offset = -1; Offset <= 1; Offset += 2) {
                B.decal(Wx + Lx * Offset, R.F + 1 + Rng.nextInt(2), Wz + Lz * Offset, face(Into), Scp1730Builder.Claws, R.Zone);
            }
            B.decal(Middle[0], R.F, Middle[1], "up", Rng.nextBoolean() ? Scp1730Builder.Blood : Scp1730Builder.Cracks, R.Zone);
            R.put(Middle[0] + Scp1730Layout.Dx[Into] * 2 + Lx, R.F + 1, Middle[1] + Scp1730Layout.Dz[Into] * 2 + Lz, Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Into + 1)));
            B.keepClear(Middle[0], R.F + 1, Middle[1]);
            B.keepClear(Middle[0], R.F + 2, Middle[1]);
            return;
        }
    }
}
