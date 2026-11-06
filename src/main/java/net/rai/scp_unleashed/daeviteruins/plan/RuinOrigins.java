package net.rai.scp_unleashed.daeviteruins.plan;

public final class RuinOrigins {
    private RuinOrigins() {
    }

    public static void shrine(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Half = 2 + Random.nextInt(2);
        Canvas.scorchGround(0, 0, Half + 3.5, 0.45, 11);
        Canvas.box(-Half - 1, 0, -Half - 1, Half + 1, 0, Half + 1, RuinMix.Floor);
        Canvas.box(-Half, 0, -Half, Half, 0, Half, RuinMix.Plinth);
        Canvas.box(-Half, 1, -Half, Half, 1, Half, RuinMix.Plinth);
        for (int X = -Half; X <= Half; X++) {
            Canvas.stairs(X, 1, Half + 1, RuinMat.RedSandstoneStairs, RuinMat.North, false);
        }
        for (int Z = -Half; Z <= Half; Z++) {
            if (Random.at(-Half - 1, 1, Z, 3) < 0.7) {
                Canvas.stairs(-Half - 1, 1, Z, RuinMat.RedSandstoneStairs, RuinMat.East, false);
            }
            if (Random.at(Half + 1, 1, Z, 3) < 0.7) {
                Canvas.stairs(Half + 1, 1, Z, RuinMat.RedSandstoneStairs, RuinMat.West, false);
            }
        }
        int Back = -Half;
        for (int X = -Half; X <= Half; X++) {
            int Top = Math.abs(X) == Half ? 4 : 3;
            for (int Y = 2; Y <= Top; Y++) {
                Canvas.put(X, Y, Back, RuinMix.Dressed);
            }
        }
        Canvas.put(0, 4, Back, RuinMat.RuinReliefGod, RuinMat.South);
        Canvas.put(0, 5, Back, RuinMat.ChiseledRedSandstone);
        Canvas.stairs(-1, 4, Back, RuinMat.RedSandstoneStairs, RuinMat.East, false);
        Canvas.stairs(1, 4, Back, RuinMat.RedSandstoneStairs, RuinMat.West, false);
        Canvas.put(-Half, 5, Back, RuinMix.Brass);
        Canvas.put(Half, 5, Back, RuinMix.Brass);
        Canvas.pedestalBrazier(0, 2, Back + 1, false);
        Canvas.banner(-1, 3, Back + 1, RuinMat.South);
        Canvas.banner(1, 3, Back + 1, RuinMat.South);
        Canvas.put(-Half, 2, Half, RuinMat.PolishedBasalt, RuinMat.Up);
        Canvas.put(Half, 2, Half, RuinMat.PolishedBasalt, RuinMat.Up);
        Canvas.candles(-Half, 3, Half, 3);
        Canvas.candles(Half, 3, Half, 2);
        Canvas.pot(-1, 2, Back + 2, RuinMat.South, RuinCanvas.LootOffering);
        Canvas.candles(1, 2, Back + 2, 1);
        Canvas.put(0, 2, Back + 2, RuinMat.BlackCarpet);
        Canvas.clearIfEmpty(-Half, 2, -Half, Half, 5, Half);
        Canvas.brush(Half + 2, 0, 1, false);
        Canvas.rubble(-Half - 2, -Half + 1, 1.6, 1, 13);
        Canvas.entrance(0, 1, Half + 2, RuinMat.South);
        Canvas.centre(0, 2, 0);
    }

