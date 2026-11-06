package net.rai.scp_unleashed.worldend;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class WorldEndConfig {
    public static ModConfigSpec.BooleanValue AllowWorldDeletion;
    private static int OverrideDeletion = -1;

    public static void define(ModConfigSpec.Builder Builder) {
        AllowWorldDeletion = Builder.comment("Safety switch for world endings. When false, no anomaly may delete the world folder or stop the server, even while the scpWorldEndings game rule is on: the ending plays and then releases everyone back into the world.")
                .translation("config.scp_unleashed.worldend.allow_world_deletion").define("allowWorldDeletion", true);
    }

    private WorldEndConfig() {
    }

    public static boolean allowDeletion() {
        if (OverrideDeletion >= 0) {
            return OverrideDeletion == 1;
        }
        return !ScpConfigs.Server.isLoaded() || AllowWorldDeletion.get();
    }

    public static void overrideForTests(int Value) {
        OverrideDeletion = Value;
    }
}
