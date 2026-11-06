package net.rai.scp_unleashed.rifttech;

import net.minecraft.util.StringRepresentable;

public enum PylonMode implements StringRepresentable {
    Off("off"),
    Idle("idle"),
    Linked("linked"),
    Active("active"),
    Apocalyptic("apocalyptic");

    private final String Name;

    PylonMode(String Name) {
        this.Name = Name;
    }

    public boolean isCharging() {
        return this == Active || this == Apocalyptic;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
