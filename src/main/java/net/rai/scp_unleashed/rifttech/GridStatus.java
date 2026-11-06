package net.rai.scp_unleashed.rifttech;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum GridStatus implements StringRepresentable {
    Unpowered("unpowered", ChatFormatting.RED, PylonMode.Off),
    Searching("searching", ChatFormatting.GOLD, PylonMode.Idle),
    CornerUnpowered("corner_unpowered", ChatFormatting.RED, PylonMode.Idle),
    Conflict("conflict", ChatFormatting.GOLD, PylonMode.Idle),
    Offline("offline", ChatFormatting.GRAY, PylonMode.Linked),
    Charging("charging", ChatFormatting.GREEN, PylonMode.Active),
    LowPower("low_power", ChatFormatting.GOLD, PylonMode.Active),
    Saturated("saturated", ChatFormatting.AQUA, PylonMode.Linked),
    Obstructed("obstructed", ChatFormatting.GOLD, PylonMode.Linked),
    Apocalyptic("apocalyptic", ChatFormatting.YELLOW, PylonMode.Apocalyptic),
    Starved("starved", ChatFormatting.GOLD, PylonMode.Linked),
    Holding("holding", ChatFormatting.YELLOW, PylonMode.Linked),
    Dormant("dormant", ChatFormatting.GRAY, PylonMode.Linked);

    private static final GridStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;
    private final PylonMode Mode;

    GridStatus(String Name, ChatFormatting Colour, PylonMode Mode) {
        this.Name = Name;
        this.Colour = Colour;
        this.Mode = Mode;
    }

    public static GridStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Unpowered;
    }

    public static GridStatus byName(String Wanted) {
        for (GridStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Unpowered;
    }

    public boolean isFormed() {
        return ordinal() >= Offline.ordinal();
    }

    public boolean isCharging() {
        return Mode.isCharging();
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public PylonMode getMode() {
        return Mode;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.instability_pylon.status." + Name;
    }

    public String getHintKey() {
        return "gui.scp_unleashed.instability_pylon.status." + Name + ".hint";
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
