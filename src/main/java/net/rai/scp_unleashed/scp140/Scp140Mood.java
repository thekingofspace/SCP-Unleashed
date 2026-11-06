package net.rai.scp_unleashed.scp140;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum Scp140Mood {
    Dry("dry", ChatFormatting.GRAY),
    Glistens("glistens", ChatFormatting.RED),
    Fresh("fresh", ChatFormatting.DARK_RED),
    Complete("complete", ChatFormatting.DARK_RED);

    private final String Name;
    private final ChatFormatting Colour;

    Scp140Mood(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public String getName() {
        return Name;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public Component describe() {
        return Component.translatable("jade.scp_unleashed.scp140_chronicle." + Name).withStyle(Colour);
    }

    public static Scp140Mood byId(int Id) {
        Scp140Mood[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Id))];
    }
}
