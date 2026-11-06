package net.rai.scp_unleashed.gateway;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.client.GatewayClientHooks;
import net.rai.scp_unleashed.gateway.network.GatewayEnterPayload;
import net.rai.scp_unleashed.gateway.network.GatewayPromptPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GatewayEvents {
    public static final double DyingReach = 4.0;
    private static final Map<UUID, Vec3> Dying = new ConcurrentHashMap<>();

    private GatewayEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(GatewayPromptPayload.PayloadType, GatewayPromptPayload.PayloadCodec, (Payload, Context) -> GatewayClientHooks.prompt(Payload));
        Registrar.playToServer(GatewayEnterPayload.PayloadType, GatewayEnterPayload.PayloadCodec, GatewayEvents::entering);
    }

    private static void entering(GatewayEnterPayload Payload, IPayloadContext Context) {
        if (!(Context.player() instanceof ServerPlayer User)) {
            return;
        }
        ServerLevel World = User.serverLevel();
        if (!World.isLoaded(Payload.Gate()) || !(World.getBlockEntity(Payload.Gate()) instanceof ContainedGatewayBlockEntity Gate) || !Gate.canSend(User)) {
            return;
        }
        GatewayDestination Destination = Gate.destination();
        if (Destination == null) {
            return;
        }
        if (!GatewayTravel.enter(User, Destination, Gate.returnSpot())) {
            User.displayClientMessage(Component.translatable(ContainedGatewayBlockEntity.MessagePrefix + "refused").withStyle(ChatFormatting.GRAY), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(GatewayRegistry.GatewayItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Anomalies.getKey()) {
            Event.accept(GatewayRegistry.Shard.get());
            for (GatewayDestination Destination : GatewayDestinations.all()) {
                Event.accept(GatewayRegistry.shardFor(Destination.Id()));
            }
        }
    }

    public static boolean sealed(Entity Target) {
        return GatewayDestinations.sealed(Target.level().dimension()) && binds(Target.level().dimension(), Target);
    }

    public static boolean binds(ResourceKey<Level> Dimension, Entity Target) {
        GatewayDestination Here = GatewayDestinations.inDimension(Dimension);
        return Here == null || !(Target instanceof ServerPlayer User) || Here.Hooks().binds(User);
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        GatewayDestination From = GatewayDestinations.inDimension(Event.getFrom());
        GatewayDestination To = GatewayDestinations.inDimension(Event.getTo());
        if (From != null && From != To && From.Hooks().binds(User)) {
            ServerLevel Left = User.server.getLevel(Event.getFrom());
            if (Left != null) {
                From.Hooks().left(User, Left);
            }
            if (From.Rules().StashBelongings()) {
                settle(User);
            }
            GatewayTravel.clearReturn(User);
        }
        if (To != null && From != To && To.Rules().StashBelongings() && !GatewayStash.get(User.server).holds(User.getUUID()) && !GatewayTravel.exempt(User) && To.Hooks().binds(User)) {
            ServerLevel Target = User.server.getLevel(Event.getTo());
            GatewayStash.get(User.server).stash(User, To.Id(), GatewayTravel.respawnSpot(User), To.Rules().StashExperience());
            if (Target != null) {
                To.Hooks().arrived(User, Target);
            }
        }
    }

    public static void settle(ServerPlayer User) {
        GatewayStash Stash = GatewayStash.get(User.server);
        GatewayStash.Entry Found = Stash.entry(User.getUUID());
        if (Found == null) {
            return;
        }
        if (GatewayStash.matches(User, Found)) {
            Stash.restore(User);
        } else {
            Stash.discard(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || User.isDeadOrDying()) {
            return;
        }
        GatewayDestination Here = GatewayDestinations.inDimension(User.level().dimension());
        GatewayStash Stash = GatewayStash.get(User.server);
        if (Here != null && Here.Rules().StashBelongings() && Here.Hooks().binds(User)) {
            GatewayStash.Entry Found = Stash.entry(User.getUUID());
            if (Found != null && !GatewayStash.matches(User, Found)) {
                Stash.discard(User.getUUID());
                Found = null;
            }
            if (Found == null && !GatewayTravel.exempt(User)) {
                Stash.stash(User, Here.Id(), GatewayTravel.respawnSpot(User), Here.Rules().StashExperience());
            }
            return;
        }
        settle(User);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dying(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && sealed(User)) {
            Dying.put(User.getUUID(), User.position());
            GatewayDestination Here = GatewayDestinations.inDimension(User.level().dimension());
            if (Here != null) {
                Here.Hooks().died(User, User.serverLevel());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void drops(LivingDropsEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && sealed(User)) {
            GatewayDestination Here = GatewayDestinations.inDimension(User.level().dimension());
            if (Here != null && Here.Rules().WipeOnDeath()) {
                Event.getDrops().clear();
                Event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void experience(LivingExperienceDropEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && sealed(User)) {
            GatewayDestination Here = GatewayDestinations.inDimension(User.level().dimension());
            if (Here != null && Here.Rules().WipeOnDeath()) {
                Event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void joining(EntityJoinLevelEvent Event) {
        if (Dying.isEmpty() || Event.getLevel().isClientSide() || !(Event.getEntity() instanceof ItemEntity || Event.getEntity() instanceof ExperienceOrb)) {
            return;
        }
        Level World = Event.getLevel();
        if (!GatewayDestinations.sealed(World.dimension())) {
            return;
        }
        Entity Joined = Event.getEntity();
        UUID Thrower = Joined instanceof ItemEntity Dropped && Dropped.getOwner() != null ? Dropped.getOwner().getUUID() : null;
        for (Map.Entry<UUID, Vec3> Entry : Dying.entrySet()) {
            if (Entry.getKey().equals(Thrower) || Entry.getValue().distanceToSqr(Joined.position()) <= DyingReach * DyingReach) {
                Event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void cloned(PlayerEvent.Clone Event) {
        if (!Event.isWasDeath() || !(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        Player Old = Event.getOriginal();
        GatewayDestination Where = GatewayDestinations.inDimension(Old.level().dimension());
        if (Where != null && Where.Rules().WipeOnDeath() && Where.Hooks().binds(User)) {
            GatewayStash.wipe(User, Where.Rules().StashExperience());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || Event.isEndConquered()) {
            return;
        }
        GatewayDestination Here = GatewayDestinations.inDimension(User.level().dimension());
        if (Here != null && Here.Rules().StashBelongings()) {
            return;
        }
        settle(User);
        GatewayTravel.clearReturn(User);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void spawnPoint(PlayerSetSpawnEvent Event) {
        if (Event.getSpawnLevel() != null && GatewayDestinations.sealed(Event.getSpawnLevel()) && binds(Event.getSpawnLevel(), Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        if (!Dying.isEmpty()) {
            Dying.clear();
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Dying.clear();
    }
}
