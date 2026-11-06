package net.rai.scp_unleashed.oil;

import net.minecraft.ChatFormatting;

public enum OilStatus {
    Idle("idle", ChatFormatting.GRAY, 0),
    Working("working", ChatFormatting.GREEN, 1),
    LowPower("low_power", ChatFormatting.GOLD, 2),
    Unpowered("unpowered", ChatFormatting.RED, 3),
    NeedsBedrock("needs_bedrock", ChatFormatting.RED, 3),
    OutputFull("output_full", ChatFormatting.GOLD, 2),
    Waiting("waiting", ChatFormatting.GRAY, 0),
    NoFuel("no_fuel", ChatFormatting.GRAY, 0),
    ExhaustFull("exhaust_full", ChatFormatting.RED, 3),
    BufferFull("buffer_full", ChatFormatting.GRAY, 1);

    private static final OilStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;
    private final int Lamp;

    OilStatus(String Name, ChatFormatting Colour, int Lamp) {
        this.Name = Name;
        this.Colour = Colour;
        this.Lamp = Lamp;
    }

    public static OilStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Idle;
    }

    public static OilStatus byName(String Wanted) {
        for (OilStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Idle;
    }

    public boolean isWorking() {
        return this == Working || this == LowPower;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public int getLamp() {
        return Lamp;
    }

    public String getSerializedName() {
        return Name;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.oil.status." + Name;
    }
}
