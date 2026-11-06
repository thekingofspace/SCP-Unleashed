package net.rai.scp_unleashed.scp253;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp253Events {
    public static final int TidyTicks = 20;

    private Scp253Events() {
    }

    @SubscribeEvent
    public static void tickInfected(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (Ticked.level().isClientSide) {
            return;
        }
        visit(Ticked);
    }

    private static void visit(Entity Ticked) {
        if (Ticked instanceof LivingEntity Subject) {
            if (Scp253Infection.isInfected(Subject)) {
                Scp253Infection.tick(Subject);
            } else if (Subject.getExistingDataOrNull(Scp253Registry.Display.get()) != null || Subject instanceof ServerPlayer && stale(Subject)) {
                Scp253Infection.clear(Subject);
            }
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    private static boolean stale(LivingEntity Subject) {
        if (Math.floorMod(Subject.tickCount, TidyTicks) != 0) {
            return false;
        }
        AttributeInstance Speed = Subject.getAttribute(Attributes.MOVEMENT_SPEED);
        return Speed != null && Speed.hasModifier(Scp253Registry.SlownessId);
    }

    @SubscribeEvent
    public static void passOnBlow(LivingDamageEvent.Post Event) {
        LivingEntity Victim = Event.getEntity();
        if (Victim.level().isClientSide || Event.getNewDamage() <= 0.0f) {
            return;
        }
        DamageSource Source = Event.getSource();
        if (!isBlow(Source) || !(Source.getEntity() instanceof LivingEntity Attacker) || Attacker == Victim) {
            return;
        }
        if (Scp253Infection.isInfected(Attacker) && !Scp253Infection.isInfected(Victim)) {
            Scp253Infection.infect(Victim);
        }
    }

    public static boolean isBlow(DamageSource Source) {
        return Source.isDirect() && (Source.is(DamageTypes.PLAYER_ATTACK) || Source.is(DamageTypes.MOB_ATTACK) || Source.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent Event) {
        LivingEntity Fallen = Event.getEntity();
        if (Event.isCanceled() || !(Fallen.level() instanceof ServerLevel World) || !Scp253Infection.isInfected(Fallen)) {
            return;
        }
        Scp253Infection.cure(Fallen);
        Scp253Infection.burst(World, Fallen);
    }
}
