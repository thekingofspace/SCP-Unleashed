package net.rai.scp_unleashed.scp2521;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.scp2521.client.Scp2521ClientHooks;
import net.rai.scp_unleashed.scp2521.network.Scp2521ReadPayload;
import net.rai.scp_unleashed.scp2521.network.Scp2521ScenePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp2521Events {
    private Scp2521Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp2521ScenePayload.PayloadType, Scp2521ScenePayload.PayloadCodec, (Payload, Context) -> Scp2521ClientHooks.scene(Payload));
        Registrar.playToServer(Scp2521ReadPayload.PayloadType, Scp2521ReadPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer Reader) {
                Scp2521Rules.learn(Reader);
            }
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp2521Registry.CreatureType.get(), Scp2521.createAttributes().build());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        Scp2521Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        Scp2521Trips.tick(Event.getServer());
        Scp2521Containment.watch(Event.getServer());
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent Event) {
        Scp2521Trips.recover(Event.getServer());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        Scp2521Trips.stopping(Event.getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp2521Trips.forgetAll();
        Scp2521Containment.forgetAll();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp2521Trips.loggedIn(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp2521Trips.leave(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !Event.isEndConquered()) {
            Scp2521Trips.loggedIn(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !Event.isCanceled()) {
            Scp2521Trips.died(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void damaged(LivingIncomingDamageEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Scp2521Trips.active(User.getUUID()) && !Event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void joining(EntityJoinLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        if (Event.getEntity() instanceof Scp2521 Creature) {
            boolean Wanted = Creature.contained() ? Scp2521Containment.wanted(World.getServer(), Creature) : Scp2521Trips.wants(World.getServer(), Creature);
            if (!Wanted) {
                Event.setCanceled(true);
            }
        } else if (Event.getEntity() instanceof Scp2521Tendrils Tendrils && !Scp2521Trips.wants(World.getServer(), Tendrils)) {
            Event.setCanceled(true);
        }
    }

    public static boolean bound(Player User) {
        if (User.level().isClientSide) {
            return Scp2521ClientHooks.cinematic();
        }
        return Scp2521Trips.active(User.getUUID());
    }

    private static void hold(Player User, ICancellableEvent Event) {
        if (bound(User)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        if (bound(User)) {
            Event.setCanceled(true);
            return;
        }
        ItemStack Stack = Event.getItemStack();
        if (!Scp2521Containment.isDocument(Stack) || !Scp2521Containment.isSand(Event.getLevel().getBlockState(Event.getPos()))) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.sidedSuccess(Event.getLevel().isClientSide));
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp2521Containment.arm(World, Event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem Event) {
        Player User = Event.getEntity();
        if (bound(User)) {
            Event.setCanceled(true);
            return;
        }
        if (User instanceof ServerPlayer Reader && Scp2521Containment.isDocument(Event.getItemStack()) && (Event.getItemStack().is(ArchiveRegistry.OpenedDocument.get())
                || Event.getItemStack().is(ArchiveRegistry.DatabaseCopy.get()))) {
            Scp2521Rules.learn(Reader);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock Event) {
        hold(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entityInteract(PlayerInteractEvent.EntityInteract Event) {
        hold(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent Event) {
        hold(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        if (Scp2521Trips.active(Event.getPlayer().getUUID())) {
            Event.setCanceled(true);
        }
    }
}
