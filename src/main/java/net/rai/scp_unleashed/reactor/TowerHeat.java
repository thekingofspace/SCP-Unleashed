package net.rai.scp_unleashed.reactor;

import net.minecraft.ChatFormatting;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;

public enum TowerHeat {
    Stable("stable", ChatFormatting.GREEN, 0xFF7CF0A8),
    Hot("hot", ChatFormatting.GOLD, 0xFFFFC452),
    Critical("critical", ChatFormatting.RED, 0xFFFF6A5A),
    Breach("breach", ChatFormatting.DARK_RED, 0xFFFF3030);

    private final String Name;
    private final ChatFormatting Colour;
    private final int ScreenColour;

    TowerHeat(String Name, ChatFormatting Colour, int ScreenColour) {
        this.Name = Name;
        this.Colour = Colour;
        this.ScreenColour = ScreenColour;
    }

    public static TowerHeat of(float Temperature, float Integrity) {
        if (Integrity <= ReactorTuning.BreachLevel) {
            return Breach;
        }
        if (Temperature >= ReactorTuning.CriticalLevel) {
            return Critical;
        }
        return Temperature >= ReactorTuning.HotLevel ? Hot : Stable;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public int getScreenColour() {
        return ScreenColour;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.reactor.heat." + Name;
    }

    public String getShortKey() {
        return "gui.scp_unleashed.reactor.band." + Name;
    }
}
