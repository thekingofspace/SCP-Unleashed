package net.rai.scp_unleashed.instability;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum ReactorStatus implements StringRepresentable {
    Off("off", ChatFormatting.GRAY),
    Idle("idle", ChatFormatting.YELLOW),
    Running("running", ChatFormatting.GREEN);

    private final String Name;
    private final ChatFormatting Colour;

    ReactorStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static ReactorStatus byIndex(int Index) {
        ReactorStatus[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Index))];
    }

    public boolean isLit() {
        return this != Off;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "container.scp_unleashed.instability_reactor.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
