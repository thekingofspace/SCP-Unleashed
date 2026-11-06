package net.rai.scp_unleashed.advmaterials;

import net.minecraft.util.StringRepresentable;

public enum BenchLight implements StringRepresentable {
    Off("off"),
    Idle("idle"),
    Working("working"),
    Alert("alert");

    private final String Name;

    BenchLight(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
