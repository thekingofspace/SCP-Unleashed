package net.rai.scp_unleashed.daeviteruins.plan;

public final class RuinKit {
    private RuinKit() {
    }

    public static void wall(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Y0, int Height, RuinMix Body, RuinMix Base) {
        int Lx = Math.min(X1, X2);
        int Hx = Math.max(X1, X2);
        int Lz = Math.min(Z1, Z2);
        int Hz = Math.max(Z1, Z2);
        RuinRandom Random = Canvas.random();
        for (int X = Lx; X <= Hx; X++) {
            for (int Z = Lz; Z <= Hz; Z++) {
                Canvas.put(X, Y0 - 1, Z, Base);
                boolean End = (X == Lx || X == Hx) && (Z == Lz || Z == Hz);
                for (int Y = Y0; Y < Y0 + Height; Y++) {
                    RuinMat Mat;
                    int Course = Y - Y0;
                    if (Course == 0) {
                        Mat = Base.at(Random, X, Y, Z);
                    } else if (Body == RuinMix.Dressed && (Course % 4 == 3 || End && Course % 2 == 1)) {
                        Mat = RuinMix.Trim.at(Random, X, Y, Z);
                    } else {
                        Mat = Body.at(Random, X, Y, Z);
                    }
                    Canvas.put(X, Y, Z, Mat);
                }
            }
        }
    }

    public static void ring(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Y0, int Height, RuinMix Body, RuinMix Base) {
        wall(Canvas, X1, Z1, X2, Z1, Y0, Height, Body, Base);
        wall(Canvas, X1, Z2, X2, Z2, Y0, Height, Body, Base);
        wall(Canvas, X1, Z1, X1, Z2, Y0, Height, Body, Base);
        wall(Canvas, X2, Z1, X2, Z2, Y0, Height, Body, Base);
    }

