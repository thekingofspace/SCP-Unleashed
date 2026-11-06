package net.rai.scp_unleashed.scp3008.gear;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carry;
import net.rai.scp_unleashed.scp3008.gear.client.Scp3008CarryClient;
import net.rai.scp_unleashed.scp3008.gear.network.Scp3008CarryActionPayload;
import net.rai.scp_unleashed.scp3008.gear.network.Scp3008CarrySyncPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008GearEvents {
    private Scp3008GearEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp3008CarrySyncPayload.PayloadType, Scp3008CarrySyncPayload.PayloadCodec, (Payload, Context) -> Scp3008CarryClient.sync(Payload));
        Registrar.playToServer(Scp3008CarryActionPayload.PayloadType, Scp3008CarryActionPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp3008Carry.act(User, Payload.Action());
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp3008GearRegistry.Crowbar.get());
            Event.accept(Scp3008GearRegistry.TableLeg.get());
            Event.accept(Scp3008GearRegistry.CabinetDoor.get());
            Event.accept(Scp3008GearRegistry.Spear.get());
            Event.accept(Scp3008GearRegistry.Shield.get());
        }
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp3008GearRegistry.ShelfShortItem.get());
            Event.accept(Scp3008GearRegistry.ShelfTallItem.get());
        }
    }

    private static boolean busy(Player User) {
        return User != null && Scp3008Carry.carrying(User);
    }

    private static void block(ICancellableEvent Event) {
        Event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        if (busy(Event.getEntity())) {
            Event.setCancellationResult(InteractionResult.FAIL);
            block(Event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem Event) {
        if (busy(Event.getEntity())) {
            Event.setCancellationResult(InteractionResult.FAIL);
            block(Event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactEntity(PlayerInteractEvent.EntityInteract Event) {
        if (busy(Event.getEntity())) {
            Event.setCancellationResult(InteractionResult.FAIL);
            block(Event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactEntityAt(PlayerInteractEvent.EntityInteractSpecific Event) {
        if (busy(Event.getEntity())) {
            Event.setCancellationResult(InteractionResult.FAIL);
            block(Event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock Event) {
        if (busy(Event.getEntity())) {
            block(Event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attack(AttackEntityEvent Event) {
        if (busy(Event.getEntity())) {
            block(Event);
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        Scp3008Carry.tick(Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof Player User && !User.level().isClientSide && Scp3008Carry.carrying(User)) {
            Scp3008Carry.release(User);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getTarget() instanceof Player Target && Event.getEntity() instanceof ServerPlayer Watcher) {
            Scp3008Carry.sendTo(Watcher, Target);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp3008Carry.sync(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp3008Carry.forget(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp3008Carry.sync(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp3008Carry.sync(User);
        }
    }

    @SubscribeEvent
    public static void shieldBlocked(LivingShieldBlockEvent Event) {
        LivingEntity Holder = Event.getEntity();
        if (Event.getBlocked() && !Holder.level().isClientSide && Holder.getUseItem().is(Scp3008GearRegistry.Shield.get())) {
            Holder.level().playSound(null, Holder.getX(), Holder.getY() + 1.0, Holder.getZ(), Scp3008GearRegistry.ShieldThunk.get(), SoundSource.PLAYERS, 0.8f,
                    0.9f + Holder.getRandom().nextFloat() * 0.2f);
        }
    }
}
