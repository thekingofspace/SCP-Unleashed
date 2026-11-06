package net.rai.scp_unleashed.zoneprops;

import net.minecraft.util.StringRepresentable;

public enum PairPart implements StringRepresentable {
    Main("main"),
    Extension("extension");

    private final String Name;

    PairPart(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
