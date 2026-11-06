package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scp1730AnomalousCheck {
    private Scp1730AnomalousCheck() {
    }

    public static List<String> check(long Seed) {
        return check(Scp1730Plan.fresh(Seed));
    }

    public static List<String> check(Scp1730Plan Plan) {
        List<String> Problems = new ArrayList<>();
        Map<String, Integer> Kinds = new HashMap<>();
        int[] PerZone = new int[4];
        int Total = 0;
        Scp1730RoomInfo Hall = null;
        Scp1730RoomInfo Vault = null;
        for (Scp1730RoomInfo Room : Plan.rooms()) {
            if (Room.Type().equals(Scp1730AnomalousRooms.VaultHall)) {
                Hall = Room;
                continue;
            }
            if (!Scp1730AnomalousRooms.Kinds.contains(Room.Type())) {
                continue;
            }
            Total++;
            PerZone[Room.Zone()]++;
            int Seen = Kinds.merge(Room.Type(), 1, Integer::sum);
            if (Seen > 1) {
                Problems.add(Room.Type() + " appears more than once");
            }
            if (Room.Zone() == 3) {
                Problems.add(Room.Type() + " placed in the deep zone");
            }
            if (Room.Type().equals(Scp1730AnomalousRooms.Vault)) {
                Vault = Room;
            }
            for (String Kind : new String[]{"safe", "control", "card", "seal", "setpiece", "thresher", "guardian"}) {
                for (Scp1730Fixture Each : Plan.fixtures(Kind, Room.Zone())) {
                    if (inside(Room, Each.X(), Each.Y(), Each.Z())) {
                        Problems.add(Room.Type() + " holds a " + Kind + " fixture");
                    }
                }
            }
            if (Room.Type().equals(Scp1730AnomalousRooms.Television)) {
                boolean Found = false;
                for (Scp1730Fixture Each : Plan.fixtures("television", Room.Zone())) {
                    Found |= inside(Room, Each.X(), Each.Y(), Each.Z()) && Plan.stateAt(Each.X(), Each.Y(), Each.Z()) != null
                            && Plan.stateAt(Each.X(), Each.Y(), Each.Z()).startsWith(Scp1730Blocks.Mod + "scp1730_television");
                }
                if (!Found) {
                    Problems.add("television room has no television");
                }
            }
            Problems.addAll(bypass(Plan, Room));
        }
        if (Total > Scp1730AnomalousRooms.MaxRooms) {
            Problems.add("too many anomalous rooms: " + Total);
        }
        for (int Zone = 0; Zone < 4; Zone++) {
            if (PerZone[Zone] > Scp1730AnomalousRooms.PerZone) {
                Problems.add("zone " + Zone + " holds " + PerZone[Zone] + " anomalous rooms");
            }
        }
        if (Vault != null && Hall == null) {
            Problems.add("vault room without a hall");
        }
        if (Hall != null) {
            if (Vault == null) {
                Problems.add("hall without a vault room");
            }
            Problems.addAll(pocket(Plan, Hall));
        }
        return Problems;
    }

    static boolean inside(Scp1730RoomInfo Room, int X, int Y, int Z) {
        return X >= Room.MinX() && X <= Room.MaxX() && Z >= Room.MinZ() && Z <= Room.MaxZ() && Y >= Room.Floor() && Y <= Room.Floor() + Room.Height() + 1;
    }

    static List<String> bypass(Scp1730Plan Plan, Scp1730RoomInfo Room) {
        List<String> Problems = new ArrayList<>();
        int Zone = Room.Zone();
        Scp1730Spot Entry = Plan.entry(Zone);
        if (Entry == null) {
            return Problems;
        }
        Scp1730PlanCheck.Walker Around = new Scp1730PlanCheck.Walker(Plan, Plan.floorY(Zone) - 2, Plan.topY(Zone) + 1, false, Room);
        Around.flood((int) Math.floor(Entry.X()), (int) Math.floor(Entry.Y()), (int) Math.floor(Entry.Z()));
        for (Scp1730Fixture Each : Plan.fixtures("control", Zone)) {
            if (!Around.near(Each.X(), Plan.floorY(Zone) + 1, Each.Z(), 2)) {
                Problems.add("zone " + Zone + ": the " + Room.Type() + " room is the only route to control " + Each.Data());
            }
        }
        for (Scp1730Fixture Each : Plan.fixtures("card", Zone)) {
            if (!Around.near(Each.X(), Plan.floorY(Zone) + 1, Each.Z(), 2)) {
                Problems.add("zone " + Zone + ": the " + Room.Type() + " room is the only route to the card");
            }
        }
        List<Scp1730Fixture> Seals = Plan.fixtures("seal", Zone);
        if (Seals.size() == 9) {
            Scp1730Fixture Middle = Seals.get(4);
            int Ox = Scp1730PlanCheck.dx(Middle.Facing());
            int Oz = Scp1730PlanCheck.dz(Middle.Facing());
            if (!Around.near(Middle.X() + Ox, Plan.floorY(Zone) + 1, Middle.Z() + Oz, 1)) {
                Problems.add("zone " + Zone + ": the " + Room.Type() + " room is the only route to the seal");
            }
        }
        return Problems;
    }

    static List<String> pocket(Scp1730Plan Plan, Scp1730RoomInfo Hall) {
        List<String> Problems = new ArrayList<>();
        int[] Door = Hall.Doors().getFirst();
        int Ox = Scp1730Layout.Dx[Door[2]];
        int Oz = Scp1730Layout.Dz[Door[2]];
        int Lx = Door[2] % 2 == 0 ? 1 : 0;
        int Lz = Door[2] % 2 == 0 ? 0 : 1;
        int Low = Hall.Floor() - 1;
        int High = Hall.Floor() + Hall.Height() + 2;
        for (Scp1730RoomInfo Other : Plan.rooms()) {
            if (Other == Hall) {
                continue;
            }
            boolean Across = Other.MaxX() >= Hall.MinX() - 1 && Other.MinX() <= Hall.MaxX() + 1 && Other.MaxZ() >= Hall.MinZ() - 1 && Other.MinZ() <= Hall.MaxZ() + 1;
            boolean Through = Other.Floor() <= High && Other.Floor() + Other.Height() + 1 >= Low;
            if (Across && Through) {
                Problems.add("vault hall overlaps the " + Other.Type() + " room");
            }
        }
        if (Plan.zoneAt(Hall.MinX() + 2, Hall.Floor() + 1, Hall.MinZ() + 2) != Hall.Zone()) {
            Problems.add("vault hall sits outside its zone band");
        }
        for (int X = Hall.MinX() - 1; X <= Hall.MaxX() + 1; X++) {
            for (int Z = Hall.MinZ() - 1; Z <= Hall.MaxZ() + 1; Z++) {
                boolean Ring = X < Hall.MinX() || X > Hall.MaxX() || Z < Hall.MinZ() || Z > Hall.MaxZ();
                for (int Y = Low; Y <= High; Y++) {
                    boolean Shell = Y == Low || Y == High;
                    if (!Ring && !Shell) {
                        continue;
                    }
                    int Along = (X - Door[0]) * Ox + (Z - Door[1]) * Oz;
                    int Side = Math.abs((X - Door[0]) * Lx + (Z - Door[1]) * Lz);
                    boolean Stub = Along >= 0 && Along <= Scp1730AnomalousRooms.StubDepth + 1 && Side <= 3 && Y >= Hall.Floor() && Y <= Hall.Floor() + 4;
                    if (!Stub && Plan.stateAt(X, Y, Z) != null) {
                        Problems.add("vault hall pocket is not clear at " + X + "," + Y + "," + Z);
                        return Problems;
                    }
                }
            }
        }
        return Problems;
    }
}
