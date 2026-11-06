package net.rai.scp_unleashed.dclass;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.Locale;

public enum DClassTrait {
    Cowardly,
    Curious,
    Lazy,
    Restless,
    Combative,
    Obedient,
    Paranoid,
    Stoic,
    Gluttonous,
    Sociable,
    Insomniac,
    Cheerful;

    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component getName() {
        return Component.translatable("dclass.scp_unleashed.trait." + getId());
    }

    public Component getDescription() {
        return Component.translatable("dclass.scp_unleashed.trait." + getId() + ".description");
    }

    public static DClassTrait byIndex(int Index) {
        DClassTrait[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }

    public static DClassTrait random(RandomSource Random) {
        return byIndex(Random.nextInt(values().length));
    }

    public static DClassTrait byId(String Id) {
        for (DClassTrait Trait : values()) {
            if (Trait.getId().equals(Id)) {
                return Trait;
            }
        }
        return Cowardly;
    }

    public float fleeHealth() {
        return switch (this) {
            case Cowardly -> 0.85f;
            case Stoic, Combative -> 0.2f;
            case Paranoid -> 0.7f;
            default -> 0.5f;
        };
    }

    public float avoidRange() {
        return switch (this) {
            case Cowardly -> 12.0f;
            case Paranoid -> 10.0f;
            case Stoic, Combative -> 4.0f;
            default -> 7.0f;
        };
    }

    public int strollRange() {
        return switch (this) {
            case Restless -> 22;
            case Lazy -> 5;
            case Insomniac -> 14;
            default -> 10;
        };
    }

    public int strollInterval() {
        return switch (this) {
            case Restless -> 40;
            case Lazy -> 400;
            default -> 120;
        };
    }

    public int bedtime() {
        return switch (this) {
            case Restless -> 14000;
            default -> 13000;
        };
    }

    public int wakeTime() {
        return switch (this) {
            case Lazy -> 23600;
            case Restless -> 22000;
            default -> 23000;
        };
    }

    public boolean sleeps() {
        return this != Insomniac;
    }

    public boolean fightsBack() {
        return this == Combative;
    }

    public boolean picksUpItems() {
        return this != Lazy;
    }

    public int hungerThreshold() {
        return switch (this) {
            case Gluttonous -> 19;
            case Stoic -> 10;
            default -> 14;
        };
    }

    public float glanceRate() {
        return switch (this) {
            case Paranoid -> 2.6f;
            case Curious -> 1.6f;
            case Stoic, Lazy -> 0.6f;
            default -> 1.0f;
        };
    }
}
