package net.rai.scp_unleashed.scp001day;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class Scp001DayClientConfig {
    public static final String Section = "scp001_day";
    public static ModConfigSpec.BooleanValue SunShafts;

    public static void define(ModConfigSpec.Builder Builder) {
        SunShafts = Builder.comment("Draw the sun shafts and sunlit haze in SCP-001 When Day Breaks. Turn off if the game stutters or crashes there by day.")
                .translation("config.scp_unleashed.scp001_day.sun_shafts").define("sunShafts", true);
    }

    private Scp001DayClientConfig() {
    }

    public static boolean sunShafts() {
        return !ScpConfigs.Client.isLoaded() || SunShafts == null || SunShafts.get();
    }
}
