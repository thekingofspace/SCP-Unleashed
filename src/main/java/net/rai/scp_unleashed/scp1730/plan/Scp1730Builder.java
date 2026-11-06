package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public final class Scp1730Builder {
    public static final int[] Floors = {196, 184, 170, 126};
    public static final String[] Kinds = {"entrance", "light", "heavy", "deep"};
    public static final String[] Objectives = {"card", "release", "breaker", "thresher"};
    public static final int CorridorHeight = 3;
    public static final Set<String> Large = Set.of("server", "pit", "heavy_cell", "sanctum", "stair_exit", "stair_entry", Scp1730Sectors.BioWing, Scp1730Sectors.Reservoir);
    public static final int PitDepth = 14;
    public static final int ShaftHead = 4;
    public static final int ShaftWide = 5;
    public static final int ShaftFlight = 6;
    public static final int ShaftLong = 12;
    public static final int StainedLight = 4;
    public static final int StainedHeavy = 8;
    public static final int HoledLight = 2;
    public static final int HoledHeavy = 4;
    public static final String[] DecalKinds = {"blood_splatter", "scorch_marks", "bullet_holes", "cracks", "claw_marks", "footprints", "grime", "rust_streaks", "oil_stain", "puddle", "foundation_emblem",
            "corrosion", "hazard_sign", "zone_sign", "arrow_marker", "vent"};
    public static final int[] DecalVariants = {4, 3, 3, 3, 4, 3, 3, 3, 3, 3, 2, 4, 6, 5, 4, 4};
    public static final int Blood = 0;
    public static final int Scorch = 1;
    public static final int Bullets = 2;
    public static final int Cracks = 3;
    public static final int Claws = 4;
    public static final int Footprints = 5;
    public static final int Grime = 6;
    public static final int Rust = 7;
    public static final int Oil = 8;
    public static final int Puddle = 9;
    public static final int Emblem = 10;
    public static final int Corrosion = 11;
    public static final int HazardDecal = 12;
    public static final int ZoneDecal = 13;
    public static final int ArrowDecal = 14;
    public static final int VentDecal = 15;

    final Scp1730Grid Grid = new Scp1730Grid();
    final Random Rng;
    final long Seed;
    final List<Scp1730Fixture> Fixtures = new ArrayList<>();
    final List<Scp1730Pit> Pits = new ArrayList<>();
    final List<Scp1730Basin> Basins = new ArrayList<>();
    final List<int[]> HallVariants = new ArrayList<>();
    final List<int[]> SealedCells = new ArrayList<>();
    final List<Scp1730Zone> Zones = new ArrayList<>();
    final Scp1730Layout[] Layouts = new Scp1730Layout[4];
    final Scp1730Spot[] Entries = new Scp1730Spot[4];
    final int[] ZoneTop = new int[4];
    final int[] ZoneBottom = new int[4];
    final Set<Long> Clear = new HashSet<>();
    final List<int[]> CorridorWalls = new ArrayList<>();
    final List<int[]> CeilingSpots = new ArrayList<>();
    final List<int[]> ArmSpots = new ArrayList<>();
    final List<int[]> RoomWalls = new ArrayList<>();
    final List<int[]> TurretSpots = new ArrayList<>();
    final List<Scp1730RoomInfo> RoomInfos = new ArrayList<>();
    Scp1730Spot Arrival;
    Scp1730Rooms Rooms;

    public static final int Retries = 32;
    final boolean Fallback;

    Scp1730Builder(long Seed) {
        this(Seed, 0);
    }

    Scp1730Builder(long Seed, int Attempt) {
        this.Seed = Seed;
        this.SetPieces = new Scp1730SetPieces.Picker(Seed);
        this.Fallback = Attempt < 0;
        this.Rng = new Random(Attempt == 0 ? Seed ^ 0x5DEECE66DL * 1730L : mix(Seed, Attempt < 0 ? 0x1730 : Attempt));
    }

    static long mix(long Seed, int Attempt) {
        long Value = Seed + 0x9E3779B97F4A7C15L * (Attempt + 1L);
        Value = (Value ^ (Value >>> 30)) * 0xBF58476D1CE4E5B9L;
        Value = (Value ^ (Value >>> 27)) * 0x94D049BB133111EBL;
        return Value ^ (Value >>> 31);
    }

    static Scp1730Builder of(long Seed) {
        for (int Attempt = 0; Attempt <= Retries; Attempt++) {
            Scp1730Builder Made = new Scp1730Builder(Seed, Attempt);
            try {
                if (Made.layout()) {
                    Made.build();
                    return Made;
                }
            } catch (RuntimeException Ignored) {
            }
        }
        Scp1730Builder Made = new Scp1730Builder(Seed, -1);
        Made.layout();
        Made.build();
        return Made;
    }

    public static Scp1730Layout.Spec specFor(int Zone, String Type) {
        return switch (Type) {
            case "lobby" -> new Scp1730Layout.Spec(Type, 2, 2, 2, 3);
            case "checkpoint" -> new Scp1730Layout.Spec(Type, 1, 2, 2, 2);
            case "office" -> new Scp1730Layout.Spec(Type, 1, 2, 1, 2);
            case "small_office" -> new Scp1730Layout.Spec(Type, 1, 1, 1, 1);
            case "cafeteria" -> new Scp1730Layout.Spec(Type, 2, 2, 1, 2);
            case "elevator" -> new Scp1730Layout.Spec(Type, 1, 2, 1, 2);
            case "security" -> new Scp1730Layout.Spec(Type, 1, 1, 1, 1);
            case "breaker" -> new Scp1730Layout.Spec(Type, 1, 1, 1, 1);
            case "storage" -> new Scp1730Layout.Spec(Type, 1, 1, 1, 1);
            case "stair_exit" -> new Scp1730Layout.Spec(Type, 1, 2, 1, 1);
            case "stair_entry" -> new Scp1730Layout.Spec(Type, 1, 2, 1, 2);
            case "cell_block" -> new Scp1730Layout.Spec(Type, 2, 2, 2, 2);
            case "test_chamber" -> new Scp1730Layout.Spec(Type, 2, 2, 1, 1);
            case "medical" -> new Scp1730Layout.Spec(Type, 1, 2, 1, 2);
            case "dorms" -> new Scp1730Layout.Spec(Type, 2, 1, 1, 2);
            case "research" -> new Scp1730Layout.Spec(Type, 1, 1, 1, 1);
            case "heavy_cell" -> new Scp1730Layout.Spec(Type, 2, 2, 1, 1);
            case "tesla" -> new Scp1730Layout.Spec(Type, 1, 2, 2, 2);
            case "server" -> new Scp1730Layout.Spec(Type, 2, 2, 1, 2);
            case "generator" -> new Scp1730Layout.Spec(Type, 2, 2, 1, 1);
            case "armoury" -> new Scp1730Layout.Spec(Type, 1, 2, 1, 1);
            case "pit" -> new Scp1730Layout.Spec(Type, 2, 2, 2, 2);
            case "sanctum" -> new Scp1730Layout.Spec(Type, 3, 4, 1, 1);
            case "collapsed" -> new Scp1730Layout.Spec(Type, 1, 1, 1, 1);
            case "holding_cell" -> new Scp1730Layout.Spec(Type, 1, 1, 1, 1);
            case Scp1730Sectors.BioWing -> new Scp1730Layout.Spec(Type, 2, 2, 1, 1);
            case Scp1730Sectors.Reservoir -> new Scp1730Layout.Spec(Type, 2, 2, 2, 3);
            case "records", "storage_bay", "cooling" -> new Scp1730Layout.Spec(Type, 1, 2, 1, 2);
            case "decon" -> new Scp1730Layout.Spec(Type, 1, 1, 2, 2);
            case "catwalk_room" -> new Scp1730Layout.Spec(Type, 2, 2, 2, 3);
            case "pump_room" -> new Scp1730Layout.Spec(Type, 1, 1, 1, 2);
            default -> new Scp1730Layout.Spec(Type, 1, 1, 1, 1);
        };
    }

    public static int heightOf(String Type) {
        return switch (Type) {
            case "pit" -> 10;
            case "heavy_cell" -> 8;
            case "generator", "server", "stair_entry", "stair_exit" -> 5;
            case "cafeteria", "elevator", "cell_block", "test_chamber", "tesla" -> 4;
            case "sanctum" -> 24;
            case Scp1730Sectors.Reservoir -> Scp1730Sectors.ReservoirHeight;
            case Scp1730Sectors.BioWing -> Scp1730Sectors.WingHeight;
            case "catwalk_room" -> 6;
            case "cooling", "incinerator", "storage_bay", "pump_room", "comms" -> 4;
            default -> 3;
        };
    }

    Scp1730Layout.Spec sized(int Zone, String Type) {
        Scp1730Layout.Spec Base = specFor(Zone, Type);
        int W = Base.W();
        int D = Base.D();
        switch (Type) {
            case "cafeteria", "dorms" -> D = 1 + Rng.nextInt(2);
            case "office", "medical", "armoury" -> D = 1 + Rng.nextInt(2);
            case "server" -> D = 2 + Rng.nextInt(2);
            case "heavy_cell" -> D = 2 + Rng.nextInt(2);
            case "collapsed", "storage", "records", "cooling", "storage_bay" -> D = 1 + Rng.nextInt(2);
            case Scp1730Sectors.BioWing -> W = 2 + Rng.nextInt(2);
            default -> {
            }
        }
        return new Scp1730Layout.Spec(Type, W, D, Base.MinDoors(), Base.MaxDoors());
    }

    List<Scp1730Layout.Spec> required(int Zone) {
        List<Scp1730Layout.Spec> Made = new ArrayList<>();
        String[] Types = switch (Zone) {
            case 0 -> new String[]{"checkpoint", "security", "cafeteria", "office", "elevator", "breaker", "holding_cell", "holding_cell"};
            case 1 -> new String[]{"cell_block", "test_chamber", "medical", "dorms", "breaker", "holding_cell", "holding_cell", Scp1730Sectors.BioWing};
            case 2 -> new String[]{"heavy_cell", "tesla", "server", "generator", "armoury", "pit", "holding_cell", "holding_cell", Scp1730Sectors.Reservoir,
                    Scp1730Sectors.BioWing};
            default -> new String[]{"sanctum", "holding_cell", Scp1730Sectors.BioWing};
        };
        for (String Type : Types) {
            Made.add(sized(Zone, Type));
        }
        return Made;
    }

    List<Scp1730Layout.Spec> fillers(int Zone) {
        List<Scp1730Layout.Spec> Made = new ArrayList<>();
        String[] Pool = switch (Zone) {
            case 0 -> new String[]{"small_office", "storage", "small_office", "office", "cafeteria", "elevator", "small_office", "storage", "holding_cell", "records", "conference", "comms",
                    "records", "conference"};
            case 1 -> new String[]{"cell_block", "research", "storage", "research", "medical", "dorms", "cell_block", "small_office", "holding_cell", "decon", "storage_bay", "decon"};
            case 2 -> new String[]{"heavy_cell", "storage", "armoury", "server", "heavy_cell", "storage", "collapsed", "holding_cell", "cooling", "incinerator", "catwalk_room", "pump_room",
                    "cooling", "incinerator"};
            default -> new String[]{"collapsed", "collapsed", "collapsed", "holding_cell"};
        };
        int Count = Zone == 3 ? 1 + Rng.nextInt(3) : 4 + Rng.nextInt(3);
        List<String> Options = new ArrayList<>(List.of(Pool));
        shuffle(Options);
        for (int Index = 0; Index < Count && Index < Options.size(); Index++) {
            Made.add(sized(Zone, Options.get(Index)));
        }
        return Made;
    }

    boolean layout() {
        int[] EntryRect = null;
        for (int Zone = 0; Zone < 4; Zone++) {
            Scp1730Layout.Spec EntrySpec = Zone == 0 ? specFor(0, "lobby") : specFor(Zone, "stair_entry");
            Scp1730Layout.Spec ExitSpec = Zone < 3 ? specFor(Zone, "stair_exit") : null;
            Layouts[Zone] = Fallback ? Scp1730Layout.fixed(Zone, Rng, EntrySpec, ExitSpec)
                    : Scp1730Layout.build(Zone, Rng, EntryRect, required(Zone), fillers(Zone), EntrySpec, ExitSpec, Zone == 2 ? "pit" : null);
            if (Layouts[Zone] == null) {
                return false;
            }
            for (Scp1730Layout.Room Each : Layouts[Zone].Rooms) {
                Each.Floor = Floors[Zone];
                Each.Height = heightOf(Each.Type);
                insetRoom(Each);
            }
            if (Layouts[Zone].Exit != null) {
                Scp1730Layout.Room Exit = Layouts[Zone].Exit;
                EntryRect = new int[]{Exit.Sx, Exit.Sz, Exit.W, Exit.D};
            }
        }
        return true;
    }

    void build() {
        Scp1730AnomalousRooms.assign(this);
        Rooms = new Scp1730Rooms(this);
        for (int Zone = 0; Zone < 4; Zone++) {
            corridors(Zone);
            for (Scp1730Layout.Room Each : Layouts[Zone].Rooms) {
                if (!Each.Type.startsWith("stair")) {
                    shell(Each, Zone);
                }
            }
        }
        for (int Zone = 0; Zone < 3; Zone++) {
            stairwell(Zone, Layouts[Zone].Exit, Layouts[Zone + 1].Entry);
        }
        for (int Zone = 0; Zone < 4; Zone++) {
            for (Scp1730Layout.Room Each : Layouts[Zone].Rooms) {
                if (!Each.Type.startsWith("stair")) {
                    doorways(Each, Zone);
                }
            }
        }
        for (int Zone = 0; Zone < 4; Zone++) {
            chooseSafe(Zone);
        }
        for (int Zone = 0; Zone < 4; Zone++) {
            for (Scp1730Layout.Room Each : Layouts[Zone].Rooms) {
                if (!Each.Type.startsWith("stair")) {
                    Rooms.furnish(Each, Zone);
                }
            }
            Scp1730Sectors.clearance(this, Zone);
            dressCorridors(Zone);
            if (Zone <= 1) {
                decayCorridors(Zone, Zone == 1);
            }
            ensurePainter(Zone);
        }
        for (int Zone = 0; Zone < 4; Zone++) {
            for (Scp1730Layout.Room Each : Layouts[Zone].Rooms) {
                List<int[]> Doors = new ArrayList<>();
                for (Scp1730Layout.Door Door : Each.Doors) {
                    Doors.add(new int[]{Each.doorX(Door), Each.doorZ(Door), Door.Dir});
                }
                int Floor = Each.Type.equals("stair_entry") ? Floors[Zone] : Each.Floor;
                int Height = Each.Type.startsWith("stair") ? 5 : Each.Height;
                RoomInfos.add(new Scp1730RoomInfo(Each.Type, Zone, Each.Bx0, Each.Bz0, Each.Bx1, Each.Bz1, Floor, Height, List.copyOf(Doors)));
            }
        }
        for (int Zone = 0; Zone < 4; Zone++) {
            hazards(Zone);
            markers(Zone);
            decay(Zone);
            Scp1730Halls.dress(this, Zone);
        }
        Scp1730PuzzlePlan.place(this);
        for (int Zone = 0; Zone < 4; Zone++) {
            int MinX = Integer.MAX_VALUE;
            int MinZ = Integer.MAX_VALUE;
            int MaxX = Integer.MIN_VALUE;
            int MaxZ = Integer.MIN_VALUE;
            int Top = Floors[Zone] + CorridorHeight + 1;
            for (Scp1730Layout.Room Each : Layouts[Zone].Rooms) {
                MinX = Math.min(MinX, Each.x0());
                MinZ = Math.min(MinZ, Each.z0());
                MaxX = Math.max(MaxX, Each.x1());
                MaxZ = Math.max(MaxZ, Each.z1());
                if (!Each.Type.startsWith("stair")) {
                    Top = Math.max(Top, Each.Floor + Each.Height + 1);
                }
            }
            for (int Sx = 0; Sx < Scp1730Layout.Slots; Sx++) {
                for (int Sz = 0; Sz < Scp1730Layout.Slots; Sz++) {
                    if (Layouts[Zone].Corridor[Sx][Sz]) {
                        MinX = Math.min(MinX, Scp1730Layout.Origin + Sx * 16);
                        MinZ = Math.min(MinZ, Scp1730Layout.Origin + Sz * 16);
                        MaxX = Math.max(MaxX, Scp1730Layout.Origin + Sx * 16 + 15);
                        MaxZ = Math.max(MaxZ, Scp1730Layout.Origin + Sz * 16 + 15);
                    }
                }
            }
            ZoneTop[Zone] = Zone == 0 ? Top : Math.max(Top, Floors[Zone] + 6);
            int Bottom = Floors[Zone] - 1;
            for (Scp1730Pit Each : Pits) {
                if (Each.Zone() == Zone) {
                    Bottom = Math.min(Bottom, Each.BottomY() - 1);
                }
            }
            for (Scp1730Basin Each : Basins) {
                if (Each.Zone() == Zone) {
                    Bottom = Math.min(Bottom, Each.BottomY() - 1);
                }
            }
            ZoneBottom[Zone] = Bottom;
            Zones.add(new Scp1730Zone(Zone, Kinds[Zone], MinX, Bottom, MinZ, MaxX, ZoneTop[Zone], MaxZ, Zone == 0 ? Arrival : Entries[Zone], Scp1730PuzzlePlan.objective(this, Zone)));
        }
        fitWideSigns();
        Scp1730Sectors.settleScreens(this);
        Scp1730Cutoffs.place(this);
        Grid.freeze();
    }

    public static boolean wideSign(String State) {
        return State != null && State.startsWith(Scp1730Blocks.Mod + "scp1730_sign_");
    }

    public static int[] outOf(String State) {
        int At = State.indexOf("facing=");
        String Facing = At < 0 ? "north" : State.substring(At + 7).split("[,\\]]")[0];
        return switch (Facing) {
            case "south" -> new int[]{0, 1};
            case "east" -> new int[]{1, 0};
            case "west" -> new int[]{-1, 0};
            default -> new int[]{0, -1};
        };
    }

    boolean wideFits(int X, int Y, int Z, int[] Out) {
        int Ax = Out[1] != 0 ? 1 : 0;
        int Az = Out[0] != 0 ? 1 : 0;
        for (int Side = -1; Side <= 1; Side++) {
            int Cx = X + Ax * Side;
            int Cz = Z + Az * Side;
            if (Side != 0 && !Grid.empty(Cx, Y, Cz)) {
                return false;
            }
            String Behind = Grid.get(Cx - Out[0], Y, Cz - Out[1]);
            if (Behind == null || Scp1730Blocks.passable(Behind) || wideSign(Behind)) {
                return false;
            }
        }
        return true;
    }

    void fitWideSigns() {
        Grid.freeze();
        List<int[]> Found = new ArrayList<>();
        List<String> States = new ArrayList<>();
        Grid.paint(0, 0, Scp1730Plan.Width - 1, Scp1730Plan.Depth - 1, (X, Y, Z, State) -> {
            if (wideSign(State)) {
                Found.add(new int[]{X, Y, Z});
                States.add(State);
            }
        });
        for (int Index = 0; Index < Found.size(); Index++) {
            int[] At = Found.get(Index);
            String State = States.get(Index);
            int[] Out = outOf(State);
            if (wideFits(At[0], At[1], At[2], Out)) {
                continue;
            }
            Grid.set(At[0], At[1], At[2], null);
            int Ax = Out[1] != 0 ? 1 : 0;
            int Az = Out[0] != 0 ? 1 : 0;
            for (int Shift : new int[]{1, -1, 2, -2}) {
                int X = At[0] + Ax * Shift;
                int Z = At[2] + Az * Shift;
                if (Grid.empty(X, At[1], Z) && wideFits(X, At[1], Z, Out)) {
                    Grid.set(X, At[1], Z, State);
                    break;
                }
            }
        }
    }

    void fixture(String Type, int X, int Y, int Z, String Facing, int Data, int Zone) {
        Fixtures.add(new Scp1730Fixture(Type, X, Y, Z, Facing, Data, Zone));
    }

    void decal(int X, int Y, int Z, String Face, int Kind, int Zone) {
        String Host = Grid.get(X, Y, Z);
        if (Host == null || !Host.startsWith(Scp1730Blocks.Mod) || Host.contains("scp1730") || Host.contains("[")) {
            return;
        }
        int Variant = 1 + Rng.nextInt(DecalVariants[Kind]);
        fixture("decal", X, Y, Z, Face, Kind * 16 + Variant, Zone);
        DecalHosts.add(key(X, Y, Z));
    }

    void decalExact(int X, int Y, int Z, String Face, int Kind, int Variant, int Zone) {
        String Host = Grid.get(X, Y, Z);
        if (Host == null || !Host.startsWith(Scp1730Blocks.Mod) || Host.contains("scp1730") || Host.contains("[")) {
            return;
        }
        fixture("decal", X, Y, Z, Face, Kind * 16 + Variant, Zone);
        DecalHosts.add(key(X, Y, Z));
    }

    final Set<Long> DecalHosts = new HashSet<>();

    boolean ceilingHole(int X, int Top, int Z) {
        String Above = Grid.get(X, Top + 1, Z);
        if (Above == null || Above.contains("scp1730") || !Grid.empty(X, Top, Z) || DecalHosts.contains(key(X, Top + 1, Z))) {
            return false;
        }
        Grid.set(X, Top + 1, Z, null);
        if (Grid.empty(X, Top + 2, Z)) {
            Grid.set(X, Top + 2, Z, Scp1730Blocks.RubbleFloor);
        }
        return true;
    }

    void rubbleFloor(int X, int Y, int Z) {
        String Floor = Grid.get(X, Y, Z);
        if (Floor != null && Floor.startsWith(Scp1730Blocks.Mod) && Floor.endsWith("_floor") && !DecalHosts.contains(key(X, Y, Z))) {
            Grid.set(X, Y, Z, Scp1730Blocks.RubbleFloor);
        }
    }

    void decay(int Zone) {
        int F = Floors[Zone];
        String Set = Zone == 0 ? "ez" : Zone == 1 ? "lcz" : null;
        Scp1730Layout.Room Lobby = Zone == 0 ? Layouts[0].Entry : null;
        for (int X = 0; X < Scp1730Plan.Width; X++) {
            for (int Z = 0; Z < Scp1730Plan.Depth; Z++) {
                boolean Heavy = Zone == 1 || Lobby != null && X >= Lobby.Bx0 && X <= Lobby.Bx1 && Z >= Lobby.Bz0 && Z <= Lobby.Bz1;
                String Floor = Grid.get(X, F, Z);
                if (Floor != null && !DecalHosts.contains(key(X, F, Z))) {
                    int Roll = Rng.nextInt(100);
                    if (Set != null && Floor.equals(Scp1730Blocks.Mod + Set + "_floor")) {
                        int Rubble = Heavy ? 8 : 2;
                        int Broken = Rubble + (Heavy ? 15 : 5);
                        int Cracked = Broken + (Heavy ? 32 : 16);
                        if (Roll < Rubble) {
                            Grid.set(X, F, Z, Scp1730Blocks.RubbleFloor);
                        } else if (Roll < Broken) {
                            Grid.set(X, F, Z, Scp1730Blocks.Mod + "scp1730_" + Set + "_floor_broken");
                        } else if (Roll < Cracked) {
                            Grid.set(X, F, Z, Scp1730Blocks.Mod + "scp1730_" + Set + "_floor_cracked");
                        }
                    } else if (Set == null && Floor.startsWith(Scp1730Blocks.Mod) && Floor.endsWith("_floor") && Roll < 3) {
                        Grid.set(X, F, Z, Scp1730Blocks.RubbleFloor);
                    }
                }
                if (Set == null) {
                    continue;
                }
                for (int Y = F + 2; Y <= F + 4; Y++) {
                    String Wall = Grid.get(X, Y, Z);
                    if (Wall != null && Wall.equals(Scp1730Blocks.Mod + Set + "_wall_middle") && !DecalHosts.contains(key(X, Y, Z))) {
                        int Roll = Rng.nextInt(100);
                        int Holed = Heavy ? HoledHeavy : HoledLight;
                        int Stained = Holed + (Heavy ? StainedHeavy : StainedLight);
                        if (Roll < Stained && !nearWallVariant(X, Y, Z)) {
                            Grid.set(X, Y, Z, Scp1730Blocks.Mod + "scp1730_" + Set + (Roll < Holed ? "_wall_holed" : "_wall_stained"));
                        }
                    }
                }
            }
        }
    }

    boolean nearWallVariant(int X, int Y, int Z) {
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dy = -1; Dy <= 1; Dy++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    String Other = (Dx | Dy | Dz) == 0 ? null : Grid.get(X + Dx, Y + Dy, Z + Dz);
                    if (Other != null && (Other.endsWith("_wall_stained") || Other.endsWith("_wall_holed"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void decayCorridors(int Zone, boolean Heavy) {
        int F = Floors[Zone];
        int Top = F + CorridorHeight;
        boolean Defence = false;
        int Collapses = 0;
        for (int[] Arm : ArmSpots) {
            if (Arm[3] != Zone || Arm.length < 8) {
                continue;
            }
            int Dir = Arm[2];
            int Lx = Dir % 2 == 0 ? 1 : 0;
            int Lz = Dir % 2 == 0 ? 0 : 1;
            int Ax = Scp1730Layout.Dx[Dir];
            int Az = Scp1730Layout.Dz[Dir];
            int Mx = Arm[6];
            int Mz = Arm[7];
            if (Heavy && Collapses < 3 && Rng.nextInt(100) < 30 && collapseCorridor(Mx, Mz, Ax, Az, Lx, Lz, F, Top, Zone)) {
                Collapses++;
            }
            int Rate = Heavy ? 3 : 1;
            for (int Pass = 0; Pass < Rate; Pass++) {
                int Along = Rng.nextInt(7) - 3;
                int Side = Rng.nextBoolean() ? -1 : 1;
                int X = Mx + Ax * Along + Lx * Side;
                int Z = Mz + Az * Along + Lz * Side;
                if (Grid.empty(X, F + 1, Z) && standable(X, F, Z)) {
                    String[] Floor = {Scp1730Blocks.GlassShards, Scp1730Blocks.MouldPatch, Scp1730Blocks.DragMarks};
                    Grid.set(X, F + 1, Z, Scp1730Blocks.facing(Floor[Rng.nextInt(Floor.length)], "up"));
                }
                int Cx = Mx + Ax * (Along + 2) - Lx * Side;
                int Cz = Mz + Az * (Along + 2) - Lz * Side;
                boolean SideFree = Grid.empty(Cx, F + 1, Cz) && Grid.empty(Cx + Lx * Side, F + 1, Cz + Lz * Side) && Grid.empty(Cx + Lx * Side * 2, F + 1, Cz + Lz * Side * 2);
                if (SideFree && Rng.nextInt(100) < (Heavy ? 70 : 40)) {
                    String[] Junk = {Scp1730Blocks.Rubble, Scp1730Blocks.FallenPanel, Scp1730Blocks.CableTangle, Scp1730Blocks.OverturnedBench, Scp1730Blocks.OverturnedChair, Scp1730Blocks.BodyBag,
                            Scp1730Blocks.Rubble, Scp1730Blocks.OverturnedDesk};
                    Grid.set(Cx, F + 1, Cz, Scp1730Blocks.facing(Junk[Rng.nextInt(Junk.length)], facingOf(Ax, Az)));
                }
                int Px = Mx + Ax * (Along - 1) + Lx * (Rng.nextInt(3) - 1);
                int Pz = Mz + Az * (Along - 1) + Lz * (Rng.nextInt(3) - 1);
                if (Heavy && Grid.empty(Px, F + 1, Pz) && standable(Px, F, Pz)) {
                    Grid.set(Px, F + 1, Pz, Scp1730Blocks.PaperStack + "[facing=" + Scp1730Layout.Facings[Rng.nextInt(4)] + ",variant=" + Rng.nextInt(6) + "]");
                }
                int Hx = Mx + Ax * (Along - 2);
                int Hz = Mz + Az * (Along - 2);
                if (Grid.empty(Hx, Top, Hz) && Grid.get(Hx, Top + 1, Hz) != null && Rng.nextInt(100) < (Heavy ? 55 : 20)) {
                    Grid.set(Hx, Top, Hz, Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.DanglingPanel : Scp1730Blocks.HangingCables, "down"));
                }
                int Wx = Mx + Ax * Along + Lx * Side * 2;
                int Wz = Mz + Az * Along + Lz * Side * 2;
                decal(Wx, F + 1 + Rng.nextInt(2), Wz, facingOf(-Lx * Side, -Lz * Side), pick(new int[]{Rust, Grime, Corrosion, Scorch, Cracks, Claws}), Zone);
                decal(Mx + Ax * Along, F, Mz + Az * Along, "up", pick(new int[]{Puddle, Grime, Oil, Footprints}), Zone);
                if (Heavy) {
                    int Ox = Mx + Ax * (Along + 1) - Lx * Side * 2;
                    int Oz = Mz + Az * (Along + 1) - Lz * Side * 2;
                    decal(Ox, F + 1 + Rng.nextInt(2), Oz, facingOf(Lx * Side, Lz * Side), pick(new int[]{Grime, Rust, Bullets, Blood, Cracks, Corrosion}), Zone);
                    decal(Mx + Ax * (Along + 2) + Lx * Side, F, Mz + Az * (Along + 2) + Lz * Side, "up", pick(new int[]{Puddle, Blood, Grime, Cracks}), Zone);
                }
            }
            if (Heavy && Rng.nextInt(100) < 35) {
                int Side = Rng.nextBoolean() ? -1 : 1;
                int Along = Rng.nextInt(5) - 2;
                int X = Mx + Ax * Along + Lx * Side;
                int Z = Mz + Az * Along + Lz * Side;
                if (Grid.empty(X, F + 1, Z) && isWallLike(Grid.get(X + Lx * Side, F + 1, Z + Lz * Side))) {
                    Grid.set(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.BrokenVent, facingOf(-Lx * Side, -Lz * Side)));
                    decal(X + Lx * Side, F + 2, Z + Lz * Side, facingOf(-Lx * Side, -Lz * Side), Claws, Zone);
                }
            }
            if (!Defence && Rng.nextInt(100) < 35) {
                int Side = Rng.nextBoolean() ? -1 : 1;
                int X = Mx + Lx * Side;
                int Z = Mz + Lz * Side;
                if (Grid.empty(X, F + 1, Z) && Grid.empty(X + Ax, F + 1, Z + Az) && Grid.empty(Mx, F + 1, Mz) && Grid.empty(Mx - Lx * Side, F + 1, Mz - Lz * Side)) {
                    Grid.set(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Sandbags, facingOf(Ax, Az)));
                    Grid.set(X + Ax, F + 1, Z + Az, Scp1730Blocks.facing(Rng.nextBoolean() ? Scp1730Blocks.Barricade : Scp1730Blocks.AmmoCrate, facingOf(Ax, Az)));
                    if (Grid.empty(X - Ax, F + 1, Z - Az)) {
                        Grid.set(X - Ax, F + 1, Z - Az, Scp1730Blocks.facing(Scp1730Blocks.BodyBag, facingOf(Lx, Lz)));
                    }
                    decal(X + Lx * Side, F + 2, Z + Lz * Side, facingOf(-Lx * Side, -Lz * Side), Bullets, Zone);
                    decal(X, F, Z, "up", Scorch, Zone);
                    Defence = true;
                }
            }
        }
    }

    boolean collapseCorridor(int Mx, int Mz, int Ax, int Az, int Lx, int Lz, int F, int Top, int Zone) {
        for (int Step = -2; Step <= 3; Step++) {
            for (int Lane = -1; Lane <= 1; Lane++) {
                int X = Mx + Ax * Step + Lx * Lane;
                int Z = Mz + Az * Step + Lz * Lane;
                if (!Grid.empty(X, F + 1, Z) || !Grid.empty(X, F + 2, Z) || !Grid.empty(X, Top, Z) || !standable(X, F, Z)) {
                    return false;
                }
            }
        }
        int Side = Rng.nextBoolean() ? -1 : 1;
        for (int Step = 0; Step <= 1; Step++) {
            for (int Lane : new int[]{Side, 0}) {
                int X = Mx + Ax * Step + Lx * Lane;
                int Z = Mz + Az * Step + Lz * Lane;
                ceilingHole(X, Top, Z);
                rubbleFloor(X, F, Z);
                if (Lane == Side) {
                    Grid.set(X, F + 1, Z, Scp1730Blocks.facing(Step == 0 ? Scp1730Blocks.Rubble : Scp1730Blocks.FallenPanel, facingOf(Ax, Az)));
                } else if (Step == 0) {
                    Grid.set(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Rubble, facingOf(Lx, Lz)));
                } else {
                    Grid.set(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.GlassShards, "up"));
                }
            }
        }
        for (int Step : new int[]{-1, 2}) {
            for (int Lane : new int[]{Side, 0}) {
                int X = Mx + Ax * Step + Lx * Lane;
                int Z = Mz + Az * Step + Lz * Lane;
                if (Grid.empty(X, Top, Z) && Grid.get(X, Top + 1, Z) != null) {
                    Grid.set(X, Top, Z, Scp1730Blocks.facing((Step + Lane) % 2 == 0 ? Scp1730Blocks.DanglingPanel : Scp1730Blocks.HangingCables, "down"));
                }
            }
            int X = Mx + Ax * Step + Lx * Side;
            int Z = Mz + Az * Step + Lz * Side;
            if (Grid.empty(X, F + 1, Z)) {
                Grid.set(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Rubble, facingOf(Ax, Az)));
            }
        }
        decal(Mx + Ax * 3 - Lx * Side, F, Mz + Az * 3 - Lz * Side, "up", Cracks, Zone);
        decal(Mx - Ax * 2 - Lx * Side, F, Mz - Az * 2 - Lz * Side, "up", Puddle, Zone);
        decal(Mx + Lx * Side * 2, F + 2, Mz + Lz * Side * 2, facingOf(-Lx * Side, -Lz * Side), Cracks, Zone);
        return true;
    }

    static long key(int X, int Y, int Z) {
        return ((long) X & 0xFFFFL) << 32 | ((long) Y & 0xFFFFL) << 16 | ((long) Z & 0xFFFFL);
    }

    void keepClear(int X, int Y, int Z) {
        Clear.add(key(X, Y, Z));
    }

    boolean cleared(int X, int Y, int Z) {
        return Clear.contains(key(X, Y, Z));
    }

    boolean freeCell(int X, int Y, int Z) {
        return Grid.empty(X, Y, Z) && !cleared(X, Y, Z);
    }

    static String facingOf(int Dx, int Dz) {
        if (Dz < 0) {
            return "north";
        }
        if (Dx > 0) {
            return "east";
        }
        if (Dz > 0) {
            return "south";
        }
        return "west";
    }

    static int dirOf(String Facing) {
        return switch (Facing) {
            case "north" -> 0;
            case "east" -> 1;
            case "south" -> 2;
            default -> 3;
        };
    }

    Scp1730Blocks.Palette corridorPalette(int Zone) {
        return switch (Zone) {
            case 0 -> Scp1730Blocks.Ez;
            case 1 -> Scp1730Blocks.Lcz;
            case 2 -> Scp1730Blocks.Hcz;
            default -> Scp1730Blocks.Containment;
        };
    }

    void corridors(int Zone) {
        Scp1730Layout Made = Layouts[Zone];
        Scp1730Blocks.Palette Look = corridorPalette(Zone);
        int F = Floors[Zone];
        for (int Sx = 0; Sx < Scp1730Layout.Slots; Sx++) {
            for (int Sz = 0; Sz < Scp1730Layout.Slots; Sz++) {
                if (!Made.Corridor[Sx][Sz]) {
                    continue;
                }
                boolean[][] Inside = new boolean[16][16];
                for (int A = 6; A <= 8; A++) {
                    for (int B = 6; B <= 8; B++) {
                        Inside[A][B] = true;
                    }
                }
                boolean[] Arms = Made.Links[Sx][Sz];
                for (int Dir = 0; Dir < 4; Dir++) {
                    if (!Arms[Dir]) {
                        continue;
                    }
                    for (int Step = 0; Step <= 8; Step++) {
                        for (int Wide = 6; Wide <= 8; Wide++) {
                            int Lx = switch (Dir) {
                                case 1 -> 7 + Step;
                                case 3 -> 7 - Step;
                                default -> Wide;
                            };
                            int Lz = switch (Dir) {
                                case 0 -> 7 - Step;
                                case 2 -> 7 + Step;
                                default -> Wide;
                            };
                            if (Lx >= 0 && Lx < 16 && Lz >= 0 && Lz < 16) {
                                Inside[Lx][Lz] = true;
                            }
                        }
                    }
                }
                int X0 = Scp1730Layout.Origin + Sx * 16;
                int Z0 = Scp1730Layout.Origin + Sz * 16;
                for (int Lx = 0; Lx < 16; Lx++) {
                    for (int Lz = 0; Lz < 16; Lz++) {
                        boolean Wall = false;
                        if (!Inside[Lx][Lz]) {
                            for (int Ox = -1; Ox <= 1; Ox++) {
                                for (int Oz = -1; Oz <= 1; Oz++) {
                                    int Nx = Lx + Ox;
                                    int Nz = Lz + Oz;
                                    if (Nx >= 0 && Nx < 16 && Nz >= 0 && Nz < 16 && Inside[Nx][Nz]) {
                                        Wall = true;
                                    }
                                }
                            }
                        }
                        if (!Inside[Lx][Lz] && !Wall) {
                            continue;
                        }
                        int X = X0 + Lx;
                        int Z = Z0 + Lz;
                        Grid.set(X, F, Z, Look.Floor);
                        Grid.set(X, F + CorridorHeight + 1, Z, Look.Ceiling);
                        if (Wall) {
                            for (int Level = 1; Level <= CorridorHeight; Level++) {
                                Grid.set(X, F + Level, Z, Look.wall(Level, CorridorHeight));
                            }
                            CorridorWalls.add(new int[]{X, Z, Zone});
                        }
                    }
                }
                CeilingSpots.add(new int[]{X0 + 7, F + CorridorHeight, Z0 + 7, Zone, Sx, Sz});
                for (int Dir = 0; Dir < 4; Dir++) {
                    if (Arms[Dir]) {
                        ArmSpots.add(new int[]{X0, Z0, Dir, Zone, Sx, Sz});
                    }
                }
            }
        }
    }

    void shell(Scp1730Layout.Room Made, int Zone) {
        Scp1730Blocks.Palette Look = Rooms.paletteOf(Made, Zone);
        int F = Made.Floor;
        int H = Made.Height;
        for (int X = Made.Bx0; X <= Made.Bx1; X++) {
            for (int Z = Made.Bz0; Z <= Made.Bz1; Z++) {
                Grid.set(X, F, Z, Look.Floor);
                Grid.set(X, F + H + 1, Z, Look.Ceiling);
                boolean Edge = X == Made.Bx0 || X == Made.Bx1 || Z == Made.Bz0 || Z == Made.Bz1;
                if (Edge) {
                    for (int Level = 1; Level <= H; Level++) {
                        Grid.set(X, F + Level, Z, Look.wall(Level, H));
                    }
                }
            }
        }
    }

    void doorways(Scp1730Layout.Room Made, int Zone) {
        for (Scp1730Layout.Door Each : Made.Doors) {
            carveDoor(Made, Each, Zone);
        }
    }

    void insetRoom(Scp1730Layout.Room Made) {
        if (Large.contains(Made.Type)) {
            return;
        }
        int ShrinkX = Made.Type.equals("lobby") ? 10 : Made.W == 1 ? 4 + Rng.nextInt(2) : 7 + Rng.nextInt(3);
        int ShrinkZ = Made.Type.equals("lobby") ? 10 : Made.D == 1 ? 4 + Rng.nextInt(2) : 7 + Rng.nextInt(3);
        int West = split(ShrinkX);
        int North = split(ShrinkZ);
        Made.inset(West, North, ShrinkX - West, ShrinkZ - North);
    }

    int split(int Shrink) {
        int Low = Math.max(0, Shrink - 5);
        int High = Math.min(5, Shrink);
        return Low + Rng.nextInt(High - Low + 1);
    }

    void carveDoor(Scp1730Layout.Room Made, Scp1730Layout.Door Each, int Zone) {
        int Cx = Made.doorX(Each);
        int Cz = Made.doorZ(Each);
        int Ex = Each.centreX();
        int Ez = Each.centreZ();
        int F = Made.Floor;
        int Lx = Each.Dir % 2 == 0 ? 1 : 0;
        int Lz = Each.Dir % 2 == 0 ? 0 : 1;
        int Ix = -Scp1730Layout.Dx[Each.Dir];
        int Iz = -Scp1730Layout.Dz[Each.Dir];
        int Ox = Scp1730Layout.Dx[Each.Dir];
        int Oz = Scp1730Layout.Dz[Each.Dir];
        Scp1730Blocks.Palette Hall = corridorPalette(Zone);
        int Length = Math.abs(Cx - Ex) + Math.abs(Cz - Ez);
        for (int Step = 0; Step < Length; Step++) {
            int Sx = Ex + Ix * Step;
            int Sz = Ez + Iz * Step;
            for (int Side = -2; Side <= 2; Side++) {
                int X = Sx + Lx * Side;
                int Z = Sz + Lz * Side;
                Grid.set(X, F, Z, Hall.Floor);
                Grid.set(X, F + CorridorHeight + 1, Z, Hall.Ceiling);
                for (int Level = 1; Level <= CorridorHeight; Level++) {
                    Grid.set(X, F + Level, Z, Math.abs(Side) == 2 ? Hall.wall(Level, CorridorHeight) : null);
                }
            }
            if (Step == Length / 2 && Rng.nextInt(3) == 0) {
                Rooms.ceilingLight(Sx, F + CorridorHeight, Sz, Zone, Lx == 0);
            }
        }
        for (int Side = -1; Side <= 1; Side++) {
            for (int Level = 1; Level <= 3; Level++) {
                Grid.set(Cx + Lx * Side, F + Level, Cz + Lz * Side, null);
            }
            for (int Depth = 1; Depth <= 3; Depth++) {
                for (int Level = 1; Level <= 3; Level++) {
                    keepClear(Cx + Lx * Side + Ix * Depth, F + Level, Cz + Lz * Side + Iz * Depth);
                }
            }
        }
        Scp1730Blocks.Palette Look = Rooms.paletteOf(Made, Zone);
        for (int Side = -1; Side <= 1; Side++) {
            Grid.set(Cx + Lx * Side, F, Cz + Lz * Side, Look.marked(Scp1730Layout.Facings[Each.Dir]));
        }
        if (Made.Type.equals("sanctum") || Made.Type.equals(Scp1730Sectors.BioWing)) {
            return;
        }
        int Roll = Rng.nextInt(100);
        int Breach = Zone == 0 ? 35 : Zone == 1 ? 50 : 65;
        if (Roll < Breach / 2) {
            for (int Side = -1; Side <= 1; Side++) {
                Grid.set(Cx + Lx * Side, F + 3, Cz + Lz * Side, Scp1730Blocks.facing(Scp1730Blocks.Shutter, Scp1730Layout.Facings[Each.Dir]));
            }
        } else if (Roll < Breach) {
            int Side = Rng.nextBoolean() ? -1 : 1;
            for (int Level = 1; Level <= 3; Level++) {
                Grid.set(Cx + Lx * Side, F + Level, Cz + Lz * Side, Scp1730Blocks.facing(Scp1730Blocks.HeavyDoor, Scp1730Layout.Facings[Each.Dir]));
            }
            int Wx = Cx + Ix * 2 - Lx * Side;
            int Wz = Cz + Iz * 2 - Lz * Side;
            if (Rng.nextBoolean() && Grid.empty(Wx, F + 1, Wz)) {
                Grid.set(Wx, F + 1, Wz, Scp1730Blocks.facing(Scp1730Blocks.CellDoorWreck, Scp1730Layout.Facings[(Each.Dir + 1) % 4]));
            }
            decal(Cx + Ix, F, Cz + Iz, "up", Scorch, Zone);
        }
        Rooms.doorSign(Made, Each, Cx + Ox + Lx, F + 3, Cz + Oz + Lz, Zone);
    }

    void stairwell(int Zone, Scp1730Layout.Room Upper, Scp1730Layout.Room Lower) {
        Scp1730Layout.Door Door = Upper.Doors.get(0);
        int Fu = Floors[Zone];
        int Fl = Floors[Zone + 1];
        int Top = Fu + ShaftHead;
        Scp1730Blocks.Palette UpperLook = corridorPalette(Zone);
        Scp1730Blocks.Palette LowerLook = corridorPalette(Zone + 1);
        int X0 = Upper.x0();
        int Z0 = Upper.z0();
        int X1 = Upper.x1();
        int Z1 = Upper.z1();
        for (int X = X0; X <= X1; X++) {
            for (int Z = Z0; Z <= Z1; Z++) {
                for (int Y = Fl; Y <= Top; Y++) {
                    String State;
                    if (Y == Fl) {
                        State = LowerLook.Floor;
                    } else if (Y == Top) {
                        State = UpperLook.Ceiling;
                    } else if (Y <= Fl + 5) {
                        State = LowerLook.wall(Y - Fl, 5);
                    } else if (Y > Fu) {
                        State = UpperLook.wall(Y - Fu, ShaftHead);
                    } else {
                        State = (Y > Fl + 6 ? UpperLook : LowerLook).WallMiddle;
                    }
                    Grid.set(X, Y, Z, State);
                }
            }
        }
        int Cx = Door.centreX();
        int Cz = Door.centreZ();
        int Ox = Scp1730Layout.Dx[Door.Dir];
        int Oz = Scp1730Layout.Dz[Door.Dir];
        int Ix = -Ox;
        int Iz = -Oz;
        int Lx = Door.Dir % 2 == 0 ? 1 : 0;
        int Lz = Door.Dir % 2 == 0 ? 0 : 1;
        int LowSide = Lx != 0 ? X0 - Cx : Z0 - Cz;
        int HighSide = Lx != 0 ? X1 - Cx : Z1 - Cz;
        int Middle = Math.max(LowSide + 3, Math.min(HighSide - 3, 0));
        int A0 = Middle - 2;
        int A1 = Middle + 2;
        int Rail = Middle;
        int Reach = Lx != 0 ? Math.abs(Iz != 0 ? (Iz > 0 ? Z1 - Cz : Cz - Z0) : 0) : Math.abs(Ix > 0 ? X1 - Cx : Cx - X0);
        int Long = Math.min(ShaftLong, Reach - 1);
        int Flight = Math.max(2, Long - 6);
        for (int U = A0; U <= A1; U++) {
            for (int V = 1; V <= Long; V++) {
                for (int Y = Fl + 1; Y < Top; Y++) {
                    Grid.set(Cx + Lx * U + Ix * V, Y, Cz + Lz * U + Iz * V, null);
                }
            }
        }
        for (int U = Math.min(A0, -1); U <= Math.max(A1, 1); U++) {
            for (int V = 1; V <= 3; V++) {
                for (int Y = Fu + 1; Y < Top; Y++) {
                    Grid.set(Cx + Lx * U + Ix * V, Y, Cz + Lz * U + Iz * V, null);
                }
                Grid.set(Cx + Lx * U + Ix * V, Fu, Cz + Lz * U + Iz * V, UpperLook.Floor);
            }
        }
        String Ascend = facingOf(-Ix, -Iz);
        String Descend = facingOf(Ix, Iz);
        String Bars = Ix != 0 ? "minecraft:iron_bars[east=true,west=true,north=false,south=false]" : "minecraft:iron_bars[north=true,south=true,east=false,west=false]";
        int Level = Fu;
        int Remaining = Fu - Fl;
        int Flights = Math.max(1, (Remaining + Flight - 1) / Flight);
        int Count = 0;
        boolean EndForward = true;
        while (Remaining > 0) {
            int Left = Flights - Count;
            int Steps = Math.min(Flight, (Remaining + Left - 1) / Math.max(1, Left));
            boolean Forward = Count % 2 == 0;
            int L0 = Forward ? A0 : Rail + 1;
            int L1 = Forward ? Rail - 1 : A1;
            Scp1730Blocks.Palette Look = Level - Steps / 2 > Fl + 6 ? UpperLook : LowerLook;
            for (int Step = 0; Step < Steps; Step++) {
                int V = Forward ? 4 + Step : 3 + Flight - Step;
                int Y = Level - Step;
                for (int U = L0; U <= L1; U++) {
                    int X = Cx + Lx * U + Ix * V;
                    int Z = Cz + Lz * U + Iz * V;
                    Grid.set(X, Y, Z, Look.stairs(Forward ? Ascend : Descend));
                    if (Y - 1 > Fl) {
                        Grid.set(X, Y - 1, Z, Look.WallMiddle);
                    }
                }
                int Rx = Cx + Lx * Rail + Ix * V;
                int Rz = Cz + Lz * Rail + Iz * V;
                Grid.set(Rx, Y, Rz, Look.WallMiddle);
                if (Y + 1 < Top) {
                    Grid.set(Rx, Y + 1, Rz, Bars);
                }
            }
            Level -= Steps;
            Remaining -= Steps;
            EndForward = Forward;
            if (Remaining > 0) {
                int V0 = Forward ? 4 + Flight : 1;
                int V1 = Forward ? Long : 3;
                for (int U = A0; U <= A1; U++) {
                    for (int V = V0; V <= V1; V++) {
                        Grid.set(Cx + Lx * U + Ix * V, Level, Cz + Lz * U + Iz * V, Look.Floor);
                    }
                }
                int G0 = Forward ? 4 + Steps : 4;
                int G1 = Forward ? 3 + Flight : 3 + Flight - Steps;
                for (int U = L0; U <= L1; U++) {
                    for (int V = G0; V <= G1; V++) {
                        Grid.set(Cx + Lx * U + Ix * V, Level, Cz + Lz * U + Iz * V, Look.Floor);
                    }
                }
                for (int V = G0; V <= G1; V++) {
                    Grid.set(Cx + Lx * Rail + Ix * V, Level, Cz + Lz * Rail + Iz * V, Look.WallMiddle);
                    Grid.set(Cx + Lx * Rail + Ix * V, Level + 1, Cz + Lz * Rail + Iz * V, Bars);
                }
                int Lu = Forward ? A1 : A0;
                int Lv = Forward ? V1 : 2;
                String Out = facingOf(Lu == A0 ? Lx : -Lx, Lu == A0 ? Lz : -Lz);
                int Wx = Cx + Lx * Lu + Ix * Lv;
                int Wz = Cz + Lz * Lu + Iz * Lv;
                if (Grid.empty(Wx, Level + 3, Wz)) {
                    Grid.set(Wx, Level + 3, Wz, Scp1730Blocks.facing(Count % 3 == 1 ? Scp1730Blocks.BrokenLight : Scp1730Blocks.EmergencyLight, Out));
                }
            }
            Count++;
        }
        int Bv = EndForward ? Long - 1 : 2;
        int Tx = Cx + Lx * Middle + Ix * Bv;
        int Tz = Cz + Lz * Middle + Iz * Bv;
        for (Scp1730Layout.Door Each : Lower.Doors) {
            int Dx = Each.centreX();
            int Dz = Each.centreZ();
            int Nx = -Scp1730Layout.Dx[Each.Dir];
            int Nz = -Scp1730Layout.Dz[Each.Dir];
            if ((Dx - Cx) * Ix + (Dz - Cz) * Iz < 0) {
                Nx = Scp1730Layout.Dx[Each.Dir];
                Nz = Scp1730Layout.Dz[Each.Dir];
            }
            int[] Shaft = {Cx, Cz, Lx, Lz, Ix, Iz, A0, A1, Long, X0, Z0, X1, Z1, Fl};
            int Du = (Dx - Cx) * Lx + (Dz - Cz) * Lz;
            int Dv = (Dx - Cx) * Ix + (Dz - Cz) * Iz;
            int Nu = Nx * Lx + Nz * Lz;
            int Nv = Nx * Ix + Nz * Iz;
            int Su = Du + Nu * 2;
            int Sv = Dv + Nv * 2;
            leg(Du, Dv, Su, Sv, Shaft);
            leg(Su, Sv, Su, Bv, Shaft);
            leg(Su, Bv, Middle, Bv, Shaft);
            int Ex = Each.Dir % 2 == 0 ? 1 : 0;
            int Ez = Each.Dir % 2 == 0 ? 0 : 1;
            for (int Side = -1; Side <= 1; Side++) {
                for (int Y = 1; Y <= 3; Y++) {
                    Grid.set(Dx + Ex * Side, Fl + Y, Dz + Ez * Side, null);
                }
                Grid.set(Dx + Ex * Side, Fl, Dz + Ez * Side, LowerLook.marked(Scp1730Layout.Facings[Each.Dir]));
            }
        }
        String Axis = Door.Dir % 2 == 0 ? "x" : "z";
        for (int Side = -1; Side <= 1; Side++) {
            for (int Y = 1; Y <= 3; Y++) {
                int X = Cx + Lx * Side;
                int Z = Cz + Lz * Side;
                Grid.set(X, Fu + Y, Z, Scp1730Blocks.with(Scp1730Blocks.Seal, "axis=" + Axis));
                fixture("seal", X, Fu + Y, Z, Scp1730Layout.Facings[Door.Dir], (Side + 1) * 3 + (Y - 1), Zone);
            }
            Grid.set(Cx + Lx * Side, Fu, Cz + Lz * Side, UpperLook.marked(Scp1730Layout.Facings[Door.Dir]));
        }
        String Outward = facingOf(Ox, Oz);
        int[] Panel = beside(Cx, Cz, Lx, Lz, Ox, Oz, Fu + 2, 1);
        if (Panel == null) {
            Panel = niche(Cx, Cz, Lx, Lz, Ox, Oz, Fu, 1, UpperLook);
        }
        if (Zone == 0 && Panel != null) {
            Grid.set(Panel[0], Fu + 2, Panel[1], Scp1730Blocks.with(Scp1730Blocks.Control, "facing=" + Outward + ",kind=reader,active=false"));
            fixture("control", Panel[0], Fu + 2, Panel[1], Outward, 0, Zone);
        }
        int[] SignAt = beside(Cx, Cz, Lx, Lz, Ox, Oz, Fu + 3, Panel != null && Panel[2] == 1 ? -1 : 1);
        if (SignAt == null && Panel != null && Grid.empty(Panel[0], Fu + 3, Panel[1])) {
            SignAt = Panel;
        }
        if (SignAt != null) {
            Grid.set(SignAt[0], Fu + 3, SignAt[1], Scp1730Blocks.facing(Scp1730Blocks.Mod + "facility_sign_stairs_small", Outward));
        }
        Scp1730Layout.Door Out = Lower.Doors.get(0);
        float Yaw = yawToward(Tx + 0.5, Tz + 0.5, Out.centreX() + 0.5, Out.centreZ() + 0.5);
        Entries[Zone + 1] = new Scp1730Spot(Tx + 0.5, Fl + 1, Tz + 0.5, Yaw);
    }

    static boolean inShaft(int X, int Z, int[] Shaft) {
        int U = (X - Shaft[0]) * Shaft[2] + (Z - Shaft[1]) * Shaft[3];
        int V = (X - Shaft[0]) * Shaft[4] + (Z - Shaft[1]) * Shaft[5];
        return U >= Shaft[6] && U <= Shaft[7] && V >= 1 && V <= Shaft[8];
    }

    void leg(int U0, int V0, int U1, int V1, int[] Shaft) {
        for (int U = Math.min(U0, U1) - 1; U <= Math.max(U0, U1) + 1; U++) {
            for (int V = Math.min(V0, V1) - 1; V <= Math.max(V0, V1) + 1; V++) {
                boolean AlongU = U0 != U1;
                if (AlongU ? V < Math.min(V0, V1) - 1 || V > Math.max(V0, V1) + 1 || U < Math.min(U0, U1) || U > Math.max(U0, U1)
                        : U < Math.min(U0, U1) - 1 || U > Math.max(U0, U1) + 1 || V < Math.min(V0, V1) || V > Math.max(V0, V1)) {
                    continue;
                }
                int X = Shaft[0] + Shaft[2] * U + Shaft[4] * V;
                int Z = Shaft[1] + Shaft[3] * U + Shaft[5] * V;
                if (X <= Shaft[9] || X >= Shaft[11] || Z <= Shaft[10] || Z >= Shaft[12] || inShaft(X, Z, Shaft)) {
                    continue;
                }
                for (int Y = Shaft[13] + 1; Y <= Shaft[13] + 3; Y++) {
                    Grid.set(X, Y, Z, null);
                }
            }
        }
    }

    int[] niche(int Cx, int Cz, int Lx, int Lz, int Ox, int Oz, int Fu, int Prefer, Scp1730Blocks.Palette Look) {
        for (int Pass = 0; Pass < 2; Pass++) {
            int Side = Pass == 0 ? Prefer : -Prefer;
            int Wx = Cx + Lx * 2 * Side;
            int Wz = Cz + Lz * 2 * Side;
            int Px = Wx + Ox;
            int Pz = Wz + Oz;
            int Bx = Px + Lx * Side;
            int Bz = Pz + Lz * Side;
            if (!isWallLike(Grid.get(Wx, Fu + 2, Wz)) || !isWallLike(Grid.get(Px, Fu + 2, Pz)) || !Grid.empty(Px - Lx * Side, Fu + 2, Pz - Lz * Side)) {
                continue;
            }
            boolean Clear = true;
            for (int Y = Fu; Y <= Fu + 4; Y++) {
                String Behind = Grid.get(Bx, Y, Bz);
                if (Behind == null ? Y > Fu && Y < Fu + 4 && Grid.get(Bx, Fu, Bz) != null : !isWallLike(Behind) && !Behind.equals(Look.Floor) && !Behind.equals(Look.Ceiling)
                        && !Behind.equals(Look.WallTop)) {
                    Clear = false;
                }
            }
            if (!Clear) {
                continue;
            }
            for (int Y = Fu; Y <= Fu + 4; Y++) {
                if (Grid.empty(Bx, Y, Bz)) {
                    Grid.set(Bx, Y, Bz, Y == Fu ? Look.Floor : Y == Fu + 4 ? Look.Ceiling : Look.wall(Y - Fu, CorridorHeight));
                }
            }
            for (int Y = Fu + 1; Y <= Fu + 3; Y++) {
                Grid.set(Px, Y, Pz, null);
            }
            Grid.set(Px, Fu, Pz, Look.Floor);
            Grid.set(Px, Fu + 4, Pz, Look.Ceiling);
            return new int[]{Px, Pz, Side};
        }
        return null;
    }

    int[] beside(int Cx, int Cz, int Lx, int Lz, int Ox, int Oz, int Y, int Prefer) {
        for (int Pass = 0; Pass < 2; Pass++) {
            int Side = Pass == 0 ? Prefer : -Prefer;
            int Wx = Cx + Lx * 2 * Side;
            int Wz = Cz + Lz * 2 * Side;
            int Px = Wx + Ox;
            int Pz = Wz + Oz;
            if (isWallLike(Grid.get(Wx, Y, Wz)) && Grid.empty(Px, Y, Pz) && Grid.empty(Px, Y - 1, Pz)) {
                return new int[]{Px, Pz, Side};
            }
        }
        return null;
    }

    static float yawToward(double X, double Z, double Tx, double Tz) {
        double Ax = Tx - X;
        double Az = Tz - Z;
        return (float) Math.toDegrees(Math.atan2(-Ax, Az));
    }

    final class StairFrame {
        final int X0;
        final int Z0;
        final int X1;
        final int Z1;
        final boolean AlongZ;
        final boolean Near;

        StairFrame(Scp1730Layout.Room Box, boolean AlongZ, boolean Near) {
            this.X0 = Box.x0();
            this.Z0 = Box.z0();
            this.X1 = Box.x1();
            this.Z1 = Box.z1();
            this.AlongZ = AlongZ;
            this.Near = Near;
        }

        int x(int U, int V) {
            if (AlongZ) {
                return Near ? X0 + U : X1 - U;
            }
            return Near ? X0 + V : X1 - V;
        }

        int z(int U, int V) {
            if (AlongZ) {
                return Near ? Z0 + V : Z1 - V;
            }
            return Near ? Z1 - U : Z0 + U;
        }

        String facing(int Du, int Dv) {
            int Dx = x(Du + 5, Dv + 5) - x(5, 5);
            int Dz = z(Du + 5, Dv + 5) - z(5, 5);
            return facingOf(Dx, Dz);
        }

        String bars(boolean AlongV) {
            int Dx = AlongV ? x(5, 6) - x(5, 5) : x(6, 5) - x(5, 5);
            boolean XAxis = Dx != 0;
            return XAxis ? "minecraft:iron_bars[east=true,west=true,north=false,south=false]" : "minecraft:iron_bars[north=true,south=true,east=false,west=false]";
        }
    }

    void dressCorridors(int Zone) {
        int F = Floors[Zone];
        int Top = F + CorridorHeight;
        for (int[] Spot : CeilingSpots) {
            if (Spot[3] != Zone) {
                continue;
            }
            boolean[] Arms = Layouts[Zone].Links[Spot[4]][Spot[5]];
            boolean AlongX = Arms[1] || Arms[3];
            boolean AlongZ = Arms[0] || Arms[2];
            Rooms.ceilingLight(Spot[0], Spot[1], Spot[2], Zone, AlongX && !AlongZ);
            int Junctions = (Arms[0] ? 1 : 0) + (Arms[1] ? 1 : 0) + (Arms[2] ? 1 : 0) + (Arms[3] ? 1 : 0);
            if (Junctions >= 3) {
                TurretSpots.add(new int[]{Spot[0], Spot[1], Spot[2], Zone});
            }
            if (Rng.nextInt(100) < 35) {
                int Px = Spot[0] + Rng.nextInt(3) - 1;
                int Pz = Spot[2] + Rng.nextInt(3) - 1;
                if (Grid.empty(Px, Top, Pz)) {
                    Grid.set(Px, Top + 1, Pz, Rooms.lampOf(Zone));
                }
            }
            if (Rng.nextInt(100) < 45) {
                decal(Spot[0] + Rng.nextInt(3) - 1, F, Spot[2] + Rng.nextInt(3) - 1, "up", pick(new int[]{Puddle, Grime, Footprints, Oil}), Zone);
            }
        }
        for (int[] Arm : ArmSpots) {
            if (Arm[3] != Zone) {
                continue;
            }
            int Dir = Arm[2];
            int X0 = Arm[0];
            int Z0 = Arm[1];
            int Mx = Dir == 1 ? X0 + 12 : Dir == 3 ? X0 + 2 : X0 + 7;
            int Mz = Dir == 2 ? Z0 + 12 : Dir == 0 ? Z0 + 2 : Z0 + 7;
            if (Rng.nextInt(100) < 50) {
                Rooms.ceilingLight(Mx, Top, Mz, Zone, Dir % 2 == 1);
            }
            int Lx = Dir % 2 == 0 ? 1 : 0;
            int Lz = Dir % 2 == 0 ? 0 : 1;
            int Side = Rng.nextBoolean() ? -1 : 1;
            int Wx = Mx + Lx * 2 * Side;
            int Wz = Mz + Lz * 2 * Side;
            String Out = facingOf(-Lx * Side, -Lz * Side);
            int Ax = Scp1730Layout.Dx[Dir];
            int Az = Scp1730Layout.Dz[Dir];
            decal(Mx + Rng.nextInt(3) - 1, F, Mz + Rng.nextInt(3) - 1, "up", pick(Zone == 0 ? new int[]{Footprints, Grime, Puddle, Scorch, Puddle}
                    : new int[]{Blood, Scorch, Footprints, Claws, Grime, Oil, Puddle}), Zone);
            decal(Wx, F + 1 + Rng.nextInt(2), Wz, Out, pick(new int[]{Bullets, Cracks, Claws, Scorch, Rust, Blood, Grime, Rust}), Zone);
            if (Rng.nextInt(100) < 55) {
                decal(Wx + Ax * 2, F + 2, Wz + Az * 2, Out, pick(new int[]{Rust, Grime, Cracks}), Zone);
            }
            int Cx = Mx + Lx * Side;
            int Cz = Mz + Lz * Side;
            int Roll = Rng.nextInt(100);
            if (Roll < 16 + Zone * 4) {
                int Hx = Cx - Ax;
                int Hz = Cz - Az;
                if (ceilingHole(Hx, Top, Hz)) {
                    rubbleFloor(Hx, F, Hz);
                    if (Grid.empty(Hx, F + 1, Hz)) {
                        Grid.set(Hx, F + 1, Hz, Scp1730Blocks.facing(Scp1730Blocks.FallenPanel, facingOf(Ax, Az)));
                    }
                    if (Grid.empty(Hx + Ax, Top, Hz + Az)) {
                        Grid.set(Hx + Ax, Top, Hz + Az, Scp1730Blocks.facing(Scp1730Blocks.HangingCables, "down"));
                    }
                }
            } else if (Roll < 34 + Zone * 4) {
                if (Grid.empty(Cx, F + 1, Cz)) {
                    String[] Clutter = Zone == 0 ? new String[]{Scp1730Blocks.OverturnedChair, Scp1730Blocks.Rubble, Scp1730Blocks.CableTangle, Scp1730Blocks.PaperStack}
                            : new String[]{Scp1730Blocks.Rubble, Scp1730Blocks.CableTangle, Scp1730Blocks.OverturnedDesk, Scp1730Blocks.Rubble, Scp1730Blocks.BodyBag};
                    String Pick = Clutter[Rng.nextInt(Clutter.length)];
                    Grid.set(Cx, F + 1, Cz, Pick.equals(Scp1730Blocks.PaperStack) ? Pick + "[facing=" + Out + ",variant=" + Rng.nextInt(6) + "]" : Scp1730Blocks.facing(Pick, Out));
                }
            } else if (Roll < 44 + Zone * 4) {
                if (Grid.empty(Cx, F + 1, Cz)) {
                    Grid.set(Cx, F + 1, Cz, Scp1730Blocks.facing(Scp1730Blocks.DragMarks, "up"));
                }
            }
            if (Rng.nextInt(100) < 30) {
                int Hx = Mx + Ax * 3;
                int Hz = Mz + Az * 3;
                if (Grid.empty(Hx, Top, Hz)) {
                    Grid.set(Hx, Top, Hz, Scp1730Blocks.facing(Scp1730Blocks.HangingCables, "down"));
                }
            }
            if (Rng.nextInt(100) < 18 && Grid.empty(Cx, F + 2, Cz) && Grid.empty(Cx, F + 1, Cz) && backed(Grid.get(Wx, F + 2, Wz))) {
                String Wall = Zone == 0 ? Scp1730Blocks.Mod + "fire_extinguisher" : Zone == 1 ? Scp1730Blocks.Conduit : Scp1730Blocks.Mod + "hcz_caged_lamp";
                String State = Wall.endsWith("fire_extinguisher") ? Scp1730Blocks.with(Wall, "facing=" + Out + ",wall=true")
                        : Wall.endsWith("hcz_caged_lamp") ? Scp1730Blocks.with(Wall, "facing=" + Out + ",part=single") : Scp1730Blocks.facing(Wall, Out);
                Grid.set(Cx, F + 2, Cz, State);
            }
            if (Rng.nextInt(100) < 30) {
                int Px = Cx + Ax * 2;
                int Pz = Cz + Az * 2;
                if (Grid.empty(Px, Top, Pz) && backed(Grid.get(Wx + Ax * 2, Top, Wz + Az * 2))) {
                    Grid.set(Px, Top, Pz, Scp1730Blocks.facing(Scp1730Blocks.Conduit, Out));
                }
            }
            if (Rng.nextInt(100) < 14 && isWallLike(Grid.get(Wx, F + 2, Wz)) && Grid.empty(Wx + Lx * Side, F + 2, Wz + Lz * Side)) {
                Grid.set(Wx, F + 2, Wz, Scp1730Blocks.facing(Scp1730Blocks.BoardedWindow, Out));
            }
            if (Zone > 0 && Rng.nextInt(100) < 10 + Zone * 6 && Grid.empty(Cx - Ax * 3, Top, Cz - Az * 3)) {
                Grid.set(Cx - Ax * 3, Top, Cz - Az * 3, Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, Out));
            }
            if (Rng.nextInt(100) < 16) {
                int[] ZoneSign = {3, 1, 2, 2};
                decalExact(Wx - Ax * 3, F + 2, Wz - Az * 3, Out, ZoneDecal, ZoneSign[Zone], Zone);
            } else if (Rng.nextInt(100) < 12) {
                decalExact(Wx - Ax * 3, F + 2, Wz - Az * 3, Out, ArrowDecal, 1 + Rng.nextInt(4), Zone);
            } else if (Zone > 0 && Rng.nextInt(100) < 10) {
                decalExact(Wx - Ax * 3, F + 2, Wz - Az * 3, Out, HazardDecal, new int[]{1, 3, 6}[Rng.nextInt(3)], Zone);
            }
            ArmSpots.set(ArmSpots.indexOf(Arm), new int[]{X0, Z0, Dir, Zone, Arm[4], Arm[5], Mx, Mz});
        }
        int Placards = 0;
        List<int[]> Arms = new ArrayList<>();
        for (int[] Arm : ArmSpots) {
            if (Arm[3] == Zone && Arm.length > 6) {
                Arms.add(Arm);
            }
        }
        shuffle(Arms);
        for (int[] Arm : Arms) {
            if (Placards >= 2) {
                break;
            }
            int Dir = Arm[2];
            int Lx = Dir % 2 == 0 ? 1 : 0;
            int Lz = Dir % 2 == 0 ? 0 : 1;
            int Ax = Scp1730Layout.Dx[Dir];
            int Az = Scp1730Layout.Dz[Dir];
            int Side = Rng.nextBoolean() ? -1 : 1;
            int X = Arm[6] + Lx * Side;
            int Z = Arm[7] + Lz * Side;
            boolean Room = true;
            for (int Step = -1; Step <= 1; Step++) {
                if (!Grid.empty(X + Ax * Step, F + 2, Z + Az * Step) || !isWallLike(Grid.get(X + Ax * Step + Lx * Side, F + 2, Z + Az * Step + Lz * Side))) {
                    Room = false;
                }
            }
            if (!Room) {
                continue;
            }
            Grid.set(X, F + 2, Z, Scp1730Blocks.facing(Scp1730Blocks.SignSector + "abcd".charAt(Zone), facingOf(-Lx * Side, -Lz * Side)));
            Placards++;
        }
    }

    void loot(int X, int Y, int Z, String Facing, int Tier, int Zone) {
        fixture("loot", X, Y, Z, Facing, Tier, Zone);
        LootCount.merge(Zone * 4 + Tier, 1, Integer::sum);
    }

    final Map<Integer, Integer> LootCount = new HashMap<>();
    final List<Scp1730Layout.Room> SafeRooms = new ArrayList<>();
    final Scp1730SetPieces.Picker SetPieces;

    static final int[][] LootFloor = {{4, 1, 1, 0}, {2, 3, 1, 0}, {1, 2, 3, 1}, {0, 1, 1, 2}};

    boolean standable(int X, int F, int Z) {
        String Below = Grid.get(X, F, Z);
        return Below != null && !Scp1730Blocks.passable(Below) && Grid.empty(X, F + 1, Z) && Grid.empty(X, F + 2, Z);
    }

    boolean nearStairs(int Zone, int X, int Z) {
        for (int Index = Math.max(0, Zone - 1); Index <= Zone; Index++) {
            for (Scp1730Layout.Room Each : new Scp1730Layout.Room[]{Layouts[Index].Exit, Layouts[Index].Entry}) {
                if (Each == null || !Each.Type.startsWith("stair")) {
                    continue;
                }
                if (Index != Zone && Each != Layouts[Index].Exit) {
                    continue;
                }
                if (X >= Each.x0() - 6 && X <= Each.x1() + 6 && Z >= Each.z0() - 6 && Z <= Each.z1() + 6) {
                    return true;
                }
            }
        }
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Type().equals("seal") && Each.Zone() == Zone && Math.abs(Each.X() - X) <= 6 && Math.abs(Each.Z() - Z) <= 6) {
                return true;
            }
        }
        return false;
    }

    boolean safeAt(int X, int Z) {
        for (Scp1730Layout.Room Each : SafeRooms) {
            if (X >= Each.Bx0 && X <= Each.Bx1 && Z >= Each.Bz0 && Z <= Each.Bz1) {
                return true;
            }
        }
        return false;
    }

    void chooseSafe(int Zone) {
        String[] Wanted = switch (Zone) {
            case 0 -> new String[]{"security", "small_office", "storage", "breaker"};
            case 1 -> new String[]{"research", "small_office", "storage", "breaker"};
            case 2 -> new String[]{"armoury", "storage"};
            default -> new String[]{"collapsed"};
        };
        int Limit = Zone == 3 ? 1 : 1 + Rng.nextInt(2);
        for (String Type : Wanted) {
            for (Scp1730Layout.Room Each : Layouts[Zone].Rooms) {
                if (SafeRooms.size() >= Limit + countSafeBefore(Zone)) {
                    return;
                }
                if (Each.Type.equals(Type) && !SafeRooms.contains(Each)) {
                    SafeRooms.add(Each);
                    int Half = Math.max((Each.Bx1 - Each.Bx0 - 1) / 2, (Each.Bz1 - Each.Bz0 - 1) / 2) + 1;
                    fixture("safe", (Each.Bx0 + Each.Bx1) / 2, Each.Floor + 1, (Each.Bz0 + Each.Bz1) / 2, "north", Half, Zone);
                }
            }
        }
    }

    int countSafeBefore(int Zone) {
        int Count = 0;
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Type().equals("safe") && Each.Zone() < Zone) {
                Count++;
            }
        }
        return Count;
    }

    void markers(int Zone) {
        int F = Floors[Zone];
        for (int Tier = 0; Tier < 4; Tier++) {
            int Missing = LootFloor[Zone][Tier] - LootCount.getOrDefault(Zone * 4 + Tier, 0);
            List<int[]> Spots = new ArrayList<>();
            for (int[] Arm : ArmSpots) {
                if (Arm[3] == Zone && Arm.length > 6) {
                    Spots.add(Arm);
                }
            }
            shuffle(Spots);
            for (int[] Arm : Spots) {
                if (Missing <= 0) {
                    break;
                }
                int Dir = Arm[2];
                int Lx = Dir % 2 == 0 ? 1 : 0;
                int Lz = Dir % 2 == 0 ? 0 : 1;
                int Ax = Scp1730Layout.Dx[Dir];
                int Az = Scp1730Layout.Dz[Dir];
                for (int Side = -1; Side <= 1 && Missing > 0; Side += 2) {
                    int X = Arm[6] + Lx * Side - Ax * 2;
                    int Z = Arm[7] + Lz * Side - Az * 2;
                    if (Grid.empty(X, F + 1, Z) && Grid.empty(X, F + 2, Z) && !nearStairs(Zone, X, Z) && Grid.empty(X - Lx * Side, F + 1, Z - Lz * Side)) {
                        String Facing = facingOf(-Lx * Side, -Lz * Side);
                        Grid.set(X, F + 1, Z, Scp1730Blocks.Mod + "supply_crate[facing=" + Facing + ",open=false]");
                        loot(X, F + 1, Z, Facing, Tier, Zone);
                        Missing--;
                    }
                }
            }
        }
        for (Scp1730Fixture Each : new ArrayList<>(Fixtures)) {
            if (!Each.Type().equals("loot") || Each.Zone() != Zone || Each.Data() < 1 || Rng.nextInt(100) >= 40) {
                continue;
            }
            int Ground = Floors[Zone];
            for (int Dir = 0; Dir < 4; Dir++) {
                int X = Each.X() + Scp1730Layout.Dx[Dir];
                int Z = Each.Z() + Scp1730Layout.Dz[Dir];
                if (Scp1730SupportCheck.full(Grid.get(X, Ground, Z)) && standable(X, Ground, Z) && !cleared(X, Ground + 1, Z)) {
                    Grid.set(X, Ground + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.BodyBag, Scp1730Layout.Facings[(Dir + 1) % 4]));
                    decal(X, Ground, Z, "up", Blood, Zone);
                    break;
                }
            }
        }
        for (Scp1730Layout.Room Each : Layouts[Zone].Rooms) {
            if (Each.Type.startsWith("stair") || Each.Type.equals("sanctum") || Each.Type.equals("lobby") || SafeRooms.contains(Each)) {
                continue;
            }
            int Area = (Each.Bx1 - Each.Bx0 - 1) * (Each.Bz1 - Each.Bz0 - 1);
            int Count = Math.max(1, Math.min(3, Area / 90));
            int Placed = 0;
            for (int Try = 0; Try < 40 && Placed < Count; Try++) {
                int X = Each.Bx0 + 1 + Rng.nextInt(Math.max(1, Each.Bx1 - Each.Bx0 - 1));
                int Z = Each.Bz0 + 1 + Rng.nextInt(Math.max(1, Each.Bz1 - Each.Bz0 - 1));
                if (standable(X, F, Z) && !nearStairs(Zone, X, Z) && !inPitHole(X, Z)) {
                    fixture("spawn", X, F + 1, Z, Scp1730Layout.Facings[Rng.nextInt(4)], Zone, Zone);
                    Placed++;
                }
            }
        }
        for (int[] Spot : CeilingSpots) {
            if (Spot[3] != Zone || Rng.nextInt(100) >= 40) {
                continue;
            }
            int X = Spot[0];
            int Z = Spot[2];
            if (standable(X, F, Z) && !nearStairs(Zone, X, Z) && !safeAt(X, Z)) {
                fixture("spawn", X, F + 1, Z, Scp1730Layout.Facings[Rng.nextInt(4)], Zone, Zone);
            }
        }
        List<int[]> Arms = new ArrayList<>();
        for (int[] Arm : ArmSpots) {
            if (Arm[3] == Zone && Arm.length > 6) {
                Arms.add(Arm);
            }
        }
        shuffle(Arms);
        int Wanted = Zone == 0 ? 1 : Zone == 1 ? 2 : Zone == 2 ? 3 : 1 + Rng.nextInt(2);
        int Made = 0;
        for (int[] Arm : Arms) {
            if (Made >= Wanted) {
                break;
            }
            int Dir = Arm[2];
            int Lx = Dir % 2 == 0 ? 1 : 0;
            int Lz = Dir % 2 == 0 ? 0 : 1;
            int Ax = Scp1730Layout.Dx[Dir];
            int Az = Scp1730Layout.Dz[Dir];
            for (int Side = -1; Side <= 1 && Made < Wanted; Side += 2) {
                int X = Arm[6] + Lx * Side + Ax;
                int Z = Arm[7] + Lz * Side + Az;
                if (!Grid.empty(X, F + 1, Z) || !isWallLike(Grid.get(X + Lx * Side, F + 1, Z + Lz * Side)) || nearStairs(Zone, X, Z)) {
                    continue;
                }
                String Out = facingOf(-Lx * Side, -Lz * Side);
                Grid.set(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.BrokenVent, Out));
                fixture("breach", X, F + 1, Z, facingOf(-Ax, -Az), Made, Zone);
                Side = 2;
                decal(X + Lx * Side, F + 2, Z + Lz * Side, Out, Claws, Zone);
                decal(X, F, Z, "up", pick(new int[]{Oil, Blood, Scorch}), Zone);
                if (Grid.empty(X, F + 3, Z)) {
                    Grid.set(X, F + 3, Z, Scp1730Blocks.facing(Scp1730Blocks.AlarmLight, Out));
                }
                Made++;
            }
        }
    }

    boolean inPitHole(int X, int Z) {
        for (Scp1730Pit Each : Pits) {
            if (X >= Each.MinX() - 1 && X <= Each.MaxX() + 1 && Z >= Each.MinZ() - 1 && Z <= Each.MaxZ() + 1) {
                return true;
            }
        }
        return false;
    }

    void ensurePainter(int Zone) {
        if (Zone > 2) {
            return;
        }
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Type().equals("painter") && Each.Zone() == Zone) {
                return;
            }
        }
        for (int[] Spot : CeilingSpots) {
            if (Spot[3] == Zone) {
                fixture("painter", Spot[0], Floors[Zone] + 1, Spot[2], "north", 0, Zone);
                return;
            }
        }
    }

    int pick(int[] Options) {
        return Options[Rng.nextInt(Options.length)];
    }

    void hazards(int Zone) {
        int F = Floors[Zone];
        int[] TurretCount = {3, 5, 9, 2};
        int[] LaserCount = {1, 3, 6, 1};
        int[] CrackCount = {3, 5, 8, 6};
        int[] SymbolCount = {3, 7, 12, 4};
        List<int[]> Turrets = new ArrayList<>();
        for (int[] Each : TurretSpots) {
            if (Each[3] == Zone) {
                Turrets.add(Each);
            }
        }
        shuffle(Turrets);
        int Placed = 0;
        for (int[] Each : Turrets) {
            if (Placed >= TurretCount[Zone]) {
                break;
            }
            int X = Each[0];
            int Y = Each[1];
            int Z = Each[2];
            String Above = Grid.get(X, Y + 1, Z);
            String Here = Grid.get(X, Y, Z);
            if (Above == null || isLight(Above) || Here != null && !isLight(Here)) {
                continue;
            }
            String Facing = Scp1730Layout.Facings[Rng.nextInt(4)];
            Grid.set(X, Y, Z, Scp1730Blocks.facing(Scp1730Blocks.Turret, Facing));
            fixture("turret", X, Y, Z, Facing, 0, Zone);
            Placed++;
        }
        List<int[]> Arms = new ArrayList<>();
        for (int[] Each : ArmSpots) {
            if (Each[3] == Zone && Each.length > 6) {
                Arms.add(Each);
            }
        }
        shuffle(Arms);
        Placed = 0;
        for (int[] Each : Arms) {
            if (Placed >= LaserCount[Zone]) {
                break;
            }
            int Dir = Each[2];
            int Lx = Dir % 2 == 0 ? 1 : 0;
            int Lz = Dir % 2 == 0 ? 0 : 1;
            int Along = Dir == 1 || Dir == 2 ? 1 : -1;
            int Mx = Each[6] + (Dir % 2 == 1 ? Along : 0);
            int Mz = Each[7] + (Dir % 2 == 0 ? Along : 0);
            int Ex = Mx - Lx;
            int Ez = Mz - Lz;
            int Y = F + 1 + Rng.nextInt(2);
            if (!Grid.empty(Ex, Y, Ez) || !Grid.empty(Mx, Y, Mz) || !Grid.empty(Mx + Lx, Y, Mz + Lz)) {
                continue;
            }
            String Beam = facingOf(Lx, Lz);
            Grid.set(Ex, Y, Ez, Scp1730Blocks.with(Scp1730Blocks.Laser, "facing=" + Beam + ",powered=true"));
            fixture("laser", Ex, Y, Ez, Beam, 2, Zone);
            Placed++;
        }
        List<int[]> Walls = new ArrayList<>();
        for (int[] Each : CorridorWalls) {
            if (Each[2] == Zone) {
                Walls.add(Each);
            }
        }
        for (int[] Each : RoomWalls) {
            if (Each[2] == Zone) {
                Walls.add(Each);
            }
        }
        shuffle(Walls);
        Placed = 0;
        int SymbolsPlaced = 0;
        Set<Long> Used = new HashSet<>();
        for (int[] Each : Walls) {
            if (Placed >= CrackCount[Zone] && SymbolsPlaced >= SymbolCount[Zone]) {
                break;
            }
            int X = Each[0];
            int Z = Each[1];
            int Open = -1;
            for (int Dir = 0; Dir < 4; Dir++) {
                int Nx = X + Scp1730Layout.Dx[Dir];
                int Nz = Z + Scp1730Layout.Dz[Dir];
                if (Open < 0 && Grid.empty(Nx, F + 1, Nz) && Grid.empty(Nx, F + 2, Nz) && Grid.get(Nx, F, Nz) != null && isWallLike(Grid.get(X, F + 2, Z))) {
                    Open = Dir;
                }
            }
            if (Open < 0) {
                continue;
            }
            long Mark = key(X / 3, 0, Z / 3);
            if (Used.contains(Mark)) {
                continue;
            }
            int Nx = X + Scp1730Layout.Dx[Open];
            int Nz = Z + Scp1730Layout.Dz[Open];
            String Out = Scp1730Layout.Facings[Open];
            boolean SideWall = sideWall(Nx, F + 1, Nz, Open);
            if (Placed < CrackCount[Zone] && (SymbolsPlaced >= SymbolCount[Zone] || Rng.nextBoolean())) {
                Grid.set(X, F + 2, Z, Scp1730Blocks.facing(Scp1730Blocks.Crack, Out));
                if (Rng.nextBoolean() && isWallLike(Grid.get(X, F + 1, Z))) {
                    Grid.set(X, F + 1, Z, Scp1730Blocks.facing(Scp1730Blocks.Crack, Out));
                    fixture("crack", X, F + 1, Z, Out, 0, Zone);
                }
                fixture("crack", X, F + 2, Z, Out, 0, Zone);
                decal(Nx, F, Nz, "up", Oil, Zone);
                Used.add(Mark);
                Placed++;
            } else if (SymbolsPlaced < SymbolCount[Zone] && SideWall && Grid.empty(Nx, F + 2, Nz)) {
                int Glyph = Rng.nextInt(12);
                Grid.set(Nx, F + 2, Nz, Scp1730Blocks.with(Scp1730Blocks.Symbol, "facing=" + Out + ",glyph=" + Glyph));
                fixture("symbol", Nx, F + 2, Nz, Out, Glyph, Zone);
                Used.add(Mark);
                SymbolsPlaced++;
            }
        }
    }

    boolean sideWall(int X, int Y, int Z, int Open) {
        int Lx = Open % 2 == 0 ? 1 : 0;
        int Lz = Open % 2 == 0 ? 0 : 1;
        int Run = 0;
        for (int Side = -1; Side <= 1; Side += 2) {
            for (int Step = 1; Step <= 3; Step++) {
                if (Grid.empty(X + Lx * Side * Step, Y, Z + Lz * Side * Step)) {
                    Run++;
                } else {
                    break;
                }
            }
        }
        int Across = 0;
        for (int Step = 1; Step <= 6; Step++) {
            if (Grid.empty(X + Scp1730Layout.Dx[Open] * Step, Y, Z + Scp1730Layout.Dz[Open] * Step)) {
                Across++;
            } else {
                break;
            }
        }
        return Run >= 4 && Across <= 4;
    }

    static boolean isLight(String State) {
        return State != null && (State.contains("light") || State.contains("lamp"));
    }

    static boolean backed(String State) {
        return isWallLike(State) || State != null && (State.endsWith("_wall_top") || State.endsWith("_pillar"));
    }

    static boolean isWallLike(String State) {
        return State != null && (State.endsWith("_wall_middle") || State.endsWith("_wall_bottom"));
    }

    <T> void shuffle(List<T> Items) {
        for (int Index = Items.size() - 1; Index > 0; Index--) {
            int Swap = Rng.nextInt(Index + 1);
            T Held = Items.get(Index);
            Items.set(Index, Items.get(Swap));
            Items.set(Swap, Held);
        }
    }
}
