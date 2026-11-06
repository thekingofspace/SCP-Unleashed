package net.rai.scp_unleashed.scp082;

import net.minecraft.ChatFormatting;

public enum Scp082Mood {
    Jovial("jovial", ChatFormatting.GOLD, 0.98f),
    Irritable("irritable", ChatFormatting.YELLOW, 0.92f),
    Ravenous("ravenous", ChatFormatting.RED, 0.86f),
    Starving("starving", ChatFormatting.DARK_RED, 0.8f);

    public static final float SatedLine = 60.0f;
    public static final float IrritableLine = 35.0f;
    public static final float StarvingLine = 15.0f;

    public final String Key;
    public final ChatFormatting Colour;
    public final float VoicePitch;

    Scp082Mood(String Key, ChatFormatting Colour, float VoicePitch) {
        this.Key = Key;
        this.Colour = Colour;
        this.VoicePitch = VoicePitch;
    }

    public static Scp082Mood of(float Hunger) {
        if (Hunger >= SatedLine) {
            return Jovial;
        }
        if (Hunger >= IrritableLine) {
            return Irritable;
        }
        if (Hunger >= StarvingLine) {
            return Ravenous;
        }
        return Starving;
    }

    public static Scp082Mood byIndex(int Index) {
        Scp082Mood[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Index))];
    }

    public boolean hungry() {
        return this != Jovial;
    }

    public boolean violent() {
        return this == Ravenous || this == Starving;
    }
}
