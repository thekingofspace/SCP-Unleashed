package net.rai.scp_unleashed.daeviteruins.plan;

public final class RuinMix {
    public static final RuinMix Dressed = of(RuinMat.RuinBrick, 76, RuinMat.RedNetherBricks, 10, RuinMat.RedTerracotta, 8, RuinMat.CutRedSandstone, 6);
    public static final RuinMix Trim = of(RuinMat.CutRedSandstone, 55, RuinMat.SmoothRedSandstone, 30, RuinMat.RedSandstone, 15);
    public static final RuinMix Rough = of(RuinMat.RuinBrick, 30, RuinMat.RedTerracotta, 20, RuinMat.Granite, 14, RuinMat.RedSandstone, 14, RuinMat.Terracotta, 10,
            RuinMat.Blackstone, 12);
    public static final RuinMix Plinth = of(RuinMat.SmoothBasalt, 40, RuinMat.Blackstone, 20, RuinMat.PolishedBlackstone, 16, RuinMat.Basalt, 12, RuinMat.BlackstoneBricks, 12);
    public static final RuinMix DarkBrick = of(RuinMat.BlackstoneBricks, 50, RuinMat.CrackedBlackstoneBricks, 28, RuinMat.PolishedBlackstone, 10, RuinMat.GildedBlackstone, 4,
            RuinMat.Blackstone, 8);
    public static final RuinMix Crimson = of(RuinMat.RedNetherBricks, 68, RuinMat.NetherBricks, 10, RuinMat.CrackedNetherBricks, 10, RuinMat.RedTerracotta, 12);
    public static final RuinMix Floor = of(RuinMat.SmoothBasalt, 22, RuinMat.PolishedBlackstone, 18, RuinMat.CutRedSandstone, 20, RuinMat.SmoothRedSandstone, 15,
            RuinMat.RedTerracotta, 10, RuinMat.PolishedGranite, 15);
    public static final RuinMix Paving = of(RuinMat.PolishedBlackstone, 25, RuinMat.SmoothBasalt, 25, RuinMat.BlackstoneBricks, 20, RuinMat.CrackedBlackstoneBricks, 10,
            RuinMat.Gravel, 10, RuinMat.CoarseDirt, 10);
    public static final RuinMix Brass = of(RuinMat.GildedBlackstone, 40, RuinMat.AgedCopper, 34, RuinMat.RawGoldBlock, 14, RuinMat.ChiseledCopper, 12);
    public static final RuinMix Gold = of(RuinMat.GildedBlackstone, 50, RuinMat.RawGoldBlock, 30, RuinMat.GoldBlock, 20);
    public static final RuinMix Scorch = of(RuinMat.NetherBricks, 40, RuinMat.CrackedNetherBricks, 30, RuinMat.Blackstone, 15, RuinMat.Basalt, 15);
    public static final RuinMix Ash = of(RuinMat.CoarseDirt, 34, RuinMat.Gravel, 20, RuinMat.AshPowder, 14, RuinMat.SootPowder, 8, RuinMat.Tuff, 10, RuinMat.Dirt, 14);
    public static final RuinMix Timber = of(RuinMat.StrippedDarkOakLog, 40, RuinMat.DarkOakLog, 30, RuinMat.StrippedCrimsonStem, 20, RuinMat.CrimsonStem, 10);
    public static final RuinMix Cloth = of(RuinMat.RedWool, 84, RuinMat.NetherWartBlock, 16);
    public static final RuinMix Rubble = of(RuinMat.RuinBrick, 16, RuinMat.RedTerracotta, 10, RuinMat.Blackstone, 14, RuinMat.Gravel, 18, RuinMat.CoarseDirt, 14,
            RuinMat.BlackstoneSlab, 10, RuinMat.RedNetherBrickSlab, 8, RuinMat.Granite, 10);
    public static final RuinMix WhiteCity = of(RuinMat.Calcite, 50, RuinMat.PolishedDiorite, 30, RuinMat.SmoothQuartz, 20);
    public static final RuinMix Earth = of(RuinMat.CoarseDirt, 40, RuinMat.Dirt, 30, RuinMat.PackedMud, 15, RuinMat.Gravel, 15);

    private final RuinMat[] Mats;
    private final int[] Weights;
    private final int Total;

    private RuinMix(RuinMat[] Mats, int[] Weights) {
        this.Mats = Mats;
        this.Weights = Weights;
        int Sum = 0;
        for (int Weight : Weights) {
            Sum += Weight;
        }
        this.Total = Sum;
    }

    public static RuinMix of(Object... Pairs) {
        int Count = Pairs.length / 2;
        RuinMat[] Mats = new RuinMat[Count];
        int[] Weights = new int[Count];
        for (int Index = 0; Index < Count; Index++) {
            Mats[Index] = (RuinMat) Pairs[Index * 2];
            Weights[Index] = (Integer) Pairs[Index * 2 + 1];
        }
        return new RuinMix(Mats, Weights);
    }

    public static RuinMix single(RuinMat Mat) {
        return new RuinMix(new RuinMat[]{Mat}, new int[]{1});
    }

    public RuinMat pick(double Roll) {
        double Target = Roll * Total;
        for (int Index = 0; Index < Mats.length; Index++) {
            Target -= Weights[Index];
            if (Target < 0) {
                return Mats[Index];
            }
        }
        return Mats[Mats.length - 1];
    }

    public RuinMat at(RuinRandom Random, int X, int Y, int Z) {
        if (Random.at(X, Y, Z, 5) < 0.68) {
            return pick(Random.at(Math.floorDiv(X, 2), Math.floorDiv(Y, 2), Math.floorDiv(Z, 2), 9));
        }
        return pick(Random.at(X, Y, Z, 7));
    }

    public RuinMat patch(RuinRandom Random, int X, int Y, int Z, int Size) {
        return pick(Random.at(Math.floorDiv(X, Size), Math.floorDiv(Y, Size), Math.floorDiv(Z, Size), 11));
    }

    public RuinMat first() {
        return Mats[0];
    }
}
