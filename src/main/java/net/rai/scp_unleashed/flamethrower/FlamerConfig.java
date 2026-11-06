package net.rai.scp_unleashed.flamethrower;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class FlamerConfig {
    public static ModConfigSpec.EnumValue<FireRule> WorldFire;
    public static ModConfigSpec.BooleanValue BurnFlesh;

    public enum FireRule {
        Flint,
        MobGriefing,
        Off
    }

    public static void define(ModConfigSpec.Builder Builder) {
        WorldFire = Builder.comment("How flamethrower flames treat blocks. Flint: a player's flamethrower places fire like flint and steel, any other user needs the mobGriefing game rule. MobGriefing: every flamethrower needs mobGriefing. Off: flamethrowers never place fire or burn blocks.")
                .translation("config.scp_unleashed.flamethrower.world_fire").defineEnum("worldFire", FireRule.Flint);
        BurnFlesh = Builder.comment("Whether flamethrower flames burn away flame burnable flesh blocks, under the same rule as worldFire.")
                .translation("config.scp_unleashed.flamethrower.burn_flesh").define("burnFlesh", true);
    }

    private FlamerConfig() {
    }

    public static FireRule worldFire() {
        return ScpConfigs.Server.isLoaded() ? WorldFire.get() : WorldFire.getDefault();
    }

    public static boolean burnFlesh() {
        return ScpConfigs.Server.isLoaded() ? BurnFlesh.getAsBoolean() : BurnFlesh.getDefault();
    }
}
