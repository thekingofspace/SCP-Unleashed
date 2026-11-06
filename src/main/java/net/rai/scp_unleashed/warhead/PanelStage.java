package net.rai.scp_unleashed.warhead;

import net.minecraft.util.StringRepresentable;

public enum PanelStage implements StringRepresentable {
    Sealed("sealed"),
    Unlocked("unlocked"),
    Armed("armed"),
    Ready("ready"),
    Counting("counting"),
    Locked("locked");

    private final String Name;

    PanelStage(String Name) {
        this.Name = Name;
    }

    public boolean isArmed() {
        return ordinal() >= Armed.ordinal();
    }

    public boolean isActive() {
        return this == Counting || this == Locked;
    }

    public static PanelStage byIndex(int Index) {
        PanelStage[] Values = values();
        return Values[Math.floorMod(Index, Values.length)];
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
