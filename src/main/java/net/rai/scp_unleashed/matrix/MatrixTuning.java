package net.rai.scp_unleashed.matrix;

import net.rai.scp_unleashed.anomaly.FragmentRarity;

public final class MatrixTuning {
    public static final int FieldRadius = 32;
    public static final int MinRange = 8;
    public static final int VerticalReach = 20;
    public static final double PlayerClearance = 6.0;
    public static final int WarmupTicks = 600;
    public static final int DecayTicks = 1200;
    public static final int PulseTicks = 20;
    public static final int CensusTicks = 40;
    public static final int Attempts = 16;
    public static final int ScopeReach = 40;
    public static final int HorizonSeconds = 7200;
    private static final int[] Draws = {150, 400, 1000, 2500, 6000};
    private static final int[] MeanSeconds = {150, 180, 210, 255, 300};

    private MatrixTuning() {
    }

    public static int drawFor(FragmentRarity Tier) {
        return Draws[Tier.getTier() - 1];
    }

    public static int meanSecondsFor(FragmentRarity Tier) {
        return MeanSeconds[Tier.getTier() - 1];
    }

    public static double chancePerPulse(FragmentRarity Tier, double Ratio, double Resonance) {
        return Math.max(0.0, Math.min(1.0, Ratio * Resonance * PulseTicks / (20.0 * meanSecondsFor(Tier))));
    }

    public static double oddsPerMinute(FragmentRarity Tier, double Ratio, double Resonance) {
        double Chance = chancePerPulse(Tier, Ratio, Resonance);
        return 1.0 - Math.pow(1.0 - Chance, 1200.0 / PulseTicks);
    }

    public static int expectedSeconds(FragmentRarity Tier, double Ratio, double Resonance) {
        if (Ratio <= 0.0) {
            return -1;
        }
        double Survival = 1.0;
        double Expected = 0.0;
        double Charge = Math.max(0.0, Math.min(1.0, Resonance));
        double Growth = Ratio * PulseTicks / WarmupTicks;
        double Step = PulseTicks / 20.0;
        for (int Second = 0; Second < HorizonSeconds && Survival > 0.0005; Second++) {
            Expected += Survival * Step;
            Survival *= 1.0 - chancePerPulse(Tier, Ratio, Charge);
            Charge = Math.min(1.0, Charge + Growth);
        }
        double Last = chancePerPulse(Tier, Ratio, Charge);
        if (Survival > 0.0005 && Last > 0.0) {
            Expected += Survival * Step / Last;
        }
        return (int) Math.round(Expected);
    }

    public static int fullPowerSeconds(FragmentRarity Tier) {
        return expectedSeconds(Tier, 1.0, 0.0);
    }

    public static String clock(int Seconds) {
        if (Seconds < 0) {
            return "--:--";
        }
        int Clamped = Math.min(Seconds, 99 * 60 + 59);
        return String.format("%d:%02d", Clamped / 60, Clamped % 60);
    }

    public static String grouped(int Value) {
        return String.format("%,d", Value);
    }
}
