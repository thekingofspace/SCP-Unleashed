package net.rai.scp_unleashed.nethub;

import net.minecraft.util.StringRepresentable;

public enum HubStatus implements StringRepresentable {
    Off("off"),
    Standby("standby"),
    Bridging("bridging");

    private final String Name;

    HubStatus(String Name) {
        this.Name = Name;
    }

    public static HubStatus byIndex(int Index) {
        HubStatus[] Values = values();
        return Index >= 0 && Index < Values.length ? Values[Index] : Off;
    }

    public boolean isPowered() {
        return this != Off;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
