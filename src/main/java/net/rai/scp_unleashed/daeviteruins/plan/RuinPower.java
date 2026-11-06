package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayList;
import java.util.List;

public final class RuinPower {
    private RuinPower() {
    }

    public static void ziggurat(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        int[] Halves = {15, 12, 9, 6, 3};
        Canvas.scorchGround(0, 0, 19.0, 0.35, 211);
        for (int Level = 0; Level < Halves.length; Level++) {
            int Half = Halves[Level];
            int Top = Level * 3 + 2;
            RuinKit.platform(Canvas, -Half, -Half, Half, Half, Top, RuinMix.Rough, RuinMix.Dressed);
            for (int X = -Half; X <= Half; X++) {
                for (int Z = -Half; Z <= Half; Z++) {
                    boolean Edge = Math.abs(X) == Half || Math.abs(Z) == Half;
                    if (Edge) {
                        Canvas.put(X, Level * 3, Z, RuinMix.Plinth);
                        Canvas.put(X, Top, Z, RuinMix.Trim);
                    } else if (Level == Halves.length - 1 || Math.max(Math.abs(X), Math.abs(Z)) > Halves[Level + 1]) {
                        Canvas.put(X, Top, Z, ((X + Z) & 3) == 0 ? RuinMat.CutRedSandstone : RuinMix.Plinth.at(Random, X, Top, Z));
                    }
                }
            }
        }
        int Peak = Halves.length * 3 - 1;
        for (int Step = 0; Step <= Peak - 1; Step++) {
            int Y = Step + 1;
            int Z = 16 - Step;
            boolean Landing = Step == 4 || Step == 8 || Step == 11;
            for (int X = -2; X <= 2; X++) {
                if (Landing) {
                    Canvas.put(X, Y, Z, RuinMix.Plinth);
                } else {
                    Canvas.stairs(X, Y, Z, RuinMat.SmoothRedSandstoneStairs, RuinMat.North, false);
                }
                for (int Fill = Y - 1; Fill >= 0; Fill--) {
                    if (!Canvas.filled(X, Fill, Z)) {
                        Canvas.put(X, Fill, Z, RuinMix.Rough);
                    }
                }
                Canvas.protect(X, Y, Z);
                Canvas.air(X, Y + 1, Z);
                Canvas.air(X, Y + 2, Z);
                Canvas.keepClear(X, Y + 1, Z);
            }
            for (int Side : new int[]{-3, 3}) {
                Canvas.put(Side, Y, Z, RuinMix.Dressed);
                Canvas.put(Side, Y + 1, Z, Step % 3 == 0 ? RuinMat.BlackstoneBrickWall : RuinMat.RedSandstoneWall);
                for (int Fill = Y - 1; Fill >= 0; Fill--) {
                    if (!Canvas.filled(Side, Fill, Z)) {
                        Canvas.put(Side, Fill, Z, RuinMix.Rough);
                    }
                }
            }
            if (Landing) {
                RuinDanger.dartTrap(Canvas, Step == 8 ? 1 : 0, Y + 1, Z, true, 4, RuinDanger.dartAmmo(Random, Tier), Step == 11);
            }
        }
        int Top = Peak + 1;
        for (int[] Corner : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            Canvas.column(Corner[0], Top, Corner[1], 4, true);
        }
        for (int X = -2; X <= 2; X++) {
            Canvas.slab(X, Top + 4, -2, RuinMat.CutRedSandstoneSlab, false);
            Canvas.slab(X, Top + 4, 2, RuinMat.CutRedSandstoneSlab, false);
        }
        for (int X = -1; X <= 1; X++) {
            for (int Y = Top; Y <= Top + 2; Y++) {
                Canvas.put(X, Y, -3, RuinMix.Dressed);
            }
        }
        Canvas.put(0, Top + 1, -2, RuinMat.RuinReliefGod, RuinMat.South);
        Canvas.keep(0, Top + 1, -2);
        Canvas.greatBrazier(0, Top, 0, true, true);
        for (int Level : new int[]{1, 3}) {
            int Corner = Halves[Level] + 1;
            int Y = Level * 3;
            for (int[] Spot : new int[][]{{-Corner, -Corner}, {Corner, -Corner}, {-Corner, Corner}, {Corner, Corner}}) {
                Canvas.greatBrazier(Spot[0], Y, Spot[1], Level == 3, Level == 3);
            }
        }
        for (int Z = -15; Z <= -5; Z++) {
            for (int X = -1; X <= 1; X++) {
                for (int Y = 1; Y <= 3; Y++) {
                    Canvas.air(X, Y, Z);
                }
                Canvas.put(X, 0, Z, RuinMix.DarkBrick);
            }
            Canvas.keepClear(0, 1, Z);
        }
        Canvas.clear(-5, 1, -5, 5, 4, 5);
        RuinKit.mosaic(Canvas, -5, -5, 5, 5, 0);
        Canvas.spawner(0, 1, 0, RuinDanger.spawnerKind(Random, Tier));
        Canvas.chest(-4, 1, 4, RuinMat.North, RuinCanvas.LootVault);
        Canvas.chest(4, 1, 4, RuinMat.North, RuinCanvas.LootCache);
        for (int[] Urn : new int[][]{{-5, -3}, {5, -3}, {-5, 0}, {5, 0}}) {
            Canvas.put(Urn[0], 1, Urn[1], RuinMat.RuinUrn, Urn[0] < 0 ? RuinMat.East : RuinMat.West);
        }
        Canvas.hangingLantern(-3, 4, -3, 0);
        Canvas.hangingLantern(3, 4, 3, 0);
        RuinDanger.dartTrap(Canvas, 0, 1, -12, true, 3, RuinDanger.dartAmmo(Random, Tier), false);
        RuinDanger.collapse(Canvas, -1, -8, 1, -7, 0, 4, RuinDanger.PitMagma, RuinMix.DarkBrick);
        List<int[]> Spots = new ArrayList<>();
        for (int Level = 0; Level < Halves.length; Level++) {
            int Half = Halves[Level];
            Spots.addAll(RuinDanger.floorSpots(Canvas, -Half, Level * 3 + 3, -Half, Half, Half));
        }
        Spots.addAll(RuinDanger.floorSpots(Canvas, -4, 1, -4, 4, 4));
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.brush(17, 0, 6, true);
        Canvas.brush(-17, 0, -9, false);
        Canvas.entrance(0, 1, 18, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void arena(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        double A = 15.0;
        double B = 12.0;
        int Tiers = 4;
        int PitFloor = -7;
        double Pit = 0.5;
        Canvas.scorchGround(0, 0, 19.0, 0.3, 221);
        int Reach = (int) A + 3;
        for (int X = -Reach; X <= Reach; X++) {
            for (int Z = -Reach; Z <= Reach; Z++) {
                double E = Math.sqrt((X / A) * (X / A) + (Z / B) * (Z / B));
                if (E > 1.16) {
                    continue;
                }
                if (E > 1.0) {
                    Canvas.put(X, 0, Z, RuinMix.Plinth);
                    for (int Y = 1; Y <= 5; Y++) {
                        Canvas.put(X, Y, Z, Y == 5 ? RuinMix.Trim.at(Random, X, Y, Z) : RuinMix.Dressed.at(Random, X, Y, Z));
                    }
                    double Angle = Math.atan2(Z / B, X / A);
                    int Sector = (int) Math.floor((Angle + Math.PI) / (Math.PI * 2) * 24);
                    if (Sector % 2 == 0) {
                        Canvas.air(X, 1, Z);
                        Canvas.air(X, 2, Z);
                        Canvas.air(X, 3, Z);
                    }
                    continue;
                }
                if (E >= Pit) {
                    double Span = (1.0 - Pit) / Tiers;
                    int Ring = Math.min(Tiers - 1, (int) Math.floor((1.0 - E) / Span));
                    int Seat = -Ring;
                    for (int Y = PitFloor; Y < Seat; Y++) {
                        Canvas.put(X, Y, Z, RuinMix.Rough);
                    }
                    int Out = RuinCanvas.faceToward(X, Z);
                    if (Ring == Tiers - 1) {
                        for (int Y = PitFloor + 1; Y <= Seat; Y++) {
                            Canvas.put(X, Y, Z, RuinMix.DarkBrick);
                        }
                    } else {
                        Canvas.stairs(X, Seat, Z, Ring % 2 == 0 ? RuinMat.RedSandstoneStairs : RuinMat.BlackstoneBrickStairs, Out, false);
                    }
                    for (int Y = Seat + 1; Y <= 2; Y++) {
                        Canvas.air(X, Y, Z);
                    }
                    continue;
                }
                Canvas.put(X, PitFloor, Z, Random.at(X, PitFloor, Z, 222) < 0.12 ? RuinMat.Magma : RuinMix.of(RuinMat.CoarseDirt, 40, RuinMat.RedSand, 35, RuinMat.Gravel, 25)
                        .at(Random, X, PitFloor, Z));
                Canvas.put(X, PitFloor - 1, Z, RuinMix.Plinth);
                for (int Y = PitFloor + 1; Y <= 2; Y++) {
                    Canvas.air(X, Y, Z);
                }
            }
        }
        Canvas.box(-1, PitFloor + 1, -1, 1, PitFloor + 6, 1, RuinMix.Plinth);
        Canvas.greatBrazier(0, PitFloor + 7, 0, true, true);
        for (int[] Chain : new int[][]{{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            Canvas.pillar(Chain[0], PitFloor + 3, PitFloor + 5, Chain[1], RuinMat.Chain);
            Canvas.put(Chain[0], PitFloor + 2, Chain[1], RuinMat.IronBars);
            Canvas.put(Chain[0], PitFloor + 1, Chain[1], RuinMat.IronBars);
        }
        int Gate = (int) Math.round(B * Pit);
        for (int Side : new int[]{-1, 1}) {
            int Z0 = Side * Gate;
            for (int Step = 0; Step <= 7; Step++) {
                int Z = Z0 + Side * Step;
                for (int X = -2; X <= 2; X++) {
                    for (int Y = PitFloor; Y <= PitFloor + 4; Y++) {
                        boolean Inside = Math.abs(X) <= 1 && Y >= PitFloor + 1 && Y <= PitFloor + 3;
                        if (Inside) {
                            Canvas.air(X, Y, Z);
                        } else {
                            Canvas.put(X, Y, Z, RuinMix.DarkBrick);
                        }
                    }
                }
            }
            for (int X = -1; X <= 1; X++) {
                Canvas.put(X, PitFloor + 1, Z0, RuinMat.IronBars);
                Canvas.put(X, PitFloor + 2, Z0, RuinMat.IronBars);
                Canvas.put(X, PitFloor + 3, Z0, RuinMat.IronBars);
            }
            Canvas.air(0, PitFloor + 1, Z0);
            Canvas.air(0, PitFloor + 2, Z0);
            int Pen = Z0 + Side * 6;
            if (Side < 0) {
                Canvas.spawner(0, PitFloor + 1, Pen, RuinDanger.spawnerKind(Random, Tier));
            } else {
                Canvas.chest(-1, PitFloor + 1, Pen + Side, RuinMat.North, RuinCanvas.LootVault);
                Canvas.chest(1, PitFloor + 1, Pen + Side, RuinMat.North, RuinCanvas.LootCache);
                RuinDanger.dartTrap(Canvas, 0, PitFloor + 1, Z0 + Side * 3, true, 3, RuinDanger.dartAmmo(Random, Tier), false);
            }
        }
        RuinKit.wallBanner(Canvas, 0, PitFloor + 4, Gate, RuinMat.North);
        for (int Index = 0; Index < 4; Index++) {
            double Angle = Index * Math.PI / 2 + Math.PI / 4;
            int X = (int) Math.round(Math.cos(Angle) * (A + 0.5));
            int Z = (int) Math.round(Math.sin(Angle) * (B + 0.5));
            Canvas.greatBrazier(X, 6, Z, false, false);
        }
        RuinDanger.ventFloor(Canvas, 4, PitFloor, 3, 2.5, 0.35, 223);
        RuinDanger.ventFloor(Canvas, -4, PitFloor, -2, 2.5, 0.35, 224);
        List<int[]> Spots = new ArrayList<>(RuinDanger.floorSpots(Canvas, -6, PitFloor + 1, -5, 6, 5));
        for (int Ring = 0; Ring < 3; Ring++) {
            Spots.addAll(RuinDanger.floorSpots(Canvas, -(int) A, -Ring + 1, -(int) B, (int) A, (int) B));
        }
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.brush(-17, 0, 3, true);
        Canvas.entrance(0, 1, (int) B + 4, RuinMat.South);
        Canvas.centre(0, PitFloor + 1, 0);
    }

    public static void palace(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        int Hx = 12;
        int Hz = 10;
        Canvas.scorchGround(0, 0, 21.0, 0.35, 251);
        Canvas.box(-Hx, 0, -Hz, Hx, 0, Hz, RuinMix.Plinth);
        RuinKit.mosaic(Canvas, -Hx + 3, -Hz + 3, Hx - 3, Hz - 3, 0);
        RuinKit.wall(Canvas, -Hx, -Hz, Hx, -Hz, 1, 8, RuinMix.Dressed, RuinMix.Plinth);
        RuinKit.wall(Canvas, -Hx, -Hz + 1, -Hx, Hz, 1, 8, RuinMix.Dressed, RuinMix.Plinth);
        RuinKit.wall(Canvas, Hx, -Hz + 1, Hx, Hz, 1, 8, RuinMix.Dressed, RuinMix.Plinth);
        for (int Z = -Hz + 2; Z <= Hz - 2; Z += 3) {
            Canvas.column(-Hx + 3, 1, Z, 7, true);
            Canvas.column(Hx - 3, 1, Z, 7, true);
        }
        for (int X = -Hx + 3; X <= Hx - 3; X += 3) {
            Canvas.column(X, 1, -Hz + 3, 7, true);
        }
        RuinKit.roof(Canvas, -Hx, -Hz, -Hx + 3, Hz, 8, RuinMat.BlackstoneBrickSlab, 0.7, 252);
        RuinKit.roof(Canvas, Hx - 3, -Hz, Hx, Hz, 8, RuinMat.BlackstoneBrickSlab, 0.7, 253);
        RuinKit.roof(Canvas, -Hx, -Hz, Hx, -Hz + 3, 8, RuinMat.BlackstoneBrickSlab, 0.7, 254);
        for (int X = -3; X <= 3; X++) {
            for (int Z = -2; Z <= 2; Z++) {
                boolean Rim = Math.abs(X) == 3 || Math.abs(Z) == 2;
                if (Rim) {
                    Canvas.put(X, 0, Z, RuinMix.Plinth);
                    Canvas.put(X, 1, Z, RuinMix.Trim);
                } else {
                    Canvas.air(X, 0, Z);
                    Canvas.put(X, -1, Z, Random.at(X, -1, Z, 255) < 0.6 ? RuinMat.RedSand : RuinMat.Gravel);
                }
            }
        }
        Canvas.put(0, -1, 0, RuinMix.Plinth);
        Canvas.put(0, 0, 0, RuinMix.Plinth);
        RuinKit.statue(Canvas, 0, 1, 0, RuinMat.South, 9.0, RuinFigures.overseer(40), false);
        int Fz = Hz;
        RuinKit.wall(Canvas, -Hx, Fz, Hx, Fz + 1, 1, 12, RuinMix.Dressed, RuinMix.Plinth);
        for (int Side : new int[]{-1, 1}) {
            int X1 = Side * 3;
            int X2 = Side * 7;
            RuinKit.ring(Canvas, X1, Fz - 1, X2, Fz + 3, 1, 15, RuinMix.Dressed, RuinMix.Plinth);
            Canvas.clear(Math.min(X1, X2) + 1, 1, Fz, Math.max(X1, X2) - 1, 15, Fz + 2);
            RuinKit.merlons(Canvas, Math.min(X1, X2), Fz + 3, Math.max(X1, X2), Fz + 3, 16, RuinMix.Trim);
            RuinKit.wallBanner(Canvas, (X1 + X2) / 2, 9, Fz + 4, RuinMat.South);
            Canvas.put((X1 + X2) / 2, 12, Fz + 3, RuinMat.RuinReliefGod, RuinMat.South);
        }
        RuinKit.opening(Canvas, 0, 1, Fz, true, 3, 6, true);
        RuinKit.opening(Canvas, 0, 1, Fz + 1, true, 3, 6, true);
        for (int Y = 1; Y <= 5; Y++) {
            Canvas.put(-2, Y, Fz + 2, RuinMix.Brass);
            if (Y <= 2) {
                Canvas.put(2, Y, Fz + 3, RuinMix.Brass);
            }
        }
        for (int Z = Fz + 4; Z <= Fz + 6; Z++) {
            Canvas.put(3, 1, Z, RuinMix.Brass);
        }
        for (int Side : new int[]{-1, 1}) {
            int Wx1 = Side * (Hx + 1);
            int Wx2 = Side * (Hx + 9);
            Canvas.box(Math.min(Wx1, Wx2), 0, -8, Math.max(Wx1, Wx2), 0, 8, RuinMix.Plinth);
            RuinKit.ring(Canvas, Wx1, -8, Wx2, 8, 1, 6, RuinMix.Dressed, RuinMix.Plinth);
            Canvas.clear(Math.min(Wx1, Wx2) + 1, 1, -7, Math.max(Wx1, Wx2) - 1, 6, 7);
            RuinKit.wall(Canvas, Side * (Hx + 5), -7, Side * (Hx + 5), 7, 1, 6, RuinMix.Dressed, RuinMix.Plinth);
            RuinKit.opening(Canvas, Side * Hx, 1, 0, false, 1, 3, false);
            RuinKit.opening(Canvas, Wx1, 1, 0, false, 1, 3, false);
            RuinKit.opening(Canvas, Side * (Hx + 5), 1, -3, false, 1, 3, false);
            RuinKit.roof(Canvas, Wx1, -8, Wx2, 8, 7, RuinMat.BlackstoneBrickSlab, 0.45, 256 + Side);
            if (Side < 0) {
                Canvas.spawner(Side * (Hx + 7), 1, 3, RuinDanger.spawnerKind(Random, Tier));
                Canvas.chest(Side * (Hx + 3), 1, -6, RuinMat.South, RuinCanvas.LootCache);
            } else {
                Canvas.chest(Side * (Hx + 7), 1, -6, RuinMat.South, RuinCanvas.LootVault);
                RuinDanger.dartTrap(Canvas, Side * (Hx + 3), 1, 0, true, 4, RuinDanger.dartAmmo(Random, Tier), false);
                for (int Z = -6; Z <= 6; Z += 3) {
                    Canvas.put(Side * (Hx + 8), 1, Z, RuinMat.RuinUrn, RuinMat.West);
                }
            }
        }
        RuinDanger.collapse(Canvas, -1, 4, 1, 7, 0, 4, RuinDanger.PitMagma, RuinMix.Plinth);
        RuinDanger.dartTrap(Canvas, 0, 1, Fz + 1, true, 3, RuinDanger.dartAmmo(Random, Tier), true);
        Canvas.chest(-Hx + 1, 1, -Hz + 1, RuinMat.South, RuinCanvas.LootCache);
        List<int[]> Spots = new ArrayList<>(RuinDanger.floorSpots(Canvas, -Hx + 1, 1, -Hz + 1, Hx - 1, Hz - 1));
        Spots.addAll(RuinDanger.floorSpots(Canvas, -Hx - 8, 1, -7, Hx + 8, 7));
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier) + 1);
        Canvas.brush(Hx + 3, 0, Hz + 4, false);
        Canvas.entrance(0, 1, Fz + 5, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void idol(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        int[] Halves = {9, 7, 5};
        Canvas.scorchGround(0, 4, 20.0, 0.4, 261);
        for (int Level = 0; Level < Halves.length; Level++) {
            int Half = Halves[Level];
            int Top = Level * 3 + 2;
            RuinKit.platform(Canvas, -Half, -Half, Half, Half, Top, RuinMix.Rough, RuinMix.Dressed);
            RuinKit.band(Canvas, -Half, -Half, Half, Half, Top, RuinMix.Trim);
            for (int X = -Half; X <= Half; X++) {
                for (int Z = -Half; Z <= Half; Z++) {
                    if (Math.abs(X) == Half || Math.abs(Z) == Half) {
                        Canvas.put(X, Level * 3, Z, RuinMix.Plinth);
                    }
                }
            }
        }
        int Top = 8;
        for (int Step = 0; Step < Top; Step++) {
            int Y = Step + 1;
            int Z = 10 - Step;
            for (int X = -2; X <= 2; X++) {
                Canvas.stairs(X, Y, Z, RuinMat.SmoothRedSandstoneStairs, RuinMat.North, false);
                for (int Fill = Y - 1; Fill >= 0; Fill--) {
                    if (!Canvas.filled(X, Fill, Z)) {
                        Canvas.put(X, Fill, Z, RuinMix.Rough);
                    }
                }
                Canvas.protect(X, Y, Z);
                Canvas.air(X, Y + 1, Z);
                Canvas.air(X, Y + 2, Z);
            }
        }
        RuinKit.statue(Canvas, 0, Top + 1, -1, RuinMat.South, 1.8, RuinFigures.god(true), false);
        Canvas.box(-5, 0, 11, 5, 0, 18, RuinMix.Paving);
        for (int Z = 12; Z <= 18; Z += 3) {
            Canvas.pedestalBrazier(-5, 1, Z, true);
            Canvas.pedestalBrazier(5, 1, Z, true);
        }
        Canvas.box(-1, 1, 15, 1, 1, 16, RuinMix.DarkBrick);
        Canvas.candles(-1, 2, 15, 3);
        Canvas.candles(1, 2, 16, 2);
        Canvas.pot(0, 2, 15, RuinMat.South, RuinCanvas.LootOffering);
        RuinDanger.collapse(Canvas, -2, 12, 2, 13, 0, 4, RuinDanger.PitMagma, RuinMix.Paving);
        for (int Index = 0; Index < 6; Index++) {
            double Angle = Index * Math.PI / 3 + Math.PI / 6;
            int X = (int) Math.round(Math.cos(Angle) * 13);
            int Z = (int) Math.round(Math.sin(Angle) * 13);
            if (Z > 9 && Math.abs(X) < 7) {
                continue;
            }
            Canvas.greatBrazier(X, 1, Z, true, Index % 2 == 0);
        }
        for (int Z = -9; Z >= -14; Z--) {
            for (int X = -1; X <= 1; X++) {
                Canvas.air(X, 1, Z);
                Canvas.air(X, 2, Z);
                Canvas.put(X, 0, Z, RuinMix.DarkBrick);
            }
        }
        Canvas.box(-2, 0, -15, 2, 3, -15, RuinMix.Dressed);
        Canvas.air(0, 1, -15);
        Canvas.air(0, 2, -15);
        for (int Z = -14; Z <= -10; Z++) {
            Canvas.put(-2, 1, Z, RuinMix.Dressed);
            Canvas.put(2, 1, Z, RuinMix.Dressed);
            Canvas.put(-2, 2, Z, RuinMix.Dressed);
            Canvas.put(2, 2, Z, RuinMix.Dressed);
            Canvas.put(-1, 3, Z, RuinMix.Dressed);
            Canvas.put(0, 3, Z, RuinMix.Dressed);
            Canvas.put(1, 3, Z, RuinMix.Dressed);
        }
        Canvas.clear(-4, 1, -8, 4, 2, -2);
        RuinKit.mosaic(Canvas, -4, -8, 4, -2, 0);
        Canvas.clear(-1, 1, -9, 1, 2, -9);
        Canvas.spawner(0, 1, -5, RuinDanger.spawnerKind(Random, Tier));
        Canvas.chest(-3, 1, -3, RuinMat.North, RuinCanvas.LootVault);
        Canvas.chest(3, 1, -7, RuinMat.West, RuinCanvas.LootCache);
        RuinDanger.dartTrap(Canvas, 0, 1, -12, true, 3, RuinDanger.dartAmmo(Random, Tier), false);
        List<int[]> Spots = new ArrayList<>();
        Spots.addAll(RuinDanger.floorSpots(Canvas, -16, 1, -16, 16, 18));
        for (int Level = 0; Level < Halves.length; Level++) {
            Spots.addAll(RuinDanger.floorSpots(Canvas, -Halves[Level], Level * 3 + 3, -Halves[Level], Halves[Level], Halves[Level]));
        }
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.brush(-12, 0, 12, true);
        Canvas.entrance(0, 1, 20, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }
}
