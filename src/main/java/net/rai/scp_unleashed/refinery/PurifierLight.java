package net.rai.scp_unleashed.refinery;

import net.minecraft.util.StringRepresentable;

public enum PurifierLight implements StringRepresentable {
    Off("off"),
    Idle("idle"),
    Working("working"),
    Alert("alert");

    private final String Name;

    PurifierLight(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
