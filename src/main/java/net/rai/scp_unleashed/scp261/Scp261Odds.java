package net.rai.scp_unleashed.scp261;

import net.minecraft.util.RandomSource;

public final class Scp261Odds {
    public static final float IronSwallow = 0.04f;
    public static final float GoldSwallow = 0.02f;
    public static final float IronDouble = 0.04f;
    public static final float GoldDouble = 0.10f;

    private Scp261Odds() {
    }

    public static int totalWeight(boolean Gold) {
        int Total = 0;
        for (Scp261Snack Treat : Scp261Snack.values()) {
            Total += Treat.getTier().weight(Gold);
        }
        return Total;
    }

    public static Scp261Snack pick(RandomSource Random, boolean Gold) {
        int Ticket = Random.nextInt(totalWeight(Gold));
        for (Scp261Snack Treat : Scp261Snack.values()) {
            Ticket -= Treat.getTier().weight(Gold);
            if (Ticket < 0) {
                return Treat;
            }
        }
        return Scp261Snack.GravityGum;
    }

    public static Roll roll(RandomSource Random, boolean Gold) {
        float Luck = Random.nextFloat();
        float Swallow = Gold ? GoldSwallow : IronSwallow;
        float Twice = Gold ? GoldDouble : IronDouble;
        Scp261Snack First = pick(Random, Gold);
        if (Luck < Swallow) {
            return new Roll(Outcome.Swallowed, First, null);
        }
        if (Luck < Swallow + Twice) {
            Scp261Snack Second = pick(Random, Gold);
            for (int Retry = 0; Retry < 4 && Second == First; Retry++) {
                Second = pick(Random, Gold);
            }
            return new Roll(Outcome.Pair, First, Second == First ? null : Second);
        }
        return new Roll(Outcome.Single, First, null);
    }

    public enum Outcome {
        Single,
        Pair,
        Swallowed
    }

    public record Roll(Outcome Result, Scp261Snack First, Scp261Snack Second) {
        public int count() {
            return switch (Result) {
                case Swallowed -> 0;
                case Single -> 1;
                case Pair -> 2;
            };
        }
    }
}
