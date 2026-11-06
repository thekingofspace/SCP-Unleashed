package net.rai.scp_unleashed.fission;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;

public enum ReactorThermal {
    Normal("normal", ChatFormatting.GREEN),
    Warning("warning", ChatFormatting.GOLD),
    Critical("critical", ChatFormatting.RED);

    public static final float Ambient = 20.0f;
    public static final float WarningLevel = 900.0f;
    public static final float CriticalLevel = 1200.0f;
    public static final float MeltdownLevel = 1500.0f;

    private static final ReactorThermal[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    ReactorThermal(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static ReactorThermal of(float Temperature) {
        if (Temperature >= CriticalLevel) {
            return Critical;
        }
        return Temperature >= WarningLevel ? Warning : Normal;
    }

    public static ReactorThermal byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Normal;
    }

    public static float fraction(float Temperature) {
        return Mth.clamp((Temperature - Ambient) / (MeltdownLevel - Ambient), 0.0f, 1.0f);
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.fission_reactor.thermal." + Name;
    }

    public String getSerializedName() {
        return Name;
    }
}
