package net.rai.scp_unleashed.scp939.compressor;

import net.minecraft.ChatFormatting;

public enum CompressorStatus {
    Idle("idle", ChatFormatting.GRAY, 0),
    Running("running", ChatFormatting.GREEN, 1),
    Underpowered("underpowered", ChatFormatting.GOLD, 2),
    Unpowered("unpowered", ChatFormatting.RED, 3),
    Blocked("blocked", ChatFormatting.GOLD, 4),
    Thin("thin", ChatFormatting.GRAY, 2);

    private final String Name;
    private final ChatFormatting Colour;
    private final int Lamp;

    CompressorStatus(String Name, ChatFormatting Colour, int Lamp) {
        this.Name = Name;
        this.Colour = Colour;
        this.Lamp = Lamp;
    }

    public static CompressorStatus byIndex(int Index) {
        CompressorStatus[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Idle;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public int getLamp() {
        return Lamp;
    }

    public String getTranslationKey() {
        return "container.scp_unleashed.gas_compressor.status." + Name;
    }
}
