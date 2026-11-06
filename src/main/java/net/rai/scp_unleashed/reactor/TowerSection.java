package net.rai.scp_unleashed.reactor;

import net.minecraft.util.StringRepresentable;

public enum TowerSection implements StringRepresentable {
    Single("single", true, true),
    Bottom("bottom", true, false),
    Middle("middle", false, false),
    Top("top", false, true);

    private final String Name;
    private final boolean Lowest;
    private final boolean Highest;

    TowerSection(String Name, boolean Lowest, boolean Highest) {
        this.Name = Name;
        this.Lowest = Lowest;
        this.Highest = Highest;
    }

    public static TowerSection of(boolean Below, boolean Above) {
        if (Below) {
            return Above ? Middle : Top;
        }
        return Above ? Bottom : Single;
    }

    public boolean isLowest() {
        return Lowest;
    }

    public boolean isHighest() {
        return Highest;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
