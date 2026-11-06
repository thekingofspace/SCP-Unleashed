package net.rai.scp_unleashed.warhead;

import net.minecraft.util.StringRepresentable;

public enum WarheadStatus implements StringRepresentable {
    Idle("idle"),
    Armed("armed"),
    Counting("counting"),
    Locked("locked");

    private final String Name;

    WarheadStatus(String Name) {
        this.Name = Name;
    }

    public boolean isActive() {
        return this == Counting || this == Locked;
    }

    public static WarheadStatus byIndex(int Index) {
        WarheadStatus[] Values = values();
        return Values[Math.floorMod(Index, Values.length)];
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