    public static void mosaic(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Y) {
        RuinRandom Random = Canvas.random();
        int Cx = (X1 + X2) / 2;
        int Cz = (Z1 + Z2) / 2;
        for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                int Ring = Math.abs(X - Cx) + Math.abs(Z - Cz);
                RuinMat Mat;
                if (Ring % 4 == 0) {
                    Mat = RuinMat.CutRedSandstone;
                } else if (Ring % 4 == 2) {
                    Mat = RuinMat.RedTerracotta;
                } else {
                    Mat = Random.at(X, Y, Z, 17) < 0.8 ? RuinMat.SmoothBasalt : RuinMat.PolishedBlackstone;
                }
                Canvas.put(X, Y, Z, Mat);
            }
        }
    }

    public static void band(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Y, RuinMix Mix) {
        for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                if (Canvas.filled(X, Y, Z) && Canvas.at(X, Y, Z).isOpaqueCube()) {
                    Canvas.put(X, Y, Z, Mix);
                }
            }
        }
    }

    public static void merlons(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Y, RuinMix Mix) {
        for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                if (((X + Z) & 1) == 0) {
                    Canvas.put(X, Y, Z, Mix);
                } else {
                    Canvas.slab(X, Y, Z, RuinMat.CutRedSandstoneSlab, false);
                }
            }
        }
    }

    public static void opening(RuinCanvas Canvas, int X, int Y, int Z, boolean AlongX, int Width, int Height, boolean Arch) {
        int Half = Width / 2;
        for (int Offset = -Half; Offset <= Width - 1 - Half; Offset++) {
            int Ox = AlongX ? X + Offset : X;
            int Oz = AlongX ? Z : Z + Offset;
            for (int Level = 0; Level < Height; Level++) {
                Canvas.air(Ox, Y + Level, Oz);
                Canvas.keepClear(Ox, Y + Level, Oz);
            }
        }
        if (Arch && Width >= 2) {
            int Left = -Half;
            int Right = Width - 1 - Half;
            int Lx = AlongX ? X + Left : X;
            int Lz = AlongX ? Z : Z + Left;
            int Rx = AlongX ? X + Right : X;
            int Rz = AlongX ? Z : Z + Right;
            Canvas.stairs(Lx, Y + Height - 1, Lz, RuinMat.RedSandstoneStairs, AlongX ? RuinMat.East : RuinMat.South, true);
            Canvas.stairs(Rx, Y + Height - 1, Rz, RuinMat.RedSandstoneStairs, AlongX ? RuinMat.West : RuinMat.North, true);
        }
    }

    public static void stairRun(RuinCanvas Canvas, int X, int Y, int Z, int Dx, int Dz, int Steps, int Width, RuinMat Stair, RuinMix Fill) {
        int Facing = Dx > 0 ? RuinMat.East : Dx < 0 ? RuinMat.West : Dz > 0 ? RuinMat.South : RuinMat.North;
        for (int Step = 0; Step < Steps; Step++) {
            for (int Across = 0; Across < Width; Across++) {
                int Sx = X + Dx * Step + (Dx == 0 ? Across : 0);
                int Sz = Z + Dz * Step + (Dz == 0 ? Across : 0);
                int Sy = Y + Step;
                Canvas.stairs(Sx, Sy, Sz, Stair, Facing, false);
                for (int Below = Sy - 1; Below >= Y - 1 && Below >= 0; Below--) {
                    if (!Canvas.filled(Sx, Below, Sz)) {
                        Canvas.put(Sx, Below, Sz, Fill);
                    }
                }
                Canvas.keepClear(Sx, Sy + 1, Sz);
                Canvas.keepClear(Sx, Sy + 2, Sz);
            }
        }
    }

    public static void steps(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Y, int Facing, RuinMat Stair) {
        for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                Canvas.stairs(X, Y, Z, Stair, Facing, false);
            }
        }
    }

    public static void floor(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Y, RuinMix Mix) {
        Canvas.box(X1, Y, Z1, X2, Y, Z2, Mix);
    }

    public static void platform(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Top, RuinMix Core, RuinMix Skin) {
        for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                boolean Edge = X == Math.min(X1, X2) || X == Math.max(X1, X2) || Z == Math.min(Z1, Z2) || Z == Math.max(Z1, Z2);
                for (int Y = 0; Y <= Top; Y++) {
                    Canvas.put(X, Y, Z, Edge ? Skin : Core);
                }
            }
        }
    }

    public static void roof(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Y, RuinMat Slab, double Coverage, int Salt) {
        for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                if (Canvas.random().smooth(X, Z, 3.0, Salt) * 0.8 + Canvas.roll(X, Y, Z, Salt) * 0.2 < Coverage) {
                    Canvas.slab(X, Y, Z, Slab, false);
                }
            }
        }
    }

    public static void beams(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Y, boolean AlongX, int Spacing, RuinMat Log) {
        for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                int Index = AlongX ? Z - Math.min(Z1, Z2) : X - Math.min(X1, X2);
                if (Index % Spacing == 0) {
                    Canvas.put(X, Y, Z, Log, AlongX ? RuinMat.East : RuinMat.South);
                }
            }
        }
    }

    public static void tent(RuinCanvas Canvas, int Cx, int Cz, int Length, boolean AlongX, boolean Collapsed, int Salt) {
        RuinRandom Random = Canvas.random();
        int Half = Length / 2;
        for (int Along = -Half; Along <= Half; Along++) {
            for (int Across = -2; Across <= 2; Across++) {
                int X = AlongX ? Cx + Along : Cx + Across;
                int Z = AlongX ? Cz + Across : Cz + Along;
                Canvas.put(X, 0, Z, RuinMix.Ash);
                if (Collapsed) {
                    if (Random.at(X, 1, Z, Salt) < 0.75) {
                        Canvas.put(X, 1, Z, Random.at(X, 2, Z, Salt) < 0.6 ? RuinMat.RedCarpet : RuinMat.BlackCarpet);
                    }
                    continue;
                }
                int Height = 3 - Math.abs(Across);
                if (Height <= 0) {
                    continue;
                }
                if (Math.abs(Along) == Half && Math.abs(Across) == 0) {
                    Canvas.put(X, 1, Z, RuinMat.DarkOakFence);
                    Canvas.put(X, 2, Z, RuinMat.DarkOakFence);
                    Canvas.put(X, 3, Z, RuinMix.Cloth);
                    continue;
                }
                Canvas.put(X, Height, Z, RuinMix.Cloth);
                if (Math.abs(Across) == 1) {
                    for (int Y = 1; Y < Height; Y++) {
                        if (Math.abs(Along) == Half) {
                            Canvas.put(X, Y, Z, RuinMix.Cloth);
                        } else {
                            Canvas.air(X, Y, Z);
                        }
                    }
                } else if (Across == 0) {
                    for (int Y = 1; Y < Height; Y++) {
                        Canvas.air(X, Y, Z);
                    }
                }
            }
        }
    }

    public static void hill(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Peak, boolean West, boolean East, boolean North, boolean South, int Salt) {
        RuinRandom Random = Canvas.random();
        int Lx = Math.min(X1, X2);
        int Hx = Math.max(X1, X2);
        int Lz = Math.min(Z1, Z2);
        int Hz = Math.max(Z1, Z2);
        double Falloff = Math.max(2.0, Peak * 0.9);
        for (int X = Lx; X <= Hx; X++) {
            for (int Z = Lz; Z <= Hz; Z++) {
                double Shape = 1.0;
                if (West) {
                    Shape = Math.min(Shape, (X - Lx + 0.5) / Falloff);
                }
                if (East) {
                    Shape = Math.min(Shape, (Hx - X + 0.5) / Falloff);
                }
                if (North) {
                    Shape = Math.min(Shape, (Z - Lz + 0.5) / Falloff);
                }
                if (South) {
                    Shape = Math.min(Shape, (Hz - Z + 0.5) / Falloff);
                }
                double Noise = Random.smooth(X, Z, 3.5, Salt) - 0.5;
                int Height = (int) Math.round(Peak * Math.min(1.0, Shape) + Noise * 2.2);
                if (Height <= 0) {
                    continue;
                }
                if (!Canvas.filled(X, 0, Z)) {
                    Canvas.put(X, 0, Z, RuinMat.Dirt);
                }
                for (int Y = 1; Y <= Height; Y++) {
                    if (Canvas.filled(X, Y, Z)) {
                        continue;
                    }
                    RuinMat Mat = Y == Height ? RuinMat.Ground : Y >= Height - 2 ? RuinMat.Dirt : RuinMix.Earth.at(Random, X, Y, Z);
                    if (Mat == RuinMat.Gravel) {
                        Mat = RuinMat.CoarseDirt;
                    }
                    Canvas.put(X, Y, Z, Mat);
                }
            }
        }
    }

    public static void bannerPole(RuinCanvas Canvas, int X, int Z, int Height, int Facing) {
        Canvas.put(X, 0, Z, RuinMix.Plinth);
        for (int Y = 1; Y <= Height; Y++) {
            Canvas.put(X, Y, Z, RuinMat.DarkOakFence);
        }
        Canvas.put(X, Height + 1, Z, RuinMat.LightningRod, RuinMat.Up);
        Canvas.banner(X + RuinMat.step(Facing)[0], Height - 1, Z + RuinMat.step(Facing)[2], Facing);
        Canvas.keep(X, Height - 1, Z);
        Canvas.keep(X + RuinMat.step(Facing)[0], Height - 1, Z + RuinMat.step(Facing)[2]);
    }

    public static void wallBanner(RuinCanvas Canvas, int X, int Y, int Z, int Facing) {
        int[] Back = RuinMat.step(RuinMat.opposite(Facing));
        if (Canvas.filled(X + Back[0], Y, Z + Back[2]) && Canvas.at(X + Back[0], Y, Z + Back[2]).carriesLoad()) {
            Canvas.banner(X, Y, Z, Facing);
            Canvas.keep(X, Y, Z);
            Canvas.keep(X + Back[0], Y, Z + Back[2]);
            for (int Below = Y - 1; Below >= 1; Below--) {
                if (!Canvas.filled(X + Back[0], Below, Z + Back[2])) {
                    break;
                }
                Canvas.keep(X + Back[0], Below, Z + Back[2]);
            }
        }
    }

    public static void statue(RuinCanvas Canvas, int X, int Y, int Z, int Facing, double PixelsPerBlock, RuinFigures.Figure Shape, boolean Plinth) {
        if (Plinth) {
            int Reach = (int) Math.ceil(10.0 / PixelsPerBlock);
            for (int Dx = -Reach; Dx <= Reach; Dx++) {
                for (int Dz = -Reach; Dz <= Reach; Dz++) {
                    Canvas.put(X + Dx, Y - 1, Z + Dz, RuinMix.Plinth);
                }
            }
        }
        double Yaw = switch (Facing) {
            case RuinMat.North -> 180.0;
            case RuinMat.East -> 90.0;
            case RuinMat.West -> -90.0;
            default -> 0.0;
        };
        RuinFigures.draw(Canvas, Shape, new RuinFigures.Pose(X + 0.5, Y, Z + 0.5, Yaw, 0.0, 0.0, PixelsPerBlock), Y);
    }

    public static void pillarRow(RuinCanvas Canvas, int X1, int Z1, int X2, int Z2, int Spacing, int Y, int Height, boolean Brass) {
        int Count = 0;
        for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                if (Count++ % Spacing == 0) {
                    Canvas.column(X, Y, Z, Height, Brass);
                }
            }
        }
    }

    public static void cart(RuinCanvas Canvas, int X, int Z, boolean AlongX) {
        int Dx = AlongX ? 1 : 0;
        int Dz = AlongX ? 0 : 1;
        int Facing = AlongX ? RuinMat.South : RuinMat.East;
        for (int Step = -1; Step <= 1; Step++) {
            int Px = X + Dx * Step;
            int Pz = Z + Dz * Step;
            for (int Side : new int[]{-1, 1}) {
                int Wx = Px + Dz * Side;
                int Wz = Pz + Dx * Side;
                Canvas.put(Wx, 0, Wz, RuinMix.Ash);
                if (Step != 0) {
                    Canvas.put(Wx, 1, Wz, RuinMat.StrippedDarkOakLog, Facing);
                }
            }
            Canvas.put(Px, 0, Pz, RuinMix.Ash);
            Canvas.slab(Px, 2, Pz, RuinMat.DarkOakSlab, false);
            Canvas.put(Px, 1, Pz, RuinMat.DarkOakFence);
        }
        Canvas.put(X + Dx * 2, 0, Z + Dz * 2, RuinMix.Ash);
        Canvas.put(X + Dx * 2, 1, Z + Dz * 2, RuinMat.DarkOakFence);
        Canvas.put(X + Dx * 3, 0, Z + Dz * 3, RuinMix.Ash);
        Canvas.put(X + Dx * 3, 1, Z + Dz * 3, RuinMat.DarkOakFence);
        Canvas.put(X - Dx, 3, Z - Dz, RuinMat.Barrel, RuinMat.Up, RuinCanvas.LootNone);
    }
}
