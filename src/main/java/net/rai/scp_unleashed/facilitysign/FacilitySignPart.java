package net.rai.scp_unleashed.facilitysign;

import net.minecraft.util.StringRepresentable;

public enum FacilitySignPart implements StringRepresentable {
    Left("left"),
    Right("right");

    private final String SerializedName;

    FacilitySignPart(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    public FacilitySignPart other() {
        return this == Left ? Right : Left;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }
}
