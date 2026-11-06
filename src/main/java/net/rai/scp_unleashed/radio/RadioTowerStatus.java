package net.rai.scp_unleashed.radio;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum RadioTowerStatus implements StringRepresentable {
    Online("online", ChatFormatting.GREEN),
    Unpowered("unpowered", ChatFormatting.RED),
    NoMast("no_mast", ChatFormatting.GOLD);

    private final String Name;
    private final ChatFormatting Colour;

    RadioTowerStatus(String Name, ChatFormatting Colour) {
        this.Name = Name;
        this.Colour = Colour;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public String key() {
        return "radio.scp_unleashed.tower.status." + Name;
    }

    public ChatFormatting colour() {
        return Colour;
    }

    public static RadioTowerStatus byName(String Text) {
        for (RadioTowerStatus Each : values()) {
            if (Each.Name.equals(Text)) {
                return Each;
            }
        }
        return Unpowered;
    }
}
