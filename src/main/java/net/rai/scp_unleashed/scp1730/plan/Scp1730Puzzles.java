package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Scp1730Puzzles {
    public static final String[] Ids = {"keycard", "fuses", "override", "dualkey", "vent", "splice", "relays", "sample", "cells", "generator", "coolant", "powercycle", "pylons", "sluices"};
    public static final int Keycard = 0;
    public static final int Fuses = 1;
    public static final int Override = 2;
    public static final int DualKey = 3;
    public static final int Vent = 4;
    public static final int Splice = 5;
    public static final int Relays = 6;
    public static final int Sample = 7;
    public static final int Cells = 8;
    public static final int Generator = 9;
    public static final int Coolant = 10;
    public static final int PowerCycle = 11;
    public static final int Pylons = 12;
    public static final int Sluices = 13;
    public static final int[][] Pools = {{Keycard, Fuses, Override, DualKey}, {Vent, Splice, Relays, Sample}, {Cells, Generator, Coolant, PowerCycle}, {Pylons, Sluices}};
    public static final int[] Steps = {2, 3, 2, 2, 3, 2, 3, 2, 3, 3, 3, 3, 3, 2};
    public static final int Zones = 4;
    public static final int DeepZone = 3;
    public static final int Cranks = 8;
    public static final long Salt = 0x1730B055L;

    public static final int RolePanel = 0;
    public static final int RoleJunction = 1;
    public static final int RoleTerminalA = 2;
    public static final int RoleTerminalB = 3;
    public static final int RoleClueA = 4;
    public static final int RoleClueB = 5;
    public static final int RoleKeyA = 6;
    public static final int RoleKeyB = 7;
    public static final int RoleInterlock = 8;
    public static final int RoleVent = 9;
    public static final int RoleVentInner = 10;
    public static final int RoleFan = 11;
    public static final int RoleCableA = 12;
    public static final int RoleCableB = 13;
    public static final int RoleRelayRed = 14;
    public static final int RoleRelayYellow = 15;
    public static final int RoleRelayBlue = 16;
    public static final int RoleChart = 17;
    public static final int RoleHatch = 18;
    public static final int RoleFlush = 19;
    public static final int RoleLockA = 20;
    public static final int RoleLockB = 21;
    public static final int RoleLockC = 22;
    public static final int RoleGenerator = 23;
    public static final int RoleInflow = 24;
    public static final int RoleBypass = 25;
    public static final int RoleDrain = 26;
    public static final int RoleBreaker = 27;
    public static final int RoleController = 28;
    public static final int RoleLockerReader = 29;
    public static final int RolePylonA = 30;
    public static final int RolePylonB = 31;
    public static final int RolePylonC = 32;
    public static final int RoleTabletA = 33;
    public static final int RoleTabletB = 34;
    public static final int RoleTabletC = 35;
    public static final int RoleSluiceA = 36;
    public static final int RoleSluiceB = 37;

    public static final int ItemFuse = 0;
    public static final int ItemOverride = 1;
    public static final int ItemKey = 2;
    public static final int ItemSplice = 3;
    public static final int ItemSample = 4;
    public static final int ItemFuel = 5;
    public static final String[] Items = {"fuse", "override_card", "duty_key", "splice_kit", "sample", "fuel"};
    public static final int Glyphs = 6;
    public static final int[][] Orders = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
    public static final int ChartBase = Glyphs;
    public static final int[][] Roles = {{RoleLockerReader}, {RoleJunction}, {RoleTerminalA, RoleTerminalB, RoleClueA, RoleClueB}, {RoleKeyA, RoleKeyB},
            {RoleInterlock, RoleVent, RoleVentInner, RoleFan}, {RoleCableA, RoleCableB}, {RoleChart, RoleRelayRed, RoleRelayYellow, RoleRelayBlue}, {RoleHatch, RoleFlush},
            {RoleLockA, RoleLockB, RoleLockC}, {RoleGenerator}, {RoleInflow, RoleBypass, RoleDrain}, {RoleBreaker, RoleController},
            {RolePylonA, RolePylonB, RolePylonC, RoleTabletA, RoleTabletB, RoleTabletC}, {RoleSluiceA, RoleSluiceB}};
    public static final int[][] Needs = {{ItemOverride, 1}, {ItemFuse, 3}, {}, {ItemKey, 1}, {}, {ItemSplice, 2}, {}, {ItemSample, 1}, {}, {ItemFuel, 2}, {}, {}, {}, {}};

    private Scp1730Puzzles() {
    }

    public static int zoneOf(int Puzzle) {
        return Puzzle >= 0 && Puzzle < Ids.length ? Puzzle / 4 : -1;
    }

    public static int steps(int Puzzle) {
        return Puzzle >= 0 && Puzzle < Steps.length ? Steps[Puzzle] : 2;
    }

    public static int role(int Data) {
        return Data & 63;
    }

    public static int extra(int Data) {
        return Data >> 6;
    }

    public static int data(int Role, int Extra) {
        return Role | Extra << 6;
    }

    public static int item(int Data) {
        return Data >> 4;
    }

    public static int pickup(int Item, int Index) {
        return Item << 4 | Index & 15;
    }

    public static List<Integer> order(long Seed, int Zone) {
        Random Pick = new Random(Scp1730Builder.mix(Seed ^ Salt, Zone + 5));
        List<Integer> Made = new ArrayList<>();
        int[] Pool = Pools[Zone];
        int First = Pick.nextInt(Pool.length);
        for (int Index = 0; Index < Pool.length; Index++) {
            Made.add(Pool[(First + Index) % Pool.length]);
        }
        return Made;
    }

    public static int puzzleOf(Scp1730Plan Plan, int Zone) {
        for (Scp1730Fixture Each : Plan.fixtures("puzzle", Zone)) {
            return Each.Data();
        }
        return -1;
    }

    public static Scp1730Fixture device(Scp1730Plan Plan, int Zone, int Role) {
        for (Scp1730Fixture Each : Plan.fixtures("pdevice", Zone)) {
            if (role(Each.Data()) == Role) {
                return Each;
            }
        }
        return null;
    }

    public static Scp1730Fixture panel(Scp1730Plan Plan, int Zone) {
        for (Scp1730Fixture Each : Plan.fixtures("puzzle", Zone)) {
            return Each;
        }
        return null;
    }
}
