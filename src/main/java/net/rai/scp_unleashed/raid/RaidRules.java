package net.rai.scp_unleashed.raid;

import net.minecraft.world.Difficulty;

public final class RaidRules {
    public static final int TickEvery = 20;
    public static final int PlanEvery = 600;
    public static final int PruneEvery = 6000;
    public static final int MaxActive = 2;
    public static final int GlobalRaiders = 18;
    public static final int MaxAlive = 8;
    public static final double MinConfidence = 0.2;
    public static final double PlanChance = 0.35;
    public static final double HomeReach = 80.0;
    public static final double HomeDepth = 40.0;
    public static final double SiteClearance = 96.0;
    public static final double PresenceReach = 128.0;
    public static final long AbandonTicks = 1200L;
    public static final long RetryTicks = 2400L;
    public static final double FirstDelayShare = 0.5;
    public static final double DefeatedStretch = 1.25;
    public static final double Jitter = 0.15;
    public static final int NoticeRange = 144;
    public static final double SpotReach = 48.0;
    public static final double SpotPull = 0.5;
    public static final int IntelReach = 64;
    public static final int MaxIntel = 16;
    public static final int MaxSpoils = 12;

    private RaidRules() {
    }

    public static double cooldownScale(Difficulty Level) {
        return switch (Level) {
            case EASY -> 1.5;
            case HARD -> 0.7;
            default -> 1.0;
        };
    }
}
