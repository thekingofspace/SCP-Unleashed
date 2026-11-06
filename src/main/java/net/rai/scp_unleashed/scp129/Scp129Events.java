package net.rai.scp_unleashed.scp129;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp129.client.Scp129ClientHooks;
import net.rai.scp_unleashed.scp129.network.Scp129EpisodePayload;
import net.rai.scp_unleashed.scp129.network.Scp129StatePayload;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp129Events {
    private Scp129Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp129StatePayload.PayloadType, Scp129StatePayload.PayloadCodec, (Payload, Context) -> Scp129ClientHooks.state(Payload));
        Registrar.playToClient(Scp129EpisodePayload.PayloadType, Scp129EpisodePayload.PayloadCodec, (Payload, Context) -> Scp129ClientHooks.episode(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp129Registry.Suppressant.get());
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp129Infection.tick(User);
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (!Ticked.level().isClientSide) {
            visit(Ticked);
        }
    }

    private static void visit(Entity Ticked) {
        if (Ticked instanceof LivingEntity Subject && !(Ticked instanceof Player)
                && (Subject.hasData(Scp129Registry.Infection.get()) || Subject.hasData(Scp129Registry.EpisodeState.get()))) {
            Scp129Infection.tick(Subject);
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post Event) {
        LivingEntity Victim = Event.getEntity();
        if (Victim.level().isClientSide || Event.getNewDamage() <= 0.0f) {
            return;
        }
        if (Event.getSource().getDirectEntity() instanceof LivingEntity Attacker) {
            Scp129Spread.contact(Attacker, Victim);
        }
    }

    @SubscribeEvent
    public static void healing(LivingHealEvent Event) {
        LivingEntity Patient = Event.getEntity();
        if (Patient.level().isClientSide) {
            return;
        }
        float Share = Scp129Course.healShare(Patient);
        if (Share <= 0.0f) {
            Event.setCanceled(true);
        } else if (Share < 1.0f) {
            Event.setAmount(Event.getAmount() * Share);
        }
    }

    @SubscribeEvent
    public static void pickedUp(ItemEntityPickupEvent.Post Event) {
        if (Event.getPlayer() instanceof ServerPlayer Picker) {
            Scp129Spread.handOver(Picker, Event.getItemEntity());
        }
    }

    @SubscribeEvent
    public static void injectOther(PlayerInteractEvent.EntityInteract Event) {
        if (!(Event.getTarget() instanceof LivingEntity Target) || !Scp129SuppressantItem.isSuppressant(Event.getItemStack())) {
            return;
        }
        InteractionResult Result = Scp129SuppressantItem.inject(Event.getEntity(), Event.getHand(), Target);
        if (Result.consumesAction()) {
            Event.setCancellationResult(Result);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        restart(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        restart(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp129Sync.forget(User);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        Scp129Sync.forgetAll();
    }

    private static void resend(Player Target) {
        if (Target instanceof ServerPlayer User) {
            Scp129Sync.send(User);
        }
    }

    private static void restart(Player Target) {
        if (Target instanceof ServerPlayer User) {
            Scp129Sync.episode(User, Scp129Symptoms.Clear, 0, 0.0f);
            Scp129Sync.send(User);
        }
    }
}
