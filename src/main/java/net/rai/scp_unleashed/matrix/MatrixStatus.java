package net.rai.scp_unleashed.matrix;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum MatrixStatus implements StringRepresentable {
    Offline("offline", ChatFormatting.GRAY, MatrixMode.Off),
    Unpowered("unpowered", ChatFormatting.RED, MatrixMode.Idle),
    LowPower("low_power", ChatFormatting.GOLD, MatrixMode.Active),
    Resonating("resonating", ChatFormatting.GREEN, MatrixMode.Active),
    Saturated("saturated", ChatFormatting.AQUA, MatrixMode.Idle),
    Obstructed("obstructed", ChatFormatting.GOLD, MatrixMode.Idle),
    Dormant("dormant", ChatFormatting.GRAY, MatrixMode.Idle);

    private static final MatrixStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;
    private final MatrixMode Mode;

    MatrixStatus(String Name, ChatFormatting Colour, MatrixMode Mode) {
        this.Name = Name;
        this.Colour = Colour;
        this.Mode = Mode;
    }

    public static MatrixStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Offline;
    }

    public static MatrixStatus byName(String Wanted) {
        for (MatrixStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Offline;
    }

    public boolean isRunning() {
        return Mode == MatrixMode.Active;
    }

    public boolean isHolding() {
        return this == Saturated || this == Obstructed;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public MatrixMode getMode() {
        return Mode;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.instability_matrix.status." + Name;
    }

    public String getHintKey() {
        return "gui.scp_unleashed.instability_matrix.status." + Name + ".hint";
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
