package net.rai.scp_unleashed.facilitysign;

import net.minecraft.util.StringRepresentable;

public enum FacilityHeaderPart implements StringRepresentable {
    Left("left"),
    Centre("centre"),
    Right("right");

    private final String SerializedName;

    FacilityHeaderPart(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    public FacilityHeaderPart mirrored() {
        return this == Left ? Right : this == Right ? Left : this;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }
}
