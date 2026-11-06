package net.rai.scp_unleashed.scp020.client;

import java.util.List;

public final class Scp020EyeShapes {
    public static final int Empty = 0;
    public static final int Lid = 1;
    public static final int LidLight = 2;
    public static final int LidLower = 3;
    public static final int Corner = 4;
    public static final int Sclera = 5;
    public static final int ScleraShade = 6;
    public static final int Iris = 7;
    public static final int IrisLight = 8;
    public static final int Pupil = 9;
    public static final int Glint = 10;
    public static final int Lash = 11;
    public static final int IrisRim = 12;
    public static final int Open = 3;
    public static final List<Shape> All = List.of(
            shape("almond", 2, 1,
                    "...sssssss...",
                    ".sssssssssss.",
                    "sssssssssssss",
                    ".sssssssssss.",
                    "...sssssss..."),
            shape("slit", 1, 0,
                    "..sssssssss..",
                    "sssssssssssss",
                    "..sssssssss.."),
            shape("round", 3, 1,
                    "..sssss..",
                    ".sssssss.",
                    "sssssssss",
                    "sssssssss",
                    "sssssssss",
                    ".sssssss.",
                    "..sssss.."),
            shape("small", 1, 0,
                    ".sssssss.",
                    "sssssssss",
                    ".sssssss."),
            shape("wide", 2, 1,
                    "....sssssssss....",
                    "..sssssssssssss..",
                    "sssssssssssssssss",
                    "..sssssssssssss..",
                    "....sssssssss....")
    );

    private Scp020EyeShapes() {
    }

    public static Shape shape(String Name, int IrisRadius, int PupilReach, String... Rows) {
        int Width = 0;
        for (String Row : Rows) {
            Width = Math.max(Width, Row.length());
        }
        int Height = Rows.length;
        int[] Top = new int[Width];
        int[] Bottom = new int[Width];
        for (int X = 0; X < Width; X++) {
            Top[X] = -1;
            Bottom[X] = -1;
            for (int Y = 0; Y < Height; Y++) {
                if (X < Rows[Y].length() && Rows[Y].charAt(X) == 's') {
                    if (Top[X] < 0) {
                        Top[X] = Y;
                    }
                    Bottom[X] = Y;
                }
            }
        }
        return new Shape(Name, Width, Height, IrisRadius, PupilReach, Top, Bottom);
    }

    public record Shape(String Name, int Width, int Height, int IrisRadius, int PupilReach, int[] Top, int[] Bottom) {
        public int gridWidth() {
            return Width + 2;
        }

        public int gridHeight() {
            return Height + 2;
        }

        public int centre() {
            return Height / 2;
        }

        public int lookReach() {
            return Width >= 11 ? 2 : 1;
        }

        public int[] build(int Aperture, int LookX, int LookY) {
            int GridWidth = gridWidth();
            int[] Grid = new int[GridWidth * gridHeight()];
            int Middle = centre();
            int Left = -1;
            int Right = -1;
            for (int X = 0; X < Width; X++) {
                if (Top[X] >= 0 && Top[X] <= Middle && Bottom[X] >= Middle) {
                    if (Left < 0) {
                        Left = X;
                    }
                    Right = X;
                }
            }
            if (Left < 0) {
                return Grid;
            }
            if (Aperture <= 0) {
                for (int X = Left; X <= Right; X++) {
                    Grid[(Middle + 1) * GridWidth + X + 1] = X < Width / 2 ? LidLight : Lid;
                }
                for (int X = Left + 2; X <= Right - 2; X += 2) {
                    Grid[(Middle + 2) * GridWidth + X + 1] = Lash;
                }
                Grid[(Middle + 1) * GridWidth + Left] = Corner;
                Grid[(Middle + 1) * GridWidth + Right + 2] = Corner;
                return Grid;
            }
            int Close = Open - Math.min(Open, Aperture);
            for (int X = 0; X < Width; X++) {
                if (Top[X] < 0) {
                    continue;
                }
                int NewTop = Top[X] + ((Middle - Top[X]) * Close + Open - 1) / Open;
                int NewBottom = Bottom[X] - (Bottom[X] - Middle) * Close / (Open * 2);
                if (NewTop > NewBottom) {
                    Grid[(Middle + 1) * GridWidth + X + 1] = X < Width / 2 ? LidLight : Lid;
                    continue;
                }
                for (int Y = NewTop; Y <= NewBottom; Y++) {
                    Grid[(Y + 1) * GridWidth + X + 1] = Y == NewTop ? ScleraShade : Sclera;
                }
                Grid[NewTop * GridWidth + X + 1] = X < Width / 2 ? LidLight : Lid;
                Grid[(NewBottom + 2) * GridWidth + X + 1] = LidLower;
            }
            Grid[(Middle + 1) * GridWidth + Left] = Corner;
            Grid[(Middle + 1) * GridWidth + Right + 2] = Corner;
            int CentreX = Width / 2 + LookX;
            int CentreY = Middle + LookY;
            int Reach = IrisRadius * IrisRadius + IrisRadius;
            int Inner = IrisRadius >= 2 ? (IrisRadius - 1) * (IrisRadius - 1) + IrisRadius - 1 : Reach;
            for (int Y = 0; Y < Height; Y++) {
                for (int X = 0; X < Width; X++) {
                    int Index = (Y + 1) * GridWidth + X + 1;
                    int Kind = Grid[Index];
                    if (Kind != Sclera && Kind != ScleraShade) {
                        continue;
                    }
                    int Dx = X - CentreX;
                    int Dy = Y - CentreY;
                    if (Dx * Dx + Dy * Dy > Reach) {
                        continue;
                    }
                    if (Dx == 0 && Math.abs(Dy) <= PupilReach) {
                        Grid[Index] = Pupil;
                    } else if (Dx * Dx + Dy * Dy > Inner) {
                        Grid[Index] = IrisRim;
                    } else if (Dx == -1 && Dy == -1 && IrisRadius >= 2) {
                        Grid[Index] = Glint;
                    } else if (Dx < 0 && Dy <= 0 || Dx == 0 && Dy < 0) {
                        Grid[Index] = IrisLight;
                    } else {
                        Grid[Index] = Iris;
                    }
                }
            }
            return Grid;
        }
    }
}
