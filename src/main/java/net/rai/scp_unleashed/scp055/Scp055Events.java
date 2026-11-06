package net.rai.scp_unleashed.scp055;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp055.client.Scp055ClientHooks;
import net.rai.scp_unleashed.scp055.network.Scp055EpisodePayload;
import net.rai.scp_unleashed.scp055.network.Scp055WakePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp055Events {
    private Scp055Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp055EpisodePayload.PayloadType, Scp055EpisodePayload.PayloadCodec, (Payload, Context) -> Scp055ClientHooks.episode(Payload));
        Registrar.playToClient(Scp055WakePayload.PayloadType, Scp055WakePayload.PayloadCodec, (Payload, Context) -> Scp055ClientHooks.wake(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp055Registry.CaseItem.get());
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp055Episodes.tick(User);
        }
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Event.getNewDamage() > 0.0f) {
            Scp055Episodes.interrupt(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !Event.isCanceled()) {
            Scp055Episodes.interrupt(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        Scp055Episodes.interrupt(Event.getEntity());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp055Episodes.forget(Event.getEntity());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp055Episodes.clear();
    }
}
