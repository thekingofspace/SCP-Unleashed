package net.rai.scp_unleashed.scp2317;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class Scp2317Config {
    public static ModConfigSpec.IntValue RitualWindow;
    public static ModConfigSpec.DoubleValue OfferingReach;

    public static void define(ModConfigSpec.Builder Builder) {
        RitualWindow = Builder.comment("Ticks allowed between the offering and the end of the rite.")
                .translation("config.scp_unleashed.scarlet_2317.ritual_window").defineInRange("ritualWindow", 2400, 200, 72000);
        OfferingReach = Builder.comment("How close to the vase the offering must fall, in blocks.")
                .translation("config.scp_unleashed.scarlet_2317.offering_reach").defineInRange("offeringReach", 4.0, 1.0, 16.0);
    }

    private Scp2317Config() {
    }

    public static int window() {
        return ScpConfigs.Server.isLoaded() ? RitualWindow.get() : RitualWindow.getDefault();
    }

    public static double reach() {
        return ScpConfigs.Server.isLoaded() ? OfferingReach.get() : OfferingReach.getDefault();
    }
}
