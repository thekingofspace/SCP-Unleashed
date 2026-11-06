package net.rai.scp_unleashed.scp5001.event;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

import java.util.HashMap;
import java.util.Map;

public final class Scp5001EventConfig {
    public static ModConfigSpec.DoubleValue RiseMinutes;
    public static ModConfigSpec.IntValue AlarmSeconds;
    public static ModConfigSpec.IntValue RodLatchSeconds;
    public static ModConfigSpec.DoubleValue RodDrag;
    public static ModConfigSpec.DoubleValue OverdriveSlow;
    public static ModConfigSpec.DoubleValue OverdriveReverse;
    public static ModConfigSpec.IntValue RiseHeight;
    public static ModConfigSpec.IntValue HoverHeight;
    public static ModConfigSpec.IntValue WaveSeconds;
    public static ModConfigSpec.IntValue ExposureMax;
    public static ModConfigSpec.IntValue ExposureGainFar;
    public static ModConfigSpec.IntValue ExposureGainNear;
    public static ModConfigSpec.IntValue ExposureDecaySeconds;
    public static ModConfigSpec.IntValue SiteRadius;
    public static ModConfigSpec.IntValue SiteHeight;
    public static ModConfigSpec.IntValue SwarmWaves;
    public static ModConfigSpec.IntValue SwarmBase;
    public static ModConfigSpec.IntValue SwarmPerPlayer;
    public static ModConfigSpec.IntValue SwarmCap;
    public static ModConfigSpec.IntValue SwarmDelaySeconds;
    public static ModConfigSpec.IntValue AbandonSeconds;
    public static ModConfigSpec.IntValue ResetSeconds;
    public static ModConfigSpec.DoubleValue CooldownDays;
    public static ModConfigSpec.IntValue BurstRadius;
    private static final Map<String, Double> Overrides = new HashMap<>();

    public static void define(ModConfigSpec.Builder Builder) {
        RiseMinutes = Builder.comment("Real time minutes the SCP-5001 vessel takes to rise to a breach once the safety interlocks are gone, with every restraint pillar latched.")
                .translation(key("rise_minutes")).defineInRange("riseMinutes", 12.0, 1.0, 240.0);
        AlarmSeconds = Builder.comment("Seconds of alarm between the interlocks failing and the vessel starting to rise.")
                .translation(key("alarm_seconds")).defineInRange("alarmSeconds", 20, 0, 600);
        RodLatchSeconds = Builder.comment("Seconds the restraint pillars take to reach and latch onto the vessel.")
                .translation(key("rod_latch_seconds")).defineInRange("rodLatchSeconds", 10, 0, 600);
        RodDrag = Builder.comment("How much the latched restraint pillars slow the rise. 0.25 means the vessel rises a third faster without them.")
                .translation(key("rod_drag")).defineInRange("rodDrag", 0.25, 0.0, 0.9);
        OverdriveSlow = Builder.comment("How much each generator in overdrive slows the rise, as a share of the normal rate.")
                .translation(key("overdrive_slow")).defineInRange("overdriveSlow", 0.2, 0.0, 0.3);
        OverdriveReverse = Builder.comment("How far each generator kicked into overdrive pushes the vessel back down, as a share of the full rise.")
                .translation(key("overdrive_reverse")).defineInRange("overdriveReverse", 0.03, 0.0, 0.5);
        RiseHeight = Builder.comment("Blocks the vessel climbs between rest and a breach.")
                .translation(key("rise_height")).defineInRange("riseHeight", 30, 4, 200);
        HoverHeight = Builder.comment("Blocks between the chamber core and the bottom of the vessel at rest.")
                .translation(key("hover_height")).defineInRange("hoverHeight", 4, 1, 32);
        WaveSeconds = Builder.comment("Seconds between the waves the vessel sends through the facility while it is active.")
                .translation(key("wave_seconds")).defineInRange("waveSeconds", 8, 1, 120);
        ExposureMax = Builder.comment("Exposure at which a creature inside the facility dies and leaves a mimic behind.")
                .translation(key("exposure_max")).defineInRange("exposureMax", 100, 5, 10000);
        ExposureGainFar = Builder.comment("Exposure each wave adds at the edge of the facility.")
                .translation(key("exposure_gain_far")).defineInRange("exposureGainFar", 1, 0, 1000);
        ExposureGainNear = Builder.comment("Exposure each wave adds right beside the vessel.")
                .translation(key("exposure_gain_near")).defineInRange("exposureGainNear", 4, 0, 1000);
        ExposureDecaySeconds = Builder.comment("Seconds between each point of exposure fading while outside the facility or between incidents.")
                .translation(key("exposure_decay_seconds")).defineInRange("exposureDecaySeconds", 12, 1, 3600);
        SiteRadius = Builder.comment("Horizontal reach in blocks from the chamber core that counts as inside the facility.")
                .translation(key("site_radius")).defineInRange("siteRadius", 110, 16, 512);
        SiteHeight = Builder.comment("Vertical reach in blocks from the chamber core that counts as inside the facility.")
                .translation(key("site_height")).defineInRange("siteHeight", 48, 8, 256);
        SwarmWaves = Builder.comment("Waves of SCP-610 the pinned vessel releases.")
                .translation(key("swarm_waves")).defineInRange("swarmWaves", 3, 1, 20);
        SwarmBase = Builder.comment("SCP-610 instances in every wave before counting players.")
                .translation(key("swarm_base")).defineInRange("swarmBase", 4, 0, 64);
        SwarmPerPlayer = Builder.comment("Extra SCP-610 instances per wave for each player inside the facility.")
                .translation(key("swarm_per_player")).defineInRange("swarmPerPlayer", 3, 0, 32);
        SwarmCap = Builder.comment("Most SCP-610 instances in a single wave.")
                .translation(key("swarm_cap")).defineInRange("swarmCap", 24, 1, 128);
        SwarmDelaySeconds = Builder.comment("Seconds between one wave being cleared and the next one emerging.")
                .translation(key("swarm_delay_seconds")).defineInRange("swarmDelaySeconds", 6, 0, 600);
        AbandonSeconds = Builder.comment("Seconds without any player inside the facility before the pinned vessel slips free again.")
                .translation(key("abandon_seconds")).defineInRange("abandonSeconds", 300, 10, 72000);
        ResetSeconds = Builder.comment("Seconds the vessel takes to settle back onto the core after a reset.")
                .translation(key("reset_seconds")).defineInRange("resetSeconds", 20, 1, 600);
        CooldownDays = Builder.comment("In-game days after an incident before the safety interlocks re-engage and a new incident is possible.")
                .translation(key("cooldown_days")).defineInRange("cooldownDays", 3.0, 0.0, 365.0);
        BurstRadius = Builder.comment("Reach in blocks of the shockwave when the vessel bursts.")
                .translation(key("burst_radius")).defineInRange("burstRadius", 40, 0, 256);
    }

