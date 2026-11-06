package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;

public enum LabAnalyzerStatus {
    Idle("idle", ChatFormatting.GRAY),
    Unpowered("unpowered", ChatFormatting.RED),
    Underpowered("underpowered", ChatFormatting.GOLD),
    Working("working", ChatFormatting.AQUA),
    Blocked("blocked", ChatFormatting.GOLD);

    private static final LabAnalyzerStatus[] Values = values();

    private final String Key;
    private final ChatFormatting Colour;

    LabAnalyzerStatus(String Key, ChatFormatting Colour) {
        this.Key = Key;
        this.Colour = Colour;
    }

    public static LabAnalyzerStatus byIndex(int Index) {
        return Values[Math.max(0, Math.min(Values.length - 1, Index))];
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.lab_analyzer.status." + Key;
    }

    public String getHintKey() {
        return "gui.scp_unleashed.lab_analyzer.hint." + Key;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public boolean isRunning() {
        return this == Working || this == Underpowered;
    }
}
