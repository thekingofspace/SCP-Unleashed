package net.rai.scp_unleashed.faction;

import net.minecraft.util.StringRepresentable;

public enum FactionPhase implements StringRepresentable {
    Dormant("dormant"),
    Awakened("awakened"),
    Ascendant("ascendant"),
    Apex("apex"),
    Defeated("defeated");

    private final String Name;

    FactionPhase(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public boolean active() {
        return this == Awakened || this == Ascendant || this == Apex;
    }

    public static FactionPhase byName(String Text) {
        for (FactionPhase Phase : values()) {
            if (Phase.Name.equals(Text)) {
                return Phase;
            }
        }
        return Dormant;
    }
}
