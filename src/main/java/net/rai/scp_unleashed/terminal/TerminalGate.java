package net.rai.scp_unleashed.terminal;

import java.util.Map;

public final class TerminalGate {
    public enum Kind {
        None,
        Title,
        LevelLoading,
        Receiving,
        Connect,
        Message,
        Progress,
        Disconnected,
        Worlds
    }

    public enum Decision {
        Keep,
        Terminal,
        Vanilla
    }

    public static final Map<String, Kind> Covered = Map.of(
            "net.minecraft.client.gui.screens.TitleScreen", Kind.Title,
            "net.minecraft.client.gui.screens.LevelLoadingScreen", Kind.LevelLoading,
            "net.minecraft.client.gui.screens.ReceivingLevelScreen", Kind.Receiving,
            "net.minecraft.client.gui.screens.ConnectScreen", Kind.Connect,
            "net.minecraft.client.gui.screens.GenericMessageScreen", Kind.Message,
            "net.minecraft.client.gui.screens.ProgressScreen", Kind.Progress,
            "net.minecraft.client.gui.screens.DisconnectedScreen", Kind.Disconnected,
            "net.minecraft.client.gui.screens.worldselection.SelectWorldScreen", Kind.Worlds,
            "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen", Kind.Worlds,
            "net.minecraft.client.gui.screens.worldselection.EditWorldScreen", Kind.Worlds);

    private TerminalGate() {
    }

    public static Kind kindOf(String ClassName) {
        return ClassName == null ? Kind.None : Covered.getOrDefault(ClassName, Kind.None);
    }

    public static boolean applies(Kind Which, boolean Master, boolean Menu, boolean Loading, boolean Worlds) {
        if (!Master || Which == null) {
            return false;
        }
        return switch (Which) {
            case None -> false;
            case Title -> Menu;
            case Worlds -> Worlds;
            default -> Loading;
        };
    }

    public static boolean applies(Kind Which, boolean Master, boolean Menu, boolean Loading) {
        return applies(Which, Master, Menu, Loading, false);
    }

    public static boolean applies(String ClassName, boolean Master, boolean Menu, boolean Loading) {
        return applies(kindOf(ClassName), Master, Menu, Loading);
    }

    public static boolean applies(String ClassName, boolean Master, boolean Menu, boolean Loading, boolean Worlds) {
        return applies(kindOf(ClassName), Master, Menu, Loading, Worlds);
    }

    public static Decision opening(String ClassName, boolean TerminalTitle, boolean MenuOn) {
        if (TerminalTitle) {
            return MenuOn ? Decision.Keep : Decision.Vanilla;
        }
        return kindOf(ClassName) == Kind.Title && MenuOn ? Decision.Terminal : Decision.Keep;
    }

    public static boolean paintsOnly(Kind Which) {
        return Which != Kind.Title;
    }

    public static boolean skipReceiving(boolean InitialJoin, boolean CutsceneFrom, boolean CutsceneTo, boolean SceneActive) {
        if (CutsceneTo) {
            return true;
        }
        if (InitialJoin) {
            return false;
        }
        return CutsceneFrom || SceneActive;
    }

    public static boolean sounds(boolean Master, boolean Sounds) {
        return Master && Sounds;
    }

    public static boolean effects(boolean Master, boolean ReducedMotion) {
        return Master && !ReducedMotion;
    }

    public static boolean dimensionScreen(boolean Applies, boolean CutsceneFrom, boolean CutsceneTo, boolean Cinematic) {
        return Applies && !skipReceiving(false, CutsceneFrom, CutsceneTo, Cinematic);
    }
}
