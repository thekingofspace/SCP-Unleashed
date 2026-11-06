package net.rai.scp_unleashed.scp5001.world.plan;

import java.util.Random;

final class Scp5001Builder {
    public static final int RodLow = 12;
    public static final int RodHigh = 28;
    public static final int LaserY = 44;
    public static final int ChamberTop = 52;

    private final Scp5001Plan Plan;
    private final Scp5001Layout Layout;
    private final Random Random;

    Scp5001Builder(Scp5001Plan Plan, Scp5001Layout Layout) {
        this.Plan = Plan;
        this.Layout = Layout;
        this.Random = Plan.Random;
    }

    void build() {
        for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
            carve(Level);
        }
        for (Scp5001Room Core : Layout.Stairs) {
            stairs(Core, Layout.StairFrames.get(Core.Id));
        }
        halls();
        chamber();
        skin();
        doors();
        entry();
        anchors();
        connectRailings();
    }

    static String floorFor(Scp5001Room Room) {
        return switch (Room.Type) {
            case "lab", "field_lab", "medbay", "specimen_vault", "office", "conference", "records", "quarters", "lounge", "mess", "galley", "observation", "hallway" ->
                    Scp5001Blocks.Floor;
            case "substation", "coolant", "workshop", "generator", "engine", "maintenance" -> Scp5001Blocks.FloorGrate;
            default -> Scp5001Blocks.FloorHex;
        };
    }

    static String style(Scp5001Room Room) {
        return switch (Room.Type) {
            case "substation", "coolant", "workshop", "generator", "engine", "drone_bay", "storage" -> "eng";
            case "maintenance" -> "dark";
            case "server", "archive", "control", "emergency", "comms", "monitoring", "security", "checkpoint", "airlock", "armory", "stair" -> "tech";
            default -> "clean";
        };
    }

    int height(Scp5001Room Room) {
        return Room.corridor() ? Scp5001Plan.CorridorHeight : Scp5001Plan.RoomHeight;
    }

    private int along(int Dx, int Dz) {
        return Math.abs(Dx) >= Math.abs(Dz) ? Dz : Dx;
    }

    boolean lit(Scp5001Room Room, int X, int Z) {
        int Dx = X - Scp5001Plan.CentreX;
        int Dz = Z - Scp5001Plan.CentreZ;
        int Band = Scp5001Plan.band(X, Z);
        switch (Room.Type) {
            case "hallway" -> {
                return (Band == Scp5001Layout.HallwayNear || Band == Scp5001Layout.HallwayFar) && Math.floorMod(X + Z, 3) == 0;
            }
            case "maintenance" -> {
                return Band == 3 && Math.floorMod(X + Z, 9) == 0;
            }
            case "spoke" -> {
                int Across = Math.abs(Dx) <= 2 ? Dx : Dz;
                int Along = Math.abs(Dx) <= 2 ? Dz : Dx;
                return Math.abs(Across) == 1 && Math.floorMod(Along, 3) == 0;
            }
            case "gallery" -> {
                int Octagon = Scp5001Layout.oct(Dx, Dz);
                return (Octagon == 24 || Octagon == 25) && Math.floorMod(Dx + Dz, 3) == 0;
            }
            default -> {
                if (Room.Dark) {
                    return Math.floorMod(X, 6) == 3 && Math.floorMod(Z, 6) == 3;
                }
                return Math.floorMod(X, 3) == 1 && Math.floorMod(Z, 3) == 1;
            }
        }
    }

    private void carve(int Level) {
        int Floor = Scp5001Plan.floorY(Level);
        for (int X = 0; X < Scp5001Plan.Width; X++) {
            for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                Scp5001Room Room = Plan.room(Plan.region(Level, X, Z));
                if (Room == null || Room.Kind.equals(Scp5001Plan.KindStair)) {
                    continue;
                }
                int Top;
                if (Room.Kind.equals(Scp5001Plan.KindHall)) {
                    if (Level != Room.Level) {
                        continue;
                    }
                    Top = Scp5001Plan.floorY(Room.Level + Room.Levels - 1) + Scp5001Plan.RoomHeight;
                } else {
                    Top = Floor + height(Room);
                }
                Plan.set(X, Floor, Z, floorAt(Room, Level, X, Z));
                for (int Y = Floor + 1; Y <= Top; Y++) {
                    Plan.set(X, Y, Z, Scp5001Blocks.Air);
                }
                Plan.set(X, Top + 1, Z, lit(Room, X, Z) ? Scp5001Blocks.CeilingLight : Scp5001Blocks.Ceiling);
            }
        }
    }

    private String floorAt(Scp5001Room Room, int Level, int X, int Z) {
        int Dx = X - Scp5001Plan.CentreX;
        int Dz = Z - Scp5001Plan.CentreZ;
        int Band = Scp5001Plan.band(X, Z);
        switch (Room.Type) {
            case "hallway" -> {
                if (Band == Scp5001Layout.HallwayNear || Band == Scp5001Layout.HallwayFar) {
                    return Scp5001Blocks.FloorSeam + "[axis=" + tangent(Dx, Dz) + "]";
                }
                return Scp5001Blocks.FloorHex;
            }
            case "spoke" -> {
                boolean NorthSouth = Math.abs(Dx) <= 2 && Math.abs(Dz) > 2;
                int Across = NorthSouth ? Dx : Dz;
                if (Across == 0) {
                    return Scp5001Blocks.FloorSeam + "[axis=" + (NorthSouth ? "z" : "x") + "]";
                }
                return Scp5001Blocks.FloorHex;
            }
            case "maintenance" -> {
                return Scp5001Blocks.FloorGrate;
            }
            case "gallery" -> {
                return Scp5001Layout.oct(Dx, Dz) == 23 ? Scp5001Blocks.FloorSeam + "[axis=" + (Math.abs(Dx) >= Math.abs(Dz) ? "z" : "x") + "]" : Scp5001Blocks.FloorHex;
            }
            default -> {
            }
        }
        String Base = floorFor(Room);
        if (Base.equals(Scp5001Blocks.Floor) && edge(Room, Level, X, Z)) {
            return Scp5001Blocks.FloorHex;
        }
        return Base;
    }

    static String tangent(int Dx, int Dz) {
        double Gx = Math.abs(Dx / (Scp5001Hull.RadiusX * Scp5001Hull.RadiusX));
        double Gz = Math.abs(Dz / (Scp5001Hull.RadiusZ * Scp5001Hull.RadiusZ));
        return Gx >= Gz ? "z" : "x";
    }

    boolean edge(Scp5001Room Room, int Level, int X, int Z) {
        return Plan.region(Level, X + 1, Z) != Room.Id || Plan.region(Level, X - 1, Z) != Room.Id || Plan.region(Level, X, Z + 1) != Room.Id
                || Plan.region(Level, X, Z - 1) != Room.Id;
    }

    private String direction(int[] Frame, int Dv) {
        int Axis = Frame[0];
        int Sign = Frame[1] * Dv;
        if (Axis == 0) {
            return Sign > 0 ? "south" : "north";
        }
        return Sign > 0 ? "east" : "west";
    }

    private void stairs(Scp5001Room Core, int[] Frame) {
        int Bottom = Scp5001Plan.floorY(0);
        int Top = Scp5001Plan.floorY(Scp5001Plan.Levels - 1) + Scp5001Plan.RoomHeight;
        for (int U = 0; U < 7; U++) {
            for (int V = 0; V < 14; V++) {
                int[] At = Scp5001Layout.stairCell(Frame, U, V);
                Plan.set(At[0], Bottom, At[1], Scp5001Blocks.FloorHex);
                for (int Y = Bottom + 1; Y <= Top; Y++) {
                    Plan.set(At[0], Y, At[1], Scp5001Blocks.Air);
                }
                Plan.set(At[0], Top + 1, At[1], U % 3 == 0 && V % 4 == 2 ? Scp5001Blocks.CeilingLight : Scp5001Blocks.Ceiling);
            }
        }
        String Up = direction(Frame, 1);
        String Down = direction(Frame, -1);
        for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
            int Floor = Scp5001Plan.floorY(Level);
            for (int U = 0; U < 7; U++) {
                for (int V = 0; V <= 2; V++) {
                    int[] At = Scp5001Layout.stairCell(Frame, U, V);
                    Plan.set(At[0], Floor, At[1], V == 0 || U == 0 || U == 6 ? Scp5001Blocks.FloorHex : Scp5001Blocks.Floor);
                }
            }
            for (int U : new int[]{0, 6}) {
                int[] Edge = Scp5001Layout.stairCell(Frame, U, 2);
                Plan.set(Edge[0], Floor + 1, Edge[1], Scp5001Blocks.Railing);
            }
            if (Level == Scp5001Plan.Levels - 1) {
                continue;
            }
            for (int Step = 1; Step <= 8; Step++) {
                int V = 2 + Step;
                for (int U = 4; U <= 5; U++) {
                    int[] At = Scp5001Layout.stairCell(Frame, U, V);
                    for (int Y = Floor + 1; Y < Floor + Step; Y++) {
                        Plan.set(At[0], Y, At[1], Scp5001Blocks.Graphite);
                    }
                    Plan.set(At[0], Floor + Step, At[1], Scp5001Blocks.Stairs + "[facing=" + Up + ",half=bottom,shape=straight]");
                }
                int[] Stringer = Scp5001Layout.stairCell(Frame, 6, V);
                for (int Y = Floor + 1; Y <= Floor + Step; Y++) {
                    Plan.set(Stringer[0], Y, Stringer[1], Y == Floor + Step ? Scp5001Blocks.GraphiteSeam : Scp5001Blocks.Graphite);
                }
                Plan.set(Stringer[0], Floor + Step + 1, Stringer[1], Scp5001Blocks.Railing);
                int V2 = 11 - Step;
                for (int U = 1; U <= 2; U++) {
                    int[] At = Scp5001Layout.stairCell(Frame, U, V2);
                    Plan.set(At[0], Floor + 8 + Step, At[1], Scp5001Blocks.Stairs + "[facing=" + Down + ",half=bottom,shape=straight]");
                    if (Step < 8) {
                        Plan.set(At[0], Floor + 7 + Step, At[1], Scp5001Blocks.Slab + "[type=top,waterlogged=false]");
                    }
                }
                int[] Outer = Scp5001Layout.stairCell(Frame, 0, V2);
                Plan.set(Outer[0], Floor + 8 + Step, Outer[1], Scp5001Blocks.GraphiteSeam);
                Plan.set(Outer[0], Floor + 9 + Step, Outer[1], Scp5001Blocks.Railing);
            }
            for (int V = 3; V <= 10; V++) {
                int[] Divider = Scp5001Layout.stairCell(Frame, 3, V);
                for (int Y = Floor + 1; Y <= Floor + 16; Y++) {
                    Plan.set(Divider[0], Y, Divider[1], Math.floorMod(Y - Floor, 4) == 3 ? Scp5001Blocks.GraphiteSeam : Scp5001Blocks.Graphite);
                }
            }
            for (int V = 11; V <= 13; V++) {
                for (int U = 0; U <= 6; U++) {
                    int[] At = Scp5001Layout.stairCell(Frame, U, V);
                    for (int Y = Floor + 1; Y < Floor + 8; Y++) {
                        Plan.set(At[0], Y, At[1], Scp5001Blocks.Graphite);
                    }
                    Plan.set(At[0], Floor + 8, At[1], V == 13 || U == 0 || U == 6 ? Scp5001Blocks.FloorHex : Scp5001Blocks.Floor);
                }
            }
        }
    }

    int[][] depth(Scp5001Room Hall, int Level) {
        int[][] Depth = new int[Scp5001Plan.Width][Scp5001Plan.Depth];
        java.util.ArrayDeque<int[]> Queue = new java.util.ArrayDeque<>();
        for (int X = Hall.X0; X <= Hall.X1; X++) {
            for (int Z = Hall.Z0; Z <= Hall.Z1; Z++) {
                if (Plan.region(Level, X, Z) != Hall.Id) {
                    continue;
                }
                if (Plan.region(Level, X + 1, Z) != Hall.Id || Plan.region(Level, X - 1, Z) != Hall.Id || Plan.region(Level, X, Z + 1) != Hall.Id
                        || Plan.region(Level, X, Z - 1) != Hall.Id) {
                    Depth[X][Z] = 1;
                    Queue.add(new int[]{X, Z});
                }
            }
        }
        while (!Queue.isEmpty()) {
            int[] At = Queue.poll();
            for (int[] Step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int X = At[0] + Step[0];
                int Z = At[1] + Step[1];
                if (X < 0 || Z < 0 || X >= Scp5001Plan.Width || Z >= Scp5001Plan.Depth || Plan.region(Level, X, Z) != Hall.Id || Depth[X][Z] != 0) {
                    continue;
                }
                Depth[X][Z] = Depth[At[0]][At[1]] + 1;
                Queue.add(new int[]{X, Z});
            }
        }
        return Depth;
    }

    int[] deepest(Scp5001Room Hall, int Level) {
        int[][] Depth = depth(Hall, Level);
        int[] Best = {(Hall.X0 + Hall.X1) / 2, (Hall.Z0 + Hall.Z1) / 2};
        int Most = -1;
        double Cx = (Hall.X0 + Hall.X1) / 2.0;
        double Cz = (Hall.Z0 + Hall.Z1) / 2.0;
        for (int X = Hall.X0; X <= Hall.X1; X++) {
            for (int Z = Hall.Z0; Z <= Hall.Z1; Z++) {
                int Score = Depth[X][Z] * 100 - (int) Math.round(Math.abs(X - Cx) + Math.abs(Z - Cz));
                if (Depth[X][Z] > 0 && Score > Most) {
                    Most = Score;
                    Best = new int[]{X, Z};
                }
            }
        }
        return Best;
    }

    private void halls() {
        for (Scp5001Room Hall : Plan.Rooms) {
            if (!Hall.Kind.equals(Scp5001Plan.KindHall)) {
                continue;
            }
            int Upper = Scp5001Plan.floorY(Hall.Level + 1);
            int[][] Depth = depth(Hall, Hall.Level + 1);
            for (int X = Hall.X0; X <= Hall.X1; X++) {
                for (int Z = Hall.Z0; Z <= Hall.Z1; Z++) {
                    int Edge = Depth[X][Z];
                    if (Edge == 0) {
                        continue;
                    }
                    if (Edge <= 2) {
                        Plan.set(X, Upper, Z, Scp5001Blocks.FloorGrate);
                        Plan.set(X, Upper - 1, Z, Edge == 2 ? Scp5001Blocks.GraphiteSeam : Scp5001Blocks.Graphite);
                    } else if (Edge == 3) {
                        Plan.set(X, Upper + 1, Z, Scp5001Blocks.Railing);
                    }
                }
            }
            int[] Centre = deepest(Hall, Hall.Level);
            Hall.Inward = Centre[0] * 1000 + Centre[1];
            int Cx = Centre[0];
            int Cz = Centre[1];
            int Floor = Scp5001Plan.floorY(Hall.Level);
            for (int Dx = -4; Dx <= 4; Dx++) {
                for (int Dz = -4; Dz <= 4; Dz++) {
                    if (Plan.region(Hall.Level, Cx + Dx, Cz + Dz) != Hall.Id) {
                        continue;
                    }
                    int Ring = Math.max(Math.abs(Dx), Math.abs(Dz));
                    if (Ring <= 2) {
                        Plan.set(Cx + Dx, Floor, Cz + Dz, Scp5001Blocks.FloorHex);
                    } else if (Ring == 3) {
                        Plan.set(Cx + Dx, Floor, Cz + Dz, Math.abs(Dx) == Math.abs(Dz) ? Scp5001Blocks.FloorGlyph
                                : Scp5001Blocks.FloorSeam + "[axis=" + (Math.abs(Dx) == 3 ? "z" : "x") + "]");
                    }
                }
            }
            if (Hall.Type.equals("engine")) {
                for (int Dx = -5; Dx <= 5; Dx++) {
                    for (int Dz = -5; Dz <= 5; Dz++) {
                        if (Math.max(Math.abs(Dx), Math.abs(Dz)) == 5 && Math.abs(Dx) > 1 && Math.abs(Dz) > 1 && Plan.region(Hall.Level, Cx + Dx, Cz + Dz) == Hall.Id) {
                            Plan.set(Cx + Dx, Floor + 1, Cz + Dz, Scp5001Blocks.Railing);
                        }
                    }
                }
                for (int Dx = -1; Dx <= 1; Dx++) {
                    for (int Dz = -1; Dz <= 1; Dz++) {
                        Plan.set(Cx + Dx, Floor, Cz + Dz, Dx == 0 && Dz == 0 ? Scp5001Blocks.FloorGlyph : Scp5001Blocks.FloorGrate);
                    }
                }
            }
        }
    }

    private void chamber() {
        int Cx = Scp5001Plan.CentreX;
        int Cz = Scp5001Plan.CentreZ;
        int Bottom = Scp5001Plan.floorY(0);
        int Roof = ChamberTop + 1;
        for (int Dx = -Scp5001Layout.ChamberWall; Dx <= Scp5001Layout.ChamberWall; Dx++) {
            for (int Dz = -Scp5001Layout.ChamberWall; Dz <= Scp5001Layout.ChamberWall; Dz++) {
                int Octagon = Scp5001Layout.oct(Dx, Dz);
                int X = Cx + Dx;
                int Z = Cz + Dz;
                if (Octagon <= Scp5001Layout.ChamberRadius) {
                    Plan.set(X, Bottom, Z, chamberFloor(Dx, Dz, Octagon));
                    for (int Y = Bottom + 1; Y <= ChamberTop; Y++) {
                        Plan.set(X, Y, Z, Scp5001Blocks.Air);
                    }
                    boolean Light = Octagon == 12 && Math.floorMod(Dx + Dz, 3) == 0 || Octagon == 6 && Math.floorMod(Dx - Dz, 2) == 0;
                    Plan.set(X, Roof, Z, Light ? Scp5001Blocks.CeilingLight : Scp5001Blocks.Ceiling);
                } else if (Octagon <= Scp5001Layout.ChamberWall) {
                    for (int Y = Bottom; Y <= Roof; Y++) {
                        Plan.set(X, Y, Z, Scp5001Blocks.ChamberWall);
                    }
                    for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
                        int Floor = Scp5001Plan.floorY(Level);
                        boolean Control = Level == 2 && Dx < 0 && Math.abs(Dz) <= 10;
                        int TopRow = Control ? 5 : 4;
                        for (int Row = 2; Row <= TopRow; Row++) {
                            Plan.set(X, Floor + Row, Z, Scp5001Blocks.Glass);
                        }
                    }
                    boolean Cardinal = Math.abs(Dx) <= 1 || Math.abs(Dz) <= 1;
                    if (Cardinal) {
                        Plan.set(X, Bottom, Z, Scp5001Blocks.FloorHex);
                        for (int Y = Bottom + 1; Y <= Bottom + 4; Y++) {
                            Plan.set(X, Y, Z, Scp5001Blocks.Air);
                        }
                        Plan.set(X, Bottom + 5, Z, Scp5001Blocks.GraphiteSeam);
                    }
                }
            }
        }
    }

    private String chamberFloor(int Dx, int Dz, int Octagon) {
        if (Octagon <= 2) {
            return Scp5001Blocks.FloorHex;
        }
        if (Octagon == 7 || Octagon == 15 && Math.floorMod(Dx * 3 + Dz, 4) == 0) {
            return Scp5001Blocks.FloorGlyph;
        }
        if ((Dx == 0 || Dz == 0) && Octagon >= 9) {
            return Scp5001Blocks.FloorSeam + "[axis=" + (Dx == 0 ? "z" : "x") + "]";
        }
        if ((Octagon == 11 || Octagon == 18) && Math.abs(Dx) != Math.abs(Dz)) {
            return Scp5001Blocks.FloorSeam + "[axis=" + (Math.abs(Dx) > Math.abs(Dz) ? "z" : "x") + "]";
        }
        return Scp5001Blocks.ChamberFloor;
    }

    private String wall(String Style, int Row, boolean Rib, int Tall) {
        if (Row > Tall) {
            return Scp5001Blocks.Graphite;
        }
        if (Tall > 7 && Row > 6) {
            if (Row == Tall) {
                return Scp5001Blocks.Graphite;
            }
            if (Rib) {
                return Scp5001Blocks.Rib;
            }
            return Math.floorMod(Row, 6) == 5 ? Scp5001Blocks.PanelSeam : Style.equals("clean") ? Scp5001Blocks.Panel : Scp5001Blocks.GraphitePanel;
        }
        if (Rib) {
            return Row == Tall ? Scp5001Blocks.GraphiteSeam : Scp5001Blocks.Rib;
        }
        int Top = Math.min(Tall, 6);
        if (Row == Top) {
            return Scp5001Blocks.Graphite;
        }
        if (Row == Top - 1) {
            return switch (Style) {
                case "clean" -> Scp5001Blocks.PanelSeam;
                case "dark" -> Scp5001Blocks.Graphite;
                default -> Scp5001Blocks.GraphiteSeam;
            };
        }
        if (Row == 1) {
            return switch (Style) {
                case "eng" -> Scp5001Blocks.TrimAmber;
                case "dark" -> Scp5001Blocks.Graphite;
                default -> Scp5001Blocks.Trim;
            };
        }
        return switch (Style) {
            case "clean" -> Scp5001Blocks.Panel;
            case "eng" -> Row == 2 ? Scp5001Blocks.GraphitePanel : Scp5001Blocks.Panel;
            default -> Scp5001Blocks.GraphitePanel;
        };
    }

    private static final int[][] Neighbours = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final String[] Facings = {"north", "east", "south", "west"};

    private void skin() {
        for (int Level = 0; Level < Scp5001Plan.Levels; Level++) {
            int Floor = Scp5001Plan.floorY(Level);
            for (int X = 0; X < Scp5001Plan.Width; X++) {
                for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                    Scp5001Room Room = Plan.room(Plan.region(Level, X, Z));
                    if (Room == null) {
                        continue;
                    }
                    boolean Stair = Room.Kind.equals(Scp5001Plan.KindStair);
                    boolean Hall = Room.Kind.equals(Scp5001Plan.KindHall);
                    if ((Stair || Hall) && Level != Room.Level) {
                        continue;
                    }
                    int Tall = Stair ? Scp5001Plan.floorY(Scp5001Plan.Levels - 1) + Scp5001Plan.RoomHeight - Floor
                            : Hall ? Scp5001Plan.floorY(Room.Level + 1) + Scp5001Plan.RoomHeight - Floor : height(Room);
                    String Style = Stair ? "tech" : style(Room);
                    for (int Side = 0; Side < 4; Side++) {
                        int Wx = X + Neighbours[Side][0];
                        int Wz = Z + Neighbours[Side][1];
                        if (!Scp5001Plan.inside(Wx, Floor, Wz) || Plan.region(Level, Wx, Wz) != 0) {
                            continue;
                        }
                        boolean AlongX = Neighbours[Side][0] == 0;
                        int Run = AlongX ? Wx : Wz;
                        boolean Rib = Math.floorMod(Run, Style.equals("dark") ? 8 : 4) == 0;
                        String Out = Facings[(Side + 2) % 4];
                        for (int Row = 1; Row <= Tall + 1; Row++) {
                            int Y = Floor + Row;
                            if (!Plan.get(Wx, Y, Wz).equals(Scp5001Blocks.Graphite)) {
                                continue;
                            }
                            String State = wall(Style, Row, Rib, Tall);
                            int Cornice = Math.min(Tall, 6) - 1;
                            if (!Rib && Row == 3 && Row < Cornice && Style.equals("clean") && accent(Wx, Wz, 11)) {
                                State = Scp5001Blocks.PanelGlyph + "[glyph=" + Math.floorMod(Wx * 7 + Wz * 3, 3) + "]";
                            } else if (!Rib && Row == 2 && accent(Wx + 3, Wz + 7, Style.equals("clean") ? 23 : 9)) {
                                State = Scp5001Blocks.Hatch + "[facing=" + Out + "]";
                            } else if (!Rib && Row == Cornice - 1 && Row > 2 && accent(Wx + 5, Wz + 3, 13) && !Style.equals("clean")) {
                                State = Scp5001Blocks.Vent + "[facing=" + Out + "]";
                            }
                            Plan.set(Wx, Y, Wz, State);
                        }
                    }
                }
            }
        }
    }

    private boolean accent(int X, int Z, int Every) {
        long Mixed = (X * 73856093L) ^ (Z * 19349663L) ^ Plan.seed();
        return Math.floorMod(Mixed, Every) == 0;
    }

    private void doors() {
        for (Scp5001Room Room : Plan.Rooms) {
            for (Scp5001Door Door : Room.Doors) {
                placeDoor(Door, Room);
            }
        }
    }

    static String doorPart(Scp5001Door Door, int Column, int Row) {
        return Scp5001Blocks.Door + "[column=" + Column + ",facing=" + (Door.AlongX() ? "north" : "east") + ",open=" + Door.Open() + ",row=" + Row + "]";
    }

    private void placeDoor(Scp5001Door Door, Scp5001Room Room) {
        int Ax = Door.AlongX() ? 1 : 0;
        int Az = Door.AlongX() ? 0 : 1;
        for (int Column = -1; Column <= 2; Column++) {
            int X = Door.X() + Ax * Column;
            int Z = Door.Z() + Az * Column;
            boolean Jamb = Column < 0 || Column > 1;
            for (int Row = -1; Row <= 3; Row++) {
                int Y = Door.Y() + Row;
                if (!Jamb && Row >= 0 && Row <= 2) {
                    Plan.set(X, Y, Z, doorPart(Door, Column, Row));
                } else if (Row == -1) {
                    Plan.set(X, Y, Z, Scp5001Blocks.FloorHex);
                } else if (Plan.get(X, Y, Z).startsWith(Scp5001Blocks.Door)) {
                    continue;
                } else if (Jamb || !Plan.air(X, Y, Z)) {
                    Plan.set(X, Y, Z, Row == 3 ? Scp5001Blocks.GraphiteSeam : Scp5001Blocks.Rib);
                }
            }
            if (Jamb) {
                pilaster(X, Z, Door.X() + Ax * (Column < 0 ? 0 : 1), Door.Z() + Az * (Column < 0 ? 0 : 1), Door.Y());
            }
        }
        int Gx = Door.X() + Ax * 3;
        int Gz = Door.Z() + Az * 3;
        String Beside = Plan.get(Gx, Door.Y() + 1, Gz);
        if (Beside.equals(Scp5001Blocks.Panel) || Beside.equals(Scp5001Blocks.GraphitePanel)) {
            Plan.set(Gx, Door.Y() + 1, Gz, Scp5001Blocks.PanelGlyph + "[glyph=" + Math.floorMod(Gx + Gz, 3) + "]");
        }
    }

    private void pilaster(int X, int Z, int BesideX, int BesideZ, int Base) {
        for (int Row = 4; Row < Scp5001Plan.RoomHeight; Row++) {
            int Y = Base + Row;
            if (!Plan.air(X, Y, Z) || Plan.air(BesideX, Y, BesideZ)) {
                return;
            }
            Plan.set(X, Y, Z, plain(Plan.get(BesideX, Y, BesideZ)));
        }
    }

    static String plain(String Wall) {
        String Id = Scp5001Blocks.idOf(Wall);
        if (Id.equals(Scp5001Blocks.Hatch) || Id.equals(Scp5001Blocks.Vent)) {
            return Scp5001Blocks.GraphitePanel;
        }
        if (Id.equals(Scp5001Blocks.PanelGlyph)) {
            return Scp5001Blocks.Panel;
        }
        if (!Scp5001Blocks.solid(Wall) || Id.equals(Scp5001Blocks.Glass) || Id.startsWith(Scp5001Blocks.Mod + "scp5001_shell")) {
            return Scp5001Blocks.Graphite;
        }
        return Wall;
    }

    private void entry() {
        int Y0 = Scp5001Plan.entryY();
        int X0 = Scp5001Plan.entryX();
        for (int Column = 0; Column < Scp5001Plan.EntryWidth; Column++) {
            for (int Row = 0; Row < Scp5001Plan.EntryHeight; Row++) {
                Plan.set(X0 + Column, Y0 + Row, 0, Scp5001Blocks.EntryDoor + "[column=" + Column + ",facing=north,open=false,row=" + Row + "]");
                Plan.set(X0 + Column, Y0 + Row, 1, Scp5001Blocks.Air);
            }
        }
        int Floor = Scp5001Plan.floorY(Scp5001Plan.EntryLevel);
        Scp5001Room Airlock = Layout.Airlock;
        for (int X = Airlock.X0; X <= Airlock.X1; X++) {
            for (int Z = Airlock.Z0; Z <= Airlock.Z1; Z++) {
                if (Plan.region(Airlock.Level, X, Z) != Airlock.Id) {
                    continue;
                }
                boolean Edge = X == Airlock.X0 || X == Airlock.X1;
                Plan.set(X, Floor, Z, Edge ? Scp5001Blocks.FloorGrate : Math.floorMod(Z, 3) == 0 ? Scp5001Blocks.FloorSeam + "[axis=x]" : Scp5001Blocks.FloorHex);
                Plan.set(X, Floor + Scp5001Plan.RoomHeight + 1, Z, Math.floorMod(Z, 2) == 0 && !Edge ? Scp5001Blocks.CeilingLight : Scp5001Blocks.Ceiling);
            }
        }
    }

    private void anchor(String Type, int X, int Y, int Z, String Facing) {
        Plan.Anchors.add(new Scp5001Anchor(Type, X, Y, Z, Facing));
        Plan.set(X, Y, Z, Type + "[facing=" + Facing + "]");
    }

    private void anchors() {
        int Cx = Scp5001Plan.CentreX;
        int Cz = Scp5001Plan.CentreZ;
        int Bottom = Scp5001Plan.floorY(0);
        anchor(Scp5001Blocks.ChamberCore, Cx, Bottom + 1, Cz, "north");
        anchor(Scp5001Blocks.SkyEmitter, Cx, ChamberTop + 1, Cz, "up");
        int Inner = Scp5001Layout.ChamberRadius + 1;
        anchor(Scp5001Blocks.RodEmitter, Cx, RodLow, Cz - Inner, "south");
        anchor(Scp5001Blocks.RodEmitter, Cx + Inner, RodLow, Cz, "west");
        anchor(Scp5001Blocks.RodEmitter, Cx, RodLow, Cz + Inner, "north");
        anchor(Scp5001Blocks.RodEmitter, Cx - Inner, RodLow, Cz, "east");
        anchor(Scp5001Blocks.RodEmitter, Cx + 15, RodHigh, Cz - 14, "west");
        anchor(Scp5001Blocks.RodEmitter, Cx + 14, RodHigh, Cz + 15, "north");
        anchor(Scp5001Blocks.RodEmitter, Cx - 15, RodHigh, Cz + 14, "east");
        anchor(Scp5001Blocks.RodEmitter, Cx - 14, RodHigh, Cz - 15, "south");
        for (int Index = 0; Index < 6; Index++) {
            double Angle = Math.toRadians(30 + Index * 60);
            double Sx = Math.sin(Angle);
            double Sz = -Math.cos(Angle);
            for (double Reach = 17.0; Reach <= 24.0; Reach += 0.25) {
                int Dx = (int) Math.round(Sx * Reach);
                int Dz = (int) Math.round(Sz * Reach);
                if (Scp5001Layout.oct(Dx, Dz) != Inner) {
                    continue;
                }
                String Facing = Math.abs(Dx) >= Math.abs(Dz) ? (Dx > 0 ? "west" : "east") : (Dz > 0 ? "north" : "south");
                int Ix = Dx + (Facing.equals("west") ? -1 : Facing.equals("east") ? 1 : 0);
                int Iz = Dz + (Facing.equals("north") ? -1 : Facing.equals("south") ? 1 : 0);
                if (Scp5001Layout.oct(Ix, Iz) <= Scp5001Layout.ChamberRadius) {
                    anchor(Scp5001Blocks.LaserEmitter, Cx + Dx, LaserY, Cz + Dz, Facing);
                    break;
                }
            }
        }
        int Control = Scp5001Plan.floorY(2) + 1;
        anchor(Scp5001Blocks.ConsoleMain, Cx - 25, Control, Cz, "west");
        emergency(Layout.Emergency, Control);
        for (Scp5001Room Hall : Layout.Generators) {
            hallAnchors(Hall, Scp5001Blocks.Generator);
        }
        int Engine = Layout.Engine.Inward;
        anchor(Scp5001Blocks.Engine, Engine / 1000, Scp5001Plan.floorY(Layout.Engine.Level) + 1, Engine % 1000, "north");
    }

    private static final int[][] Sides = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final String[] Names = {"north", "east", "south", "west"};

    private boolean nearDoor(Scp5001Room Room, int X, int Z, int Range) {
        for (Scp5001Room Any : Plan.Rooms) {
            for (Scp5001Door Door : Any.Doors) {
                if (Door.Y() == Scp5001Plan.floorY(Room.Level) + 1 && Math.abs(Door.X() - X) <= Range && Math.abs(Door.Z() - Z) <= Range) {
                    return true;
                }
            }
        }
        return false;
    }

    private void emergency(Scp5001Room Room, int Y) {
        int[] Intake = null;
        int IntakeSide = 0;
        for (int X = Room.X0; X <= Room.X1 && Intake == null; X++) {
            for (int Z = Room.Z0; Z <= Room.Z1 && Intake == null; Z++) {
                if (Plan.region(Room.Level, X, Z) != Room.Id) {
                    continue;
                }
                for (int Side = 0; Side < 4; Side++) {
                    int Wx = X - Sides[Side][0];
                    int Wz = Z - Sides[Side][1];
                    if (Plan.region(Room.Level, Wx, Wz) != 0) {
                        continue;
                    }
                    boolean Clear = true;
                    for (int Step = 1; Step <= 3 && Clear; Step++) {
                        for (int Across = -1; Across <= 1 && Clear; Across++) {
                            int Ax = X + Sides[Side][0] * Step + Sides[(Side + 1) % 4][0] * Across;
                            int Az = Z + Sides[Side][1] * Step + Sides[(Side + 1) % 4][1] * Across;
                            Clear = Plan.region(Room.Level, Ax, Az) == Room.Id && !nearDoor(Room, Ax, Az, 2);
                        }
                    }
                    if (Clear && !nearDoor(Room, X, Z, 3)) {
                        Intake = new int[]{X, Z};
                        IntakeSide = Side;
                        break;
                    }
                }
            }
        }
        if (Intake == null) {
            return;
        }
        anchor(Scp5001Blocks.EmergencyIntake, Intake[0], Y, Intake[1], Names[IntakeSide]);
        for (int X = Room.X1; X >= Room.X0; X--) {
            for (int Z = Room.Z1; Z >= Room.Z0; Z--) {
                if (Plan.region(Room.Level, X, Z) != Room.Id || Math.abs(X - Intake[0]) + Math.abs(Z - Intake[1]) < 6 || nearDoor(Room, X, Z, 3)) {
                    continue;
                }
                for (int Side = 0; Side < 4; Side++) {
                    if (Plan.region(Room.Level, X - Sides[Side][0], Z - Sides[Side][1]) == 0
                            && Plan.region(Room.Level, X + Sides[Side][0], Z + Sides[Side][1]) == Room.Id) {
                        anchor(Scp5001Blocks.EmergencyConsole, X, Y, Z, Names[Side]);
                        return;
                    }
                }
            }
        }
    }

    private void hallAnchors(Scp5001Room Hall, String Type) {
        int Cx = Hall.Inward / 1000;
        int Cz = Hall.Inward % 1000;
        int Y = Scp5001Plan.floorY(Hall.Level) + 1;
        anchor(Type, Cx, Y, Cz, "north");
        int Dx = 0;
        int Dz = 1;
        for (Scp5001Door Door : Hall.Doors) {
            if (Door.Y() == Y) {
                int Mx = Door.X() - Cx;
                int Mz = Door.Z() - Cz;
                if (Math.abs(Mx) >= Math.abs(Mz)) {
                    Dx = Integer.signum(Mx);
                    Dz = 0;
                } else {
                    Dx = 0;
                    Dz = Integer.signum(Mz);
                }
                break;
            }
        }
        String Facing = Dx > 0 ? "east" : Dx < 0 ? "west" : Dz > 0 ? "south" : "north";
        int[][] Tries = {{5, 2}, {5, -2}, {4, 2}, {4, -2}, {5, 0}, {4, 0}, {3, 3}, {3, -3}};
        for (int[] Try : Tries) {
            int X = Cx + Dx * Try[0] + Dz * Try[1];
            int Z = Cz + Dz * Try[0] + Dx * Try[1];
            if (Plan.region(Hall.Level, X, Z) == Hall.Id && Plan.region(Hall.Level, X + Dx, Z + Dz) == Hall.Id) {
                anchor(Scp5001Blocks.GeneratorConsole, X, Y, Z, Facing);
                return;
            }
        }
        anchor(Scp5001Blocks.GeneratorConsole, Cx + Dz * 3, Y, Cz + Dx * 3, Facing);
    }

    private void connectRailings() {
        for (int Y = 0; Y < Scp5001Plan.Height; Y++) {
            for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                for (int X = 0; X < Scp5001Plan.Width; X++) {
                    if (!Plan.get(X, Y, Z).equals(Scp5001Blocks.Railing)) {
                        continue;
                    }
                    StringBuilder State = new StringBuilder(Scp5001Blocks.Railing).append('[');
                    String[] Names = {"east", "north", "south", "west"};
                    int[][] Steps = {{1, 0}, {0, -1}, {0, 1}, {-1, 0}};
                    for (int Index = 0; Index < 4; Index++) {
                        String Next = Plan.get(X + Steps[Index][0], Y, Z + Steps[Index][1]);
                        boolean Link = Next.startsWith(Scp5001Blocks.Railing);
                        State.append(Names[Index]).append('=').append(Link).append(Index < 3 ? "," : "");
                    }
                    State.append(",waterlogged=false]");
                    Plan.set(X, Y, Z, State.toString());
                }
            }
        }
    }
}
