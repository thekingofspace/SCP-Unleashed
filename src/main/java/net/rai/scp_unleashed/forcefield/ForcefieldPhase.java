package net.rai.scp_unleashed.forcefield;

public enum ForcefieldPhase {
    Offline,
    Booting,
    Online,
    Collapsing;

    public static ForcefieldPhase byIndex(int Index) {
        ForcefieldPhase[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Offline;
    }

    public boolean raised() {
        return this == Online || this == Collapsing;
    }
}
