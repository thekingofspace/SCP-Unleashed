package net.rai.scp_unleashed.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.terminal.TerminalColour;

public final class ScpClientConfig {
    public static ModConfigSpec.DoubleValue RealityDistortion;
    public static ModConfigSpec.BooleanValue TerminalScreens;
    public static ModConfigSpec.BooleanValue TerminalMenu;
    public static ModConfigSpec.BooleanValue TerminalLoading;
    public static ModConfigSpec.BooleanValue TerminalWorlds;
    public static ModConfigSpec.EnumValue<TerminalColour> TerminalTheme;
    public static ModConfigSpec.BooleanValue TerminalSounds;
    public static ModConfigSpec.BooleanValue TerminalReducedMotion;
    public static ModConfigSpec.BooleanValue Scp066LoudMusic;
    public static ModConfigSpec.DoubleValue NoticeScale;
    public static ModConfigSpec.IntValue NoticeOffsetX;
    public static ModConfigSpec.IntValue NoticeOffsetY;
    public static ModConfigSpec.BooleanValue WarheadEffects;
    public static ModConfigSpec.BooleanValue PostEffects;
    public static ModConfigSpec.BooleanValue SkipIntroCutscene;
    public static ModConfigSpec.BooleanValue LocationTitles;
    public static ModConfigSpec.BooleanValue AllowVoiceRecording;

    public static void define(ModConfigSpec.Builder Builder) {
        Scp066LoudMusic = Builder.translation("config.scp_unleashed.scp066_loud_music").define("scp066LoudMusic", true);
        NoticeScale = Builder.translation("config.scp_unleashed.notice_scale").defineInRange("noticeScale", 1.0, 0.5, 2.0);
        NoticeOffsetX = Builder.translation("config.scp_unleashed.notice_offset_x").defineInRange("noticeOffsetX", 0, 0, 400);
        NoticeOffsetY = Builder.translation("config.scp_unleashed.notice_offset_y").defineInRange("noticeOffsetY", 0, 0, 400);
        WarheadEffects = Builder.translation("config.scp_unleashed.warhead_effects").define("warheadEffects", true);
        PostEffects = Builder.translation("config.scp_unleashed.post_effects").define("postEffects", true);
        SkipIntroCutscene = Builder.translation("config.scp_unleashed.skip_intro_cutscene").define("skipIntroCutscene", false);
        LocationTitles = Builder.translation("config.scp_unleashed.location_titles").define("locationTitles", true);
        AllowVoiceRecording = Builder.translation("config.scp_unleashed.allow_voice_recording").define("allowVoiceRecording", true);
        Builder.translation("config.scp_unleashed.interface").push("interface");
        TerminalScreens = Builder.translation("config.scp_unleashed.terminal_screens").define("terminalScreens", true);
        TerminalMenu = Builder.translation("config.scp_unleashed.terminal_menu").define("terminalMenu", true);
        TerminalLoading = Builder.translation("config.scp_unleashed.terminal_loading").define("terminalLoading", true);
        TerminalWorlds = Builder.translation("config.scp_unleashed.terminal_worlds").define("terminalWorlds", true);
        TerminalTheme = Builder.translation("config.scp_unleashed.terminal_theme").defineEnum("terminalTheme", TerminalColour.Green);
        TerminalSounds = Builder.translation("config.scp_unleashed.terminal_sounds").define("terminalSounds", true);
        TerminalReducedMotion = Builder.translation("config.scp_unleashed.terminal_reduced_motion").define("terminalReducedMotion", false);
        Builder.pop();
        RealityDistortion = Builder.translation("config.scp_unleashed.reality_distortion").defineInRange("realityDistortion", 1.0, 0.0, 1.0);
    }

    private ScpClientConfig() {
    }

    public static boolean skipIntroCutscene() {
        return ScpConfigs.Client.isLoaded() && SkipIntroCutscene.get();
    }

    public static boolean locationTitles() {
        return !ScpConfigs.Client.isLoaded() || LocationTitles.get();
    }

    public static boolean allowVoiceRecording() {
        return !ScpConfigs.Client.isLoaded() || AllowVoiceRecording.get();
    }

    public static double realityDistortion() {
        return ScpConfigs.Client.isLoaded() ? RealityDistortion.get() : 1.0;
    }

    public static boolean postEffects() {
        return !ScpConfigs.Client.isLoaded() || PostEffects.get();
    }
}
