package net.rai.scp_unleashed.door;

import net.minecraft.util.StringRepresentable;

public enum ScanStatus implements StringRepresentable {
    Idle("idle"),
    Granted("granted"),
    Denied("denied");

    private final String Name;

    ScanStatus(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
