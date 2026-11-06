package net.rai.scp_unleashed.tesla;

public enum TeslaPhase {
    Idle,
    Charged,
    WindUp,
    Discharge,
    Cooldown;

    private static final TeslaPhase[] Values = values();

    public static TeslaPhase byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Idle;
    }

    public boolean isActive() {
        return this != Idle;
    }
}
