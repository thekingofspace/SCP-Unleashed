package net.rai.scp_unleashed.scp012;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp012.client.Scp012ClientHooks;
import net.rai.scp_unleashed.scp012.network.Scp012StatePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp012Events {
    private Scp012Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp012StatePayload.PayloadType, Scp012StatePayload.PayloadCodec, (Payload, Context) -> Scp012ClientHooks.state(Payload));
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp012Exposure.tick(User);
            Scp012Composer.tick(User);
        }
    }

    @SubscribeEvent
    public static void pickedUp(ItemEntityPickupEvent.Post Event) {
        if (Event.getPlayer() instanceof ServerPlayer User) {
            Scp012Composer.pickedUp(User, Event.getOriginalStack());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp012Exposure.forget(Event.getEntity());
        Scp012Composer.forget(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !Event.isCanceled()) {
            User.removeData(Scp012Registry.Composer.get());
            Scp012Composer.forget(User);
            Scp012Exposure.sync(User);
        }
    }

    private static void resend(Player Target) {
        if (Target instanceof ServerPlayer User) {
            Scp012Exposure.resend(User);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp012Exposure.clear();
        Scp012Composer.clear();
        Scp012Sources.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp012CompositionItem.blank());
        }
    }
}
