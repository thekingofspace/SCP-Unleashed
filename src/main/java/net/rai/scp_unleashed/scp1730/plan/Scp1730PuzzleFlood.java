package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayDeque;
import java.util.Set;

public final class Scp1730PuzzleFlood {
    public static final int Drop = 3;
    final Scp1730RoomDressing.Cells Source;
    final int Low;
    final int High;
    final Set<Long> OpenSeals;
    final Set<Long> Shut;
    final int[] Distance;

    Scp1730PuzzleFlood(Scp1730RoomDressing.Cells Source, int Low, int High, Set<Long> OpenSeals) {
        this(Source, Low, High, OpenSeals, Set.of());
    }

    Scp1730PuzzleFlood(Scp1730RoomDressing.Cells Source, int Low, int High, Set<Long> OpenSeals, Set<Long> Shut) {
        this.Source = Source;
        this.Low = Low;
        this.High = High;
        this.OpenSeals = OpenSeals;
        this.Shut = Shut;
        this.Distance = new int[Scp1730Plan.Width * Scp1730Plan.Depth * (High - Low + 1)];
        java.util.Arrays.fill(Distance, -1);
    }

    int index(int X, int Y, int Z) {
        return ((Y - Low) * Scp1730Plan.Depth + Z) * Scp1730Plan.Width + X;
    }

    boolean inside(int X, int Y, int Z) {
        return X >= 0 && Z >= 0 && X < Scp1730Plan.Width && Z < Scp1730Plan.Depth && Y >= Low && Y <= High;
    }

    boolean open(int X, int Y, int Z) {
        if (!Shut.isEmpty() && Shut.contains(Scp1730Builder.key(X, Y, Z))) {
            return false;
        }
        String State = Source.at(X, Y, Z);
        if (State != null && State.startsWith(Scp1730Blocks.Seal) && OpenSeals.contains(Scp1730Builder.key(X, Y, Z))) {
            return true;
        }
        return Scp1730Blocks.passable(State);
    }

    boolean water(int X, int Y, int Z) {
        return Scp1730Blocks.water(Source.at(X, Y, Z));
    }

    boolean stand(int X, int Y, int Z) {
        if (!inside(X, Y, Z) || !inside(X, Y + 1, Z)) {
            return false;
        }
        return open(X, Y, Z) && open(X, Y + 1, Z) && !open(X, Y - 1, Z) && !water(X, Y + 1, Z) && !water(X, Y - 1, Z);
    }

    Scp1730PuzzleFlood flood(double Sx, double Sy, double Sz) {
        int X = (int) Math.floor(Sx);
        int Y = (int) Math.floor(Sy);
        int Z = (int) Math.floor(Sz);
        int Start = Y;
        if (!stand(X, Start, Z)) {
            if (stand(X, Start + 1, Z)) {
                Start++;
            } else if (stand(X, Start - 1, Z)) {
                Start--;
            } else {
                return this;
            }
        }
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        Distance[index(X, Start, Z)] = 0;
        Queue.add(new int[]{X, Start, Z});
        while (!Queue.isEmpty()) {
            int[] Here = Queue.poll();
            int Now = Distance[index(Here[0], Here[1], Here[2])];
            boolean Jump = open(Here[0], Here[1] + 2, Here[2]);
            for (int Dir = 0; Dir < 4; Dir++) {
                int Nx = Here[0] + Scp1730Layout.Dx[Dir];
                int Nz = Here[2] + Scp1730Layout.Dz[Dir];
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
                    if (stand(Nx, Ny, Nz)) {
                        int Key = index(Nx, Ny, Nz);
                        if (Distance[Key] < 0) {
                            Distance[Key] = Now + 1;
                            Queue.add(new int[]{Nx, Ny, Nz});
                        }
                        break;
                    }
                }
            }
        }
        return this;
    }

    boolean seen(int X, int Y, int Z) {
        return inside(X, Y, Z) && Distance[index(X, Y, Z)] >= 0;
    }

    int distance(int X, int Y, int Z, int Radius) {
        int Best = -1;
        for (int Ox = -Radius; Ox <= Radius; Ox++) {
            for (int Oz = -Radius; Oz <= Radius; Oz++) {
                for (int Oy = -1; Oy <= 1; Oy++) {
                    if (seen(X + Ox, Y + Oy, Z + Oz)) {
                        int Found = Distance[index(X + Ox, Y + Oy, Z + Oz)];
                        if (Best < 0 || Found < Best) {
                            Best = Found;
                        }
                    }
                }
            }
        }
        return Best;
    }

    boolean near(int X, int Y, int Z, int Radius) {
        return distance(X, Y, Z, Radius) >= 0;
    }

    public static boolean crawl(Scp1730RoomDressing.Cells Source, int Gx, int Y, int Gz, int Dir, int Length) {
        int Ax = Scp1730Layout.Dx[Dir];
        int Az = Scp1730Layout.Dz[Dir];
        for (int Step = 0; Step <= Length + 1; Step++) {
            int X = Gx + Ax * Step;
            int Z = Gz + Az * Step;
            String Cell = Source.at(X, Y + 1, Z);
            boolean Grate = Cell != null && Cell.startsWith(Scp1730PuzzlePlan.VentGrate);
            if (!(Cell == null || Grate) || Scp1730Blocks.passable(Source.at(X, Y, Z)) || Scp1730Blocks.water(Source.at(X, Y, Z))) {
                return false;
            }
            if (Step > 0 && Step <= Length && Scp1730Blocks.passable(Source.at(X, Y + 2, Z))) {
                return false;
            }
        }
        int X = Gx + Ax * (Length + 2);
        int Z = Gz + Az * (Length + 2);
        return Scp1730Blocks.passable(Source.at(X, Y + 1, Z)) && Scp1730Blocks.passable(Source.at(X, Y + 2, Z)) && !Scp1730Blocks.passable(Source.at(X, Y, Z));
    }
}
