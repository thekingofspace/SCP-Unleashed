package net.rai.scp_unleashed.rifttech;

import net.minecraft.util.StringRepresentable;

public enum CollectorMode implements StringRepresentable {
    Off("off"),
    Idle("idle"),
    Active("active");

    private final String Name;

    CollectorMode(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
