package net.rai.scp_unleashed.scp610;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class Scp610Config {
    public static final int DefaultMaxHives = 6;
    public static final int DefaultMaxFleshPerHive = 4096;
    public static final int DefaultBlocksPerTick = 12;
    public static final int DefaultMaxChamberBlocksPerHive = 24000;
    public static final int DefaultLordDepthMin = 28;
    public static final int DefaultLordDepthMax = 36;
    public static ModConfigSpec.IntValue MaxHives;
    public static ModConfigSpec.IntValue MaxFleshPerHive;
    public static ModConfigSpec.IntValue BlocksPerTick;
    public static ModConfigSpec.IntValue MaxChamberBlocksPerHive;
    public static ModConfigSpec.IntValue LordDepthMin;
    public static ModConfigSpec.IntValue LordDepthMax;
    private static int OverrideFlesh = -1;
    private static int OverrideHives = -1;
    private static int OverrideBudget = -1;
    private static int OverrideChamber = -1;
    private static int OverrideDepth = -1;

    public static void define(ModConfigSpec.Builder Builder) {
        MaxHives = Builder.comment("Most SCP-610 hives that may exist in a world at once.")
                .translation("config.scp_unleashed.scp610.max_hives").defineInRange("maxHives", DefaultMaxHives, 0, 64);
        MaxFleshPerHive = Builder.comment("Most flesh blocks a single SCP-610 hive may grow, above and below ground.")
                .translation("config.scp_unleashed.scp610.max_flesh_per_hive").defineInRange("maxFleshPerHive", DefaultMaxFleshPerHive, 0, 1000000);
        BlocksPerTick = Builder.comment("Most block changes all SCP-610 hives together may make in one server tick.")
                .translation("config.scp_unleashed.scp610.blocks_per_tick").defineInRange("blocksPerTick", DefaultBlocksPerTick, 0, 256);
        MaxChamberBlocksPerHive = Builder.comment("Most blocks a single SCP-610 hive may hollow out underground for its chambers and tunnels.")
                .translation("config.scp_unleashed.scp610.max_chamber_blocks_per_hive").defineInRange("maxChamberBlocksPerHive", DefaultMaxChamberBlocksPerHive, 0, 1000000);
        LordDepthMin = Builder.comment("Least depth in blocks below the oldest hive at which the deep chamber may form.")
                .translation("config.scp_unleashed.scp610.lord_depth_min").defineInRange("lordDepthMin", DefaultLordDepthMin, 8, 120);
        LordDepthMax = Builder.comment("Greatest depth in blocks below the oldest hive at which the deep chamber may form.")
                .translation("config.scp_unleashed.scp610.lord_depth_max").defineInRange("lordDepthMax", DefaultLordDepthMax, 8, 120);
    }

    private Scp610Config() {
    }

    private static int value(ModConfigSpec.IntValue Value) {
        return ScpConfigs.Server.isLoaded() ? Value.getAsInt() : Value.getDefault();
    }

    public static int maxHives() {
        return OverrideHives >= 0 ? OverrideHives : value(MaxHives);
    }

    public static int maxFleshPerHive() {
        return OverrideFlesh >= 0 ? OverrideFlesh : value(MaxFleshPerHive);
    }

    public static int blocksPerTick() {
        return OverrideBudget >= 0 ? OverrideBudget : value(BlocksPerTick);
    }

    public static int maxChamberBlocksPerHive() {
        return OverrideChamber >= 0 ? OverrideChamber : value(MaxChamberBlocksPerHive);
    }

    public static int lordDepthMin() {
        return OverrideDepth >= 0 ? OverrideDepth : Math.min(value(LordDepthMin), value(LordDepthMax));
    }

    public static int lordDepthMax() {
        return OverrideDepth >= 0 ? OverrideDepth : Math.max(value(LordDepthMin), value(LordDepthMax));
    }

    public static void overrideDepthForTests(int Depth) {
        OverrideDepth = Depth;
    }

    public static void overrideForTests(int Hives, int Flesh, int Budget, int Chamber) {
        OverrideHives = Hives;
        OverrideFlesh = Flesh;
        OverrideBudget = Budget;
        OverrideChamber = Chamber;
    }
}
