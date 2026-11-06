package net.rai.scp_unleashed.z0001;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.z0001.client.Z0001ClientHooks;
import net.rai.scp_unleashed.z0001.network.Z0001DreadPayload;
import net.rai.scp_unleashed.z0001.network.Z0001ResultPayload;
import net.rai.scp_unleashed.z0001.network.Z0001WritePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Z0001Events {
    private Z0001Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(Z0001WritePayload.PayloadType, Z0001WritePayload.PayloadCodec, Z0001WritePayload::handle);
        Registrar.playToClient(Z0001ResultPayload.PayloadType, Z0001ResultPayload.PayloadCodec, (Payload, Context) -> Z0001ClientHooks.result(Payload));
        Registrar.playToClient(Z0001DreadPayload.PayloadType, Z0001DreadPayload.PayloadCodec, (Payload, Context) -> Z0001ClientHooks.dread(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Z0001BookItem.create());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Z0001Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Z0001Ink.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void totem(LivingUseTotemEvent Event) {
        if (Event.getSource().is(Z0001Registry.BlackBookDamage)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Victim) {
            Z0001Ink.died(Victim);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Z0001Ink.leaving(User);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Z0001Ink.clear();
    }
}
