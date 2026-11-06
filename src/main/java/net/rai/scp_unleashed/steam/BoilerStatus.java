package net.rai.scp_unleashed.steam;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum BoilerStatus implements StringRepresentable {
    Idle("idle", ChatFormatting.GRAY),
    Heating("heating", ChatFormatting.GOLD),
    Boiling("boiling", ChatFormatting.GREEN),
    Cooling("cooling", ChatFormatting.GOLD),
    NoWater("no_water", ChatFormatting.RED),
    Blocked("blocked", ChatFormatting.GOLD);

    private static final BoilerStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    BoilerStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static BoilerStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Idle;
    }

    public static BoilerStatus byName(String Wanted) {
        for (BoilerStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Idle;
    }

    public boolean isBoiling() {
        return this == Heating || this == Boiling || this == Cooling;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.water_boiler.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
