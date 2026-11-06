package net.rai.scp_unleashed.scp001scarlet.king;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class ScarletKingConfig {
    public static ModConfigSpec.DoubleValue BaseHealth;
    public static ModConfigSpec.DoubleValue ExtraPlayerShare;
    public static ModConfigSpec.DoubleValue HitCapShare;
    public static ModConfigSpec.IntValue PlayerRadius;
    public static ModConfigSpec.IntValue KneelSeconds;
    public static ModConfigSpec.DoubleValue KneelDamage;
    public static ModConfigSpec.IntValue EnrageMinutes;
    public static ModConfigSpec.IntValue ResetSeconds;
    public static ModConfigSpec.IntValue PodTicks;
    public static ModConfigSpec.BooleanValue ArenaChanges;
    public static ModConfigSpec.IntValue RiteRadius;

    public static void define(ModConfigSpec.Builder Builder) {
        String Prefix = "config.scp_unleashed.scarlet_king.";
        BaseHealth = Builder.comment("The Scarlet King's health for a single player.").translation(Prefix + "base_health").defineInRange("baseHealth", 32000.0, 100.0, 1000000.0);
        ExtraPlayerShare = Builder.comment("Extra health for each additional player near the throne when the fight starts, as a share of the base health.")
                .translation(Prefix + "extra_player_share").defineInRange("extraPlayerShare", 0.6, 0.0, 10.0);
        HitCapShare = Builder.comment("The largest share of the King's maximum health a single hit may remove.").translation(Prefix + "hit_cap_share")
                .defineInRange("hitCapShare", 0.0125, 0.0001, 1.0);
        PlayerRadius = Builder.comment("Players within this many blocks of the throne count toward the fight.").translation(Prefix + "player_radius")
                .defineInRange("playerRadius", 96, 16, 512);
        KneelSeconds = Builder.comment("How long the King kneels after a binding snaps.").translation(Prefix + "kneel_seconds").defineInRange("kneelSeconds", 10, 0, 120);
        KneelDamage = Builder.comment("Damage multiplier while the King kneels.").translation(Prefix + "kneel_damage").defineInRange("kneelDamage", 1.5, 1.0, 10.0);
        EnrageMinutes = Builder.comment("Minutes of fighting before the King grows frenzied.").translation(Prefix + "enrage_minutes").defineInRange("enrageMinutes", 14, 1, 600);
        ResetSeconds = Builder.comment("Seconds with nobody at the throne before the fight resets.").translation(Prefix + "reset_seconds").defineInRange("resetSeconds", 10, 1, 600);
        PodTicks = Builder.comment("Ticks a dormant pod needs to regrow.").translation(Prefix + "pod_ticks").defineInRange("podTicks", 36000, 20, 1000000);
        ArenaChanges = Builder.comment("Whether the fight may crack and topple pieces of the throne pillars.").translation(Prefix + "arena_changes").define("arenaChanges", true);
        RiteRadius = Builder.comment("How close to the throne the words must be spoken.").translation(Prefix + "rite_radius").defineInRange("riteRadius", 14, 2, 128);
    }

    private ScarletKingConfig() {
    }

    public static double baseHealth() {
        return ScpConfigs.Server.isLoaded() ? BaseHealth.get() : BaseHealth.getDefault();
    }

    public static double extraPlayerShare() {
        return ScpConfigs.Server.isLoaded() ? ExtraPlayerShare.get() : ExtraPlayerShare.getDefault();
    }

    public static double hitCapShare() {
        return ScpConfigs.Server.isLoaded() ? HitCapShare.get() : HitCapShare.getDefault();
    }

    public static int playerRadius() {
        return ScpConfigs.Server.isLoaded() ? PlayerRadius.get() : PlayerRadius.getDefault();
    }

    public static int kneelTicks() {
        return (ScpConfigs.Server.isLoaded() ? KneelSeconds.get() : KneelSeconds.getDefault()) * 20;
    }

    public static double kneelDamage() {
        return ScpConfigs.Server.isLoaded() ? KneelDamage.get() : KneelDamage.getDefault();
    }

    public static int enrageTicks() {
        return (ScpConfigs.Server.isLoaded() ? EnrageMinutes.get() : EnrageMinutes.getDefault()) * 1200;
    }

    public static int resetTicks() {
        return (ScpConfigs.Server.isLoaded() ? ResetSeconds.get() : ResetSeconds.getDefault()) * 20;
    }

    public static int podTicks() {
        return ScpConfigs.Server.isLoaded() ? PodTicks.get() : PodTicks.getDefault();
    }

    public static boolean arenaChanges() {
        return ScpConfigs.Server.isLoaded() ? ArenaChanges.getAsBoolean() : ArenaChanges.getDefault();
    }

    public static int riteRadius() {
        return ScpConfigs.Server.isLoaded() ? RiteRadius.get() : RiteRadius.getDefault();
    }
}
