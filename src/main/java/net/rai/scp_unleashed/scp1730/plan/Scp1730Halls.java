package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class Scp1730Halls {
    public static final List<String> Kinds = List.of("flooded", "cell_row", "gallery", "bypass", "catwalk_drop");
    public static final Set<String> Keep = Set.of("loot", "breach", "laser", "crack", "symbol", "control", "card", "seal", "setpiece", "safe", "painter", "turret", "bioseal", "biocontrol");
    public static final int[][] Wanted = {{3}, {1, 1, 2, 3, 0}, {0, 2, 3, 4, 4}, {3}};
    public static final int[] Rate = {35, 100, 100, 50};
    public static final int DropDepth = 3;

    record Arm(int Cx, int Cz, int Ax, int Az, int Lx, int Lz, int Dir, int Sx, int Sz) {
        int x(int A, int Lat) {
            return Cx + Ax * A + Lx * Lat;
        }

        int z(int A, int Lat) {
            return Cz + Az * A + Lz * Lat;
        }
    }

    private Scp1730Halls() {
    }

    static String face(int Dir) {
        return Scp1730Layout.Facings[(Dir % 4 + 4) % 4];
    }

    static void dress(Scp1730Builder B, int Zone) {
        unseal(B, Zone);
        if (B.Fallback) {
            return;
        }
        Random Rng = B.Rng;
        Set<Long> Used = new HashSet<>();
        List<Arm> Arms = new ArrayList<>();
        for (int[] Spot : B.ArmSpots) {
            if (Spot[3] == Zone && Spot.length > 6) {
                int Dir = Spot[2];
                Arms.add(new Arm(Spot[0] + 7, Spot[1] + 7, Scp1730Layout.Dx[Dir], Scp1730Layout.Dz[Dir], Dir % 2 == 0 ? 1 : 0, Dir % 2 == 0 ? 0 : 1, Dir, Spot[4], Spot[5]));
            }
        }
        for (Scp1730Layout.Room Each : B.Layouts[Zone].Rooms) {
            if (!Each.Type.equals(Scp1730Sectors.Reservoir)) {
                continue;
            }
            for (Scp1730Layout.Door Door : Each.Doors) {
                int Nx = Door.Sx + Scp1730Layout.Dx[Door.Dir];
                int Nz = Door.Sz + Scp1730Layout.Dz[Door.Dir];
                for (Arm Option : Arms) {
                    if (Option.Sx() == Nx && Option.Sz() == Nz && Option.Dir() == (Door.Dir + 2) % 4 && !Used.contains(slot(Option)) && calm(B, Option, Zone) && flooded(B, Option, Zone)) {
                        Used.add(slot(Option));
                        break;
                    }
                }
            }
        }
        if (Rng.nextInt(100) >= Rate[Zone]) {
            return;
        }
        B.shuffle(Arms);
        List<Integer> Kinds = new ArrayList<>();
        for (int Each : Wanted[Zone]) {
            Kinds.add(Each);
        }
        for (int Kind : Kinds) {
            for (Arm Option : Arms) {
                if (Used.contains(slot(Option)) || !calm(B, Option, Zone)) {
                    continue;
                }
                boolean Done = switch (Kind) {
                    case 0 -> flooded(B, Option, Zone);
                    case 1 -> cellRow(B, Option, Zone);
                    case 2 -> gallery(B, Option, Zone);
                    case 3 -> bypass(B, Option, Zone);
                    default -> catwalkDrop(B, Option, Zone);
                };
                if (Done) {
                    Used.add(slot(Option));
                    break;
                }
            }
        }
    }

    static long slot(Arm Option) {
        return Option.Sx() * 64L + Option.Sz();
    }

    static void unseal(Scp1730Builder B, int Zone) {
        B.Fixtures.removeIf(Each -> Each.Type().equals("spawn") && Each.Zone() == Zone && sealedAt(B, Each.X(), Each.Y(), Each.Z()));
    }

    static boolean sealedAt(Scp1730Builder B, int X, int Y, int Z) {
        for (int[] Cell : B.SealedCells) {
            if (X >= Cell[0] && X <= Cell[2] && Z >= Cell[1] && Z <= Cell[3] && Math.abs(Y - Cell[4] - 1) <= 1) {
                return true;
            }
        }
        return false;
    }

    static boolean calm(Scp1730Builder B, Arm Option, int Zone) {
        for (Scp1730Fixture Each : B.Fixtures) {
            if (Each.Zone() != Zone || !Keep.contains(Each.Type())) {
                continue;
            }
            int A = (Each.X() - Option.Cx()) * Option.Ax() + (Each.Z() - Option.Cz()) * Option.Az();
            int Lat = (Each.X() - Option.Cx()) * Option.Lx() + (Each.Z() - Option.Cz()) * Option.Lz();
            if (A >= 1 && A <= 9 && Math.abs(Lat) <= 8) {
                return false;
            }
        }
        return true;
    }

    static void clearLane(Scp1730Builder B, Arm Option, int A, int Lat, int F) {
        for (int Level = 1; Level <= 3; Level++) {
            String Here = B.Grid.get(Option.x(A, Lat), F + Level, Option.z(A, Lat));
            if (Here != null && !(Level == 3 && Scp1730Builder.isLight(Here))) {
                B.Grid.set(Option.x(A, Lat), F + Level, Option.z(A, Lat), null);
            }
        }
    }

    static boolean clash(Scp1730Builder B, Arm Option, int A0, int A1, int Lat0, int Lat1, int Y0, int Y1) {
        int X0 = Math.min(Option.x(A0, Lat0), Option.x(A1, Lat1));
        int X1 = Math.max(Option.x(A0, Lat0), Option.x(A1, Lat1));
        int Z0 = Math.min(Option.z(A0, Lat0), Option.z(A1, Lat1));
        int Z1 = Math.max(Option.z(A0, Lat0), Option.z(A1, Lat1));
        for (Scp1730RoomInfo Room : B.RoomInfos) {
            boolean Across = Room.MaxX() + 2 >= X0 && Room.MinX() - 2 <= X1 && Room.MaxZ() + 2 >= Z0 && Room.MinZ() - 2 <= Z1;
            boolean Through = Room.Floor() - 2 <= Y1 && Room.Floor() + Room.Height() + 3 >= Y0;
            if (Across && Through) {
                return true;
            }
        }
        return false;
    }

    static boolean empty(Scp1730Builder B, Arm Option, int A0, int A1, int Lat0, int Lat1, int Y0, int Y1) {
        if (clash(B, Option, A0, A1, Lat0, Lat1, Y0, Y1)) {
            return false;
        }
        for (int A = A0; A <= A1; A++) {
            for (int Lat = Math.min(Lat0, Lat1); Lat <= Math.max(Lat0, Lat1); Lat++) {
                for (int Y = Y0; Y <= Y1; Y++) {
                    if (!B.Grid.empty(Option.x(A, Lat), Y, Option.z(A, Lat))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static boolean straight(Scp1730Builder B, Arm Option, int F, int A0, int A1) {
        for (int A = A0; A <= A1; A++) {
            for (int Lat = -1; Lat <= 1; Lat++) {
                String Floor = B.Grid.get(Option.x(A, Lat), F, Option.z(A, Lat));
                if (Floor == null || Scp1730Blocks.passable(Floor) || Scp1730Blocks.water(Floor)) {
                    return false;
                }
            }
            for (int Lat = -2; Lat <= 2; Lat += 4) {
                if (!Scp1730Builder.isWallLike(B.Grid.get(Option.x(A, Lat), F + 2, Option.z(A, Lat)))) {
                    return false;
                }
            }
        }
        return true;
    }

    static void mark(Scp1730Builder B, Arm Option, int Kind, int Zone) {
        int F = Scp1730Builder.Floors[Zone];
        B.fixture("hall", Option.x(5, 0), F + 1, Option.z(5, 0), face(Option.Dir()), Kind, Zone);
        B.HallVariants.add(new int[]{Option.x(5, 0), Option.z(5, 0), Kind, Zone});
    }

    static boolean flooded(Scp1730Builder B, Arm Option, int Zone) {
        int F = Scp1730Builder.Floors[Zone];
        if (!straight(B, Option, F, 2, 7) || clash(B, Option, 2, 7, -2, 2, F - 1, F + 5)) {
            return false;
        }
        Scp1730Blocks.Palette Look = B.corridorPalette(Zone);
        for (int A = 3; A <= 7; A++) {
            for (int Lat = -1; Lat <= 1; Lat++) {
                int X = Option.x(A, Lat);
                int Z = Option.z(A, Lat);
                clearLane(B, Option, A, Lat, F);
                B.Grid.set(X, F - 1, Z, Look.Floor);
                B.Grid.set(X, F, Z, Scp1730Blocks.Water);
            }
            for (int Lat = -2; Lat <= 2; Lat += 4) {
                int X = Option.x(A, Lat);
                int Z = Option.z(A, Lat);
                if (B.Grid.empty(X, F - 1, Z)) {
                    B.Grid.set(X, F - 1, Z, Look.Floor);
                }
            }
        }
        int Lat = B.Rng.nextBoolean() ? 1 : -1;
        int X = Option.x(5, Lat);
        int Z = Option.z(5, Lat);
        String Ceiling = B.Grid.get(X, F + 4, Z);
        if (Ceiling != null && Ceiling.equals(Look.Ceiling) && B.Grid.empty(X, F + 5, Z)) {
            B.Grid.set(X, F + 5, Z, Look.Ceiling);
            B.Grid.set(X, F + 4, Z, Scp1730Blocks.Water);
            for (int Y = F + 1; Y <= F + 3; Y++) {
                B.Grid.set(X, Y, Z, Scp1730Blocks.Falling);
            }
        }
        for (int A = 2; A <= 8; A += 6) {
            B.decal(Option.x(A, 0), F, Option.z(A, 0), "up", Scp1730Builder.Puddle, Zone);
        }
        mark(B, Option, 0, Zone);
        return true;
    }

    static void stripWall(Scp1730Builder B, Arm Option, int A, int Side, int F) {
        clearLane(B, Option, A, Side, F);
    }

    static boolean cellRow(Scp1730Builder B, Arm Option, int Zone) {
        int F = Scp1730Builder.Floors[Zone];
        if (!straight(B, Option, F, 3, 7)) {
            return false;
        }
        Scp1730Grid G = B.Grid;
        Scp1730Blocks.Palette Look = B.corridorPalette(Zone);
        Scp1730Blocks.Palette Cell = Scp1730Blocks.Containment;
        int Made = 0;
        for (int Side = -1; Side <= 1; Side += 2) {
            if (!empty(B, Option, 3, 7, Side * 3, Side * 6, F - 1, F + 5)) {
                continue;
            }
            for (int A = 3; A <= 7; A++) {
                for (int Lat = 3; Lat <= 6; Lat++) {
                    int X = Option.x(A, Lat * Side);
                    int Z = Option.z(A, Lat * Side);
                    boolean Edge = A == 3 || A == 7 || Lat == 6;
                    G.set(X, F, Z, Edge ? Look.Floor : Cell.Floor);
                    G.set(X, F + 4, Z, Look.Ceiling);
                    for (int Level = 1; Level <= 3; Level++) {
                        G.set(X, F + Level, Z, Edge ? Look.wall(Level, 3) : null);
                    }
                }
            }
            String Toward = Scp1730Builder.facingOf(Option.Lx() * Side, Option.Lz() * Side);
            String Back = Scp1730Builder.facingOf(-Option.Lx() * Side, -Option.Lz() * Side);
            String Bars = Option.Ax() != 0 ? "minecraft:iron_bars[east=true,west=true,north=false,south=false]" : "minecraft:iron_bars[north=true,south=true,east=false,west=false]";
            boolean Breached = B.Rng.nextBoolean();
            for (int A = 4; A <= 6; A++) {
                int X = Option.x(A, Side * 2);
                int Z = Option.z(A, Side * 2);
                stripWall(B, Option, A, Side, F);
                for (int Level = 1; Level <= 3; Level++) {
                    String State = Level == 3 ? Look.WallTop : Bars;
                    if (A == 5 && Level <= 2) {
                        State = Breached ? null : Bars;
                    }
                    G.set(X, F + Level, Z, State);
                }
                G.set(X, F, Z, Cell.Floor);
            }
            int Bx = Option.x(4, Side * 5);
            int Bz = Option.z(4, Side * 5);
            G.set(Bx, F + 1, Bz, Scp1730Blocks.Mod + "lcz_cell_bunk[facing=" + face(Option.Dir()) + ",pair=main]");
            G.set(Option.x(5, Side * 5), F + 1, Option.z(5, Side * 5), Scp1730Blocks.Mod + "lcz_cell_bunk[facing=" + face(Option.Dir()) + ",pair=extension]");
            if (Breached) {
                G.set(Option.x(6, Side * 4), F + 1, Option.z(6, Side * 4), Scp1730Blocks.facing(Scp1730Blocks.CellDoorWreck, face(Option.Dir())));
                B.decal(Option.x(5, Side * 4), F, Option.z(5, Side * 4), "up", Scp1730Builder.Blood, Zone);
            } else {
                G.set(Option.x(6, Side * 4), F + 1, Option.z(6, Side * 4), Scp1730Blocks.PaperStack + "[facing=" + Toward + ",variant=" + B.Rng.nextInt(6) + "]");
            }
            int Px = Option.x(3, Side);
            int Pz = Option.z(3, Side);
            if (G.empty(Px, F + 2, Pz) && Scp1730Builder.isWallLike(G.get(Option.x(3, Side * 2), F + 2, Option.z(3, Side * 2)))) {
                G.set(Px, F + 2, Pz, Scp1730Blocks.Mod + "containment_room_cell_plaque[facing=" + Back + ",number=" + (1 + B.Rng.nextInt(16)) + "]");
            }
            if (G.empty(Option.x(5, Side * 4), F + 3, Option.z(5, Side * 4))) {
                G.set(Option.x(5, Side * 4), F + 3, Option.z(5, Side * 4), Scp1730Blocks.facing(B.Rng.nextBoolean() ? Scp1730Blocks.DeadLight : Scp1730Blocks.EmergencyLight, "down"));
            }
            Made++;
        }
        if (Made == 0) {
            return false;
        }
        mark(B, Option, 1, Zone);
        return true;
    }

    static boolean gallery(Scp1730Builder B, Arm Option, int Zone) {
        int F = Scp1730Builder.Floors[Zone];
        if (!straight(B, Option, F, 3, 7)) {
            return false;
        }
        Scp1730Grid G = B.Grid;
        Scp1730Blocks.Palette Look = B.corridorPalette(Zone);
        Scp1730Blocks.Palette Pit = Scp1730Blocks.Containment;
        for (int Side = -1; Side <= 1; Side += 2) {
            if (!empty(B, Option, 3, 7, Side * 3, Side * 7, F - 2, F + 5)) {
                continue;
            }
            for (int A = 3; A <= 7; A++) {
                for (int Lat = 3; Lat <= 7; Lat++) {
                    int X = Option.x(A, Lat * Side);
                    int Z = Option.z(A, Lat * Side);
                    boolean Edge = A == 3 || A == 7 || Lat == 7;
                    G.set(X, F - 2, Z, Pit.Floor);
                    G.set(X, F + 4, Z, Pit.Ceiling);
                    for (int Level = -1; Level <= 3; Level++) {
                        G.set(X, F + Level, Z, Edge ? Pit.wall(Math.max(1, Level + 2), 5) : null);
                    }
                }
            }
            String Axis = Option.Ax() != 0 ? "z" : "x";
            for (int A = 4; A <= 6; A++) {
                int X = Option.x(A, Side * 2);
                int Z = Option.z(A, Side * 2);
                stripWall(B, Option, A, Side, F);
                G.set(X, F + 2, Z, Look.window(Axis));
                for (int Level = -1; Level <= 0; Level++) {
                    G.set(X, F + Level, Z, Pit.WallMiddle);
                }
            }
            String Back = Scp1730Builder.facingOf(-Option.Lx() * Side, -Option.Lz() * Side);
            G.set(Option.x(5, Side * 6), F - 1, Option.z(5, Side * 6), Scp1730Blocks.Mod + "containment_room_restraint_anchor[facing=" + Back + "]");
            G.set(Option.x(4, Side * 6), F - 1, Option.z(4, Side * 6), Scp1730Blocks.Mod + "containment_room_specimen_tank[facing=" + Back + ",part=bottom]");
            G.set(Option.x(4, Side * 6), F, Option.z(4, Side * 6), Scp1730Blocks.Mod + "containment_room_specimen_tank[facing=" + Back + ",part=top]");
            G.set(Option.x(6, Side * 4), F - 1, Option.z(6, Side * 4), Scp1730Blocks.facing(Scp1730Blocks.BodyBag, face(Option.Dir())));
            G.set(Option.x(5, Side * 5), F + 3, Option.z(5, Side * 5), Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, "down"));
            for (int Mark = 0; Mark < 4; Mark++) {
                B.decal(Option.x(4 + B.Rng.nextInt(3), Side * (3 + B.Rng.nextInt(4))), F - 2, Option.z(4 + B.Rng.nextInt(3), Side * (3 + B.Rng.nextInt(4))), "up",
                        B.pick(new int[]{Scp1730Builder.Blood, Scp1730Builder.Claws, Scp1730Builder.Scorch}), Zone);
            }
            int Sx = Option.x(3, Side);
            int Sz = Option.z(3, Side);
            if (G.empty(Sx, F + 3, Sz)) {
                G.set(Sx, F + 3, Sz, Scp1730Blocks.facing(Zone == 1 ? Scp1730Blocks.SignBiohazard : Scp1730Blocks.SignKeter, Back));
            }
            mark(B, Option, 2, Zone);
            return true;
        }
        return false;
    }

    static boolean bypass(Scp1730Builder B, Arm Option, int Zone) {
        int F = Scp1730Builder.Floors[Zone];
        if (!straight(B, Option, F, 3, 7)) {
            return false;
        }
        Scp1730Grid G = B.Grid;
        Scp1730Blocks.Palette Look = B.corridorPalette(Zone);
        for (int Side = -1; Side <= 1; Side += 2) {
            if (!empty(B, Option, 3, 7, Side * 3, Side * 4, F, F + 4) || !empty(B, Option, 8, 8, Side * 3, Side * 4, F, F + 4)) {
                continue;
            }
            for (int A = 2; A <= 8; A++) {
                for (int Lat = 3; Lat <= 4; Lat++) {
                    int X = Option.x(A, Lat * Side);
                    int Z = Option.z(A, Lat * Side);
                    boolean Tunnel = Lat == 3 && A >= 3 && A <= 7;
                    if (!Tunnel && !B.Grid.empty(X, F + 1, Z)) {
                        continue;
                    }
                    G.set(X, F, Z, Tunnel ? Scp1730Blocks.RubbleFloor : Look.Floor);
                    G.set(X, F + 3, Z, Look.Ceiling);
                    for (int Level = 1; Level <= 2; Level++) {
                        G.set(X, F + Level, Z, Tunnel ? null : Look.wall(Level, 2));
                    }
                    if (!Tunnel) {
                        G.set(X, F + 3, Z, Look.WallTop);
                    }
                }
            }
            for (int A : new int[]{3, 7}) {
                int X = Option.x(A, Side * 2);
                int Z = Option.z(A, Side * 2);
                stripWall(B, Option, A, Side, F);
                G.set(X, F + 1, Z, null);
                G.set(X, F + 2, Z, null);
                G.set(X, F, Z, Scp1730Blocks.RubbleFloor);
                B.decal(Option.x(A, Side * 4), F + 1, Option.z(A, Side * 4), Scp1730Builder.facingOf(-Option.Lx() * Side, -Option.Lz() * Side), Scp1730Builder.Cracks, Zone);
            }
            B.ceilingHole(Option.x(5, 0), F + 3, Option.z(5, 0));
            for (int Lat = -1; Lat <= 1; Lat++) {
                clearLane(B, Option, 5, Lat, F);
                int X = Option.x(5, Lat);
                int Z = Option.z(5, Lat);
                G.set(X, F + 1, Z, Scp1730Blocks.RubbleFloor);
                G.set(X, F + 2, Z, Lat == 0 ? Look.WallMiddle : Scp1730Blocks.RubbleFloor);
                if (Lat != 0) {
                    G.set(X, F + 3, Z, Scp1730Blocks.RubbleFloor);
                }
                for (int A : new int[]{4, 6}) {
                    clearLane(B, Option, A, Lat, F);
                    if (B.Rng.nextBoolean()) {
                        G.set(Option.x(A, Lat), F + 1, Option.z(A, Lat), Scp1730Blocks.facing(B.Rng.nextBoolean() ? Scp1730Blocks.Rubble : Scp1730Blocks.FallenPanel, face(Option.Dir())));
                    }
                }
            }
            int Hx = Option.x(4, 0);
            int Hz = Option.z(4, 0);
            if (G.empty(Hx, F + 3, Hz) && G.get(Hx, F + 4, Hz) != null) {
                G.set(Hx, F + 3, Hz, Scp1730Blocks.facing(Scp1730Blocks.DanglingPanel, "down"));
            }
            mark(B, Option, 3, Zone);
            return true;
        }
        return false;
    }

    static boolean catwalkDrop(Scp1730Builder B, Arm Option, int Zone) {
        int F = Scp1730Builder.Floors[Zone];
        if (!straight(B, Option, F, 2, 7) || clash(B, Option, 2, 7, -2, 2, F - DropDepth, F + 4) || !empty(B, Option, 2, 7, -2, 2, F - DropDepth, F - 1)) {
            return false;
        }
        Scp1730Grid G = B.Grid;
        Scp1730Blocks.Palette Look = B.corridorPalette(Zone);
        int Bottom = F - DropDepth;
        for (int A = 2; A <= 7; A++) {
            for (int Lat = -2; Lat <= 2; Lat++) {
                int X = Option.x(A, Lat);
                int Z = Option.z(A, Lat);
                boolean Pit = A >= 3 && A <= 6 && Math.abs(Lat) <= 1;
                if (!Pit) {
                    for (int Y = Bottom; Y < F; Y++) {
                        G.set(X, Y, Z, Look.WallMiddle);
                    }
                    continue;
                }
                clearLane(B, Option, A, Lat, F);
                G.set(X, Bottom, Z, Look.Floor);
                for (int Y = Bottom + 1; Y < F; Y++) {
                    G.set(X, Y, Z, null);
                }
                if (Lat == 0) {
                    G.set(X, F, Z, Scp1730Sectors.railing(Scp1730Blocks.RailingYellow, true, Scp1730Builder.facingOf(-Option.Lx(), -Option.Lz())));
                } else {
                    G.set(X, F, Z, null);
                }
            }
        }
        String Up = face(Option.Dir());
        G.set(Option.x(5, 1), Bottom + 1, Option.z(5, 1), Look.stairs(Up));
        G.set(Option.x(6, 1), Bottom + 1, Option.z(6, 1), Look.WallMiddle);
        G.set(Option.x(6, 1), Bottom + 2, Option.z(6, 1), Look.stairs(Up));
        G.set(Option.x(4, -1), Bottom + 1, Option.z(4, -1), Scp1730Blocks.facing(Scp1730Blocks.CableTangle, Up));
        G.set(Option.x(3, 1), Bottom + 1, Option.z(3, 1), Scp1730Blocks.facing(Scp1730Blocks.Rubble, Up));
        Scp1730Sectors.posts(G, Math.min(Option.x(2, -2), Option.x(7, 2)), Math.min(Option.z(2, -2), Option.z(7, 2)), Math.max(Option.x(2, -2), Option.x(7, 2)),
                Math.max(Option.z(2, -2), Option.z(7, 2)), F, F);
        B.Basins.add(new Scp1730Basin("drop", Math.min(Option.x(3, -1), Option.x(6, 1)), Math.min(Option.z(3, -1), Option.z(6, 1)), Math.max(Option.x(3, -1), Option.x(6, 1)),
                Math.max(Option.z(3, -1), Option.z(6, 1)), F - 1, Bottom, Zone));
        mark(B, Option, 4, Zone);
        return true;
    }
}
