package net.rai.scp_unleashed.terminal;

import net.rai.scp_unleashed.config.ScpConfigs;
import net.rai.scp_unleashed.config.ScpClientConfig;

public final class TerminalSettings {
    private TerminalSettings() {
    }

    public static boolean loaded() {
        return ScpConfigs.Client.isLoaded();
    }

    public static boolean master() {
        return !loaded() || ScpClientConfig.TerminalScreens.get();
    }

    public static boolean menuOption() {
        return !loaded() || ScpClientConfig.TerminalMenu.get();
    }

    public static boolean loadingOption() {
        return !loaded() || ScpClientConfig.TerminalLoading.get();
    }

    public static boolean worldsOption() {
        return !loaded() || ScpClientConfig.TerminalWorlds.get();
    }

    public static boolean worlds() {
        return TerminalGate.applies(TerminalGate.Kind.Worlds, master(), menuOption(), loadingOption(), worldsOption());
    }

    public static boolean menu() {
        return TerminalGate.applies(TerminalGate.Kind.Title, master(), menuOption(), loadingOption());
    }

    public static boolean applies(TerminalGate.Kind Which) {
        return TerminalGate.applies(Which, master(), menuOption(), loadingOption(), worldsOption());
    }

    public static boolean sounds() {
        return TerminalGate.sounds(master(), !loaded() || ScpClientConfig.TerminalSounds.get());
    }

    public static boolean reducedMotion() {
        return loaded() && ScpClientConfig.TerminalReducedMotion.get();
    }

    public static TerminalColour colour() {
        return loaded() ? ScpClientConfig.TerminalTheme.get() : TerminalColour.Green;
    }

    public static TerminalPalette palette() {
        return TerminalPalette.of(colour());
    }
}
