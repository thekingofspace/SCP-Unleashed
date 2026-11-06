package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class Scp1730ReachCheck {
    public static final int Drop = 3;

    public interface Feel {
        boolean open(String State);

        boolean low(String State);

        boolean floor(String State);
    }

    public static final Feel Plain = new Feel() {
        @Override
        public boolean open(String State) {
            return Scp1730Blocks.passable(State);
        }

        @Override
        public boolean low(String State) {
            return false;
        }

        @Override
        public boolean floor(String State) {
            return State != null && !Scp1730Blocks.passable(State);
        }
    };

    private Scp1730ReachCheck() {
    }

    public static List<String> check(Scp1730Plan Plan, Feel Sense) {
        List<String> Problems = new ArrayList<>();
        for (int Zone = 0; Zone < 4; Zone++) {
            Scp1730Spot Entry = Plan.entry(Zone);
            if (Entry == null) {
                Problems.add("zone " + Zone + ": no entry");
                continue;
            }
            Walk Free = new Walk(Plan, Sense, Plan.floorY(Zone) - 2, Plan.topY(Zone) + 1, false);
            Free.flood((int) Math.floor(Entry.X()), (int) Math.floor(Entry.Y()), (int) Math.floor(Entry.Z()));
            if (Free.Count == 0) {
                Problems.add("zone " + Zone + ": entry not walkable");
                continue;
            }
            for (String Kind : new String[]{"control", "card"}) {
                for (Scp1730Fixture Each : Plan.fixtures(Kind, Zone)) {
                    if (!Free.near(Each.X(), Plan.floorY(Zone) + 1, Each.Z(), 2)) {
                        Problems.add("zone " + Zone + ": " + Kind + " " + Each.Data() + " cannot be reached at " + Each.X() + "," + Each.Y() + "," + Each.Z());
                    }
                }
            }
            List<Scp1730Fixture> Seals = Plan.fixtures("seal", Zone);
            if (Zone < 3 && Seals.size() == 9) {
                Scp1730Fixture Middle = Seals.get(4);
                int Ox = Scp1730PlanCheck.dx(Middle.Facing());
                int Oz = Scp1730PlanCheck.dz(Middle.Facing());
                if (!Free.near(Middle.X() + Ox, Plan.floorY(Zone) + 1, Middle.Z() + Oz, 1)) {
                    Problems.add("zone " + Zone + ": the sealed stairwell door cannot be reached");
                }
                Walk Through = new Walk(Plan, Sense, Plan.floorY(Zone + 1) - 2, Plan.floorY(Zone) + 8, true);
                Through.flood(Middle.X() + Ox, Plan.floorY(Zone) + 1, Middle.Z() + Oz);
                Scp1730Spot Next = Plan.entry(Zone + 1);
                if (Next == null || !Through.near((int) Math.floor(Next.X()), (int) Math.floor(Next.Y()), (int) Math.floor(Next.Z()), 1)) {
                    Problems.add("zone " + Zone + ": the stairwell does not lead down to zone " + (Zone + 1));
                }
            }
            if (Zone == 3) {
                Scp1730Spot Guardian = Plan.guardian();
                Scp1730Spot Thresher = Plan.thresher();
                if (Guardian == null || Thresher == null) {
                    Problems.add("deep zone: no Thresher chamber");
                } else {
                    if (!Free.near((int) Math.floor(Guardian.X()), (int) Math.floor(Guardian.Y()), (int) Math.floor(Guardian.Z()), 1)) {
                        Problems.add("deep zone: the Thresher chamber cannot be entered");
                    }
                    if (!Free.nearAny((int) Math.floor(Thresher.X()), (int) Math.floor(Thresher.Z()), (int) Thresher.Y() - 2, (int) Thresher.Y() + 1, 6)) {
                        Problems.add("deep zone: the Thresher plinth cannot be reached");
                    }
                }
            }
        }
        return Problems;
    }

    static final class Walk {
        final Scp1730Plan Plan;
        final Feel Sense;
        final int Low;
        final int High;
        final boolean SealsOpen;
        final boolean[] Seen;
        final Scp1730Cutoffs.Gates Cuts;
        int Count;

        Walk(Scp1730Plan Plan, Feel Sense, int Low, int High, boolean SealsOpen) {
            this.Plan = Plan;
            this.Sense = Sense;
            this.Low = Low;
            this.High = High;
            this.SealsOpen = SealsOpen;
            this.Cuts = Scp1730Cutoffs.gates(Plan);
            this.Seen = new boolean[Scp1730Plan.Width * Scp1730Plan.Depth * (High - Low + 1)];
        }

        int index(int X, int Y, int Z) {
            return ((Y - Low) * Scp1730Plan.Depth + Z) * Scp1730Plan.Width + X;
        }

        boolean inside(int X, int Y, int Z) {
            return X >= 0 && Z >= 0 && X < Scp1730Plan.Width && Z < Scp1730Plan.Depth && Y >= Low && Y <= High;
        }

        boolean seal(String State) {
            return SealsOpen && State != null && State.startsWith(Scp1730Blocks.Seal);
        }

        boolean clear(int X, int Y, int Z) {
            String State = Cuts.at(Plan, X, Y, Z);
            return State == null || seal(State) || Sense.open(State);
        }

        boolean feet(int X, int Y, int Z) {
            String State = Cuts.at(Plan, X, Y, Z);
            return State == null || seal(State) || Sense.open(State) || Sense.low(State);
        }

        boolean stand(int X, int Y, int Z) {
            if (!inside(X, Y, Z) || !inside(X, Y + 1, Z)) {
                return false;
            }
            if (!feet(X, Y, Z) || !clear(X, Y + 1, Z)) {
                return false;
            }
            String Here = Cuts.at(Plan, X, Y, Z);
            String Below = Cuts.at(Plan, X, Y - 1, Z);
            return Here != null && Sense.low(Here) || Below != null && !seal(Below) && Sense.floor(Below);
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
            while (!Queue.isEmpty()) {
                int[] Here = Queue.poll();
                boolean Jump = clear(Here[0], Here[1] + 2, Here[2]);
                for (int Dir = 0; Dir < 4; Dir++) {
                    int Nx = Here[0] + Scp1730Layout.Dx[Dir];
                    int Nz = Here[2] + Scp1730Layout.Dz[Dir];
                    for (int Dy = 1; Dy >= -Drop; Dy--) {
                        int Ny = Here[1] + Dy;
                        if (Dy == 1 && !Jump) {
                            continue;
                        }
                        if (Dy < 0) {
                            boolean Open = true;
                            for (int Step = 0; Step > Dy; Step--) {
                                if (!clear(Nx, Here[1] + Step, Nz) || !clear(Nx, Here[1] + Step + 1, Nz)) {
                                    Open = false;
                                }
                            }
                            if (!Open) {
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
                        if (inside(X + Ox, Y + Oy, Z + Oz) && Seen[index(X + Ox, Y + Oy, Z + Oz)]) {
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
