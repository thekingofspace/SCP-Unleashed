package net.rai.scp_unleashed.space;

import net.minecraft.ChatFormatting;

public enum AirInfuserStatus {
    Idle("idle", ChatFormatting.GRAY),
    Running("running", ChatFormatting.GREEN),
    Underpowered("underpowered", ChatFormatting.GOLD),
    Unpowered("unpowered", ChatFormatting.RED),
    Blocked("blocked", ChatFormatting.GOLD),
    NoAtmosphere("no_atmosphere", ChatFormatting.RED);

    private final String Name;
    private final ChatFormatting Colour;

    AirInfuserStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static AirInfuserStatus byIndex(int Index) {
        AirInfuserStatus[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Idle;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "container.scp_unleashed.air_infuser.status." + Name;
    }
}