    private Scp5001EventConfig() {
    }

    private static String key(String Name) {
        return "config.scp_unleashed.scp5001_event." + Name;
    }

    public static void overrideForTests(String Name, double Value) {
        Overrides.put(Name, Value);
    }

    public static void clearTestOverrides() {
        Overrides.clear();
    }

    private static double read(String Name, ModConfigSpec.ConfigValue<? extends Number> Value) {
        Double Forced = Overrides.get(Name);
        if (Forced != null) {
            return Forced;
        }
        return (ScpConfigs.Server.isLoaded() ? Value.get() : Value.getDefault()).doubleValue();
    }

    public static int riseTicks() {
        return Math.max(20, (int) Math.round(read("riseMinutes", RiseMinutes) * 1200.0));
    }

    public static int alarmTicks() {
        return (int) Math.round(read("alarmSeconds", AlarmSeconds) * 20.0);
    }

    public static int rodLatchTicks() {
        return (int) Math.round(read("rodLatchSeconds", RodLatchSeconds) * 20.0);
    }

    public static double rodDrag() {
        return read("rodDrag", RodDrag);
    }

    public static double overdriveSlow() {
        return read("overdriveSlow", OverdriveSlow);
    }

    public static double overdriveReverse() {
        return read("overdriveReverse", OverdriveReverse);
    }

    public static int riseHeight() {
        return (int) read("riseHeight", RiseHeight);
    }

    public static int hoverHeight() {
        return (int) read("hoverHeight", HoverHeight);
    }

    public static int waveTicks() {
        return Math.max(1, (int) Math.round(read("waveSeconds", WaveSeconds) * 20.0));
    }

    public static int exposureMax() {
        return (int) read("exposureMax", ExposureMax);
    }

    public static int exposureGainFar() {
        return (int) read("exposureGainFar", ExposureGainFar);
    }

    public static int exposureGainNear() {
        return (int) read("exposureGainNear", ExposureGainNear);
    }

    public static int exposureDecayTicks() {
        return Math.max(1, (int) Math.round(read("exposureDecaySeconds", ExposureDecaySeconds) * 20.0));
    }

    public static int siteRadius() {
        return (int) read("siteRadius", SiteRadius);
    }

    public static int siteHeight() {
        return (int) read("siteHeight", SiteHeight);
    }

    public static int swarmWaves() {
        return (int) read("swarmWaves", SwarmWaves);
    }

    public static int swarmBase() {
        return (int) read("swarmBase", SwarmBase);
    }

    public static int swarmPerPlayer() {
        return (int) read("swarmPerPlayer", SwarmPerPlayer);
    }

    public static int swarmCap() {
        return (int) read("swarmCap", SwarmCap);
    }

    public static int swarmDelayTicks() {
        return (int) Math.round(read("swarmDelaySeconds", SwarmDelaySeconds) * 20.0);
    }

    public static int abandonTicks() {
        return (int) Math.round(read("abandonSeconds", AbandonSeconds) * 20.0);
    }

    public static int resetTicks() {
        return Math.max(1, (int) Math.round(read("resetSeconds", ResetSeconds) * 20.0));
    }

    public static long cooldownTicks() {
        return Math.round(read("cooldownDays", CooldownDays) * 24000.0);
    }

    public static int burstRadius() {
        return (int) read("burstRadius", BurstRadius);
    }
}
