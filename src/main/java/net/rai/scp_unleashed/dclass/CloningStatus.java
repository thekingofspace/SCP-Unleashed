package net.rai.scp_unleashed.dclass;

import java.util.Locale;

public enum CloningStatus {
    Off,
    Idle,
    TrayFull,
    Growing,
    Blocked,
    Releasing;

    public String getKey() {
        return name().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    public CloningLamp lamp() {
        return switch (this) {
            case Off -> CloningLamp.Off;
            case Idle -> CloningLamp.Ready;
            case Growing, Releasing -> CloningLamp.Working;
            case TrayFull, Blocked -> CloningLamp.Alert;
        };
    }

    public static CloningStatus byIndex(int Index) {
        CloningStatus[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Off;
    }
}
