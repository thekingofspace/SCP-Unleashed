package net.rai.scp_unleashed.steam;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum TowerStatus implements StringRepresentable {
    Idle("idle", ChatFormatting.GRAY),
    Condensing("condensing", ChatFormatting.GREEN),
    Full("full", ChatFormatting.GOLD);

    private static final TowerStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    TowerStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static TowerStatus byName(String Wanted) {
        for (TowerStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Idle;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.cooling_tower.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
