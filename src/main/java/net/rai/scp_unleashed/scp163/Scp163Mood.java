package net.rai.scp_unleashed.scp163;

import net.minecraft.ChatFormatting;

import java.util.Locale;

public enum Scp163Mood {
    Calm(ChatFormatting.GREEN, 1.0f, 1.0f),
    Distress(ChatFormatting.YELLOW, 0.94f, 1.12f),
    Anger(ChatFormatting.RED, 1.08f, 0.84f);

    public static final String SettledKey = "entity.scp_unleashed.scp163_castaway.mood.settled";

    private final ChatFormatting Colour;
    private final float Across;
    private final float Tall;

    Scp163Mood(ChatFormatting Colour, float Across, float Tall) {
        this.Colour = Colour;
        this.Across = Across;
        this.Tall = Tall;
    }

    public float across() {
        return Across;
    }

    public float tall() {
        return Tall;
    }

    public ChatFormatting colour() {
        return Colour;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "entity.scp_unleashed.scp163_castaway.mood." + key();
    }

    public static Scp163Mood byId(int Id) {
        Scp163Mood[] All = values();
        return Id >= 0 && Id < All.length ? All[Id] : Calm;
    }

    public static Scp163Mood byKey(String Key) {
        for (Scp163Mood Mood : values()) {
            if (Mood.key().equals(Key)) {
                return Mood;
            }
        }
        return Calm;
    }
}
