package net.rai.scp_unleashed.scp5001.world.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public final class Scp5001PlanCheck {
    public static final Map<String, Integer> AnchorCounts = Map.of(Scp5001Blocks.ChamberCore, 1, Scp5001Blocks.RodEmitter, 8, Scp5001Blocks.LaserEmitter, 6,
            Scp5001Blocks.ConsoleMain, 1, Scp5001Blocks.Generator, 4, Scp5001Blocks.GeneratorConsole, 4, Scp5001Blocks.EmergencyConsole, 1, Scp5001Blocks.EmergencyIntake, 1,
            Scp5001Blocks.SkyEmitter, 1, Scp5001Blocks.Engine, 1);
    public static final int MinimumRooms = 60;

    private Scp5001PlanCheck() {
    }

    private static int index(int X, int Y, int Z) {
        return (Y * Scp5001Plan.Depth + Z) * Scp5001Plan.Width + X;
    }

    public static boolean passable(String State) {
        String Id = Scp5001Blocks.idOf(State);
        return Id.equals(Scp5001Blocks.Air) || Id.equals(Scp5001Blocks.Door) || Id.equals(Scp5001Blocks.EntryDoor);
    }

    public static boolean support(String State) {
        String Id = Scp5001Blocks.idOf(State);
        return !passable(State) && !Id.equals(Scp5001Blocks.Railing) && !Id.equals(Scp5001Blocks.Terminal);
    }

    public static BitSet reach(Scp5001Plan Plan) {
        BitSet Seen = new BitSet(Scp5001Plan.Width * Scp5001Plan.Height * Scp5001Plan.Depth);
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        int[] Start = {Scp5001Plan.entryX() + 1, Scp5001Plan.entryY(), 1};
        Seen.set(index(Start[0], Start[1], Start[2]));
        Queue.add(Start);
        int[][] Steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!Queue.isEmpty()) {
            int[] At = Queue.poll();
            for (int[] Step : Steps) {
                for (int Dy = -1; Dy <= 1; Dy++) {
                    int X = At[0] + Step[0];
                    int Y = At[1] + Dy;
                    int Z = At[2] + Step[1];
                    if (!Scp5001Plan.inside(X, Y, Z) || !Scp5001Plan.inside(X, Y + 1, Z) || Y < 1) {
                        continue;
                    }
                    if (Seen.get(index(X, Y, Z)) || !passable(Plan.get(X, Y, Z)) || !passable(Plan.get(X, Y + 1, Z))) {
                        continue;
                    }
                    if (Dy == 1 && !passable(Plan.get(At[0], At[1] + 2, At[2]))) {
                        continue;
                    }
                    if (!support(Plan.get(X, Y - 1, Z))) {
                        continue;
                    }
                    Seen.set(index(X, Y, Z));
                    Queue.add(new int[]{X, Y, Z});
                }
            }
        }
        return Seen;
    }

    public static List<String> check(Scp5001Plan Plan) {
        List<String> Problems = new ArrayList<>();
        if (Plan.roomCount() < MinimumRooms) {
            Problems.add("only " + Plan.roomCount() + " rooms");
        }
        BitSet Seen = reach(Plan);
        for (Scp5001Room Room : Plan.Rooms) {
            if (Room.Type.equals("void")) {
                continue;
            }
            if (!reached(Plan, Room, Seen)) {
                Problems.add("room " + Room.Id + " " + Room.Type + " on level " + Room.Level + " at " + Room.X0 + "," + Room.Z0 + " is unreachable");
            }
        }
        if (!Seen.get(index(Scp5001Plan.CentreX + 3, Scp5001Plan.floorY(0) + 1, Scp5001Plan.CentreZ))) {
            Problems.add("the chamber floor is unreachable");
        }
        Problems.addAll(enclosure(Plan));
        Problems.addAll(doors(Plan));
        for (Map.Entry<String, Integer> Entry : AnchorCounts.entrySet()) {
            int Found = Plan.anchors(Entry.getKey()).size();
            if (Found != Entry.getValue()) {
                Problems.add(Entry.getKey() + " anchors: " + Found + " instead of " + Entry.getValue());
            }
            for (Scp5001Anchor Each : Plan.anchors(Entry.getKey())) {
                if (!Scp5001Blocks.idOf(Plan.get(Each.X(), Each.Y(), Each.Z())).equals(Entry.getKey())) {
                    Problems.add(Entry.getKey() + " anchor was overwritten at " + Each.X() + "," + Each.Y() + "," + Each.Z());
                }
            }
        }
        return Problems;
    }

    private static boolean reached(Scp5001Plan Plan, Scp5001Room Room, BitSet Seen) {
        for (int Level = Room.Level; Level < Room.Level + Room.Levels; Level++) {
            int Y = Scp5001Plan.floorY(Level) + 1;
            for (int X = Room.X0; X <= Room.X1; X++) {
                for (int Z = Room.Z0; Z <= Room.Z1; Z++) {
                    if (Plan.region(Level, X, Z) == Room.Id && Seen.get(index(X, Y, Z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static List<String> enclosure(Scp5001Plan Plan) {
        List<String> Problems = new ArrayList<>();
        int Openings = 0;
        int[][] Steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int Y = 0; Y < Scp5001Plan.Height; Y++) {
            for (int Z = 0; Z < Scp5001Plan.Depth; Z++) {
                for (int X = 0; X < Scp5001Plan.Width; X++) {
                    String State = Plan.get(X, Y, Z);
                    boolean Hull = Scp5001Plan.hullCell(X, Y, Z);
                    if (!Hull) {
                        if (!State.equals(Scp5001Plan.Outside) && Problems.size() < 8) {
                            Problems.add("cell outside the hull is written at " + X + "," + Y + "," + Z);
                        }
                        continue;
                    }
                    String Id = Scp5001Blocks.idOf(State);
                    if (Scp5001Plan.interiorCell(X, Y, Z)) {
                        for (int[] Step : Steps) {
                            int Nx = X + Step[0];
                            int Ny = Y + Step[1];
                            int Nz = Z + Step[2];
                            if (!Scp5001Plan.hullCell(Nx, Ny, Nz) && Problems.size() < 8) {
                                Problems.add("interior cell touches the outside at " + X + "," + Y + "," + Z);
                            }
                        }
                        continue;
                    }
                    if (Id.startsWith(Scp5001Blocks.Mod + "scp5001_shell")) {
                        continue;
                    }
                    if (!entryCell(X, Y, Z)) {
                        if (Problems.size() < 8) {
                            Problems.add("shell gap at " + X + "," + Y + "," + Z + ": " + Id);
                        }
                        continue;
                    }
                    if (Z == 0 && !Id.equals(Scp5001Blocks.EntryDoor) || Z == 1 && !Id.equals(Scp5001Blocks.Air)) {
                        Problems.add("entry cell " + X + "," + Y + "," + Z + " holds " + Id);
                    }
                    Openings++;
                }
            }
        }
        if (Openings != Scp5001Plan.EntryWidth * Scp5001Plan.EntryHeight * 2) {
            Problems.add("entry opening has " + Openings + " cells");
        }
        return Problems;
    }

    public static List<String> doors(Scp5001Plan Plan) {
        List<String> Problems = new ArrayList<>();
        for (Scp5001Room Room : Plan.Rooms) {
            for (Scp5001Door Door : Room.Doors) {
                String Found = door(Plan, Door);
                if (Found != null && Problems.size() < 8) {
                    Problems.add("door at " + Door.X() + "," + Door.Y() + "," + Door.Z() + " " + Found);
                }
            }
        }
        int X0 = Scp5001Plan.entryX();
        int Y0 = Scp5001Plan.entryY();
        for (int Column = -1; Column <= Scp5001Plan.EntryWidth; Column++) {
            for (int Row = 0; Row <= Scp5001Plan.EntryHeight; Row++) {
                String State = Plan.get(X0 + Column, Y0 + Row, 0);
                boolean Inner = Column >= 0 && Column < Scp5001Plan.EntryWidth && Row < Scp5001Plan.EntryHeight;
                if (Inner && !State.equals(Scp5001Blocks.EntryDoor + "[column=" + Column + ",facing=north,open=false,row=" + Row + "]")) {
                    Problems.add("entry door part " + Column + "," + Row + " holds " + State);
                } else if (!Inner && !Scp5001Blocks.solid(State)) {
                    Problems.add("entry door frame " + Column + "," + Row + " holds " + State);
                }
            }
        }
        return Problems;
    }

    static String door(Scp5001Plan Plan, Scp5001Door Door) {
        int Ax = Door.AlongX() ? 1 : 0;
        int Az = Door.AlongX() ? 0 : 1;
        for (int Column = -1; Column <= 2; Column++) {
            int X = Door.X() + Ax * Column;
            int Z = Door.Z() + Az * Column;
            boolean Jamb = Column < 0 || Column > 1;
            for (int Row = -1; Row <= 3; Row++) {
                String State = Plan.get(X, Door.Y() + Row, Z);
                if (!Jamb && Row >= 0 && Row <= 2) {
                    if (!State.equals(Scp5001Builder.doorPart(Door, Column, Row))) {
                        return "part " + Column + "," + Row + " holds " + State;
                    }
                } else if (!Scp5001Blocks.solid(State)) {
                    return (Row < 0 ? "sill " : Row == 3 ? "lintel " : "jamb ") + Column + "," + Row + " holds " + State;
                }
            }
        }
        return null;
    }

    public static boolean entryCell(int X, int Y, int Z) {
        return Z <= 1 && X >= Scp5001Plan.entryX() && X < Scp5001Plan.entryX() + Scp5001Plan.EntryWidth && Y >= Scp5001Plan.entryY()
                && Y < Scp5001Plan.entryY() + Scp5001Plan.EntryHeight;
    }
}
