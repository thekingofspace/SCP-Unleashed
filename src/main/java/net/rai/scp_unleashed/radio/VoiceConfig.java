package net.rai.scp_unleashed.radio;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class VoiceConfig {
    public static final String Prefix = "config.scp_unleashed.voice.";
    public static final int DefaultDirectRange = 64;
    public static final int DefaultCoverageBase = 64;
    public static final int DefaultCoveragePerMast = 32;
    public static final int DefaultLinkBase = 128;
    public static final int DefaultLinkPerMast = 64;
    public static final int DefaultBaseDraw = 8;
    public static final int DefaultDrawPerMast = 4;
    public static final String DefaultChatPrefix = "!";
    public static final int DefaultClipsPerAnomaly = 64;
    public static final int DefaultClipsPerSpeaker = 16;
    public static final int DefaultMaxAgeDays = 14;
    public static final int DefaultLureMinSeconds = 6;
    public static final int DefaultLureMaxSeconds = 14;
    public static final int DefaultIdleLureMinSeconds = 20;
    public static final int DefaultIdleLureMaxSeconds = 45;
    public static ModConfigSpec.BooleanValue VoiceRecording;
    public static ModConfigSpec.BooleanValue Scp939VoiceLures;
    public static ModConfigSpec.BooleanValue Scp001DayVoices;
    public static ModConfigSpec.BooleanValue Scp939VoiceAttraction;
    public static ModConfigSpec.IntValue Scp939LureMinSeconds;
    public static ModConfigSpec.IntValue Scp939LureMaxSeconds;
    public static ModConfigSpec.IntValue Scp939IdleLureMinSeconds;
    public static ModConfigSpec.IntValue Scp939IdleLureMaxSeconds;
    public static ModConfigSpec.IntValue ClipsPerAnomaly;
    public static ModConfigSpec.IntValue ClipsPerSpeaker;
    public static ModConfigSpec.IntValue MaxAgeDays;
    public static ModConfigSpec.IntValue DirectRange;
    public static ModConfigSpec.IntValue CoverageBase;
    public static ModConfigSpec.IntValue CoveragePerMast;
    public static ModConfigSpec.IntValue LinkBase;
    public static ModConfigSpec.IntValue LinkPerMast;
    public static ModConfigSpec.IntValue BaseDraw;
    public static ModConfigSpec.IntValue DrawPerMast;
    public static ModConfigSpec.ConfigValue<String> ChatPrefix;

    public static void define(ModConfigSpec.Builder Builder) {
        VoiceRecording = Builder.comment("Needs Simple Voice Chat. Master switch for anomaly voice recording. When on, some anomalies may record short clips (one to three seconds) of players talking and replay them later.",
                        "Clips are kept per anomaly in the world save under data/scp_unleashed/voice so they survive restarts. Every player is told on joining while this is on, and can delete their own clips with /scp_unleashed voice forgetme.",
                        "Players who are spectating, whispering, in a voice chat group, silenced or have voice chat disabled are never recorded. When off, nothing is recorded or replayed and stored clips are left untouched.")
                .translation(Prefix + "voice_recording").define("voiceRecording", false);
        Scp939VoiceLures = Builder.comment("Only with voiceRecording on. SCP-939 may record short clips of players talking near it and replay them from its own position to lure players.")
                .translation(Prefix + "scp939_voice_lures").define("scp939VoiceLures", true);
        Scp939VoiceAttraction = Builder.comment("Only with voiceRecording on. Players talking through Simple Voice Chat draw SCP-939 toward where they spoke: about 32 blocks for normal speech and 6 for whispers. Nothing is recorded for this.")
                .translation(Prefix + "scp939_voice_attraction").define("scp939VoiceAttraction", true);
        Scp939LureMinSeconds = Builder.comment("Shortest time in seconds between SCP-939 lure calls while it stalks prey or a heard voice.")
                .translation(Prefix + "scp939_lure_min_seconds").defineInRange("scp939LureMinSeconds", DefaultLureMinSeconds, 1, 600);
        Scp939LureMaxSeconds = Builder.comment("Longest time in seconds between SCP-939 lure calls while it stalks prey or a heard voice.")
                .translation(Prefix + "scp939_lure_max_seconds").defineInRange("scp939LureMaxSeconds", DefaultLureMaxSeconds, 1, 600);
        Scp939IdleLureMinSeconds = Builder.comment("Shortest time in seconds between SCP-939 lure calls while it has no prey but players are within 48 blocks.")
                .translation(Prefix + "scp939_idle_lure_min_seconds").defineInRange("scp939IdleLureMinSeconds", DefaultIdleLureMinSeconds, 1, 3600);
        Scp939IdleLureMaxSeconds = Builder.comment("Longest time in seconds between SCP-939 lure calls while it has no prey but players are within 48 blocks.")
                .translation(Prefix + "scp939_idle_lure_max_seconds").defineInRange("scp939IdleLureMaxSeconds", DefaultIdleLureMaxSeconds, 1, 3600);
        Scp001DayVoices = Builder.comment("Only with voiceRecording on. SCP-001 (When Day Breaks) may record players while they are in its dimension and replay them.")
                .translation(Prefix + "scp001_day_voices").define("scp001DayVoices", true);
        ClipsPerAnomaly = Builder.comment("Most voice clips kept for one anomaly. The oldest are removed first.")
                .translation(Prefix + "clips_per_anomaly").defineInRange("voiceClipsPerAnomaly", DefaultClipsPerAnomaly, 1, 1024);
        ClipsPerSpeaker = Builder.comment("Most voice clips kept of one player for one anomaly. The oldest are removed first.")
                .translation(Prefix + "clips_per_speaker").defineInRange("voiceClipsPerSpeaker", DefaultClipsPerSpeaker, 1, 1024);
        MaxAgeDays = Builder.comment("Voice clips older than this many days are deleted when an anomaly's clips are loaded and once every in-game day.")
                .translation(Prefix + "max_age_days").defineInRange("voiceMaxAgeDays", DefaultMaxAgeDays, 1, 3650);
        DirectRange = Builder.comment("Blocks two handheld radios reach each other without any booster tower.")
                .translation(Prefix + "direct_range").defineInRange("radioDirectRange", DefaultDirectRange, 8, 1024);
        CoverageBase = Builder.comment("Base reach in blocks from a powered booster tower to handheld radios. Every mast section adds towerCoveragePerMast, so with the defaults one mast reaches 96 blocks and four masts reach 192.")
                .translation(Prefix + "coverage_base").defineInRange("towerCoverageBase", DefaultCoverageBase, 8, 2048);
        CoveragePerMast = Builder.comment("Extra radio reach in blocks for every mast section on a booster tower.")
                .translation(Prefix + "coverage_per_mast").defineInRange("towerCoveragePerMast", DefaultCoveragePerMast, 0, 512);
        LinkBase = Builder.comment("Base tower to tower link range in blocks. Every mast section adds towerLinkPerMast, so with the defaults one mast links 192 blocks and four masts link 384. Two towers link when they are within the shorter of their two link ranges.")
                .translation(Prefix + "link_base").defineInRange("towerLinkBase", DefaultLinkBase, 8, 4096);
        LinkPerMast = Builder.comment("Extra tower to tower link range in blocks for every mast section.")
                .translation(Prefix + "link_per_mast").defineInRange("towerLinkPerMast", DefaultLinkPerMast, 0, 1024);
        BaseDraw = Builder.comment("Flat SP/t every booster tower with at least one mast draws.")
                .translation(Prefix + "base_draw").defineInRange("towerBaseDraw", DefaultBaseDraw, 0, 10000);
        DrawPerMast = Builder.comment("Extra SP/t drawn for every mast section on a booster tower.")
                .translation(Prefix + "draw_per_mast").defineInRange("towerDrawPerMast", DefaultDrawPerMast, 0, 10000);
        ChatPrefix = Builder.comment("Chat that starts with this text while holding a switched on handheld radio goes out over the radio instead of to everyone.")
                .translation(Prefix + "chat_prefix").define("radioChatPrefix", DefaultChatPrefix, Value -> Value instanceof String Text && !Text.isBlank() && Text.length() <= 4 && Text.strip().equals(Text));
    }

    private VoiceConfig() {
    }

    private static int value(ModConfigSpec.IntValue Value) {
        return ScpConfigs.Server.isLoaded() ? Value.getAsInt() : Value.getDefault();
    }

    private static boolean flag(ModConfigSpec.BooleanValue Value) {
        return ScpConfigs.Server.isLoaded() ? Value.getAsBoolean() : Value.getDefault();
    }

    public static boolean recording() {
        return flag(VoiceRecording);
    }

    public static boolean lures() {
        return flag(Scp939VoiceLures);
    }

    public static boolean voiceAttraction() {
        return flag(Scp939VoiceAttraction);
    }

    public static int lureMinSeconds() {
        return Math.min(value(Scp939LureMinSeconds), value(Scp939LureMaxSeconds));
    }

    public static int lureMaxSeconds() {
        return Math.max(value(Scp939LureMinSeconds), value(Scp939LureMaxSeconds));
    }

    public static int idleLureMinSeconds() {
        return Math.min(value(Scp939IdleLureMinSeconds), value(Scp939IdleLureMaxSeconds));
    }

    public static int idleLureMaxSeconds() {
        return Math.max(value(Scp939IdleLureMinSeconds), value(Scp939IdleLureMaxSeconds));
    }

    public static boolean dayVoices() {
        return flag(Scp001DayVoices);
    }

    public static int clipsPerAnomaly() {
        return value(ClipsPerAnomaly);
    }

    public static int clipsPerSpeaker() {
        return value(ClipsPerSpeaker);
    }

    public static int maxAgeDays() {
        return value(MaxAgeDays);
    }

    public static RadioNetwork.Ranges ranges() {
        return new RadioNetwork.Ranges(value(DirectRange), value(CoverageBase), value(CoveragePerMast), value(LinkBase), value(LinkPerMast));
    }

    public static int demand(int Masts) {
        if (Masts <= 0) {
            return 0;
        }
        return value(BaseDraw) + value(DrawPerMast) * Math.min(Masts, RadioNetwork.MaxMasts);
    }

    public static String chatPrefix() {
        String Text = ScpConfigs.Server.isLoaded() ? ChatPrefix.get() : ChatPrefix.getDefault();
        return Text == null || Text.isBlank() ? DefaultChatPrefix : Text;
    }
}
