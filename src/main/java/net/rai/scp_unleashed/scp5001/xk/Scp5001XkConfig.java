package net.rai.scp_unleashed.scp5001.xk;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class Scp5001XkConfig {
    public static final double DefaultRiseChance = 0.35;
    public static final int DefaultFirstPhaseTicks = 24000;
    public static final int DefaultSkyDays = 3;
    public static final int DefaultChunkCap = 4;
    public static ModConfigSpec.DoubleValue RiseChance;
    public static ModConfigSpec.IntValue FirstPhaseTicks;
    public static ModConfigSpec.IntValue SkyDays;
    public static ModConfigSpec.IntValue ChunkCap;
    private static double OverrideChance = -1.0;
    private static int OverrideFirst = -1;
    private static long OverrideSky = -1L;
    private static int OverrideCap = -1;

    public static void define(ModConfigSpec.Builder Builder) {
        RiseChance = Builder.comment("Chance that a mob killed during the first XK phase rises again as an SCP-610 form.")
                .translation("config.scp_unleashed.scp5001_xk.rise_chance").defineInRange("riseChance", DefaultRiseChance, 0.0, 1.0);
        FirstPhaseTicks = Builder.comment("Length of the first XK phase in game ticks before the second phase begins.")
                .translation("config.scp_unleashed.scp5001_xk.first_phase_ticks").defineInRange("firstPhaseTicks", DefaultFirstPhaseTicks, 200, 2400000);
        SkyDays = Builder.comment("In-game days of the second XK phase before the end arrives.")
                .translation("config.scp_unleashed.scp5001_xk.sky_days").defineInRange("skyDays", DefaultSkyDays, 1, 30);
        ChunkCap = Builder.comment("Most SCP-610 forms per chunk that kills may raise during the first XK phase.")
                .translation("config.scp_unleashed.scp5001_xk.chunk_cap").defineInRange("chunkCap", DefaultChunkCap, 0, 64);
    }

    private Scp5001XkConfig() {
    }

    public static double riseChance() {
        return OverrideChance >= 0.0 ? OverrideChance : ScpConfigs.Server.isLoaded() ? RiseChance.get() : DefaultRiseChance;
    }

    public static int firstPhaseTicks() {
        return OverrideFirst >= 0 ? OverrideFirst : ScpConfigs.Server.isLoaded() ? FirstPhaseTicks.get() : DefaultFirstPhaseTicks;
    }

    public static long skyTicks() {
        if (OverrideSky >= 0L) {
            return OverrideSky;
        }
        int Days = ScpConfigs.Server.isLoaded() ? SkyDays.get() : DefaultSkyDays;
        return Days * 24000L;
    }

    public static int chunkCap() {
        return OverrideCap >= 0 ? OverrideCap : ScpConfigs.Server.isLoaded() ? ChunkCap.get() : DefaultChunkCap;
    }

    public static void overrideForTests(double Chance, int First, long Sky, int Cap) {
        OverrideChance = Chance;
        OverrideFirst = First;
        OverrideSky = Sky;
        OverrideCap = Cap;
    }

    public static void clearOverrides() {
        overrideForTests(-1.0, -1, -1L, -1);
    }
}
