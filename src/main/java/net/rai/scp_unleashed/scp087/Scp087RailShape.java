package net.rai.scp_unleashed.scp087;

import net.minecraft.util.StringRepresentable;

public enum Scp087RailShape implements StringRepresentable {
    Slope("slope"),
    SlopePost("slope_post"),
    SlopeTop("slope_top"),
    SlopeBottom("slope_bottom"),
    Flat("flat"),
    FlatPost("flat_post"),
    CornerIn("corner_in"),
    CornerOut("corner_out");

    private final String SerializedName;

    Scp087RailShape(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }

    public boolean flat() {
        return this == Flat || this == FlatPost || this == CornerIn || this == CornerOut;
    }
}
