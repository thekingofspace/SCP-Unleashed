package net.rai.scp_unleashed.dclass;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.Locale;

public enum DClassCrime {
    Smuggling,
    GrandTheft,
    Arson,
    Fraud,
    Desertion,
    Sabotage,
    Counterfeiting,
    Burglary,
    Racketeering,
    Espionage,
    Assault,
    Vandalism;

    public String getId() {
        return name().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    public Component getName() {
        return Component.translatable("dclass.scp_unleashed.crime." + getId());
    }

    public Component getDescription() {
        return Component.translatable("dclass.scp_unleashed.crime." + getId() + ".description");
    }

    public boolean hoards() {
        return this == Smuggling;
    }

    public static DClassCrime byIndex(int Index) {
        DClassCrime[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }

    public static DClassCrime random(RandomSource Random) {
        return byIndex(Random.nextInt(values().length));
    }

    public static DClassCrime byId(String Id) {
        for (DClassCrime Crime : values()) {
            if (Crime.getId().equals(Id)) {
                return Crime;
            }
        }
        return Smuggling;
    }
}
