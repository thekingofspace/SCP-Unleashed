package net.rai.scp_unleashed.scp268;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp268.integration.Scp268Curios;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp268 {
    public static final int SwapCooldown = 20;
    private static final Map<Player, Seen> CurioWorn = Collections.synchronizedMap(new WeakHashMap<>());

    private record Seen(long Tick, boolean Worn) {
    }

    private Scp268() {
    }

    public static boolean isNeglected(@Nullable Entity Target) {
        return Target instanceof Player User && !User.isSpectator() && isWorn(User);
    }

    public static boolean overlooks(@Nullable LivingEntity Looker) {
        return Looker instanceof Enemy || Looker != null && Scp_unleashed.ModId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(Looker.getType()).getNamespace());
    }

    public static boolean isWorn(@Nullable LivingEntity Wearer) {
        if (Wearer == null) {
            return false;
        }
        if (Wearer.getItemBySlot(EquipmentSlot.HEAD).is(Scp268Registry.Cap.get())) {
            return true;
        }
        return Wearer instanceof Player User && inAccessorySlot(User);
    }

    private static boolean inAccessorySlot(Player User) {
        if (!Senses.curiosLoaded()) {
            return false;
        }
        long Tick = User.level().getGameTime();
        Seen Last = CurioWorn.get(User);
        if (Last != null && Last.Tick() == Tick) {
            return Last.Worn();
        }
        boolean Worn = Scp268Curios.isWorn(User);
        CurioWorn.put(User, new Seen(Tick, Worn));
        return Worn;
    }

    public static boolean settling(@Nullable LivingEntity Wearer) {
        return Wearer instanceof Player User && User.getCooldowns().isOnCooldown(Scp268Registry.Cap.get());
    }

    public static boolean wornLive(@Nullable LivingEntity Wearer) {
        return Senses.isWearing(Wearer, Scp268Registry.Cap.get());
    }

    public static boolean refresh(Player User) {
        forget(User);
        return isNeglected(User);
    }

    public static void forget(Player User) {
        CurioWorn.remove(User);
    }
}
