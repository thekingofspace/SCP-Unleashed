package net.rai.scp_unleashed.tesla;

public enum TeslaStatus {
    Off("off"),
    Ready("ready"),
    Maintenance("maintenance"),
    Fault("fault"),
    Armed("armed");

    private static final TeslaStatus[] Values = values();

    private final String Name;

    TeslaStatus(String Name) {
        this.Name = Name;
    }

    public static TeslaStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Off;
    }

    public String getSerializedName() {
        return Name;
    }
}
