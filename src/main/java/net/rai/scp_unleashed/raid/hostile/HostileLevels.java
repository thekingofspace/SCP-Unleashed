package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;

public final class HostileLevels {
    public static final int MaxLevel = 6;
    public static final double[] Thresholds = {0.0, 100.0, 250.0, 450.0, 700.0, 1000.0};
    public static final double MaxXp = 2000.0;
    public static final int[] KeycardTiers = {0, 1, 2, 3, 4, 5};
    public static final double[] ExtraArmour = {0.0, 0.0, 1.0, 2.0, 3.0, 4.0};
    public static final float[] Accuracy = {0.25f, 0.38f, 0.52f, 0.64f, 0.76f, 0.88f};
    public static final int[] BurstMin = {2, 2, 3, 4, 5, 6};
    public static final int[] BurstMax = {3, 4, 5, 6, 8, 10};
    public static final int[] PauseMin = {50, 40, 30, 20, 12, 6};
    public static final int[] PauseMax = {80, 60, 45, 35, 22, 12};
    public static final int[] ReactionTicks = {26, 22, 18, 14, 10, 7};
    public static final int StrafeLevel = 3;
    public static final int KiteLevel = 4;

    private HostileLevels() {
    }

    public static int index(int Level) {
        return Mth.clamp(Level - 1, 0, MaxLevel - 1);
    }

    public static int level(double Xp) {
        int Found = 1;
        for (int Index = 0; Index < Thresholds.length; Index++) {
            if (Xp >= Thresholds[Index]) {
                Found = Index + 1;
            }
        }
        return Mth.clamp(Found, 1, MaxLevel);
    }

    public static double threshold(int Level) {
        return Thresholds[index(Level)];
    }

    public static double toNext(double Xp) {
        int Level = level(Xp);
        if (Level >= MaxLevel) {
            return 0.0;
        }
        return Math.max(0.0, threshold(Level + 1) - Xp);
    }

    public static int keycardTier(int Level) {
        return KeycardTiers[index(Level)];
    }

    public static double extraArmour(int Level) {
        return ExtraArmour[index(Level)];
    }

    public static float accuracy(int Level) {
        return Accuracy[index(Level)];
    }

    public static int reactionTicks(int Level) {
        return ReactionTicks[index(Level)];
    }

    public static int burst(int Level, RandomSource Random) {
        return Mth.nextInt(Random, BurstMin[index(Level)], BurstMax[index(Level)]);
    }

    public static int pause(int Level, RandomSource Random) {
        return Mth.nextInt(Random, PauseMin[index(Level)], PauseMax[index(Level)]);
    }

    public static int pick(int[] Table, int Level) {
        return Table[index(Level)];
    }

    public static double pick(double[] Table, int Level) {
        return Table[index(Level)];
    }

    public static int squad(int[] Table, int Level, Difficulty Hardness) {
        int Size = pick(Table, Level);
        if (Hardness == Difficulty.EASY) {
            Size--;
        } else if (Hardness == Difficulty.HARD) {
            Size++;
        }
        return Mth.clamp(Size, 2, 8);
    }
}
