package net.rai.scp_unleashed.reactor;

import net.minecraft.ChatFormatting;

public enum GridState {
    Ready("ready", ChatFormatting.GREEN),
    Incomplete("incomplete", ChatFormatting.GOLD),
    Oversized("oversized", ChatFormatting.RED),
    Missing("missing", ChatFormatting.GRAY);

    private static final GridState[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    GridState(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static GridState byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Missing;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.reactor.grid." + Name;
    }
}
