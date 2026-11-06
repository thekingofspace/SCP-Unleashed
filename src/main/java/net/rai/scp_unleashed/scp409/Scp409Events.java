package net.rai.scp_unleashed.scp409;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp409.client.Scp409ClientState;
import net.rai.scp_unleashed.scp409.network.Scp409StatePayload;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp409Events {
    public static final int CleanupTicks = 20;

    private Scp409Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp409StatePayload.PayloadType, Scp409StatePayload.PayloadCodec, (Payload, Context) -> Scp409ClientState.receive(Payload));
    }

    @SubscribeEvent
    public static void tickEntity(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (!Ticked.level().isClientSide) {
            visit(Ticked);
        }
    }

    private static void visit(Entity Ticked) {
        if (Ticked instanceof LivingEntity Subject) {
            if (Scp409Infection.isInfected(Subject)) {
                Scp409Infection.tick(Subject);
            } else if (Subject instanceof ServerPlayer User) {
                Scp409Sync.idle(User);
                if (Math.floorMod(User.tickCount, CleanupTicks) == 0 && Scp409Infection.hasModifiers(User)) {
                    Scp409Infection.clearModifiers(User);
                }
            }
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post Event) {
        DamageSource Source = Event.getSource();
        LivingEntity Victim = Event.getEntity();
        if (Victim.level().isClientSide || Source.is(Scp409Registry.OvergrownDamage)) {
            return;
        }
        if (Source.getEntity() instanceof LivingEntity Attacker && Source.getDirectEntity() == Attacker) {
            Scp409Exposure.melee(Attacker, Victim);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Dead = Event.getEntity();
        if (Event.isCanceled() || Dead.level().isClientSide || !Scp409Shatter.shouldShatter(Dead, Event.getSource())) {
            return;
        }
        Scp409Shatter.shatter(Dead);
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getTarget() instanceof LivingEntity Target && Event.getEntity() instanceof ServerPlayer Watcher && Scp409Infection.isInfected(Target)) {
            Scp409Sync.sendTo(Watcher, Target);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        Scp409Sync.resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        Scp409Sync.resend(Event.getEntity());
        if (!Scp409Infection.isInfected(Event.getEntity())) {
            Scp409Infection.clearModifiers(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        Scp409Sync.resend(Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp409Registry.CaseItem.get());
        }
    }
}
