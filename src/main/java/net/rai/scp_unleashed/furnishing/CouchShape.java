package net.rai.scp_unleashed.furnishing;

import net.minecraft.util.StringRepresentable;

public enum CouchShape implements StringRepresentable {
    Straight("straight"),
    CornerLeft("corner_left"),
    CornerRight("corner_right");

    private final String Name;

    CouchShape(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
