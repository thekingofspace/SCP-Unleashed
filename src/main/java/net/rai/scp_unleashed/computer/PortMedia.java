package net.rai.scp_unleashed.computer;

import net.minecraft.util.StringRepresentable;

public enum PortMedia implements StringRepresentable {
    Empty("empty"),
    Floppy("floppy"),
    Dossier("dossier");

    private final String SerializedName;

    PortMedia(String SerializedName) {
        this.SerializedName = SerializedName;
    }

    public static PortMedia byId(int Id) {
        PortMedia[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Id))];
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }
}
