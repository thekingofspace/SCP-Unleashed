package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Scp1730Plan {
    public static final int Width = 160;
    public static final int Depth = 160;
    public static final int CacheSize = 3;
    private static final Map<Long, Scp1730Plan> Cache = new LinkedHashMap<>(8, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Scp1730Plan> Eldest) {
            return size() > CacheSize;
        }
    };

    private final long Seed;
    private final Scp1730Grid Grid;
    private final List<Scp1730Zone> Zones;
    private final List<Scp1730Fixture> Fixtures;
    private final List<Scp1730Pit> Pits;
    private final List<Scp1730Basin> Basins;
    private final Scp1730Spot Arrival;
    private final int[] Floors;
    private final int[] Tops;
    private final List<Scp1730RoomInfo> RoomInfos;

    private Scp1730Plan(long Seed) {
        this(Seed, Scp1730Builder.of(Seed));
    }

    Scp1730Plan(long Seed, Scp1730Builder Made) {
        this.Seed = Seed;
        this.Grid = Made.Grid;
        this.Zones = Collections.unmodifiableList(new ArrayList<>(Made.Zones));
        this.Fixtures = Collections.unmodifiableList(new ArrayList<>(Made.Fixtures));
        this.Pits = Collections.unmodifiableList(new ArrayList<>(Made.Pits));
        this.Basins = Collections.unmodifiableList(new ArrayList<>(Made.Basins));
        this.Arrival = Made.Arrival;
        this.Floors = Scp1730Builder.Floors.clone();
        this.Tops = Made.ZoneTop.clone();
        this.RoomInfos = Collections.unmodifiableList(new ArrayList<>(Made.RoomInfos));
    }

    public static Scp1730Plan of(long Seed) {
        synchronized (Cache) {
            Scp1730Plan Found = Cache.get(Seed);
            if (Found == null) {
                Found = new Scp1730Plan(Seed);
                Cache.put(Seed, Found);
            }
            return Found;
        }
    }

    public static Scp1730Plan fresh(long Seed) {
        return new Scp1730Plan(Seed);
    }

    public long seed() {
        return Seed;
    }

    public void paint(int MinX, int MinZ, int MaxX, int MaxZ, Scp1730Canvas Canvas) {
        Grid.paint(MinX, MinZ, MaxX, MaxZ, Canvas);
    }

    public String stateAt(int X, int Y, int Z) {
        return Grid.get(X, Y, Z);
    }

    public List<Scp1730Zone> zones() {
        return Zones;
    }

    public Scp1730Zone zone(int Index) {
        return Zones.get(Index);
    }

    public List<Scp1730Fixture> fixtures() {
        return Fixtures;
    }

    public List<Scp1730Fixture> fixtures(String Type) {
        List<Scp1730Fixture> Found = new ArrayList<>();
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Type().equals(Type)) {
                Found.add(Each);
            }
        }
        return Found;
    }

    public List<Scp1730Fixture> fixtures(String Type, int Zone) {
        List<Scp1730Fixture> Found = new ArrayList<>();
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Type().equals(Type) && Each.Zone() == Zone) {
                Found.add(Each);
            }
        }
        return Found;
    }

    public List<Scp1730RoomInfo> rooms() {
        return RoomInfos;
    }

    public Scp1730RoomInfo roomAt(int X, int Y, int Z) {
        for (Scp1730RoomInfo Each : RoomInfos) {
            if (Each.contains(X, Y, Z)) {
                return Each;
            }
        }
        return null;
    }

    public List<Scp1730Pit> pits() {
        return Pits;
    }

    public List<Scp1730Basin> basins() {
        return Basins;
    }

    public List<Scp1730RoomInfo> rooms(String Type) {
        List<Scp1730RoomInfo> Found = new ArrayList<>();
        for (Scp1730RoomInfo Each : RoomInfos) {
            if (Each.Type().equals(Type)) {
                Found.add(Each);
            }
        }
        return Found;
    }

    public Scp1730RoomInfo wingAt(int X, int Y, int Z) {
        for (Scp1730RoomInfo Each : RoomInfos) {
            if (Each.Type().equals(Scp1730Sectors.BioWing) && Each.contains(X, Y, Z)) {
                return Each;
            }
        }
        return null;
    }

    public boolean inWing(int X, int Y, int Z) {
        return wingAt(X, Y, Z) != null;
    }

    public boolean wingSeal(int X, int Y, int Z) {
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Type().equals("bioseal") && Each.X() == X && Each.Y() == Y && Each.Z() == Z) {
                return true;
            }
        }
        return false;
    }

    public Scp1730Spot arrival() {
        return Arrival;
    }

    public Scp1730Spot entry(int Zone) {
        return Zones.get(Zone).Entry();
    }

    public int floorY(int Zone) {
        return Floors[Zone];
    }

    public int topY(int Zone) {
        return Tops[Zone];
    }

    public Scp1730Spot thresher() {
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Type().equals("thresher")) {
                return new Scp1730Spot(Each.X() + 0.5, Each.Y(), Each.Z() + 0.5, yawOf(Each.Facing()));
            }
        }
        return null;
    }

    public Scp1730Spot guardian() {
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Type().equals("guardian")) {
                return new Scp1730Spot(Each.X() + 0.5, Each.Y(), Each.Z() + 0.5, yawOf(Each.Facing()));
            }
        }
        return null;
    }

    public List<Scp1730Spot> painterSpawns() {
        List<Scp1730Spot> Found = new ArrayList<>();
        for (Scp1730Fixture Each : Fixtures) {
            if (Each.Type().equals("painter")) {
                Found.add(new Scp1730Spot(Each.X() + 0.5, Each.Y(), Each.Z() + 0.5, 0.0f));
            }
        }
        return Found;
    }

    public int zoneAt(int X, int Y, int Z) {
        if (X < 0 || Z < 0 || X >= Width || Z >= Depth) {
            return -1;
        }
        for (Scp1730Pit Each : Pits) {
            if (X >= Each.MinX() && X <= Each.MaxX() && Z >= Each.MinZ() && Z <= Each.MaxZ() && Y >= Each.BottomY() - 1 && Y < Each.RimY()) {
                return Each.Zone();
            }
        }
        for (Scp1730Basin Each : Basins) {
            if (X >= Each.MinX() && X <= Each.MaxX() && Z >= Each.MinZ() && Z <= Each.MaxZ() && Y >= Each.BottomY() - 1 && Y <= Each.TopY()) {
                return Each.Zone();
            }
        }
        if (Y > Tops[0] + 2) {
            return -1;
        }
        for (int Index = 0; Index < Floors.length; Index++) {
            if (Y >= Floors[Index] - 1) {
                return Index;
            }
        }
        return -1;
    }

    public boolean inPit(int X, int Y, int Z) {
        for (Scp1730Pit Each : Pits) {
            if (Each.lethal(X, Y, Z)) {
                return true;
            }
        }
        return false;
    }

    public List<String> palette() {
        return Grid.palette();
    }

    public int sectionCount() {
        return Grid.sectionCount();
    }

    public long checksum() {
        return Grid.checksum();
    }

    public static String decalKind(int Data) {
        int Kind = Data >> 4;
        return Kind >= 0 && Kind < Scp1730Builder.DecalKinds.length ? Scp1730Builder.DecalKinds[Kind] : "grime";
    }

    public static int decalVariant(int Data) {
        return Math.max(1, Data & 15);
    }

    public static float yawOf(String Facing) {
        return switch (Facing) {
            case "north" -> 180.0f;
            case "east" -> -90.0f;
            case "west" -> 90.0f;
            default -> 0.0f;
        };
    }
}
