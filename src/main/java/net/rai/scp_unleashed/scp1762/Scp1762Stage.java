package net.rai.scp_unleashed.scp1762;

import net.minecraft.util.RandomSource;

public final class Scp1762Stage {
    public static final int First = 0;
    public static final int Thanks = 1;
    public static final int Goodbye = 2;
    public static final int Empty = 3;
    public static final int Last = Empty;
    public static final int FirstMin = 24;
    public static final int FirstMax = 30;
    public static final int ThanksMin = 14;
    public static final int ThanksMax = 16;
    public static final int GoodbyeCount = 7;

    private Scp1762Stage() {
    }

    public static int count(int Stage, RandomSource Random) {
        return switch (Stage) {
            case First -> FirstMin + Random.nextInt(FirstMax - FirstMin + 1);
            case Thanks -> ThanksMin + Random.nextInt(ThanksMax - ThanksMin + 1);
            case Goodbye -> GoodbyeCount;
            default -> 0;
        };
    }

    public static int afterRecall(int Stage) {
        return Math.min(Empty, Stage + 1);
    }

    public static int afterReturn(int Stage) {
        return Stage == Thanks ? Goodbye : Stage;
    }

    public static boolean recallsAtEnd(int Stage) {
        return Stage == Goodbye;
    }

    public static int note(int Stage) {
        return Stage == Thanks ? 0 : Stage == Goodbye ? 1 : -1;
    }

    public static boolean free(int Stage) {
        return Stage >= Empty;
    }
}
