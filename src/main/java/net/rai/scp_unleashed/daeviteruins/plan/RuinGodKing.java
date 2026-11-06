package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayList;
import java.util.List;

public final class RuinGodKing {
    private RuinGodKing() {
    }

    public static void crownForge(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        int Hw = 12;
        int Hl = 16;
        int Height = 12;
        Canvas.scorchGround(0, 0, 24.0, 0.6, 281);
        Canvas.box(-Hw - 2, 0, -Hl - 2, Hw + 2, 0, Hl + 2, RuinMix.Plinth);
        RuinKit.ring(Canvas, -Hw, -Hl, Hw, Hl, 1, Height, RuinMix.DarkBrick, RuinMix.Plinth);
        RuinKit.band(Canvas, -Hw, -Hl, Hw, Hl, 4, RuinMix.Dressed);
        RuinKit.band(Canvas, -Hw, -Hl, Hw, Hl, 8, RuinMix.Brass);
        Canvas.clear(-Hw + 1, 1, -Hl + 1, Hw - 1, Height + 2, Hl - 1);
        Canvas.box(-Hw + 1, 0, -Hl + 1, Hw - 1, 0, Hl - 1, RuinMix.of(RuinMat.SmoothBasalt, 35, RuinMat.PolishedBlackstone, 25, RuinMat.Blackstone, 20, RuinMat.CoalBlock, 8,
                RuinMat.CrackedBlackstoneBricks, 12));
        for (int Z = -Hl + 5; Z <= Hl - 4; Z++) {
            for (int X = -1; X <= 0; X++) {
                Canvas.put(X, -1, Z, RuinMat.SmoothBasalt);
                Canvas.put(X, 0, Z, RuinMat.Lava);
                Canvas.keep(X, 0, Z);
            }
            Canvas.put(-2, 0, Z, RuinMat.PolishedBasalt, RuinMat.Up);
            Canvas.put(1, 0, Z, RuinMat.PolishedBasalt, RuinMat.Up);
            Canvas.put(-2, 1, Z, RuinMat.BlackstoneWall);
            Canvas.put(1, 1, Z, RuinMat.BlackstoneWall);
            Canvas.keep(-2, 0, Z);
            Canvas.keep(1, 0, Z);
            Canvas.keep(-2, 1, Z);
            Canvas.keep(1, 1, Z);
        }
        for (int X = -2; X <= 1; X++) {
            Canvas.put(X, 0, -Hl + 4, RuinMat.SmoothBasalt);
            Canvas.put(X, 0, Hl - 3, RuinMat.SmoothBasalt);
            Canvas.put(X, 1, -Hl + 4, RuinMat.BlackstoneWall);
            Canvas.put(X, 1, Hl - 3, RuinMat.BlackstoneWall);
            Canvas.keep(X, 0, -Hl + 4);
            Canvas.keep(X, 0, Hl - 3);
        }
        for (int Index = 0; Index < 3; Index++) {
            int Cx = -8 + Index * 8;
            int Cz = -Hl - 2;
            Canvas.box(Cx - 2, 0, Cz - 2, Cx + 2, 0, Cz + 2, RuinMix.Plinth);
            for (int Y = 1; Y <= 24; Y++) {
                Canvas.ring(Cx, Y, Cz, 1.5, 2.6, RuinMix.DarkBrick);
            }
            for (int Y = 1; Y <= 24; Y++) {
                Canvas.discAir(Cx, Y, Cz, 1.4);
            }
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    Canvas.put(Cx + Dx, 0, Cz + Dz, Dx == 0 && Dz == 0 ? RuinMat.Netherrack : RuinMat.Magma);
                }
            }
            Canvas.put(Cx, 1, Cz, RuinMat.Fire);
            Canvas.keep(Cx, 0, Cz);
            Canvas.keep(Cx, 1, Cz);
            Canvas.clear(Cx - 1, 1, Cz + 2, Cx + 1, 3, Cz + 3);
            for (int Dx = -1; Dx <= 1; Dx++) {
                Canvas.put(Cx + Dx, 0, Cz + 2, RuinMat.Magma);
                Canvas.put(Cx + Dx, 0, Cz + 3, RuinMat.CoalBlock);
            }
            Canvas.spawner(Cx + (Index - 1) * 3, 1, -Hl + 3, Index == 1 ? RuinDanger.Blaze : RuinDanger.spawnerKind(Random, Tier));
        }
        for (int[] Anvil : new int[][]{{-7, -6}, {6, -6}, {-7, 4}, {6, 4}, {-5, 10}}) {
            Canvas.put(Anvil[0], 1, Anvil[1], RuinMat.PolishedBasalt, RuinMat.Up);
            Canvas.put(Anvil[0], 2, Anvil[1], Random.chance(0.5) ? RuinMat.DamagedAnvil : RuinMat.ChippedAnvil, RuinMat.East);
        }
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                int X = 7 + Dx;
                int Z = 11 + Dz;
                if (Dx == 0 && Dz == 0) {
                    Canvas.put(X, 0, Z, RuinMat.Magma);
                    Canvas.put(X, 1, Z, RuinMat.Fire);
                } else {
                    Canvas.put(X, 1, Z, RuinMat.Cauldron);
                }
            }
        }
        for (int Z = -4; Z <= 2; Z += 2) {
            Canvas.put(-9, 1, Z, RuinMat.Cauldron);
        }
        for (int[] Heap : new int[][]{{9, -10}, {-10, -10}, {10, 0}}) {
            Canvas.rubble(Heap[0], Heap[1], 1.6, 2, 282);
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    if (Canvas.filled(Heap[0] + Dx, 1, Heap[1] + Dz) && Canvas.at(Heap[0] + Dx, 1, Heap[1] + Dz).isOpaqueCube()) {
                        Canvas.put(Heap[0] + Dx, 1, Heap[1] + Dz, RuinMat.CoalBlock);
                    }
                }
            }
        }
        int Altar = Hl - 4;
        for (int X = -3; X <= 3; X++) {
            for (int Z = Altar - 2; Z <= Altar + 2; Z++) {
                Canvas.put(X, 1, Z, RuinMix.Plinth);
            }
        }
        for (int Step = 0; Step < 24; Step++) {
            double Angle = Step * Math.PI * 2 / 24;
            int X = (int) Math.round(Math.cos(Angle) * 2.2);
            int Z = Altar + (int) Math.round(Math.sin(Angle) * 1.6);
            Canvas.put(X, 2, Z, Step % 4 == 0 ? RuinMat.GoldBlock : RuinMat.GildedBlackstone);
            if (Step % 4 == 0) {
                Canvas.put(X, 3, Z, RuinMat.LightningRod, RuinMat.Up);
            }
        }
        for (int X = -2; X <= 2; X++) {
            for (int Y = 2; Y <= 5; Y++) {
                Canvas.put(X, Y, Hl - 1, RuinMix.Dressed);
            }
        }
        Canvas.put(0, 4, Hl - 2, RuinMat.RuinReliefGod, RuinMat.North);
        Canvas.keep(0, 4, Hl - 2);
        Canvas.chest(-3, 2, Altar + 2, RuinMat.North, RuinCanvas.LootVault);
        Canvas.chest(3, 2, Altar + 2, RuinMat.North, RuinCanvas.LootVault);
        Canvas.chest(-Hw + 1, 1, 0, RuinMat.East, RuinCanvas.LootCache);
        RuinKit.opening(Canvas, 0, 1, -Hl, true, 3, 5, true);
        RuinKit.opening(Canvas, Hw, 1, 0, false, 3, 5, true);
        RuinKit.opening(Canvas, -Hw, 1, 0, false, 3, 5, true);
        RuinKit.beams(Canvas, -Hw, -Hl, Hw, Hl, Height + 1, false, 4, RuinMat.StrippedDarkOakLog);
        RuinKit.roof(Canvas, -Hw, -Hl, Hw, Hl, Height + 2, RuinMat.BlackstoneBrickSlab, 0.3, 283);
        for (int Z = -Hl + 3; Z <= Hl - 3; Z += 4) {
            Canvas.column(-Hw + 3, 1, Z, Height, true);
            Canvas.column(Hw - 3, 1, Z, Height, true);
        }
        RuinDanger.ventFloor(Canvas, 7, 0, 11, 3.0, 0.5, 284);
        RuinDanger.ventFloor(Canvas, -8, 0, -8, 3.5, 0.4, 285);
        RuinDanger.dartTrap(Canvas, Hw - 1, 1, 0, false, 4, RuinDanger.FireCharges, true);
        RuinDanger.dartTrap(Canvas, -Hw + 1, 1, 0, false, 4, RuinDanger.FireCharges, false);
        RuinDanger.dartTrap(Canvas, 0, 1, -Hl + 1, true, Hw - 2, RuinDanger.dartAmmo(Random, Tier), true);
        RuinDanger.collapse(Canvas, 3, -2, 5, 1, 0, 5, RuinDanger.PitMagma, RuinMix.DarkBrick);
        RuinDanger.collapse(Canvas, -5, 6, -3, 8, 0, 5, RuinDanger.PitMagma, RuinMix.DarkBrick);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -Hw + 1, 1, -Hl + 1, Hw - 1, Hl - 1);
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.brush(Hw + 4, 0, -5, true);
        Canvas.entrance(0, 1, -Hl - 6, RuinMat.North);
        Canvas.centre(0, 1, 0);
    }

    public static void warAltar(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        int[] Halves = {17, 13, 9};
        Canvas.scorchGround(0, 0, 24.0, 0.55, 291);
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
        int[][] Ways = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] Way : Ways) {
            for (int Step = 0; Step < Top; Step++) {
                int Y = Step + 1;
                int Along = 18 - Step;
                boolean Landing = Step == 3;
                for (int Across = -2; Across <= 2; Across++) {
                    int X = Way[0] != 0 ? Way[0] * Along : Across;
                    int Z = Way[1] != 0 ? Way[1] * Along : Across;
                    int Facing = Way[0] > 0 ? RuinMat.West : Way[0] < 0 ? RuinMat.East : Way[1] > 0 ? RuinMat.North : RuinMat.South;
                    if (Landing) {
                        Canvas.put(X, Y, Z, RuinMix.Plinth);
                    } else {
                        Canvas.stairs(X, Y, Z, RuinMat.BlackstoneBrickStairs, Facing, false);
                    }
                    for (int Fill = Y - 1; Fill >= 0; Fill--) {
                        if (!Canvas.filled(X, Fill, Z)) {
                            Canvas.put(X, Fill, Z, RuinMix.Rough);
                        }
                    }
                    Canvas.protect(X, Y, Z);
                    Canvas.air(X, Y + 1, Z);
                    Canvas.air(X, Y + 2, Z);
                }
                for (int Side : new int[]{-3, 3}) {
                    int X = Way[0] != 0 ? Way[0] * Along : Side;
                    int Z = Way[1] != 0 ? Way[1] * Along : Side;
                    Canvas.put(X, Y, Z, RuinMix.Dressed);
                    Canvas.put(X, Y + 1, Z, RuinMat.BlackstoneBrickWall);
                }
            }
            int Lx = Way[0] * 15;
            int Lz = Way[1] * 15;
            if (Way[1] != 0) {
                RuinDanger.collapse(Canvas, -2, Lz - (Way[1] > 0 ? 1 : 0), 2, Lz + (Way[1] < 0 ? 1 : 0), 4, 4, RuinDanger.PitMagma, RuinMix.DarkBrick);
            } else {
                RuinDanger.collapse(Canvas, Lx - (Way[0] > 0 ? 1 : 0), -2, Lx + (Way[0] < 0 ? 1 : 0), 2, 4, 4, RuinDanger.PitMagma, RuinMix.DarkBrick);
            }
            int Px = Way[0] * 10;
            int Pz = Way[1] * 10;
            RuinDanger.dartTrap(Canvas, Px, Top + 1, Pz, Way[1] != 0, 4, RuinDanger.dartAmmo(Random, Tier), Way[0] != 0);
        }
        int Circle = Top;
        for (int X = -8; X <= 8; X++) {
            for (int Z = -8; Z <= 8; Z++) {
                double Distance = Math.sqrt(X * X + Z * Z);
                double Angle = Math.atan2(Z, X);
                int Spoke = (int) Math.round((Angle + Math.PI) / (Math.PI / 3));
                double Phase = (Angle + Math.PI) / (Math.PI / 3) - Spoke;
                RuinMat Mat = null;
                if (Distance > 6.4 && Distance <= 7.5) {
                    Mat = RuinMat.RedNetherBricks;
                } else if (Distance > 4.4 && Distance <= 5.3) {
                    Mat = RuinMat.GildedBlackstone;
                } else if (Distance > 1.6 && Distance <= 6.4 && Math.abs(Phase) < 0.12) {
                    Mat = RuinMat.RedTerracotta;
                } else if (Distance > 5.3 && Distance <= 6.4 && Math.abs(Phase) > 0.4) {
                    Mat = RuinMat.ChiseledBlackstone;
                }
                if (Mat != null) {
                    Canvas.put(X, Circle, Z, Mat);
                    Canvas.keep(X, Circle, Z);
                }
            }
        }
        Canvas.box(-1, Top + 1, -1, 1, Top + 1, 1, RuinMix.DarkBrick);
        Canvas.put(0, Top + 1, -2, RuinMat.RuinReliefGod, RuinMat.North);
        Canvas.put(0, Top + 1, 2, RuinMat.RuinReliefGod, RuinMat.South);
        Canvas.put(-2, Top + 1, 0, RuinMat.RuinReliefGod, RuinMat.West);
        Canvas.put(2, Top + 1, 0, RuinMat.RuinReliefGod, RuinMat.East);
        Canvas.greatBrazier(0, Top + 2, 0, true, true);
        for (int Index = 0; Index < 6; Index++) {
            double Angle = Index * Math.PI / 3;
            int X = (int) Math.round(Math.cos(Angle) * 8.5);
            int Z = (int) Math.round(Math.sin(Angle) * 8.5);
            Canvas.spear(X, Top + 1, Z, 2, true);
            Canvas.spear(X + (Index % 2 == 0 ? 1 : 0), Top + 1, Z + (Index % 2 == 0 ? 0 : 1), 3, true);
        }
        for (int[] Corner : new int[][]{{-15, -15}, {15, -15}, {-15, 15}, {15, 15}}) {
            Canvas.box(Corner[0] - 1, 3, Corner[1] - 1, Corner[0] + 1, 3, Corner[1] + 1, RuinMix.Plinth);
            for (int Y = 4; Y <= 13; Y++) {
                Canvas.box(Corner[0], Y, Corner[1], Corner[0] + (Y < 10 ? 1 : 0), Y, Corner[1] + (Y < 10 ? 1 : 0), Y % 5 == 0 ? RuinMix.Trim : RuinMix.Dressed);
            }
            Canvas.put(Corner[0], 14, Corner[1], RuinMat.GoldBlock);
            Canvas.put(Corner[0], 15, Corner[1], RuinMat.LightningRod, RuinMat.Up);
        }
        for (int[] Cage : new int[][]{{-11, -11}, {11, 11}}) {
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    for (int Y = 6; Y <= 8; Y++) {
                        if (Math.abs(Dx) == 1 || Math.abs(Dz) == 1) {
                            Canvas.put(Cage[0] + Dx, Y, Cage[1] + Dz, RuinMat.IronBars);
                        }
                    }
                    Canvas.put(Cage[0] + Dx, 9, Cage[1] + Dz, RuinMat.PolishedBlackstoneSlab, RuinMat.South, 0);
                }
            }
            Canvas.spawner(Cage[0], 6, Cage[1], RuinDanger.spawnerKind(Random, Tier));
        }
        Canvas.chest(-3, Top + 1, -3, RuinMat.South, RuinCanvas.LootVault);
        Canvas.chest(3, Top + 1, 3, RuinMat.North, RuinCanvas.LootVault);
        Canvas.chest(-12, 6, 4, RuinMat.East, RuinCanvas.LootCache);
        Canvas.chest(12, 3, -15, RuinMat.West, RuinCanvas.LootCache);
        List<int[]> Spots = new ArrayList<>();
        for (int Level = 0; Level < Halves.length; Level++) {
            Spots.addAll(RuinDanger.floorSpots(Canvas, -Halves[Level], Level * 3 + 3, -Halves[Level], Halves[Level], Halves[Level]));
        }
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier) + 1);
        Canvas.brush(20, 0, 8, true);
        Canvas.brush(-20, 0, -6, false);
        Canvas.entrance(0, 1, 21, RuinMat.South);
        Canvas.centre(0, Top + 1, 0);
    }

    public static void godGate(RuinCanvas Canvas) {
        RuinEmpire.gate(Canvas, 2);
    }

    public static void procession(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        int Length = 30;
        Canvas.scorchGround(0, 0, 14.0, 0.3, 311);
        for (int Z = -Length; Z <= Length; Z++) {
            for (int X = -4; X <= 4; X++) {
                RuinMat Stone = RuinMix.Paving.at(Random, X, 0, Z);
                if (Stone == RuinMat.Gravel || Stone == RuinMat.CoarseDirt) {
                    Stone = Random.at(X, 1, Z, 312) < 0.5 ? RuinMat.SmoothBasalt : RuinMat.CutRedSandstone;
                }
                if (X == 0 && (Z & 1) == 0) {
                    Stone = RuinMat.GildedBlackstone;
                }
                Canvas.put(X, 0, Z, Stone);
            }
            for (int Side : new int[]{-5, 5}) {
                Canvas.put(Side, 0, Z, RuinMix.Plinth);
                if (Random.at(Side, 1, Z, 313) < 0.75) {
                    Canvas.slab(Side, 1, Z, RuinMat.PolishedBlackstoneSlab, false);
                }
            }
        }
        int Count = 0;
        for (int Z = -Length + 6; Z <= Length - 4; Z += 6) {
            for (int Side : new int[]{-1, 1}) {
                int X = Side * 8;
                Canvas.box(X - 1, 0, Z - 1, X + 1, 1, Z + 1, RuinMix.Plinth);
                RuinKit.band(Canvas, X - 1, Z - 1, X + 1, Z + 1, 1, RuinMix.Trim);
                RuinFigures.Figure Shape = (Count + (Side < 0 ? 0 : 1)) % 2 == 0 ? RuinFigures.knight(true, 30) : RuinFigures.overseer(20);
                RuinKit.statue(Canvas, X, 2, Z, Side < 0 ? RuinMat.East : RuinMat.West, 4.2, Shape, false);
            }
            Count++;
            if (Z + 3 <= Length - 4) {
                Canvas.pedestalBrazier(-6, 1, Z + 3, false);
                Canvas.pedestalBrazier(6, 1, Z + 3, false);
            }
        }
        int Gz = -Length - 1;
        for (int Side : new int[]{-1, 1}) {
            int X1 = Side * 3;
            int X2 = Side * 8;
            Canvas.box(Math.min(X1, X2), 0, Gz - 4, Math.max(X1, X2), 0, Gz, RuinMix.Plinth);
            RuinKit.ring(Canvas, X1, Gz - 4, X2, Gz, 1, 16, RuinMix.Dressed, RuinMix.Plinth);
            Canvas.clear(Math.min(X1, X2) + 1, 1, Gz - 3, Math.max(X1, X2) - 1, 16, Gz - 1);
            RuinKit.merlons(Canvas, Math.min(X1, X2), Gz, Math.max(X1, X2), Gz, 17, RuinMix.Trim);
            Canvas.put((X1 + X2) / 2, 12, Gz + 1, RuinMat.RuinReliefGod, RuinMat.South);
            RuinKit.wallBanner(Canvas, (X1 + X2) / 2, 8, Gz + 1, RuinMat.South);
        }
        Canvas.box(-2, 0, Gz - 4, 2, 0, Gz, RuinMix.Paving);
        Canvas.box(-2, 11, Gz - 4, 2, 13, Gz, RuinMix.Dressed);
        RuinKit.merlons(Canvas, -2, Gz, 2, Gz, 14, RuinMix.Trim);
        Canvas.keepClearBox(-2, 1, Gz - 4, 2, 3, Gz);
        RuinDanger.dartTrap(Canvas, 0, 1, Gz - 1, true, 4, RuinDanger.FireCharges, false);
        RuinDanger.dartTrap(Canvas, 0, 1, Gz - 3, true, 4, RuinDanger.dartAmmo(Random, Tier), true);
        int Pz = Gz - 5;
        Canvas.box(-7, 0, Pz - 10, 7, 3, Pz, RuinMix.Rough);
        RuinKit.band(Canvas, -7, Pz - 10, 7, Pz, 3, RuinMix.Trim);
        for (int Step = 0; Step < 3; Step++) {
            for (int X = -2; X <= 2; X++) {
                Canvas.stairs(X, Step + 1, Pz - Step, RuinMat.SmoothRedSandstoneStairs, RuinMat.North, false);
            }
        }
        RuinEmpire.throne(Canvas, 0, 4, Pz - 8, true);
        for (int X = -4; X <= 4; X++) {
            for (int Y = 4; Y <= 8; Y++) {
                Canvas.put(X, Y, Pz - 10, Y == 8 ? RuinMix.Trim.at(Random, X, Y, Pz - 10) : RuinMix.Dressed.at(Random, X, Y, Pz - 10));
            }
        }
        Canvas.put(0, 6, Pz - 9, RuinMat.RuinReliefGod, RuinMat.South);
        Canvas.keep(0, 6, Pz - 9);
        Canvas.greatBrazier(-5, 4, Pz - 6, true, true);
        Canvas.greatBrazier(5, 4, Pz - 6, true, true);
        Canvas.spawner(-4, 4, Pz - 9, RuinDanger.spawnerKind(Random, Tier));
        Canvas.spawner(4, 4, Pz - 9, RuinDanger.Blaze);
        Canvas.chest(-2, 4, Pz - 9, RuinMat.South, RuinCanvas.LootVault);
        Canvas.chest(2, 4, Pz - 9, RuinMat.South, RuinCanvas.LootVault);
        for (int Side : new int[]{-1, 1}) {
            Canvas.column(Side * 5, 1, Length + 2, 9, true);
            Canvas.put(Side * 5, 0, Length + 2, RuinMix.Plinth);
        }
        RuinDanger.collapse(Canvas, -2, -8, 2, -6, 0, 5, RuinDanger.PitMagma, RuinMix.Paving);
        RuinDanger.collapse(Canvas, -2, 12, 2, 14, 0, 4, RuinDanger.PitMagma, RuinMix.Paving);
        Canvas.chest(-10, 1, 0, RuinMat.East, RuinCanvas.LootCache);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -12, 1, -Length, 12, Length);
        Spots.addAll(RuinDanger.floorSpots(Canvas, -7, 4, Pz - 10, 7, Pz));
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier) + 2);
        Canvas.brush(11, 0, 20, true);
        Canvas.brush(-11, 0, -18, true);
        Canvas.entrance(0, 1, Length + 4, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }
}
