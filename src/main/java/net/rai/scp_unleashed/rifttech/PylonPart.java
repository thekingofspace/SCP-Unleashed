package net.rai.scp_unleashed.rifttech;

import net.minecraft.util.StringRepresentable;

public enum PylonPart implements StringRepresentable {
    Base("base", 0),
    Shaft("shaft", 1),
    Crown("crown", 2);

    private final String Name;
    private final int Offset;

    PylonPart(String Name, int Offset) {
        this.Name = Name;
        this.Offset = Offset;
    }

    public int getOffset() {
        return Offset;
    }

    public static PylonPart byOffset(int Offset) {
        for (PylonPart Part : values()) {
            if (Part.Offset == Offset) {
                return Part;
            }
        }
        return Base;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
