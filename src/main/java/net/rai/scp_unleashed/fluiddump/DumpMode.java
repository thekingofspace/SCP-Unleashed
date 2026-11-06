package net.rai.scp_unleashed.fluiddump;

import net.minecraft.ChatFormatting;

public enum DumpMode {
    Idle("idle", ChatFormatting.GRAY, false),
    Pouring("pouring", ChatFormatting.AQUA, false),
    Blocked("blocked", ChatFormatting.GOLD, false),
    Quenching("quenching", ChatFormatting.GOLD, true),
    Polluting("polluting", ChatFormatting.RED, true),
    Absorbing("absorbing", ChatFormatting.AQUA, true);

    private final String Key;
    private final ChatFormatting Colour;
    private final boolean Submerged;

    DumpMode(String Key, ChatFormatting Colour, boolean Submerged) {
        this.Key = Key;
        this.Colour = Colour;
        this.Submerged = Submerged;
    }

    public String getKey() {
        return Key;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public boolean isSubmerged() {
        return Submerged;
    }

    public boolean isWorking() {
        return this != Idle && this != Blocked;
    }

    public static DumpMode byIndex(int Index) {
        DumpMode[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Idle;
    }
}
