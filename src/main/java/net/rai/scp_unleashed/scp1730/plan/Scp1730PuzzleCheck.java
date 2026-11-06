package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Scp1730PuzzleCheck {
    private Scp1730PuzzleCheck() {
    }

    public static List<String> check(Scp1730Plan Plan) {
        List<String> Problems = new ArrayList<>();
        for (int Zone = 0; Zone < Scp1730Puzzles.Zones; Zone++) {
            zone(Plan, Zone, Problems);
        }
        return Problems;
    }

    static boolean solid(String State) {
        return State != null && !Scp1730Blocks.passable(State);
    }

    static void zone(Scp1730Plan Plan, int Zone, List<String> Problems) {
        String Where = "zone " + Zone + ": ";
        List<Scp1730Fixture> Marks = Plan.fixtures("puzzle", Zone);
        if (Marks.size() != 1) {
            Problems.add(Where + Marks.size() + " puzzles");
            return;
        }
        Scp1730Fixture Mark = Marks.getFirst();
        int Puzzle = Mark.Data();
        boolean Pooled = false;
        for (int Each : Scp1730Puzzles.Pools[Zone]) {
            Pooled |= Each == Puzzle;
        }
        if (!Pooled) {
            Problems.add(Where + "puzzle " + Puzzle + " is not in this zone's pool");
            return;
        }
        String Name = Where + Scp1730Puzzles.Ids[Puzzle] + " ";
        int F = Plan.floorY(Zone);
        String PanelState = Plan.stateAt(Mark.X(), Mark.Y(), Mark.Z());
        if (PanelState == null || !PanelState.startsWith(Scp1730PuzzlePlan.Panel) || !PanelState.contains("steps=" + Scp1730Puzzles.steps(Puzzle))) {
            Problems.add(Name + "has no lockdown panel at the exit (" + PanelState + ")");
        }
        boolean Control = false;
        for (Scp1730Fixture Each : Plan.fixtures("control", Zone)) {
            Control |= Each.X() == Mark.X() && Each.Y() == Mark.Y() && Each.Z() == Mark.Z();
        }
        if (!Control) {
            Problems.add(Name + "panel is not a control");
        }
        boolean Deep = Zone == Scp1730Puzzles.DeepZone;
        Scp1730Fixture Middle = null;
        Set<Long> Gate = new HashSet<>();
        for (Scp1730Fixture Each : Plan.fixtures(Deep ? "dseal" : "seal", Zone)) {
            if (Each.Data() == 4) {
                Middle = Each;
            }
            if (Deep) {
                Gate.add(Scp1730Builder.key(Each.X(), Each.Y(), Each.Z()));
                if (Plan.stateAt(Each.X(), Each.Y(), Each.Z()) != null) {
                    Problems.add(Name + "sanctum gate cell is not clear at " + Each.X() + "," + Each.Y() + "," + Each.Z());
                }
            }
        }
        if (Deep && Gate.size() != 9) {
            Problems.add(Name + "sanctum gate has " + Gate.size() + " cells");
        }
        if (Middle == null || Math.abs(Middle.X() - Mark.X()) > 3 || Math.abs(Middle.Z() - Mark.Z()) > 3) {
            Problems.add(Name + "panel is not beside the exit seal");
        }
        Scp1730Spot Entry = Plan.entry(Zone);
        Scp1730RoomDressing.Cells Source = Scp1730Cutoffs.through(Plan);
        Scp1730PuzzleFlood Closed = new Scp1730PuzzleFlood(Source, F - 2, F + 10, Set.of(), Gate).flood(Entry.X(), Entry.Y(), Entry.Z());
        Set<Long> Locks = new HashSet<>();
        for (Scp1730Fixture Each : Plan.fixtures("pseal", Zone)) {
            Locks.add(Scp1730Builder.key(Each.X(), Each.Y(), Each.Z()));
        }
        Scp1730PuzzleFlood Opened = Locks.isEmpty() ? Closed : new Scp1730PuzzleFlood(Source, F - 2, F + 10, Locks, Gate).flood(Entry.X(), Entry.Y(), Entry.Z());
        if (Deep && Middle != null) {
            int Dir = Scp1730Builder.dirOf(Middle.Facing());
            if (!Closed.near(Middle.X() + Scp1730Layout.Dx[Dir], F + 1, Middle.Z() + Scp1730Layout.Dz[Dir], 1)) {
                Problems.add(Name + "sanctum gate cannot be reached");
            }
            Scp1730Spot Guardian = Plan.guardian();
            if (Guardian != null && Closed.near((int) Math.floor(Guardian.X()), (int) Math.floor(Guardian.Y()), (int) Math.floor(Guardian.Z()), 1)) {
                Problems.add(Name + "the Thresher Chamber can be reached past the shut gate");
            }
        }
        if (!Closed.near(Mark.X(), F + 1, Mark.Z(), Scp1730PuzzlePlan.Touch)) {
            Problems.add(Name + "panel cannot be reached");
        }
        for (int Role : Scp1730Puzzles.Roles[Puzzle]) {
            Scp1730Fixture Device = Scp1730Puzzles.device(Plan, Zone, Role);
            if (Device == null) {
                Problems.add(Name + "is missing part " + Role);
                continue;
            }
            String Part = Name + "part " + Role + " at " + Device.X() + "," + Device.Y() + "," + Device.Z();
            if (Plan.inWing(Device.X(), Device.Y(), Device.Z())) {
                Problems.add(Part + " lies in the biological wing");
            }
            String State = Plan.stateAt(Device.X(), Device.Y(), Device.Z());
            if (State == null) {
                Problems.add(Part + " is empty");
                continue;
            }
            int Dir = Scp1730Builder.dirOf(Device.Facing());
            if (Device.Y() == F + 2 && !solid(Plan.stateAt(Device.X() - Scp1730Layout.Dx[Dir], Device.Y(), Device.Z() - Scp1730Layout.Dz[Dir]))) {
                Problems.add(Part + " has no wall behind it");
            }
            if (Role == Scp1730Puzzles.RoleVentInner || Role == Scp1730Puzzles.RoleFan) {
                continue;
            }
            if (Role == Scp1730Puzzles.RoleVent) {
                int Back = (Dir + 2) % 4;
                if (!Closed.near(Device.X() + Scp1730Layout.Dx[Dir], F + 1, Device.Z() + Scp1730Layout.Dz[Dir], 0)) {
                    Problems.add(Part + " cannot be reached");
                }
                if (!Scp1730PuzzleFlood.crawl(Source, Device.X(), F, Device.Z(), Back, Scp1730Puzzles.extra(Device.Data()))) {
                    Problems.add(Part + " has no crawlway to the fan room");
                }
                Scp1730Fixture Fan = Scp1730Puzzles.device(Plan, Zone, Scp1730Puzzles.RoleFan);
                int Length = Scp1730Puzzles.extra(Device.Data());
                int Rx = Device.X() + Scp1730Layout.Dx[Back] * (Length + 2);
                int Rz = Device.Z() + Scp1730Layout.Dz[Back] * (Length + 2);
                if (Fan == null || Math.abs(Fan.X() - Rx) + Math.abs(Fan.Z() - Rz) > Scp1730PuzzlePlan.FanDepth + 2) {
                    Problems.add(Part + " leads to no fan breaker");
                }
                continue;
            }
            if (!Closed.near(Device.X(), F + 1, Device.Z(), Scp1730PuzzlePlan.Touch)) {
                Problems.add(Part + " cannot be reached");
            }
        }
        int[] Need = Scp1730Puzzles.Needs[Puzzle];
        if (Need.length == 2) {
            int Count = 0;
            for (Scp1730Fixture Each : Plan.fixtures("pickup", Zone)) {
                if (Scp1730Puzzles.item(Each.Data()) != Need[0]) {
                    continue;
                }
                Count++;
                String Part = Name + Scp1730Puzzles.Items[Need[0]] + " at " + Each.X() + "," + Each.Y() + "," + Each.Z();
                if (Plan.inWing(Each.X(), Each.Y(), Each.Z())) {
                    Problems.add(Part + " lies in the biological wing");
                }
                if (!solid(Plan.stateAt(Each.X(), Each.Y() - 1, Each.Z()))) {
                    Problems.add(Part + " lies on nothing");
                }
                boolean Inside = Puzzle == Scp1730Puzzles.Keycard;
                if (Inside ? Closed.near(Each.X(), F + 1, Each.Z(), Scp1730PuzzlePlan.Touch) || !Opened.near(Each.X(), F + 1, Each.Z(), Scp1730PuzzlePlan.Touch)
                        : !Closed.near(Each.X(), F + 1, Each.Z(), Scp1730PuzzlePlan.Touch)) {
                    Problems.add(Part + (Inside ? " is not behind its locked door" : " cannot be reached"));
                }
            }
            if (Count < Need[1]) {
                Problems.add(Name + "has " + Count + " of " + Need[1] + " " + Scp1730Puzzles.Items[Need[0]]);
            }
        }
        boolean Card = false;
        for (Scp1730Fixture Each : Plan.fixtures("card", Zone)) {
            Card |= Each.Data() == 0;
        }
        if (Zone == 0 && Card != (Puzzle == Scp1730Puzzles.Keycard)) {
            Problems.add(Name + (Card ? "leaves a stray access card" : "has no access card"));
        }
        if (Puzzle == Scp1730Puzzles.Keycard && Locks.size() != 9) {
            Problems.add(Name + "locked room has " + Locks.size() + " seal blocks");
        }
        boolean Lever = false;
        for (Scp1730Fixture Each : Plan.fixtures("control", Zone)) {
            Lever |= Each.Data() == 1;
        }
        if (Zone == 1 && Lever != (Puzzle == Scp1730Puzzles.Sample)) {
            Problems.add(Name + (Lever ? "leaves a stray release lever" : "has no flush lever"));
        }
    }
}
