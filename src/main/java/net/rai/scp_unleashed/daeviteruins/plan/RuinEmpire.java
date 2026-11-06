package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayList;
import java.util.List;

public final class RuinEmpire {
    public static final int HallKnights = 0;
    public static final int HallScriptorium = 1;
    public static final int HallGreat = 2;
    public static final int HallThrone = 3;

    private RuinEmpire() {
    }

    public static void kingsGate(RuinCanvas Canvas) {
        gate(Canvas, 1);
    }

    public static void gate(RuinCanvas Canvas, int Scale) {
        RuinRandom Random = Canvas.random();
        boolean Grand = Scale > 1;
        int Passage = Grand ? 7 : 3;
        int Half = Passage / 2;
        int PassageHigh = Grand ? 13 : 6;
        int Tower = Grand ? 9 : 5;
        int Depth = Grand ? 5 : 3;
        int WallHigh = Grand ? 18 : 9;
        int TowerHigh = WallHigh + (Grand ? 8 : 4);
        int Span = Grand ? 26 : 17;
        int WallHalf = Grand ? 2 : 1;
        int Inner = Half + 1;
        int Outer = Half + Tower;
        Canvas.scorchGround(0, 0, Span + 3, 0.4, 131);
        Canvas.box(-Span, 0, -Depth - 1, Span, 0, Depth + 1, RuinMix.Plinth);
        for (int Side : new int[]{-1, 1}) {
            RuinKit.wall(Canvas, Side * (Outer + 1), -WallHalf, Side * Span, WallHalf, 1, WallHigh, RuinMix.Dressed, RuinMix.Plinth);
            RuinKit.merlons(Canvas, Side * (Outer + 1), WallHalf, Side * Span, WallHalf, WallHigh + 1, RuinMix.Trim);
            RuinKit.merlons(Canvas, Side * (Outer + 1), -WallHalf, Side * Span, -WallHalf, WallHigh + 1, RuinMix.Trim);
            int X1 = Side * Inner;
            int X2 = Side * Outer;
            RuinKit.ring(Canvas, X1, -Depth, X2, Depth, 1, TowerHigh, RuinMix.Dressed, RuinMix.Plinth);
            Canvas.clear(Math.min(X1, X2) + 1, 1, -Depth + 1, Math.max(X1, X2) - 1, TowerHigh, Depth - 1);
            RuinKit.floor(Canvas, Math.min(X1, X2) + 1, -Depth + 1, Math.max(X1, X2) - 1, Depth - 1, WallHigh, RuinMix.Floor);
            RuinKit.merlons(Canvas, Math.min(X1, X2), Depth, Math.max(X1, X2), Depth, TowerHigh + 1, RuinMix.Trim);
            RuinKit.merlons(Canvas, Math.min(X1, X2), -Depth, Math.max(X1, X2), -Depth, TowerHigh + 1, RuinMix.Trim);
            RuinKit.merlons(Canvas, X2, -Depth + 1, X2, Depth - 1, TowerHigh + 1, RuinMix.Trim);
            RuinKit.band(Canvas, Math.min(X1, X2), -Depth, Math.max(X1, X2), Depth, WallHigh - 1, RuinMix.Brass);
            RuinKit.opening(Canvas, X1, 1, 0, false, 1, 3, false);
            int Ladder = X2 - Side;
            for (int Y = 1; Y < WallHigh; Y++) {
                Canvas.put(Ladder, Y, 0, RuinMat.Ladder, Side < 0 ? RuinMat.East : RuinMat.West);
            }
            Canvas.air(Ladder, WallHigh, 0);
            for (int Level = 4; Level < TowerHigh - 1; Level += 4) {
                Canvas.air(X2, Level, 0);
                Canvas.air(X2, Level + 1, 0);
            }
            RuinKit.wallBanner(Canvas, (X1 + X2) / 2, PassageHigh - 1, Depth + 1, RuinMat.South);
            Canvas.pedestalBrazier((X1 + X2) / 2, TowerHigh - (Grand ? 4 : 2), 0, false);
        }
        Canvas.box(-Half, PassageHigh, -Depth, Half, WallHigh, Depth, RuinMix.Dressed);
        Canvas.clear(-Half, 1, -Depth, Half, PassageHigh - 1, Depth);
        Canvas.keepClearBox(-Half, 1, -Depth - 1, Half, Math.min(3, PassageHigh - 1), Depth + 1);
        RuinKit.floor(Canvas, -Half, -Depth, Half, Depth, 0, RuinMix.Paving);
        for (int Z : new int[]{-Depth, Depth}) {
            Canvas.stairs(-Half, PassageHigh - 1, Z, RuinMat.RedSandstoneStairs, RuinMat.East, true);
            Canvas.stairs(Half, PassageHigh - 1, Z, RuinMat.RedSandstoneStairs, RuinMat.West, true);
        }
        RuinKit.merlons(Canvas, -Half, Depth, Half, Depth, WallHigh + 1, RuinMix.Trim);
        for (int X = -Half; X <= Half; X++) {
            for (int Y = PassageHigh - 1; Y >= PassageHigh - (Grand ? 6 : 3); Y--) {
                if (Random.at(X, Y, 0, 132) < 0.85) {
                    Canvas.put(X, Y, 0, RuinMat.IronBars);
                }
            }
        }
        Canvas.put(0, PassageHigh + 1, Depth, RuinMat.RuinReliefGod, RuinMat.South);
        Canvas.keep(0, PassageHigh + 1, Depth);
        double Scale2 = Grand ? 3.0 : 7.5;
        RuinFigures.Figure King = Grand ? RuinFigures.god(false) : RuinFigures.king(true);
        int Count = 0;
        int Gap = Math.max(3, (Span - Outer - 4) / 2);
        for (int Side : new int[]{-1, 1}) {
            for (int Index = 0; Index < 3; Index++) {
                int X = Side * Math.min(Span - 1, Outer + 3 + Index * Gap);
                RuinKit.statue(Canvas, X, 1, WallHalf + (Grand ? 4 : 3), RuinMat.South, Scale2, King, true);
                Count++;
            }
        }
        if (Count == 6) {
            RuinKit.statue(Canvas, 0, WallHigh + 1, 0, RuinMat.South, Scale2 * 1.2, King, false);
        }
        int Tier = Canvas.tier();
        RuinDanger.dartTrap(Canvas, 0, 1, Depth - 1, true, Half + 2, RuinDanger.dartAmmo(Random, Tier), false);
        RuinDanger.dartTrap(Canvas, 0, 1, -Depth + 1, true, Half + 2, RuinDanger.dartAmmo(Random, Tier), true);
        if (Grand) {
            RuinDanger.dartTrap(Canvas, -1, 1, 2, true, Half + 3, RuinDanger.FireCharges, true);
            RuinDanger.ventFloor(Canvas, 0, 0, -Depth - 2, 3.0, 0.45, 133);
            Canvas.spawner(-(Inner + Outer) / 2, 1, -2, RuinDanger.spawnerKind(Random, Tier));
            Canvas.spawner((Inner + Outer) / 2, 1, 2, RuinDanger.spawnerKind(Random, Tier));
        }
        Canvas.chest(-(Inner + Outer) / 2, 1, Depth - 1, RuinMat.North, RuinCanvas.LootCache);
        Canvas.chest((Inner + Outer) / 2, WallHigh + 1, -Depth + 1, RuinMat.South, RuinCanvas.LootVault);
        List<int[]> Spots = new ArrayList<>();
        Spots.addAll(RuinDanger.floorSpots(Canvas, -Span, WallHigh + 1, -WallHalf, Span, WallHalf));
        Spots.addAll(RuinDanger.floorSpots(Canvas, -Outer, 1, -Depth - 6, Outer, -Depth - 1));
        Spots.addAll(RuinDanger.floorSpots(Canvas, -Outer, WallHigh + 1, -Depth, Outer, Depth));
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.brush(Span - 3, 0, Depth + 4, true);
        Canvas.entrance(0, 1, Depth + 3, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void treasury(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        Canvas.scorchGround(0, 0, 8.0, 0.35, 141);
        Canvas.box(-5, 0, -4, 5, 0, 6, RuinMix.Plinth);
        for (int Index = 0; Index < 8; Index++) {
            double Angle = Index * Math.PI / 4;
            int X = (int) Math.round(Math.cos(Angle) * 4.5);
            int Z = 1 + (int) Math.round(Math.sin(Angle) * 4.5);
            if (Math.abs(X) <= 2 && Z >= 2) {
                continue;
            }
            Canvas.column(X, 1, Z, 2 + Random.nextInt(4), true);
        }
        Canvas.column(-2, 1, 4, 5, true);
        Canvas.column(2, 1, 4, 5, true);
        Canvas.box(-2, 6, 4, 2, 6, 4, RuinMix.Trim);
        Canvas.put(0, 6, 5, RuinMat.RuinReliefHost, RuinMat.South);
        int Bottom = -9;
        for (int Step = 0; Step <= 9; Step++) {
            int Z = 3 - Step;
            int Y = -Step;
            for (int X = -1; X <= 1; X++) {
                for (int Clear = Y + 1; Clear <= Math.min(0, Y + 3); Clear++) {
                    Canvas.air(X, Clear, Z);
                }
                if (Step < 9) {
                    Canvas.stairs(X, Y, Z, RuinMat.BlackstoneBrickStairs, RuinMat.North, false);
                } else {
                    Canvas.put(X, Y - 1, Z, RuinMix.DarkBrick);
                    Canvas.air(X, Y, Z);
                }
                Canvas.put(X, Y - 1, Z, RuinMix.DarkBrick);
                if (Y + 4 <= 0) {
                    Canvas.put(X, Y + 4, Z, RuinMix.DarkBrick);
                }
            }
            for (int Side : new int[]{-2, 2}) {
                for (int Level = Y - 1; Level <= Math.min(0, Y + 4); Level++) {
                    Canvas.put(Side, Level, Z, RuinMix.DarkBrick);
                }
            }
        }
        int Floor = Bottom - 1;
        Canvas.box(-4, Floor - 1, -14, 4, Bottom + 4, -6, RuinMix.DarkBrick);
        Canvas.box(-3, Floor, -13, 3, Floor, -7, RuinMix.Floor);
        Canvas.clear(-3, Bottom, -13, 3, Bottom + 3, -7);
        Canvas.clear(-1, Bottom, -6, 1, Bottom + 2, -6);
        RuinDanger.collapse(Canvas, -1, -11, 1, -9, Floor, 4, RuinDanger.PitMagma, RuinMix.DarkBrick);
        Canvas.put(-3, Bottom, -10, RuinMat.RuinUrn, RuinMat.East);
        Canvas.put(3, Bottom, -8, RuinMat.RuinUrn, RuinMat.West);
        int Vz1 = -26;
        int Vz2 = -15;
        Canvas.box(-7, Floor - 1, Vz1 - 1, 7, Bottom + 5, Vz2 + 1, RuinMix.Dressed);
        Canvas.box(-6, Floor, Vz1, 6, Floor, Vz2, RuinMix.Floor);
        RuinKit.mosaic(Canvas, -6, Vz1, 6, Vz2, Floor);
        Canvas.clear(-6, Bottom, Vz1, 6, Bottom + 4, Vz2);
        Canvas.clear(-1, Bottom, -14, 1, Bottom + 2, -14);
        RuinKit.band(Canvas, -7, Vz1 - 1, 7, Vz2 + 1, Bottom + 3, RuinMix.Brass);
        for (int Z = Vz1 + 1; Z <= Vz2 - 1; Z += 3) {
            Canvas.put(-7, Bottom + 1, Z, Z % 2 == 0 ? RuinMat.RuinReliefHost : RuinMat.RuinReliefGod, RuinMat.East);
            Canvas.put(7, Bottom + 1, Z, Z % 2 == 0 ? RuinMat.RuinReliefGod : RuinMat.RuinReliefHost, RuinMat.West);
        }
        for (int X = -2; X <= 2; X++) {
            if (X != 0) {
                Canvas.put(X, Bottom, Vz1 + 4, RuinMat.IronBars);
                Canvas.put(X, Bottom + 1, Vz1 + 4, RuinMat.IronBars);
            }
            Canvas.put(X, Bottom + 2, Vz1 + 4, RuinMat.IronBars);
        }
        for (int Z = Vz1; Z <= Vz1 + 3; Z++) {
            Canvas.put(-3, Bottom, Z, RuinMix.Dressed);
            Canvas.put(-3, Bottom + 1, Z, RuinMix.Dressed);
            Canvas.put(-3, Bottom + 2, Z, RuinMix.Dressed);
            Canvas.put(3, Bottom, Z, RuinMix.Dressed);
            Canvas.put(3, Bottom + 1, Z, RuinMix.Dressed);
            Canvas.put(3, Bottom + 2, Z, RuinMix.Dressed);
        }
        Canvas.chest(0, Bottom, Vz1, RuinMat.South, RuinCanvas.LootVault);
        Canvas.chest(-5, Bottom, Vz2, RuinMat.North, RuinCanvas.LootCache);
        Canvas.chest(5, Bottom, Vz1 + 6, RuinMat.West, RuinCanvas.LootCache);
        for (int[] Pile : new int[][]{{-5, Vz1 + 1}, {5, Vz1 + 1}, {-2, Vz1 + 1}, {2, Vz1 + 1}}) {
            Canvas.put(Pile[0], Bottom, Pile[1], RuinMix.Gold);
            if (Random.chance(0.6)) {
                Canvas.put(Pile[0], Bottom + 1, Pile[1], RuinMat.RawGoldBlock);
            }
        }
        for (int Z = Vz2 - 2; Z >= Vz1 + 6; Z -= 3) {
            Canvas.put(-6, Bottom, Z, RuinMat.RuinUrn, RuinMat.East);
            Canvas.put(6, Bottom, Z, RuinMat.RuinUrn, RuinMat.West);
        }
        Canvas.hangingLantern(0, Bottom + 4, (Vz1 + Vz2) / 2, 1);
        Canvas.hangingLantern(0, Bottom + 3, -10, 0);
        Canvas.put(-6, Bottom + 4, Vz2, RuinMat.Cobweb);
        Canvas.put(6, Bottom + 4, Vz1, RuinMat.Cobweb);
        Canvas.put(-3, Bottom + 3, -7, RuinMat.Cobweb);
        RuinDanger.dartTrap(Canvas, 0, Bottom, -7, true, 5, RuinDanger.dartAmmo(Random, Tier), false);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -5, Bottom, Vz1 + 5, 5, Vz2 - 1);
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.brush(4, 0, -3, false);
        Canvas.entrance(0, 1, 7, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void whisperTower(RuinCanvas Canvas) {
        roundTower(Canvas, 4, 20, false);
    }

    public static void omenObservatory(RuinCanvas Canvas) {
        roundTower(Canvas, 6, 14, true);
    }

    public static void roundTower(RuinCanvas Canvas, int Radius, int Height, boolean Observatory) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        Canvas.scorchGround(0, 0, Radius + 7, 0.4, 151);
        Canvas.disc(0, 0, 0, Radius + 2.2, RuinMix.Plinth);
        for (int Y = 1; Y <= Height; Y++) {
            Canvas.ring(0, Y, 0, Radius - 1, Radius, Y == 1 ? RuinMix.Plinth : (Y % 4 == 0 ? RuinMix.Trim : RuinMix.Dressed));
        }
        Canvas.ring(0, 1, 0, Radius, Radius + 1.1, RuinMix.Plinth);
        for (int Y = 1; Y <= Height; Y++) {
            Canvas.discAir(0, Y, 0, Radius - 1);
        }
        int Ladder = Radius - 1;
        List<Integer> Floors = new ArrayList<>();
        for (int Level = 5; Level < Height; Level += 5) {
            Floors.add(Level);
        }
        Floors.add(Height);
        for (int Level : Floors) {
            Canvas.disc(0, Level, 0, Radius - 1, Level == Height && Observatory ? RuinMix.Plinth : RuinMix.of(RuinMat.DarkOakPlanks, 70, RuinMat.CrimsonPlanks, 30));
            Canvas.air(Ladder, Level, 0);
            for (int X = -1; X <= 1; X++) {
                Canvas.air(X, Level + 2, Radius);
                Canvas.air(X, Level + 2, -Radius);
            }
            Canvas.air(Radius, Level + 2, 0);
            Canvas.air(-Radius, Level + 2, 0);
        }
        for (int Y = 1; Y < Height; Y++) {
            Canvas.put(Ladder, Y, 0, RuinMat.Ladder, RuinMat.West);
        }
        RuinKit.opening(Canvas, 0, 1, Radius, true, 1, 3, false);
        RuinKit.opening(Canvas, 0, 1, Radius + 1, true, 1, 3, false);
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                if (Canvas.onRing(Dx, Dz, Radius - 1, Radius) && ((Dx + Dz) & 1) == 0) {
                    Canvas.put(Dx, Height + 1, Dz, RuinMix.Trim);
                }
            }
        }
        RuinKit.wallBanner(Canvas, -2, 6, Radius + 1, RuinMat.South);
        RuinKit.wallBanner(Canvas, 2, 6, Radius + 1, RuinMat.South);
        Canvas.chest(-Radius + 2, 1, -1, RuinMat.East, RuinCanvas.LootCache);
        Canvas.put(-Radius + 2, 1, 1, RuinMat.Barrel, RuinMat.Up, RuinCanvas.LootNone);
        int First = Floors.get(0);
        Canvas.put(-1, First + 1, -Radius + 2, RuinMat.Lectern, RuinMat.South);
        Canvas.put(1, First + 1, -Radius + 2, RuinMat.ChiseledBookshelf, RuinMat.South);
        Canvas.put(0, First + 1, -Radius + 1, RuinMat.Bookshelf);
        Canvas.put(-Radius + 1, First + 4, 0, RuinMat.Cobweb);
        if (Floors.size() > 2) {
            int Second = Floors.get(1);
            Canvas.put(0, Second + 1, -Radius + 2, RuinMat.RuinBrazier, RuinMat.South, 0);
            Canvas.chest(-Radius + 2, Second + 1, 0, RuinMat.East, RuinCanvas.LootVault);
        }
        if (Observatory) {
            int Top = Height + 1;
            RuinKit.mosaic(Canvas, -Radius + 1, -Radius + 1, Radius - 1, Radius - 1, Height);
            for (int Dx = -Radius + 1; Dx <= Radius - 1; Dx++) {
                for (int Dz = -Radius + 1; Dz <= Radius - 1; Dz++) {
                    if (Dx * Dx + Dz * Dz <= (Radius - 1) * (Radius - 1) && Random.at(Dx, Height, Dz, 152) < 0.12) {
                        Canvas.put(Dx, Height, Dz, RuinMat.GildedBlackstone);
                    }
                }
            }
            Canvas.air(Ladder, Height, 0);
            Canvas.pillar(0, Top, Top + 2, 0, RuinMat.PolishedBasalt);
            int Ring = 4;
            int Centre = Top + 3 + Ring;
            for (int Step = 0; Step < 64; Step++) {
                double Angle = Step * Math.PI * 2 / 64;
                int A = (int) Math.round(Math.cos(Angle) * Ring);
                int B = (int) Math.round(Math.sin(Angle) * Ring);
                Canvas.put(A, Centre + B, 0, RuinMix.Brass);
                Canvas.put(0, Centre + B, A, RuinMix.Gold);
                if (Step % 2 == 0) {
                    Canvas.put(A, Centre, B, RuinMix.Brass);
                }
            }
            Canvas.pillar(0, Top + 3, Centre - Ring - 1, 0, RuinMat.Chain);
            Canvas.put(0, Centre, 0, RuinMat.GoldBlock);
            Canvas.pillar(0, Centre - Ring + 1, Centre - 1, 0, RuinMat.Chain);
            int Ax = Radius + 5;
            Canvas.box(Ax - 3, 0, -4, Ax + 3, 0, 4, RuinMix.Plinth);
            RuinKit.ring(Canvas, Ax - 3, -4, Ax + 3, 4, 1, 5, RuinMix.Dressed, RuinMix.Plinth);
            Canvas.clear(Ax - 2, 1, -3, Ax + 2, 5, 3);
            RuinKit.roof(Canvas, Ax - 3, -4, Ax + 3, 4, 6, RuinMat.BlackstoneBrickSlab, 0.6, 153);
            RuinKit.opening(Canvas, Ax - 3, 1, 0, false, 1, 3, false);
            Canvas.spawner(Ax, 1, 0, RuinDanger.spawnerKind(Random, Tier));
            Canvas.chest(Ax + 2, 1, -3, RuinMat.West, RuinCanvas.LootVault);
            RuinDanger.dartTrap(Canvas, Ax - 2, 1, 0, false, 4, RuinDanger.dartAmmo(Random, Tier), false);
        } else {
            RuinKit.statue(Canvas, 0, Height + 1, -1, RuinMat.South, 8.0, RuinFigures.overseer(30), false);
            for (int[] Corner : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                Canvas.brazier(Corner[0], Height + 1, Corner[1], false);
            }
        }
        RuinDanger.dartTrap(Canvas, 0, 1, Radius - 2, true, Radius + 1, RuinDanger.dartAmmo(Random, Tier), false);
        List<int[]> Spots = new ArrayList<>();
        for (int Level : Floors) {
            Spots.addAll(RuinDanger.floorSpots(Canvas, -Radius + 1, Level + 1, -Radius + 1, Radius - 1, Radius - 1));
        }
        Spots.addAll(RuinDanger.floorSpots(Canvas, -Radius - 4, 1, -Radius - 4, Radius + 4, Radius + 4));
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.brush(-Radius - 3, 0, 3, true);
        Canvas.entrance(0, 1, Radius + 3, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void colossus(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        double Scale = 1.7;
        Canvas.scorchGround(0, 8, 16.0, 0.5, 161);
        Canvas.box(-6, 0, -6, 6, 1, 6, RuinMix.Plinth);
        Canvas.box(-5, 2, -5, 5, 3, 5, RuinMix.Dressed);
        RuinKit.band(Canvas, -5, -5, 5, 5, 3, RuinMix.Trim);
        for (int Side = -4; Side <= 4; Side += 4) {
            Canvas.put(Side, 2, 5, RuinMat.RuinReliefHost, RuinMat.South);
            Canvas.put(5, 2, Side, RuinMat.RuinReliefGod, RuinMat.East);
            Canvas.put(-5, 2, Side, RuinMat.RuinReliefHost, RuinMat.West);
        }
        for (int X = -6; X <= 6; X++) {
            Canvas.stairs(X, 2, 6, RuinMat.PolishedBlackstoneStairs, RuinMat.North, false);
        }
        RuinFigures.draw(Canvas, RuinFigures.legs(), new RuinFigures.Pose(0.5, 4, 0.5, 0, 0, 0, Scale), 4, 9);
        double Lift = 1 + 2.5 / Scale;
        RuinFigures.draw(Canvas, RuinFigures.upper(true, false), new RuinFigures.Pose(0.5, Lift, 0.5, 180, -90, 0, Scale), 1);
        RuinFigures.draw(Canvas, RuinFigures.headAlone(), new RuinFigures.Pose(6.5, 1 + 4.0 / Scale, 17.5, 25, 0, 90, Scale), 1);
        for (int Z : new int[]{11, 15}) {
            for (int X = -7; X <= 7; X++) {
                for (int Y = 1; Y <= 8; Y++) {
                    if (Random.at(X, Y, Z, 162) < 0.8) {
                        Canvas.air(X, Y, Z);
                    }
                }
            }
        }
        for (int X = -7; X <= 7; X++) {
            for (int Z = 6; Z <= 26; Z++) {
                int Low = 0;
                for (int Y = 1; Y <= 8; Y++) {
                    if (Canvas.filled(X, Y, Z) && Canvas.at(X, Y, Z).isOpaqueCube()) {
                        Low = Y;
                        break;
                    }
                }
                for (int Y = 1; Y < Low; Y++) {
                    Canvas.put(X, Y, Z, Y == Low - 1 ? RuinMix.Rubble.at(Random, X, Y, Z) : RuinMat.Gravel);
                }
                if (Low > 0) {
                    Canvas.put(X, 0, Z, RuinMix.Ash);
                }
            }
        }
        Canvas.rubble(-6, 12, 2.2, 2, 163);
        Canvas.rubble(5, 18, 1.8, 2, 164);
        Canvas.clear(-2, 1, -2, 2, 2, 2);
        Canvas.box(-2, 0, -2, 2, 0, 2, RuinMix.Floor);
        RuinKit.opening(Canvas, 0, 1, -5, true, 1, 2, false);
        RuinKit.opening(Canvas, 0, 1, -6, true, 1, 2, false);
        Canvas.chest(0, 1, 2, RuinMat.North, RuinCanvas.LootVault);
        Canvas.put(-2, 1, 2, RuinMat.RuinUrn, RuinMat.East);
        Canvas.put(2, 3, 2, RuinMat.Cobweb);
        RuinDanger.dartTrap(Canvas, 0, 1, -1, true, 3, RuinDanger.dartAmmo(Random, Tier), false);
        Canvas.chest(-8, 1, 6, RuinMat.East, RuinCanvas.LootCache);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -9, 1, -9, 9, 24);
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.brush(7, 0, 4, true);
        Canvas.brush(-8, 0, 16, true);
        Canvas.entrance(0, 1, -8, RuinMat.North);
        Canvas.centre(0, 1, 6);
    }

    public static void siege(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        Canvas.scorchGround(0, 0, 16.0, 0.6, 171);
        RuinKit.wall(Canvas, -15, -16, 15, -14, 1, 9, RuinMix.WhiteCity, RuinMix.WhiteCity);
        RuinKit.merlons(Canvas, -15, -14, 15, -14, 10, RuinMix.WhiteCity);
        Canvas.clear(-3, 1, -16, 3, 10, -14);
        Canvas.rubble(0, -13, 4.0, 4, 172);
        for (int X = -4; X <= 4; X++) {
            for (int Z = -16; Z <= -10; Z++) {
                for (int Y = 1; Y <= 4; Y++) {
                    if (Canvas.filled(X, Y, Z) && Random.at(X, Y, Z, 173) < 0.6) {
                        Canvas.put(X, Y, Z, RuinMix.WhiteCity);
                    }
                }
            }
        }
        RuinKit.ring(Canvas, 9, -18, 13, -14, 1, 12, RuinMix.WhiteCity, RuinMix.WhiteCity);
        Canvas.clear(10, 1, -17, 12, 12, -15);
        RuinKit.opening(Canvas, 11, 1, -14, true, 1, 3, false);
        Canvas.chest(11, 1, -17, RuinMat.South, RuinCanvas.LootVault);
        RuinDanger.dartTrap(Canvas, 11, 1, -16, true, 3, RuinDanger.dartAmmo(Random, Tier), false);
        for (int[] Crater : new int[][]{{-8, -10}, {6, -9}, {-2, -6}}) {
            Canvas.crater(Crater[0], Crater[1], 2.2, 174);
        }
        Canvas.brush(-8, -1, -10, true);
        RuinKit.hill(Canvas, -14, 5, 14, 8, 3, true, true, true, true, 175);
        for (int X = -12; X <= 12; X++) {
            if (Random.at(X, 0, 5, 176) < 0.7) {
                int Top = Canvas.plan().topAt(X, 6);
                if (Top >= 1) {
                    Canvas.put(X, Top + 1, 6, RuinMat.StrippedDarkOakLog, RuinMat.Up);
                    Canvas.put(X, Top + 2, 6, RuinMat.DarkOakFence);
                }
            }
        }
        for (int Side : new int[]{-1, 1}) {
            int X = Side * 8;
            int Z = -1;
            for (int Dz = -3; Dz <= 3; Dz++) {
                Canvas.put(X - 2, 1, Z + Dz, RuinMat.StrippedDarkOakLog, RuinMat.South);
                Canvas.put(X + 2, 1, Z + Dz, RuinMat.StrippedDarkOakLog, RuinMat.South);
                Canvas.put(X - 2, 0, Z + Dz, RuinMix.Ash);
                Canvas.put(X + 2, 0, Z + Dz, RuinMix.Ash);
            }
            for (int Dx = -1; Dx <= 1; Dx++) {
                Canvas.put(X + Dx, 1, Z - 3, RuinMat.StrippedDarkOakLog, RuinMat.East);
                Canvas.put(X + Dx, 1, Z + 3, RuinMat.StrippedDarkOakLog, RuinMat.East);
            }
            for (int Y = 2; Y <= 6; Y++) {
                Canvas.put(X - 2, Y, Z, RuinMat.DarkOakLog, RuinMat.Up);
                Canvas.put(X + 2, Y, Z, RuinMat.DarkOakLog, RuinMat.Up);
            }
            for (int Dx = -2; Dx <= 2; Dx++) {
                Canvas.put(X + Dx, 7, Z, RuinMat.StrippedDarkOakLog, RuinMat.East);
            }
            int Arm = Side < 0 ? 6 : 4;
            for (int Step = 1; Step <= Arm; Step++) {
                Canvas.put(X, 7 + Step, Z - Step, RuinMat.DarkOakFence);
            }
            Canvas.pillar(X, 4, 6, Z + 2, RuinMat.Chain);
            Canvas.box(X - 1, 2, Z + 2, X + 1, 3, Z + 3, RuinMix.of(RuinMat.Blackstone, 60, RuinMat.CrackedBlackstoneBricks, 40));
            Canvas.put(X, 3, Z + 2, RuinMat.Blackstone);
            Canvas.rubble(X + Side * 4, Z + 4, 1.4, 2, 177);
        }
        for (int Z = -9; Z <= -3; Z++) {
            Canvas.put(0, 2, Z, RuinMat.StrippedDarkOakLog, RuinMat.South);
            Canvas.put(-2, 0, Z, RuinMix.Ash);
            Canvas.put(2, 0, Z, RuinMix.Ash);
            if (Z % 3 == 0) {
                Canvas.pillar(-2, 1, 3, Z, RuinMat.DarkOakFence);
                Canvas.pillar(2, 1, 3, Z, RuinMat.DarkOakFence);
            }
            for (int X = -2; X <= 2; X++) {
                if (Random.at(X, 4, Z, 178) < 0.65) {
                    Canvas.slab(X, 4, Z, RuinMat.DarkOakSlab, false);
                }
            }
            Canvas.put(-1, 1, Z, RuinMat.DarkOakFence);
            Canvas.put(1, 1, Z, RuinMat.DarkOakFence);
        }
        Canvas.put(0, 2, -10, RuinMat.AgedCopper);
        Canvas.put(0, 1, -10, RuinMat.DarkOakFence);
        Canvas.pillar(0, 3, 3, -6, RuinMat.Chain);
        RuinKit.tent(Canvas, 0, 12, 5, true, false, 179);
        Canvas.chest(0, 1, 12, RuinMat.South, RuinCanvas.LootCache);
        RuinDanger.collapse(Canvas, 4, -1, 6, 2, 0, 4, RuinDanger.PitMagma, RuinConquests.Tread);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -14, 1, -12, 14, 14);
        for (int X = -12; X <= 12; X += 3) {
            int Top = Canvas.plan().topAt(X, 7);
            if (Top >= 2) {
                Spots.add(new int[]{X, Top + 1, 7});
            }
        }
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.entrance(0, 1, 16, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void knightsHall(RuinCanvas Canvas) {
        hall(Canvas, 11, 21, 8, HallKnights);
    }

    public static void scriptorium(RuinCanvas Canvas) {
        hall(Canvas, 13, 17, 9, HallScriptorium);
    }

    public static void greatHall(RuinCanvas Canvas) {
        hall(Canvas, 17, 31, 12, HallGreat);
    }

    public static void throneRoom(RuinCanvas Canvas) {
        hall(Canvas, 15, 27, 11, HallThrone);
    }

    public static void hall(RuinCanvas Canvas, int Width, int Length, int Height, int Kind) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        int Hw = Width / 2;
        int Hl = Length / 2;
        Canvas.scorchGround(0, 0, Math.max(Hw, Hl) + 5, 0.4, 181);
        Canvas.box(-Hw - 2, 0, -Hl - 2, Hw + 2, 0, Hl + 3, RuinMix.Plinth);
        RuinKit.ring(Canvas, -Hw, -Hl, Hw, Hl, 1, Height, RuinMix.Dressed, RuinMix.Plinth);
        Canvas.clear(-Hw + 1, 1, -Hl + 1, Hw - 1, Height + 3, Hl - 1);
        if (Kind == HallThrone) {
            Canvas.box(-Hw + 1, 0, -Hl + 1, Hw - 1, 0, Hl - 1, RuinMix.of(RuinMat.SmoothBasalt, 40, RuinMat.Blackstone, 25, RuinMat.SootPowder, 15, RuinMat.Tuff, 20));
        } else {
            RuinKit.mosaic(Canvas, -Hw + 1, -Hl + 1, Hw - 1, Hl - 1, 0);
        }
        for (int Z = -Hl + 2; Z <= Hl - 2; Z += 4) {
            for (int Side : new int[]{-1, 1}) {
                int X = Side * (Hw + 1);
                Canvas.put(X, 0, Z, RuinMix.Plinth);
                for (int Y = 1; Y < Height - 1; Y++) {
                    Canvas.put(X, Y, Z, Y % 4 == 3 ? RuinMix.Brass.at(Random, X, Y, Z) : RuinMix.Trim.at(Random, X, Y, Z));
                }
                Canvas.stairs(X, Height - 1, Z, RuinMat.RedSandstoneStairs, Side < 0 ? RuinMat.East : RuinMat.West, false);
            }
        }
        for (int Z = -Hl + 4; Z <= Hl - 4; Z += 4) {
            for (int Side : new int[]{-1, 1}) {
                Canvas.air(Side * Hw, Height - 3, Z);
                Canvas.air(Side * Hw, Height - 2, Z);
            }
        }
        RuinKit.opening(Canvas, 0, 1, Hl, true, 3, Math.min(6, Height - 2), true);
        RuinKit.band(Canvas, -Hw, -Hl, Hw, Hl, Height, RuinMix.Brass);
        int Rows = Kind == HallGreat ? 2 : 1;
        List<Integer> ColumnX = new ArrayList<>();
        for (int Row = 0; Row < Rows; Row++) {
            int Offset = Row == 0 ? Hw - 2 : Hw - 5;
            ColumnX.add(-Offset);
            ColumnX.add(Offset);
        }
        for (int X : ColumnX) {
            for (int Z = -Hl + 3; Z <= Hl - 3; Z += 4) {
                Canvas.column(X, 1, Z, Height, true);
            }
        }
        RuinKit.beams(Canvas, -Hw, -Hl, Hw, Hl, Height + 1, true, 3, RuinMat.StrippedDarkOakLog);
        RuinKit.roof(Canvas, -Hw, -Hl, Hw, Hl, Height + 2, Kind == HallScriptorium ? RuinMat.DarkOakSlab : RuinMat.BlackstoneBrickSlab, 0.4, 182);
        int Dais = -Hl + 1;
        for (int X = -3; X <= 3; X++) {
            for (int Z = Dais; Z <= Dais + 2; Z++) {
                Canvas.put(X, 1, Z, RuinMix.Plinth);
            }
            Canvas.stairs(X, 1, Dais + 3, RuinMat.PolishedBlackstoneStairs, RuinMat.North, false);
        }
        for (int Z = -Hl + 3; Z <= Hl - 5; Z += 4) {
            RuinKit.wallBanner(Canvas, -Hw + 1, Height - 4, Z + 2, RuinMat.East);
            RuinKit.wallBanner(Canvas, Hw - 1, Height - 4, Z + 2, RuinMat.West);
        }
        List<int[]> Spots = new ArrayList<>(RuinDanger.floorSpots(Canvas, -Hw + 1, 1, -Hl + 4, Hw - 1, Hl - 2));
        switch (Kind) {
            case HallKnights -> {
                for (int Z = -Hl + 5; Z <= Hl - 5; Z += 4) {
                    for (int Side : new int[]{-1, 1}) {
                        RuinKit.statue(Canvas, Side * (Hw - 2), 1, Z, Side < 0 ? RuinMat.East : RuinMat.West, 8.0, RuinFigures.knight(true, 10), false);
                    }
                }
                for (int Z = -Hl + 6; Z <= Hl - 6; Z++) {
                    Canvas.slab(0, 2, Z, RuinMat.DarkOakSlab, true);
                    if (Z % 3 == 0) {
                        Canvas.put(0, 1, Z, RuinMat.DarkOakFence);
                    }
                    if (Random.chance(0.4)) {
                        Canvas.put(0, 3, Z, RuinMat.RedCarpet);
                    }
                    Canvas.stairs(-1, 1, Z, RuinMat.DarkOakStairs, RuinMat.East, false);
                    Canvas.stairs(1, 1, Z, RuinMat.DarkOakStairs, RuinMat.West, false);
                }
                throne(Canvas, 0, 2, Dais, false);
                Canvas.chest(-2, 2, Dais, RuinMat.South, RuinCanvas.LootVault);
                Canvas.chest(Hw - 1, 1, Hl - 2, RuinMat.West, RuinCanvas.LootCache);
                Canvas.put(-Hw + 1, 1, Hl - 2, RuinMat.RuinWeaponRack, RuinMat.East);
            }
            case HallScriptorium -> {
                for (int Z = -Hl + 1; Z <= Hl - 1; Z++) {
                    for (int Side : new int[]{-1, 1}) {
                        int X = Side * (Hw - 1);
                        if (Z % 4 == 0 && Math.abs(Z) < Hl - 1) {
                            continue;
                        }
                        for (int Y = 1; Y <= 3; Y++) {
                            double Pick = Random.at(X, Y, Z, 183);
                            Canvas.put(X, Y, Z, Pick < 0.4 ? RuinMat.Bookshelf : Pick < 0.7 ? RuinMat.ChiseledBookshelf : Pick < 0.88 ? RuinMat.CoalBlock : RuinMat.DarkOakPlanks,
                                    Side < 0 ? RuinMat.East : RuinMat.West);
                        }
                        Canvas.slab(X, 5, Z, RuinMat.DarkOakSlab, true);
                        Canvas.slab(X - Side, 5, Z, RuinMat.DarkOakSlab, true);
                    }
                }
                for (int Z = -Hl + 5; Z <= Hl - 4; Z += 3) {
                    for (int X : new int[]{-2, 2}) {
                        Canvas.put(X, 1, Z, RuinMat.Lectern, X < 0 ? RuinMat.East : RuinMat.West);
                    }
                    Canvas.put(0, 1, Z, RuinMat.RedCarpet);
                    Canvas.put(0, 1, Z + 1, RuinMat.RedCarpet);
                }
                for (int Y = 1; Y <= 4; Y++) {
                    Canvas.put(-Hw + 2, Y, Hl - 2, RuinMat.Ladder, RuinMat.East);
                }
                Canvas.put(-Hw + 2, 5, Hl - 2, RuinMat.Air);
                Canvas.chest(2, 2, Dais, RuinMat.South, RuinCanvas.LootVault);
                Canvas.put(0, 2, Dais, RuinMat.Lectern, RuinMat.South);
                Canvas.candles(-2, 2, Dais, 3);
                Canvas.chest(Hw - 2, 6, -Hl + 2, RuinMat.West, RuinCanvas.LootCache);
                RuinDanger.collapse(Canvas, -1, -2, 1, 0, 0, 3, RuinDanger.PitWebs, RuinMix.DarkBrick);
                Spots.addAll(RuinDanger.floorSpots(Canvas, -Hw + 1, 6, -Hl + 1, Hw - 1, Hl - 1));
            }
            case HallGreat -> {
                for (int X = -1; X <= 1; X++) {
                    for (int Z = -4; Z <= 4; Z++) {
                        Canvas.put(X, 1, Z, RuinMat.DarkOakPlanks);
                        Canvas.put(X, 2, Z, Random.chance(0.6) ? RuinMat.RedCarpet : RuinMat.BlackCarpet);
                    }
                }
                Canvas.put(0, 2, 0, RuinMat.GoldBlock);
                for (int Side : new int[]{-1, 1}) {
                    int Cx = Side * (Hw + 4);
                    Canvas.box(Cx - 3, 0, -3, Cx + 3, 0, 3, RuinMix.Plinth);
                    RuinKit.ring(Canvas, Cx - 3, -3, Cx + 3, 3, 1, 6, RuinMix.Dressed, RuinMix.Plinth);
                    Canvas.clear(Cx - 2, 1, -2, Cx + 2, 6, 2);
                    RuinKit.opening(Canvas, Side * Hw, 1, 0, false, 1, 3, false);
                    RuinKit.opening(Canvas, Side * (Hw + 1), 1, 0, false, 1, 3, false);
                    RuinKit.opening(Canvas, Cx - Side * 3, 1, 0, false, 1, 3, false);
                    RuinKit.roof(Canvas, Cx - 3, -3, Cx + 3, 3, 7, RuinMat.BlackstoneBrickSlab, 0.6, 184);
                    if (Side < 0) {
                        Canvas.spawner(Cx, 1, 0, RuinDanger.spawnerKind(Random, Tier));
                    } else {
                        Canvas.chest(Cx + 2, 1, 0, RuinMat.West, RuinCanvas.LootVault);
                        RuinDanger.dartTrap(Canvas, Cx - 1, 1, 0, false, 3, RuinDanger.dartAmmo(Random, Tier), false);
                    }
                }
                throne(Canvas, 0, 2, Dais, true);
                Canvas.chest(Hw - 1, 1, -Hl + 2, RuinMat.West, RuinCanvas.LootCache);
                RuinDanger.collapse(Canvas, -2, -Hl + 5, 2, -Hl + 7, 0, 4, RuinDanger.PitMagma, RuinMix.DarkBrick);
            }
            default -> {
                Canvas.box(-4, 1, Dais, 4, 3, Dais + 2, RuinMix.Plinth);
                for (int X = -3; X <= 3; X++) {
                    Canvas.box(X, 1, Dais + 3, X, 2, Dais + 3, RuinMix.Plinth);
                    Canvas.stairs(X, 3, Dais + 3, RuinMat.PolishedBlackstoneStairs, RuinMat.North, false);
                    Canvas.put(X, 1, Dais + 4, RuinMix.Plinth);
                    Canvas.stairs(X, 2, Dais + 4, RuinMat.PolishedBlackstoneStairs, RuinMat.North, false);
                    Canvas.stairs(X, 1, Dais + 5, RuinMat.PolishedBlackstoneStairs, RuinMat.North, false);
                }
                throne(Canvas, 0, 4, Dais, true);
                for (int Side : new int[]{-1, 1}) {
                    Canvas.greatBrazier(Side * 6, 1, Dais + 2, true, true);
                }
                Canvas.box(-6, 0, -Hl - 6, 6, 0, -Hl - 1, RuinMix.DarkBrick);
                RuinKit.ring(Canvas, -6, -Hl - 6, 6, -Hl, 1, 5, RuinMix.DarkBrick, RuinMix.Plinth);
                Canvas.clear(-5, 1, -Hl - 5, 5, 5, -Hl - 1);
                RuinKit.roof(Canvas, -6, -Hl - 6, 6, -Hl, 6, RuinMat.BlackstoneBrickSlab, 0.7, 185);
                RuinKit.opening(Canvas, -5, 1, -Hl, true, 1, 2, false);
                Canvas.spawner(0, 1, -Hl - 3, RuinDanger.spawnerKind(Random, Tier));
                Canvas.chest(-4, 1, -Hl - 5, RuinMat.South, RuinCanvas.LootVault);
                Canvas.chest(4, 1, -Hl - 5, RuinMat.South, RuinCanvas.LootCache);
                for (int Z = Dais + 8; Z <= Hl - 5; Z += 6) {
                    RuinDanger.dartTrap(Canvas, 0, 1, Z, true, Hw + 1, RuinDanger.dartAmmo(Random, Tier), Z % 12 == 0);
                }
                RuinDanger.collapse(Canvas, -2, Dais + 5, 2, Dais + 6, 0, 4, RuinDanger.PitMagma, RuinMix.DarkBrick);
            }
        }
        RuinDanger.dartTrap(Canvas, 0, 1, Hl - 1, true, Hw + 1, RuinDanger.dartAmmo(Random, Tier), false);
        Canvas.put(-Hw + 1, Height, -Hl + 1, RuinMat.Cobweb);
        Canvas.put(Hw - 1, Height - 1, Hl - 1, RuinMat.Cobweb);
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier) + (Kind == HallGreat ? 2 : 0));
        Canvas.brush(Hw + 3, 0, Hl + 1, false);
        Canvas.entrance(0, 1, Hl + 4, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void throne(RuinCanvas Canvas, int X, int Y, int Z, boolean Grand) {
        Canvas.put(X, Y, Z + 1, RuinMat.BlackstoneBrickStairs, RuinMat.North);
        Canvas.put(X, Y, Z, RuinMat.ChiseledBlackstone);
        Canvas.put(X, Y + 1, Z, RuinMat.GildedBlackstone);
        Canvas.put(X, Y + 2, Z, Grand ? RuinMat.GoldBlock : RuinMat.ChiseledBlackstone);
        Canvas.stairs(X - 1, Y, Z + 1, RuinMat.BlackstoneBrickStairs, RuinMat.East, false);
        Canvas.stairs(X + 1, Y, Z + 1, RuinMat.BlackstoneBrickStairs, RuinMat.West, false);
        Canvas.put(X - 1, Y, Z, RuinMat.PolishedBlackstone);
        Canvas.put(X + 1, Y, Z, RuinMat.PolishedBlackstone);
        if (Grand) {
            Canvas.put(X - 1, Y + 1, Z, RuinMat.GildedBlackstone);
            Canvas.put(X + 1, Y + 1, Z, RuinMat.GildedBlackstone);
            Canvas.put(X, Y + 3, Z, RuinMat.LightningRod, RuinMat.Up);
            Canvas.put(X - 1, Y + 2, Z, RuinMat.LightningRod, RuinMat.Up);
            Canvas.put(X + 1, Y + 2, Z, RuinMat.LightningRod, RuinMat.Up);
        }
        Canvas.keep(X, Y, Z);
        Canvas.keep(X, Y + 1, Z);
        Canvas.keep(X, Y + 2, Z);
    }

    public static void braziers(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Tier = Canvas.tier();
        int[] Halves = {11, 8, 5};
        int[] Tops = {2, 4, 6};
        Canvas.scorchGround(0, 0, 15.0, 0.45, 201);
        for (int Level = 0; Level < 3; Level++) {
            int Half = Halves[Level];
            RuinKit.platform(Canvas, -Half, -Half, Half, Half, Tops[Level], RuinMix.Rough, RuinMix.Dressed);
            RuinKit.band(Canvas, -Half, -Half, Half, Half, Tops[Level], RuinMix.Trim);
            RuinKit.mosaic(Canvas, -Half + 1, -Half + 1, Half - 1, Half - 1, Tops[Level]);
            int Below = Level == 0 ? 0 : Tops[Level - 1];
            int Rise = Tops[Level] - Below;
            for (int Step = 0; Step < Rise; Step++) {
                for (int X = -2; X <= 2; X++) {
                    int Z = Half + Rise - Step;
                    Canvas.stairs(X, Below + 1 + Step, Z, RuinMat.SmoothRedSandstoneStairs, RuinMat.North, false);
                    Canvas.protect(X, Below + 1 + Step, Z);
                    for (int Fill = Below + Step; Fill >= 0 && Fill > Below - 1; Fill--) {
                        if (!Canvas.filled(X, Fill, Z)) {
                            Canvas.put(X, Fill, Z, RuinMix.Rough);
                        }
                    }
                }
            }
            for (int X = -Half; X <= Half; X++) {
                for (int Z : new int[]{-Half, Half}) {
                    if (Z == Half && Math.abs(X) <= 2) {
                        continue;
                    }
                    if (Random.at(X, Tops[Level] + 1, Z, 202) < 0.8) {
                        Canvas.put(X, Tops[Level] + 1, Z, RuinMat.RedSandstoneWall);
                    }
                }
            }
            for (int Z = -Half + 1; Z < Half; Z++) {
                for (int X : new int[]{-Half, Half}) {
                    if (Random.at(X, Tops[Level] + 1, Z, 203) < 0.8) {
                        Canvas.put(X, Tops[Level] + 1, Z, RuinMat.RedSandstoneWall);
                    }
                }
            }
            Canvas.put(-3, Tops[Level] + 1, Half, RuinMat.BlackstoneWall);
            Canvas.put(3, Tops[Level] + 1, Half, RuinMat.BlackstoneWall);
            int Corner = Half - 2;
            if (Level < 2) {
                for (int[] Spot : new int[][]{{-Corner, -Corner}, {Corner, -Corner}, {-Corner, Corner}, {Corner, Corner}}) {
                    Canvas.greatBrazier(Spot[0], Tops[Level] + 1, Spot[1], true, Level == 1);
                    RuinDanger.ventFloor(Canvas, Spot[0], Tops[Level], Spot[1], 2.6, 0.3, 204 + Level);
                }
            }
        }
        int Top = Tops[2];
        Canvas.box(-1, Top + 1, -1, 1, Top + 1, 1, RuinMix.Plinth);
        Canvas.greatBrazier(0, Top + 2, 0, true, true);
        for (int X = -3; X <= 3; X++) {
            for (int Y = Top + 1; Y <= Top + 4; Y++) {
                Canvas.put(X, Y, -4, Y == Top + 4 ? RuinMix.Trim.at(Random, X, Y, -4) : RuinMix.Dressed.at(Random, X, Y, -4));
            }
        }
        Canvas.put(0, Top + 3, -3, RuinMat.RuinReliefGod, RuinMat.South);
        Canvas.put(0, Top + 3, -4, RuinMix.Dressed);
        Canvas.put(-2, Top + 3, -3, RuinMat.RuinReliefHost, RuinMat.South);
        Canvas.put(2, Top + 3, -3, RuinMat.RuinReliefHost, RuinMat.South);
        Canvas.keep(0, Top + 3, -3);
        Canvas.chest(-3, Top + 1, -3, RuinMat.South, RuinCanvas.LootVault);
        Canvas.chest(-6, 3, -10, RuinMat.South, RuinCanvas.LootCache);
        RuinDanger.dartTrap(Canvas, 0, Top + 1, 5, true, 4, RuinDanger.dartAmmo(Random, Tier), false);
        List<int[]> Spots = new ArrayList<>();
        for (int Level = 0; Level < 3; Level++) {
            Spots.addAll(RuinDanger.floorSpots(Canvas, -Halves[Level] + 1, Tops[Level] + 1, -Halves[Level] + 1, Halves[Level] - 1, Halves[Level] - 1));
        }
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Tier));
        Canvas.brush(-13, 0, 6, true);
        Canvas.entrance(0, 1, 15, RuinMat.South);
        Canvas.centre(0, Top + 1, 0);
    }
}
