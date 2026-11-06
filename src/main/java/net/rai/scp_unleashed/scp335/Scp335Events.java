package net.rai.scp_unleashed.scp335;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.UploadPortBlockEntity;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp335.client.Scp335ClientHooks;
import net.rai.scp_unleashed.scp335.network.Scp335StatePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp335Events {
    private Scp335Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp335StatePayload.PayloadType, Scp335StatePayload.PayloadCodec, (Payload, Context) -> Scp335ClientHooks.state(Payload));
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp335Registry.Disk.get());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp335Bodies.tick(Event.getServer());
        Scp335Episodes.tickAll(Event.getServer());
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (!Event.getLevel().isClientSide()) {
            Scp335Bodies.claim(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp335Episodes.loggedOut(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp335Episodes.resync(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp335Episodes.respawned(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp335Episodes.died(User);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getTarget() instanceof ServerPlayer Target && Event.getEntity() instanceof ServerPlayer Watcher) {
            Scp335Episodes.watched(Watcher, Target);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useBlock(PlayerInteractEvent.RightClickBlock Event) {
        if (stop(Event.getEntity(), Event)) {
            return;
        }
        ItemStack Stack = Event.getItemStack();
        if (Event.getLevel() instanceof ServerLevel World && Stack.getItem() instanceof Scp335DiskItem
                && World.getBlockEntity(Event.getPos()) instanceof UploadPortBlockEntity Port && Port.isEmpty()) {
            Scp335Episodes.playAt(World, Vec3.atBottomCenterOf(Event.getPos()), Scp335Registry.DriveLayers, 0.7f, 1.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        stop(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hitBlock(PlayerInteractEvent.LeftClickBlock Event) {
        stop(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useEntity(PlayerInteractEvent.EntityInteract Event) {
        stop(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useEntityAt(PlayerInteractEvent.EntityInteractSpecific Event) {
        stop(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent Event) {
        stop(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent Event) {
        stop(Event.getPlayer(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void startUsing(LivingEntityUseItemEvent.Start Event) {
        if (Event.getEntity() instanceof Player User) {
            stop(User, Event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void swapHands(LivingSwapItemsEvent.Hands Event) {
        if (Event.getEntity() instanceof Player User) {
            stop(User, Event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void toss(ItemTossEvent Event) {
        Player User = Event.getPlayer();
        if (User == null || User.level().isClientSide || !Scp335Episodes.isLocked(User)) {
            return;
        }
        ItemStack Returned = Event.getEntity().getItem().copy();
        Event.setCanceled(true);
        if (!Returned.isEmpty()) {
            User.getInventory().placeItemBackInInventory(Returned);
        }
    }

    private static boolean stop(Player User, ICancellableEvent Event) {
        if (User != null && Scp335Episodes.isLocked(User)) {
            Event.setCanceled(true);
            return true;
        }
        return false;
    }
}
