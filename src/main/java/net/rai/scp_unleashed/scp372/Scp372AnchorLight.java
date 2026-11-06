package net.rai.scp_unleashed.scp372;

import net.minecraft.util.StringRepresentable;

public enum Scp372AnchorLight implements StringRepresentable {
    Off("off"),
    Powered("powered"),
    Holding("holding");

    private final String Name;

    Scp372AnchorLight(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
