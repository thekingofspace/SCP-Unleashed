package net.rai.scp_unleashed.fusion;

import net.minecraft.ChatFormatting;

public enum FusionPhase {
    Offline("offline", ChatFormatting.GRAY, FusionGlow.Off),
    Charging("charging", ChatFormatting.AQUA, FusionGlow.Charging),
    Igniting("igniting", ChatFormatting.LIGHT_PURPLE, FusionGlow.Igniting),
    Running("running", ChatFormatting.GREEN, FusionGlow.Running),
    Quenching("quenching", ChatFormatting.GOLD, FusionGlow.Fading);

    private static final FusionPhase[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;
    private final FusionGlow Glow;

    FusionPhase(String Name, ChatFormatting Colour, FusionGlow Glow) {
        this.Name = Name;
        this.Colour = Colour;
        this.Glow = Glow;
    }

    public static FusionPhase byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Offline;
    }

    public String getSerializedName() {
        return Name;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public FusionGlow getGlow() {
        return Glow;
    }

    public boolean isActive() {
        return this != Offline;
    }

    public boolean isLit() {
        return this == Igniting || this == Running || this == Quenching;
    }

    public String getTranslationKey() {
        return "fusion.scp_unleashed.phase." + Name;
    }
}
