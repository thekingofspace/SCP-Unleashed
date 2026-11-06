package net.rai.scp_unleashed.scp217;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp217.client.Scp217ClientState;
import net.rai.scp_unleashed.scp217.client.Scp217PulseHooks;
import net.rai.scp_unleashed.scp217.network.Scp217PulsePayload;
import net.rai.scp_unleashed.scp217.network.Scp217PulseRequestPayload;
import net.rai.scp_unleashed.scp217.network.Scp217StatePayload;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp217Events {
    public static final int CleanupTicks = 20;

    private Scp217Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp217StatePayload.PayloadType, Scp217StatePayload.PayloadCodec, (Payload, Context) -> Scp217ClientState.receive(Payload));
        Registrar.playToClient(Scp217PulsePayload.PayloadType, Scp217PulsePayload.PayloadCodec, (Payload, Context) -> Scp217PulseHooks.receive(Payload));
        Registrar.playToServer(Scp217PulseRequestPayload.PayloadType, Scp217PulseRequestPayload.PayloadCodec, Scp217PulseRequestPayload::handle);
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
            if (Scp217Clockwork.isInfected(Subject)) {
                Scp217Clockwork.tick(Subject);
            } else if (Subject instanceof ServerPlayer User) {
                Scp217Sync.idle(User);
                if (Math.floorMod(User.tickCount, CleanupTicks) == 0 && Scp217Clockwork.hasDrag(User)) {
                    Scp217Clockwork.clearDrag(User);
                }
            }
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent Event) {
        LivingEntity Fallen = Event.getEntity();
        if (Event.isCanceled() || Fallen.level().isClientSide || !Scp217Clockwork.isInfected(Fallen)) {
            return;
        }
        Scp217Clockwork.forget(Fallen);
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getTarget() instanceof LivingEntity Target && Event.getEntity() instanceof ServerPlayer Watcher && Scp217Clockwork.isInfected(Target)) {
            Scp217Sync.sendTo(Watcher, Target);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        Scp217Sync.resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        Scp217Sync.resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        Scp217Sync.resend(Event.getEntity());
    }
}
