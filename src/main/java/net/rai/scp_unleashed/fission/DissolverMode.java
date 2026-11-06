package net.rai.scp_unleashed.fission;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum DissolverMode implements StringRepresentable {
    Empty("empty", ChatFormatting.RED),
    Idle("idle", ChatFormatting.GRAY),
    Working("working", ChatFormatting.GREEN);

    private static final DissolverMode[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    DissolverMode(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static DissolverMode byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Empty;
    }

    public static DissolverMode byName(String Wanted) {
        for (DissolverMode Mode : Values) {
            if (Mode.Name.equals(Wanted)) {
                return Mode;
            }
        }
        return Empty;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.gas_dissolver.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
