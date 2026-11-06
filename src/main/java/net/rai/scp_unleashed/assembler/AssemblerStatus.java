package net.rai.scp_unleashed.assembler;

import net.minecraft.util.StringRepresentable;

public enum AssemblerStatus implements StringRepresentable {
    Off("off"),
    Idle("idle"),
    Working("working"),
    Blocked("blocked");

    private final String Name;

    AssemblerStatus(String Name) {
        this.Name = Name;
    }

    public static AssemblerStatus byIndex(int Index) {
        AssemblerStatus[] Values = values();
        return Index >= 0 && Index < Values.length ? Values[Index] : Off;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
