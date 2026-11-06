package net.rai.scp_unleashed.scp016;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public enum Scp016Cause {
    Other("other"),
    Slain("player"),
    Starved("starvation"),
    Drowned("drowning"),
    Fell("fall");

    private static final Map<DamageSource, Scp016Cause> Pending = Collections.synchronizedMap(new WeakHashMap<>());
    private final String Key;

    Scp016Cause(String Key) {
        this.Key = Key;
    }

    public String key() {
        return Key;
    }

    @Nullable
    public static Scp016Cause byKey(String Name) {
        String Wanted = Name.toLowerCase(Locale.ROOT);
        for (Scp016Cause Cause : values()) {
            if (Cause.Key.equals(Wanted)) {
                return Cause;
            }
        }
        return null;
    }

    public static Scp016Cause classify(DamageSource Source, LivingEntity Fallen) {
        if (Source.is(Scp016Registry.SplitDamage)) {
            return Other;
        }
        if (Source.is(DamageTypeTags.IS_FALL)) {
            return Fell;
        }
        if (Source.is(DamageTypeTags.IS_DROWNING)) {
            return Drowned;
        }
        if (Source.is(DamageTypes.STARVE)) {
            return Starved;
        }
        Entity Causing = Source.getEntity();
        if (Causing instanceof Scp016Spawn) {
            return Other;
        }
        if (Causing != null) {
            return credited(Causing) ? Slain : Other;
        }
        return Fallen.getKillCredit() instanceof Player ? Slain : Other;
    }

    public static boolean credited(Entity Causing) {
        if (Causing instanceof Player) {
            return true;
        }
        return Causing instanceof OwnableEntity Pet && Pet.getOwner() instanceof Player;
    }

    public static void remember(DamageSource Source, Scp016Cause Cause) {
        Pending.putIfAbsent(Source, Cause);
    }

    public static Scp016Cause recall(DamageSource Source, LivingEntity Fallen) {
        Scp016Cause Stored = Pending.remove(Source);
        return Stored != null ? Stored : classify(Source, Fallen);
    }
}
