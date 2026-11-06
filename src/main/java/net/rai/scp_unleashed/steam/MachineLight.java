package net.rai.scp_unleashed.steam;

import net.minecraft.util.StringRepresentable;

public enum MachineLight implements StringRepresentable {
    Off("off"),
    Working("working"),
    Waiting("waiting"),
    Alert("alert");

    private final String Name;

    MachineLight(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
