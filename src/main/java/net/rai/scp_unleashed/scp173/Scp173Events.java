package net.rai.scp_unleashed.scp173;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import net.rai.scp_unleashed.scp173.network.Scp173SightPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp173Events {
    private Scp173Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp173Registry.Statue.get(), Scp173Statue.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(Scp173SightPayload.PayloadType, Scp173SightPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp173Sight.report(User, Payload.Ids());
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp173Registry.SpawnEgg.get());
        }
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp173Registry.ResidueItem.get());
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getLevel().isClientSide) {
            return;
        }
        Entity Joined = Event.getEntity();
        if (Joined instanceof Scp131Pod Pod) {
            Scp173PodWatch.attach(Pod);
        } else if (Joined instanceof Scp173Statue Statue && Statue.isClone() && Event.getLevel() instanceof ServerLevel World) {
            Scp173Clones.get(World.getServer()).add(Statue.getUUID());
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        if (Event.getLevel().isClientSide || !(Event.getEntity() instanceof Scp173Statue Statue) || !(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Statue.trackers().clear();
        Entity.RemovalReason Reason = Statue.getRemovalReason();
        if (Statue.isClone() && Reason != null && Reason.shouldDestroy()) {
            Scp173Clones.get(World.getServer()).remove(Statue.getUUID());
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getTarget() instanceof Scp173Statue Statue && Event.getEntity() instanceof ServerPlayer User) {
            Statue.trackers().add(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void stopTracking(PlayerEvent.StopTracking Event) {
        if (Event.getTarget() instanceof Scp173Statue Statue && Event.getEntity() instanceof ServerPlayer User) {
            Statue.trackers().remove(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp173Sight.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp173Sight.reset(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp173Sight.reset(User);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp173Sight.clear();
    }
}
