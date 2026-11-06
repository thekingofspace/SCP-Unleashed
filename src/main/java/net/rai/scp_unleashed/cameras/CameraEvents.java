package net.rai.scp_unleashed.cameras;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cameras.client.CameraClientHooks;
import net.rai.scp_unleashed.cameras.network.CameraEditPayload;
import net.rai.scp_unleashed.cameras.network.CameraExitPayload;
import net.rai.scp_unleashed.cameras.network.CameraFeedPayload;
import net.rai.scp_unleashed.cameras.network.CameraLookPayload;
import net.rai.scp_unleashed.cameras.network.CameraOpenPayload;
import net.rai.scp_unleashed.cameras.network.CameraSwitchPayload;
import net.rai.scp_unleashed.cameras.network.CameraWindowPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class CameraEvents {
    private CameraEvents() {
    }

    private static InteractionHand hand(int Ordinal) {
        return Ordinal == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(CameraFeedPayload.PayloadType, CameraFeedPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> CameraClientHooks.feed(Payload)));
        Registrar.playToClient(CameraWindowPayload.PayloadType, CameraWindowPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> CameraClientHooks.window(Payload)));
        Registrar.playToServer(CameraOpenPayload.PayloadType, CameraOpenPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                CameraSessions.open(User, hand(Payload.Hand()), Payload.Index());
            }
        }));
        Registrar.playToServer(CameraSwitchPayload.PayloadType, CameraSwitchPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                CameraSessions.cycle(User, Payload.Step());
            }
        }));
        Registrar.playToServer(CameraExitPayload.PayloadType, CameraExitPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                CameraSessions.end(User, CameraSessions.End.Left);
            }
        }));
        Registrar.playToServer(CameraLookPayload.PayloadType, CameraLookPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                CameraSessions.look(User, Payload.Pan(), Payload.Tilt());
            }
        }));
        Registrar.playToServer(CameraEditPayload.PayloadType, CameraEditPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                CameraSessions.edit(User, hand(Payload.Hand()), Payload.Index(), Payload.Remove(), Payload.Name());
            }
        }));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(CameraRegistry.CameraItem.get());
            Event.accept(CameraRegistry.Tablet.get());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        CameraSessions.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void hurt(LivingDamageEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Event.getNewDamage() > 0.0f && CameraSessions.isViewing(User)) {
            CameraSessions.end(User, CameraSessions.End.Hurt);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        if (!Event.isCanceled() && Event.getEntity() instanceof ServerPlayer User) {
            CameraSessions.end(User, CameraSessions.End.Quiet);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void travelling(EntityTravelToDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            CameraSessions.end(User, CameraSessions.End.Quiet);
        }
    }

    @SubscribeEvent
    public static void teleported(EntityTeleportEvent Event) {
        if (!Event.isCanceled() && Event.getEntity() instanceof ServerPlayer User) {
            CameraSessions.end(User, CameraSessions.End.Quiet);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            CameraSessions.end(User, CameraSessions.End.Quiet);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            CameraSessions.end(User, CameraSessions.End.Quiet);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            CameraSessions.end(User, CameraSessions.End.Quiet);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        CameraSessions.clear(Event.getServer());
    }

    private static void block(Player User, ICancellableEvent Event) {
        if (User instanceof ServerPlayer Viewer && CameraSessions.isViewing(Viewer)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        block(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem Event) {
        block(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock Event) {
        block(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactEntity(PlayerInteractEvent.EntityInteract Event) {
        block(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactEntityAt(PlayerInteractEvent.EntityInteractSpecific Event) {
        block(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attack(AttackEntityEvent Event) {
        block(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void breaking(BlockEvent.BreakEvent Event) {
        block(Event.getPlayer(), Event);
    }
}
