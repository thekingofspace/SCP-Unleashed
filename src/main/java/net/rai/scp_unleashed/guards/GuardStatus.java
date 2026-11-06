package net.rai.scp_unleashed.guards;

import net.minecraft.ChatFormatting;

public enum GuardStatus {
    Patrolling("patrolling", ChatFormatting.GREEN),
    Standing("standing", ChatFormatting.AQUA),
    Engaging("engaging", ChatFormatting.RED),
    Returning("returning", ChatFormatting.YELLOW),
    Blocked("blocked", ChatFormatting.GOLD),
    Elsewhere("elsewhere", ChatFormatting.DARK_GRAY);

    private final String Key;
    private final ChatFormatting Colour;

    GuardStatus(String Key, ChatFormatting Colour) {
        this.Key = Key;
        this.Colour = Colour;
    }

    public String getKey() {
        return Key;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "jade.scp_unleashed.patrol_guard.status." + Key;
    }

    public static GuardStatus byIndex(int Index) {
        GuardStatus[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }
}
