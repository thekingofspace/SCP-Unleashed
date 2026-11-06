package net.rai.scp_unleashed.health;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class HealthGuard {
    public enum Repair {
        None,
        Restored,
        Finished
    }

    private HealthGuard() {
    }

    public static boolean broken(float Value) {
        return !Float.isFinite(Value);
    }

    public static float safeDamage(float Original, float Current) {
        if (Float.isFinite(Current)) {
            return Current;
        }
        if (Current == Float.POSITIVE_INFINITY) {
            return Float.MAX_VALUE;
        }
        if (Current == Float.NEGATIVE_INFINITY) {
            return 0.0f;
        }
        if (Float.isFinite(Original)) {
            return Math.max(0.0f, Original);
        }
        return Original == Float.POSITIVE_INFINITY ? Float.MAX_VALUE : 0.0f;
    }

    public static boolean needsRepair(LivingEntity Body) {
        return broken(Body.getHealth()) || broken(Body.getAbsorptionAmount());
    }

    public static Repair repair(LivingEntity Body, boolean Live) {
        if (!needsRepair(Body)) {
            return Repair.None;
        }
        if (broken(Body.getAbsorptionAmount())) {
            Body.setAbsorptionAmount(0.0f);
        }
        if (!broken(Body.getHealth())) {
            return Repair.Restored;
        }
        DamageSource Last = Live ? Body.getLastDamageSource() : null;
        if (Last != null) {
            Body.setHealth(0.0f);
            Body.die(Last);
            return Repair.Finished;
        }
        Body.setHealth(Body.getMaxHealth());
        return Repair.Restored;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void incoming(LivingIncomingDamageEvent Event) {
        float Amount = Event.getAmount();
        if (Float.isNaN(Amount)) {
            Event.setCanceled(true);
        } else if (broken(Amount)) {
            Event.setAmount(safeDamage(Amount, Amount));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent.Pre Event) {
        float Current = Event.getNewDamage();
        if (broken(Current)) {
            Event.setNewDamage(safeDamage(Event.getOriginalDamage(), Current));
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && needsRepair(User)) {
            repair(User, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        restore(Event.getEntity() instanceof ServerPlayer User ? User : null);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        restore(Event.getEntity() instanceof ServerPlayer User ? User : null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        restore(Event.getEntity() instanceof ServerPlayer User ? User : null);
    }

    private static void restore(@Nullable ServerPlayer User) {
        if (User != null) {
            repair(User, false);
        }
    }
}
