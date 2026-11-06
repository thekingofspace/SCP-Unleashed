package net.rai.scp_unleashed.steam;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum TurbineStatus implements StringRepresentable {
    Idle("idle", ChatFormatting.GRAY),
    Spinning("spinning", ChatFormatting.GREEN),
    Throttled("throttled", ChatFormatting.GOLD),
    Backed("backed", ChatFormatting.RED),
    NoLoad("no_load", ChatFormatting.GOLD);

    private static final TurbineStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    TurbineStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static TurbineStatus byName(String Wanted) {
        for (TurbineStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Idle;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.steam_turbine.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
