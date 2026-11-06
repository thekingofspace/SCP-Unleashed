package net.rai.scp_unleashed.processing;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum MachineStatus implements StringRepresentable {
    Idle("idle", ChatFormatting.GRAY),
    Running("running", ChatFormatting.GREEN),
    Underpowered("underpowered", ChatFormatting.GOLD),
    Unpowered("unpowered", ChatFormatting.RED),
    Blocked("blocked", ChatFormatting.GOLD);

    private final String Name;
    private final ChatFormatting Colour;

    MachineStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static MachineStatus byIndex(int Index) {
        MachineStatus[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : Idle;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "container.scp_unleashed.processing.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
