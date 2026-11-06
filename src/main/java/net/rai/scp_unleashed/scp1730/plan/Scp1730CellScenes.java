package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.List;

final class Scp1730CellScenes {
    private static final String M = Scp1730Blocks.Mod;
    public static final String HazardSign = M + "containment_room_hazard_sign";
    public static final String Extinguisher = M + "fire_extinguisher";

    record Cell(int StartX, int StartZ, int Ix, int Iz, int Lx, int Lz, int Span0, int Span1, int Centre, int Depth, int WallDepth, int Px, int Pz, String Toward,
                String Back) {
        int x(int D, int A) {
            return Ix != 0 ? StartX + Ix * D : A;
        }

        int z(int D, int A) {
            return Ix != 0 ? A : StartZ + Iz * D;
        }

        int inner() {
            return WallDepth + 1;
        }

        int spot() {
            return Ix != 0 ? (Px - StartX) * Ix : (Pz - StartZ) * Iz;
        }

        int depthOf(int X, int Z) {
            return Ix != 0 ? (X - StartX) * Ix : (Z - StartZ) * Iz;
        }

        String side(int Sign) {
            if (Lx != 0) {
                return Sign > 0 ? "east" : "west";
            }
            return Sign > 0 ? "south" : "north";
        }
    }

    private Scp1730CellScenes() {
    }

    static void dress(Scp1730Builder B, Scp1730Rooms.Box R, String Scene, Cell C) {
        clearApproach(B, R, C);
        switch (Scene) {
            case Scp1730SetPieces.PlagueDoctor -> plagueDoctor(B, R, C);
            case Scp1730SetPieces.Hive -> hive(B, R, C);
            case Scp1730SetPieces.Incinerator -> incinerator(B, R, C);
            case Scp1730SetPieces.BeastPen -> beastPen(B, R, C);
            case Scp1730SetPieces.StatueCell -> statueCell(B, R, C);
            case Scp1730SetPieces.DarkCell -> darkCell(B, R, C);
            default -> {
            }
        }
    }

    private static void clearApproach(Scp1730Builder B, Scp1730Rooms.Box R, Cell C) {
        for (int D = C.inner(); D <= Math.min(C.Depth(), C.spot() + 1); D++) {
            for (int A = C.Centre() - 1; A <= C.Centre() + 1; A++) {
                for (int Level = 1; Level <= 2; Level++) {
                    B.keepClear(C.x(D, A), R.F + Level, C.z(D, A));
                }
            }
        }
    }

    private static List<int[]> floorCells(Scp1730Builder B, Scp1730Rooms.Box R, Cell C) {
        List<int[]> Found = new ArrayList<>();
        for (int D = C.inner(); D <= C.Depth(); D++) {
            for (int A = C.Span0(); A <= C.Span1(); A++) {
                int X = C.x(D, A);
                int Z = C.z(D, A);
                if (R.free(X, R.F + 1, Z)) {
                    Found.add(new int[]{X, Z});
                }
            }
        }
        B.shuffle(Found);
        return Found;
    }

    private static String randomFacing(Scp1730Builder B) {
        return Scp1730Layout.Facings[B.Rng.nextInt(4)];
    }

    private static boolean mount(Scp1730Builder B, Scp1730Rooms.Box R, int X, int Y, int Z, String State, String Facing) {
        if (!R.free(X, Y, Z) || !Scp1730AnomalousRooms.backed(B, X, Y, Z, Facing)) {
            return false;
        }
        B.Grid.set(X, Y, Z, State);
        return true;
    }

    private static void scatter(Scp1730Builder B, Scp1730Rooms.Box R, Cell C, String Base, int Count, boolean Facing) {
        List<int[]> Cells = floorCells(B, R, C);
        for (int Index = 0; Index < Count && Index < Cells.size(); Index++) {
            int[] Each = Cells.get(Index);
            R.put(Each[0], R.F + 1, Each[1], Facing ? Scp1730Blocks.facing(Base, randomFacing(B)) : Scp1730Blocks.facing(Base, "up"));
        }
    }

    private static void floorMarks(Scp1730Builder B, Scp1730Rooms.Box R, Cell C, int[] Kinds, int Count) {
        for (int Index = 0; Index < Count; Index++) {
            int D = C.inner() + B.Rng.nextInt(Math.max(1, C.Depth() - C.inner() + 1));
            int A = C.Span0() + B.Rng.nextInt(C.Span1() - C.Span0() + 1);
            B.decal(C.x(D, A), R.F, C.z(D, A), "up", B.pick(Kinds), R.Zone);
        }
    }

