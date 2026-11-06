package net.rai.scp_unleashed.storage.gel;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum GelStatus implements StringRepresentable {
    Idle("idle", ChatFormatting.GRAY),
    Forming("forming", ChatFormatting.GOLD),
    Sealed("sealed", ChatFormatting.GREEN),
    Failing("failing", ChatFormatting.RED);

    private final String Name;
    private final ChatFormatting Colour;

    GelStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public ChatFormatting colour() {
        return Colour;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
