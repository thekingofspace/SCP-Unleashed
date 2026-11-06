package net.rai.scp_unleashed.gas;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum VaporizerStatus implements StringRepresentable {
    Idle("idle", ChatFormatting.GRAY),
    Working("working", ChatFormatting.GREEN),
    LowPower("low_power", ChatFormatting.GOLD),
    Unpowered("unpowered", ChatFormatting.RED),
    Blocked("blocked", ChatFormatting.GOLD),
    Short("short", ChatFormatting.GRAY);

    private static final VaporizerStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    VaporizerStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static VaporizerStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Idle;
    }

    public static VaporizerStatus byName(String Wanted) {
        for (VaporizerStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Idle;
    }

    public boolean isWorking() {
        return this == Working || this == LowPower;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.liquid_vaporizer.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
