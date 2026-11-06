package net.rai.scp_unleashed.z0002;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.z0002.client.Z0002ClientHooks;
import net.rai.scp_unleashed.z0002.network.Z0002ChannelPayload;
import net.rai.scp_unleashed.z0002.network.Z0002DomainPayload;
import net.rai.scp_unleashed.z0002.network.Z0002WarningPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Z0002Events {
    private Z0002Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Z0002DomainPayload.PayloadType, Z0002DomainPayload.PayloadCodec, (Payload, Context) -> Z0002ClientHooks.domain(Payload));
        Registrar.playToClient(Z0002ChannelPayload.PayloadType, Z0002ChannelPayload.PayloadCodec, (Payload, Context) -> Z0002ClientHooks.channel(Payload));
        Registrar.playToClient(Z0002WarningPayload.PayloadType, Z0002WarningPayload.PayloadCodec, (Payload, Context) -> Z0002ClientHooks.warning(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Z0002FingerItem.create());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Z0002Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Z0002Domains.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent Event) {
        Z0002Domains.beginRecovery();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Z0002Domains.stopped();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        if (!Event.isCanceled() && Event.getEntity() instanceof ServerPlayer Victim) {
            Z0002Domains.died(Victim);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void damaged(LivingDamageEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer Victim) {
            Z0002Domains.hurt(Victim, Event.getNewDamage());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Z0002Domains.loggedIn(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Z0002Domains.loggedOut(User);
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getEntity() instanceof Z0002Shrine Shrine && !Z0002Domains.shrineValid(World, Shrine)) {
            Event.setCanceled(true);
        }
    }
}
