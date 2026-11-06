package net.rai.scp_unleashed.dclass;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum CloningLamp implements StringRepresentable {
    Off,
    Ready,
    Working,
    Alert;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
