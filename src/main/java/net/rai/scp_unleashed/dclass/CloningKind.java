package net.rai.scp_unleashed.dclass;

public enum CloningKind {
    None(false),
    Personnel(false),
    TheodoreLine(false),
    PlayerLine(false);

    private final boolean CreativeOnly;

    CloningKind(boolean CreativeOnly) {
        this.CreativeOnly = CreativeOnly;
    }

    public boolean creativeOnly() {
        return CreativeOnly;
    }

    public String getKey() {
        return switch (this) {
            case TheodoreLine -> "theodore";
            case PlayerLine -> "player";
            default -> "dclass";
        };
    }

    public static CloningKind byIndex(int Index) {
        CloningKind[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : None;
    }
}
