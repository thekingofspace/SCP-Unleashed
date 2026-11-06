package net.rai.scp_unleashed.scp330;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum Scp330Colour {
    Blue,
    Red,
    Green,
    Purple,
    Black,
    Pink;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String itemId() {
        return "scp330_candy_" + id();
    }

    public static Scp330Colour random(RandomSource Random) {
        Scp330Colour[] All = values();
        return All[Random.nextInt(All.length)];
    }

    @Nullable
    public static Scp330Colour byId(String Id) {
        for (Scp330Colour Colour : values()) {
            if (Colour.id().equals(Id.toLowerCase(Locale.ROOT))) {
                return Colour;
            }
        }
        return null;
    }
}
