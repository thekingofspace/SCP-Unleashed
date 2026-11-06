package net.rai.scp_unleashed.fission;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum FissionStatus implements StringRepresentable {
    Offline("offline", ChatFormatting.GRAY),
    Running("running", ChatFormatting.GREEN),
    LoadFollowing("load_following", ChatFormatting.AQUA),
    WasteFull("waste_full", ChatFormatting.GOLD),
    NoFuel("no_fuel", ChatFormatting.GOLD),
    SpentFull("spent_full", ChatFormatting.GOLD),
    Interlocked("interlocked", ChatFormatting.RED);

    private static final FissionStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;

    FissionStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    public static FissionStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Offline;
    }

    public static FissionStatus byName(String Wanted) {
        for (FissionStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Offline;
    }

    public boolean isOnline() {
        return this == Running || this == LoadFollowing || this == WasteFull;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.fission_reactor.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
