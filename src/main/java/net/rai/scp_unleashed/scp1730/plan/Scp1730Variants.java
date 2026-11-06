package net.rai.scp_unleashed.scp1730.plan;

import java.util.List;
import java.util.Random;

public final class Scp1730Variants {
    public static final List<String> Rooms = List.of("records", "conference", "comms", "decon", "storage_bay", "cooling", "incinerator", "catwalk_room", "pump_room");
    public static final int DropDepth = 4;

    private Scp1730Variants() {
    }

    static String mod(String Name) {
        return Scp1730Blocks.Mod + Name;
    }

    static String face(int Dir) {
        return Scp1730Layout.Facings[(Dir % 4 + 4) % 4];
    }

    public static Scp1730Blocks.Palette palette(String Type, int Zone, Scp1730Blocks.Palette Fallback) {
        return switch (Type) {
            case Scp1730Sectors.BioWing -> Scp1730Blocks.Containment;
            case Scp1730Sectors.Reservoir, "incinerator", "catwalk_room" -> Scp1730Blocks.Hcz;
            case "records", "conference" -> Scp1730Blocks.Office;
            case "comms", "cooling", "pump_room" -> Scp1730Blocks.Power;
            case "decon" -> Scp1730Blocks.Medical;
            case "storage_bay" -> Scp1730Blocks.Lcz;
            default -> Fallback;
        };
    }

    static boolean furnish(Scp1730Builder B, Scp1730Rooms.Box R) {
        switch (R.Room.Type) {
            case "records" -> records(B, R);
            case "conference" -> conference(B, R);
            case "comms" -> comms(B, R);
            case "decon" -> decon(B, R);
            case "storage_bay" -> storageBay(B, R);
            case "cooling" -> cooling(B, R);
            case "incinerator" -> incinerator(B, R);
            case "catwalk_room" -> catwalkRoom(B, R);
            case "pump_room" -> pumpRoom(B, R);
            default -> {
                return false;
            }
        }
        return true;
    }

    static boolean alongX(Scp1730Rooms.Box R) {
        return R.X1 - R.X0 >= R.Z1 - R.Z0;
    }

