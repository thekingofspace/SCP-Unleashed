package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class Scp1730PlanCheck {
    public static final int Drop = 3;

    private Scp1730PlanCheck() {
    }

    public static List<String> check(long Seed) {
        List<String> Problems = new ArrayList<>();
        Scp1730Plan First = Scp1730Plan.fresh(Seed);
        Scp1730Plan Second = Scp1730Plan.fresh(Seed);
        if (First.checksum() != Second.checksum() || !First.fixtures().equals(Second.fixtures())) {
            Problems.add("seed " + Seed + ": two builds differ");
        }
        Problems.addAll(reach(First));
        return Problems;
    }

    public static List<String> reach(Scp1730Plan Plan) {
        List<String> Problems = new ArrayList<>();
        for (int Zone = 0; Zone < 4; Zone++) {
            Scp1730Spot Entry = Plan.entry(Zone);
            if (Entry == null) {
                Problems.add("zone " + Zone + ": no entry");
                continue;
            }
            int Low = Plan.floorY(Zone) - 2;
            int High = Plan.topY(Zone) + 1;
            Walker Free = new Walker(Plan, Low, High, false, null);
            Free.flood((int) Math.floor(Entry.X()), (int) Math.floor(Entry.Y()), (int) Math.floor(Entry.Z()));
            Walker Wing = new Walker(Plan, Low, High, false, null, true);
            Wing.flood((int) Math.floor(Entry.X()), (int) Math.floor(Entry.Y()), (int) Math.floor(Entry.Z()));
            if (Free.Count == 0) {
                Problems.add("zone " + Zone + ": entry not walkable at " + Entry);
                continue;
            }
            if (Zone < 3 && Plan.fixtures("control", Zone).isEmpty()) {
                Problems.add("zone " + Zone + ": no objective control");
            }
            for (Scp1730Fixture Each : Plan.fixtures("control", Zone)) {
                if (!Free.near(Each.X(), Plan.floorY(Zone) + 1, Each.Z(), 2)) {
                    Problems.add("zone " + Zone + ": control " + Each.Data() + " unreachable at " + Each.X() + "," + Each.Y() + "," + Each.Z());
                }
            }
            for (Scp1730Fixture Each : Plan.fixtures("loot", Zone)) {
                if (!Wing.near(Each.X(), Each.Y(), Each.Z(), 2)) {
                    Problems.add("zone " + Zone + ": loot tier " + Each.Data() + " unreachable at " + Each.X() + "," + Each.Y() + "," + Each.Z());
                }
            }
            for (Scp1730Fixture Each : Plan.fixtures("safe", Zone)) {
                if (!Wing.near(Each.X(), Each.Y(), Each.Z(), Math.max(2, Each.Data() - 1))) {
                    Problems.add("zone " + Zone + ": safe room unreachable at " + Each.X() + "," + Each.Y() + "," + Each.Z());
                }
            }
            for (Scp1730Fixture Each : Plan.fixtures("setpiece", Zone)) {
                if (!Wing.near(Each.X(), Each.Y(), Each.Z(), 2)) {
                    Problems.add("zone " + Zone + ": setpiece " + Each.Data() + " unreachable at " + Each.X() + "," + Each.Y() + "," + Each.Z());
                }
            }
            for (Scp1730Fixture Each : Plan.fixtures("card", Zone)) {
                if (!Free.near(Each.X(), Plan.floorY(Zone) + 1, Each.Z(), 2)) {
                    Problems.add("zone " + Zone + ": card unreachable");
                }
            }
            List<Scp1730Fixture> Seals = Plan.fixtures("seal", Zone);
            if (Zone < 3) {
                if (Seals.size() != 9) {
                    Problems.add("zone " + Zone + ": seal has " + Seals.size() + " blocks");
                } else {
                    Scp1730Fixture Middle = Seals.get(4);
                    int Ox = dx(Middle.Facing());
                    int Oz = dz(Middle.Facing());
                    if (!Free.near(Middle.X() + Ox, Plan.floorY(Zone) + 1, Middle.Z() + Oz, 1)) {
                        Problems.add("zone " + Zone + ": seal outside unreachable");
                    }
                    if (Free.near(Middle.X() - Ox * 2, Plan.floorY(Zone) + 1, Middle.Z() - Oz * 2, 0)) {
                        Problems.add("zone " + Zone + ": stair room reachable without opening the seal");
                    }
                    Walker Through = new Walker(Plan, Plan.floorY(Zone + 1) - 2, Plan.floorY(Zone) + 8, true, null);
                    Through.flood(Middle.X() - Ox * 2, Plan.floorY(Zone) + 1, Middle.Z() - Oz * 2);
                    Scp1730Spot Next = Plan.entry(Zone + 1);
                    if (Next == null || !Through.near((int) Math.floor(Next.X()), (int) Math.floor(Next.Y()), (int) Math.floor(Next.Z()), 1)) {
                        Problems.add("zone " + Zone + ": stairwell does not reach zone " + (Zone + 1) + " entry");
                    }
                    if (!Plan.fixtures("pit", Zone).isEmpty() || !Plan.pits().isEmpty()) {
                        for (Scp1730RoomInfo Room : Plan.rooms()) {
                            if (Room.Zone() != Zone || !Room.Type().equals("pit")) {
                                continue;
                            }
                            Walker Around = new Walker(Plan, Low, High, false, Room);
                            Around.flood((int) Math.floor(Entry.X()), (int) Math.floor(Entry.Y()), (int) Math.floor(Entry.Z()));
                            if (!Around.near(Middle.X() + Ox, Plan.floorY(Zone) + 1, Middle.Z() + Oz, 1)) {
                                Problems.add("zone " + Zone + ": the pit room is the only route to the seal");
                            }
                            for (Scp1730Fixture Each : Plan.fixtures("control", Zone)) {
                                if (!Around.near(Each.X(), Plan.floorY(Zone) + 1, Each.Z(), 2)) {
                                    Problems.add("zone " + Zone + ": the pit room is the only route to control " + Each.Data());
                                }
                            }
                        }
                    }
                }
            } else {
                Scp1730Spot Thresher = Plan.thresher();
                Scp1730Spot Guardian = Plan.guardian();
                if (Thresher == null || Guardian == null) {
                    Problems.add("deep zone: no thresher or guardian spot");
                } else {
                    if (!Free.near((int) Math.floor(Guardian.X()), (int) Math.floor(Guardian.Y()), (int) Math.floor(Guardian.Z()), 1)) {
                        Problems.add("deep zone: Thresher Chamber unreachable from the deep entry");
                    }
                    if (!Free.nearAny((int) Math.floor(Thresher.X()), (int) Math.floor(Thresher.Z()), (int) Thresher.Y() - 2, (int) Thresher.Y() + 1, 6)) {
                        Problems.add("deep zone: thresher plinth unreachable");
                    }
                }
            }
        }
        Problems.addAll(Scp1730PuzzleCheck.check(Plan));
        return Problems;
    }

    static int dx(String Facing) {
        return Facing.equals("east") ? 1 : Facing.equals("west") ? -1 : 0;
    }

    static int dz(String Facing) {
        return Facing.equals("south") ? 1 : Facing.equals("north") ? -1 : 0;
    }

    static final class Walker {
        final Scp1730Plan Plan;
        final int Low;
        final int High;
        final boolean SealsOpen;
        final Scp1730RoomInfo Blocked;
        final boolean WingOpen;
        final boolean[] Seen;
        final Scp1730Cutoffs.Gates Cuts;
        int Count;

        Walker(Scp1730Plan Plan, int Low, int High, boolean SealsOpen, Scp1730RoomInfo Blocked) {
            this(Plan, Low, High, SealsOpen, Blocked, false);
        }

        Walker(Scp1730Plan Plan, int Low, int High, boolean SealsOpen, Scp1730RoomInfo Blocked, boolean WingOpen) {
            this.WingOpen = WingOpen;
            this.Cuts = Scp1730Cutoffs.gates(Plan);
            this.Plan = Plan;
            this.Low = Low;
            this.High = High;
            this.SealsOpen = SealsOpen;
            this.Blocked = Blocked;
            this.Seen = new boolean[Scp1730Plan.Width * Scp1730Plan.Depth * (High - Low + 1)];
        }

        int index(int X, int Y, int Z) {
            return ((Y - Low) * Scp1730Plan.Depth + Z) * Scp1730Plan.Width + X;
        }

        boolean inside(int X, int Y, int Z) {
            return X >= 0 && Z >= 0 && X < Scp1730Plan.Width && Z < Scp1730Plan.Depth && Y >= Low && Y <= High;
        }

        boolean unsealed(String State, int X, int Y, int Z) {
            return State != null && State.startsWith(Scp1730Blocks.Seal) && (SealsOpen || WingOpen && (Plan.wingSeal(X, Y, Z) || Scp1730PuzzlePlan.lockedDoor(Plan, X, Y, Z)));
        }

        boolean open(int X, int Y, int Z) {
            String State = Cuts.at(Plan, X, Y, Z);
            if (unsealed(State, X, Y, Z)) {
                return true;
            }
            return Scp1730Blocks.passable(State);
        }

        boolean solid(int X, int Y, int Z) {
            String State = Cuts.at(Plan, X, Y, Z);
            if (unsealed(State, X, Y, Z)) {
                return false;
            }
            return !Scp1730Blocks.passable(State);
        }

        boolean stand(int X, int Y, int Z) {
            if (!inside(X, Y, Z) || !inside(X, Y + 1, Z)) {
                return false;
            }
            if (Blocked != null && X >= Blocked.MinX() && X <= Blocked.MaxX() && Z >= Blocked.MinZ() && Z <= Blocked.MaxZ() && Y >= Blocked.Floor() && Y <= Blocked.Floor() + 2) {
                return false;
            }
            return open(X, Y, Z) && open(X, Y + 1, Z) && solid(X, Y - 1, Z);
        }

        void flood(int X, int Y, int Z) {
            int Start = Y;
            if (!stand(X, Start, Z)) {
                if (stand(X, Start + 1, Z)) {
                    Start++;
                } else if (stand(X, Start - 1, Z)) {
                    Start--;
                } else {
                    return;
                }
            }
            ArrayDeque<int[]> Queue = new ArrayDeque<>();
            Seen[index(X, Start, Z)] = true;
            Queue.add(new int[]{X, Start, Z});
            Count = 1;
            int[] Dx = {0, 1, 0, -1};
            int[] Dz = {-1, 0, 1, 0};
            while (!Queue.isEmpty()) {
                int[] Here = Queue.poll();
                boolean Jump = open(Here[0], Here[1] + 2, Here[2]);
                for (int Dir = 0; Dir < 4; Dir++) {
                    int Nx = Here[0] + Dx[Dir];
                    int Nz = Here[2] + Dz[Dir];
                    for (int Dy = 1; Dy >= -Drop; Dy--) {
                        int Ny = Here[1] + Dy;
                        if (Dy == 1 && !Jump) {
                            continue;
                        }
                        if (Dy < 0) {
                            boolean Clear = true;
                            for (int Step = 0; Step > Dy; Step--) {
                                if (!open(Nx, Here[1] + Step, Nz) || !open(Nx, Here[1] + Step + 1, Nz)) {
                                    Clear = false;
                                }
                            }
                            if (!Clear) {
                                break;
                            }
                        }
                        if (stand(Nx, Ny, Nz) && Cuts.enter(Here[0], Here[2], Nx, Ny, Nz)) {
                            int Key = index(Nx, Ny, Nz);
                            if (!Seen[Key]) {
                                Seen[Key] = true;
                                Count++;
                                Queue.add(new int[]{Nx, Ny, Nz});
                            }
                            break;
                        }
                    }
                }
            }
        }

        boolean near(int X, int Y, int Z, int Radius) {
            for (int Ox = -Radius; Ox <= Radius; Ox++) {
                for (int Oz = -Radius; Oz <= Radius; Oz++) {
                    for (int Oy = -1; Oy <= 1; Oy++) {
                        int Nx = X + Ox;
                        int Ny = Y + Oy;
                        int Nz = Z + Oz;
                        if (inside(Nx, Ny, Nz) && Seen[index(Nx, Ny, Nz)]) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        boolean nearAny(int X, int Z, int Y0, int Y1, int Radius) {
            for (int Y = Y0; Y <= Y1; Y++) {
                if (near(X, Y, Z, Radius)) {
                    return true;
                }
            }
            return false;
        }
    }
}
