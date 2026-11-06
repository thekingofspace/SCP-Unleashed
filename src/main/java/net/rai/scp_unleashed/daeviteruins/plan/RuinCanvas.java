package net.rai.scp_unleashed.daeviteruins.plan;

public final class RuinCanvas {
    public static final int LootCache = 0;
    public static final int LootVault = 1;
    public static final int LootOffering = 2;
    public static final int LootNone = 3;
    private final RuinPlan Plan;
    private final RuinRandom Random;
    private int NextGroup = 1;

    public RuinCanvas(RuinPlan Plan, RuinRandom Random) {
        this.Plan = Plan;
        this.Random = Random;
    }

    public RuinPlan plan() {
        return Plan;
    }

    public RuinRandom random() {
        return Random;
    }

    public int tier() {
        return Plan.tier();
    }

    public int nextGroup() {
        return NextGroup++;
    }

    public void put(int X, int Y, int Z, RuinMat Mat) {
        Plan.set(X, Y, Z, Mat, Mat.shape() == RuinMat.Shape.Pillar || Mat.shape() == RuinMat.Shape.Chain ? RuinMat.Up : RuinMat.South, 0);
    }

    public void put(int X, int Y, int Z, RuinMat Mat, int Facing) {
        Plan.set(X, Y, Z, Mat, Facing, 0);
    }

    public void put(int X, int Y, int Z, RuinMat Mat, int Facing, int Variant) {
        Plan.set(X, Y, Z, Mat, Facing, Variant);
    }

    public void put(int X, int Y, int Z, RuinMix Mix) {
        put(X, Y, Z, Mix.at(Random, X, Y, Z));
    }

    public void patch(int X, int Y, int Z, RuinMix Mix, int Size) {
        put(X, Y, Z, Mix.patch(Random, X, Y, Z, Size));
    }

    public void air(int X, int Y, int Z) {
        Plan.clearCell(X, Y, Z);
    }

    public boolean filled(int X, int Y, int Z) {
        return Plan.filled(X, Y, Z);
    }

    public RuinMat at(int X, int Y, int Z) {
        return Plan.matAt(X, Y, Z);
    }

    public double roll(int X, int Y, int Z, int Salt) {
        return Random.at(X, Y, Z, Salt);
    }

