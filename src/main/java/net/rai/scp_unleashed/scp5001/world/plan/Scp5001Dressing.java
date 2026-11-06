package net.rai.scp_unleashed.scp5001.world.plan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class Scp5001Dressing {
    public static final Set<String> WallRoles = Set.of("lab_wall_unit", "pipe_wall", "service_panel", "tool_rack", "wall_screen", "signage", "med_cabinet", "drone_dock");
    public static final Set<String> CeilingRoles = Set.of("lab_ceiling", "pipe_ceiling", "cable_ceiling", "camera");

    record Recipe(String[] Wall, int Spacing, String Mode, String[] Island, String[] Clutter, int Clutters, String[] Ceiling, int Pool, int TerminalChance) {
    }

    static final Map<String, Recipe> Recipes = new HashMap<>();

    static {
        recipe("lab", new String[]{"lab_wall_unit", "fume_hood", "specimen_tank", "sample_rack", "analysis_machine", "scope_station"}, 1, "rows", new String[]{"lab_bench"},
                new String[]{"lab_clutter", "lab_cart", "lab_clutter"}, 4, new String[]{"lab_ceiling"}, 1, 60);
        recipe("field_lab", new String[]{"analysis_machine", "scope_station", "lab_wall_unit", "specimen_tank", "wall_screen"}, 1, "center", new String[]{"holo_table"},
                new String[]{"lab_clutter", "lab_cart"}, 3, new String[]{"lab_ceiling"}, 7, 70);
        recipe("specimen_vault", new String[]{"specimen_tank", "specimen_tank", "sample_rack", "lab_wall_unit"}, 1, "rows", new String[]{"specimen_tank"},
                new String[]{"lab_clutter"}, 2, new String[]{"lab_ceiling", "camera"}, 1, 50);
        recipe("medbay", new String[]{"med_cabinet", "med_bed", "analysis_machine", "locker", "med_bed"}, 1, "rows", new String[]{"med_bed"},
                new String[]{"lab_cart", "hab_clutter"}, 3, new String[]{"lab_ceiling"}, 5, 70);
        recipe("server", new String[]{"server_rack", "service_panel", "server_rack"}, 1, "rows", new String[]{"server_rack"}, new String[]{"ctrl_clutter"}, 2,
                new String[]{"cable_ceiling"}, 3, 80);
        recipe("archive", new String[]{"archive_cabinet", "archive_cabinet", "signage", "archive_cabinet"}, 1, "rows", new String[]{"archive_cabinet"},
                new String[]{"ctrl_clutter"}, 3, new String[]{"camera"}, 6, 90);
        recipe("records", new String[]{"archive_cabinet", "shelf", "workstation", "archive_cabinet"}, 1, "scatter", new String[]{"workstation"}, new String[]{"ctrl_clutter"}, 4,
                new String[]{"lab_ceiling"}, 6, 60);
        recipe("office", new String[]{"workstation", "archive_cabinet", "shelf", "wall_screen", "workstation"}, 2, "scatter", new String[]{"workstation"},
                new String[]{"ctrl_clutter", "hab_clutter"}, 4, new String[]{"camera"}, 0, 40);
        recipe("conference", new String[]{"wall_screen", "shelf", "wall_screen"}, 2, "center", new String[]{"holo_table"}, new String[]{"ctrl_clutter"}, 2,
                new String[]{"camera"}, 0, 40);
        recipe("comms", new String[]{"server_rack", "workstation", "wall_screen", "workstation"}, 1, "center", new String[]{"holo_table"}, new String[]{"ctrl_clutter"}, 3,
                new String[]{"cable_ceiling"}, 3, 70);
        recipe("monitoring", new String[]{"workstation", "wall_screen", "server_rack", "workstation"}, 1, "center", new String[]{"holo_table"}, new String[]{"ctrl_clutter"}, 2,
                new String[]{"camera", "cable_ceiling"}, 7, 80);
        recipe("observation", new String[]{"wall_screen", "shelf", "signage"}, 2, "tables", new String[]{"mess_table"}, new String[]{"hab_clutter"}, 2,
                new String[]{"lab_ceiling"}, 7, 60);
        recipe("security", new String[]{"weapon_locker", "wall_screen", "workstation", "signage", "weapon_locker"}, 1, "scatter", new String[]{"workstation"},
                new String[]{"ctrl_clutter"}, 2, new String[]{"camera"}, 3, 60);
        recipe("checkpoint", new String[]{"weapon_locker", "workstation", "wall_screen", "signage"}, 1, "gates", new String[]{"security_gate"}, new String[]{"ctrl_clutter"}, 2,
                new String[]{"camera"}, 3, 100);
        recipe("airlock", new String[]{"signage", "service_panel"}, 4, "none", new String[]{}, new String[]{}, 0, new String[]{"camera"}, 0, 0);
        recipe("armory", new String[]{"weapon_locker", "weapon_locker", "tool_rack", "crate"}, 1, "scatter", new String[]{"crate"}, new String[]{"eng_clutter"}, 2,
                new String[]{"camera"}, 3, 30);
        recipe("quarters", new String[]{"locker", "shelf", "locker", "bunk"}, 1, "rows", new String[]{"bunk"}, new String[]{"hab_clutter"}, 4, new String[]{"lab_ceiling"}, 4, 40);
        recipe("mess", new String[]{"galley_unit", "shelf", "galley_unit"}, 1, "tables", new String[]{"mess_table"}, new String[]{"hab_clutter"}, 5, new String[]{"lab_ceiling"}, 4,
                70);
        recipe("galley", new String[]{"galley_unit", "galley_unit", "shelf"}, 1, "scatter", new String[]{"crate"}, new String[]{"hab_clutter"}, 3, new String[]{"pipe_ceiling"}, 4,
                30);
        recipe("lounge", new String[]{"shelf", "wall_screen", "galley_unit"}, 2, "tables", new String[]{"mess_table"}, new String[]{"hab_clutter"}, 3, new String[]{"lab_ceiling"},
                4, 40);
        recipe("storage", new String[]{"shelf", "crate", "shelf", "crate"}, 1, "scatter", new String[]{"crate"}, new String[]{"eng_clutter", "hab_clutter"}, 4,
                new String[]{"cable_ceiling"}, 2, 30);
        recipe("drone_bay", new String[]{"drone_dock", "tool_rack", "drone_dock", "crate"}, 1, "scatter", new String[]{"crate"}, new String[]{"eng_clutter"}, 3,
                new String[]{"cable_ceiling"}, 2, 40);
        recipe("substation", new String[]{"transformer", "capacitor_bank", "service_panel", "pipe_wall", "capacitor_bank"}, 1, "rows", new String[]{"transformer"},
                new String[]{"eng_clutter", "conduit_floor"}, 3, new String[]{"cable_ceiling", "pipe_ceiling"}, 2, 50);
        recipe("coolant", new String[]{"coolant_tank", "pipe_wall", "coolant_tank", "service_panel"}, 1, "rows", new String[]{"coolant_tank"},
                new String[]{"eng_clutter", "conduit_floor"}, 3, new String[]{"pipe_ceiling"}, 2, 50);
        recipe("workshop", new String[]{"tool_rack", "service_panel", "shelf", "tool_rack"}, 1, "scatter", new String[]{"crate"}, new String[]{"eng_clutter"}, 4,
                new String[]{"cable_ceiling"}, 2, 40);
        recipe("control", new String[]{"server_rack", "wall_screen", "server_rack", "wall_screen"}, 1, "control", new String[]{"workstation"}, new String[]{"ctrl_clutter"}, 3,
                new String[]{"camera"}, 3, 100);
        recipe("emergency", new String[]{"signage", "service_panel"}, 3, "none", new String[]{}, new String[]{}, 0, new String[]{"camera"}, 3, 0);
        recipe("generator", new String[]{"capacitor_bank", "transformer", "service_panel", "pipe_wall", "coolant_tank", "capacitor_bank"}, 1, "none", new String[]{},
                new String[]{"eng_clutter", "conduit_floor"}, 6, new String[]{"cable_ceiling"}, 2, 100);
        recipe("engine", new String[]{"service_panel", "pipe_wall", "capacitor_bank", "coolant_tank"}, 1, "none", new String[]{}, new String[]{"conduit_floor"}, 6,
                new String[]{"cable_ceiling"}, 2, 100);
    }

    private static void recipe(String Type, String[] Wall, int Spacing, String Mode, String[] Island, String[] Clutter, int Clutters, String[] Ceiling, int Pool,
                               int TerminalChance) {
        Recipes.put(Type, new Recipe(Wall, Spacing, Mode, Island, Clutter, Clutters, Ceiling, Pool, TerminalChance));
    }

    private final Scp5001Plan Plan;
    private final Scp5001Layout Layout;
    private final Random Random;
    private final Set<Long> Used = new HashSet<>();

    Scp5001Dressing(Scp5001Plan Plan, Scp5001Layout Layout) {
        this.Plan = Plan;
        this.Layout = Layout;
        this.Random = Plan.Random;
    }

    private static long key(int X, int Y, int Z) {
        return ((long) X << 32) ^ ((long) Y << 16) ^ Z;
    }

    private boolean freeCell(int X, int Y, int Z) {
        return Plan.air(X, Y, Z) && !Used.contains(key(X, Y, Z));
    }

    private void use(int X, int Y, int Z) {
        Used.add(key(X, Y, Z));
    }

    void dress() {
        for (Scp5001Anchor Each : Plan.Anchors) {
            int Radius = Each.Type().equals(Scp5001Blocks.Generator) ? 2 : Each.Type().equals(Scp5001Blocks.Engine) ? 4 : 1;
            for (int Dx = -Radius; Dx <= Radius; Dx++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    for (int Dy = -1; Dy <= 3; Dy++) {
                        use(Each.X() + Dx, Each.Y() + Dy, Each.Z() + Dz);
                    }
                }
            }
        }
        for (Scp5001Anchor Intake : Plan.anchors(Scp5001Blocks.EmergencyIntake)) {
            int[] Step = step(Intake.Facing());
            for (int Ahead = 0; Ahead <= 4; Ahead++) {
                for (int Across = -2; Across <= 2; Across++) {
                    for (int Dy = 0; Dy <= 3; Dy++) {
                        use(Intake.X() + Step[0] * Ahead + Step[1] * Across, Intake.Y() + Dy, Intake.Z() + Step[1] * Ahead + Step[0] * Across);
                    }
                }
            }
        }
        for (Scp5001Anchor Console : Plan.anchors(Scp5001Blocks.ConsoleMain)) {
            for (int Dx = -2; Dx <= 2; Dx++) {
                for (int Dz = -2; Dz <= 2; Dz++) {
                    for (int Dy = 0; Dy <= 3; Dy++) {
                        use(Console.X() + Dx, Console.Y() + Dy, Console.Z() + Dz);
                    }
                }
            }
        }
        for (Scp5001Room Room : Plan.Rooms) {
            for (Scp5001Door Door : Room.Doors) {
                blockDoor(Door);
            }
        }
        int Terminals = 0;
        for (Scp5001Room Room : Plan.Rooms) {
            if (Room.corridor()) {
                corridor(Room);
                continue;
            }
            Recipe Found = Recipes.get(Room.Type);
            if (Found == null || Room.Kind.equals(Scp5001Plan.KindStair) || Room.Type.equals("void")) {
                continue;
            }
            if (terminal(Room, Found)) {
                Terminals++;
            }
            room(Room, Found);
        }
    }

    private void blockDoor(Scp5001Door Door) {
        int Ax = Door.AlongX() ? 1 : 0;
        int Az = Door.AlongX() ? 0 : 1;
        int Nx = Door.AlongX() ? 0 : 1;
        int Nz = Door.AlongX() ? 1 : 0;
        for (int Column = -1; Column <= 2; Column++) {
            for (int Depth = -3; Depth <= 3; Depth++) {
                for (int Row = 0; Row <= 3; Row++) {
                    use(Door.X() + Ax * Column + Nx * Depth, Door.Y() + Row, Door.Z() + Az * Column + Nz * Depth);
                }
            }
        }
    }

    private int floorOf(Scp5001Room Room) {
        return Scp5001Plan.floorY(Room.Level);
    }

    private int ceilingOf(Scp5001Room Room) {
        if (Room.Kind.equals(Scp5001Plan.KindHall)) {
            return Scp5001Plan.floorY(Room.Level + 1) + Scp5001Plan.RoomHeight;
        }
        return floorOf(Room) + (Room.corridor() ? Scp5001Plan.CorridorHeight : Scp5001Plan.RoomHeight);
    }

    private String pick(String[] Roles, int Index) {
        return Roles[Math.floorMod(Index, Roles.length)];
    }

    private void slot(String Role, int X, int Y, int Z, String Facing, boolean Wall, int Ceiling, Scp5001Room Room) {
        Plan.Slots.add(new Scp5001Slot(Role, X, Y, Z, Facing, Wall, Ceiling, Room.Id));
        use(X, Y, Z);
        use(X, Y + 1, Z);
        if (Wall) {
            use(X, Y + 2, Z);
        }
    }

    private boolean roomCell(Scp5001Room Room, int X, int Z) {
        int Level = Room.Level;
        return Plan.region(Level, X, Z) == Room.Id;
    }

    static int[] step(String Facing) {
        return switch (Facing) {
            case "north" -> new int[]{0, -1};
            case "south" -> new int[]{0, 1};
            case "east" -> new int[]{1, 0};
            case "west" -> new int[]{-1, 0};
            default -> new int[]{0, 0};
        };
    }

    private static final int[][] Sides = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final String[] Away = {"south", "west", "north", "east"};

    private int wallSide(Scp5001Room Room, int X, int Y, int Z) {
        for (int Side = 0; Side < 4; Side++) {
            int Wx = X + Sides[Side][0];
            int Wz = Z + Sides[Side][1];
            if (Plan.region(Room.Level, Wx, Wz) == 0 && !Plan.air(Wx, Y, Wz) && !Plan.air(Wx, Y + 1, Wz) && !Plan.air(Wx, Y + 2, Wz)
                    && !Plan.get(Wx, Y + 1, Wz).startsWith(Scp5001Blocks.Door) && !Plan.get(Wx, Y + 1, Wz).equals(Scp5001Blocks.Glass)) {
                return Side;
            }
        }
        return -1;
    }

    private int[][] distances(Scp5001Room Room) {
        int Level = Room.Level;
        int[][] Distance = new int[Scp5001Plan.Width][Scp5001Plan.Depth];
        java.util.ArrayDeque<int[]> Queue = new java.util.ArrayDeque<>();
        for (int X = Room.X0; X <= Room.X1; X++) {
            for (int Z = Room.Z0; Z <= Room.Z1; Z++) {
                if (Plan.region(Level, X, Z) != Room.Id) {
                    continue;
                }
                for (int[] Side : Sides) {
                    if (Plan.region(Level, X + Side[0], Z + Side[1]) != Room.Id) {
                        Distance[X][Z] = 1;
                        Queue.add(new int[]{X, Z});
                        break;
                    }
                }
            }
        }
        while (!Queue.isEmpty()) {
            int[] At = Queue.poll();
            for (int[] Side : Sides) {
                int X = At[0] + Side[0];
                int Z = At[1] + Side[1];
                if (X < 0 || Z < 0 || X >= Scp5001Plan.Width || Z >= Scp5001Plan.Depth || Plan.region(Level, X, Z) != Room.Id || Distance[X][Z] != 0) {
                    continue;
                }
                Distance[X][Z] = Distance[At[0]][At[1]] + 1;
                Queue.add(new int[]{X, Z});
            }
        }
        return Distance;
    }

    private boolean terminal(Scp5001Room Room, Recipe Found) {
        if (Found.TerminalChance() <= 0 || Random.nextInt(100) >= Found.TerminalChance() * 2 / 3) {
            return false;
        }
        int Y = floorOf(Room) + 1;
        java.util.List<int[]> Options = new java.util.ArrayList<>();
        for (int X = Room.X0; X <= Room.X1; X++) {
            for (int Z = Room.Z0; Z <= Room.Z1; Z++) {
                if (!roomCell(Room, X, Z) || !freeCell(X, Y, Z) || !freeCell(X, Y + 1, Z)) {
                    continue;
                }
                int Side = wallSide(Room, X, Y, Z);
                if (Side >= 0) {
                    Options.add(new int[]{X, Z, Side});
                }
            }
        }
        if (Options.isEmpty()) {
            return false;
        }
        int[] Pick = Options.get(Random.nextInt(Options.size()));
        Plan.set(Pick[0], Y, Pick[1], Scp5001Blocks.Terminal + "[facing=" + Away[Pick[2]] + ",pool=" + Found.Pool() + "]");
        use(Pick[0], Y, Pick[1]);
        use(Pick[0], Y + 1, Pick[1]);
        Room.Terminal = true;
        return true;
    }

    private void room(Scp5001Room Room, Recipe Found) {
        int Y = floorOf(Room) + 1;
        int Ceiling = ceilingOf(Room);
        int Offset = Random.nextInt(97);
        int Index = 0;
        int Counter = 0;
        int[][] Distance = distances(Room);
        boolean Hall = Room.Kind.equals(Scp5001Plan.KindHall);
        for (int X = Room.X0; X <= Room.X1; X++) {
            for (int Z = Room.Z0; Z <= Room.Z1; Z++) {
                if (!roomCell(Room, X, Z) || Distance[X][Z] != 1) {
                    continue;
                }
                int Side = wallSide(Room, X, Y, Z);
                if (Side < 0) {
                    continue;
                }
                Counter++;
                if (Found.Spacing() > 1 && Math.floorMod(X + Z, Found.Spacing()) != 0) {
                    continue;
                }
                if (!freeCell(X, Y, Z) || !freeCell(X, Y + 1, Z) || !freeCell(X, Y + 2, Z)) {
                    continue;
                }
                String Role = pick(Found.Wall(), Offset + Index);
                Index++;
                slot(Role, X, Y, Z, Away[Side], true, Ceiling, Room);
            }
        }
        if (!Hall && Found.Island().length > 0) {
            islands(Room, Found, Y, Ceiling, Distance);
        }
        if (Room.Type.equals("control")) {
            control(Room, Y, Ceiling);
        }
        int Clutter = Found.Clutters() * Math.max(1, Room.Cells / 150);
        for (int Count = 0; Count < Clutter * 4 && Clutter > 0 && Found.Clutter().length > 0; Count++) {
            int X = Room.X0 + Random.nextInt(Math.max(1, Room.X1 - Room.X0 + 1));
            int Z = Room.Z0 + Random.nextInt(Math.max(1, Room.Z1 - Room.Z0 + 1));
            if (!roomCell(Room, X, Z) || Distance[X][Z] < 2 || !freeCell(X, Y, Z) || !freeCell(X, Y + 1, Z)) {
                continue;
            }
            slot(pick(Found.Clutter(), Count + Offset), X, Y, Z, Away[Random.nextInt(4)], false, Ceiling, Room);
            Clutter--;
            if (Clutter <= 0) {
                break;
            }
        }
        if (Found.Ceiling().length > 0) {
            int Step = Hall ? 3 : 4;
            for (int X = Room.X0; X <= Room.X1; X++) {
                for (int Z = Room.Z0; Z <= Room.Z1; Z++) {
                    if (Math.floorMod(X, Step) != 0 || Math.floorMod(Z, Step) != 0 || !roomCell(Room, X, Z) || Distance[X][Z] < 2) {
                        continue;
                    }
                    String Role = pick(Found.Ceiling(), X * 3 + Z);
                    if (Role.equals("camera") && Math.floorMod(X * 5 + Z * 7, 3) != 0) {
                        continue;
                    }
                    if (!freeCell(X, Ceiling, Z) || !freeCell(X, Ceiling - 1, Z) || Plan.get(X, Ceiling + 1, Z).equals(Scp5001Blocks.CeilingLight)) {
                        continue;
                    }
                    Plan.Slots.add(new Scp5001Slot(Role, X, Ceiling, Z, Away[Math.floorMod(X + Z, 4)], false, Ceiling, Room.Id));
                    use(X, Ceiling, Z);
                }
            }
        }
    }

    private void islands(Scp5001Room Room, Recipe Found, int Y, int Ceiling, int[][] Distance) {
        int Deepest = 0;
        int[] Centre = null;
        for (int X = Room.X0; X <= Room.X1; X++) {
            for (int Z = Room.Z0; Z <= Room.Z1; Z++) {
                if (roomCell(Room, X, Z) && Distance[X][Z] > Deepest) {
                    Deepest = Distance[X][Z];
                    Centre = new int[]{X, Z};
                }
            }
        }
        if (Centre == null || Deepest < 3) {
            return;
        }
        switch (Found.Mode()) {
            case "rows" -> {
                for (int X = Room.X0; X <= Room.X1; X++) {
                    for (int Z = Room.Z0; Z <= Room.Z1; Z++) {
                        int Depth = Distance[X][Z];
                        if (!roomCell(Room, X, Z) || Depth < 3 || Math.floorMod(Depth - 3, 3) != 0 || Math.floorMod(X * 7 + Z * 3, 6) == 0) {
                            continue;
                        }
                        island(Room, Found.Island()[0], X, Y, Z, facingOut(Room, X, Z, Distance), Ceiling);
                    }
                }
            }
            case "tables" -> {
                for (int X = Room.X0; X <= Room.X1; X++) {
                    for (int Z = Room.Z0; Z <= Room.Z1; Z++) {
                        int Depth = Distance[X][Z];
                        if (!roomCell(Room, X, Z) || Depth < 4 || Math.floorMod(Depth - 4, 4) != 0 || Math.floorMod(X + Z, 2) != 0 || Math.floorMod(X * 5 + Z * 3, 7) == 0) {
                            continue;
                        }
                        if (island(Room, Found.Island()[0], X, Y, Z, "north", Ceiling)) {
                            for (int Side = 0; Side < 4; Side++) {
                                int Sx = X + Sides[Side][0];
                                int Sz = Z + Sides[Side][1];
                                if (roomCell(Room, Sx, Sz) && Distance[Sx][Sz] != Depth && Math.floorMod(Side + X, 2) == 0) {
                                    island(Room, "mess_seat", Sx, Y, Sz, towards(Side), Ceiling);
                                }
                            }
                        }
                    }
                }
            }
            case "center" -> {
                island(Room, Found.Island()[0], Centre[0], Y, Centre[1], "south", Ceiling);
                if (Room.Type.equals("conference")) {
                    for (int Side = 0; Side < 4; Side++) {
                        island(Room, "mess_seat", Centre[0] + Sides[Side][0] * 2, Y, Centre[1] + Sides[Side][1] * 2, towards(Side), Ceiling);
                    }
                }
            }
            case "gates" -> {
                for (int X = Room.X0; X <= Room.X1; X += 2) {
                    int Z = Centre[1];
                    if (Math.abs(X - Centre[0]) <= 1) {
                        continue;
                    }
                    island(Room, Found.Island()[0], X, Y, Z, "north", Ceiling);
                }
            }
            case "scatter" -> {
                int Count = Math.max(1, Room.Cells / 40);
                for (int Attempt = 0; Attempt < Count * 6 && Count > 0; Attempt++) {
                    int X = Room.X0 + Random.nextInt(Math.max(1, Room.X1 - Room.X0 + 1));
                    int Z = Room.Z0 + Random.nextInt(Math.max(1, Room.Z1 - Room.Z0 + 1));
                    if (!roomCell(Room, X, Z) || Distance[X][Z] < 3) {
                        continue;
                    }
                    if (island(Room, pick(Found.Island(), Attempt), X, Y, Z, Away[Random.nextInt(4)], Ceiling)) {
                        Count--;
                    }
                }
            }
            default -> {
            }
        }
    }

    private static String towards(int Side) {
        return new String[]{"south", "west", "north", "east"}[Side];
    }

    private String facingOut(Scp5001Room Room, int X, int Z, int[][] Distance) {
        int Depth = Distance[X][Z];
        for (int Side = 0; Side < 4; Side++) {
            int Nx = X + Sides[Side][0];
            int Nz = Z + Sides[Side][1];
            if (roomCell(Room, Nx, Nz) && Distance[Nx][Nz] > Depth) {
                return new String[]{"north", "east", "south", "west"}[Side];
            }
        }
        return "north";
    }

    private boolean island(Scp5001Room Room, String Role, int X, int Y, int Z, String Facing, int Ceiling) {
        if (!roomCell(Room, X, Z) || !freeCell(X, Y, Z) || !freeCell(X, Y + 1, Z)) {
            return false;
        }
        slot(Role, X, Y, Z, Facing, false, Ceiling, Room);
        return true;
    }

    private void control(Scp5001Room Room, int Y, int Ceiling) {
        int Cx = Scp5001Plan.CentreX;
        int Cz = Scp5001Plan.CentreZ;
        for (int Row = 0; Row < 3; Row++) {
            int X = Cx - 28 - Row * 3;
            for (int Dz = -9; Dz <= 9; Dz++) {
                if (Math.abs(Dz) <= 2 || Math.floorMod(Dz, 4) == 0) {
                    continue;
                }
                island(Room, "workstation", X, Y, Cz + Dz, "west", Ceiling);
            }
        }
        island(Room, "holo_table", Cx - 38, Y, Cz - 6, "east", Ceiling);
        island(Room, "holo_table", Cx - 38, Y, Cz + 6, "east", Ceiling);
    }

    private void corridor(Scp5001Room Room) {
        int Level = Room.Level;
        int Floor = Scp5001Plan.floorY(Level);
        int Y = Floor + 1;
        int Ceiling = Floor + Scp5001Plan.CorridorHeight;
        int[][] Steps = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        String[] Out = {"south", "west", "north", "east"};
        boolean Maintenance = Room.Type.equals("maintenance");
        for (int X = 0; X < Scp5001Plan.Width; X++) {
            for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                if (Plan.region(Level, X, Z) != Room.Id) {
                    continue;
                }
                long Mixed = (X * 73856093L) ^ (Z * 19349663L) ^ (Level * 83492791L) ^ Plan.seed();
                int Roll = (int) Math.floorMod(Mixed, 1000);
                int Walls = 0;
                int Side = -1;
                for (int Index = 0; Index < 4; Index++) {
                    int Wx = X + Steps[Index][0];
                    int Wz = Z + Steps[Index][1];
                    if (Plan.region(Level, Wx, Wz) == 0 && !Plan.air(Wx, Y + 1, Wz) && !Plan.get(Wx, Y + 1, Wz).startsWith(Scp5001Blocks.Door)
                            && !Plan.get(Wx, Y + 1, Wz).equals(Scp5001Blocks.Glass)) {
                        Walls++;
                        Side = Index;
                    }
                }
                if (Walls == 1) {
                    String Role = null;
                    if (Maintenance) {
                        Role = Roll < 220 ? "pipe_wall" : Roll < 290 ? "service_panel" : Roll < 310 ? "tool_rack" : null;
                    } else if (Room.Type.equals("gallery")) {
                        Role = Roll < 25 ? "wall_screen" : Roll < 45 ? "signage" : null;
                    } else {
                        Role = Roll < 35 ? "signage" : Roll < 60 ? "wall_screen" : Roll < 72 ? "drone_dock" : null;
                    }
                    if (Role != null && freeCell(X, Y, Z) && freeCell(X, Y + 1, Z) && freeCell(X, Y + 2, Z)) {
                        slot(Role, X, Y, Z, Out[Side], true, Ceiling, Room);
                        continue;
                    }
                    if (!Maintenance && Roll >= 990 && freeCell(X, Y, Z) && freeCell(X, Y + 1, Z)) {
                        Plan.set(X, Y, Z, Scp5001Blocks.Terminal + "[facing=" + Out[Side] + ",pool=" + (Room.Type.equals("gallery") ? 7 : 0) + "]");
                        use(X, Y, Z);
                        use(X, Y + 1, Z);
                        continue;
                    }
                }
                if (Walls == 0 && freeCell(X, Ceiling, Z) && !Plan.get(X, Ceiling + 1, Z).equals(Scp5001Blocks.CeilingLight)) {
                    String Role = null;
                    if (Maintenance) {
                        Role = Roll < 300 ? "pipe_ceiling" : Roll < 450 ? "cable_ceiling" : null;
                    } else if (Roll < 12) {
                        Role = "camera";
                    } else if (Roll < 40 && !Room.Type.equals("gallery")) {
                        Role = "cable_ceiling";
                    }
                    if (Role != null) {
                        Plan.Slots.add(new Scp5001Slot(Role, X, Ceiling, Z, Out[Math.floorMod(X + Z, 4)], false, Ceiling, Room.Id));
                        use(X, Ceiling, Z);
                    }
                }
                if (Maintenance && Walls == 1 && Roll >= 960 && freeCell(X, Y, Z) && freeCell(X, Y + 1, Z)) {
                    slot("eng_clutter", X, Y, Z, Out[Math.max(0, Side)], false, Ceiling, Room);
                }
            }
        }
    }

    static List<String> roles() {
        return List.of("lab_bench", "lab_wall_unit", "specimen_tank", "sample_rack", "analysis_machine", "scope_station", "fume_hood", "lab_cart", "lab_clutter", "lab_ceiling",
                "pipe_wall", "pipe_ceiling", "conduit_floor", "coolant_tank", "capacitor_bank", "transformer", "service_panel", "tool_rack", "eng_clutter", "cable_ceiling",
                "workstation", "wall_screen", "server_rack", "holo_table", "camera", "security_gate", "weapon_locker", "archive_cabinet", "ctrl_clutter", "signage",
                "bunk", "locker", "mess_table", "mess_seat", "galley_unit", "med_bed", "med_cabinet", "crate", "shelf", "hab_clutter", "drone_dock");
    }
}
