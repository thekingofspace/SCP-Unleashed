package net.rai.scp_unleashed.control;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.client.ControlClientHooks;
import net.rai.scp_unleashed.control.network.ControlEventPayload;
import net.rai.scp_unleashed.control.network.ControlLookPayload;
import net.rai.scp_unleashed.control.network.ControlReleasePayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ControlEvents {
    private ControlEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(ControlLookPayload.PayloadType, ControlLookPayload.PayloadCodec, (Payload, Context) -> ControlClientHooks.look(Payload));
        Registrar.playToClient(ControlEventPayload.PayloadType, ControlEventPayload.PayloadCodec, (Payload, Context) -> ControlClientHooks.event(Payload));
        Registrar.playToServer(ControlReleasePayload.PayloadType, ControlReleasePayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                ControlManager.release(User);
            }
        }));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(ControlRegistry.Controller.get());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        ControlCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interceptDeath(LivingDeathEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || !ControlSessions.isControlling(User)) {
            return;
        }
        Event.setCanceled(true);
        User.setHealth(1.0f);
        ControlManager.markDeath(User, Event.getSource());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void shieldPending(LivingIncomingDamageEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && ControlManager.hasPendingDeath(User)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        for (ServerPlayer User : List.copyOf(Server.getPlayerList().getPlayers())) {
            if (ControlManager.hasPendingDeath(User)) {
                ControlManager.processDeath(User);
            } else if (ControlManager.takeRelease(User)) {
                ControlManager.release(User);
            } else if (ControlSessions.isControlling(User) && User.isDeadOrDying() && !User.isRemoved()) {
                ControlManager.markDeath(User, User.damageSources().generic());
                User.setHealth(1.0f);
                ControlManager.processDeath(User);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            if (ControlManager.hasPendingDeath(User)) {
                ControlManager.processDeath(User);
            } else {
                ControlManager.release(User);
            }
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && ControlSessions.isControlling(User)) {
            ControlManager.release(User);
            User.sendSystemMessage(Component.translatable("message.scp_unleashed.control.recovered").withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || !ControlSessions.isControlling(User) || Event.isEndConquered()) {
            return;
        }
        GlobalPos Fell = User.getLastDeathLocation().orElse(null);
        ServerLevel Where = Fell == null ? null : User.server.getLevel(Fell.dimension());
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().removeItemNoUpdate(Slot);
            if (Stack.isEmpty()) {
                continue;
            }
            if (Where != null) {
                Containers.dropItemStack(Where, Fell.pos().getX() + 0.5, Fell.pos().getY() + 0.5, Fell.pos().getZ() + 0.5, Stack);
            } else {
                User.drop(Stack, false);
            }
        }
        ControlManager.abandon(User);
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getTarget() instanceof ServerPlayer Watched && Event.getEntity() instanceof ServerPlayer Watcher && ControlSessions.isControlling(Watched)) {
            PacketDistributor.sendToPlayer(Watcher, ControlManager.lookOf(Watched));
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            ControlManager.broadcastLook(User);
        }
    }

    @SubscribeEvent
    public static void leavingEnd(EntityTravelToDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && ControlSessions.isControlling(User) && User.level().dimension() == Level.END
                && Event.getDimension() == Level.OVERWORLD) {
            Event.setCanceled(true);
            ControlManager.requestRelease(User);
        }
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent Event) {
        ControlManager.releaseAll(Event.getServer());
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Entity Joining = Event.getEntity();
        if (Joining instanceof Controllable Body && !ControlSessions.isTransferring(Joining.getUUID())) {
            ControlRoster Roster = ControlRoster.get(World.getServer());
            if (!Roster.accepts(Joining.getUUID(), Body.controlIncarnation())) {
                Event.setCanceled(true);
                return;
            }
            Roster.know(Joining.getUUID(), Body.controlIncarnation());
        } else if (Joining instanceof VacantBody Seat && Event.loadedFromDisk()) {
            UUID Owner = Seat.getOwnerId();
            if (Owner == null || !Joining.getUUID().equals(ControlRoster.get(World.getServer()).vacantOf(Owner))) {
                Event.setCanceled(true);
            }
        }
    }
}
