package net.rai.scp_unleashed.facilitysign;

import net.minecraft.util.StringRepresentable;

public enum FacilitySignMount implements StringRepresentable {
    Wall("wall"),
    Ceiling("ceiling");

    private final String SerializedName;

    FacilitySignMount(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }
}
