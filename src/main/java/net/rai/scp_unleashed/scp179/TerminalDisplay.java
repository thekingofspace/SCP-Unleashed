package net.rai.scp_unleashed.scp179;

import net.minecraft.util.StringRepresentable;

public enum TerminalDisplay implements StringRepresentable {
    Off("off", 0),
    NoLink("nolink", 4),
    On("on", 6),
    Alert("alert", 8);

    private final String SerializedName;
    private final int Light;

    TerminalDisplay(String SerializedName, int Light) {
        this.SerializedName = SerializedName;
        this.Light = Light;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }

    public int getLight() {
        return Light;
    }
}
