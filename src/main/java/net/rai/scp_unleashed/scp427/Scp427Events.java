package net.rai.scp_unleashed.scp427;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp427.client.Scp427ClientHooks;
import net.rai.scp_unleashed.scp427.network.Scp427ConvulsePayload;
import net.rai.scp_unleashed.scp427.network.Scp427StatePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp427Events {
    private Scp427Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp427StatePayload.PayloadType, Scp427StatePayload.PayloadCodec, (Payload, Context) -> Scp427ClientHooks.state(Payload));
        Registrar.playToClient(Scp427ConvulsePayload.PayloadType, Scp427ConvulsePayload.PayloadCodec, (Payload, Context) -> Scp427ClientHooks.convulse(Payload));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp427Registry.Flesh.get(), Scp427Flesh.createAttributes().build());
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (Ticked.level().isClientSide || !(Ticked instanceof LivingEntity Subject)) {
            return;
        }
        Scp427Exposure.tick(Subject);
        if (Subject instanceof ServerPlayer User) {
            Scp427Sync.update(User);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp427Light.sweep(Event.getServer());
        Scp427Transformation.endTick(Event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        Scp427Transformation.died(Event);
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp427Light.release(User);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp427Sync.forget(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp427Sync.forget(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp427Sync.forget(User);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getTarget() instanceof LivingEntity Target && Event.getEntity() instanceof ServerPlayer Watcher) {
            Scp427Sync.sendTo(Watcher, Target);
        }
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent Event) {
        Scp427Light.recover(Event.getServer());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent Event) {
        Scp427Light.releaseAll(Event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp427Registry.Locket.get());
            Event.accept(Scp427Registry.SpawnEgg.get());
        }
    }
}
