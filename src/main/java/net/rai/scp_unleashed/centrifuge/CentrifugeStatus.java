package net.rai.scp_unleashed.centrifuge;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum CentrifugeStatus implements StringRepresentable {
    Idle("idle", ChatFormatting.GRAY),
    Spinning("spinning", ChatFormatting.GREEN),
    LowPower("low_power", ChatFormatting.GOLD),
    Unpowered("unpowered", ChatFormatting.RED),
    Blocked("blocked", ChatFormatting.GOLD);

    private static final CentrifugeStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    CentrifugeStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static CentrifugeStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Idle;
    }

    public static CentrifugeStatus byName(String Wanted) {
        for (CentrifugeStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Idle;
    }

    public boolean isWorking() {
        return this == Spinning || this == LowPower;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "container.scp_unleashed.centrifuge.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
