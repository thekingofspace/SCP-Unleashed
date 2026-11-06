package net.rai.scp_unleashed.scp1730.guardian;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class Scp1730GuardianConfig {
    public static ModConfigSpec.DoubleValue CaptureShare;
    public static ModConfigSpec.DoubleValue WindowShare;
    public static ModConfigSpec.IntValue EscapeTicks;

    public static void define(ModConfigSpec.Builder Builder) {
        CaptureShare = Builder.comment("Health share at or below which SCP-1730-2 can be recovered.")
                .translation("config.scp_unleashed.scp1730_guardian.capture_share").defineInRange("captureShare", 0.10, 0.01, 0.5);
        WindowShare = Builder.comment("The least health share one blow can leave SCP-1730-2 with when it lands from above the recovery share.")
                .translation("config.scp_unleashed.scp1730_guardian.window_share").defineInRange("windowShare", 0.05, 0.0, 0.5);
        EscapeTicks = Builder.comment("Ticks the recovered SCP-1730-2 needs to slip away once nothing holds it.")
                .translation("config.scp_unleashed.scp1730_guardian.escape_ticks").defineInRange("escapeTicks", 2, 1, 200);
    }

    private Scp1730GuardianConfig() {
    }

    public static double captureShare() {
        return ScpConfigs.Server.isLoaded() ? CaptureShare.get() : CaptureShare.getDefault();
    }

    public static double windowShare() {
        return ScpConfigs.Server.isLoaded() ? WindowShare.get() : WindowShare.getDefault();
    }

    public static int escapeTicks() {
        return ScpConfigs.Server.isLoaded() ? EscapeTicks.get() : EscapeTicks.getDefault();
    }
}
