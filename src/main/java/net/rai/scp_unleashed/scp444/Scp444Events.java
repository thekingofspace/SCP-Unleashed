package net.rai.scp_unleashed.scp444;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp444.client.Scp444ClientHooks;
import net.rai.scp_unleashed.scp444.client.Scp444ClientState;
import net.rai.scp_unleashed.scp444.network.Scp444ClosedPayload;
import net.rai.scp_unleashed.scp444.network.Scp444OpenPayload;
import net.rai.scp_unleashed.scp444.network.Scp444StatePayload;
import net.rai.scp_unleashed.scp444.network.Scp444TurnPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp444Events {
    private Scp444Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp444OpenPayload.PayloadType, Scp444OpenPayload.PayloadCodec, (Payload, Context) -> Scp444ClientHooks.open());
        Registrar.playToClient(Scp444StatePayload.PayloadType, Scp444StatePayload.PayloadCodec, (Payload, Context) -> Scp444ClientState.receive(Payload));
        Registrar.playToServer(Scp444TurnPayload.PayloadType, Scp444TurnPayload.PayloadCodec, Scp444TurnPayload::handle);
        Registrar.playToServer(Scp444ClosedPayload.PayloadType, Scp444ClosedPayload.PayloadCodec, Scp444ClosedPayload::handle);
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        Scp444Reading.tick();
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            Scp444Sync.update(User);
            if (Scp444Affliction.isAfflicted(User)) {
                Scp444Affliction.ambience(User);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Dead = Event.getEntity();
        if (Event.isCanceled() || Dead.level().isClientSide) {
            return;
        }
        Scp444Affliction.clear(Dead);
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp444Sync.resend(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp444Reading.forget(Event.getEntity().getUUID());
        Scp444Silence.forget(Event.getEntity().getUUID());
        Scp444Sync.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp444Sync.resend(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp444Sync.resend(User);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp444Reading.clear();
        Scp444Silence.clear();
        Scp444Sync.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp444Registry.Book.get());
        }
    }
}
