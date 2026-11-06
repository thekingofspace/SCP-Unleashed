package net.rai.scp_unleashed.rocketry.platform;

import net.minecraft.ChatFormatting;

public enum LaunchStatus {
    Empty("empty", ChatFormatting.GRAY, 0),
    NotReady("not_ready", ChatFormatting.GOLD, 2),
    Ready("ready", ChatFormatting.GREEN, 1),
    Launching("launching", ChatFormatting.AQUA, 1);

    private static final LaunchStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;
    private final int Lamp;

    LaunchStatus(String Name, ChatFormatting Colour, int Lamp) {
        this.Name = Name;
        this.Colour = Colour;
        this.Lamp = Lamp;
    }

    public static LaunchStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Empty;
    }

    public static LaunchStatus byName(String Wanted) {
        for (LaunchStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Empty;
    }

    public String getSerializedName() {
        return Name;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public int getLamp() {
        return Lamp;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.rocketry.status." + Name;
    }
}