    private static void wallMarks(Scp1730Builder B, Scp1730Rooms.Box R, Cell C, int[] Kinds, int Count) {
        for (int Index = 0; Index < Count; Index++) {
            int Level = 1 + B.Rng.nextInt(Math.min(3, R.H));
            int Pick = B.Rng.nextInt(3);
            if (Pick == 0) {
                int A = C.Span0() + B.Rng.nextInt(C.Span1() - C.Span0() + 1);
                B.decal(C.x(C.Depth() + 1, A), R.F + Level, C.z(C.Depth() + 1, A), C.Back(), B.pick(Kinds), R.Zone);
            } else {
                int D = C.inner() + B.Rng.nextInt(Math.max(1, C.Depth() - C.inner() + 1));
                int A = Pick == 1 ? C.Span0() - 1 : C.Span1() + 1;
                B.decal(C.x(D, A), R.F + Level, C.z(D, A), C.side(Pick == 1 ? 1 : -1), B.pick(Kinds), R.Zone);
            }
        }
    }

    private static void ceilingCables(Scp1730Builder B, Scp1730Rooms.Box R, Cell C, int Count) {
        for (int Index = 0; Index < Count; Index++) {
            int D = C.inner() + B.Rng.nextInt(Math.max(1, C.Depth() - C.inner() + 1));
            int A = C.Span0() + B.Rng.nextInt(C.Span1() - C.Span0() + 1);
            int X = C.x(D, A);
            int Z = C.z(D, A);
            if (B.Grid.empty(X, R.F + R.H, Z) && B.Grid.get(X, R.F + R.H + 1, Z) != null) {
                B.Grid.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Scp1730Blocks.HangingCables, "down"));
            }
        }
    }

    private static void signs(Scp1730Builder B, Scp1730Rooms.Box R, Cell C, String Warning, int Variant) {
        int Sx = C.x(C.WallDepth() - 1, C.Centre() + 3);
        int Sz = C.z(C.WallDepth() - 1, C.Centre() + 3);
        String Current = B.Grid.get(Sx, R.F + 3, Sz);
        if (Current != null && Current.startsWith(M + "scp1730_sign_")) {
            B.Grid.set(Sx, R.F + 3, Sz, Scp1730Blocks.facing(Warning, C.Back()));
        }
        int Hx = C.x(C.WallDepth() - 1, C.Centre() - 2);
        int Hz = C.z(C.WallDepth() - 1, C.Centre() - 2);
        mount(B, R, Hx, R.F + 3, Hz, HazardSign + "[facing=" + C.Back() + ",half=left,variant=" + Variant + "]", C.Back());
    }

    private static boolean bed(Scp1730Builder B, Scp1730Rooms.Box R, Cell C, int A) {
        int Hx = C.x(C.Depth(), A);
        int Hz = C.z(C.Depth(), A);
        int Fx = C.x(C.Depth() - 1, A);
        int Fz = C.z(C.Depth() - 1, A);
        if (!R.free(Hx, R.F + 1, Hz) || !R.free(Fx, R.F + 1, Fz)) {
            return false;
        }
        B.Grid.set(Hx, R.F + 1, Hz, M + "hospital_bed[facing=" + C.Toward() + ",part=head]");
        B.Grid.set(Fx, R.F + 1, Fz, M + "hospital_bed[facing=" + C.Toward() + ",part=foot]");
        return true;
    }

    private static void plagueDoctor(Scp1730Builder B, Scp1730Rooms.Box R, Cell C) {
        signs(B, R, C, Scp1730Blocks.SignBiohazard, 2);
        int Back = C.Depth();
        if (bed(B, R, C, C.Centre() - 4)) {
            R.tall(C.x(Back, C.Centre() - 5), C.z(Back, C.Centre() - 5), M + "iv_stand", "facing=" + C.Back());
            R.put(C.x(Back, C.Centre() - 3), R.F + 1, C.z(Back, C.Centre() - 3), M + "vitals_monitor[facing=" + C.Back() + ",lit=true]");
        }
        R.put(C.x(Back - 1, C.Centre() + 4), R.F + 1, C.z(Back - 1, C.Centre() + 4), Scp1730Blocks.facing(Scp1730Blocks.Gurney, C.side(1)));
        R.put(C.x(Back, C.Centre() + 2), R.F + 1, C.z(Back, C.Centre() + 2), M + "medical_cart[facing=" + C.Back() + "]");
        R.put(C.x(Back, C.Span1()), R.F + 1, C.z(Back, C.Span1()), M + "medical_cabinet[facing=" + C.Back() + ",open=true]");
        R.put(C.x(C.inner() + 1, C.Span1()), R.F + 1, C.z(C.inner() + 1, C.Span1()), M + "medical_supply_shelf[facing=" + C.side(-1) + "]");
        R.put(C.x(C.inner(), C.Span0()), R.F + 1, C.z(C.inner(), C.Span0()), M + "biohazard_bin[facing=" + C.side(1) + "]");
        R.put(C.x(C.inner() + 2, C.Span0()), R.F + 1, C.z(C.inner() + 2, C.Span0()), M + "medical_cabinet[facing=" + C.side(1) + ",open=true]");
        List<int[]> Cells = floorCells(B, R, C);
        for (int Index = 0; Index < 3 && Index < Cells.size(); Index++) {
            int[] Each = Cells.get(Index);
            R.put(Each[0], R.F + 1, Each[1], Scp1730Blocks.PaperStack + "[facing=" + randomFacing(B) + ",variant=" + B.Rng.nextInt(6) + "]");
        }
        floorMarks(B, R, C, new int[]{Scp1730Builder.Footprints, Scp1730Builder.Footprints, Scp1730Builder.Grime, Scp1730Builder.Puddle}, 8);
        wallMarks(B, R, C, new int[]{Scp1730Builder.Grime, Scp1730Builder.Rust}, 3);
    }

    private static void hive(Scp1730Builder B, Scp1730Rooms.Box R, Cell C) {
        signs(B, R, C, Scp1730Blocks.SignBiohazard, 1);
        int Back = C.Depth();
        for (int Sign = -1; Sign <= 1; Sign += 2) {
            int A = C.Centre() + Sign * 3;
            mount(B, R, C.x(Back, A), R.F + 1, C.z(Back, A), Scp1730Blocks.facing(Scp1730Blocks.BrokenVent, C.Back()), C.Back());
        }
        mount(B, R, C.x(C.inner() + 2, C.Span0()), R.F + 1, C.z(C.inner() + 2, C.Span0()), Scp1730Blocks.facing(Scp1730Blocks.BrokenVent, C.side(1)), C.side(1));
        for (int A : new int[]{C.Span0(), C.Span1()}) {
            if (R.put(C.x(Back, A), R.F + 1, C.z(Back, A), M + "containment_room_specimen_tank[facing=" + C.Back() + ",part=single]")) {
                R.put(C.x(Back - 1, A), R.F + 1, C.z(Back - 1, A), Scp1730Blocks.facing(Scp1730Blocks.GlassShards, "up"));
            }
        }
        R.put(C.x(C.inner(), C.Span1()), R.F + 1, C.z(C.inner(), C.Span1()), M + "supply_crate[facing=" + C.side(-1) + ",open=true]");
        scatter(B, R, C, Scp1730Blocks.MouldPatch, 12, false);
        scatter(B, R, C, Scp1730Blocks.Rubble, 3, true);
        floorMarks(B, R, C, new int[]{Scp1730Builder.Grime, Scp1730Builder.Corrosion, Scp1730Builder.Claws}, 8);
        wallMarks(B, R, C, new int[]{Scp1730Builder.Grime, Scp1730Builder.Corrosion, Scp1730Builder.Claws}, 6);
        ceilingCables(B, R, C, 2);
    }

    private static void darken(Scp1730Builder B, Scp1730Rooms.Box R, Cell C, boolean Whole) {
        for (int X = R.X0 + 2; X <= R.X1; X += 5) {
            for (int Z = R.Z0 + 2; Z <= R.Z1; Z += 5) {
                if ((Whole || C.depthOf(X, Z) > C.WallDepth()) && B.Grid.empty(X, R.F + R.H, Z) && B.Grid.get(X, R.F + R.H + 1, Z) != null) {
                    B.Grid.set(X, R.F + R.H, Z, Scp1730Blocks.facing(Scp1730Blocks.DeadLight, "down"));
                }
            }
        }
    }

    private static void incinerator(Scp1730Builder B, Scp1730Rooms.Box R, Cell C) {
        signs(B, R, C, Scp1730Blocks.SignKeter, 0);
        darken(B, R, C, false);
        for (int Sign = -1; Sign <= 1; Sign += 2) {
            int A = C.Centre() + Sign * 4;
            mount(B, R, C.x(C.WallDepth() - 1, A), R.F + 2, C.z(C.WallDepth() - 1, A), Extinguisher + "[facing=" + C.Back() + ",wall=true]", C.Back());
        }
        R.put(C.x(C.Depth(), C.Span0() + 1), R.F + 1, C.z(C.Depth(), C.Span0() + 1), Scp1730Blocks.facing(Scp1730Blocks.OverturnedDesk, C.side(1)));
        scatter(B, R, C, Scp1730Blocks.Rubble, 4, true);
        scatter(B, R, C, Scp1730Blocks.FallenPanel, 2, true);
        scatter(B, R, C, Scp1730Blocks.CableTangle, 1, true);
        floorMarks(B, R, C, new int[]{Scp1730Builder.Scorch, Scp1730Builder.Scorch, Scp1730Builder.Scorch, Scp1730Builder.Grime}, 18);
        wallMarks(B, R, C, new int[]{Scp1730Builder.Scorch, Scp1730Builder.Scorch, Scp1730Builder.Grime}, 12);
        for (int Index = 0; Index < 3; Index++) {
            int D = 1 + B.Rng.nextInt(Math.max(1, C.WallDepth() - 1));
            int A = C.Centre() - 2 + B.Rng.nextInt(5);
            B.decal(C.x(D, A), R.F, C.z(D, A), "up", Scp1730Builder.Scorch, R.Zone);
        }
        ceilingCables(B, R, C, 2);
    }

    private static void beastPen(Scp1730Builder B, Scp1730Rooms.Box R, Cell C) {
        signs(B, R, C, Scp1730Blocks.SignKeter, 2);
        int Back = C.Depth();
        for (int Sign = -1; Sign <= 1; Sign += 2) {
            int A = C.Centre() + Sign * 3;
            R.put(C.x(Back, A), R.F + 1, C.z(Back, A), M + "containment_room_restraint_anchor[facing=" + C.Back() + "]");
            for (int Level = R.H; Level >= R.H - 1; Level--) {
                if (B.Grid.empty(C.x(Back - 1, A), R.F + Level, C.z(Back - 1, A)) && !B.cleared(C.x(Back - 1, A), R.F + Level, C.z(Back - 1, A))) {
                    B.Grid.set(C.x(Back - 1, A), R.F + Level, C.z(Back - 1, A), Scp1730Blocks.Chain);
                }
            }
            int Dx = C.x(C.spot(), C.Centre() + Sign * 4);
            int Dz = C.z(C.spot(), C.Centre() + Sign * 4);
            String Below = B.Grid.get(Dx, R.F, Dz);
            if (Below != null && Below.startsWith(M) && !Below.contains("[") && B.Grid.empty(Dx, R.F + 1, Dz)) {
                B.Grid.set(Dx, R.F, Dz, Scp1730Blocks.Drain);
            }
        }
        scatter(B, R, C, Scp1730Blocks.Rubble, 5, true);
        scatter(B, R, C, Scp1730Blocks.FallenPanel, 2, true);
        floorMarks(B, R, C, new int[]{Scp1730Builder.Cracks, Scp1730Builder.Claws, Scp1730Builder.Puddle, Scp1730Builder.Blood}, 14);
        wallMarks(B, R, C, new int[]{Scp1730Builder.Claws, Scp1730Builder.Claws, Scp1730Builder.Cracks}, 10);
        ceilingCables(B, R, C, 2);
    }

    private static void statueCell(Scp1730Builder B, Scp1730Rooms.Box R, Cell C) {
        signs(B, R, C, Scp1730Blocks.SignKeter, 0);
        int Back = C.Depth();
        R.put(C.x(Back, C.Span0()), R.F + 1, C.z(Back, C.Span0()), Scp1730Blocks.facing(Scp1730Blocks.OverturnedChair, C.side(1)));
        R.put(C.x(Back, C.Span1()), R.F + 1, C.z(Back, C.Span1()), Scp1730Blocks.facing(Scp1730Blocks.CrackedTerminal, C.Back()));
        List<int[]> Cells = floorCells(B, R, C);
        for (int Index = 0; Index < 2 && Index < Cells.size(); Index++) {
            int[] Each = Cells.get(Index);
            R.put(Each[0], R.F + 1, Each[1], Scp1730Blocks.PaperStack + "[facing=" + randomFacing(B) + ",variant=" + B.Rng.nextInt(6) + "]");
        }
        scatter(B, R, C, Scp1730Blocks.DragMarks, 5, false);
        scatter(B, R, C, Scp1730Blocks.GlassShards, 2, false);
        floorMarks(B, R, C, new int[]{Scp1730Builder.Oil, Scp1730Builder.Cracks, Scp1730Builder.Grime, Scp1730Builder.Footprints}, 10);
        wallMarks(B, R, C, new int[]{Scp1730Builder.Cracks, Scp1730Builder.Grime, Scp1730Builder.Blood}, 6);
    }

    private static void darkCell(Scp1730Builder B, Scp1730Rooms.Box R, Cell C) {
        signs(B, R, C, Scp1730Blocks.SignMemetic, 1);
        darken(B, R, C, true);
        scatter(B, R, C, Scp1730Blocks.CableTangle, 2, true);
        scatter(B, R, C, Scp1730Blocks.FallenPanel, 2, true);
        scatter(B, R, C, Scp1730Blocks.OverturnedChair, 2, true);
        scatter(B, R, C, Scp1730Blocks.GlassShards, 3, false);
        floorMarks(B, R, C, new int[]{Scp1730Builder.Footprints, Scp1730Builder.Footprints, Scp1730Builder.Grime, Scp1730Builder.Scorch}, 12);
        wallMarks(B, R, C, new int[]{Scp1730Builder.Claws, Scp1730Builder.Claws, Scp1730Builder.Grime}, 8);
        ceilingCables(B, R, C, 3);
    }
}
