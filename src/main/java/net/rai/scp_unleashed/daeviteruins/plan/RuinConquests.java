package net.rai.scp_unleashed.daeviteruins.plan;

import java.util.ArrayList;
import java.util.List;

public final class RuinConquests {
    public static final RuinMix Tread = RuinMix.of(RuinMat.CoarseDirt, 50, RuinMat.Dirt, 30, RuinMat.PackedMud, 20);

    private RuinConquests() {
    }

    public static void watchtower(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Height = 12 + Random.nextInt(3);
        Canvas.scorchGround(0, 0, 10.0, 0.55, 61);
        Canvas.box(-3, 0, -3, 3, 0, 3, RuinMix.Plinth);
        RuinKit.ring(Canvas, -2, -2, 2, 2, 1, Height, RuinMix.Dressed, RuinMix.Plinth);
        Canvas.clear(-1, 1, -1, 1, Height, 1);
        for (int Level : new int[]{5, 9}) {
            for (int X = -1; X <= 1; X++) {
                for (int Z = -1; Z <= 1; Z++) {
                    if (Random.at(X, Level, Z, 62) < 0.72) {
                        Canvas.put(X, Level, Z, RuinMat.DarkOakPlanks);
                    }
                }
            }
            RuinKit.band(Canvas, -2, -2, 2, 2, Level, RuinMix.Brass);
        }
        for (int Y = 1; Y <= 4; Y++) {
            if (Y % 2 == 1) {
                Canvas.put(-1, Y, -1, RuinMat.CrimsonTrapdoor, RuinMat.East, 2);
            }
        }
        RuinKit.opening(Canvas, 0, 1, 2, true, 1, 3, false);
        for (int Level : new int[]{6, 10}) {
            Canvas.air(0, Level, -2);
            Canvas.air(0, Level, 2);
            Canvas.air(-2, Level, 0);
            Canvas.air(2, Level, 0);
        }
        RuinKit.merlons(Canvas, -2, -2, 2, -2, Height + 1, RuinMix.Dressed);
        RuinKit.merlons(Canvas, -2, 2, 2, 2, Height + 1, RuinMix.Dressed);
        RuinKit.merlons(Canvas, -2, -1, -2, 1, Height + 1, RuinMix.Dressed);
        RuinKit.merlons(Canvas, 2, -1, 2, 1, Height + 1, RuinMix.Dressed);
        Canvas.brazier(1, 10, 1, false);
        RuinKit.wallBanner(Canvas, -1, 7, 3, RuinMat.South);
        RuinKit.wallBanner(Canvas, 1, 7, 3, RuinMat.South);
        for (int[] Hut : new int[][]{{7, 3}, {-7, -4}}) {
            int Hx = Hut[0];
            int Hz = Hut[1];
            Canvas.box(Hx - 2, 0, Hz - 2, Hx + 2, 0, Hz + 2, RuinMix.Rough);
            for (int[] Corner : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
                int Tall = 1 + Random.nextInt(3);
                for (int Y = 1; Y <= Tall; Y++) {
                    Canvas.put(Hx + Corner[0], Y, Hz + Corner[1], Y == Tall && Random.chance(0.5) ? RuinMat.CoalBlock : RuinMat.DarkOakLog, RuinMat.Up);
                }
            }
            for (int X = Hx - 1; X <= Hx + 1; X++) {
                for (int Z = Hz - 1; Z <= Hz + 1; Z++) {
                    if (Random.at(X, 1, Z, 63) < 0.45) {
                        Canvas.put(X, 1, Z, Random.at(X, 1, Z, 64) < 0.5 ? RuinMat.BlackCarpet : RuinMat.DarkOakSlab);
                    }
                }
            }
        }
        Canvas.brush(8, 0, -1, true);
        Canvas.chest(1, 1, -1, RuinMat.West, RuinCanvas.LootCache);
        RuinDanger.dartTrap(Canvas, 0, 1, 1, true, 3, RuinDanger.Arrows, false);
        Canvas.put(-1, 6, 1, RuinMat.Cobweb);
        Canvas.put(1, 10, -1, RuinMat.Cobweb);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -6, 1, -6, 6, 6);
        Spots.removeIf(Spot -> Math.abs(Spot[0]) <= 2 && Math.abs(Spot[2]) <= 2);
        Spots.add(new int[]{-1, 10, 0});
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Canvas.tier()));
        Canvas.entrance(0, 1, 4, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void warCamp(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        Canvas.scorchGround(0, 0, 12.0, 0.65, 71);
        Canvas.disc(0, 0, 0, 1.6, RuinMix.of(RuinMat.CoalBlock, 30, RuinMat.SootPowder, 30, RuinMat.Blackstone, 40));
        Canvas.put(0, 1, 0, RuinMat.Campfire, RuinMat.South, 0);
        for (int[] Ring : new int[][]{{-2, 0}, {2, 0}, {0, -2}, {0, 2}, {-1, -2}, {1, 2}}) {
            Canvas.slab(Ring[0], 1, Ring[1], RuinMat.BlackstoneSlab, false);
        }
        int[][] Tents = {{-6, 4, 0}, {6, 4, 0}, {-7, -3, 1}, {7, -3, 1}, {0, 8, 0}};
        int Fallen = Random.nextInt(Tents.length);
        for (int Index = 0; Index < Tents.length; Index++) {
            int[] Tent = Tents[Index];
            RuinKit.tent(Canvas, Tent[0], Tent[1], 5, Tent[2] == 1, Index == Fallen, 72 + Index);
        }
        RuinKit.tent(Canvas, 0, -8, 7, true, false, 79);
        Canvas.chest(0, 1, -8, RuinMat.South, RuinCanvas.LootCache);
        Canvas.put(-2, 1, -8, RuinMat.RuinWeaponRack, RuinMat.East);
        Canvas.put(2, 1, -8, RuinMat.Barrel, RuinMat.Up, RuinCanvas.LootNone);
        RuinKit.bannerPole(Canvas, -3, -5, 4, RuinMat.South);
        RuinKit.bannerPole(Canvas, 3, -5, 4, RuinMat.South);
        for (int X = -11; X <= 11; X++) {
            if (Random.at(X, 0, -12, 73) < 0.62) {
                Canvas.put(X, 0, -12, RuinMix.Ash);
                int Tall = 1 + Random.nextInt(3);
                for (int Y = 1; Y <= Tall; Y++) {
                    Canvas.put(X, Y, -12, RuinMat.StrippedDarkOakLog, RuinMat.Up);
                }
                if (Tall == 3) {
                    Canvas.put(X, 4, -12, RuinMat.DarkOakFence);
                }
            }
        }
        for (int[] Rack : new int[][]{{-4, 6}, {4, 6}, {-9, -1}, {9, -1}}) {
            Canvas.put(Rack[0], 1, Rack[1], RuinMat.RuinWeaponRack, Rack[0] < 0 ? RuinMat.East : RuinMat.West);
        }
        for (int Index = 0; Index < 7; Index++) {
            int X = Random.range(-10, 10);
            int Z = Random.range(-10, 10);
            if (Math.abs(X) + Math.abs(Z) < 4 || Canvas.filled(X, 1, Z)) {
                continue;
            }
            Canvas.spear(X, 1, Z, 1 + Random.nextInt(2), Random.chance(0.7));
        }
        for (int[] Crate : new int[][]{{9, 4}, {9, 5}, {10, 4}, {-9, 5}}) {
            Canvas.put(Crate[0], 1, Crate[1], RuinMat.Barrel, RuinMat.Up, Crate[0] == -9 ? RuinCanvas.LootCache : RuinCanvas.LootNone);
            Canvas.keep(Crate[0], 1, Crate[1]);
        }
        Canvas.put(-6, 1, 4, RuinMat.Cobweb);
        Canvas.put(7, 1, -3, RuinMat.Cobweb);
        Canvas.brush(-3, 0, 9, true);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -10, 1, -10, 10, 10);
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Canvas.tier()));
        Canvas.entrance(0, 1, 12, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void barracks(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Wx = 4;
        int Wz = 7;
        int Height = 5;
        Canvas.scorchGround(0, 0, 10.0, 0.4, 81);
        Canvas.box(-Wx, 0, -Wz, Wx, 0, Wz, RuinMix.Floor);
        RuinKit.ring(Canvas, -Wx, -Wz, Wx, Wz, 1, Height, RuinMix.Dressed, RuinMix.Plinth);
        Canvas.clear(-Wx + 1, 1, -Wz + 1, Wx - 1, Height + 1, Wz - 1);
        RuinKit.opening(Canvas, 0, 1, Wz, true, 3, 4, true);
        for (int Z = -Wz + 2; Z <= Wz - 2; Z += 4) {
            Canvas.air(-Wx, 3, Z);
            Canvas.air(Wx, 3, Z);
        }
        RuinKit.band(Canvas, -Wx, -Wz, Wx, Wz, Height, RuinMix.Crimson);
        RuinKit.beams(Canvas, -Wx, -Wz, Wx, Wz, Height + 1, true, 3, RuinMat.StrippedDarkOakLog);
        RuinKit.roof(Canvas, -Wx, -Wz, Wx, Wz, Height + 2, RuinMat.DarkOakSlab, 0.45, 82);
        for (int Z = -Wz + 2; Z <= Wz - 3; Z += 3) {
            for (int Side : new int[]{-1, 1}) {
                int X = Side * (Wx - 1);
                Canvas.slab(X, 1, Z, RuinMat.DarkOakSlab, true);
                Canvas.slab(X, 1, Z + 1, RuinMat.DarkOakSlab, true);
                Canvas.put(X, 2, Z, Random.chance(0.6) ? RuinMat.RedCarpet : RuinMat.BlackCarpet);
                Canvas.put(X, 2, Z + 1, Random.chance(0.6) ? RuinMat.RedCarpet : RuinMat.BlackCarpet);
            }
        }
        for (int X = -1; X <= 1; X++) {
            Canvas.put(X, 1, -Wz + 1, X == 0 ? RuinMat.Campfire : RuinMat.BlackstoneBricks, RuinMat.South);
            Canvas.put(X, 2, -Wz + 1, X == 0 ? RuinMat.Air : RuinMat.BlackstoneBricks);
            Canvas.put(X, 3, -Wz + 1, RuinMat.BlackstoneBrickSlab, RuinMat.South, 0);
        }
        Canvas.chest(-2, 1, -Wz + 1, RuinMat.South, RuinCanvas.LootCache);
        Canvas.put(2, 1, -Wz + 1, RuinMat.Barrel, RuinMat.Up, RuinCanvas.LootNone);
        Canvas.put(-Wx + 1, 1, 0, RuinMat.RuinWeaponRack, RuinMat.East);
        Canvas.put(Wx - 1, 1, 1, RuinMat.RuinWeaponRack, RuinMat.West);
        RuinKit.wallBanner(Canvas, 0, 3, -Wz + 1, RuinMat.South);
        RuinKit.hill(Canvas, -Wx - 9, -Wz - 9, Wx + 7, -Wz - 1, Height + 1, true, true, true, false, 85);
        RuinKit.hill(Canvas, -Wx - 7, -Wz, -Wx - 1, Wz - 1, Height - 1, true, false, false, true, 86);
        Canvas.put(Wx - 2, 1, -2, RuinMat.Cobweb);
        Canvas.put(-Wx + 2, 2, 3, RuinMat.Cobweb);
        RuinDanger.dartTrap(Canvas, 0, 1, Wz - 2, true, Wx + 1, RuinDanger.Arrows, false);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -Wx + 1, 1, -Wz + 2, Wx - 1, Wz - 3);
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Canvas.tier()) + 1);
        Canvas.entrance(0, 1, Wz + 2, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void bridge(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Bank = 9;
        for (int X = -13; X <= 13; X++) {
            for (int Z = -Bank + 2; Z <= Bank - 2; Z++) {
                int Depth = Math.abs(Z) <= Bank - 4 ? 3 : 2;
                for (int Y = 0; Y > -Depth; Y--) {
                    Canvas.air(X, Y, Z);
                }
                Canvas.put(X, -Depth, Z, Random.at(X, -Depth, Z, 91) < 0.7 ? RuinMat.Gravel : RuinMat.CoarseDirt);
                if (Random.at(X, -Depth + 1, Z, 92) < 0.08) {
                    Canvas.put(X, -Depth + 1, Z, RuinMat.Growth);
                }
            }
        }
        int Deck = 3;
        for (int Pier : new int[]{-4, 0, 4}) {
            Canvas.box(-1, -3, Pier - 1, 1, Deck - 1, Pier + 1, RuinMix.Plinth);
            Canvas.box(-1, 0, Pier - 1, 1, Deck - 1, Pier + 1, RuinMix.Dressed);
            Canvas.box(-2, -3, Pier, 2, -2, Pier, RuinMix.Plinth);
        }
        for (int Z = -Bank - 1; Z <= Bank + 1; Z++) {
            boolean Gap = Z >= -3 && Z <= -1;
            boolean Abutment = Math.abs(Z) >= Bank - 1;
            if (Abutment) {
                Canvas.box(-1, 0, Z, 1, Deck - 1, Z, RuinMix.Plinth);
            }
            if (Gap) {
                continue;
            }
            for (int X = -1; X <= 1; X++) {
                Canvas.put(X, Deck, Z, RuinMix.Paving.at(Random, X, Deck, Z).shape() == RuinMat.Shape.Cube ? RuinMix.Paving.at(Random, X, Deck, Z) : RuinMat.SmoothBasalt);
            }
            if (Random.at(-2, Deck, Z, 93) < 0.7) {
                Canvas.put(-2, Deck, Z, RuinMat.RedSandstoneWall);
            }
            if (Random.at(2, Deck, Z, 94) < 0.7) {
                Canvas.put(2, Deck, Z, RuinMat.RedSandstoneWall);
            }
        }
        for (int Pier : new int[]{-4, 0, 4}) {
            for (int Side : new int[]{-2, 2}) {
                int Z = Pier + Side;
                if (Z >= -3 && Z <= -1) {
                    continue;
                }
                for (int X = -1; X <= 1; X++) {
                    Canvas.stairs(X, Deck - 1, Z, RuinMat.RedSandstoneStairs, Side < 0 ? RuinMat.South : RuinMat.North, true);
                }
            }
        }
        int Gz = Bank + 1;
        Canvas.box(-5, 0, Gz, 5, 0, Gz + 6, RuinMix.Plinth);
        RuinKit.ring(Canvas, -5, Gz, 5, Gz + 6, 1, 6, RuinMix.Dressed, RuinMix.Plinth);
        Canvas.clear(-4, 1, Gz + 1, 4, 6, Gz + 5);
        RuinKit.wall(Canvas, -2, Gz + 1, -2, Gz + 5, 1, 6, RuinMix.Dressed, RuinMix.Plinth);
        RuinKit.wall(Canvas, 2, Gz + 1, 2, Gz + 5, 1, 6, RuinMix.Dressed, RuinMix.Plinth);
        RuinKit.opening(Canvas, 0, 1, Gz, true, 3, 4, true);
        RuinKit.opening(Canvas, 0, 1, Gz + 6, true, 3, 4, true);
        Canvas.clear(-1, 1, Gz + 1, 1, 4, Gz + 5);
        Canvas.box(-1, 5, Gz + 1, 1, 6, Gz + 5, RuinMix.Dressed);
        for (int X = -1; X <= 1; X++) {
            Canvas.put(X, 4, Gz + 3, RuinMat.IronBars);
        }
        RuinKit.opening(Canvas, -2, 1, Gz + 4, false, 1, 2, false);
        RuinKit.opening(Canvas, 2, 1, Gz + 4, false, 1, 2, false);
        for (int[] Corner : new int[][]{{-5, Gz}, {4, Gz}, {-5, Gz + 5}, {4, Gz + 5}}) {
            Canvas.box(Corner[0], 7, Corner[1], Corner[0] + 1, 8, Corner[1] + 1, RuinMix.Dressed);
            Canvas.put(Corner[0], 9, Corner[1], RuinMix.Dressed);
        }
        RuinKit.merlons(Canvas, -3, Gz, 3, Gz, 7, RuinMix.Dressed);
        RuinKit.wallBanner(Canvas, -3, 4, Gz - 1, RuinMat.North);
        RuinKit.wallBanner(Canvas, 3, 4, Gz - 1, RuinMat.North);
        Canvas.chest(-4, 1, Gz + 1, RuinMat.East, RuinCanvas.LootCache);
        Canvas.put(4, 1, Gz + 1, RuinMat.RuinWeaponRack, RuinMat.West);
        RuinDanger.dartTrap(Canvas, 0, 1, Gz + 2, true, 3, RuinDanger.Arrows, false);
        Canvas.put(-1, 3, Gz + 5, RuinMat.Cobweb);
        List<int[]> Spots = new ArrayList<>(RuinDanger.floorSpots(Canvas, -4, 1, Gz + 1, 4, Gz + 5));
        Spots.addAll(RuinDanger.floorSpots(Canvas, -1, Deck + 1, -Bank, 1, Bank));
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Canvas.tier()));
        Canvas.brush(-6, -2, -2, true);
        Canvas.entrance(0, 1, Gz + 8, RuinMat.South);
        Canvas.centre(0, Deck + 1, 0);
    }

    public static void armory(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        Canvas.scorchGround(0, 0, 6.0, 0.35, 101);
        Canvas.box(-3, 0, -1, 3, 0, 5, RuinMix.Plinth);
        for (int[] Corner : new int[][]{{-3, -1}, {3, -1}, {-3, 5}, {3, 5}}) {
            Canvas.column(Corner[0], 1, Corner[1], 2 + Random.nextInt(3), true);
        }
        int Top = 0;
        int Bottom = -6;
        for (int Step = 0; Step < 6; Step++) {
            int Z = 3 - Step;
            int Y = Top - Step;
            for (int X = -1; X <= 1; X++) {
                for (int Clear = Y + 1; Clear <= Top + 3; Clear++) {
                    if (Clear <= 0 || Step == 0) {
                        Canvas.air(X, Clear, Z);
                    }
                }
                Canvas.stairs(X, Y, Z, RuinMat.BlackstoneBrickStairs, RuinMat.North, false);
                Canvas.put(X, Y - 1, Z, RuinMix.DarkBrick);
            }
            Canvas.put(-2, Y, Z, RuinMix.DarkBrick);
            Canvas.put(2, Y, Z, RuinMix.DarkBrick);
            Canvas.put(-2, Y + 1, Z, RuinMix.DarkBrick);
            Canvas.put(2, Y + 1, Z, RuinMix.DarkBrick);
            Canvas.put(-2, Y + 2, Z, RuinMix.DarkBrick);
            Canvas.put(2, Y + 2, Z, RuinMix.DarkBrick);
        }
        int Floor = Bottom - 1;
        Canvas.box(-1, Floor, -5, 1, Floor, -3, RuinMix.DarkBrick);
        Canvas.clear(-1, Bottom, -5, 1, Bottom + 2, -3);
        Canvas.box(-2, Bottom, -5, -2, Bottom + 3, -3, RuinMix.DarkBrick);
        Canvas.box(2, Bottom, -5, 2, Bottom + 3, -3, RuinMix.DarkBrick);
        Canvas.box(-1, Bottom + 3, -5, 1, Bottom + 3, -3, RuinMix.DarkBrick);
        int Rx = 5;
        int Rz1 = -14;
        int Rz2 = -6;
        Canvas.box(-Rx - 1, Floor - 1, Rz1 - 1, Rx + 1, Bottom + 4, Rz2 + 1, RuinMix.Dressed);
        Canvas.box(-Rx, Floor, Rz1, Rx, Floor, Rz2, RuinMix.Floor);
        Canvas.clear(-Rx, Bottom, Rz1, Rx, Bottom + 3, Rz2);
        Canvas.clear(-1, Bottom, Rz2 + 1, 1, Bottom + 2, Rz2 + 1);
        RuinKit.band(Canvas, -Rx - 1, Rz1 - 1, Rx + 1, Rz2 + 1, Bottom + 2, RuinMix.Brass);
        for (int Z = Rz1 + 1; Z <= Rz2 - 1; Z += 2) {
            Canvas.put(-Rx, Bottom, Z, RuinMat.RuinWeaponRack, RuinMat.East);
            Canvas.put(Rx, Bottom, Z, RuinMat.RuinWeaponRack, RuinMat.West);
        }
        Canvas.chest(0, Bottom, Rz1, RuinMat.South, RuinCanvas.LootVault);
        Canvas.chest(-2, Bottom, Rz1, RuinMat.South, RuinCanvas.LootCache);
        Canvas.put(2, Bottom, Rz1, RuinMat.DamagedAnvil, RuinMat.East);
        Canvas.put(3, Bottom, Rz1, RuinMat.Barrel, RuinMat.Up, RuinCanvas.LootNone);
        Canvas.put(-3, Bottom, Rz1, RuinMat.Barrel, RuinMat.Up, RuinCanvas.LootNone);
        Canvas.hangingLantern(0, Bottom + 3, (Rz1 + Rz2) / 2, 1);
        RuinKit.wallBanner(Canvas, 0, Bottom + 2, Rz1, RuinMat.South);
        Canvas.put(-Rx, Bottom + 3, Rz2, RuinMat.Cobweb);
        Canvas.put(Rx, Bottom + 3, Rz1, RuinMat.Cobweb);
        Canvas.put(Rx - 1, Bottom, Rz2, RuinMat.Cobweb);
        RuinDanger.dartTrap(Canvas, 0, Bottom, -4, true, 3, RuinDanger.Arrows, false);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -Rx + 1, Bottom, Rz1 + 1, Rx - 1, Rz2 - 1);
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Canvas.tier()));
        Canvas.brush(4, 0, 6, false);
        Canvas.entrance(0, 1, 7, RuinMat.South);
        Canvas.centre(0, 1, 2);
    }

    public static void battlefield(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Reach = 13;
        Canvas.scorchGround(0, 0, Reach, 0.72, 111);
        List<int[]> Craters = new ArrayList<>();
        for (int Index = 0; Index < 5; Index++) {
            int X = Random.range(-Reach + 3, Reach - 3);
            int Z = Random.range(-Reach + 3, Reach - 3);
            double Size = 1.8 + Random.nextDouble() * 1.6;
            Canvas.crater(X, Z, Size, 112 + Index);
            Craters.add(new int[]{X, Z});
        }
        for (int Index = 0; Index < 2; Index++) {
            int[] Crater = Craters.get(Index);
            Canvas.brush(Crater[0], -1, Crater[1], true);
        }
        for (int Index = 0; Index < 28; Index++) {
            int X = Random.range(-Reach, Reach);
            int Z = Random.range(-Reach, Reach);
            if (Canvas.filled(X, 1, Z) || !Canvas.filled(X, 0, Z) && Random.chance(0.5)) {
                continue;
            }
            if (!Canvas.filled(X, 0, Z)) {
                Canvas.put(X, 0, Z, RuinMix.Ash);
            }
            if (Canvas.at(X, 0, Z).isAir()) {
                continue;
            }
            Canvas.spear(X, 1, Z, 1 + Random.nextInt(2), Random.chance(0.5));
        }
        for (int Index = 0; Index < 8; Index++) {
            int X = Random.range(-Reach, Reach);
            int Z = Random.range(-Reach, Reach);
            if (Canvas.filled(X, 1, Z) || Canvas.at(X, 0, Z).isAir()) {
                continue;
            }
            double Pick = Random.nextDouble();
            if (Pick < 0.4) {
                Canvas.slab(X, 1, Z, Random.chance(0.5) ? RuinMat.AgedCopperSlab : RuinMat.CopperSlab, false);
            } else if (Pick < 0.7) {
                Canvas.put(X, 1, Z, RuinMat.BoneBlock, Random.chance(0.5) ? RuinMat.East : RuinMat.South);
            } else {
                Canvas.put(X, 1, Z, RuinMat.RuinUrn, RuinMat.South);
            }
        }
        for (int[] Cairn : new int[][]{{-8, -7}, {9, 6}}) {
            Canvas.rubble(Cairn[0], Cairn[1], 1.8, 3, 113);
            Canvas.spear(Cairn[0], Canvas.plan().topAt(Cairn[0], Cairn[1]) + 1, Cairn[1], 2, true);
        }
        RuinKit.cart(Canvas, 4, -6, true);
        RuinKit.cart(Canvas, -6, 5, false);
        RuinKit.bannerPole(Canvas, 0, 0, 5, RuinMat.South);
        for (int Step = 0; Step < 6; Step++) {
            Canvas.put(-3 - Step, 1, 9, RuinMat.StrippedDarkOakLog, RuinMat.East);
            Canvas.put(-3 - Step, 0, 9, RuinMix.Ash);
            if (Step < 4) {
                Canvas.put(-3 - Step, 1, 10, Random.chance(0.6) ? RuinMat.RedCarpet : RuinMat.BlackCarpet);
                Canvas.put(-3 - Step, 0, 10, RuinMix.Ash);
            }
        }
        Canvas.chest(9, 1, -9, RuinMat.West, RuinCanvas.LootCache);
        Canvas.put(9, 0, -9, RuinMix.Plinth);
        Canvas.rubble(10, -8, 1.2, 1, 114);
        RuinDanger.collapse(Canvas, 5, 2, 6, 3, 0, 3, RuinDanger.PitBones, Tread);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -Reach, 1, -Reach, Reach, Reach);
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Canvas.tier()) + 1);
        Canvas.entrance(0, 1, Reach + 2, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void waystation(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Length = 18;
        for (int X = -Length; X <= Length; X++) {
            for (int Z = -2; Z <= 2; Z++) {
                if (Random.at(X, 0, Z, 121) < 0.1) {
                    Canvas.put(X, 0, Z, RuinMix.Ash);
                    continue;
                }
                RuinMat Stone = RuinMix.Paving.at(Random, X, 0, Z);
                Canvas.put(X, 0, Z, Stone == RuinMat.Gravel || Stone == RuinMat.CoarseDirt ? RuinMat.SmoothBasalt : Stone);
            }
            for (int Side : new int[]{-3, 3}) {
                if (Random.at(X, 1, Side, 122) < 0.8) {
                    Canvas.put(X, 0, Side, RuinMix.Plinth);
                    if (Random.at(X, 2, Side, 123) < 0.5) {
                        Canvas.slab(X, 1, Side, RuinMat.CutRedSandstoneSlab, false);
                    }
                }
            }
        }
        for (int X = -Length + 2; X <= Length - 2; X += 8) {
            Canvas.put(X, 0, -4, RuinMix.Plinth);
            Canvas.put(X, 1, -4, RuinMat.ChiseledRedSandstone);
            Canvas.put(X, 2, -4, RuinMat.RuinReliefHost, RuinMat.South);
            Canvas.put(X, 3, -4, RuinMix.Brass);
        }
        int Sx = 2;
        int Sz = 5;
        Canvas.box(Sx - 1, 0, Sz, Sx + 9, 0, Sz + 7, RuinMix.Floor);
        RuinKit.wall(Canvas, Sx - 1, Sz + 7, Sx + 9, Sz + 7, 1, 5, RuinMix.Dressed, RuinMix.Plinth);
        RuinKit.wall(Canvas, Sx + 9, Sz + 3, Sx + 9, Sz + 7, 1, 5, RuinMix.Dressed, RuinMix.Plinth);
        RuinKit.wall(Canvas, Sx + 5, Sz + 3, Sx + 5, Sz + 7, 1, 5, RuinMix.Dressed, RuinMix.Plinth);
        RuinKit.wall(Canvas, Sx + 5, Sz + 3, Sx + 9, Sz + 3, 1, 5, RuinMix.Dressed, RuinMix.Plinth);
        RuinKit.opening(Canvas, Sx + 7, 1, Sz + 3, true, 1, 2, false);
        Canvas.clear(Sx + 6, 1, Sz + 4, Sx + 8, 4, Sz + 6);
        for (int X = Sx - 1; X <= Sx + 4; X += 5) {
            Canvas.column(X, 1, Sz, 5, true);
        }
        Canvas.column(Sx - 1, 1, Sz + 3, 5, false);
        RuinKit.beams(Canvas, Sx - 1, Sz, Sx + 4, Sz + 7, 6, false, 5, RuinMat.StrippedDarkOakLog);
        RuinKit.roof(Canvas, Sx - 1, Sz, Sx + 9, Sz + 7, 6, RuinMat.CutRedSandstoneSlab, 0.5, 124);
        Canvas.put(Sx + 2, 1, Sz + 6, RuinMat.PolishedBasalt, RuinMat.Up);
        Canvas.brazier(Sx + 2, 2, Sz + 6, false);
        RuinKit.wallBanner(Canvas, Sx + 1, 3, Sz + 6, RuinMat.North);
        RuinKit.wallBanner(Canvas, Sx + 3, 3, Sz + 6, RuinMat.North);
        Canvas.pot(Sx + 1, 1, Sz + 6, RuinMat.North, RuinCanvas.LootOffering);
        Canvas.stairs(Sx, 1, Sz + 4, RuinMat.SmoothRedSandstoneStairs, RuinMat.West, false);
        Canvas.stairs(Sx, 1, Sz + 3, RuinMat.SmoothRedSandstoneStairs, RuinMat.West, false);
        Canvas.chest(Sx + 8, 1, Sz + 6, RuinMat.West, RuinCanvas.LootCache);
        Canvas.put(Sx + 6, 1, Sz + 6, RuinMat.Barrel, RuinMat.Up, RuinCanvas.LootNone);
        Canvas.put(Sx + 6, 1, Sz + 4, RuinMat.Cobweb);
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                Canvas.put(-8 + Dx, 0, 7 + Dz, Dx == 0 && Dz == 0 ? RuinMat.Cauldron : RuinMix.Plinth.at(Random, -8 + Dx, 0, 7 + Dz));
                if (Dx != 0 || Dz != 0) {
                    Canvas.slab(-8 + Dx, 1, 7 + Dz, RuinMat.PolishedBlackstoneSlab, false);
                }
            }
        }
        Canvas.put(-8, -1, 7, RuinMix.Plinth);
        Canvas.put(-12, 1, 6, RuinMat.DarkOakFence);
        Canvas.put(-12, 1, 8, RuinMat.DarkOakFence);
        RuinKit.cart(Canvas, -5, 0, true);
        RuinDanger.dartTrap(Canvas, Sx + 7, 1, Sz + 5, true, 3, RuinDanger.Arrows, false);
        List<int[]> Spots = RuinDanger.floorSpots(Canvas, -Length, 1, -3, Length, Sz + 6);
        RuinDanger.guardians(Canvas, Spots, RuinDanger.guardianCount(Canvas.tier()));
        Canvas.brush(-14, 0, 4, true);
        Canvas.entrance(0, 1, 0, RuinMat.South);
        Canvas.centre(Sx + 4, 1, Sz + 3);
    }
}
