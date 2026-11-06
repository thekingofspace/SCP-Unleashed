package net.rai.scp_unleashed.terminal;

import java.util.ArrayList;
import java.util.List;

public final class TerminalWorldTags {
    public static final String Survival = "SVR";
    public static final String Creative = "CRT";
    public static final String Adventure = "ADV";
    public static final String Spectator = "SPC";
    public static final String Hardcore = "HC";
    public static final String Cheats = "CHT";
    public static final String Older = "OLD";
    public static final String Newer = "NEW";
    public static final String Experimental = "EXP";
    public static final String Locked = "LCK";
    public static final String Broken = "ERR";
    public static final List<String> Labels = List.of(Survival, Creative, Adventure, Spectator, Hardcore, Cheats, Older, Newer, Experimental, Locked, Broken);

    private TerminalWorldTags() {
    }

    public static String mode(String GameMode) {
        return switch (GameMode == null ? "" : GameMode) {
            case "creative" -> Creative;
            case "adventure" -> Adventure;
            case "spectator" -> Spectator;
            default -> Survival;
        };
    }

    public static List<String> tags(String GameMode, boolean IsHardcore, boolean HasCheats, int WorldData, int CurrentData, boolean IsExperimental,
                                    boolean IsLocked, boolean IsCompatible, boolean NeedsConversion) {
        List<String> Found = new ArrayList<>();
        Found.add(IsHardcore ? Hardcore : mode(GameMode));
        if (HasCheats) {
            Found.add(Cheats);
        }
        if (NeedsConversion || (WorldData > 0 && WorldData < CurrentData)) {
            Found.add(Older);
        } else if (WorldData > CurrentData) {
            Found.add(Newer);
        }
        if (IsExperimental) {
            Found.add(Experimental);
        }
        if (IsLocked) {
            Found.add(Locked);
        }
        if (!IsCompatible) {
            Found.add(Broken);
        }
        return Found;
    }
}
