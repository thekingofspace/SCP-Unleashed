package net.rai.scp_unleashed.advmaterials;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum BenchStatus implements StringRepresentable {
    Idle("idle", ChatFormatting.GRAY),
    Working("working", ChatFormatting.GREEN),
    LowPower("low_power", ChatFormatting.GOLD),
    Unpowered("unpowered", ChatFormatting.RED),
    Blocked("blocked", ChatFormatting.GOLD);

    private static final BenchStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    BenchStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static BenchStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Idle;
    }

    public static BenchStatus byName(String Wanted) {
        for (BenchStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Idle;
    }

    public boolean isWorking() {
        return this == Working || this == LowPower;
    }

    public boolean isAlert() {
        return this == Unpowered || this == Blocked;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "container.scp_unleashed.advanced_materials_bench.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
