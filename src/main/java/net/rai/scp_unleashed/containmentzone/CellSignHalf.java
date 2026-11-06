package net.rai.scp_unleashed.containmentzone;

import net.minecraft.util.StringRepresentable;

public enum CellSignHalf implements StringRepresentable {
    Left("left"),
    Right("right");

    private final String Name;

    CellSignHalf(String Name) {
        this.Name = Name;
    }

    public CellSignHalf other() {
        return this == Left ? Right : Left;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
