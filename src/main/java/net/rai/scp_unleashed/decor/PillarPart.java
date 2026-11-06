package net.rai.scp_unleashed.decor;

import net.minecraft.util.StringRepresentable;

public enum PillarPart implements StringRepresentable {
    Single("single"),
    Bottom("bottom"),
    Middle("middle"),
    Top("top");

    private final String Name;

    PillarPart(String Name) {
        this.Name = Name;
    }

    public static PillarPart of(boolean JoinedBelow, boolean JoinedAbove) {
        if (JoinedBelow) {
            return JoinedAbove ? Middle : Top;
        }
        return JoinedAbove ? Bottom : Single;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
