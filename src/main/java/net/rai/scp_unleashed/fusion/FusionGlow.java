package net.rai.scp_unleashed.fusion;

import net.minecraft.util.StringRepresentable;

public enum FusionGlow implements StringRepresentable {
    Off("off"),
    Charging("charging"),
    Igniting("igniting"),
    Running("running"),
    Fading("fading");

    private final String Name;

    FusionGlow(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
