package net.rai.scp_unleashed.scp024.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Scp024Rooms {
    public static final Scp024Mat[] TileColours = {Scp024Mat.RedConcrete, Scp024Mat.OrangeConcrete, Scp024Mat.YellowConcrete, Scp024Mat.LimeConcrete,
            Scp024Mat.CyanConcrete, Scp024Mat.LightBlueConcrete, Scp024Mat.BlueConcrete, Scp024Mat.PurpleConcrete, Scp024Mat.MagentaConcrete};
    public static final Scp024Mat[] WheelColours = {Scp024Mat.RedConcrete, Scp024Mat.OrangeConcrete, Scp024Mat.YellowConcrete, Scp024Mat.LimeConcrete,
            Scp024Mat.LightBlueConcrete, Scp024Mat.PurpleConcrete};

    private Scp024Rooms() {
    }

    private static void platform(Scp024Plan Plan, int X1, int Z1, int X2, int Z2, int Y, Scp024Mat Top) {
        Plan.fill(X1, Y - 1, Z1, X2, Y - 1, Z2, Scp024Mat.StageWall);
        Plan.fill(X1, Y, Z1, X2, Y, Z2, Top);
    }

    private static void finish(Scp024Plan Plan, int X1, int Z1, int X2, int Z2, int Y) {
        for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
            for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
                Plan.set(X, Y, Z, Scp024Mat.GoldBlock);
                Plan.mark("finish", X, Y + 1, Z);
            }
        }
    }

    private static void column(Scp024Plan Plan, int X, int Top, int Z, int Bottom, Scp024Mat Cap) {
        for (int Y = Bottom; Y < Top; Y++) {
            Plan.set(X, Y, Z, Scp024Mat.StageWall);
        }
        Plan.set(X, Top, Z, Cap);
    }

    public static Scp024Plan pillarHop(long Seed) {
        Scp024Plan Plan = new Scp024Plan("pillar_hop");
        int Width = 13;
        int Depth = 37;
        Plan.stage(Width, 9, Depth, false, 12, true);
        platform(Plan, 3, 0, 9, 3, 0, Scp024Mat.StageFloor);
        Plan.fill(3, -1, Depth - 4, 9, -1, Depth - 1, Scp024Mat.StageWall);
        finish(Plan, 3, Depth - 4, 9, Depth - 1, 0);
        Random Dice = new Random(Seed);
        int X = 6;
        int Y = 0;
        int Z = 3;
        while (Z < Depth - 7) {
            boolean Wide = Dice.nextInt(3) == 0;
            int Dz = Wide ? 3 : 2;
            int Dy = Wide ? 0 : Dice.nextInt(3) - 1;
            if (Z >= Depth - 11) {
                Dy = Integer.signum(-Y);
            }
            if (Y + Dy < -1 || Y + Dy > 1) {
                Dy = 0;
            }
            int Reach = Wide || Dy > 0 ? 1 : 2;
            int Dx = Dice.nextInt(Reach * 2 + 1) - Reach;
            if (X + Dx < 1 || X + Dx > Width - 2) {
                Dx = -Dx;
            }
            X += Dx;
            Y += Dy;
            Z += Dz;
            column(Plan, X, Y, Z, -12, Scp024Mat.Marquee);
            Plan.mark("pillar", X, Y, Z);
        }
        Plan.mark("start", 6, 1, 1, Scp024Plan.South);
        Plan.rigs(Width, 9, Depth);
        return Plan;
    }

    public static Scp024Plan shootingGallery(long Seed) {
        Scp024Plan Plan = new Scp024Plan("shooting_gallery");
        int Size = 17;
        Plan.stage(Size, 8, Size, true, 0, true);
        int[][] Covers = {{3, 5}, {12, 5}, {7, 8}, {3, 11}, {12, 11}, {7, 13}, {8, 3}};
        for (int[] Cover : Covers) {
            Plan.fill(Cover[0], 1, Cover[1], Cover[0] + 1, 2, Cover[1], Scp024Mat.StageWall);
            Plan.set(Cover[0], 2, Cover[1], Scp024Mat.Marquee);
        }
        int[][] Pockets = {{3, -1, Scp024Plan.South}, {8, -1, Scp024Plan.South}, {13, -1, Scp024Plan.South},
                {-1, 5, Scp024Plan.East}, {-1, 11, Scp024Plan.East}, {Size, 5, Scp024Plan.West}, {Size, 11, Scp024Plan.West}};
        for (int[] Pocket : Pockets) {
            int Px = Pocket[0];
            int Pz = Pocket[1];
            int Ox = Pocket[2] == Scp024Plan.East ? -1 : Pocket[2] == Scp024Plan.West ? 1 : 0;
            int Oz = Pocket[2] == Scp024Plan.South ? -1 : 0;
            Plan.set(Px, 3, Pz, Scp024Mat.Air);
            Plan.set(Px, 4, Pz, Scp024Mat.Air);
            Plan.fill(Px + Ox, 2, Pz + Oz, Px + Ox, 5, Pz + Oz, Scp024Mat.StageWall);
            Plan.set(Px, 5, Pz, Scp024Mat.Marquee);
            Plan.mark("turret", Px, 3, Pz, Pocket[2]);
        }
        Plan.mark("start", 8, 1, 15, Scp024Plan.North);
        Plan.rigs(Size, 8, Size);
        return Plan;
    }

    public static Scp024Plan fightingPit(long Seed) {
        Scp024Plan Plan = new Scp024Plan("fighting_pit");
        int Size = 15;
        Plan.stage(Size, 7, Size, true, 0, true);
        for (int[] Post : new int[][]{{3, 3}, {11, 3}, {3, 11}, {11, 11}}) {
            Plan.set(Post[0], 1, Post[1], Scp024Mat.StageWall);
            Plan.set(Post[0], 2, Post[1], Scp024Mat.Marquee);
        }
        for (int[] Spawn : new int[][]{{1, 1}, {13, 1}, {1, 13}, {13, 13}, {7, 1}, {1, 7}, {13, 7}}) {
            Plan.mark("spawn", Spawn[0], 1, Spawn[1]);
        }
        Plan.mark("start", 7, 1, 9, Scp024Plan.North);
        Plan.rigs(Size, 7, Size);
        return Plan;
    }

    public static int[] ring(int Index, int Size) {
        int Side = Size - 1;
        int Step = Math.floorMod(Index, Side * 4);
        if (Step < Side) {
            return new int[]{Step, 0};
        }
        if (Step < Side * 2) {
            return new int[]{Side, Step - Side};
        }
        if (Step < Side * 3) {
            return new int[]{Side - (Step - Side * 2), Side};
        }
        return new int[]{0, Side - (Step - Side * 3)};
    }

    public static Scp024Plan risingLava(long Seed) {
        Scp024Plan Plan = new Scp024Plan("rising_lava");
        int Size = 9;
        int Height = 40;
        Plan.stage(Size, Height, Size, true, 0, true);
        int Index = 2;
        int Y = 1;
        int Count = 0;
        while (Y < 34) {
            int[] Cell = ring(Index, Size);
            Plan.set(Cell[0], Y, Cell[1], Count % 4 == 3 ? Scp024Mat.Marquee : Scp024Mat.StageWall);
            Plan.mark("step", Cell[0], Y, Cell[1]);
            Count++;
            boolean Long = Count % 7 == 6;
            Index += Long ? 3 : 2;
            if (!Long) {
                Y++;
            }
        }
        int[] Next = ring(Index, Size);
        int Cx = Math.max(1, Math.min(Size - 2, Next[0]));
        int Cz = Math.max(1, Math.min(Size - 2, Next[1]));
        finish(Plan, Cx - 1, Cz - 1, Cx + 1, Cz + 1, Y);
        for (int Ly = 6; Ly < Y - 2; Ly += 6) {
            if (Plan.matAt(4, Ly, 4).isAir()) {
                Plan.set(4, Ly, 4, Scp024Mat.Light);
            }
        }
        Plan.mark("start", 4, 1, 4, Scp024Plan.South);
        Plan.mark("rig", 1, Height, 1);
        Plan.mark("rig", Size - 2, Height, Size - 2);
        return Plan;
    }

    public static Scp024Plan crumblingFloor(long Seed) {
        Scp024Plan Plan = new Scp024Plan("crumbling_floor");
        int Size = 15;
        Plan.stage(Size, 6, Size, false, 10, true);
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                Plan.set(X, 0, Z, (X + Z) % 2 == 0 ? Scp024Mat.WhiteConcrete : Scp024Mat.BlackConcrete);
            }
        }
        Plan.mark("start", 7, 1, 7, Scp024Plan.South);
        Plan.rigs(Size, 6, Size);
        return Plan;
    }

    public static Scp024Plan anvilRain(long Seed) {
        Scp024Plan Plan = new Scp024Plan("anvil_rain");
        int Size = 13;
        Plan.stage(Size, 14, Size, true, 0, true);
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                if ((X / 2 + Z / 2) % 2 == 0) {
                    Plan.set(X, 0, Z, Scp024Mat.GrayConcrete);
                }
            }
        }
        Plan.mark("start", 6, 1, 6, Scp024Plan.South);
        Plan.rigs(Size, 14, Size);
        return Plan;
    }

    public static Scp024Plan mazeRun(long Seed) {
        Scp024Plan Plan = new Scp024Plan("maze_run");
        int Cells = 10;
        int Size = Cells * 2 - 1;
        Plan.stage(Size, 3, Size, true, 0, false);
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                if (X % 2 == 1 || Z % 2 == 1) {
                    Plan.fill(X, 1, Z, X, 3, Z, Scp024Mat.StageWall);
                }
            }
        }
        boolean[][] Seen = new boolean[Cells][Cells];
        List<int[]> Stack = new ArrayList<>();
        Random Dice = new Random(Seed);
        Stack.add(new int[]{0, 0});
        Seen[0][0] = true;
        int[][] Steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!Stack.isEmpty()) {
            int[] Here = Stack.get(Stack.size() - 1);
            List<int[]> Open = new ArrayList<>();
            for (int[] Step : Steps) {
                int Nx = Here[0] + Step[0];
                int Nz = Here[1] + Step[1];
                if (Nx >= 0 && Nz >= 0 && Nx < Cells && Nz < Cells && !Seen[Nx][Nz]) {
                    Open.add(new int[]{Nx, Nz});
                }
            }
            if (Open.isEmpty()) {
                Stack.remove(Stack.size() - 1);
                continue;
            }
            int[] Next = Open.get(Dice.nextInt(Open.size()));
            Seen[Next[0]][Next[1]] = true;
            int Wx = Here[0] + Next[0];
            int Wz = Here[1] + Next[1];
            for (int Y = 1; Y <= 3; Y++) {
                Plan.set(Wx, Y, Wz, Scp024Mat.Air);
            }
            Stack.add(Next);
        }
        for (int X = 0; X < Size; X += 4) {
            for (int Z = 0; Z < Size; Z += 4) {
                if (Plan.matAt(X, 3, Z).isAir()) {
                    Plan.set(X, 3, Z, Scp024Mat.Light);
                }
            }
        }
        for (int X = 0; X < Size; X++) {
            Plan.set(X, 3, -1, Scp024Mat.Marquee);
            Plan.set(X, 3, Size, Scp024Mat.Marquee);
            Plan.set(-1, 3, X, Scp024Mat.Marquee);
            Plan.set(Size, 3, X, Scp024Mat.Marquee);
        }
        Plan.set(Size - 1, 0, Size - 1, Scp024Mat.GoldBlock);
        Plan.mark("finish", Size - 1, 1, Size - 1);
        Plan.set(Size - 1, 3, Size - 1, Scp024Mat.Light);
        Plan.mark("start", 0, 1, 0, Scp024Plan.South);
        Plan.mark("rig", 0, 3, 0);
        Plan.mark("rig", Size - 1, 3, Size - 1);
        return Plan;
    }

    public static Scp024Plan memoryTiles(long Seed) {
        Scp024Plan Plan = new Scp024Plan("memory_tiles");
        int Width = 13;
        int Depth = 17;
        Plan.stage(Width, 7, Depth, true, 0, true);
        int Index = 0;
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 3; Column++) {
                int X = 1 + Column * 4;
                int Z = 1 + Row * 4;
                Plan.fill(X, 0, Z, X + 2, 0, Z + 2, TileColours[Index]);
                Plan.mark("tile", X, 0, Z);
                Index++;
            }
        }
        Plan.mark("start", 6, 1, 14, Scp024Plan.North);
        Plan.rigs(Width, 7, Depth);
        return Plan;
    }

    public static Scp024Plan quizShow(long Seed) {
        Scp024Plan Plan = new Scp024Plan("quiz_show");
        int Size = 11;
        Plan.stage(Size, 8, Size, true, 0, true);
        for (int X = 1; X <= 9; X++) {
            for (int Y = 2; Y <= 7; Y++) {
                boolean Edge = X == 1 || X == 9 || Y == 2 || Y == 7;
                Plan.set(X, Y, -1, Edge ? Scp024Mat.Marquee : Scp024Mat.BlackConcrete);
            }
        }
        Plan.set(5, 0, 8, Scp024Mat.IronTrapdoor, Scp024Plan.North);
        for (int Y = -6; Y <= -1; Y++) {
            for (int X = 4; X <= 6; X++) {
                for (int Z = 7; Z <= 9; Z++) {
                    Plan.set(X, Y, Z, X == 5 && Z == 8 ? Scp024Mat.Air : Scp024Mat.StageWall);
                }
            }
        }
        Plan.set(4, 1, 8, Scp024Mat.Barrier);
        Plan.set(4, 2, 8, Scp024Mat.Barrier);
        Plan.set(6, 1, 8, Scp024Mat.Barrier);
        Plan.set(6, 2, 8, Scp024Mat.Barrier);
        Plan.set(5, 1, 9, Scp024Mat.Barrier);
        Plan.set(5, 2, 9, Scp024Mat.Barrier);
        Scp024Mat[] Desk = {Scp024Mat.RedConcrete, Scp024Mat.YellowConcrete, Scp024Mat.BlueConcrete};
        for (int Answer = 0; Answer < 3; Answer++) {
            Plan.set(4 + Answer, 1, 7, Desk[Answer]);
            Plan.set(4 + Answer, 2, 7, Scp024Mat.StoneButton, Scp024Plan.Up);
            Plan.set(4 + Answer, 3, 7, Scp024Mat.Barrier);
            Plan.mark("answer", 4 + Answer, 2, 7);
        }
        Plan.mark("board", 5, 6, 0, Scp024Plan.South);
        Plan.mark("trapdoor", 5, 0, 8);
        Plan.mark("start", 5, 1, 8, Scp024Plan.North);
        Plan.rigs(Size, 8, Size);
        return Plan;
    }

    public static Scp024Plan doorRoulette(long Seed) {
        Scp024Plan Plan = new Scp024Plan("door_roulette");
        int Size = 13;
        Plan.stage(Size, 7, Size, true, 0, true);
        Plan.fill(0, 1, 3, Size - 1, 7, 3, Scp024Mat.StageWall);
        Plan.fill(0, 1, 0, Size - 1, 7, 2, Scp024Mat.Air);
        Plan.fill(0, 1, 0, Size - 1, 4, 0, Scp024Mat.GoldBlock);
        for (int Door = 0; Door < 3; Door++) {
            int Left = 1 + Door * 4;
            Plan.fill(Left, 1, 3, Left + 2, 3, 3, Scp024Mat.Curtain);
            Plan.fill(Left - 1, 1, 3, Left - 1, 4, 3, Scp024Mat.Marquee);
            Plan.fill(Left + 3, 1, 3, Left + 3, 4, 3, Scp024Mat.Marquee);
            Plan.fill(Left, 4, 3, Left + 2, 4, 3, Scp024Mat.Marquee);
            Plan.mark("door", Left, 1, 3);
        }
        Plan.mark("start", 6, 1, 10, Scp024Plan.North);
        Plan.rigs(Size, 7, Size);
        return Plan;
    }

    public static Scp024Plan tntDodge(long Seed) {
        Scp024Plan Plan = new Scp024Plan("tnt_dodge");
        int Size = 15;
        Plan.stage(Size, 10, Size, true, 0, true);
        int[][] Cannons = {{-1, 1, Scp024Plan.East}, {Size, Size - 2, Scp024Plan.West}, {1, -1, Scp024Plan.South}, {Size - 2, Size, Scp024Plan.North}};
        for (int[] Cannon : Cannons) {
            Plan.set(Cannon[0], 5, Cannon[1], Scp024Mat.Dispenser, Cannon[2]);
            Plan.mark("cannon", Cannon[0], 5, Cannon[1], Cannon[2]);
        }
        Plan.mark("start", 7, 1, 7, Scp024Plan.South);
        Plan.rigs(Size, 10, Size);
        return Plan;
    }

    public static Scp024Plan arrowCorridor(long Seed) {
        Scp024Plan Plan = new Scp024Plan("arrow_corridor");
        int Width = 5;
        int Depth = 41;
        Plan.stage(Width, 4, Depth, true, 0, true);
        int Row = 0;
        for (int Z = 6; Z <= 34; Z += 4) {
            for (int Y = 1; Y <= 2; Y++) {
                Plan.set(-1, Y, Z, Scp024Mat.Dispenser, Scp024Plan.East);
                Plan.set(Width, Y, Z, Scp024Mat.Dispenser, Scp024Plan.West);
                Plan.mark("trap", -1, Y, Z, Scp024Plan.East);
                Plan.mark("trap", Width, Y, Z, Scp024Plan.West);
            }
            int Alcove = Z + 2;
            int Side = Row % 2 == 0 ? -1 : Width;
            int Outer = Row % 2 == 0 ? -2 : Width + 1;
            Plan.set(Side, 1, Alcove, Scp024Mat.Air);
            Plan.set(Side, 2, Alcove, Scp024Mat.Air);
            Plan.set(Side, 0, Alcove, Scp024Mat.StageFloor);
            Plan.fill(Outer, 0, Alcove, Outer, 4, Alcove, Scp024Mat.StageWall);
            Plan.set(Side, 3, Alcove, Scp024Mat.Marquee);
            Plan.set(Side, 4, Alcove, Scp024Mat.StageWall);
            Plan.set(Side, 5, Alcove, Scp024Mat.Barrier);
            Plan.set(Outer, 5, Alcove, Scp024Mat.Barrier);
            Row++;
        }
        finish(Plan, 0, Depth - 3, Width - 1, Depth - 1, 0);
        Plan.mark("start", 2, 1, 1, Scp024Plan.South);
        Plan.mark("rig", 2, 4, 10);
        Plan.mark("rig", 2, 4, 30);
        return Plan;
    }

    public static Scp024Plan minefield(long Seed) {
        Scp024Plan Plan = new Scp024Plan("minefield");
        int Width = 9;
        int Depth = 17;
        Plan.stage(Width, 5, Depth, true, 0, true);
        for (int X = 0; X < Width; X++) {
            for (int Z = 1; Z < Depth - 1; Z++) {
                Plan.set(X, 0, Z, Scp024Mat.GrayConcrete);
                Plan.set(X, 1, Z, Scp024Mat.StonePlate);
            }
        }
        finish(Plan, 0, Depth - 1, Width - 1, Depth - 1, 0);
        Plan.mark("field", 0, 0, 1);
        Plan.mark("field", Width - 1, 0, Depth - 2);
        Plan.mark("start", 4, 1, 0, Scp024Plan.South);
        Plan.rigs(Width, 5, Depth);
        return Plan;
    }

    public static Scp024Plan iceBridge(long Seed) {
        Scp024Plan Plan = new Scp024Plan("ice_bridge");
        int Width = 17;
        int Depth = 41;
        Plan.stage(Width, 6, Depth, false, 12, true);
        platform(Plan, 6, 0, 10, 2, 0, Scp024Mat.StageFloor);
        Plan.fill(6, -1, Depth - 3, 10, -1, Depth - 1, Scp024Mat.StageWall);
        finish(Plan, 6, Depth - 3, 10, Depth - 1, 0);
        Random Dice = new Random(Seed);
        int X = 8;
        int Z = 3;
        boolean Forward = true;
        while (Z < Depth - 4) {
            if (Forward) {
                int Run = 3 + Dice.nextInt(3);
                for (int Step = 0; Step < Run && Z < Depth - 3; Step++) {
                    bridge(Plan, X, Z, true, Step % 4 == 2);
                    Z++;
                }
            } else {
                int Direction = X <= 4 ? 1 : X >= Width - 6 ? -1 : Dice.nextBoolean() ? 1 : -1;
                int Run = 2 + Dice.nextInt(4);
                bridge(Plan, X, Z, false, false);
                for (int Step = 0; Step < Run; Step++) {
                    int Nx = X + Direction;
                    if (Nx < 1 || Nx > Width - 3) {
                        break;
                    }
                    X = Nx;
                    bridge(Plan, X, Z, false, false);
                }
            }
            Forward = !Forward;
        }
        while (X < 6 || X > 9) {
            bridge(Plan, X, Z, false, false);
            X += X < 6 ? 1 : -1;
        }
        while (Z < Depth - 3) {
            bridge(Plan, X, Z, true, false);
            Z++;
        }
        Plan.mark("start", 8, 1, 1, Scp024Plan.South);
        Plan.rigs(Width, 6, Depth);
        return Plan;
    }

    private static void bridge(Scp024Plan Plan, int X, int Z, boolean Wide, boolean Glassy) {
        Scp024Mat Ice = Glassy ? Scp024Mat.BlueIce : Scp024Mat.PackedIce;
        if (Plan.matAt(X, 0, Z).isAir()) {
            Plan.set(X, 0, Z, Ice);
            Plan.mark("path", X, 0, Z);
        }
        if (Wide && Plan.matAt(X + 1, 0, Z).isAir()) {
            Plan.set(X + 1, 0, Z, Ice);
            Plan.mark("path", X + 1, 0, Z);
        }
    }

    public static Scp024Plan sumoRing(long Seed) {
        Scp024Plan Plan = new Scp024Plan("sumo_ring");
        int Size = 17;
        Plan.stage(Size, 7, Size, false, 10, true);
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                double Distance = Math.sqrt((X - 8) * (X - 8) + (Z - 8) * (Z - 8));
                if (Distance <= 6.5) {
                    Plan.set(X, 0, Z, Distance > 5.5 ? Scp024Mat.Marquee : (X + Z) % 2 == 0 ? Scp024Mat.WhiteConcrete : Scp024Mat.RedConcrete);
                    Plan.set(X, -1, Z, Scp024Mat.StageWall);
                }
            }
        }
        for (int[] Spawn : new int[][]{{3, 8}, {13, 8}, {8, 3}, {8, 13}}) {
            Plan.mark("spawn", Spawn[0], 1, Spawn[1]);
        }
        Plan.mark("start", 8, 1, 8, Scp024Plan.South);
        Plan.rigs(Size, 7, Size);
        return Plan;
    }

    public static Scp024Plan championBout(long Seed) {
        Scp024Plan Plan = new Scp024Plan("champion_bout");
        int Size = 15;
        Plan.stage(Size, 7, Size, true, 0, true);
        for (int X = 2; X <= 12; X++) {
            Plan.set(X, 0, 2, Scp024Mat.WhiteConcrete);
            Plan.set(X, 0, 12, Scp024Mat.WhiteConcrete);
        }
        for (int Z = 2; Z <= 12; Z++) {
            Plan.set(2, 0, Z, Scp024Mat.WhiteConcrete);
            Plan.set(12, 0, Z, Scp024Mat.WhiteConcrete);
        }
        for (int[] Post : new int[][]{{2, 2}, {12, 2}, {2, 12}, {12, 12}}) {
            Plan.fill(Post[0], 1, Post[1], Post[0], 2, Post[1], Scp024Mat.StageWall);
            Plan.set(Post[0], 3, Post[1], Scp024Mat.Marquee);
        }
        Plan.mark("champion", 7, 1, 3, Scp024Plan.South);
        Plan.mark("start", 7, 1, 11, Scp024Plan.North);
        Plan.rigs(Size, 7, Size);
        return Plan;
    }

    public static Scp024Plan silverfishEscape(long Seed) {
        Scp024Plan Plan = new Scp024Plan("silverfish_escape");
        int Width = 5;
        int Depth = 41;
        Plan.stage(Width, 4, Depth, true, 0, true);
        for (int Z : new int[]{8, 16, 27, 34}) {
            Plan.fill(0, 1, Z, Width - 1, 1, Z, Scp024Mat.StageWall);
            Plan.set(2, 1, Z, Scp024Mat.Marquee);
        }
        Plan.fill(0, 1, 11, 3, 3, 11, Scp024Mat.StageWall);
        Plan.fill(1, 1, 13, 4, 3, 13, Scp024Mat.StageWall);
        Plan.fill(0, 1, 30, 2, 3, 30, Scp024Mat.StageWall);
        Plan.fill(0, 0, 20, Width - 1, 0, 21, Scp024Mat.Air);
        Plan.fill(-1, -2, 19, Width, -1, 22, Scp024Mat.StageWall);
        Plan.fill(0, -1, 20, Width - 1, -1, 21, Scp024Mat.Air);
        Plan.set(2, -1, 21, Scp024Mat.StageWall);
        for (int Vent = 0; Vent < 3; Vent++) {
            Plan.set(1 + Vent, 1, -1, Scp024Mat.IronBars);
            Plan.mark("vent", 1 + Vent, 1, 0);
        }
        Plan.mark("ambush", 0, 1, 24);
        Plan.mark("ambush", 4, 1, 24);
        Plan.mark("ambush", 4, 1, 32);
        finish(Plan, 0, Depth - 3, Width - 1, Depth - 1, 0);
        Plan.mark("start", 2, 1, 3, Scp024Plan.South);
        Plan.mark("rig", 2, 4, 10);
        Plan.mark("rig", 2, 4, 30);
        return Plan;
    }

    public static Scp024Plan lightsOut(long Seed) {
        Scp024Plan Plan = new Scp024Plan("lights_out");
        int Size = 21;
        Plan.stage(Size, 4, Size, true, 0, false);
        Random Dice = new Random(Seed);
        boolean[][] Across = new boolean[3][4];
        boolean[][] Down = new boolean[4][3];
        for (int I = 0; I < 4; I++) {
            for (int J = 0; J < 4; J++) {
                if (Dice.nextInt(3) > 0 && I < 3) {
                    Across[I][J] = true;
                }
                if (Dice.nextInt(3) == 0 && J < 3) {
                    Down[I][J] = true;
                }
            }
        }
        openMaze(Across, Down, Dice);
        for (int I = 0; I < 4; I++) {
            for (int J = 0; J < 4; J++) {
                int X = 2 + I * 5;
                int Z = 2 + J * 5;
                Plan.fill(X, 1, Z, X + 1, 4, Z + 1, Scp024Mat.StageWall);
                if (I < 3 && Across[I][J]) {
                    Plan.fill(X + 2, 1, Z, X + 4, 4, Z, Scp024Mat.StageWall);
                }
                if (J < 3 && Down[I][J]) {
                    Plan.fill(X, 1, Z + 2, X, 4, Z + 4, Scp024Mat.StageWall);
                }
            }
        }
        Plan.fill(9, 1, 9, 11, 4, 11, Scp024Mat.Air);
        for (int[] Exit : new int[][]{{0, 0}, {Size - 1, 0}, {0, Size - 1}, {Size - 1, Size - 1}}) {
            Plan.mark("exit", Exit[0], 1, Exit[1]);
        }
        Plan.mark("start", 10, 1, 10, Scp024Plan.South);
        Plan.mark("rig", 10, 4, 0);
        return Plan;
    }

    private static void openMaze(boolean[][] Across, boolean[][] Down, Random Dice) {
        while (true) {
            boolean[][] Reached = reachable(Across, Down);
            List<int[]> Doors = new ArrayList<>();
            for (int A = 0; A < 5; A++) {
                for (int B = 0; B < 5; B++) {
                    if (B < 4 && A >= 1 && A <= 3 && Across[A - 1][B] && Reached[A][B] != Reached[A][B + 1]) {
                        Doors.add(new int[]{0, A - 1, B});
                    }
                    if (A < 4 && B >= 1 && B <= 3 && Down[A][B - 1] && Reached[A][B] != Reached[A + 1][B]) {
                        Doors.add(new int[]{1, A, B - 1});
                    }
                }
            }
            if (Doors.isEmpty()) {
                return;
            }
            int[] Door = Doors.get(Dice.nextInt(Doors.size()));
            if (Door[0] == 0) {
                Across[Door[1]][Door[2]] = false;
            } else {
                Down[Door[1]][Door[2]] = false;
            }
        }
    }

    private static boolean[][] reachable(boolean[][] Across, boolean[][] Down) {
        boolean[][] Reached = new boolean[5][5];
        ArrayDeque<int[]> Queue = new ArrayDeque<>();
        Reached[2][2] = true;
        Queue.add(new int[]{2, 2});
        while (!Queue.isEmpty()) {
            int[] Cell = Queue.poll();
            int A = Cell[0];
            int B = Cell[1];
            visit(Reached, Queue, A, B + 1, B < 4 && !(A >= 1 && A <= 3 && Across[A - 1][B]));
            visit(Reached, Queue, A, B - 1, B > 0 && !(A >= 1 && A <= 3 && Across[A - 1][B - 1]));
            visit(Reached, Queue, A + 1, B, A < 4 && !(B >= 1 && B <= 3 && Down[A][B - 1]));
            visit(Reached, Queue, A - 1, B, A > 0 && !(B >= 1 && B <= 3 && Down[A - 1][B - 1]));
        }
        return Reached;
    }

    private static void visit(boolean[][] Reached, ArrayDeque<int[]> Queue, int A, int B, boolean Open) {
        if (Open && !Reached[A][B]) {
            Reached[A][B] = true;
            Queue.add(new int[]{A, B});
        }
    }

    public static Scp024Plan hotFloor(long Seed) {
        Scp024Plan Plan = new Scp024Plan("hot_floor");
        int Size = 15;
        Plan.stage(Size, 6, Size, true, 0, true);
        Plan.mark("start", 7, 1, 7, Scp024Plan.South);
        Plan.rigs(Size, 6, Size);
        return Plan;
    }

    public static Scp024Plan shrinkingArena(long Seed) {
        Scp024Plan Plan = new Scp024Plan("shrinking_arena");
        int Size = 19;
        Plan.stage(Size, 6, Size, false, 10, true);
        for (int X = 1; X < Size - 1; X++) {
            for (int Z = 1; Z < Size - 1; Z++) {
                Plan.set(X, 0, Z, (X + Z) % 2 == 0 ? Scp024Mat.WhiteConcrete : Scp024Mat.PurpleConcrete);
            }
        }
        Plan.mark("start", 9, 1, 9, Scp024Plan.South);
        Plan.rigs(Size, 6, Size);
        return Plan;
    }

    public static Scp024Plan hotPotato(long Seed) {
        Scp024Plan Plan = new Scp024Plan("hot_potato");
        int Size = 15;
        Plan.stage(Size, 7, Size, true, 0, true);
        for (int[] Spot : new int[][]{{2, 2}, {12, 2}, {2, 12}, {12, 12}, {7, 1}, {7, 13}}) {
            Plan.set(Spot[0], 1, Spot[1], Scp024Mat.StageWall);
            Plan.set(Spot[0], 2, Spot[1], Scp024Mat.GoldBlock);
            Plan.mark("pedestal", Spot[0], 2, Spot[1]);
        }
        Plan.mark("start", 7, 1, 7, Scp024Plan.South);
        Plan.rigs(Size, 7, Size);
        return Plan;
    }

    public static Scp024Plan parkourTrial(long Seed) {
        Scp024Plan Plan = new Scp024Plan("parkour_trial");
        int Width = 13;
        int Depth = 45;
        Plan.stage(Width, 10, Depth, false, 8, true);
        for (int X = 0; X < Width; X++) {
            for (int Z = 0; Z < Depth; Z++) {
                boolean Edge = X == 0 || Z == 0 || X == Width - 1 || Z == Depth - 1;
                Plan.set(X, -6, Z, Edge ? Scp024Mat.Marquee : Scp024Mat.StageFloor);
            }
        }
        platform(Plan, 4, 0, 8, 2, 0, Scp024Mat.StageFloor);
        Plan.set(6, 0, 5, Scp024Mat.Marquee);
        Plan.set(6, 1, 7, Scp024Mat.Marquee);
        Plan.set(8, 1, 9, Scp024Mat.Marquee);
        Plan.set(8, 2, 11, Scp024Mat.Marquee);
        for (int Z = 13; Z <= 17; Z++) {
            Plan.set(8, 2, Z, Scp024Mat.StageWall);
        }
        Plan.set(6, 2, 19, Scp024Mat.SmoothSlab);
        Plan.set(4, 2, 21, Scp024Mat.SmoothSlab);
        Plan.set(4, 2, 23, Scp024Mat.Marquee);
        Plan.set(4, 2, 26, Scp024Mat.Marquee);
        Plan.set(7, 2, 27, Scp024Mat.Marquee);
        Plan.fill(6, 1, 29, 8, 6, 29, Scp024Mat.StageWall);
        for (int Y = 3; Y <= 6; Y++) {
            Plan.set(7, Y, 28, Scp024Mat.Ladder, Scp024Plan.North);
        }
        Plan.fill(6, 6, 30, 8, 6, 31, Scp024Mat.StageFloor);
        Plan.set(7, 6, 34, Scp024Mat.Marquee);
        Plan.set(5, 5, 36, Scp024Mat.Marquee);
        Plan.set(5, 4, 38, Scp024Mat.Marquee);
        platform(Plan, 4, Depth - 4, 8, Depth - 1, 4, Scp024Mat.StageFloor);
        finish(Plan, 4, Depth - 4, 8, Depth - 1, 4);
        Plan.mark("start", 6, 1, 1, Scp024Plan.South);
        Plan.rigs(Width, 10, Depth);
        return Plan;
    }

    public static Scp024Plan targetPractice(long Seed) {
        Scp024Plan Plan = new Scp024Plan("target_practice");
        int Width = 17;
        int Depth = 27;
        Plan.stage(Width, 10, Depth, false, 10, true);
        platform(Plan, 0, 0, Width - 1, 3, 0, Scp024Mat.StageFloor);
        for (int X = 0; X < Width; X++) {
            Plan.set(X, 0, 3, Scp024Mat.Marquee);
        }
        int[][] Wall = {{2, 3}, {5, 7}, {8, 4}, {11, 8}, {14, 3}, {4, 2}, {12, 5}, {8, 8}};
        for (int[] Spot : Wall) {
            Plan.mark("target", Spot[0], Spot[1], Depth);
        }
        int[][] Posts = {{3, 12, 2}, {13, 10, 4}, {8, 17, 3}, {5, 21, 6}, {11, 20, 1}};
        for (int[] Post : Posts) {
            column(Plan, Post[0], Post[2] - 1, Post[1], -10, Scp024Mat.StageWall);
            Plan.mark("target", Post[0], Post[2], Post[1]);
            Plan.include(Post[0], Post[2], Post[1]);
        }
        Plan.mark("start", 8, 1, 1, Scp024Plan.South);
        Plan.rigs(Width, 10, Depth);
        return Plan;
    }

    public static Scp024Plan wheelOfMisfortune(long Seed) {
        Scp024Plan Plan = new Scp024Plan("wheel_of_misfortune");
        int Size = 17;
        Plan.stage(Size, 9, Size, true, 0, true);
        for (int X = 4; X <= 12; X++) {
            for (int Y = 1; Y <= 9; Y++) {
                Plan.set(X, Y, -1, Scp024Mat.BlackConcrete);
            }
        }
        int Index = 0;
        for (int Step = 0; Step < 16; Step++) {
            int[] Cell = ring(Step, 5);
            int X = 6 + Cell[0];
            int Y = 7 - Cell[1];
            Plan.set(X, Y, -1, WheelColours[Index % WheelColours.length]);
            Plan.mark("segment", X, Y, -1, Index % WheelColours.length);
            Index++;
        }
        Plan.set(8, 5, -1, Scp024Mat.GoldBlock);
        Plan.set(8, 8, -1, Scp024Mat.Marquee);
        Plan.set(8, 9, -1, Scp024Mat.Marquee);
        Plan.mark("start", 8, 1, 12, Scp024Plan.North);
        Plan.rigs(Size, 9, Size);
        return Plan;
    }
}
