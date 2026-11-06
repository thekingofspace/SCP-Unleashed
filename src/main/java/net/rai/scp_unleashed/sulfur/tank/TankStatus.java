package net.rai.scp_unleashed.sulfur.tank;

import net.minecraft.ChatFormatting;

public enum TankStatus {
    Idle("idle", ChatFormatting.GRAY, -1),
    NoSulfur("no_sulfur", ChatFormatting.GOLD, 1),
    NoWater("no_water", ChatFormatting.GOLD, 1),
    Full("full", ChatFormatting.GOLD, 1),
    Running("running", ChatFormatting.GREEN, 0),
    Underpowered("underpowered", ChatFormatting.GOLD, 1),
    Unpowered("unpowered", ChatFormatting.RED, 2);

    private final String Name;
    private final ChatFormatting Colour;
    private final int Lamp;

    TankStatus(String Name, ChatFormatting Colour, int Lamp) {
        this.Name = Name;
        this.Colour = Colour;
        this.Lamp = Lamp;
    }

    public static TankStatus byIndex(int Index) {
        TankStatus[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Idle;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public int getLamp() {
        return Lamp;
    }

    public boolean isWorking() {
        return this == Running || this == Underpowered;
    }

    public boolean isWaiting() {
        return this == NoSulfur || this == NoWater || this == Full;
    }

    public String getTranslationKey() {
        return "container.scp_unleashed.industrial_tank.status." + Name;
    }
}
