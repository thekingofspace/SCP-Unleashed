package net.rai.scp_unleashed.scp001scarlet;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class ScarletConfig {
    public static ModConfigSpec.DoubleValue BaselinePerDay;
    public static ModConfigSpec.DoubleValue RitualMin;
    public static ModConfigSpec.DoubleValue RitualMax;
    public static ModConfigSpec.BooleanValue Cultists;
    public static ModConfigSpec.DoubleValue CultistMinDays;
    public static ModConfigSpec.DoubleValue CultistMaxDays;
    public static ModConfigSpec.IntValue RitualSeconds;
    public static ModConfigSpec.DoubleValue Rite2317Drop;
    public static ModConfigSpec.DoubleValue Rite2317CooldownDays;
    public static ModConfigSpec.DoubleValue Xk2Days;
    public static final double DefaultBaseline = 0.012;
    public static final double DefaultRitualMin = 0.02;
    public static final double DefaultRitualMax = 0.04;
    public static final double DefaultCultistMin = 3.0;
    public static final double DefaultCultistMax = 6.0;
    public static final int DefaultRitualSeconds = 150;
    public static final double DefaultRiteDrop = 0.2;
    public static final double DefaultRiteCooldown = 1.0;
    public static final double DefaultXk2Days = 3.0;
    private static double OverrideBaseline = -1.0;
    private static double OverrideXk2Days = -1.0;

    public static void define(ModConfigSpec.Builder Builder) {
        Builder.translation("config.scp_unleashed.scarlet.meter").push("meter");
        BaselinePerDay = Builder.translation("config.scp_unleashed.scarlet.baseline_per_day").defineInRange("baselinePerDay", DefaultBaseline, 0.0, 1.0);
        Rite2317Drop = Builder.translation("config.scp_unleashed.scarlet.rite_2317_drop").defineInRange("rite2317Drop", DefaultRiteDrop, 0.0, 1.0);
        Rite2317CooldownDays = Builder.translation("config.scp_unleashed.scarlet.rite_2317_cooldown_days").defineInRange("rite2317CooldownDays", DefaultRiteCooldown, 0.0, 30.0);
        Builder.pop();
        Builder.translation("config.scp_unleashed.scarlet.cultists").push("cultists");
        Cultists = Builder.translation("config.scp_unleashed.scarlet.cultists_enabled").define("cultists", true);
        CultistMinDays = Builder.translation("config.scp_unleashed.scarlet.cultist_min_days").defineInRange("cultistMinDays", DefaultCultistMin, 0.5, 60.0);
        CultistMaxDays = Builder.translation("config.scp_unleashed.scarlet.cultist_max_days").defineInRange("cultistMaxDays", DefaultCultistMax, 0.5, 60.0);
        RitualSeconds = Builder.translation("config.scp_unleashed.scarlet.ritual_seconds").defineInRange("ritualSeconds", DefaultRitualSeconds, 20, 1200);
        RitualMin = Builder.translation("config.scp_unleashed.scarlet.ritual_min").defineInRange("ritualMin", DefaultRitualMin, 0.0, 1.0);
        RitualMax = Builder.translation("config.scp_unleashed.scarlet.ritual_max").defineInRange("ritualMax", DefaultRitualMax, 0.0, 1.0);
        Builder.pop();
        Builder.translation("config.scp_unleashed.scarlet.xk2").push("xk2");
        Xk2Days = Builder.translation("config.scp_unleashed.scarlet.xk2_days").defineInRange("xk2Days", DefaultXk2Days, 0.05, 60.0);
        Builder.pop();
    }

    private ScarletConfig() {
    }

    private static double read(ModConfigSpec.DoubleValue Value, double Fallback) {
        return ScpConfigs.Server.isLoaded() ? Value.get() : Fallback;
    }

    private static int read(ModConfigSpec.IntValue Value, int Fallback) {
        return ScpConfigs.Server.isLoaded() ? Value.get() : Fallback;
    }

    public static double baselinePerDay() {
        return OverrideBaseline >= 0.0 ? OverrideBaseline : read(BaselinePerDay, DefaultBaseline);
    }

    public static double ritualMin() {
        return Math.min(read(RitualMin, DefaultRitualMin), read(RitualMax, DefaultRitualMax));
    }

    public static double ritualMax() {
        return Math.max(read(RitualMin, DefaultRitualMin), read(RitualMax, DefaultRitualMax));
    }

    public static boolean cultists() {
        return !ScpConfigs.Server.isLoaded() || Cultists.get();
    }

    public static double cultistMinDays() {
        return Math.min(read(CultistMinDays, DefaultCultistMin), read(CultistMaxDays, DefaultCultistMax));
    }

    public static double cultistMaxDays() {
        return Math.max(read(CultistMinDays, DefaultCultistMin), read(CultistMaxDays, DefaultCultistMax));
    }

    public static int ritualTicks() {
        return read(RitualSeconds, DefaultRitualSeconds) * 20;
    }

    public static float rite2317Drop() {
        return (float) read(Rite2317Drop, DefaultRiteDrop);
    }

    public static long rite2317CooldownTicks() {
        return (long) (read(Rite2317CooldownDays, DefaultRiteCooldown) * 24000.0);
    }

    public static long xk2Ticks() {
        double Days = OverrideXk2Days > 0.0 ? OverrideXk2Days : read(Xk2Days, DefaultXk2Days);
        return Math.max(20L, (long) (Days * 24000.0));
    }

    public static void overrideForTests(double Baseline, double Xk2Days) {
        OverrideBaseline = Baseline;
        OverrideXk2Days = Xk2Days;
    }
}
