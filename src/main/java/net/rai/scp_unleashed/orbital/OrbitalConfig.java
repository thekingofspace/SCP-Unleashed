package net.rai.scp_unleashed.orbital;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class OrbitalConfig {
    public static ModConfigSpec.DoubleValue Damage;
    public static ModConfigSpec.DoubleValue BeamRadius;
    public static ModConfigSpec.DoubleValue EdgeShare;
    public static ModConfigSpec.DoubleValue SplashRadius;
    public static ModConfigSpec.DoubleValue SplashDamage;
    public static ModConfigSpec.IntValue CooldownSeconds;
    public static ModConfigSpec.IntValue ChargeTicks;
    public static ModConfigSpec.IntValue Range;
    public static ModConfigSpec.IntValue MaxCover;
    public static ModConfigSpec.BooleanValue Crater;
    public static ModConfigSpec.IntValue CraterRadius;

    public static void define(ModConfigSpec.Builder Builder) {
        Damage = Builder.comment("Damage dealt at the centre of the beam.")
                .translation("config.scp_unleashed.orbital.damage").defineInRange("damage", 3000.0, 1.0, 100000.0);
        BeamRadius = Builder.comment("Radius of the beam in blocks. Everything inside the column takes beam damage.")
                .translation("config.scp_unleashed.orbital.beam_radius").defineInRange("beamRadius", 4.5, 1.0, 16.0);
        EdgeShare = Builder.comment("Share of the full damage dealt at the edge of the beam. Damage falls off linearly from the centre.")
                .translation("config.scp_unleashed.orbital.edge_share").defineInRange("edgeShare", 0.5, 0.0, 1.0);
        SplashRadius = Builder.comment("Radius of the blast around the impact in blocks, beyond the beam itself.")
                .translation("config.scp_unleashed.orbital.splash_radius").defineInRange("splashRadius", 10.0, 0.0, 32.0);
        SplashDamage = Builder.comment("Blast damage just outside the beam, falling to nothing at the splash radius.")
                .translation("config.scp_unleashed.orbital.splash_damage").defineInRange("splashDamage", 40.0, 0.0, 10000.0);
        CooldownSeconds = Builder.comment("Seconds an array needs to recharge between shots.")
                .translation("config.scp_unleashed.orbital.cooldown").defineInRange("cooldownSeconds", 60, 0, 86400);
        ChargeTicks = Builder.comment("Warning time in ticks between calling a strike and the beam landing.")
                .translation("config.scp_unleashed.orbital.charge_ticks").defineInRange("chargeTicks", 20, 5, 200);
        Range = Builder.comment("How far the designator can mark a target, in blocks.")
                .translation("config.scp_unleashed.orbital.range").defineInRange("range", 256, 16, 512);
        MaxCover = Builder.comment("The beam strikes the highest surface above the marked block. Targets buried deeper than this are refused.")
                .translation("config.scp_unleashed.orbital.max_cover").defineInRange("maxCover", 24, 0, 384);
        Crater = Builder.comment("Whether the strike carves a crater. The mobGriefing game rule must also allow it.")
                .translation("config.scp_unleashed.orbital.crater").define("crater", true);
        CraterRadius = Builder.comment("Radius of the crater in blocks.")
                .translation("config.scp_unleashed.orbital.crater_radius").defineInRange("craterRadius", 5, 1, 12);
    }

    private OrbitalConfig() {
    }

    public static float damage() {
        return (float) (double) (ScpConfigs.Server.isLoaded() ? Damage.get() : Damage.getDefault());
    }

    public static double beamRadius() {
        return ScpConfigs.Server.isLoaded() ? BeamRadius.get() : BeamRadius.getDefault();
    }

    public static double edgeShare() {
        return ScpConfigs.Server.isLoaded() ? EdgeShare.get() : EdgeShare.getDefault();
    }

    public static double splashRadius() {
        return ScpConfigs.Server.isLoaded() ? SplashRadius.get() : SplashRadius.getDefault();
    }

    public static float splashDamage() {
        return (float) (double) (ScpConfigs.Server.isLoaded() ? SplashDamage.get() : SplashDamage.getDefault());
    }

    public static int cooldownTicks() {
        return (ScpConfigs.Server.isLoaded() ? CooldownSeconds.get() : CooldownSeconds.getDefault()) * 20;
    }

    public static int chargeTicks() {
        return ScpConfigs.Server.isLoaded() ? ChargeTicks.get() : ChargeTicks.getDefault();
    }

    public static int range() {
        return ScpConfigs.Server.isLoaded() ? Range.get() : Range.getDefault();
    }

    public static int maxCover() {
        return ScpConfigs.Server.isLoaded() ? MaxCover.get() : MaxCover.getDefault();
    }

    public static boolean crater() {
        return ScpConfigs.Server.isLoaded() ? Crater.get() : Crater.getDefault();
    }

    public static int craterRadius() {
        return ScpConfigs.Server.isLoaded() ? CraterRadius.get() : CraterRadius.getDefault();
    }
}