    static void records(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("filing_cabinet"), 0);
        int Towers = 0;
        for (int[] Cell : R.wallCells()) {
            String Out = face(Cell[2]);
            int Along = Cell[2] % 2 == 0 ? Cell[0] : Cell[1];
            if (Along % 2 == 0 && Towers < 8 && Scp1730RoomDressing.tower(B, R, Cell[0], Cell[1], "office_records_cabinet", Out)) {
                Towers++;
            } else if (Along % 2 == 1) {
                R.put(Cell[0], R.F + 1, Cell[1], mod("binder_shelf[facing=" + Out + "]"));
            }
        }
        boolean AlongX = alongX(R);
        int Across0 = AlongX ? R.Z0 : R.X0;
        int Across1 = AlongX ? R.Z1 : R.X1;
        int Along0 = AlongX ? R.X0 : R.Z0;
        int Along1 = AlongX ? R.X1 : R.Z1;
        for (int Row = Along0 + 3; Row <= Along1 - 3; Row += 3) {
            for (int Across = Across0 + 2; Across <= Across1 - 2; Across++) {
                if (Across == (Across0 + Across1) / 2 && Rng.nextBoolean()) {
                    continue;
                }
                int X = AlongX ? Row : Across;
                int Z = AlongX ? Across : Row;
                String Facing = AlongX ? (Row % 2 == 0 ? "east" : "west") : (Row % 2 == 0 ? "south" : "north");
                if (Rng.nextInt(9) == 0) {
                    R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.FallenPanel, Facing));
                    continue;
                }
                if (R.put(X, R.F + 1, Z, mod("steel_shelf[facing=" + Facing + "]"))) {
                    R.put(X, R.F + 2, Z, Rng.nextInt(3) == 0 ? mod("paper_stack[facing=" + Facing + ",variant=" + Rng.nextInt(6) + "]") : mod("archive_boxes[facing=" + Facing + "]"));
                }
            }
        }
        int[] Back = Scp1730RoomDressing.farWall(R);
        int Dx = Scp1730Layout.Dx[Back[2]];
        int Dz = Scp1730Layout.Dz[Back[2]];
        String Toward = face(Back[2]);
        if (R.put(Back[0] + Dx, R.F + 1, Back[1] + Dz, mod("office_desk[facing=" + Toward + ",left=false,right=false]"))) {
            R.put(Back[0] + Dx, R.F + 2, Back[1] + Dz, mod("desk_lamp[facing=" + Toward + ",lit=false]"));
            R.put(Back[0] + Dx * 2, R.F + 1, Back[1] + Dz * 2, mod("office_chair[facing=" + face(Back[2] + 2) + "]"));
        }
        for (int Index = 0; Index < 6; Index++) {
            int X = R.X0 + Rng.nextInt(R.X1 - R.X0 + 1);
            int Z = R.Z0 + Rng.nextInt(R.Z1 - R.Z0 + 1);
            R.put(X, R.F + 1, Z, mod("paper_stack[facing=" + face(Rng.nextInt(4)) + ",variant=" + Rng.nextInt(6) + "]"));
        }
        Scp1730RoomDressing.vignette(B, R);
        R.lights(5);
        R.mess(3);
    }

    static void conference(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        boolean AlongX = alongX(R);
        int Cx = R.cx();
        int Cz = R.cz();
        int Half = Math.max(1, ((AlongX ? R.X1 - R.X0 : R.Z1 - R.Z0) - 5) / 2);
        for (int Along = -Half; Along <= Half; Along++) {
            for (int Side = 0; Side <= 1; Side++) {
                int X = AlongX ? Cx + Along : Cx + Side;
                int Z = AlongX ? Cz + Side : Cz + Along;
                R.put(X, R.F + 1, Z, mod("conference_table[north=false,south=false,east=false,west=false]"));
                if (Rng.nextInt(4) == 0) {
                    R.put(X, R.F + 2, Z, mod("paper_stack[facing=" + face(Rng.nextInt(4)) + ",variant=" + Rng.nextInt(6) + "]"));
                }
            }
        }
        for (int Along = -Half; Along <= Half; Along++) {
            for (int Side = -1; Side <= 2; Side += 3) {
                int X = AlongX ? Cx + Along : Cx + Side;
                int Z = AlongX ? Cz + Side : Cz + Along;
                String Facing = AlongX ? (Side < 0 ? "south" : "north") : (Side < 0 ? "east" : "west");
                if (Rng.nextInt(4) == 0) {
                    R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Rng.nextInt(4))));
                } else if (Rng.nextInt(6) > 0) {
                    R.put(X, R.F + 1, Z, mod("office_chair[facing=" + Facing + "]"));
                }
            }
        }
        int[] Back = Scp1730RoomDressing.farWall(R);
        int Lx = Back[2] % 2 == 0 ? 1 : 0;
        int Lz = Back[2] % 2 == 0 ? 0 : 1;
        String Toward = face(Back[2]);
        for (int Side = -1; Side <= 1; Side += 2) {
            R.tall(Back[0] + Lx * Side * 2, Back[1] + Lz * Side * 2, mod("whiteboard"), "facing=" + Toward + ",drawing=" + Rng.nextInt(4));
        }
        int[] Screen = {Back[0], Back[1], Back[2]};
        if (Scp1730RoomDressing.backed(B, Screen, R.F + 2) && R.free(Screen[0], R.F + 1, Screen[1])) {
            R.put(Screen[0], R.F + 2, Screen[1], mod("wall_monitor[facing=" + Toward + ",hanging=false,lit=false,screen=" + Rng.nextInt(10) + "]"));
        }
        int Small = 0;
        for (int[] Cell : R.wallCells()) {
            if (Small >= 4) {
                break;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Small) {
                case 0 -> R.put(Cell[0], R.F + 1, Cell[1], mod("potted_plant[facing=" + Out + "]"));
                case 1 -> R.tall(Cell[0], Cell[1], mod("water_cooler"), "facing=" + Out);
                case 2 -> R.tall(Cell[0], Cell[1], mod("coat_rack"), "facing=" + Out);
                default -> R.put(Cell[0], R.F + 1, Cell[1], mod("potted_plant[facing=" + Out + "]"));
            };
            if (Done) {
                Small++;
            }
        }
        for (int[] Cell : R.wallCells()) {
            if (R.free(Cell[0], R.F + 1, Cell[1]) && R.free(Cell[0], R.F + 2, Cell[1]) && Rng.nextInt(5) == 0) {
                Scp1730RoomDressing.wallProp(B, R, Cell, Rng.nextBoolean() ? "wall_clock" : "office_wall_calendar");
            }
        }
        R.wallContainer(mod("filing_cabinet"), 0);
        Scp1730RoomDressing.vignette(B, R);
        R.lights(4);
        R.mess(2);
    }

    static void comms(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.breach(1, false, false);
        int[] Back = Scp1730RoomDressing.farWall(R);
        for (int Side = 0; Side < 4; Side++) {
            if (Scp1730RoomDressing.doorOn(R, (Side + 2) % 4)) {
                continue;
            }
            List<int[]> Run = Scp1730RoomDressing.wall(R, Side);
            for (int Index = 1; Index < Run.size() - 1; Index++) {
                int[] Cell = Run.get(Index);
                if (Index % 4 == 0) {
                    continue;
                }
                if (R.tall(Cell[0], Cell[1], mod("server_rack"), "facing=" + face(Cell[2])) && R.H >= 3 && B.Grid.empty(Cell[0], R.F + R.H, Cell[1])) {
                    B.Grid.set(Cell[0], R.F + R.H, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.CableTray, "down"));
                }
            }
        }
        int Dx = Scp1730Layout.Dx[Back[2]];
        int Dz = Scp1730Layout.Dz[Back[2]];
        int Lx = Back[2] % 2 == 0 ? 1 : 0;
        int Lz = Back[2] % 2 == 0 ? 0 : 1;
        String Toward = face(Back[2] + 2);
        int Mx = R.cx();
        int Mz = R.cz();
        for (int Side = -1; Side <= 1; Side++) {
            int X = Mx + Lx * Side;
            int Z = Mz + Lz * Side;
            if (R.put(X, R.F + 1, Z, mod("metal_desk[facing=" + Toward + ",left=" + (Side > -1) + ",right=" + (Side < 1) + "]"))) {
                R.put(X, R.F + 2, Z, Side == 0 ? mod("computer_terminal[facing=" + Toward + ",lit=" + Rng.nextBoolean() + ",screen=" + Rng.nextInt(10) + "]")
                        : Rng.nextBoolean() ? Scp1730Blocks.facing(Scp1730Blocks.CrackedTerminal, Toward) : mod("crt_monitor[facing=" + Toward + ",lit=false,screen=" + Rng.nextInt(10) + "]"));
            }
            R.put(X - Dx, R.F + 1, Z - Dz, Rng.nextInt(3) == 0 ? Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, face(Rng.nextInt(4))) : mod("office_chair[facing=" + face(Back[2]) + "]"));
        }
        for (int Index = 0; Index < 4; Index++) {
            int X = R.X0 + 1 + Rng.nextInt(Math.max(1, R.X1 - R.X0 - 1));
            int Z = R.Z0 + 1 + Rng.nextInt(Math.max(1, R.Z1 - R.Z0 - 1));
            R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.CableTangle, face(Rng.nextInt(4))));
        }
        R.stack(Back[0], Back[1], mod("hcz_monitor_console"), face(Back[2]), 1);
        R.lights(5);
        R.mess(3);
    }

    static void decon(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        for (int X = R.X0; X <= R.X1; X++) {
            for (int Z = R.Z0; Z <= R.Z1; Z++) {
                boolean Grid = (X - R.X0) % 3 == 1 && (Z - R.Z0) % 3 == 1;
                B.Grid.set(X, R.F, Z, Grid ? Scp1730Blocks.Drain : Scp1730Blocks.ShowerTile);
                if (Grid) {
                    Rng.nextInt(4);
                }
            }
        }
        for (int[] Cell : R.wallCells()) {
            int Along = Cell[2] % 2 == 0 ? Cell[0] - R.X0 : Cell[1] - R.Z0;
            if (Along % 3 == 1 && B.Grid.empty(Cell[0], R.F + R.H, Cell[1])) {
                Scp1730Sectors.mount(B.Grid, Cell[0], R.F + R.H, Cell[1], mod("containment_room_sedation_vent[facing=" + face(Cell[2]) + "]"), face(Cell[2]));
            }
        }
        for (Scp1730Layout.Door Each : R.Room.Doors) {
            int Ix = -Scp1730Layout.Dx[Each.Dir];
            int Iz = -Scp1730Layout.Dz[Each.Dir];
            int Lx = Each.Dir % 2 == 0 ? 1 : 0;
            int Lz = Each.Dir % 2 == 0 ? 0 : 1;
            int X = R.Room.doorX(Each) + Ix;
            int Z = R.Room.doorZ(Each) + Iz;
            for (int Side = -1; Side <= 1; Side++) {
                B.Grid.set(X + Lx * Side, R.F, Z + Lz * Side, R.Look.marked(face(Each.Dir + 2)));
            }
            int Sx = X + Lx * 2;
            int Sz = Z + Lz * 2;
            if (R.free(Sx, R.F + 3, Sz) && Scp1730Builder.isWallLike(B.Grid.get(Sx - Ix, R.F + 3, Sz - Iz)) || R.free(Sx, R.F + 3, Sz) && Scp1730Builder.backed(B.Grid.get(Sx - Ix, R.F + 3, Sz - Iz))) {
                B.Grid.set(Sx, R.F + 3, Sz, mod("facility_sign_decontamination_small[facing=" + face(Each.Dir + 2) + "]"));
            }
        }
        int Lockers = 0;
        for (int[] Cell : R.wallCells()) {
            String Out = face(Cell[2]);
            int Along = Cell[2] % 2 == 0 ? Cell[0] : Cell[1];
            if (Along % 3 == 0 && Lockers < 6 && R.tall(Cell[0], Cell[1], mod("locker"), "facing=" + Out + ",open=" + (Rng.nextInt(3) == 0))) {
                Lockers++;
            } else if (Along % 3 == 1 && Rng.nextInt(3) == 0) {
                R.put(Cell[0], R.F + 1, Cell[1], mod(Rng.nextBoolean() ? "biohazard_bin[facing=" + Out + "]" : "medical_scrub_sink_bottom[facing=" + Out + "]"));
            }
        }
        R.wallContainer(mod("medical_cabinet"), 1);
        for (int Index = 0; Index < 4; Index++) {
            B.decal(R.X0 + Rng.nextInt(R.X1 - R.X0 + 1), R.F, R.Z0 + Rng.nextInt(R.Z1 - R.Z0 + 1), "up", Scp1730Builder.Puddle, R.Zone);
        }
        R.put(R.cx(), R.F + 1, R.cz(), Scp1730Blocks.facing(Scp1730Blocks.BodyBag, face(Rng.nextInt(4))));
        R.lights(4);
        R.mess(2);
    }

    static void storageBay(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        R.wallContainer(mod("supply_crate"), 1);
        R.wallContainer(mod("supply_crate"), 0);
        boolean AlongX = alongX(R);
        int Across0 = AlongX ? R.Z0 : R.X0;
        int Across1 = AlongX ? R.Z1 : R.X1;
        int Along0 = AlongX ? R.X0 : R.Z0;
        int Along1 = AlongX ? R.X1 : R.Z1;
        for (int Row = Across0 + 2; Row <= Across1 - 2; Row += 4) {
            for (int Along = Along0 + 2; Along <= Along1 - 2; Along++) {
                if ((Along - Along0) % 7 == 6) {
                    continue;
                }
                int X = AlongX ? Along : Row;
                int Z = AlongX ? Row : Along;
                String Facing = AlongX ? "south" : "east";
                if (Rng.nextInt(10) == 0) {
                    R.put(X, R.F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Rubble, Facing));
                    continue;
                }
                if (R.put(X, R.F + 1, Z, mod("steel_shelf[facing=" + Facing + "]"))) {
                    String[] Tops = {"supply_crate[facing=" + Facing + ",open=" + Rng.nextBoolean() + "]", "archive_boxes[facing=" + Facing + "]", "chemical_barrel[facing=" + Facing + "]"};
                    R.put(X, R.F + 2, Z, mod(Tops[Rng.nextInt(Tops.length)]));
                }
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        for (int Index = 0; Index < 8 && Index < Cells.size(); Index++) {
            int[] Cell = Cells.get(Index);
            String Out = face(Cell[2]);
            if (Index % 2 == 0) {
                R.stack(Cell[0], Cell[1], mod("hcz_hazard_crate"), Out, 1 + Rng.nextInt(Math.min(3, R.H)));
            } else {
                R.put(Cell[0], R.F + 1, Cell[1], mod("chemical_barrel[facing=" + Out + "]"));
            }
        }
        Scp1730Layout.Door Main = R.door();
        int Ix = -Scp1730Layout.Dx[Main.Dir];
        int Iz = -Scp1730Layout.Dz[Main.Dir];
        int Lx = Main.Dir % 2 == 0 ? 1 : 0;
        int Lz = Main.Dir % 2 == 0 ? 0 : 1;
        for (int Depth = 1; Depth <= 2; Depth++) {
            for (int Side = -2; Side <= 2; Side++) {
                int X = R.Room.doorX(Main) + Ix * Depth + Lx * Side;
                int Z = R.Room.doorZ(Main) + Iz * Depth + Lz * Side;
                if (R.Look.Floor.equals(B.Grid.get(X, R.F, Z))) {
                    B.Grid.set(X, R.F, Z, R.Look.marked(face(Main.Dir + 2)));
                }
            }
        }
        R.lights(6);
        R.mess(5);
    }

    static void cooling(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        boolean AlongX = alongX(R);
        int Across0 = AlongX ? R.Z0 : R.X0;
        int Across1 = AlongX ? R.Z1 : R.X1;
        int Along0 = AlongX ? R.X0 : R.Z0;
        int Along1 = AlongX ? R.X1 : R.Z1;
        int Mid = (Across0 + Across1) / 2;
        for (int Row : new int[]{Mid - 2, Mid + 2}) {
            if (Row <= Across0 || Row >= Across1) {
                continue;
            }
            String Axis = AlongX ? "x" : "z";
            List<Integer> Columns = new java.util.ArrayList<>();
            for (int Along = Along0 + 2; Along <= Along1 - 2; Along++) {
                int X = AlongX ? Along : Row;
                int Z = AlongX ? Row : Along;
                if ((Along - Along0) % 3 == 2) {
                    String Facing = AlongX ? (Row < Mid ? "south" : "north") : (Row < Mid ? "east" : "west");
                    if (R.stack(X, Z, mod("hcz_pipe_manifold"), Facing, R.H)) {
                        Columns.add(Along);
                        continue;
                    }
                }
                Rng.nextInt(3);
            }
            for (int Index = 0; Index + 1 < Columns.size(); Index++) {
                for (int Level : new int[]{R.H, R.H - 1}) {
                    boolean Free = true;
                    for (int Along = Columns.get(Index) + 1; Along < Columns.get(Index + 1); Along++) {
                        Free &= B.Grid.empty(AlongX ? Along : Row, R.F + Level, AlongX ? Row : Along);
                    }
                    if (!Free || Level == R.H - 1 && (Columns.get(Index) / 3) % 2 == 0) {
                        continue;
                    }
                    for (int Along = Columns.get(Index) + 1; Along < Columns.get(Index + 1); Along++) {
                        B.Grid.set(AlongX ? Along : Row, R.F + Level, AlongX ? Row : Along, Scp1730Blocks.Pipe + "[axis=" + Axis + ",waterlogged=false]");
                    }
                }
            }
        }
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= 8) {
                break;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Count % 4) {
                case 0 -> R.stack(Cell[0], Cell[1], mod("hcz_steam_vent"), Out, 2);
                case 1 -> R.put(Cell[0], R.F + 2, Cell[1], mod("hcz_valve_panel[facing=" + Out + "]"));
                case 2 -> R.put(Cell[0], R.F + 2, Cell[1], mod("containment_room_status_panel[facing=" + Out + ",powered=" + Rng.nextBoolean() + "]"));
                default -> R.stack(Cell[0], Cell[1], mod("hcz_pipe_manifold"), Out, R.H);
            };
            if (Done) {
                Count++;
            }
        }
        for (int Index = 0; Index < 8; Index++) {
            B.decal(R.X0 + Rng.nextInt(R.X1 - R.X0 + 1), R.F, R.Z0 + Rng.nextInt(R.Z1 - R.Z0 + 1), "up", B.pick(new int[]{Scp1730Builder.Puddle, Scp1730Builder.Corrosion, Scp1730Builder.Rust}), R.Zone);
        }
        int[] Back = Scp1730RoomDressing.farWall(R);
        R.stack(Back[0], Back[1], mod("hcz_monitor_console"), face(Back[2]), 1);
        for (int Index = 0; Index < 2; Index++) {
            int[] Cell = Cells.get(Rng.nextInt(Cells.size()));
            R.put(Cell[0], R.F + 3, Cell[1], mod("hcz_caged_lamp[facing=" + face(Cell[2]) + ",part=single]"));
        }
        R.wallContainer(mod("supply_crate"), 1);
        R.lights(6);
        R.mess(3);
    }

    static void incinerator(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        Scp1730Grid G = B.Grid;
        int[] Back = Scp1730RoomDressing.farWall(R);
        int Into = Back[2];
        int Dx = Scp1730Layout.Dx[Into];
        int Dz = Scp1730Layout.Dz[Into];
        int Lx = Into % 2 == 0 ? 1 : 0;
        int Lz = Into % 2 == 0 ? 0 : 1;
        String Toward = face(Into);
        int Span0 = Lx != 0 ? R.X0 : R.Z0;
        int Span1 = Lx != 0 ? R.X1 : R.Z1;
        int LineX = Back[0] + Dx * 3;
        int LineZ = Back[1] + Dz * 3;
        int Centre = Lx != 0 ? Back[0] : Back[1];
        for (int Along = Span0; Along <= Span1; Along++) {
            int X = Lx != 0 ? Along : LineX;
            int Z = Lx != 0 ? LineZ : Along;
            boolean Hatch = Along == Centre;
            boolean Window = Math.abs(Along - Centre) >= 2 && Along > Span0 && Along < Span1;
            for (int Level = 1; Level <= R.H; Level++) {
                String State = R.Look.wall(Level, R.H);
                if (Hatch && Level <= 2) {
                    State = Scp1730Blocks.facing(Scp1730Blocks.HeavyDoor, Toward);
                } else if (Window && Level == 2) {
                    State = Scp1730Blocks.Glass;
                }
                G.set(X, R.F + Level, Z, State);
            }
            for (int Depth = 0; Depth <= 2; Depth++) {
                int Cx = Lx != 0 ? Along : Back[0] + Dx * Depth;
                int Cz = Lx != 0 ? Back[1] + Dz * Depth : Along;
                G.set(Cx, R.F, Cz, Depth == 1 && Along % 2 == 0 ? Scp1730Blocks.Grate : "minecraft:magma_block");
                B.keepClear(Cx, R.F + 1, Cz);
                B.keepClear(Cx, R.F + 2, Cz);
                B.decal(Cx, R.F, Cz, "up", Scp1730Builder.Scorch, R.Zone);
            }
            int Wx = Lx != 0 ? Along : Back[0] - Dx;
            int Wz = Lx != 0 ? Back[1] - Dz : Along;
            if (Math.abs(Along - Centre) <= 1 && Scp1730Builder.isWallLike(G.get(Wx, R.F + 2, Wz))) {
                G.set(Wx, R.F + 2, Wz, Scp1730Blocks.RedGlow);
            }
        }
        for (int Level = R.H; Level >= R.H - 1; Level--) {
            int X = Back[0] + Dx + Lx * 2;
            int Z = Back[1] + Dz + Lz * 2;
            if (G.empty(X, R.F + Level, Z)) {
                G.set(X, R.F + Level, Z, Scp1730Blocks.Chain);
            }
        }
        G.set(Back[0] + Dx - Lx * 2, R.F + 1, Back[1] + Dz - Lz * 2, Scp1730Blocks.facing(Scp1730Blocks.BodyBag, face(Into + 1)));
        int Fx = LineX + Dx;
        int Fz = LineZ + Dz;
        for (int Side = -1; Side <= 1; Side += 2) {
            R.put(Fx + Lx * Side * 3, R.F + 1, Fz + Lz * Side * 3, mod("biohazard_bin[facing=" + Toward + "]"));
        }
        R.put(Fx + Lx * 2, R.F + 1, Fz + Lz * 2, Scp1730Blocks.facing(Scp1730Blocks.Gurney, face(Into + 1)));
        R.stack(Fx - Lx * 2, Fz - Lz * 2, mod("hcz_monitor_console"), face(Into), 1);
        for (int Mark = 0; Mark < 6; Mark++) {
            B.decal(Fx + Lx * (Rng.nextInt(7) - 3) + Dx * Rng.nextInt(3), R.F, Fz + Lz * (Rng.nextInt(7) - 3) + Dz * Rng.nextInt(3), "up",
                    B.pick(new int[]{Scp1730Builder.Scorch, Scp1730Builder.Oil, Scp1730Builder.Grime}), R.Zone);
        }
        for (int[] Cell : R.wallCells()) {
            int Depth = (Cell[0] - Back[0]) * Dx + (Cell[1] - Back[1]) * Dz;
            if (Depth > 4 && Rng.nextInt(4) == 0) {
                R.put(Cell[0], R.F + 1, Cell[1], mod("chemical_barrel[facing=" + face(Cell[2]) + "]"));
            }
        }
        int[] Sign = {LineX + Dx + Lx * (Centre == Span0 ? 1 : -1), LineZ + Dz + Lz * (Centre == Span0 ? 1 : -1)};
        if (G.empty(Sign[0], R.F + 3, Sign[1])) {
            G.set(Sign[0], R.F + 3, Sign[1], Scp1730Blocks.facing(Scp1730Blocks.SignVoltage, face(Into)));
        }
        R.lights(5);
        R.mess(3);
    }

    static void catwalkRoom(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        Scp1730Grid G = B.Grid;
        int Ledge = 2;
        int Px0 = R.X0 + Ledge;
        int Pz0 = R.Z0 + Ledge;
        int Px1 = R.X1 - Ledge;
        int Pz1 = R.Z1 - Ledge;
        if (Px1 - Px0 < 4 || Pz1 - Pz0 < 4) {
            Scp1730RoomDressing.office(B, R);
            return;
        }
        int Cx = R.cx();
        int Cz = R.cz();
        int Bottom = R.F - DropDepth - 1;
        boolean CollapseX = Rng.nextBoolean();
        for (int X = Px0 - 1; X <= Px1 + 1; X++) {
            for (int Z = Pz0 - 1; Z <= Pz1 + 1; Z++) {
                boolean Pit = X >= Px0 && X <= Px1 && Z >= Pz0 && Z <= Pz1;
                if (!Pit) {
                    for (int Y = Bottom; Y < R.F; Y++) {
                        G.set(X, Y, Z, Scp1730Blocks.Hcz.WallMiddle);
                    }
                    continue;
                }
                G.set(X, R.F, Z, null);
                G.set(X, Bottom, Z, Rng.nextInt(5) == 0 ? Scp1730Blocks.RubbleFloor : Scp1730Blocks.Hcz.Floor);
                B.keepClear(X, R.F + 1, Z);
                B.keepClear(X, R.F + 2, Z);
            }
        }
        for (int Axis = 0; Axis < 2; Axis++) {
            boolean OnX = Axis == 0;
            boolean Broken = OnX == CollapseX;
            int Lo = OnX ? Px0 : Pz0;
            int Hi = OnX ? Px1 : Pz1;
            int Gap = (Lo + Hi) / 2 + (Rng.nextBoolean() ? 1 : -1) * (2 + Rng.nextInt(2));
            for (int Along = Lo; Along <= Hi; Along++) {
                boolean Hole = Broken && (Along == Gap || Along == Gap + 1);
                for (int Lane = -1; Lane <= 1; Lane++) {
                    int X = OnX ? Along : Cx + Lane;
                    int Z = OnX ? Cz + Lane : Along;
                    if (Hole) {
                        continue;
                    }
                    if (Lane == 0) {
                        G.set(X, R.F, Z, Scp1730Blocks.Catwalk);
                        continue;
                    }
                    boolean Crossing = OnX ? Math.abs(Along - Cx) <= 1 : Math.abs(Along - Cz) <= 1;
                    if (Crossing) {
                        G.set(X, R.F, Z, Scp1730Blocks.Catwalk);
                    } else {
                        G.set(X, R.F, Z, Scp1730Sectors.railing(Scp1730Blocks.RailingYellow, true, Scp1730Builder.facingOf(OnX ? 0 : Lane, OnX ? Lane : 0)));
                    }
                }
                if (Hole && Along == Gap && G.empty(OnX ? Along : Cx, R.F + R.H, OnX ? Cz : Along)) {
                    G.set(OnX ? Along : Cx, R.F + R.H, OnX ? Cz : Along, Scp1730Blocks.facing(Scp1730Blocks.HangingCables, "down"));
                }
                if (Hole && Along == Gap) {
                    int X = OnX ? Along : Cx;
                    int Z = OnX ? Cz : Along;
                    G.set(X, Bottom + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.FallenPanel, OnX ? "east" : "south"));
                }
            }
        }
        int Ladders = 0;
        int[][] Spots = {{Px0, Pz0 + 1, 3}, {Px1, Pz1 - 1, 1}, {Px0 + 1, Pz1, 2}, {Px1 - 1, Pz0, 0}};
        for (int[] Spot : Spots) {
            int X = Spot[0];
            int Z = Spot[1];
            if (!G.empty(X, R.F, Z) || Math.abs(X - Cx) <= 1 || Math.abs(Z - Cz) <= 1) {
                continue;
            }
            String Facing = face(Spot[2] + 2);
            for (int Y = Bottom + 1; Y <= R.F; Y++) {
                G.set(X, Y, Z, Scp1730Blocks.Ladder + "[facing=" + Facing + ",waterlogged=false]");
            }
            B.fixture("ladder", X, R.F, Z, Facing, DropDepth + 1, R.Zone);
            Ladders++;
        }
        for (int Index = 0; Index < 8; Index++) {
            int X = Px0 + Rng.nextInt(Px1 - Px0 + 1);
            int Z = Pz0 + Rng.nextInt(Pz1 - Pz0 + 1);
            if (G.empty(X, Bottom + 1, Z) && G.empty(X, Bottom + 2, Z) && Math.abs(X - Cx) > 1 && Math.abs(Z - Cz) > 1) {
                String[] Junk = {Scp1730Blocks.Rubble, Scp1730Blocks.CableTangle, Scp1730Blocks.FallenPanel, Scp1730Blocks.BodyBag};
                G.set(X, Bottom + 1, Z, Scp1730Blocks.facing(Junk[Rng.nextInt(Junk.length)], face(Rng.nextInt(4))));
            }
        }
        for (int Ox = -1; Ox <= 1; Ox += 2) {
            for (int Oz = -1; Oz <= 1; Oz += 2) {
                int X = Cx + Ox * 3;
                int Z = Cz + Oz * 3;
                boolean Clear = true;
                for (int Y = Bottom + 1; Y <= R.F + R.H; Y++) {
                    Clear &= G.empty(X, Y, Z);
                }
                if (X > Px0 && X < Px1 && Z > Pz0 && Z < Pz1 && Clear) {
                    G.set(X, Bottom + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Generator, Scp1730Builder.facingOf(Ox, 0)));
                    G.set(X, Bottom + 2, Z, Scp1730Blocks.facing(Scp1730Blocks.Generator, Scp1730Builder.facingOf(Ox, 0)));
                    for (int Y = Bottom + 3; Y <= R.F + R.H; Y++) {
                        G.set(X, Y, Z, Scp1730Blocks.Pipe + "[axis=y,waterlogged=false]");
                    }
                }
            }
        }
        Scp1730Sectors.posts(G, Px0, Pz0, Px1, Pz1, R.F, R.F);
        B.Basins.add(new Scp1730Basin("drop", Px0, Pz0, Px1, Pz1, R.F - 1, Bottom, R.Zone));
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= 6) {
                break;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Count % 3) {
                case 0 -> R.stack(Cell[0], Cell[1], mod("hcz_hazard_crate"), Out, 1 + Rng.nextInt(2));
                case 1 -> R.put(Cell[0], R.F + 2, Cell[1], mod("hcz_valve_panel[facing=" + Out + "]"));
                default -> R.put(Cell[0], R.F + 1, Cell[1], mod("chemical_barrel[facing=" + Out + "]"));
            };
            if (Done) {
                Count++;
            }
        }
        for (int X = R.X0 + 2; X <= R.X1; X += 5) {
            for (int Z = R.Z0 + 2; Z <= R.Z1; Z += 5) {
                if (G.empty(X, R.F + R.H, Z) && G.get(X, R.F + R.H + 1, Z) != null) {
                    G.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Rng.nextInt(3) == 0 ? Scp1730Blocks.EmergencyLight : Scp1730Blocks.BrokenLight, "down"));
                }
            }
        }
        R.turret();
    }

    static void pumpRoom(Scp1730Builder B, Scp1730Rooms.Box R) {
        Random Rng = B.Rng;
        Scp1730Grid G = B.Grid;
        for (int X = R.X0 + 1; X <= R.X1 - 1; X++) {
            for (int Z = R.Z0 + 1; Z <= R.Z1 - 1; Z++) {
                if (Scp1730SupportCheck.LowProps.contains(Scp1730RoomDressing.shortId(String.valueOf(G.get(X, R.F + 1, Z))))) {
                    G.set(X, R.F + 1, Z, null);
                }
                G.set(X, R.F - 1, Z, Scp1730Blocks.Power.Floor);
                G.set(X, R.F, Z, Scp1730Blocks.Water);
                B.keepClear(X, R.F + 1, Z);
                B.keepClear(X, R.F + 2, Z);
            }
        }
        B.fixture("wade", R.X0 + 1, R.F, R.Z0 + 1, "north", (R.X1 - R.X0 - 1) * (R.Z1 - R.Z0 - 1), R.Zone);
        List<int[]> Cells = R.wallCells();
        B.shuffle(Cells);
        int Count = 0;
        for (int[] Cell : Cells) {
            if (Count >= 7) {
                break;
            }
            if (Scp1730Sectors.nearDoor(R, Cell[0], Cell[1], 1)) {
                continue;
            }
            String Out = face(Cell[2]);
            boolean Done = switch (Count % 4) {
                case 0 -> {
                    if (!R.free(Cell[0], R.F + 1, Cell[1]) || !R.free(Cell[0], R.F + 2, Cell[1])) {
                        yield false;
                    }
                    G.set(Cell[0], R.F + 1, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.Generator, Out));
                    G.set(Cell[0], R.F + 2, Cell[1], Scp1730Blocks.facing(Scp1730Blocks.Generator, Out));
                    for (int Level = 3; Level <= R.H; Level++) {
                        G.set(Cell[0], R.F + Level, Cell[1], mod("hcz_pipe_manifold[facing=" + Out + ",part=" + (Level == 3 ? "bottom" : Level == R.H ? "top" : "middle") + "]"));
                    }
                    yield true;
                }
                case 1 -> R.put(Cell[0], R.F + 2, Cell[1], mod("hcz_valve_panel[facing=" + Out + "]"));
                case 2 -> R.stack(Cell[0], Cell[1], mod("hcz_steam_vent"), Out, 2);
                default -> R.put(Cell[0], R.F + 1, Cell[1], mod("chemical_barrel[facing=" + Out + "]"));
            };
            if (Done) {
                Count++;
            }
        }
        boolean AlongX = alongX(R);
        int Line = AlongX ? R.cz() : R.cx();
        boolean Open = true;
        for (int Along = (AlongX ? R.X0 : R.Z0); Along <= (AlongX ? R.X1 : R.Z1); Along++) {
            Open &= G.empty(AlongX ? Along : Line, R.F + R.H, AlongX ? Line : Along);
        }
        for (int Along = (AlongX ? R.X0 : R.Z0); Along <= (AlongX ? R.X1 : R.Z1) && Open; Along++) {
            int X = AlongX ? Along : Line;
            int Z = AlongX ? Line : Along;
            G.set(X, R.F + R.H, Z, (Along % 5 == 2 ? Scp1730Blocks.PipeValve + "[orientation=down_" + (AlongX ? "x" : "z") : Scp1730Blocks.Pipe + "[axis=" + (AlongX ? "x" : "z"))
                    + ",waterlogged=false]");
        }
        for (int X = R.X0 + 2; X <= R.X1 - 1; X += 4) {
            for (int Z = R.Z0 + 2; Z <= R.Z1 - 1; Z += 4) {
                if (G.empty(X, R.F + R.H, Z) && G.get(X, R.F + R.H + 1, Z) != null) {
                    G.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.BrokenLight : Scp1730Blocks.DeadLight, "down"));
                }
            }
        }
        R.wallContainer(mod("supply_crate"), 1);
    }
}
