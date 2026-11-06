package net.rai.scp_unleashed.scp420j;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp420j.client.Scp420jClientHooks;
import net.rai.scp_unleashed.scp420j.network.Scp420jFloatPayload;
import net.rai.scp_unleashed.scp420j.network.Scp420jSongPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp420jEvents {
    private Scp420jEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp420jFloatPayload.PayloadType, Scp420jFloatPayload.PayloadCodec, (Payload, Context) -> Scp420jClientHooks.floating(Payload));
        Registrar.playToClient(Scp420jSongPayload.PayloadType, Scp420jSongPayload.PayloadCodec, (Payload, Context) -> Scp420jClientHooks.song(Payload));
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp420jRegistry.Seeds.get());
            Event.accept(Scp420jRegistry.Leaf.get());
            Event.accept(Scp420jRegistry.Joint.get());
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp420jFloat.tick(User);
        }
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent Event) {
        if (!Event.getEntity().level().isClientSide && Scp420jFloat.cushion(Event.getEntity())) {
            Event.setDistance(0.0f);
            Event.setDamageMultiplier(0.0f);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp420jFloat.stop(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp420jFloat.elsewhere(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp420jFloat.resend(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp420jFloat.forget(User);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        Scp420jFloat.forgetAll();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp420jCommands.register(Event.getDispatcher());
    }
}
