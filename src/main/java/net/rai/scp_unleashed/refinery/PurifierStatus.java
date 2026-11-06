package net.rai.scp_unleashed.refinery;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum PurifierStatus implements StringRepresentable {
    Idle("idle", ChatFormatting.GRAY),
    Working("working", ChatFormatting.GREEN),
    LowPower("low_power", ChatFormatting.GOLD),
    Unpowered("unpowered", ChatFormatting.RED),
    NoWater("no_water", ChatFormatting.GOLD),
    WasteFull("waste_full", ChatFormatting.GOLD),
    Blocked("blocked", ChatFormatting.GOLD);

    private static final PurifierStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    PurifierStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static PurifierStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Idle;
    }

    public static PurifierStatus byName(String Wanted) {
        for (PurifierStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Idle;
    }

    public boolean isWorking() {
        return this == Working || this == LowPower;
    }

    public boolean isAlert() {
        return this == Unpowered || this == NoWater || this == WasteFull || this == Blocked;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "container.scp_unleashed.ore_purifier.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