    public static void stoneRing(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Count = 7 + Random.nextInt(3);
        double Radius = 5.0 + Random.nextDouble() * 1.2;
        Canvas.scorchGround(0, 0, Radius + 1.5, 0.6, 21);
        Canvas.disc(0, 0, 0, 2.2, RuinMix.Ash);
        Canvas.box(-1, 0, -1, 1, 0, 1, RuinMix.of(RuinMat.CoalBlock, 30, RuinMat.SootPowder, 40, RuinMat.Blackstone, 30));
        Canvas.put(0, 1, 0, RuinMat.Campfire, RuinMat.South, 0);
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                int Ring = Math.max(Math.abs(Dx), Math.abs(Dz));
                if (Ring == 2 && Math.abs(Dx) + Math.abs(Dz) < 4 && Random.at(Dx, 1, Dz, 22) < 0.8) {
                    Canvas.slab(Dx, 1, Dz, Random.at(Dx, 2, Dz, 23) < 0.5 ? RuinMat.BlackstoneSlab : RuinMat.PolishedBlackstoneSlab, false);
                }
            }
        }
        double Spin = Random.nextDouble() * Math.PI * 2;
        int Toppled = 0;
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Spin + Index * Math.PI * 2 / Count;
            int X = (int) Math.round(Math.cos(Angle) * Radius);
            int Z = (int) Math.round(Math.sin(Angle) * Radius);
            int Height = 3 + Random.nextInt(3);
            boolean Wide = Random.chance(0.5);
            int Tx = Math.abs(Math.cos(Angle)) > Math.abs(Math.sin(Angle)) ? 0 : 1;
            int Tz = 1 - Tx;
            Canvas.put(X, 0, Z, RuinMix.Plinth);
            if (Wide) {
                Canvas.put(X + Tx, 0, Z + Tz, RuinMix.Plinth);
            }
            if (Toppled < 2 && Index % 3 == 1 && Random.chance(0.7)) {
                Toppled++;
                int Sx = (int) Math.signum(Math.round(Math.cos(Angle) * 2));
                int Sz = (int) Math.signum(Math.round(Math.sin(Angle) * 2));
                for (int Step = 0; Step < Height; Step++) {
                    int Px = X + Sx * Step;
                    int Pz = Z + Sz * Step;
                    Canvas.put(Px, 1, Pz, RuinMix.Rough.at(Random, Px, 1, Pz), Sx != 0 ? RuinMat.East : RuinMat.South);
                    Canvas.put(Px, 0, Pz, RuinMix.Ash);
                    if (Wide) {
                        Canvas.put(Px + Tx, 1, Pz + Tz, RuinMix.Rough);
                        Canvas.put(Px + Tx, 0, Pz + Tz, RuinMix.Ash);
                    }
                }
                continue;
            }
            for (int Y = 1; Y <= Height; Y++) {
                RuinMat Mat = Y == Height ? RuinMat.ChiseledRedSandstone : RuinMix.Rough.at(Random, X, Y, Z);
                Canvas.put(X, Y, Z, Mat);
                if (Wide) {
                    Canvas.put(X + Tx, Y, Z + Tz, Y == Height ? RuinMat.ChiseledRedSandstone : RuinMix.Rough.at(Random, X + Tx, Y, Z + Tz));
                }
            }
            if (Height >= 4 && Random.chance(0.45)) {
                int Facing = RuinCanvas.faceToward(-X, -Z);
                Canvas.put(X, 2, Z, Random.chance(0.5) ? RuinMat.RuinReliefHost : RuinMat.RuinReliefGod, Facing);
            }
            if (Random.chance(0.35)) {
                Canvas.put(X, Height + 1, Z, RuinMix.Brass);
            }
        }
        Canvas.pot(1, 1, -2 + (Random.chance(0.5) ? 0 : 4), RuinMat.South, RuinCanvas.LootOffering);
        Canvas.candles(-1, 1, 2, 2);
        Canvas.put(2, 1, 1, RuinMat.BoneBlock, RuinMat.East);
        Canvas.brush((int) Math.round(Radius) - 2, 0, -(int) Math.round(Radius) + 3, true);
        Canvas.entrance(0, 1, (int) Math.round(Radius) + 2, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void cairn(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Cx = -2;
        int Cz = -1;
        double Radius = 2.6 + Random.nextDouble() * 0.8;
        Canvas.scorchGround(0, 0, 6.5, 0.65, 31);
        int Peak = 3 + Random.nextInt(2);
        RuinMix Cairn = RuinMix.of(RuinMat.RedSandstone, 24, RuinMat.Blackstone, 22, RuinMat.Granite, 16, RuinMat.Terracotta, 12, RuinMat.Basalt, 10, RuinMat.Gravel, 8,
                RuinMat.CoarseDirt, 8);
        int Reach = (int) Math.ceil(Radius);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                if (Distance > Radius) {
                    continue;
                }
                int Height = (int) Math.round((1 - Distance / (Radius + 0.6)) * Peak + Random.at(Dx, 0, Dz, 32) * 0.8);
                Canvas.put(Cx + Dx, 0, Cz + Dz, Cairn);
                for (int Y = 1; Y <= Height; Y++) {
                    Canvas.put(Cx + Dx, Y, Cz + Dz, Cairn);
                }
            }
        }
        int Top = Canvas.plan().topAt(Cx, Cz);
        Canvas.put(Cx, Top + 1, Cz, RuinMat.ChiseledRedSandstone);
        Canvas.put(Cx, Top + 2, Cz, RuinMix.Brass);
        Canvas.put(Cx, Top + 3, Cz, RuinMat.LightningRod, RuinMat.Up);
        Canvas.put(Cx, 1, Cz + Reach, RuinMat.RuinReliefHost, RuinMat.South);
        Canvas.put(Cx, 0, Cz + Reach, RuinMix.Plinth);
        int Socket = 2;
        Canvas.put(Socket, 0, Cz, RuinMix.Plinth);
        Canvas.put(Socket, 1, Cz, RuinMat.PolishedBasalt, RuinMat.Up);
        Canvas.put(Socket, 2, Cz, RuinMat.StrippedDarkOakLog, RuinMat.Up);
        int Length = 6 + Random.nextInt(3);
        for (int Step = 1; Step <= Length; Step++) {
            Canvas.put(Socket + Step, 1, Cz, Step % 3 == 0 ? RuinMat.DarkOakLog : RuinMat.StrippedDarkOakLog, RuinMat.East);
            Canvas.put(Socket + Step, 0, Cz, RuinMix.Ash);
        }
        int Cross = Socket + Length - 1;
        Canvas.put(Cross, 1, Cz - 1, RuinMat.DarkOakFence);
        Canvas.put(Cross, 1, Cz + 1, RuinMat.DarkOakFence);
        Canvas.put(Cross, 0, Cz - 1, RuinMix.Ash);
        Canvas.put(Cross, 0, Cz + 1, RuinMix.Ash);
        for (int Step = 2; Step <= Length; Step++) {
            for (int Side = 1; Side <= 3; Side++) {
                int X = Socket + Step - (Side == 3 ? 1 : 0);
                int Z = Cz + Side;
                if (Random.at(X, 1, Z, 34) < 0.78 - Side * 0.12) {
                    Canvas.put(X, 1, Z, Random.at(X, 1, Z, 35) < 0.55 ? RuinMat.RedCarpet : RuinMat.BlackCarpet);
                }
            }
        }
        Canvas.put(Cx + Reach, 1, Cz + 2, RuinMat.RuinWeaponRack, RuinMat.South);
        for (int Index = 0; Index < 3; Index++) {
            int X = Cx - Reach + Index * 2;
            int Z = Cz - Reach - 1 + (Index == 1 ? 1 : 0);
            Canvas.put(X, 0, Z, RuinMix.Ash);
            Canvas.spear(X, 1, Z, 1 + Random.nextInt(2), Random.chance(0.6));
        }
        Canvas.brush(Cx + 1, 0, Cz + Reach + 1, true);
        Canvas.pot(Cx - Reach - 1, 1, Cz + 1, RuinMat.East, RuinCanvas.LootOffering);
        Canvas.entrance(1, 1, 5, RuinMat.South);
        Canvas.centre(0, 1, 0);
    }

    public static void obelisk(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        Canvas.scorchGround(0, 0, 6.0, 0.3, 41);
        Canvas.box(-3, 0, -3, 3, 0, 3, RuinMix.Floor);
        Canvas.box(-2, 1, -2, 2, 1, 2, RuinMix.Plinth);
        for (int Side = -2; Side <= 2; Side++) {
            Canvas.stairs(Side, 1, 3, RuinMat.PolishedBlackstoneStairs, RuinMat.North, false);
            Canvas.stairs(Side, 1, -3, RuinMat.PolishedBlackstoneStairs, RuinMat.South, false);
            Canvas.stairs(3, 1, Side, RuinMat.PolishedBlackstoneStairs, RuinMat.West, false);
            Canvas.stairs(-3, 1, Side, RuinMat.PolishedBlackstoneStairs, RuinMat.East, false);
        }
        Canvas.box(-1, 2, -1, 1, 2, 1, RuinMix.Brass);
        int Break = 6 + Random.nextInt(2);
        Canvas.box(-1, 3, -1, 1, Break, 1, RuinMix.Dressed);
        int[][] Faces = {{0, 1, RuinMat.South}, {0, -1, RuinMat.North}, {1, 0, RuinMat.East}, {-1, 0, RuinMat.West}};
        for (int[] Face : Faces) {
            for (int Y = 4; Y <= Math.min(Break, 5); Y++) {
                Canvas.put(Face[0], Y, Face[1], Y == 4 ? RuinMat.RuinReliefHost : RuinMat.RuinReliefGod, Face[2]);
            }
        }
        for (int Y = Break - 1; Y <= Break; Y++) {
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    if (Y == Break && Random.at(Dx, Y, Dz, 42) < 0.5 && (Dx != 0 || Dz != 0)) {
                        Canvas.air(Dx, Y, Dz);
                    }
                }
            }
        }
        int Fx = 4;
        for (int Step = 0; Step < 4; Step++) {
            for (int Dz = -1; Dz <= 0; Dz++) {
                Canvas.put(Fx + Step, 1, Dz + 2, Step < 3 ? RuinMix.Dressed.at(Random, Fx + Step, 1, Dz + 2) : RuinMat.ChiseledRedSandstone, RuinMat.East);
                Canvas.put(Fx + Step, 0, Dz + 2, RuinMix.Floor);
                if (Step < 2) {
                    Canvas.put(Fx + Step, 2, Dz + 2, RuinMix.Dressed);
                }
            }
        }
        Canvas.put(Fx + 2, 2, 2, RuinMat.RuinReliefHost, RuinMat.South);
        Canvas.put(Fx + 5, 1, 3, RuinMix.Gold);
        Canvas.put(Fx + 5, 0, 3, RuinMix.Ash);
        for (int[] Corner : new int[][]{{-4, -4}, {4, -4}, {-4, 4}}) {
            Canvas.put(Corner[0], 0, Corner[1], RuinMix.Plinth);
            Canvas.spear(Corner[0], 1, Corner[1], 2, true);
        }
        Canvas.rubble(-4, 1, 1.5, 1, 43);
        Canvas.brush(-3, 0, 4, false);
        Canvas.pot(2, 2, 2, RuinMat.South, RuinCanvas.LootOffering);
        Canvas.entrance(0, 1, 5, RuinMat.South);
        Canvas.centre(0, 2, 0);
    }

    public static void platform(RuinCanvas Canvas) {
        RuinRandom Random = Canvas.random();
        int Half = 3;
        Canvas.scorchGround(0, 0, 6.0, 0.25, 51);
        Canvas.box(-Half, 0, -Half, Half, 0, Half, RuinMix.Plinth);
        Canvas.box(-Half, 1, -Half, Half, 2, Half, RuinMix.Dressed);
        for (int X = -1; X <= 1; X++) {
            Canvas.stairs(X, 1, Half + 2, RuinMat.RedSandstoneStairs, RuinMat.North, false);
            Canvas.stairs(X, 2, Half + 1, RuinMat.RedSandstoneStairs, RuinMat.North, false);
            Canvas.put(X, 1, Half + 1, RuinMix.Dressed);
            Canvas.put(X, 0, Half + 1, RuinMix.Plinth);
            Canvas.put(X, 0, Half + 2, RuinMix.Plinth);
        }
        Canvas.box(-Half, 2, -Half, Half, 2, Half, RuinMix.Floor);
        for (int X = -Half; X <= Half; X++) {
            for (int Z = -Half; Z <= Half; Z++) {
                boolean Edge = Math.abs(X) == Half || Math.abs(Z) == Half;
                if (Edge && !(Z == Half && Math.abs(X) <= 1) && Random.at(X, 3, Z, 52) < 0.7) {
                    Canvas.slab(X, 3, Z, RuinMat.CutRedSandstoneSlab, false);
                }
            }
        }
        for (int X = -1; X <= 1; X++) {
            for (int Z = -1; Z <= 1; Z++) {
                Canvas.put(X, 3, Z, Random.at(X, 3, Z, 53) < 0.65 ? RuinMat.RedCarpet : RuinMat.BlackCarpet);
            }
        }
        Canvas.column(-Half + 1, 3, -Half + 1, 4, true);
        Canvas.column(Half - 1, 3, -Half + 1, 3, false);
        Canvas.put(-Half + 1, 7, -Half + 1, RuinMat.CutRedSandstoneSlab, RuinMat.South, 0);
        Canvas.put(-Half + 2, 6, -Half + 1, RuinMat.RedSandstoneStairs, RuinMat.West, 1);
        Canvas.pedestalBrazier(0, 3, -Half + 1, false);
        Canvas.candles(-1, 3, -2, 2);
        Canvas.candles(1, 3, -2, 1);
        Canvas.pot(Half - 1, 3, 1, RuinMat.West, RuinCanvas.LootOffering);
        Canvas.put(-Half - 2, 0, 0, RuinMix.Plinth);
        Canvas.put(-Half - 2, 0, -2, RuinMix.Plinth);
        Canvas.pillar(-Half - 2, 1, 2, 0, RuinMix.Timber);
        Canvas.pillar(-Half - 2, 1, 1, -2, RuinMix.Timber);
        Canvas.slab(-Half - 1, 3, 0, RuinMat.DarkOakSlab, false);
        Canvas.slab(-Half - 2, 3, 0, RuinMat.DarkOakSlab, false);
        Canvas.slab(-Half - 1, 3, -1, RuinMat.DarkOakSlab, false);
        Canvas.put(-Half - 2, 1, -1, RuinMat.Cauldron);
        Canvas.put(-Half - 2, 0, -1, RuinMix.Plinth);
        Canvas.brush(Half + 2, 0, -2, false);
        Canvas.clearIfEmpty(-Half, 3, -Half, Half, 7, Half);
        Canvas.entrance(0, 1, Half + 3, RuinMat.South);
        Canvas.centre(0, 3, 0);
    }
}
