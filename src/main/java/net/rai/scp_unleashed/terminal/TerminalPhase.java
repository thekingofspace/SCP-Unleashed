package net.rai.scp_unleashed.terminal;

public enum TerminalPhase {
    Prepare("prepare", TerminalTags.Sys, -1, 3, false, false),
    Connect("connect", TerminalTags.Net, 15, 4, false, false),
    Auth("auth", TerminalTags.Auth, 35, 3, false, false),
    Sync("sync", TerminalTags.Sync, 55, 3, false, false),
    Join("join", TerminalTags.Net, 80, 2, false, false),
    Generate("generate", TerminalTags.Gen, -1, 8, true, false),
    Terrain("terrain", TerminalTags.Map, -1, 4, false, true),
    Dimension("dimension", TerminalTags.Dim, -1, 3, false, true),
    Ready("ready", TerminalTags.Ok, -1, 2, false, false),
    Save("save", TerminalTags.Save, -1, 3, false, false),
    Disconnect("disconnect", TerminalTags.Fail, -1, 0, false, false);

    public static final String Prefix = "terminal.scp_unleashed.";

    public final String Id;
    public final String Tag;
    public final int Staged;
    public final int Flavours;
    public final boolean Progressive;
    public final boolean Counted;

    TerminalPhase(String NewId, String NewTag, int NewStaged, int NewFlavours, boolean NewProgressive, boolean NewCounted) {
        Id = NewId;
        Tag = NewTag;
        Staged = NewStaged;
        Flavours = NewFlavours;
        Progressive = NewProgressive;
        Counted = NewCounted;
    }

    public String headerKey() {
        return Prefix + "phase." + Id;
    }

    public String openKey() {
        return Prefix + "log." + Id + ".open";
    }

    public String flavourKey(int Index) {
        return Prefix + "log." + Id + ".flavour." + Index;
    }

    public String progressKey() {
        return Prefix + "log." + Id + ".progress";
    }

    public String countKey() {
        return Prefix + "log." + Id + ".count";
    }

    public String tag() {
        return TerminalTags.tag(Tag);
    }

    public static TerminalPhase fromConnectStatus(String Key) {
        if (Key == null) {
            return Connect;
        }
        return switch (Key) {
            case "connect.authorizing" -> Auth;
            case "connect.joining" -> Join;
            case "connect.reconfiguring" -> Sync;
            default -> Connect;
        };
    }

    public static TerminalPhase fromMessageKey(String Key) {
        if (Key == null) {
            return Prepare;
        }
        return switch (Key) {
            case "menu.savingLevel", "menu.savingChunks" -> Save;
            default -> Prepare;
        };
    }
}
