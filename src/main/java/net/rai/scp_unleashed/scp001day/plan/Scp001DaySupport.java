package net.rai.scp_unleashed.scp001day.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class Scp001DaySupport {
    public static final int Empty = 0;
    public static final int Full = 1;
    public static final int Bottom = 2;
    public static final int Top = 3;
    public static final int Post = 4;
    public static final int FloorProp = 5;
    public static final int CeilingProp = 6;
    public static final int PipeX = 7;
    public static final int PipeZ = 8;
    public static final int Ladder = 9;
    private static final int[] Kinds = kinds();
    private static final int[][] Steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private Scp001DaySupport() {
    }

    public record Loose(int X, int Y, int Z, int Code) {
    }

    private static int[] kinds() {
        int[] Made = new int[Scp001DayCode.Count];
        java.util.Arrays.fill(Made, Full);
        Made[Scp001DayCode.Air] = Empty;
        Made[Scp001DayCode.Water] = Empty;
        for (int Code : new int[] {Scp001DayCode.Curb, Scp001DayCode.FallenTile, Scp001DayCode.RubbleSlab, Scp001DayCode.LampArm, Scp001DayCode.ShelterRoof}) {
            Made[Code] = Bottom;
        }
        Made[Scp001DayCode.ManholeCover] = Top;
        for (int Code : new int[] {Scp001DayCode.LampPole, Scp001DayCode.ShelterPost, Scp001DayCode.Bars, Scp001DayCode.Rebar, Scp001DayCode.RubbleWall, Scp001DayCode.PipeY}) {
            Made[Code] = Post;
        }
        for (int Facing = 0; Facing < 4; Facing++) {
            for (int Code : new int[] {Scp001DayCode.Desk, Scp001DayCode.Chair, Scp001DayCode.Cabinet, Scp001DayCode.Papers, Scp001DayCode.TippedChair, Scp001DayCode.Bench}) {
                Made[Code + Facing] = FloorProp;
            }
        }
        for (int Code : new int[] {Scp001DayCode.Plant, Scp001DayCode.Copier, Scp001DayCode.PaperDrift, Scp001DayCode.Bedroll, Scp001DayCode.Lantern, Scp001DayCode.EmptyTin}) {
            Made[Code] = FloorProp;
        }
        for (int Code : new int[] {Scp001DayCode.DeadLamp, Scp001DayCode.FlickerLamp, Scp001DayCode.EmergencyLamp, Scp001DayCode.Valve}) {
            Made[Code] = CeilingProp;
        }
        Made[Scp001DayCode.PipeX] = PipeX;
        Made[Scp001DayCode.PipeZ] = PipeZ;
        Made[Scp001DayCode.LadderEast] = Ladder;
        Made[Scp001DayCode.LadderSouth] = Ladder;
        return Made;
    }

    public static int kind(int Code) {
        return Code >= 0 && Code < Kinds.length ? Kinds[Code] : Full;
    }

    private static boolean face(int Kind, int Dx, int Dy, int Dz) {
        return switch (Kind) {
            case Full, Ladder -> true;
            case Bottom -> Dy <= 0;
            case Top -> Dy >= 0;
            case Post -> Dy != 0;
            case FloorProp -> Dy != 0;
            case CeilingProp -> Dy > 0;
            case PipeX -> Dx != 0 || Dy != 0;
            case PipeZ -> Dz != 0 || Dy != 0;
            default -> false;
        };
    }

    public static boolean touches(int First, int Second, int Dx, int Dy, int Dz) {
        int A = kind(First);
        int B = kind(Second);
        if (A == Empty || B == Empty || !face(A, Dx, Dy, Dz) || !face(B, -Dx, -Dy, -Dz)) {
            return false;
        }
        if (Dy == 0 && ((A == Bottom && B == Top) || (A == Top && B == Bottom))) {
            return false;
        }
        return true;
    }

    public static List<Loose> loose(int ChunkX, int ChunkZ) {
        int Span = 48;
        int Height = Scp001DayPlan.Height;
        int[][] Region = new int[Span * Span][];
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                int[][] Columns = Scp001DayDressing.columns(ChunkX + Dx, ChunkZ + Dz);
                for (int Lx = 0; Lx < 16; Lx++) {
                    for (int Lz = 0; Lz < 16; Lz++) {
                        Region[((Dx + 1) * 16 + Lx) * Span + (Dz + 1) * 16 + Lz] = Columns[Lx * 16 + Lz];
                    }
                }
            }
        }
        boolean[] Reached = new boolean[Span * Span * Height];
        ArrayDeque<int[]> Open = new ArrayDeque<>();
        for (int X = 0; X < Span; X++) {
            for (int Z = 0; Z < Span; Z++) {
                int[] Column = Region[X * Span + Z];
                boolean Border = X == 0 || Z == 0 || X == Span - 1 || Z == Span - 1;
                for (int Y = 0; Y < Height; Y++) {
                    if (kind(Column[Y]) != Empty && (Y <= 1 || Border)) {
                        Reached[(X * Span + Z) * Height + Y] = true;
                        Open.add(new int[] {X, Y, Z});
                    }
                }
            }
        }
        while (!Open.isEmpty()) {
            int[] Here = Open.poll();
            int Code = Region[Here[0] * Span + Here[2]][Here[1]];
            for (int[] Step : Steps) {
                int X = Here[0] + Step[0];
                int Y = Here[1] + Step[1];
                int Z = Here[2] + Step[2];
                if (X < 0 || Z < 0 || X >= Span || Z >= Span || Y < 0 || Y >= Height) {
                    continue;
                }
                int Index = (X * Span + Z) * Height + Y;
                if (Reached[Index]) {
                    continue;
                }
                int Next = Region[X * Span + Z][Y];
                if (touches(Code, Next, Step[0], Step[1], Step[2])) {
                    Reached[Index] = true;
                    Open.add(new int[] {X, Y, Z});
                }
            }
        }
        List<Loose> Found = new ArrayList<>();
        for (int X = 16; X < 32; X++) {
            for (int Z = 16; Z < 32; Z++) {
                int[] Column = Region[X * Span + Z];
                for (int Y = 0; Y < Height; Y++) {
                    if (kind(Column[Y]) != Empty && !Reached[(X * Span + Z) * Height + Y]) {
                        Found.add(new Loose(ChunkX * 16 + X - 16, Y, ChunkZ * 16 + Z - 16, Column[Y]));
                    }
                }
            }
        }
        return Found;
    }
}
