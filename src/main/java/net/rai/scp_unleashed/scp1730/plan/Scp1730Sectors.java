package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Scp1730Sectors {
    public static final String BioWing = "bio_wing";
    public static final String Reservoir = "reservoir";
    public static final int WingHeight = 5;
    public static final int ReservoirHeight = 7;
    public static final int CellDepth = 6;
    public static final int CellWidth = 6;
    public static final int AirlockDepth = 7;
    public static final int AirlockHalf = 3;
    public static final int BoothHalf = 3;
    public static final int BasinDepth = 6;
    public static final int Walkway = 3;
    public static final int IslandHalf = 3;
    public static final int LadderEvery = 5;
    public static final int Waterfalls = 3;
    public static final int OuterReader = 0;
    public static final int InnerReader = 1;
    public static final int ClearanceCard = 1;
    public static final int[] WingSetPieces = {3, 4};
    public static final String Clearance = Scp1730Blocks.Mod + "scp1730_clearance";
    public static final List<List<String>> ClearanceRooms = List.of(List.of(), List.of("medical", "research", "small_office", "storage", "storage_bay", "decon", "dorms", "breaker", "cell_block"),
            List.of("armoury", "storage", "server", "cooling", "pump_room", "incinerator", "generator", "collapsed", "catwalk_room", "heavy_cell"), List.of("collapsed", "holding_cell"));

    record Frame(int Cx, int Cz, int Ix, int Iz, int Lx, int Lz) {
        int x(int U, int V) {
            return Cx + Lx * U + Ix * V;
        }

        int z(int U, int V) {
            return Cz + Lz * U + Iz * V;
        }
    }

    private Scp1730Sectors() {
    }

    static String face(int Dir) {
        return Scp1730Layout.Facings[(Dir % 4 + 4) % 4];
    }

    static String mod(String Name) {
        return Scp1730Blocks.Mod + Name;
    }

    public static boolean sector(String Type) {
        return BioWing.equals(Type) || Reservoir.equals(Type);
    }

    static boolean furnish(Scp1730Builder B, Scp1730Rooms.Box R) {
        switch (R.Room.Type) {
            case BioWing -> wing(B, R);
            case Reservoir -> reservoir(B, R);
            default -> {
                return false;
            }
        }
        return true;
    }

    static Frame frame(Scp1730Rooms.Box R, Scp1730Layout.Door Main) {
        int Into = (Main.Dir + 2) % 4;
        return new Frame(R.Room.doorX(Main), R.Room.doorZ(Main), Scp1730Layout.Dx[Into], Scp1730Layout.Dz[Into], Main.Dir % 2 == 0 ? 1 : 0, Main.Dir % 2 == 0 ? 0 : 1);
    }

    static boolean inside(Scp1730Rooms.Box R, int X, int Z) {
        return X >= R.X0 && X <= R.X1 && Z >= R.Z0 && Z <= R.Z1;
    }

    static void column(Scp1730Builder B, Scp1730Rooms.Box R, int X, int Z, int From, String Open) {
        for (int Level = From; Level <= R.H; Level++) {
            B.Grid.set(X, R.F + Level, Z, Open != null && Level <= 3 ? (Open.isEmpty() ? null : Open) : R.Look.wall(Level, R.H));
        }
    }

    static void wing(Scp1730Builder B, Scp1730Rooms.Box R) {
        Scp1730Grid G = B.Grid;
        Random Rng = B.Rng;
        Scp1730Layout.Door Main = R.door();
        Frame D = frame(R, Main);
        int Into = (Main.Dir + 2) % 4;
        String Out = face(Main.Dir);
        String In = face(Into);
        String Plus = Scp1730Builder.facingOf(D.Lx(), D.Lz());
        String Minus = Scp1730Builder.facingOf(-D.Lx(), -D.Lz());
        wingDoor(B, R, D, Main);
        int Wide = R.X1 - R.X0 + 1;
        int Deep = R.Z1 - R.Z0 + 1;
        boolean[][] Used = new boolean[Wide][Deep];
        for (int U = -AirlockHalf - 1; U <= AirlockHalf + 1; U++) {
            for (int V = 1; V <= AirlockDepth + 1; V++) {
                int X = D.x(U, V);
                int Z = D.z(U, V);
                if (inside(R, X, Z)) {
                    Used[X - R.X0][Z - R.Z0] = true;
                }
            }
        }
        for (int V = 1; V <= AirlockDepth; V++) {
            for (int U = -AirlockHalf; U <= AirlockHalf; U++) {
                int X = D.x(U, V);
                int Z = D.z(U, V);
                boolean Side = Math.abs(U) == AirlockHalf;
                boolean Inner = V == AirlockDepth;
                if (Side || Inner && Math.abs(U) >= 2) {
                    column(B, R, X, Z, 1, null);
                } else if (Inner) {
                    column(B, R, X, Z, 1, "");
                    G.set(X, R.F, Z, R.Look.marked(In));
                    B.keepClear(X, R.F + 1, Z);
                    B.keepClear(X, R.F + 2, Z);
                } else {
                    G.set(X, R.F, Z, V == 1 ? R.Look.marked(In) : Scp1730Blocks.ShowerTile);
                    if (Math.abs(U) <= 1) {
                        B.keepClear(X, R.F + 1, Z);
                        B.keepClear(X, R.F + 2, Z);
                    }
                }
            }
        }
        G.set(D.x(-1, 3), R.F, D.z(-1, 3), Scp1730Blocks.Drain);
        G.set(D.x(1, 5), R.F, D.z(1, 5), Scp1730Blocks.Drain);
        for (int V : new int[]{2, 5}) {
            mount(G, D.x(-2, V), R.F + R.H, D.z(-2, V), mod("containment_room_sedation_vent[facing=" + Plus + "]"), Plus);
            mount(G, D.x(2, V), R.F + R.H, D.z(2, V), mod("containment_room_sedation_vent[facing=" + Minus + "]"), Minus);
        }
        G.set(D.x(0, 4), R.F + R.H, D.z(0, 4), Scp1730Blocks.facing(Scp1730Blocks.EmergencyLight, "down"));
        G.set(D.x(0, 6), R.F + R.H, D.z(0, 6), Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, "down"));
        G.set(D.x(-2, 2), R.F + 2, D.z(-2, 2), Scp1730Blocks.with(Scp1730Blocks.Control, "facing=" + Plus + ",kind=exit,active=false"));
        B.fixture("biocontrol", D.x(-2, 2), R.F + 2, D.z(-2, 2), Plus, InnerReader, R.Zone);
        B.keepClear(D.x(-2, 2), R.F + 1, D.z(-2, 2));
        G.set(D.x(-2, 2), R.F + 3, D.z(-2, 2), mod("facility_sign_decontamination_small[facing=" + Plus + "]"));
        G.set(D.x(2, 2), R.F + 3, D.z(2, 2), mod("facility_sign_decontamination_small[facing=" + Minus + "]"));
        mount(G, D.x(2, 2), R.F + 2, D.z(2, 2), mod("containment_room_status_panel[facing=" + Minus + ",powered=false]"), Minus);
        R.tall(D.x(2, 4), D.z(2, 4), mod("locker"), "facing=" + Minus + ",open=false");
        R.tall(D.x(2, 5), D.z(2, 5), mod("locker"), "facing=" + Minus + ",open=" + Rng.nextBoolean());
        R.put(D.x(-2, 5), R.F + 1, D.z(-2, 5), mod("biohazard_bin[facing=" + Plus + "]"));
        R.put(D.x(-2, 4), R.F + 1, D.z(-2, 4), mod("medical_scrub_sink_bottom[facing=" + Plus + "]"));
        G.set(D.x(-2, 6), R.F + 3, D.z(-2, 6), Scp1730Blocks.facing(Scp1730Blocks.SignBiohazard, Plus));
        G.set(D.x(2, 6), R.F + 3, D.z(2, 6), Scp1730Blocks.facing(Scp1730Blocks.SignBiohazard, Minus));
        List<int[]> Cells = cells(B, R, Used);
        for (int[] Corner : new int[][]{{R.X0, R.Z0}, {R.X1 - CellDepth + 2, R.Z0}, {R.X0, R.Z1 - CellDepth + 2}, {R.X1 - CellDepth + 2, R.Z1 - CellDepth + 2}}) {
            B.SealedCells.add(new int[]{Corner[0], Corner[1], Corner[0] + CellDepth - 2, Corner[1] + CellDepth - 2, R.F});
        }
        booth(B, R, D, Out);
        int Wanted = WingSetPieces[Rng.nextInt(WingSetPieces.length)];
        List<int[]> Open = new ArrayList<>();
        for (int[] Cell : Cells) {
            Open.add(Cell);
        }
        B.shuffle(Open);
        int Plaque = 1 + Rng.nextInt(20);
        int Pieces = 0;
        int Loot = 0;
        for (int[] Cell : Open) {
            int Kind;
            if (Pieces < Wanted) {
                Kind = 0;
                Pieces++;
            } else {
                Kind = 1 + Rng.nextInt(4);
            }
            if (Kind == 3 && Loot < 1) {
                Loot++;
            }
            dressCell(B, R, Cell, Kind, Plaque++, Kind == 3 && Loot == 1);
            if (Kind == 3) {
                Loot = 2;
            }
        }
        for (int Index = 0; Index < 5; Index++) {
            int X = R.X0 + CellDepth + 1 + Rng.nextInt(Math.max(1, Wide - CellDepth * 2 - 2));
            int Z = R.Z0 + CellDepth + 1 + Rng.nextInt(Math.max(1, Deep - CellDepth * 2 - 2));
            String Prop = new String[]{Scp1730Blocks.Gurney, Scp1730Blocks.BodyBag, mod("biohazard_bin"), Scp1730Blocks.OverturnedChair, mod("iv_stand")}[Index];
            if (Prop.endsWith("iv_stand")) {
                R.tall(X, Z, Prop, "facing=" + face(Rng.nextInt(4)));
            } else if (Prop.endsWith("biohazard_bin")) {
                int Start = Rng.nextInt(4);
                for (int Turn = 0; Turn < 4; Turn++) {
                    int Way = (Start + Turn) % 4;
                    if (G.empty(X + Scp1730Layout.Dx[Way], R.F + 1, Z + Scp1730Layout.Dz[Way]) && G.empty(X + Scp1730Layout.Dx[Way], R.F + 2, Z + Scp1730Layout.Dz[Way])) {
                        R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Prop, face(Way)));
                        break;
                    }
                }
            } else {
                R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Prop, face(Rng.nextInt(4))));
            }
        }
        for (int U = -2; U <= 2; U++) {
            if (Math.abs(U) >= 2 || Rng.nextInt(3) == 0) {
                R.put(D.x(U, AirlockDepth + 3), R.F + 1, D.z(U, AirlockDepth + 3), Scp1730Blocks.facing(Scp1730Blocks.Sandbags, Out));
            }
        }
        for (int Mark = 0; Mark < 10; Mark++) {
            B.decal(D.x(Rng.nextInt(9) - 4, AirlockDepth + 1 + Rng.nextInt(10)), R.F, D.z(Rng.nextInt(9) - 4, AirlockDepth + 1 + Rng.nextInt(10)), "up",
                    B.pick(new int[]{Scp1730Builder.Blood, Scp1730Builder.Footprints, Scp1730Builder.Puddle, Scp1730Builder.Claws}), R.Zone);
        }
        R.lights(6);
        B.TurretSpots.add(new int[]{R.X0 + CellDepth + 1, R.F + R.H, R.Z0 + CellDepth + 1, R.Zone});
        B.TurretSpots.add(new int[]{R.X1 - CellDepth - 1, R.F + R.H, R.Z1 - CellDepth - 1, R.Zone});
        R.mess(6);
    }

    static void wingDoor(Scp1730Builder B, Scp1730Rooms.Box R, Frame D, Scp1730Layout.Door Main) {
        Scp1730Grid G = B.Grid;
        String Axis = Main.Dir % 2 == 0 ? "x" : "z";
        String Out = face(Main.Dir);
        for (int Side = -1; Side <= 1; Side++) {
            for (int Level = 1; Level <= 3; Level++) {
                int X = D.x(Side, 0);
                int Z = D.z(Side, 0);
                G.set(X, R.F + Level, Z, Scp1730Blocks.with(Scp1730Blocks.Seal, "axis=" + Axis));
                B.fixture("bioseal", X, R.F + Level, Z, Out, (Side + 1) * 3 + (Level - 1), R.Zone);
            }
        }
        Scp1730Blocks.Palette Hall = B.corridorPalette(R.Zone);
        int Ox = Scp1730Layout.Dx[Main.Dir];
        int Oz = Scp1730Layout.Dz[Main.Dir];
        int[] Panel = B.beside(D.Cx(), D.Cz(), D.Lx(), D.Lz(), Ox, Oz, R.F + 2, 1);
        if (Panel == null) {
            Panel = B.niche(D.Cx(), D.Cz(), D.Lx(), D.Lz(), Ox, Oz, R.F, 1, Hall);
        }
        if (Panel != null) {
            G.set(Panel[0], R.F + 2, Panel[1], Scp1730Blocks.with(Scp1730Blocks.Control, "facing=" + Out + ",kind=clearance,active=false"));
            B.fixture("biocontrol", Panel[0], R.F + 2, Panel[1], Out, OuterReader, R.Zone);
            if (G.empty(Panel[0], R.F + 3, Panel[1])) {
                G.set(Panel[0], R.F + 3, Panel[1], Scp1730Blocks.facing(Scp1730Blocks.SignBiohazard, Out));
            }
        }
        for (int Side = -1; Side <= 1; Side++) {
            int X = D.x(Side, -1);
            int Z = D.z(Side, -1);
            if (G.get(X, R.F, Z) != null && G.empty(X, R.F + 1, Z)) {
                G.set(X, R.F, Z, Hall.marked(face(Main.Dir + 2)));
            }
        }
        if (G.empty(D.x(0, -1), R.F + 3, D.z(0, -1)) && G.get(D.x(0, -1), R.F + 4, D.z(0, -1)) != null) {
            G.set(D.x(0, -1), R.F + 3, D.z(0, -1), Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, "down"));
        }
    }

    static int[] cellAt(Scp1730Rooms.Box R, int Side, int T, int Depth) {
        return switch (Side) {
            case 0 -> new int[]{T, R.Z0 + Depth};
            case 2 -> new int[]{T, R.Z1 - Depth};
            case 3 -> new int[]{R.X0 + Depth, T};
            default -> new int[]{R.X1 - Depth, T};
        };
    }

    static List<int[]> cells(Scp1730Builder B, Scp1730Rooms.Box R, boolean[][] Used) {
        List<int[]> Made = new ArrayList<>();
        for (int Side : new int[]{0, 2, 3, 1}) {
            boolean AlongX = Side % 2 == 0;
            int Start = AlongX ? R.X0 + CellDepth : R.Z0 + CellDepth;
            int End = AlongX ? R.X1 - CellDepth : R.Z1 - CellDepth;
            for (int S = Start; S + CellWidth - 2 <= End; S += CellWidth) {
                boolean Clash = false;
                for (int T = S; T <= S + CellWidth - 2 && !Clash; T++) {
                    for (int Depth = 0; Depth < CellDepth; Depth++) {
                        int[] At = cellAt(R, Side, T, Depth);
                        if (Used[At[0] - R.X0][At[1] - R.Z0]) {
                            Clash = true;
                            break;
                        }
                    }
                }
                if (Clash) {
                    continue;
                }
                for (int T = S - 1; T <= S + CellWidth - 1; T++) {
                    for (int Depth = 0; Depth < CellDepth; Depth++) {
                        int[] At = cellAt(R, Side, T, Depth);
                        if (inside(R, At[0], At[1])) {
                            Used[At[0] - R.X0][At[1] - R.Z0] = true;
                        }
                    }
                }
                Made.add(new int[]{Side, S});
                buildCell(B, R, Side, S);
            }
        }
        return Made;
    }

    static void buildCell(Scp1730Builder B, Scp1730Rooms.Box R, int Side, int S) {
        Scp1730Grid G = B.Grid;
        String Axis = Side % 2 == 0 ? "z" : "x";
        for (int T = S - 1; T <= S + CellWidth - 1; T++) {
            for (int Depth = 0; Depth < CellDepth; Depth++) {
                int[] At = cellAt(R, Side, T, Depth);
                if (!inside(R, At[0], At[1])) {
                    continue;
                }
                boolean Divider = T == S - 1 || T == S + CellWidth - 1;
                boolean Front = Depth == CellDepth - 1;
                if (Divider) {
                    column(B, R, At[0], At[1], 1, null);
                } else if (Front) {
                    int Offset = T - S;
                    for (int Level = 1; Level <= R.H; Level++) {
                        String State = R.Look.wall(Level, R.H);
                        if (Offset == 2 && Level <= 3) {
                            State = null;
                        } else if ((Offset == 1 || Offset == 3) && Level == 2) {
                            State = R.Look.window(Axis);
                        }
                        G.set(At[0], R.F + Level, At[1], State);
                    }
                }
            }
        }
    }

    static void dressCell(Scp1730Builder B, Scp1730Rooms.Box R, int[] Cell, int Kind, int Number, boolean Loot) {
        Scp1730Grid G = B.Grid;
        Random Rng = B.Rng;
        int Side = Cell[0];
        int S = Cell[1];
        String Toward = face(Side + 2);
        String Back = face(Side);
        int[] Door = cellAt(R, Side, S + 2, CellDepth - 1);
        int[] Hall = cellAt(R, Side, S + 2, CellDepth);
        int[] Centre = cellAt(R, Side, S + 2, 2);
        int[] Rear = cellAt(R, Side, S + 2, 0);
        int[] Left = cellAt(R, Side, S, 1);
        int[] Right = cellAt(R, Side, S + 4, 1);
        int[] PlaqueAt = cellAt(R, Side, S, CellDepth);
        String Inward = Side % 2 == 0 ? "west" : "north";
        int[] SignAt = cellAt(R, Side, S + 1, CellDepth);
        if (inside(R, PlaqueAt[0], PlaqueAt[1])) {
            G.set(PlaqueAt[0], R.F + 2, PlaqueAt[1], mod("containment_room_cell_plaque[facing=" + Toward + ",number=" + (1 + (Number - 1) % 16) + "]"));
        }
        if (inside(R, SignAt[0], SignAt[1]) && G.empty(SignAt[0], R.F + 3, SignAt[1])) {
            G.set(SignAt[0], R.F + 3, SignAt[1], Scp1730Blocks.facing(Kind == 0 ? Scp1730Blocks.SignBiohazard : Rng.nextBoolean() ? Scp1730Blocks.SignBiohazard : Scp1730Blocks.SignKeter, Toward));
        }
        G.set(Centre[0], R.F, Centre[1], Scp1730Blocks.Drain);
        int[] Nozzle = cellAt(R, Side, S + 2, 0);
        if (G.empty(Nozzle[0], R.F + R.H, Nozzle[1])) {
            mount(G, Nozzle[0], R.F + R.H, Nozzle[1], mod("containment_room_sedation_vent[facing=" + Toward + "]"), Toward);
        }
        boolean Sealed = Kind == 1 || Kind == 4;
        if (Sealed) {
            for (int Level = 1; Level <= 3; Level++) {
                G.set(Door[0], R.F + Level, Door[1], Scp1730Blocks.facing(Scp1730Blocks.HeavyDoor, Toward));
            }
            B.SealedCells.add(new int[]{Math.min(cellAt(R, Side, S, 0)[0], cellAt(R, Side, S + 4, CellDepth - 2)[0]), Math.min(cellAt(R, Side, S, 0)[1], cellAt(R, Side, S + 4, CellDepth - 2)[1]),
                    Math.max(cellAt(R, Side, S, 0)[0], cellAt(R, Side, S + 4, CellDepth - 2)[0]), Math.max(cellAt(R, Side, S, 0)[1], cellAt(R, Side, S + 4, CellDepth - 2)[1]), R.F});
            if (inside(R, Hall[0], Hall[1])) {
                int[] Panel = cellAt(R, Side, S + 3, CellDepth);
                if (G.empty(Panel[0], R.F + 3, Panel[1])) {
                    G.set(Panel[0], R.F + 3, Panel[1], Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, Toward));
                }
            }
        } else {
            for (int Depth = 1; Depth <= CellDepth; Depth++) {
                int[] Path = cellAt(R, Side, S + 2, Depth);
                B.keepClear(Path[0], R.F + 1, Path[1]);
                B.keepClear(Path[0], R.F + 2, Path[1]);
            }
            B.decal(Door[0], R.F, Door[1], "up", Scp1730Builder.Scorch, R.Zone);
        }
        switch (Kind) {
            case 0 -> {
                int Piece = B.SetPieces.next(R.Zone, Boolean.TRUE);
                B.fixture("setpiece", Centre[0], R.F + 1, Centre[1], Toward, Piece, R.Zone);
                scene(B, R, Side, S, Scp1730SetPieces.scene(R.Zone, Piece, B.Seed));
                int[] Wreck = cellAt(R, Side, S + 3, CellDepth);
                R.put(Wreck[0], R.F + 1, Wreck[1], Scp1730Blocks.facing(Scp1730Blocks.CellDoorWreck, face(Side + 1)));
                for (int Mark = 0; Mark < 5; Mark++) {
                    int[] Spot = cellAt(R, Side, S + Rng.nextInt(5), Rng.nextInt(CellDepth - 1));
                    B.decal(Spot[0], R.F, Spot[1], "up", B.pick(new int[]{Scp1730Builder.Claws, Scp1730Builder.Blood, Scp1730Builder.Puddle}), R.Zone);
                }
                for (int T : new int[]{S + 1, S + 3}) {
                    int[] Glass = cellAt(R, Side, T, CellDepth - 1);
                    if (Rng.nextBoolean()) {
                        G.set(Glass[0], R.F + 2, Glass[1], null);
                        R.put(cellAt(R, Side, T, CellDepth)[0], R.F + 1, cellAt(R, Side, T, CellDepth)[1], Scp1730Blocks.facing(Scp1730Blocks.GlassShards, "up"));
                    }
                }
            }
            case 1 -> {
                R.stack(Centre[0], Centre[1], mod("containment_room_specimen_tank"), Toward, 2);
                R.stack(Left[0], Left[1], mod("containment_room_padded_panel"), Toward, 2);
                R.stack(Right[0], Right[1], mod("containment_room_padded_panel"), Toward, 2);
            }
            case 2 -> {
                R.put(Centre[0], R.F + 1, Centre[1], Scp1730Blocks.facing(Scp1730Blocks.BrokenGlass, Toward));
                R.put(Rear[0], R.F + 1, Rear[1], mod("lab_viral_storage[chilled=true,facing=" + Toward + ",hatch=false,venting=false]"));
                for (int Mark = 0; Mark < 4; Mark++) {
                    int[] Spot = cellAt(R, Side, S + Rng.nextInt(5), Rng.nextInt(CellDepth - 1));
                    B.decal(Spot[0], R.F, Spot[1], "up", Scp1730Builder.Puddle, R.Zone);
                }
                R.put(Left[0], R.F + 1, Left[1], Scp1730Blocks.facing(Scp1730Blocks.GlassShards, "up"));
            }
            case 3 -> {
                int[] Bunk = cellAt(R, Side, S, 0);
                int[] Foot = cellAt(R, Side, S, 1);
                if (R.free(Bunk[0], R.F + 1, Bunk[1]) && R.free(Foot[0], R.F + 1, Foot[1])) {
                    G.set(Bunk[0], R.F + 1, Bunk[1], mod("lcz_cell_bunk[facing=" + Toward + ",pair=main]"));
                    G.set(Foot[0], R.F + 1, Foot[1], mod("lcz_cell_bunk[facing=" + Toward + ",pair=extension]"));
                }
                R.put(Right[0], R.F + 1, Right[1], mod("biohazard_bin[facing=" + Inward + "]"));
                R.tall(cellAt(R, Side, S + 4, 0)[0], cellAt(R, Side, S + 4, 0)[1], mod("iv_stand"), "facing=" + Toward);
                if (Loot) {
                    int[] Crate = cellAt(R, Side, S + 4, 2);
                    if (R.free(Crate[0], R.F + 2, Crate[1]) && mount(G, Crate[0], R.F + 2, Crate[1], mod("medical_cabinet[facing=" + Inward + ",open=false]"), Inward)) {
                        B.loot(Crate[0], R.F + 2, Crate[1], Inward, 2, R.Zone);
                    }
                }
            }
            default -> {
                for (int T = S; T <= S + 4; T++) {
                    int[] Wall = cellAt(R, Side, T, 0);
                    R.stack(Wall[0], Wall[1], mod("containment_room_padded_panel"), Toward, Math.min(3, R.H));
                }
                R.put(Centre[0], R.F + 1, Centre[1], mod("containment_room_restraint_anchor[facing=" + Toward + "]"));
            }
        }
        if (Kind != 1 && Kind != 4 && G.empty(Centre[0], R.F + R.H - 1, Centre[1]) && Rng.nextBoolean()) {
            B.Rooms.ceilingLight(cellAt(R, Side, S + 1, 1)[0], R.F + R.H, cellAt(R, Side, S + 1, 1)[1], R.Zone, Side % 2 == 1);
        }
    }

    static void scene(Scp1730Builder B, Scp1730Rooms.Box R, int Side, int S, String Scene) {
        Scp1730Grid G = B.Grid;
        Random Rng = B.Rng;
        String Toward = face(Side + 2);
        String Back = face(Side);
        int[] Rear = cellAt(R, Side, S + 2, 0);
        switch (Scene) {
            case Scp1730SetPieces.PlagueDoctor -> {
                int[] Head = cellAt(R, Side, S, 0);
                int[] Foot = cellAt(R, Side, S, 1);
                if (R.free(Head[0], R.F + 1, Head[1]) && R.free(Foot[0], R.F + 1, Foot[1])) {
                    G.set(Head[0], R.F + 1, Head[1], mod("hospital_bed[facing=" + Back + ",part=head]"));
                    G.set(Foot[0], R.F + 1, Foot[1], mod("hospital_bed[facing=" + Back + ",part=foot]"));
                }
                int[] Drip = cellAt(R, Side, S + 1, 0);
                R.tall(Drip[0], Drip[1], mod("iv_stand"), "facing=" + Toward);
                int[] Monitor = cellAt(R, Side, S + 4, 0);
                R.put(Monitor[0], R.F + 1, Monitor[1], mod("vitals_monitor[facing=" + Toward + ",lit=true]"));
                int[] Cabinet = cellAt(R, Side, S + 3, 0);
                if (R.free(Cabinet[0], R.F + 2, Cabinet[1])) {
                    mount(G, Cabinet[0], R.F + 2, Cabinet[1], mod("medical_cabinet[facing=" + Toward + ",open=true]"), Toward);
                }
                int[] Cart = cellAt(R, Side, S + 4, 2);
                R.put(Cart[0], R.F + 1, Cart[1], mod("medical_cart[facing=" + (Side % 2 == 0 ? "west" : "north") + "]"));
                for (int Mark = 0; Mark < 3; Mark++) {
                    int[] Spot = cellAt(R, Side, S + Rng.nextInt(5), Rng.nextInt(CellDepth - 1));
                    R.put(Spot[0], R.F + 1, Spot[1], Scp1730Blocks.PaperStack + "[facing=" + face(Rng.nextInt(4)) + ",variant=" + Rng.nextInt(6) + "]");
                    B.decal(Spot[0], R.F, Spot[1], "up", Scp1730Builder.Footprints, R.Zone);
                }
            }
            case Scp1730SetPieces.Hive -> {
                for (int T : new int[]{S, S + 4}) {
                    int[] Corner = cellAt(R, Side, T, 0);
                    R.put(Corner[0], R.F + 1, Corner[1], mod("containment_room_specimen_tank[facing=" + Toward + ",part=single]"));
                    int[] Shard = cellAt(R, Side, T, 1);
                    R.put(Shard[0], R.F + 1, Shard[1], Scp1730Blocks.facing(Scp1730Blocks.GlassShards, "up"));
                }
                int[] Vent = cellAt(R, Side, S + 1, 0);
                if (Scp1730RoomDressing.backed(B, new int[]{Vent[0], Vent[1], (Side + 2) % 4}, R.F + 1)) {
                    R.put(Vent[0], R.F + 1, Vent[1], Scp1730Blocks.facing(Scp1730Blocks.BrokenVent, Toward));
                }
                for (int Mark = 0; Mark < 7; Mark++) {
                    int[] Spot = cellAt(R, Side, S + Rng.nextInt(5), Rng.nextInt(CellDepth - 1));
                    R.put(Spot[0], R.F + 1, Spot[1], Scp1730Blocks.facing(Scp1730Blocks.MouldPatch, "up"));
                }
            }
            case Scp1730SetPieces.BeastPen -> {
                for (int T : new int[]{S, S + 4}) {
                    int[] Anchor = cellAt(R, Side, T, 0);
                    R.put(Anchor[0], R.F + 1, Anchor[1], mod("containment_room_restraint_anchor[facing=" + Toward + "]"));
                    int[] Hang = cellAt(R, Side, T, 2);
                    for (int Level = R.H; Level >= R.H - 2; Level--) {
                        if (G.empty(Hang[0], R.F + Level, Hang[1])) {
                            G.set(Hang[0], R.F + Level, Hang[1], Scp1730Blocks.Chain);
                        }
                    }
                }
                int[] Bag = cellAt(R, Side, S + 3, 0);
                R.put(Bag[0], R.F + 1, Bag[1], Scp1730Blocks.facing(Scp1730Blocks.BodyBag, face(Side + 1)));
                for (int Mark = 0; Mark < 6; Mark++) {
                    int[] Spot = cellAt(R, Side, S + Rng.nextInt(5), Rng.nextInt(CellDepth - 1));
                    B.decal(Spot[0], R.F, Spot[1], "up", B.pick(new int[]{Scp1730Builder.Claws, Scp1730Builder.Blood, Scp1730Builder.Scorch}), R.Zone);
                }
                int[] Rubble = cellAt(R, Side, S + 1, 1);
                R.put(Rubble[0], R.F + 1, Rubble[1], Scp1730Blocks.facing(Scp1730Blocks.Rubble, Toward));
            }
            default -> {
                G.set(Rear[0], R.F + 1, Rear[1], mod("containment_room_restraint_anchor[facing=" + Toward + "]"));
                int[] Left = cellAt(R, Side, S, 1);
                for (int Level = R.H; Level >= R.H - 1; Level--) {
                    if (G.empty(Left[0], R.F + Level, Left[1])) {
                        G.set(Left[0], R.F + Level, Left[1], Scp1730Blocks.Chain);
                    }
                }
            }
        }
    }

    static void booth(Scp1730Builder B, Scp1730Rooms.Box R, Frame D, String Out) {
        Scp1730Grid G = B.Grid;
        Random Rng = B.Rng;
        int Cx = R.cx();
        int Cz = R.cz();
        int Margin = CellDepth + 2 + BoothHalf;
        if (Cx - R.X0 < Margin || R.X1 - Cx < Margin || Cz - R.Z0 < Margin || R.Z1 - Cz < Margin) {
            return;
        }
        int Ix = D.Ix();
        int Iz = D.Iz();
        int DoorX = Cx - Ix * BoothHalf;
        int DoorZ = Cz - Iz * BoothHalf;
        for (int Ox = -BoothHalf; Ox <= BoothHalf; Ox++) {
            for (int Oz = -BoothHalf; Oz <= BoothHalf; Oz++) {
                boolean Edge = Math.abs(Ox) == BoothHalf || Math.abs(Oz) == BoothHalf;
                int X = Cx + Ox;
                int Z = Cz + Oz;
                if (!Edge) {
                    continue;
                }
                boolean Corner = Math.abs(Ox) == BoothHalf && Math.abs(Oz) == BoothHalf;
                boolean Doorway = X == DoorX && Z == DoorZ;
                String Axis = Math.abs(Oz) == BoothHalf ? "z" : "x";
                for (int Level = 1; Level <= R.H; Level++) {
                    String State = R.Look.wall(Level, R.H);
                    if (Doorway && Level <= 3) {
                        State = null;
                    } else if (!Corner && Level == 2) {
                        State = R.Look.window(Axis);
                    }
                    G.set(X, R.F + Level, Z, State);
                }
            }
        }
        for (int Step = -1; Step <= BoothHalf; Step++) {
            B.keepClear(DoorX + Ix * Step, R.F + 1, DoorZ + Iz * Step);
            B.keepClear(DoorX + Ix * Step, R.F + 2, DoorZ + Iz * Step);
        }
        G.set(DoorX, R.F, DoorZ, R.Look.marked(face(dirOf(Ix, Iz))));
        int Lx = Iz != 0 ? 1 : 0;
        int Lz = Ix != 0 ? 1 : 0;
        String Desk = Scp1730Builder.facingOf(-Ix, -Iz);
        int Rx = Cx + Ix * (BoothHalf - 1);
        int Rz = Cz + Iz * (BoothHalf - 1);
        for (int Side = -2; Side <= 2; Side++) {
            int X = Rx + Lx * Side;
            int Z = Rz + Lz * Side;
            if (R.put(X, R.F + 1, Z, mod("metal_desk[facing=" + Desk + ",left=" + (Side > -2) + ",right=" + (Side < 2) + "]"))) {
                String Top = Side % 2 == 0 ? mod("security_monitor[facing=" + Desk + ",lit=" + Rng.nextBoolean() + ",screen=" + Rng.nextInt(10) + "]")
                        : mod("computer_terminal[facing=" + Desk + ",lit=false,screen=" + Rng.nextInt(10) + "]");
                R.put(X, R.F + 2, Z, Top);
            }
        }
        for (int Side = -1; Side <= 1; Side += 2) {
            R.put(Rx - Ix + Lx * Side, R.F + 1, Rz - Iz + Lz * Side, mod("office_chair[facing=" + Scp1730Builder.facingOf(Ix, Iz) + "]"));
        }
        int Cab = BoothHalf - 1;
        if (R.free(Cx + Lx * (Cab + 1) - Lx, R.F + 2, Cz + Lz * (Cab + 1) - Lz)
                && mount(G, Cx + Lx * Cab, R.F + 2, Cz + Lz * Cab, mod("medical_cabinet[facing=" + Scp1730Builder.facingOf(-Lx, -Lz) + ",open=false]"), Scp1730Builder.facingOf(-Lx, -Lz))) {
            B.loot(Cx + Lx * Cab, R.F + 2, Cz + Lz * Cab, Scp1730Builder.facingOf(-Lx, -Lz), 2, R.Zone);
        }
        R.put(Cx - Lx * Cab, R.F + 1, Cz - Lz * Cab, mod("biohazard_bin[facing=" + Scp1730Builder.facingOf(Lx, Lz) + "]"));
        if (G.empty(Cx, R.F + R.H, Cz)) {
            G.set(Cx, R.F + R.H, Cz, Scp1730Blocks.Fluorescent);
        }
        if (G.empty(DoorX - Ix + Lx * 2, R.F + 3, DoorZ - Iz + Lz * 2)) {
            G.set(DoorX - Ix + Lx * 2, R.F + 3, DoorZ - Iz + Lz * 2, Scp1730Blocks.facing(Scp1730Blocks.SignBiohazard, Scp1730Builder.facingOf(-Ix, -Iz)));
        }
    }

    static int dirOf(int Dx, int Dz) {
        return Scp1730Builder.dirOf(Scp1730Builder.facingOf(Dx, Dz));
    }

    static void reservoir(Scp1730Builder B, Scp1730Rooms.Box R) {
        Scp1730Grid G = B.Grid;
        Random Rng = B.Rng;
        int F = R.F;
        int Bx0 = R.X0 + Walkway;
        int Bz0 = R.Z0 + Walkway;
        int Bx1 = R.X1 - Walkway;
        int Bz1 = R.Z1 - Walkway;
        int Cx = R.cx();
        int Cz = R.cz();
        int Bottom = F - BasinDepth - 1;
        String Wall = Scp1730Blocks.Hcz.WallMiddle;
        boolean[][] Basin = new boolean[R.X1 - R.X0 + 1][R.Z1 - R.Z0 + 1];
        for (int X = Bx0; X <= Bx1; X++) {
            for (int Z = Bz0; Z <= Bz1; Z++) {
                boolean Island = Math.abs(X - Cx) <= IslandHalf && Math.abs(Z - Cz) <= IslandHalf;
                Basin[X - R.X0][Z - R.Z0] = !Island;
            }
        }
        for (int X = Bx0 - 1; X <= Bx1 + 1; X++) {
            for (int Z = Bz0 - 1; Z <= Bz1 + 1; Z++) {
                if (wet(R, Basin, X, Z)) {
                    G.set(X, F, Z, null);
                    for (int Y = F - BasinDepth; Y <= F - 1; Y++) {
                        G.set(X, Y, Z, Scp1730Blocks.Water);
                    }
                    G.set(X, Bottom, Z, Rng.nextInt(9) == 0 ? Scp1730Blocks.Drain : Rng.nextInt(7) == 0 ? Scp1730Blocks.RubbleFloor : Scp1730Blocks.Hcz.Floor);
                    B.keepClear(X, F + 1, Z);
                    B.keepClear(X, F + 2, Z);
                    continue;
                }
                boolean Border = false;
                for (int Dir = 0; Dir < 4; Dir++) {
                    if (wet(R, Basin, X + Scp1730Layout.Dx[Dir], Z + Scp1730Layout.Dz[Dir])) {
                        Border = true;
                    }
                }
                if (Border) {
                    for (int Y = Bottom; Y <= F - 1; Y++) {
                        G.set(X, Y, Z, Wall);
                    }
                }
            }
        }
        B.Basins.add(new Scp1730Basin("water", Bx0, Bz0, Bx1, Bz1, F - 1, Bottom, R.Zone));
        int Collapsed = Rng.nextInt(4);
        List<int[]> Landings = new ArrayList<>();
        for (int Dir = 0; Dir < 4; Dir++) {
            int Ax = Scp1730Layout.Dx[Dir];
            int Az = Scp1730Layout.Dz[Dir];
            int Lx = Dir % 2 == 0 ? 1 : 0;
            int Lz = Dir % 2 == 0 ? 0 : 1;
            for (int Step = IslandHalf + 1; ; Step++) {
                int Mx = Cx + Ax * Step;
                int Mz = Cz + Az * Step;
                if (!wet(R, Basin, Mx, Mz)) {
                    Landings.add(new int[]{Mx, Mz, Dir});
                    break;
                }
                boolean Gap = Dir == Collapsed && (Step == IslandHalf + 4 || Step == IslandHalf + 5);
                for (int Lane = -1; Lane <= 1; Lane++) {
                    int X = Mx + Lx * Lane;
                    int Z = Mz + Lz * Lane;
                    if (Gap) {
                        G.set(X, F, Z, null);
                        continue;
                    }
                    if (Lane == 0) {
                        G.set(X, F, Z, Scp1730Blocks.Catwalk);
                    } else {
                        String Edge = Scp1730Builder.facingOf(Lx * Lane, Lz * Lane);
                        G.set(X, F, Z, railing(Scp1730Blocks.RailingYellow, true, Edge));
                    }
                }
            }
            if (Dir == Collapsed) {
                int Hx = Cx + Ax * (IslandHalf + 3);
                int Hz = Cz + Az * (IslandHalf + 3);
                if (G.empty(Hx, F + R.H, Hz)) {
                    G.set(Hx, F + R.H, Hz, Scp1730Blocks.facing(Scp1730Blocks.HangingCables, "down"));
                }
            }
        }
        for (int X = Cx - IslandHalf; X <= Cx + IslandHalf; X++) {
            for (int Z = Cz - IslandHalf; Z <= Cz + IslandHalf; Z++) {
                boolean Rim = Math.abs(X - Cx) == IslandHalf || Math.abs(Z - Cz) == IslandHalf;
                if (!Rim || Math.abs(X - Cx) <= 1 || Math.abs(Z - Cz) <= 1) {
                    continue;
                }
                List<String> Edges = new ArrayList<>();
                for (int Dir = 0; Dir < 4; Dir++) {
                    if (wet(R, Basin, X + Scp1730Layout.Dx[Dir], Z + Scp1730Layout.Dz[Dir])) {
                        Edges.add(face(Dir));
                    }
                }
                if (!Edges.isEmpty() && G.empty(X, F + 1, Z)) {
                    G.set(X, F + 1, Z, railing(Scp1730Blocks.Railing, false, Edges.toArray(new String[0])));
                    B.keepClear(X, F + 1, Z);
                }
            }
        }
        List<int[]> Ladders = ladders(B, R, Basin, Bx0, Bz0, Bx1, Bz1, Landings);
        for (int X = Bx0 - 1; X <= Bx1 + 1; X++) {
            for (int Z = Bz0 - 1; Z <= Bz1 + 1; Z++) {
                boolean Ring = X == Bx0 - 1 || X == Bx1 + 1 || Z == Bz0 - 1 || Z == Bz1 + 1;
                if (!Ring || !inside(R, X, Z) || nearAny(Landings, X, Z, 1) || nearAny(Ladders, X, Z, 1) || nearDoor(R, X, Z, 3)) {
                    continue;
                }
                List<String> Edges = new ArrayList<>();
                for (int Dir = 0; Dir < 4; Dir++) {
                    if (wet(R, Basin, X + Scp1730Layout.Dx[Dir], Z + Scp1730Layout.Dz[Dir])) {
                        Edges.add(face(Dir));
                    }
                }
                if (!Edges.isEmpty() && Rng.nextInt(5) > 0 && G.empty(X, F + 1, Z)) {
                    G.set(X, F + 1, Z, railing(Scp1730Blocks.Railing, false, Edges.toArray(new String[0])));
                    B.keepClear(X, F + 1, Z);
                }
            }
        }
        int Falls = 0;
        for (int Try = 0; Try < 60 && Falls < Waterfalls; Try++) {
            int X = Bx0 + 1 + Rng.nextInt(Bx1 - Bx0 - 1);
            int Z = Bz0 + 1 + Rng.nextInt(Bz1 - Bz0 - 1);
            if (!wet(R, Basin, X, Z) || Math.abs(X - Cx) <= 2 || Math.abs(Z - Cz) <= 2 || nearAny(Ladders, X, Z, 2) || !G.empty(X, F, Z) || !G.empty(X, F + R.H, Z)) {
                continue;
            }
            boolean Ceiling = true;
            for (int Dir = 0; Dir < 4; Dir++) {
                Ceiling &= G.get(X + Scp1730Layout.Dx[Dir], F + R.H + 1, Z + Scp1730Layout.Dz[Dir]) != null;
            }
            if (!Ceiling) {
                continue;
            }
            G.set(X, F + R.H + 2, Z, Scp1730Blocks.Hcz.Ceiling);
            G.set(X, F + R.H + 1, Z, Scp1730Blocks.Water);
            for (int Y = F; Y <= F + R.H; Y++) {
                G.set(X, Y, Z, Scp1730Blocks.Falling);
            }
            B.fixture("waterfall", X, F + R.H + 1, Z, "down", Falls, R.Zone);
            Falls++;
        }
        pumpIsland(B, R, Cx, Cz);
        wade(B, R, Basin, Landings);
        wallPipes(B, R);
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= 10) {
                break;
            }
            if (nearDoor(R, Cell[0], Cell[1], 2)) {
                continue;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Count % 5) {
                case 0 -> R.stack(Cell[0], Cell[1], mod("hcz_pipe_manifold"), Out, R.H);
                case 1 -> R.put(Cell[0], R.F + 2, Cell[1], mod("hcz_valve_panel[facing=" + Out + "]"));
                case 2 -> R.stack(Cell[0], Cell[1], mod("hcz_hazard_crate"), Out, 1 + Rng.nextInt(2));
                case 3 -> R.put(Cell[0], R.F + 1, Cell[1], mod("chemical_barrel[facing=" + Out + "]"));
                default -> R.stack(Cell[0], Cell[1], mod("hcz_steam_vent"), Out, 2);
            };
            if (Done) {
                Count++;
            }
        }
        R.wallContainer(mod("supply_crate"), 1);
        for (int X = R.X0 + 2; X <= R.X1; X += 6) {
            for (int Z = R.Z0 + 2; Z <= R.Z1; Z += 6) {
                if (!G.empty(X, F + R.H, Z) || G.empty(X, F + R.H + 1, Z)) {
                    continue;
                }
                int Roll = Rng.nextInt(100);
                String Light = Roll < 45 ? Scp1730Blocks.BrokenLight : Roll < 75 ? Scp1730Blocks.DeadLight : Roll < 92 ? Scp1730Blocks.EmergencyLight : Scp1730Blocks.AlarmLight;
                G.set(X, F + R.H, Z, Scp1730Blocks.facing(Light, "down"));
            }
        }
        for (int Index = 0; Index < 2; Index++) {
            List<int[]> Walls = R.wallCells();
            int[] Cell = Walls.get(Rng.nextInt(Walls.size()));
            R.put(Cell[0], F + 4, Cell[1], mod("hcz_caged_lamp[facing=" + face(Cell[2]) + ",part=single]"));
        }
        for (int Index = 0; Index < 8; Index++) {
            int X = R.X0 + Rng.nextInt(R.X1 - R.X0 + 1);
            int Z = R.Z0 + Rng.nextInt(R.Z1 - R.Z0 + 1);
            B.decal(X, F, Z, "up", B.pick(new int[]{Scp1730Builder.Puddle, Scp1730Builder.Oil, Scp1730Builder.Rust, Scp1730Builder.Corrosion}), R.Zone);
        }
        posts(G, R.X0, R.Z0, R.X1, R.Z1, F, F + 1);
        R.turret();
    }

    static boolean wet(Scp1730Rooms.Box R, boolean[][] Basin, int X, int Z) {
        return inside(R, X, Z) && Basin[X - R.X0][Z - R.Z0];
    }

    static boolean nearAny(List<int[]> Spots, int X, int Z, int Reach) {
        for (int[] Each : Spots) {
            if (Math.abs(Each[0] - X) <= Reach && Math.abs(Each[1] - Z) <= Reach) {
                return true;
            }
        }
        return false;
    }

    static boolean nearDoor(Scp1730Rooms.Box R, int X, int Z, int Reach) {
        for (Scp1730Layout.Door Each : R.Room.Doors) {
            if (Math.abs(R.Room.doorX(Each) - X) <= Reach && Math.abs(R.Room.doorZ(Each) - Z) <= Reach) {
                return true;
            }
        }
        return false;
    }

    static String railing(String Base, boolean Plate, String... Edges) {
        StringBuilder Made = new StringBuilder(Base).append("[catwalk=").append(Plate);
        for (String Side : new String[]{"north", "east", "south", "west"}) {
            boolean On = false;
            for (String Each : Edges) {
                On |= Each.equals(Side);
            }
            Made.append(",").append(Side).append("=").append(On);
        }
        return Made.append(",post_ne=false,post_se=false,post_sw=false,post_nw=false]").toString();
    }

    static List<int[]> ladders(Scp1730Builder B, Scp1730Rooms.Box R, boolean[][] Basin, int Bx0, int Bz0, int Bx1, int Bz1, List<int[]> Landings) {
        Scp1730Grid G = B.Grid;
        List<int[]> Made = new ArrayList<>();
        int Cx = R.cx();
        int Cz = R.cz();
        List<int[]> Spots = new ArrayList<>();
        for (int X = Bx0; X <= Bx1; X++) {
            Spots.add(new int[]{X, Bz0, 0});
            Spots.add(new int[]{X, Bz1, 2});
        }
        for (int Z = Bz0 + 1; Z < Bz1; Z++) {
            Spots.add(new int[]{Bx0, Z, 3});
            Spots.add(new int[]{Bx1, Z, 1});
        }
        int[][] Island = {{Cx, Cz - IslandHalf - 1, 2}, {Cx + IslandHalf + 1, Cz, 3}, {Cx, Cz + IslandHalf + 1, 0}, {Cx - IslandHalf - 1, Cz, 1}};
        for (int[] Each : Island) {
            for (int Shift = -2; Shift <= 2; Shift += 4) {
                int X = Each[0] + (Each[2] % 2 == 0 ? Shift : 0);
                int Z = Each[1] + (Each[2] % 2 == 0 ? 0 : Shift);
                Spots.add(new int[]{X, Z, Each[2]});
            }
        }
        int Since = LadderEvery;
        for (int[] Spot : Spots) {
            Since++;
            boolean IslandSpot = Math.abs(Spot[0] - Cx) <= IslandHalf + 1 && Math.abs(Spot[1] - Cz) <= IslandHalf + 1;
            if (!IslandSpot && Since < LadderEvery) {
                continue;
            }
            int X = Spot[0];
            int Z = Spot[1];
            int Dir = Spot[2];
            int Wx = X + Scp1730Layout.Dx[Dir];
            int Wz = Z + Scp1730Layout.Dz[Dir];
            if (!wet(R, Basin, X, Z) || wet(R, Basin, Wx, Wz) || nearAny(Landings, X, Z, 1) || !G.empty(X, R.F, Z) || G.get(Wx, R.F, Wz) == null || nearAny(Made, X, Z, 2)) {
                continue;
            }
            String Facing = face(Dir + 2);
            for (int Y = R.F - BasinDepth; Y <= R.F; Y++) {
                G.set(X, Y, Z, Scp1730Blocks.Ladder + "[facing=" + Facing + ",waterlogged=" + (Y < R.F) + "]");
            }
            Made.add(new int[]{X, Z, Dir});
            B.fixture("ladder", X, R.F, Z, Facing, BasinDepth + 1, R.Zone);
            Since = 0;
        }
        return Made;
    }

    static void pumpIsland(Scp1730Builder B, Scp1730Rooms.Box R, int Cx, int Cz) {
        Scp1730Grid G = B.Grid;
        Random Rng = B.Rng;
        int F = R.F;
        for (int Ox = -1; Ox <= 1; Ox += 2) {
            for (int Oz = -1; Oz <= 1; Oz += 2) {
                int X = Cx + Ox * 2;
                int Z = Cz + Oz * 2;
                String Out = Rng.nextBoolean() ? Scp1730Builder.facingOf(Ox, 0) : Scp1730Builder.facingOf(0, Oz);
                G.set(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Generator, Out));
                G.set(X, F + 2, Z, Scp1730Blocks.facing(Scp1730Blocks.Generator, Out));
                for (int Level = 3; Level <= R.H; Level++) {
                    G.set(X, F + Level, Z, mod("hcz_pipe_manifold[facing=" + Out + ",part=" + (Level == 3 ? "bottom" : Level == R.H ? "top" : "middle") + "]"));
                }
                B.keepClear(X, F + 1, Z);
            }
        }
        G.set(Cx, F + 1, Cz - 2, mod("hcz_monitor_console[facing=north,part=single]"));
        for (int Dir = 0; Dir < 4; Dir++) {
            int X = Cx + Scp1730Layout.Dx[Dir] * 2;
            int Z = Cz + Scp1730Layout.Dz[Dir] * 2;
            if (G.empty(X, F + 2, Z) && Rng.nextBoolean()) {
                B.keepClear(X, F + 1, Z);
            }
        }
        for (int Level = 1; Level <= R.H; Level++) {
            G.set(Cx, F + Level, Cz, Level == 2 ? Scp1730Blocks.PipeValve + "[orientation=south_y,waterlogged=false]" : Scp1730Blocks.Pipe + "[axis=y,waterlogged=false]");
        }
        mount(G, Cx + 2, F + 2, Cz + 1, mod("containment_room_status_panel[facing=north,powered=false]"), "north");
        G.set(Cx - 1, F + 1, Cz - 1, mod("chemical_barrel[facing=north]"));
        B.decal(Cx + 1, F, Cz - 1, "up", Scp1730Builder.Oil, R.Zone);
        B.decal(Cx - 1, F, Cz + 1, "up", Scp1730Builder.Puddle, R.Zone);
    }

    static void wade(Scp1730Builder B, Scp1730Rooms.Box R, boolean[][] Basin, List<int[]> Landings) {
        Scp1730Grid G = B.Grid;
        Random Rng = B.Rng;
        List<Integer> Sides = new ArrayList<>(List.of(0, 1, 2, 3));
        B.shuffle(Sides);
        for (int Side : Sides) {
            boolean AlongX = Side % 2 == 0;
            int Lo = AlongX ? R.X0 + Walkway + 1 : R.Z0 + Walkway + 1;
            int Hi = AlongX ? R.X1 - Walkway - 1 : R.Z1 - Walkway - 1;
            List<int[]> Strip = new ArrayList<>();
            for (int T = Lo; T <= Hi; T++) {
                boolean Ok = true;
                for (int Depth = 0; Depth <= 1; Depth++) {
                    int X = AlongX ? T : Side == 3 ? R.X0 + Depth : R.X1 - Depth;
                    int Z = AlongX ? (Side == 0 ? R.Z0 + Depth : R.Z1 - Depth) : T;
                    if (nearDoor(R, X, Z, 3) || !G.empty(X, R.F + 1, Z) || !G.empty(X, R.F + 2, Z) || G.get(X, R.F, Z) == null || B.cleared(X, R.F + 1, Z)) {
                        Ok = false;
                    }
                }
                if (Ok) {
                    Strip.add(new int[]{T});
                } else if (Strip.size() >= 6) {
                    break;
                } else {
                    Strip.clear();
                }
            }
            if (Strip.size() < 6) {
                continue;
            }
            int Length = Math.min(Strip.size(), 6 + Rng.nextInt(5));
            for (int Index = 0; Index < Length; Index++) {
                int T = Strip.get(Index)[0];
                for (int Depth = 0; Depth <= 1; Depth++) {
                    int X = AlongX ? T : Side == 3 ? R.X0 + Depth : R.X1 - Depth;
                    int Z = AlongX ? (Side == 0 ? R.Z0 + Depth : R.Z1 - Depth) : T;
                    G.set(X, R.F - 1, Z, Scp1730Blocks.Hcz.Floor);
                    G.set(X, R.F, Z, Scp1730Blocks.Water);
                    B.keepClear(X, R.F + 1, Z);
                    B.keepClear(X, R.F + 2, Z);
                }
            }
            int First = Strip.getFirst()[0];
            int[] Start = AlongX ? new int[]{First, Side == 0 ? R.Z0 : R.Z1} : new int[]{Side == 3 ? R.X0 : R.X1, First};
            B.fixture("wade", Start[0], R.F, Start[1], face(Side + 2), Length, R.Zone);
            return;
        }
    }

    static void wallPipes(Scp1730Builder B, Scp1730Rooms.Box R) {
        Scp1730Grid G = B.Grid;
        int Y = R.F + R.H - 2;
        for (int X = R.X0; X <= R.X1; X++) {
            for (int Z : new int[]{R.Z0, R.Z1}) {
                String Out = Z == R.Z0 ? "south" : "north";
                if (G.empty(X, Y, Z) && !B.cleared(X, Y, Z)) {
                    mount(G, X, Y, Z, Scp1730Blocks.WallPipes + "[facing=" + Out + "]", Out);
                }
            }
        }
        for (int Z = R.Z0 + 1; Z <= R.Z1 - 1; Z++) {
            for (int X : new int[]{R.X0, R.X1}) {
                String Out = X == R.X0 ? "east" : "west";
                if (G.empty(X, Y, Z) && !B.cleared(X, Y, Z)) {
                    mount(G, X, Y, Z, Scp1730Blocks.WallPipes + "[facing=" + Out + "]", Out);
                }
            }
        }
    }

    static boolean mount(Scp1730Grid G, int X, int Y, int Z, String State, String Facing) {
        int Dir = Scp1730Builder.dirOf(Facing);
        if (!Scp1730SupportCheck.solid(G.get(X - Scp1730Layout.Dx[Dir], Y, Z - Scp1730Layout.Dz[Dir]))) {
            return false;
        }
        G.set(X, Y, Z, State);
        return true;
    }

    static void posts(Scp1730Grid G, int X0, int Z0, int X1, int Z1, int Y0, int Y1) {
        for (int Y = Y0; Y <= Y1; Y++) {
            for (int X = X0; X <= X1; X++) {
                for (int Z = Z0; Z <= Z1; Z++) {
                    String State = G.get(X, Y, Z);
                    if (rail(State)) {
                        G.set(X, Y, Z, Scp1730SupportCheck.posted(G::get, X, Y, Z, State));
                    }
                }
            }
        }
    }

    static void settleScreens(Scp1730Builder B) {
        Scp1730Grid G = B.Grid;
        G.freeze();
        List<int[]> Found = new ArrayList<>();
        List<String> States = new ArrayList<>();
        G.paint(0, 0, Scp1730Plan.Width - 1, Scp1730Plan.Depth - 1, (X, Y, Z, State) -> {
            String Short = Scp1730RoomDressing.shortId(State);
            if (Scp1730SupportCheck.Screens.contains(Short) && !Scp1730SupportCheck.WallOnly.contains(Short) && !Short.equals("wall_monitor")) {
                Found.add(new int[]{X, Y, Z});
                States.add(State);
            }
        });
        for (int Index = 0; Index < Found.size(); Index++) {
            int[] At = Found.get(Index);
            String State = States.get(Index);
            String Facing = Scp1730RoomDressing.prop(State, "facing");
            if (Facing == null || !G.get(At[0], At[1], At[2]).equals(State)) {
                continue;
            }
            int Dir = Scp1730Builder.dirOf(Facing);
            if (Scp1730SupportCheck.facesOpen(G.get(At[0] + Scp1730Layout.Dx[Dir], At[1], At[2] + Scp1730Layout.Dz[Dir]))) {
                continue;
            }
            int Best = -1;
            for (int Turn = 1; Turn < 4; Turn++) {
                int Way = (Dir + (Turn == 1 ? 2 : Turn == 2 ? 1 : 3)) % 4;
                if (!Scp1730SupportCheck.facesOpen(G.get(At[0] + Scp1730Layout.Dx[Way], At[1], At[2] + Scp1730Layout.Dz[Way]))) {
                    continue;
                }
                boolean Backed = !Scp1730SupportCheck.facesOpen(G.get(At[0] - Scp1730Layout.Dx[Way], At[1], At[2] - Scp1730Layout.Dz[Way]));
                if (Best < 0 || Backed) {
                    Best = Way;
                }
                if (Backed) {
                    break;
                }
            }
            G.set(At[0], At[1], At[2], Best < 0 ? null : Scp1730RoomDressing.with(State, "facing", face(Best)));
        }
    }

    static boolean rail(String State) {
        return State != null && (State.startsWith(Scp1730Blocks.Railing + "[") || State.startsWith(Scp1730Blocks.RailingYellow + "["));
    }

    static void clearance(Scp1730Builder B, int Zone) {
        Scp1730Layout Plan = B.Layouts[Zone];
        boolean Wing = false;
        for (Scp1730Layout.Room Each : Plan.Rooms) {
            Wing |= Each.Type.equals(BioWing);
        }
        if (!Wing) {
            return;
        }
        List<String> Types = new ArrayList<>(ClearanceRooms.get(Math.min(3, Zone)));
        for (Scp1730Layout.Room Each : Plan.Rooms) {
            if (!Types.contains(Each.Type) && !Each.Type.startsWith("stair") && !sector(Each.Type) && !Each.Type.equals("sanctum") && !Each.Type.equals("pit")
                    && !Each.Type.equals("lobby") && !Each.Type.equals("tesla") && !Scp1730AnomalousRooms.anomalous(Each.Type)) {
                Types.add(Each.Type);
            }
        }
        for (String Type : Types) {
            List<Scp1730Layout.Room> Options = new ArrayList<>();
            for (Scp1730Layout.Room Each : Plan.Rooms) {
                if (Each.Type.equals(Type)) {
                    Options.add(Each);
                }
            }
            B.shuffle(Options);
            for (Scp1730Layout.Room Each : Options) {
                if (card(B, B.Rooms.new Box(Each, Zone))) {
                    return;
                }
            }
        }
    }

    static boolean card(Scp1730Builder B, Scp1730Rooms.Box R) {
        Scp1730Grid G = B.Grid;
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        for (int[] Cell : Cells) {
            int X = Cell[0];
            int Z = Cell[1];
            int Ix = X + Scp1730Layout.Dx[Cell[2]];
            int Iz = Z + Scp1730Layout.Dz[Cell[2]];
            String Out = face(Cell[2]);
            String Desk = mod("metal_desk[facing=" + Out + ",left=false,right=false]");
            if (!R.free(X, R.F + 1, Z) || !R.free(X, R.F + 2, Z) || !G.empty(Ix, R.F + 1, Iz) || !G.empty(Ix, R.F + 2, Iz) || !B.standable(Ix, R.F, Iz)
                    || !B.standable(X, R.F, Z) || nearDoor(R, X, Z, 1)) {
                continue;
            }
            G.set(X, R.F + 1, Z, Desk);
            if (!Scp1730RoomDressing.joined(G::get, X, R.F + 1, Z, Desk).equals(Desk)) {
                G.set(X, R.F + 1, Z, null);
                continue;
            }
            G.set(X, R.F + 2, Z, Clearance + "[facing=" + Out + "]");
            B.fixture("card", X, R.F + 2, Z, Out, ClearanceCard, R.Zone);
            B.keepClear(Ix, R.F + 1, Iz);
            B.keepClear(Ix, R.F + 2, Iz);
            return true;
        }
        return false;
    }
}
