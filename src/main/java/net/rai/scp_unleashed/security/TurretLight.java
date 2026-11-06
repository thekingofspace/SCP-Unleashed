package net.rai.scp_unleashed.security;

import net.minecraft.util.StringRepresentable;

public enum TurretLight implements StringRepresentable {
    Off("off"),
    Ready("ready"),
    Alert("alert"),
    Fault("fault");

    private final String Name;

    TurretLight(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