    public void box(int X1, int Y1, int Z1, int X2, int Y2, int Z2, RuinMix Mix) {
        for (int Y = Math.min(Y1, Y2); Y <= Math.max(Y1, Y2); Y++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
                    put(X, Y, Z, Mix);
                }
            }
        }
    }

    public void box(int X1, int Y1, int Z1, int X2, int Y2, int Z2, RuinMat Mat) {
        for (int Y = Math.min(Y1, Y2); Y <= Math.max(Y1, Y2); Y++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
                    put(X, Y, Z, Mat);
                }
            }
        }
    }

    public void clear(int X1, int Y1, int Z1, int X2, int Y2, int Z2) {
        for (int Y = Math.min(Y1, Y2); Y <= Math.max(Y1, Y2); Y++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
                    air(X, Y, Z);
                }
            }
        }
    }

    public void clearIfEmpty(int X1, int Y1, int Z1, int X2, int Y2, int Z2) {
        for (int Y = Math.min(Y1, Y2); Y <= Math.max(Y1, Y2); Y++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
                    if (!Plan.has(X, Y, Z)) {
                        air(X, Y, Z);
                    }
                }
            }
        }
    }

    public void walls(int X1, int Y1, int Z1, int X2, int Y2, int Z2, RuinMix Mix) {
        int Lx = Math.min(X1, X2);
        int Hx = Math.max(X1, X2);
        int Lz = Math.min(Z1, Z2);
        int Hz = Math.max(Z1, Z2);
        for (int Y = Math.min(Y1, Y2); Y <= Math.max(Y1, Y2); Y++) {
            for (int X = Lx; X <= Hx; X++) {
                put(X, Y, Lz, Mix);
                put(X, Y, Hz, Mix);
            }
            for (int Z = Lz + 1; Z < Hz; Z++) {
                put(Lx, Y, Z, Mix);
                put(Hx, Y, Z, Mix);
            }
        }
    }

    public void room(int X1, int Y1, int Z1, int X2, int Y2, int Z2, RuinMix Wall, RuinMix Floor) {
        walls(X1, Y1, Z1, X2, Y2, Z2, Wall);
        box(X1 + 1, Y1 - 1, Z1 + 1, X2 - 1, Y1 - 1, Z2 - 1, Floor);
        clear(X1 + 1, Y1, Z1 + 1, X2 - 1, Y2, Z2 - 1);
    }

    public void disc(int Cx, int Y, int Cz, double Radius, RuinMix Mix) {
        int Reach = (int) Math.ceil(Radius);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                if (Dx * Dx + Dz * Dz <= Radius * Radius + 0.5) {
                    put(Cx + Dx, Y, Cz + Dz, Mix);
                }
            }
        }
    }

    public void discAir(int Cx, int Y, int Cz, double Radius) {
        int Reach = (int) Math.ceil(Radius);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                if (Dx * Dx + Dz * Dz <= Radius * Radius + 0.5) {
                    air(Cx + Dx, Y, Cz + Dz);
                }
            }
        }
    }

    public void ring(int Cx, int Y, int Cz, double Inner, double Outer, RuinMix Mix) {
        int Reach = (int) Math.ceil(Outer);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Distance = Dx * Dx + Dz * Dz;
                if (Distance <= Outer * Outer + 0.5 && Distance > Inner * Inner + 0.5) {
                    put(Cx + Dx, Y, Cz + Dz, Mix);
                }
            }
        }
    }

    public boolean onRing(int Dx, int Dz, double Inner, double Outer) {
        double Distance = Dx * Dx + Dz * Dz;
        return Distance <= Outer * Outer + 0.5 && Distance > Inner * Inner + 0.5;
    }

    public void ground(int X1, int Z1, int X2, int Z2, RuinMix Mix, double Coverage, int Salt) {
        for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
            for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
                if (!Plan.has(X, 0, Z) && Random.smooth(X, Z, 3.5, Salt) * 0.82 + Random.at(X, 0, Z, Salt) * 0.18 < Coverage) {
                    put(X, 0, Z, Mix);
                }
            }
        }
    }

    public void scorchGround(int Cx, int Cz, double Radius, double Coverage, int Salt) {
        int Reach = (int) Math.ceil(Radius);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz) / Math.max(1.0, Radius);
                if (Distance > 1.0 || Plan.has(Cx + Dx, 0, Cz + Dz)) {
                    continue;
                }
                double Noise = Random.smooth(Cx + Dx, Cz + Dz, 3.2, Salt) * 0.82 + Random.at(Cx + Dx, 0, Cz + Dz, Salt) * 0.18;
                if (Noise < Coverage * (1.15 - Distance * 0.6)) {
                    put(Cx + Dx, 0, Cz + Dz, RuinMix.Ash);
                }
            }
        }
    }

    public void stairs(int X, int Y, int Z, RuinMat Mat, int Facing, boolean Top) {
        Plan.set(X, Y, Z, Mat, Facing, Top ? 1 : 0);
    }

    public void protect(int X, int Y, int Z) {
        for (int Below = Y; Below >= Math.min(Y, 0); Below--) {
            if (!Plan.filled(X, Below, Z)) {
                break;
            }
            Plan.keep(X, Below, Z);
        }
    }

    public void slab(int X, int Y, int Z, RuinMat Mat, boolean Top) {
        Plan.set(X, Y, Z, Mat, RuinMat.South, Top ? 1 : 0);
    }

    public void pillar(int X, int Y1, int Y2, int Z, RuinMat Mat) {
        for (int Y = Y1; Y <= Y2; Y++) {
            Plan.set(X, Y, Z, Mat, RuinMat.Up, 0);
        }
    }

    public void pillar(int X, int Y1, int Y2, int Z, RuinMix Mix) {
        for (int Y = Y1; Y <= Y2; Y++) {
            put(X, Y, Z, Mix);
        }
    }

    public void column(int X, int Y, int Z, int Height, boolean Brass) {
        put(X, Y, Z, RuinMix.Plinth);
        for (int Level = 1; Level < Height - 1; Level++) {
            Plan.set(X, Y + Level, Z, RuinMat.RuinColumn, RuinMat.Up, 0);
        }
        if (Height >= 2) {
            put(X, Y + Height - 1, Z, Brass ? RuinMix.Brass.at(Random, X, Y + Height - 1, Z) : RuinMat.ChiseledRedSandstone);
        }
    }

    public void brazier(int X, int Y, int Z, boolean Lit) {
        Plan.set(X, Y, Z, RuinMat.RuinBrazier, RuinMat.South, Lit ? 1 : 0);
    }

    public void pedestalBrazier(int X, int Y, int Z, boolean Lit) {
        put(X, Y, Z, RuinMat.PolishedBasalt, RuinMat.Up);
        brazier(X, Y + 1, Z, Lit);
    }

    public void greatBrazier(int Cx, int Y, int Cz, boolean Lit, boolean Gilded) {
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                boolean Edge = Math.abs(Dx) + Math.abs(Dz) == 2;
                if (Edge) {
                    put(Cx + Dx, Y, Cz + Dz, RuinMat.PolishedBlackstoneStairs, Dx == 0 ? (Dz < 0 ? RuinMat.South : RuinMat.North) : Dx < 0 ? RuinMat.East : RuinMat.West,
                            1);
                } else if (Dx == 0 && Dz == 0) {
                    put(Cx, Y, Cz, RuinMat.PolishedBasalt, RuinMat.Up);
                } else {
                    stairs(Cx + Dx, Y, Cz + Dz, RuinMat.BlackstoneBrickStairs, Dx == 0 ? (Dz < 0 ? RuinMat.South : RuinMat.North) : Dx < 0 ? RuinMat.East : RuinMat.West, true);
                }
            }
        }
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                boolean Rim = Dx != 0 || Dz != 0;
                if (Rim) {
                    put(Cx + Dx, Y + 1, Cz + Dz, Gilded && (Dx == 0 || Dz == 0) ? RuinMat.GildedBlackstone : RuinMix.Brass.at(Random, Cx + Dx, Y + 1, Cz + Dz));
                } else {
                    put(Cx, Y + 1, Cz, Lit ? RuinMat.Netherrack : RuinMat.CoalBlock);
                }
            }
        }
        if (Lit) {
            put(Cx, Y + 2, Cz, RuinMat.Fire);
        } else {
            air(Cx, Y + 2, Cz);
        }
    }

    public void banner(int X, int Y, int Z, int Facing) {
        Plan.set(X, Y, Z, RuinMat.RuinBanner, Facing, 0);
    }

    public void lantern(int X, int Y, int Z, boolean Hanging) {
        Plan.set(X, Y, Z, RuinMat.Lantern, RuinMat.South, Hanging ? 1 : 0);
    }

    public void hangingLantern(int X, int Y, int Z, int Chain) {
        for (int Link = 0; Link < Chain; Link++) {
            Plan.set(X, Y - Link, Z, RuinMat.Chain, RuinMat.Up, 0);
        }
        lantern(X, Y - Chain, Z, true);
    }

    public void candles(int X, int Y, int Z, int Count) {
        Plan.set(X, Y, Z, roll(X, Y, Z, 31) < 0.7 ? RuinMat.RedCandle : RuinMat.BlackCandle, RuinMat.South, Math.max(0, Math.min(3, Count - 1)));
    }

    public void chest(int X, int Y, int Z, int Facing, int Loot) {
        Plan.set(X, Y, Z, RuinMat.Chest, Facing, Loot);
        Plan.keep(X, Y, Z);
        Plan.keep(X, Y - 1, Z);
        keepClear(X, Y + 1, Z);
    }

    public void trappedChest(int X, int Y, int Z, int Facing, int Loot, int Group) {
        Plan.set(X, Y, Z, RuinMat.TrappedChest, Facing, Loot);
        Plan.keep(X, Y, Z);
        Plan.keep(X, Y - 1, Z);
        keepClear(X, Y + 1, Z);
        Plan.mark(RuinDanger.TrapChest, X, Y, Z, Facing, Group);
    }

    public void barrel(int X, int Y, int Z, int Facing, int Loot) {
        Plan.set(X, Y, Z, RuinMat.Barrel, Facing, Loot);
        Plan.keep(X, Y, Z);
    }

    public void pot(int X, int Y, int Z, int Facing, int Loot) {
        Plan.set(X, Y, Z, RuinMat.DecoratedPot, Facing, Loot);
        Plan.keep(X, Y, Z);
    }

    public void brush(int X, int Y, int Z, boolean Gravel) {
        Plan.set(X, Y, Z, Gravel ? RuinMat.SuspiciousGravel : RuinMat.SuspiciousSand, RuinMat.South, 0);
        Plan.keep(X, Y, Z);
    }

    public void spawner(int X, int Y, int Z, int Kind) {
        Plan.set(X, Y, Z, RuinMat.Spawner, RuinMat.South, Kind);
        Plan.keep(X, Y, Z);
        Plan.keep(X, Y - 1, Z);
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                if (Dx != 0 || Dz != 0) {
                    keepClear(X + Dx, Y, Z + Dz);
                    keepClear(X + Dx, Y + 1, Z + Dz);
                }
            }
        }
    }

    public void guardian(int X, int Y, int Z, int Facing, int Kind) {
        Plan.mark(RuinPlan.Guardian, X, Y, Z, Facing, Kind);
        keepClear(X, Y, Z);
        keepClear(X, Y + 1, Z);
    }

    public void keepClear(int X, int Y, int Z) {
        Plan.keepClear(X, Y, Z);
    }

    public void keep(int X, int Y, int Z) {
        Plan.keep(X, Y, Z);
    }

    public void keepClearBox(int X1, int Y1, int Z1, int X2, int Y2, int Z2) {
        for (int Y = Math.min(Y1, Y2); Y <= Math.max(Y1, Y2); Y++) {
            for (int Z = Math.min(Z1, Z2); Z <= Math.max(Z1, Z2); Z++) {
                for (int X = Math.min(X1, X2); X <= Math.max(X1, X2); X++) {
                    keepClear(X, Y, Z);
                }
            }
        }
    }

    public void rubble(int Cx, int Cz, double Radius, int Height, int Salt) {
        int Reach = (int) Math.ceil(Radius);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                if (Distance > Radius) {
                    continue;
                }
                int X = Cx + Dx;
                int Z = Cz + Dz;
                double Rise = (1.0 - Distance / (Radius + 0.5)) * Height + Random.at(X, 1, Z, Salt) * 1.2 - 0.4;
                int Top = (int) Math.floor(Rise);
                for (int Y = 1; Y <= Top; Y++) {
                    if (!Plan.filled(X, Y, Z) && !Plan.mustStayClear(X, Y, Z)) {
                        put(X, Y, Z, RuinMix.Rubble.at(Random, X, Y, Z).shape() == RuinMat.Shape.Slab && Y < Top ? RuinMat.RedSandstone : RuinMix.Rubble.at(Random, X, Y, Z));
                    }
                }
                if (Top < 1 && Random.at(X, 2, Z, Salt) < 0.35 && !Plan.filled(X, 1, Z) && !Plan.mustStayClear(X, 1, Z)) {
                    slab(X, 1, Z, Random.at(X, 3, Z, Salt) < 0.5 ? RuinMat.RedSandstoneSlab : RuinMat.CutRedSandstoneSlab, false);
                }
            }
        }
    }

    public void crater(int Cx, int Cz, double Radius, int Salt) {
        int Reach = (int) Math.ceil(Radius) + 1;
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                int X = Cx + Dx;
                int Z = Cz + Dz;
                if (Distance <= Radius * 0.55) {
                    air(X, 0, Z);
                    put(X, -1, Z, RuinMix.Scorch);
                    if (Distance <= Radius * 0.25 && Radius > 2) {
                        air(X, -1, Z);
                        put(X, -2, Z, Random.at(X, -2, Z, Salt) < 0.35 ? RuinMat.Magma : RuinMat.Blackstone);
                    }
                } else if (Distance <= Radius) {
                    put(X, 0, Z, RuinMix.Ash);
                } else if (Distance <= Radius + 1.2 && Random.at(X, 1, Z, Salt) < 0.4) {
                    put(X, 0, Z, RuinMix.Ash);
                }
            }
        }
    }

    public void spear(int X, int Y, int Z, int Length, boolean Tipped) {
        for (int Level = 0; Level < Length; Level++) {
            put(X, Y + Level, Z, RuinMat.DarkOakFence);
        }
        if (Tipped) {
            Plan.set(X, Y + Length, Z, RuinMat.LightningRod, RuinMat.Up, 0);
        }
    }

    public void growth(int X, int Y, int Z) {
        if (!Plan.filled(X, Y, Z) && !Plan.mustStayClear(X, Y, Z)) {
            put(X, Y, Z, RuinMat.Growth);
        }
    }

    public void vine(int X, int Y, int Z, int Facing) {
        if (!Plan.filled(X, Y, Z) && !Plan.mustStayClear(X, Y, Z)) {
            Plan.set(X, Y, Z, RuinMat.Vine, Facing, 0);
        }
    }

    public void entrance(int X, int Y, int Z, int Facing) {
        Plan.mark(RuinPlan.Entrance, X, Y, Z, Facing, 0);
        keepClear(X, Y, Z);
        keepClear(X, Y + 1, Z);
    }

    public void centre(int X, int Y, int Z) {
        Plan.mark(RuinPlan.Centre, X, Y, Z, 0, 0);
    }

    public static int faceToward(int Dx, int Dz) {
        if (Math.abs(Dx) >= Math.abs(Dz)) {
            return Dx > 0 ? RuinMat.East : RuinMat.West;
        }
        return Dz > 0 ? RuinMat.South : RuinMat.North;
    }

    public static int rotateCw(int Facing) {
        return switch (Facing) {
            case RuinMat.North -> RuinMat.East;
            case RuinMat.East -> RuinMat.South;
            case RuinMat.South -> RuinMat.West;
            case RuinMat.West -> RuinMat.North;
            default -> Facing;
        };
    }
}
