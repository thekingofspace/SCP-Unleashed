package net.rai.scp_unleashed.rifttech;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum CollectorStatus implements StringRepresentable {
    Unpowered("unpowered", ChatFormatting.RED, CollectorMode.Off),
    Scanning("scanning", ChatFormatting.GRAY, CollectorMode.Idle),
    Blocked("blocked", ChatFormatting.GOLD, CollectorMode.Idle),
    Tracking("tracking", ChatFormatting.AQUA, CollectorMode.Idle),
    Collecting("collecting", ChatFormatting.GREEN, CollectorMode.Active),
    LowPower("low_power", ChatFormatting.GOLD, CollectorMode.Idle),
    Full("full", ChatFormatting.GOLD, CollectorMode.Idle);

    private static final CollectorStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;
    private final CollectorMode Mode;

    CollectorStatus(String Name, ChatFormatting Colour, CollectorMode Mode) {
        this.Name = Name;
        this.Colour = Colour;
        this.Mode = Mode;
    }

    public static CollectorStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Unpowered;
    }

    public static CollectorStatus byName(String Wanted) {
        for (CollectorStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Unpowered;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public CollectorMode getMode() {
        return Mode;
    }

    public boolean isPowered() {
        return this != Unpowered;
    }

    public boolean hasTarget() {
        return this == Tracking || this == Collecting || this == LowPower;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.rift_collector.status." + Name;
    }

    public String getHintKey() {
        return "gui.scp_unleashed.rift_collector.status." + Name + ".hint";
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
