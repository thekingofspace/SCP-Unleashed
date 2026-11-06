package net.rai.scp_unleashed.scp5001.mekhane;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

import java.util.List;

public final class MekhaneConfig {
    public static final int DefaultIronDemand = 4096;
    public static final int DefaultElectronicsDemand = 1024;
    public static final int DefaultWatchdogMinutes = 5;
    public static final int DefaultClearSeconds = 70;
    public static final int DefaultRestartLimit = 3;
    public static final List<String> DefaultIronValues = List.of(
            "minecraft:iron_nugget=1/9",
            "minecraft:iron_ingot=1",
            "minecraft:raw_iron=1",
            "minecraft:iron_block=9",
            "minecraft:raw_iron_block=9");
    public static final List<String> DefaultElectronicsValues = List.of(
            "scp_unleashed:circuit_board=1",
            "scp_unleashed:advanced_circuit=4",
            "scp_unleashed:capacitor=1/2",
            "scp_unleashed:electric_motor=1",
            "scp_unleashed:superconductor_coil=2",
            "scp_unleashed:copper_wire=1/4",
            "scp_unleashed:wire_coil=1/2",
            "scp_unleashed:advanced_wire=1",
            "scp_unleashed:tether_wire=1",
            "scp_unleashed:super_heavy_wire=2",
            "scp_unleashed:ultimate_wire=4");
    public static ModConfigSpec.IntValue IronDemand;
    public static ModConfigSpec.IntValue ElectronicsDemand;
    public static ModConfigSpec.ConfigValue<List<? extends String>> IronValues;
    public static ModConfigSpec.ConfigValue<List<? extends String>> ElectronicsValues;
    public static ModConfigSpec.IntValue WatchdogMinutes;
    public static ModConfigSpec.IntValue ClearSeconds;
    public static ModConfigSpec.IntValue RestartLimit;
    private static int OverrideIron = -1;
    private static int OverrideElectronics = -1;
    private static int OverrideWatchdog = -1;

    public static void define(ModConfigSpec.Builder Builder) {
        IronDemand = Builder.comment("Iron, in ingot equivalents, that the SCP-5001 emergency protocol needs before it answers.")
                .translation("config.scp_unleashed.mekhane.iron_demand").defineInRange("ironDemand", DefaultIronDemand, 1, 1000000);
        ElectronicsDemand = Builder.comment("Electronics points that the SCP-5001 emergency protocol needs before it answers.")
                .translation("config.scp_unleashed.mekhane.electronics_demand").defineInRange("electronicsDemand", DefaultElectronicsDemand, 1, 1000000);
        IronValues = Builder.comment("Iron items the intake takes, as item=ingots. Fractions like 1/9 are allowed.")
                .translation("config.scp_unleashed.mekhane.iron_values").defineListAllowEmpty("ironValues", DefaultIronValues, () -> "minecraft:iron_ingot=1", MekhaneConfig::validEntry);
        ElectronicsValues = Builder.comment("Points per item for the scp_unleashed:mekhane_electronics tag, as item=points. Tagged items not listed here count 1.")
                .translation("config.scp_unleashed.mekhane.electronics_values").defineListAllowEmpty("electronicsValues", DefaultElectronicsValues, () -> "scp_unleashed:circuit_board=1", MekhaneConfig::validEntry);
        WatchdogMinutes = Builder.comment("Minutes a stalled duel may wait before it restarts from the standoff.")
                .translation("config.scp_unleashed.mekhane.watchdog_minutes").defineInRange("watchdogMinutes", DefaultWatchdogMinutes, 1, 60);
        ClearSeconds = Builder.comment("Seconds after the recontainment before the site returns to dormant.")
                .translation("config.scp_unleashed.mekhane.clear_seconds").defineInRange("clearSeconds", DefaultClearSeconds, 10, 600);
        RestartLimit = Builder.comment("Watchdog restarts without an opponent before the protocol completes on its own, so the end of the world is never paused forever.")
                .translation("config.scp_unleashed.mekhane.restart_limit").defineInRange("restartLimit", DefaultRestartLimit, 0, 20);
    }

    private MekhaneConfig() {
    }

    private static boolean validEntry(Object Entry) {
        return Entry instanceof String Text && MekhaneValues.parseEntry(Text) != null;
    }

    private static int value(ModConfigSpec.IntValue Value) {
        return ScpConfigs.Server.isLoaded() ? Value.getAsInt() : Value.getDefault();
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(ModConfigSpec.ConfigValue<List<? extends String>> Value, List<String> Fallback) {
        return ScpConfigs.Server.isLoaded() ? (List<String>) Value.get() : Fallback;
    }

    public static int ironDemand() {
        return OverrideIron > 0 ? OverrideIron : value(IronDemand);
    }

    public static int electronicsDemand() {
        return OverrideElectronics > 0 ? OverrideElectronics : value(ElectronicsDemand);
    }

    public static List<String> ironValues() {
        return list(IronValues, DefaultIronValues);
    }

    public static List<String> electronicsValues() {
        return list(ElectronicsValues, DefaultElectronicsValues);
    }

    public static int watchdogTicks() {
        return OverrideWatchdog > 0 ? OverrideWatchdog : value(WatchdogMinutes) * 1200;
    }

    public static int clearTicks() {
        return value(ClearSeconds) * 20;
    }

    public static int restartLimit() {
        return value(RestartLimit);
    }

    public static void overrideForTests(int Iron, int Electronics, int Watchdog) {
        OverrideIron = Iron;
        OverrideElectronics = Electronics;
        OverrideWatchdog = Watchdog;
        MekhaneValues.invalidate();
    }
}
