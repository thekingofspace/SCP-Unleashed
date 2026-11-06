package net.rai.scp_unleashed.facilitysign;

import net.minecraft.util.StringRepresentable;

public enum FacilitySignArrow implements StringRepresentable {
    None("none"),
    Left("left"),
    Right("right"),
    Up("up"),
    Down("down");

    private static final FacilitySignArrow[] Cycle = {Right, Down, Left, Up, None};

    private final String SerializedName;

    FacilitySignArrow(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    public FacilitySignArrow next() {
        return step(1);
    }

    public FacilitySignArrow previous() {
        return step(Cycle.length - 1);
    }

    private FacilitySignArrow step(int Offset) {
        for (int Index = 0; Index < Cycle.length; Index++) {
            if (Cycle[Index] == this) {
                return Cycle[(Index + Offset) % Cycle.length];
            }
        }
        return Right;
    }

    public FacilitySignArrow mirrored() {
        return this == Left ? Right : this == Right ? Left : this;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }
}
