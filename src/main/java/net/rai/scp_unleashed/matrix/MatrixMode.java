package net.rai.scp_unleashed.matrix;

import net.minecraft.util.StringRepresentable;

public enum MatrixMode implements StringRepresentable {
    Off("off"),
    Idle("idle"),
    Active("active");

    private final String Name;

    MatrixMode(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
