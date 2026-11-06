package net.rai.scp_unleashed.microhid;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum MicroChargingStatus implements StringRepresentable {
    Empty("empty", ChatFormatting.GRAY),
    Unpowered("unpowered", ChatFormatting.RED),
    Charging("charging", ChatFormatting.AQUA),
    Full("full", ChatFormatting.GREEN);

    private final String Name;
    private final ChatFormatting Colour;

    MicroChargingStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public String getTranslationKey() {
        return "block.scp_unleashed.micro_charging_platform.status." + Name;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public static MicroChargingStatus byIndex(int Index) {
        MicroChargingStatus[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }
}
