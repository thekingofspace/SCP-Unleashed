package net.rai.scp_unleashed.scp179;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum Scp179Omen implements StringRepresentable {
    None("none", 0xFFC8C8C8),
    Beware("beware", 0xFFE8473A),
    Friend("friend", 0xFF5FE08A);

    private final String SerializedName;
    private final int Colour;

    Scp179Omen(String SerializedName, int Colour) {
        this.SerializedName = SerializedName;
        this.Colour = Colour;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }

    public int getColour() {
        return Colour;
    }

    public boolean isActive() {
        return this != None;
    }

    public Component word() {
        return Component.translatable("satellite.scp_unleashed.scp179.word." + SerializedName);
    }

    public static Scp179Omen byName(String Name) {
        for (Scp179Omen Value : values()) {
            if (Value.SerializedName.equals(Name)) {
                return Value;
            }
        }
        return None;
    }

    public static Scp179Omen byId(int Id) {
        Scp179Omen[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Id))];
    }
}
