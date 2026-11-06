package net.rai.scp_unleashed.scpsign;

import net.minecraft.util.StringRepresentable;

public enum ScpSignPart implements StringRepresentable {
    Left("left"),
    Right("right");

    private final String SerializedName;

    ScpSignPart(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    public ScpSignPart other() {
        return this == Left ? Right : Left;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }
}
