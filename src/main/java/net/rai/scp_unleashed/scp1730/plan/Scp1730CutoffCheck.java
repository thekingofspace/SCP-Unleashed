package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp1730CutoffCheck {
    private Scp1730CutoffCheck() {
    }

    public static List<String> check(Scp1730Plan Plan) {
        List<String> Problems = new ArrayList<>();
        Scp1730Cutoffs.Gates Cuts = Scp1730Cutoffs.gates(Plan);
        List<Scp1730Cutoffs.Cut> All = Cuts.cuts();
        Scp1730RoomDressing.Cells Open = Scp1730Cutoffs.through(Plan);
        Map<Integer, Scp1730Cutoffs.Deep> Ways = new HashMap<>();
        Map<Integer, Scp1730Cutoffs.Route> Routes = new HashMap<>();
        Set<Long> Mine = new HashSet<>();
        for (Scp1730Cutoffs.Cut Each : All) {
            for (int Side = -1; Side <= 1; Side += 2) {
                int[] At = Each.control(Side);
                Mine.add(Scp1730Builder.key(At[0], At[1], At[2]));
            }
        }
        for (int Index = 0; Index < All.size(); Index++) {
            Scp1730Cutoffs.Cut Each = All.get(Index);
            String Where = "zone " + Each.Zone() + " cutoff at " + Each.X() + "," + Each.Y() + "," + Each.Z() + ": ";
            if (Each.Zone() < 0 || Each.Zone() > 3 || Each.Y() != Plan.floorY(Each.Zone()) + 1) {
                Problems.add(Where + "does not stand on its zone floor");
                continue;
            }
            int Before = Problems.size();
            parts(Plan, Each, Where, Problems);
            seal(Plan, Each, Where, Problems);
            controls(Plan, Cuts, Index, Each, Where, Problems);
            approach(Plan, Each, Where, Problems);
            strays(Plan, Each, Mine, Where, Problems);
            if (Problems.size() > Before) {
                continue;
            }
            Scp1730Cutoffs.Frame Window = Each.frame();
            Set<Long> Own = new HashSet<>();
            for (int Side = -1; Side <= 1; Side += 2) {
                int[] Stripe = Each.stripe(Side);
                Own.add(Scp1730Builder.key(Stripe[0], Stripe[1], Stripe[2]));
            }
            String Reason = Scp1730Cutoffs.forbidden(Plan::stateAt, Plan.fixtures(), Plan.rooms(), Plan.basins(), Plan.pits(), Window, Own);
            if (Reason != null) {
                Problems.add(Where + Reason);
            }
            for (int[] Cell : Each.doorCells()) {
                if (Plan.inWing(Cell[0], Cell[1], Cell[2])) {
                    Problems.add(Where + "lies in the biological wing");
                    break;
                }
            }
            for (int Other = Index + 1; Other < All.size(); Other++) {
                Scp1730Cutoffs.Cut Next = All.get(Other);
                int[] Here = Each.centre();
                int[] There = Next.centre();
                if (Next.Zone() == Each.Zone() && Math.max(Math.abs(Here[0] - There[0]), Math.abs(Here[2] - There[2])) < Scp1730Cutoffs.Gap) {
                    Problems.add(Where + "crowds the cutoff at " + Next.X() + "," + Next.Y() + "," + Next.Z());
                }
            }
            int F = Window.F();
            if (Each.Zone() == Scp1730Puzzles.DeepZone) {
                Scp1730Cutoffs.Deep Way = Ways.computeIfAbsent(Each.Zone(), Zone -> Scp1730Cutoffs.deep(Open, Plan.fixtures(), Plan.entry(Zone), Zone, F));
                if (Way == null || !Way.clear(Window.plane())) {
                    Problems.add(Where + "sits on the path to the sanctum");
                }
            }
            Scp1730Cutoffs.Route Timed = Routes.computeIfAbsent(Each.Zone(), Zone -> Scp1730Cutoffs.timed(Open, Plan.fixtures(), Zone, F));
            if (Timed != null && Timed.crosses(Window)) {
                Problems.add(Where + "sits on the timed key route");
            }
        }
        return Problems;
    }

    static void parts(Scp1730Plan Plan, Scp1730Cutoffs.Cut Each, String Where, List<String> Problems) {
        if (Each.Mode() == Scp1730Cutoffs.ModeButtons && Each.Open()) {
            Problems.add(Where + "a button cutoff starts open");
        }
        Scp1730Blocks.Palette Look = Scp1730Cutoffs.look(Each.Zone());
        for (int Row = 0; Row < Scp1730Cutoffs.Height; Row++) {
            for (int Column = 0; Column < Each.width(); Column++) {
                int[] At = Each.cell(Column, Row, 0);
                String State = Plan.stateAt(At[0], At[1], At[2]);
                if (!Each.doorState(Column, Row).equals(State)) {
                    Problems.add(Where + "door part " + Column + "," + Row + " is " + State + " instead of " + Each.doorState(Column, Row));
                }
            }
            if (Each.filled()) {
                int[] At = Each.cell(Each.filler(), Row, 0);
                String Wanted = Look.wall(Row + 1, Scp1730Builder.CorridorHeight);
                if (!Wanted.equals(Plan.stateAt(At[0], At[1], At[2]))) {
                    Problems.add(Where + "the narrow door's side is not filled with wall at row " + Row);
                }
            }
        }
    }

    static void seal(Scp1730Plan Plan, Scp1730Cutoffs.Cut Each, String Where, List<String> Problems) {
        Scp1730Blocks.Palette Look = Scp1730Cutoffs.look(Each.Zone());
        int Lo = Each.lo();
        for (int Along = -1; Along <= 1; Along++) {
            for (int Column : new int[]{Lo - 1, Lo + Scp1730Cutoffs.HallWidth}) {
                for (int Row = 0; Row < Scp1730Cutoffs.Height; Row++) {
                    int[] At = Each.cell(Column, Row, Along);
                    String State = Plan.stateAt(At[0], At[1], At[2]);
                    if (Along == 0 ? !Look.wall(Row + 1, Scp1730Builder.CorridorHeight).equals(State) : !Scp1730Cutoffs.solid(State)) {
                        Problems.add(Where + (Along == 0 ? "the jamb" : "the hall side") + " at column " + Column + " row " + Row + " is " + State);
                    }
                }
            }
            for (int Column = Lo - 1; Column <= Lo + Scp1730Cutoffs.HallWidth; Column++) {
                boolean Inside = Column >= Lo && Column < Lo + Scp1730Cutoffs.HallWidth;
                int[] Top = Each.cell(Column, Scp1730Cutoffs.Height, Along);
                String Lintel = Plan.stateAt(Top[0], Top[1], Top[2]);
                if (Along == 0 && Inside ? !Look.Ceiling.equals(Lintel) : !Scp1730Cutoffs.solid(Lintel)) {
                    Problems.add(Where + "the lintel at column " + Column + " is " + Lintel);
                }
                if (Inside) {
                    int[] Low = Each.cell(Column, -1, Along);
                    String Floor = Plan.stateAt(Low[0], Low[1], Low[2]);
                    if (!Scp1730Cutoffs.floor(Floor)) {
                        Problems.add(Where + "the sill at column " + Column + " is " + Floor);
                    }
                }
            }
        }
    }

    static void controls(Scp1730Plan Plan, Scp1730Cutoffs.Gates Cuts, int Index, Scp1730Cutoffs.Cut Each, String Where, List<String> Problems) {
        Scp1730Blocks.Palette Look = Scp1730Cutoffs.look(Each.Zone());
        for (int Side = -1; Side <= 1; Side += 2) {
            String Name = Side > 0 ? "front" : "back";
            int[] At = Each.control(Side);
            String State = Plan.stateAt(At[0], At[1], At[2]);
            if (!Each.controlState(Side).equals(State)) {
                Problems.add(Where + "the " + Name + " control is " + State + " instead of " + Each.controlState(Side));
            }
            int[] Back = Each.support(Side);
            if (!Look.wall(2, Scp1730Builder.CorridorHeight).equals(Plan.stateAt(Back[0], Back[1], Back[2]))) {
                Problems.add(Where + "the " + Name + " control has no wall behind it");
            }
            int Out = Each.controlFacing(Side);
            String Ahead = Plan.stateAt(At[0] + Scp1730Layout.Dx[Out], At[1], At[2] + Scp1730Layout.Dz[Out]);
            int Column = Each.controlColumn(Side) + (Out == Each.right() ? 1 : -1);
            if (Ahead != null || Column < Each.lo() || Column >= Each.lo() + Scp1730Cutoffs.HallWidth) {
                Problems.add(Where + "the " + Name + " control does not face into the hall");
            }
            if (!Each.door(Each.controlColumn(Side))) {
                Problems.add(Where + "the " + Name + " control is not beside a door part");
            }
            if (!Cuts.works(Index, Side)) {
                Problems.add(Where + "the " + Name + " control does not work");
            }
        }
        if (!Each.levers() && Each.Open()) {
            Problems.add(Where + "starts open with no lever to hold it");
        }
    }

    static void approach(Scp1730Plan Plan, Scp1730Cutoffs.Cut Each, String Where, List<String> Problems) {
        Set<Long> Controls = new HashSet<>();
        for (int Side = -1; Side <= 1; Side += 2) {
            int[] At = Each.control(Side);
            Controls.add(Scp1730Builder.key(At[0], At[1], At[2]));
        }
        for (int Along = -2; Along <= 2; Along++) {
            if (Along == 0) {
                continue;
            }
            for (int Column = Each.lo(); Column < Each.lo() + Scp1730Cutoffs.HallWidth; Column++) {
                for (int Row = 0; Row < (Math.abs(Along) == 1 ? Scp1730Cutoffs.Height : Scp1730Cutoffs.Height - 1); Row++) {
                    int[] At = Each.cell(Column, Row, Along);
                    String State = Plan.stateAt(At[0], At[1], At[2]);
                    if (State != null && !Controls.contains(Scp1730Builder.key(At[0], At[1], At[2]))) {
                        Problems.add(Where + "the approach is blocked by " + State + " at " + At[0] + "," + At[1] + "," + At[2]);
                    }
                }
            }
        }
    }

    static void strays(Scp1730Plan Plan, Scp1730Cutoffs.Cut Each, Set<Long> Mine, String Where, List<String> Problems) {
        Set<Long> Own = new HashSet<>();
        for (int Side = -1; Side <= 1; Side += 2) {
            int[] At = Each.control(Side);
            Own.add(Scp1730Builder.key(At[0], At[1], At[2]));
        }
        int[][] Steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] Cell : Each.doorCells()) {
            for (int[] Step : Steps) {
                int X = Cell[0] + Step[0];
                int Y = Cell[1] + Step[1];
                int Z = Cell[2] + Step[2];
                String State = Plan.stateAt(X, Y, Z);
                long Key = Scp1730Builder.key(X, Y, Z);
                boolean Control = State != null && (State.startsWith(Scp1730Cutoffs.Lever) || State.startsWith(Scp1730Cutoffs.Button));
                if (Control && !Own.contains(Key)) {
                    Problems.add(Where + "a stray control " + (Mine.contains(Key) ? "of another cutoff " : "") + "touches the door at " + X + "," + Y + "," + Z);
                }
            }
        }
    }
}
