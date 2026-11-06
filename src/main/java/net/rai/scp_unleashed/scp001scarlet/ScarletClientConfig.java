package net.rai.scp_unleashed.scp001scarlet;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class ScarletClientConfig {
    public static ModConfigSpec.BooleanValue ShowMeter;
    public static ModConfigSpec.BooleanValue ScarletSky;

    public static void define(ModConfigSpec.Builder Builder) {
        ShowMeter = Builder.translation("config.scp_unleashed.scarlet.show_meter").define("showManifestationMeter", true);
        ScarletSky = Builder.translation("config.scp_unleashed.scarlet.sky").define("scarletSky", true);
    }

    private ScarletClientConfig() {
    }

    public static boolean showMeter() {
        return !ScpConfigs.Client.isLoaded() || ShowMeter.get();
    }

    public static boolean sky() {
        return !ScpConfigs.Client.isLoaded() || ScarletSky.get();
    }
}
