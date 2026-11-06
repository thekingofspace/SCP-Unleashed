package net.rai.scp_unleashed.scp001scarlet.court;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class ScarletCourtConfig {
    private static final String Prefix = "config.scp_unleashed.scarlet_court.";
    public static ModConfigSpec.DoubleValue ThornsPool;
    public static ModConfigSpec.DoubleValue BridePool;
    public static ModConfigSpec.DoubleValue EyesPool;
    public static ModConfigSpec.DoubleValue MoonPool;
    public static ModConfigSpec.DoubleValue ExtraPlayerShare;
    public static ModConfigSpec.DoubleValue HitCapShare;
    public static ModConfigSpec.DoubleValue EnvironmentShare;
    public static ModConfigSpec.IntValue CapSolo;
    public static ModConfigSpec.IntValue CapPerPlayer;
    public static ModConfigSpec.IntValue CapMax;
    public static ModConfigSpec.IntValue WaveSeconds;
    public static ModConfigSpec.IntValue BreatherSeconds;
    public static ModConfigSpec.IntValue KingWaveSeconds;
    public static ModConfigSpec.IntValue OpeningSeconds;

    public static void define(ModConfigSpec.Builder Builder) {
        ThornsPool = Builder.comment("Health of the Briar Crown for a single player.").translation(Prefix + "thorns_pool").defineInRange("thornsPool", 12000.0, 10.0, 1000000.0);
        BridePool = Builder.comment("Health of the Bride in Chains for a single player.").translation(Prefix + "bride_pool").defineInRange("bridePool", 13500.0, 10.0, 1000000.0);
        EyesPool = Builder.comment("Health of the Daughter of Eyes for a single player.").translation(Prefix + "eyes_pool").defineInRange("eyesPool", 12500.0, 10.0, 1000000.0);
        MoonPool = Builder.comment("Health of the Moon Daughter for a single player.").translation(Prefix + "moon_pool").defineInRange("moonPool", 15000.0, 10.0, 1000000.0);
        ExtraPlayerShare = Builder.comment("Extra daughter health and wave size per additional player, as a share of the solo value.")
                .translation(Prefix + "extra_player_share").defineInRange("extraPlayerShare", 0.5, 0.0, 4.0);
        HitCapShare = Builder.comment("The largest share of a daughter's health a single hit may remove.").translation(Prefix + "hit_cap_share")
                .defineInRange("hitCapShare", 0.02, 0.0001, 1.0);
        EnvironmentShare = Builder.comment("Share of damage without an attacker (fire, falls, lava) that a daughter still takes.").translation(Prefix + "environment_share")
                .defineInRange("environmentShare", 0.1, 0.0, 1.0);
        CapSolo = Builder.comment("Most living court minions with a single player.").translation(Prefix + "cap_solo").defineInRange("capSolo", 12, 0, 200);
        CapPerPlayer = Builder.comment("Extra living court minions allowed per additional player.").translation(Prefix + "cap_per_player").defineInRange("capPerPlayer", 6, 0, 100);
        CapMax = Builder.comment("Most living court minions in any fight.").translation(Prefix + "cap_max").defineInRange("capMax", 30, 0, 400);
        WaveSeconds = Builder.comment("Seconds between minion waves while a daughter fights.").translation(Prefix + "wave_seconds").defineInRange("waveSeconds", 28, 5, 600);
        BreatherSeconds = Builder.comment("Seconds of rest after a daughter falls.").translation(Prefix + "breather_seconds").defineInRange("breatherSeconds", 18, 5, 120);
        KingWaveSeconds = Builder.comment("Seconds between minion waves once the King himself can be harmed.").translation(Prefix + "king_wave_seconds")
                .defineInRange("kingWaveSeconds", 40, 5, 600);
        OpeningSeconds = Builder.comment("Seconds between the opening wave and the first daughter.").translation(Prefix + "opening_seconds")
                .defineInRange("openingSeconds", 12, 1, 120);
    }

    private ScarletCourtConfig() {
    }

    private static double value(ModConfigSpec.DoubleValue Value) {
        return ScpConfigs.Server.isLoaded() ? Value.get() : Value.getDefault();
    }

    private static int value(ModConfigSpec.IntValue Value) {
        return ScpConfigs.Server.isLoaded() ? Value.get() : Value.getDefault();
    }

    public static double pool(int Daughter) {
        return switch (Daughter) {
            case 0 -> value(ThornsPool);
            case 1 -> value(BridePool);
            case 2 -> value(EyesPool);
            default -> value(MoonPool);
        };
    }

    public static double extraPlayerShare() {
        return value(ExtraPlayerShare);
    }

    public static double hitCapShare() {
        return value(HitCapShare);
    }

    public static double environmentShare() {
        return value(EnvironmentShare);
    }

    public static int capSolo() {
        return value(CapSolo);
    }

    public static int capPerPlayer() {
        return value(CapPerPlayer);
    }

    public static int capMax() {
        return value(CapMax);
    }

    public static int waveTicks() {
        return value(WaveSeconds) * 20;
    }

    public static int breatherTicks() {
        return value(BreatherSeconds) * 20;
    }

    public static int kingWaveTicks() {
        return value(KingWaveSeconds) * 20;
    }

    public static int openingTicks() {
        return value(OpeningSeconds) * 20;
    }
}
